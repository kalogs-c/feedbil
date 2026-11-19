package com.kalogsc.feedbil.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import java.io.File

/** Controlled summarizer: no native engine and no real inference in tests. */
internal class FakeSummarizer(
    var result: SummaryResult = SummaryResult.Success("Resumo."),
) : ArticleSummarizer {
    var calls = 0
    var lastInput: SummaryInput? = null

    override suspend fun summarize(input: SummaryInput): SummaryResult {
        calls++
        lastInput = input
        return result
    }
}

/** Model manager whose state is driven directly by the test. */
internal class FakeModelManager(initial: ModelState) : ModelManager(
    filesDir = File("."),
    httpClient = OkHttpClient.Builder().build(),
) {
    private val flow = MutableStateFlow(initial)
    override val state: StateFlow<ModelState> = flow.asStateFlow()
    var downloadCalls = 0

    fun setState(state: ModelState) {
        flow.value = state
    }

    override suspend fun download() {
        if (flow.value is ModelState.Downloading || flow.value is ModelState.Ready) return
        downloadCalls++
        flow.value = ModelState.Downloading(0, 1)
        flow.value = ModelState.Ready
    }
}
