package com.kalogsc.feedbil.ai

/**
 * The text sent to the on-device model. Keeping the runtime behind this small
 * interface lets the article screen use a fake in tests and keeps LiteRT-LM
 * types out of the UI layer.
 */
data class SummaryInput(
    val systemInstruction: String,
    val prompt: String,
)

/** Why a summary could not be produced. Each value has a pt-BR UI message. */
enum class SummaryError {
    EMPTY_CONTENT,
    EMPTY_OUTPUT,
    INPUT_TOO_LONG,
    MODEL_UNAVAILABLE,
    INFERENCE_FAILED,
}

sealed interface SummaryResult {
    data class Success(val text: String) : SummaryResult
    data class Failure(val error: SummaryError) : SummaryResult
}

/** Runs inference locally; implementations must never send text to a service. */
interface ArticleSummarizer {
    suspend fun summarize(input: SummaryInput): SummaryResult
}
