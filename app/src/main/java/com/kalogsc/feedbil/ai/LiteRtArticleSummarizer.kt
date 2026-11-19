package com.kalogsc.feedbil.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LiteRtLmJniException
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device summarizer backed by LiteRT-LM. The engine is expensive to load, so
 * one instance is kept for the lifetime of this object and reused; the mutex
 * keeps native calls serialized. All native work runs on a background
 * dispatcher and no article text ever leaves the device.
 */
class LiteRtArticleSummarizer(
    private val modelPath: String,
) : ArticleSummarizer {

    private val mutex = Mutex()
    private var engine: Engine? = null

    override suspend fun summarize(input: SummaryInput): SummaryResult = mutex.withLock {
        withContext(Dispatchers.Default) {
            var conversation: Conversation? = null
            try {
                if (!File(modelPath).isFile) {
                    return@withContext SummaryResult.Failure(SummaryError.MODEL_UNAVAILABLE)
                }
                val activeEngine = engine ?: createEngine()
                conversation = activeEngine.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(input.systemInstruction),
                        samplerConfig = SamplerConfig(topK = 10, topP = 0.95, temperature = 0.2),
                        maxOutputToken = MAX_OUTPUT_TOKENS,
                    ),
                )
                val text = conversation.sendMessage(input.prompt).toString().trim()
                val cleaned = cleanOutput(text)
                if (cleaned.isEmpty()) {
                    SummaryResult.Failure(SummaryError.EMPTY_OUTPUT)
                } else {
                    SummaryResult.Success(cleaned)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: LiteRtLmJniException) {
                SummaryResult.Failure(classify(error))
            } catch (error: Exception) {
                SummaryResult.Failure(classify(error))
            } finally {
                conversation?.close()
            }
        }
    }

    private fun createEngine(): Engine {
        val created = Engine(EngineConfig(modelPath = modelPath, backend = Backend.CPU()))
        try {
            created.initialize()
        } catch (error: Throwable) {
            created.close()
            throw error
        }
        engine = created
        return created
    }

    /** Called from the app when it is being torn down; safe to call repeatedly. */
    fun close() {
        engine?.close()
        engine = null
    }

    private fun classify(error: Throwable): SummaryError {
        val message = error.message.orEmpty()
        return when {
            message.contains("context", ignoreCase = true) ||
                message.contains("token", ignoreCase = true) ||
                message.contains("too long", ignoreCase = true) ||
                message.contains("exceed", ignoreCase = true) -> SummaryError.INPUT_TOO_LONG

            error is LiteRtLmJniException && engine == null -> SummaryError.MODEL_UNAVAILABLE
            else -> SummaryError.INFERENCE_FAILED
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
            .replace(Regex("\\*\\*"), "")
            .replace(Regex("\\n{3,}"), "\n\n")
    }

    private companion object {
        const val MAX_OUTPUT_TOKENS = 200
        val SUMMARY_PREFIXES = listOf("Resumo:", "Resumo —", "Summary:")
    }
}
