package com.kalogsc.feedbil.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device summarizer backed by LiteRT-LM. The engine is expensive to load,
 * so one instance is kept for the lifetime of this object and reused; the
 * mutex keeps native calls serialized. Native work runs off the main thread
 * and no article text ever leaves the device.
 */
class LiteRtArticleSummarizer(
    private val modelPath: String,
) : ArticleSummarizer {

    private val mutex = Mutex()
    private var engine: Engine? = null

    override suspend fun summarize(input: SummaryInput): SummaryResult = mutex.withLock {
        withContext(Dispatchers.Default) {
            var phase = Phase.LOADING_MODEL
            var conversation: Conversation? = null
            try {
                if (!File(modelPath).isFile) {
                    return@withContext SummaryResult.Failure(SummaryError.MODEL_UNAVAILABLE)
                }
                val activeEngine = engine ?: createEngine().also { engine = it }
                phase = Phase.GENERATING
                conversation = activeEngine.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(input.systemInstruction),
                        samplerConfig = SamplerConfig(topK = 10, topP = 0.95, temperature = 0.2),
                        maxOutputToken = MAX_OUTPUT_TOKENS,
                    ),
                )
                val response = StringBuilder()
                try {
                    conversation.sendMessageAsync(input.prompt).collect { response.append(it) }
                } catch (cancellation: CancellationException) {
                    // The runtime's Flow does not cancel native generation on its own.
                    runCatching { conversation.cancelProcess() }
                    throw cancellation
                }
                val cleaned = cleanOutput(response.toString())
                if (cleaned.isEmpty()) {
                    SummaryResult.Failure(SummaryError.EMPTY_OUTPUT)
                } else {
                    SummaryResult.Success(cleaned)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                // Errors (native ABI, OOM) are as real a failure here as exceptions.
                SummaryResult.Failure(classify(error, phase))
            } finally {
                conversation?.let { runCatching { it.close() } }
            }
        }
    }

    private fun createEngine(): Engine {
        val created = Engine(EngineConfig(modelPath = modelPath, backend = Backend.CPU()))
        try {
            created.initialize()
        } catch (error: Throwable) {
            // Engine.close() throws if initialization failed, so guard it to keep
            // the original error (and the MODEL_UNAVAILABLE mapping) intact.
            if (created.isInitialized()) runCatching { created.close() }
            throw error
        }
        return created
    }

    private fun classify(error: Throwable, phase: Phase): SummaryError = when {
        phase == Phase.LOADING_MODEL -> SummaryError.MODEL_UNAVAILABLE
        error.isTooLongInput() -> SummaryError.INPUT_TOO_LONG
        else -> SummaryError.INFERENCE_FAILED
    }

    private fun Throwable.isTooLongInput(): Boolean {
        val message = message.orEmpty()
        return listOf("context", "token", "too long", "exceed").any {
            message.contains(it, ignoreCase = true)
        }
    }

    private fun cleanOutput(text: String): String {
        var cleaned = text.trim()
        var stripped = true
        while (stripped) {
            stripped = false
            for (prefix in SUMMARY_PREFIXES) {
                if (cleaned.startsWith(prefix, ignoreCase = true)) {
                    cleaned = cleaned.removePrefix(prefix).trim()
                    stripped = true
                }
            }
        }
        return cleaned.trim('"').trim()
            .replace("**", "")
            .replace(MULTIPLE_NEWLINES, "\n\n")
    }

    private enum class Phase { LOADING_MODEL, GENERATING }

    private companion object {
        const val MAX_OUTPUT_TOKENS = 200
        val SUMMARY_PREFIXES = listOf("Resumo:", "Resumo —", "Summary:")
        val MULTIPLE_NEWLINES = Regex("\\n{3,}")
    }
}
