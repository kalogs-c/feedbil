package com.kalogsc.feedbil.ui.articles

import com.kalogsc.feedbil.MainDispatcherRule
import com.kalogsc.feedbil.ai.ArticleSummarizer
import com.kalogsc.feedbil.ai.FakeModelManager
import com.kalogsc.feedbil.ai.FakeSummarizer
import com.kalogsc.feedbil.ai.ModelState
import com.kalogsc.feedbil.ai.SummaryError
import com.kalogsc.feedbil.ai.SummaryResult
import com.kalogsc.feedbil.domain.model.Article
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArticleSummaryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val article = Article(
        id = 7L,
        feedId = 1L,
        guid = null,
        link = "https://example.com/story",
        title = "Um título",
        description = "<p>Texto do artigo.</p>",
        contentHtml = null,
        author = null,
        publishedAt = 1_800_000_000_000L,
    )

    private fun viewModel(
        article: Article? = this.article,
        summarizer: ArticleSummarizer = FakeSummarizer(),
        model: FakeModelManager = FakeModelManager(ModelState.Ready),
    ) = ArticleDetailViewModel(
        loadArticle = { article },
        markRead = {},
        articleId = 7L,
        summarizer = summarizer,
        modelManager = model,
    )

    @Test
    fun `model availability is exposed and download updates the state`() = runBlocking {
        val model = FakeModelManager(ModelState.NotDownloaded)
        val viewModel = viewModel(model = model)

        assertEquals(ModelState.NotDownloaded, viewModel.awaitState { !it.isLoading }.modelState)
        viewModel.downloadModel()

        assertEquals(ModelState.Ready, viewModel.awaitState { it.modelState is ModelState.Ready }.modelState)
        assertEquals(1, model.downloadCalls)
    }

    @Test
    fun `repeated download taps are serialized by the manager`() = runBlocking {
        val model = FakeModelManager(ModelState.NotDownloaded)
        val viewModel = viewModel(model = model)

        viewModel.downloadModel()
        viewModel.downloadModel()

        assertEquals(1, model.downloadCalls)
    }

    @Test
    fun `a successful summary is shown with the prepared prompt`() = runBlocking {
        val summarizer = FakeSummarizer(SummaryResult.Success("Resumo gerado localmente."))
        val viewModel = viewModel(summarizer = summarizer)
        viewModel.awaitState { !it.isLoading }

        viewModel.summarize()

        val state = viewModel.awaitState { it.summary != null }
        assertEquals("Resumo gerado localmente.", state.summary)
        assertNull(state.summaryError)
        assertEquals(1, summarizer.calls)
        assertTrue(summarizer.lastInput!!.prompt.contains("Texto do artigo."))
    }

    @Test
    fun `an inference failure does not hide the article`() = runBlocking {
        val summarizer = FakeSummarizer(SummaryResult.Failure(SummaryError.INFERENCE_FAILED))
        val viewModel = viewModel(summarizer = summarizer)
        viewModel.awaitState { !it.isLoading }

        viewModel.summarize()

        val state = viewModel.awaitState { it.summaryError != null }
        assertEquals(SummaryError.INFERENCE_FAILED, state.summaryError)
        assertNotNull(state.article)
    }

    @Test
    fun `an overly long article reports a specific error`() = runBlocking {
        val viewModel = viewModel(
            summarizer = FakeSummarizer(SummaryResult.Failure(SummaryError.INPUT_TOO_LONG)),
        )
        viewModel.awaitState { !it.isLoading }

        viewModel.summarize()

        assertEquals(SummaryError.INPUT_TOO_LONG, viewModel.awaitState { it.summaryError != null }.summaryError)
    }

    @Test
    fun `empty content is reported without running inference`() = runBlocking {
        val summarizer = FakeSummarizer()
        val emptyArticle = article.copy(description = null, contentHtml = "<p>  </p>")
        val viewModel = viewModel(article = emptyArticle, summarizer = summarizer)
        viewModel.awaitState { !it.isLoading }

        viewModel.summarize()

        assertEquals(SummaryError.EMPTY_CONTENT, viewModel.awaitState { it.summaryError != null }.summaryError)
        assertEquals(0, summarizer.calls)
    }

    @Test
    fun `summarizing is ignored until the model is available`() = runBlocking {
        val summarizer = FakeSummarizer()
        val viewModel = viewModel(
            summarizer = summarizer,
            model = FakeModelManager(ModelState.NotDownloaded),
        )
        viewModel.awaitState { !it.isLoading }

        viewModel.summarize()

        assertEquals(0, summarizer.calls)
    }

    @Test
    fun `retry recovers from a failed summary`() = runBlocking {
        val summarizer = FakeSummarizer(SummaryResult.Failure(SummaryError.INFERENCE_FAILED))
        val viewModel = viewModel(summarizer = summarizer)
        viewModel.awaitState { !it.isLoading }
        viewModel.summarize()
        viewModel.awaitState { it.summaryError != null }

        summarizer.result = SummaryResult.Success("Resumo após nova tentativa.")
        viewModel.summarize()

        val state = viewModel.awaitState { it.summary != null }
        assertEquals("Resumo após nova tentativa.", state.summary)
        assertNull(state.summaryError)
    }

    private suspend fun ArticleDetailViewModel.awaitState(
        predicate: (ArticleDetailUiState) -> Boolean,
    ): ArticleDetailUiState = withTimeout(AWAIT_TIMEOUT_MS) { uiState.first(predicate) }

    private companion object {
        const val AWAIT_TIMEOUT_MS = 5_000L
    }
}
