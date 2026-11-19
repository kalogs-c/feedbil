package com.kalogsc.feedbil.ui.articles

import com.kalogsc.feedbil.MainDispatcherRule
import com.kalogsc.feedbil.ai.ArticleSummarizer
import com.kalogsc.feedbil.ai.FakeModelManager
import com.kalogsc.feedbil.ai.FakeSummarizer
import com.kalogsc.feedbil.ai.ModelState
import com.kalogsc.feedbil.ai.SummaryError
import com.kalogsc.feedbil.ai.SummaryInput
import com.kalogsc.feedbil.ai.SummaryResult
import com.kalogsc.feedbil.domain.ShareContent
import com.kalogsc.feedbil.domain.model.Article
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArticleShareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val collectorScope = CoroutineScope(Dispatchers.Unconfined)

    @After
    fun tearDown() = collectorScope.cancel()

    private val article = Article(
        id = 7L,
        feedId = 1L,
        guid = null,
        link = SOURCE_URL,
        title = "Um título",
        description = "<p>Texto do artigo.</p>",
        contentHtml = null,
        author = null,
        publishedAt = 1_800_000_000_000L,
    )

    private fun viewModel(
        article: Article = this.article,
        summarizer: ArticleSummarizer = FakeSummarizer(),
        model: FakeModelManager = FakeModelManager(ModelState.Ready),
    ) = ArticleDetailViewModel(
        loadArticle = { article },
        markRead = {},
        articleId = article.id,
        summarizer = summarizer,
        modelManager = model,
    )

    @Test
    fun `normal share sends the title and url and never runs inference`() = runBlocking {
        val summarizer = FakeSummarizer()
        val viewModel = viewModel(summarizer = summarizer)
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }

        viewModel.shareNormal()
        yield()

        val content = shares.single() as ShareContent.Normal
        assertEquals("Um título", content.title)
        assertEquals(SOURCE_URL, content.url)
        assertEquals(0, summarizer.calls)
    }

    @Test
    fun `normal share leaves the title to the screen fallback when absent`() = runBlocking {
        val viewModel = viewModel(article = article.copy(title = null))
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }

        viewModel.shareNormal()
        yield()

        val content = shares.single() as ShareContent.Normal
        assertEquals(null, content.title)
        assertEquals(SOURCE_URL, content.url)
    }

    @Test
    fun `sharing is skipped when the article url is not safe`() = runBlocking {
        val viewModel = viewModel(article = article.copy(link = "javascript:alert(1)"))
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }

        viewModel.shareNormal()
        viewModel.shareWithAi()
        yield()

        assertTrue(shares.isEmpty())
    }

    @Test
    fun `ai share reuses an already generated summary`() = runBlocking {
        val summarizer = FakeSummarizer(SummaryResult.Success("Resumo local."))
        val viewModel = viewModel(summarizer = summarizer)
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }
        viewModel.summarize()
        viewModel.awaitState { it.summary != null }

        viewModel.shareWithAi()
        yield()

        val content = shares.single() as ShareContent.Ai
        assertEquals("Resumo local.", content.summary)
        assertEquals(SOURCE_URL, content.url)
        assertEquals(1, summarizer.calls)
    }

    @Test
    fun `ai share reuses the summary even if the model is no longer ready`() = runBlocking {
        val summarizer = FakeSummarizer(SummaryResult.Success("Resumo local."))
        val model = FakeModelManager(ModelState.Ready)
        val viewModel = viewModel(summarizer = summarizer, model = model)
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }
        viewModel.summarize()
        viewModel.awaitState { it.summary != null }
        model.setState(ModelState.NotDownloaded)

        viewModel.shareWithAi()
        yield()

        val content = shares.single() as ShareContent.Ai
        assertEquals("Resumo local.", content.summary)
        assertEquals(1, summarizer.calls)
        assertFalse(viewModel.uiState.value.showAiShareFallback)
    }

    @Test
    fun `ai share generates a summary when none is cached`() = runBlocking {
        val summarizer = FakeSummarizer(SummaryResult.Success("Resumo sob demanda."))
        val viewModel = viewModel(summarizer = summarizer)
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }

        viewModel.shareWithAi()
        yield()

        val content = shares.single() as ShareContent.Ai
        assertEquals("Resumo sob demanda.", content.summary)
        assertEquals(1, summarizer.calls)
        assertEquals("Resumo sob demanda.", viewModel.awaitState { it.summary != null }.summary)
    }

    @Test
    fun `a second ai share reuses the summary generated by the first`() = runBlocking {
        val summarizer = FakeSummarizer(SummaryResult.Success("Resumo único."))
        val viewModel = viewModel(summarizer = summarizer)
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }

        viewModel.shareWithAi()
        yield()
        viewModel.shareWithAi()
        yield()

        assertEquals(2, shares.size)
        assertTrue(shares.all { (it as ShareContent.Ai).summary == "Resumo único." })
        assertEquals(1, summarizer.calls)
    }

    @Test
    fun `ai share during an in-flight generation waits for that summary`() = runBlocking {
        val summarizer = DeferredSummarizer()
        val viewModel = viewModel(summarizer = summarizer)
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }
        viewModel.summarize()
        viewModel.awaitState { it.summaryLoading }

        viewModel.shareWithAi()
        yield()
        assertTrue(shares.isEmpty())
        assertFalse(viewModel.uiState.value.showAiShareFallback)

        summarizer.result.complete(SummaryResult.Success("Resumo em andamento."))
        yield()

        val content = shares.single() as ShareContent.Ai
        assertEquals("Resumo em andamento.", content.summary)
        assertEquals(1, summarizer.calls)
    }

    @Test
    fun `normal share cancels a waiting ai share`() = runBlocking {
        val summarizer = DeferredSummarizer()
        val viewModel = viewModel(summarizer = summarizer)
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }
        viewModel.summarize()
        viewModel.awaitState { it.summaryLoading }
        viewModel.shareWithAi()
        yield()

        viewModel.shareNormal()
        yield()
        summarizer.result.complete(SummaryResult.Success("Resumo tardio."))
        yield()

        assertEquals(1, shares.size)
        assertTrue(shares.single() is ShareContent.Normal)
    }

    @Test
    fun `a failed in-flight generation offers the fallback to the waiting ai share`() = runBlocking {
        val summarizer = DeferredSummarizer()
        val viewModel = viewModel(summarizer = summarizer)
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }
        viewModel.summarize()
        viewModel.awaitState { it.summaryLoading }
        viewModel.shareWithAi()
        yield()

        summarizer.result.complete(SummaryResult.Failure(SummaryError.EMPTY_OUTPUT))
        yield()

        assertTrue(viewModel.awaitState { it.showAiShareFallback }.showAiShareFallback)
        assertTrue(shares.isEmpty())
    }

    @Test
    fun `ai share offers the fallback when no summarizer is available`() = runBlocking {
        val viewModel = ArticleDetailViewModel(
            loadArticle = { article },
            markRead = {},
            articleId = article.id,
            modelManager = FakeModelManager(ModelState.Ready),
        )
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }

        viewModel.shareWithAi()
        yield()

        assertTrue(viewModel.awaitState { it.showAiShareFallback }.showAiShareFallback)
        assertTrue(shares.isEmpty())
    }

    @Test
    fun `ai share offers the fallback when the model is unavailable`() = runBlocking {
        val summarizer = FakeSummarizer()
        val viewModel = viewModel(
            summarizer = summarizer,
            model = FakeModelManager(ModelState.NotDownloaded),
        )
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }

        viewModel.shareWithAi()
        yield()

        assertTrue(viewModel.awaitState { it.showAiShareFallback }.showAiShareFallback)
        assertEquals(0, summarizer.calls)
        assertTrue(shares.isEmpty())
    }

    @Test
    fun `ai share offers the fallback when inference fails`() = runBlocking {
        val summarizer = FakeSummarizer(SummaryResult.Failure(SummaryError.INFERENCE_FAILED))
        val viewModel = viewModel(summarizer = summarizer)
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }

        viewModel.shareWithAi()
        yield()

        assertTrue(viewModel.awaitState { it.showAiShareFallback }.showAiShareFallback)
        assertEquals(SummaryError.INFERENCE_FAILED, viewModel.uiState.value.summaryError)
        assertTrue(shares.isEmpty())
    }

    @Test
    fun `ai share offers the fallback when the article has no usable text`() = runBlocking {
        val summarizer = FakeSummarizer()
        val empty = article.copy(description = null, contentHtml = "<p>  </p>")
        val viewModel = viewModel(article = empty, summarizer = summarizer)
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }

        viewModel.shareWithAi()
        yield()

        assertTrue(viewModel.awaitState { it.showAiShareFallback }.showAiShareFallback)
        assertEquals(SummaryError.EMPTY_CONTENT, viewModel.uiState.value.summaryError)
        assertEquals(0, summarizer.calls)
        assertTrue(shares.isEmpty())
    }

    @Test
    fun `normal sharing works from the fallback and dismisses it`() = runBlocking {
        val viewModel = viewModel(model = FakeModelManager(ModelState.NotDownloaded))
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }
        viewModel.shareWithAi()
        viewModel.awaitState { it.showAiShareFallback }

        viewModel.shareNormal()
        yield()

        assertEquals(SOURCE_URL, (shares.single() as ShareContent.Normal).url)
        assertFalse(viewModel.uiState.value.showAiShareFallback)
    }

    @Test
    fun `dismissing the fallback clears it without sharing`() = runBlocking {
        val viewModel = viewModel(model = FakeModelManager(ModelState.NotDownloaded))
        val shares = startShareCollector(viewModel)
        viewModel.awaitState { !it.isLoading }
        viewModel.shareWithAi()
        viewModel.awaitState { it.showAiShareFallback }

        viewModel.dismissAiShareFallback()
        yield()

        assertFalse(viewModel.awaitState { !it.showAiShareFallback }.showAiShareFallback)
        assertTrue(shares.isEmpty())
    }

    /**
     * Subscribes before the action, like the screen's LaunchedEffect does. The
     * collector lives outside the test's runBlocking scope so the test does not
     * wait for it to finish; [tearDown] cancels the scope and its collectors.
     */
    private fun startShareCollector(viewModel: ArticleDetailViewModel): MutableList<ShareContent> {
        val events = mutableListOf<ShareContent>()
        collectorScope.launch {
            viewModel.shareContent.collect { events.add(it) }
        }
        return events
    }

    private suspend fun ArticleDetailViewModel.awaitState(
        predicate: (ArticleDetailUiState) -> Boolean,
    ): ArticleDetailUiState = withTimeout(AWAIT_TIMEOUT_MS) { uiState.first(predicate) }

    /** Holds a generation open so a share request can arrive while it runs. */
    private class DeferredSummarizer : ArticleSummarizer {
        val result = CompletableDeferred<SummaryResult>()
        var calls = 0

        override suspend fun summarize(input: SummaryInput): SummaryResult {
            calls++
            return result.await()
        }
    }

    private companion object {
        const val SOURCE_URL = "https://example.com/story"
        const val AWAIT_TIMEOUT_MS = 5_000L
    }
}
