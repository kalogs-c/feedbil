package com.kalogsc.feedbil.ui.articles

import com.kalogsc.feedbil.MainDispatcherRule
import com.kalogsc.feedbil.data.FeedRefreshError
import com.kalogsc.feedbil.data.FeedRefreshFailure
import com.kalogsc.feedbil.data.FeedRepository
import com.kalogsc.feedbil.data.RefreshSummary
import com.kalogsc.feedbil.data.ArticleRepository
import com.kalogsc.feedbil.data.local.FeedbilDatabase
import com.kalogsc.feedbil.data.local.articleEntity
import com.kalogsc.feedbil.data.local.createInMemoryDatabase
import com.kalogsc.feedbil.data.local.testContext
import com.kalogsc.feedbil.data.remote.FeedFetchResult
import com.kalogsc.feedbil.domain.model.ArticleListItem
import com.kalogsc.feedbil.domain.model.Feed
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArticleListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val database: FeedbilDatabase = createInMemoryDatabase(testContext())
    private val feedRepository = FeedRepository(database.feedDao()) { NOW }
    private val articleRepository = ArticleRepository(database) { NOW }

    private var refreshCalls = 0
    private var refreshAction: suspend () -> RefreshSummary = {
        RefreshSummary(feedCount = 0, newArticleCount = 0, failures = emptyList())
    }
    private var observeList: () -> Flow<Pair<List<ArticleListItem>, List<Feed>>> = {
        combine(
            articleRepository.observeAllWithFeed(),
            feedRepository.observeAll(),
        ) { articles, feeds -> articles to feeds }
    }

    private fun viewModel(): ArticleListViewModel = ArticleListViewModel(
        articleRepository = articleRepository,
        feedRepository = feedRepository,
        refreshAll = {
            refreshCalls += 1
            refreshAction()
        },
        observeList = { observeList() },
    )

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `an empty app has no articles and does not auto refresh`() = runBlocking {
        val viewModel = viewModel()

        val state = viewModel.awaitState { !it.isLoading }

        assertFalse(state.hasFeeds)
        assertTrue(state.articles.isEmpty())
        assertEquals(0, refreshCalls)
    }

    @Test
    fun `stored articles are observed and the launch refresh runs once`() = runBlocking {
        val feed = feedRepository.add("Example", "https://example.com/rss")
        database.articleDao().insertAll(
            listOf(articleEntity(feedId = feed.id, link = "https://example.com/one", publishedAt = NOW)),
        )

        val viewModel = viewModel()
        val state = viewModel.awaitState {
            !it.isLoading && !it.isRefreshing && it.articles.size == 1 && refreshCalls == 1
        }

        assertEquals("Example", state.articles.single().feedName)
        assertEquals("https://example.com/one", state.articles.single().article.link)
        assertEquals(1, refreshCalls)

        database.articleDao().insertAll(
            listOf(articleEntity(feedId = feed.id, link = "https://example.com/two", publishedAt = NOW + 1)),
        )
        val updated = viewModel.awaitState { it.articles.size == 2 }

        assertEquals(2, updated.articles.size)
        assertEquals(1, refreshCalls)
    }

    @Test
    fun `manual refresh runs again and reports per-feed failures`() = runBlocking {
        feedRepository.add("Example", "https://example.com/rss")
        val viewModel = viewModel()
        viewModel.awaitState { !it.isLoading && !it.isRefreshing }
        val failure = FeedRefreshFailure(
            feedId = 1,
            feedName = "Example",
            reason = FeedRefreshError.Fetch(FeedFetchResult.Network),
        )
        refreshAction = {
            RefreshSummary(feedCount = 1, newArticleCount = 0, failures = listOf(failure))
        }

        viewModel.refresh()

        val state = viewModel.awaitState { it.failures.isNotEmpty() }
        assertEquals(listOf(failure), state.failures)
        assertEquals(2, refreshCalls)

        viewModel.dismissRefreshFailure()
        assertTrue(viewModel.awaitState { it.failures.isEmpty() }.failures.isEmpty())
    }

    @Test
    fun `the initial state is loading until the first emission`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        observeList = {
            flow {
                gate.await()
                emit(emptyList<ArticleListItem>() to emptyList<Feed>())
            }
        }

        val viewModel = viewModel()
        val initial = viewModel.uiState.value
        assertTrue(initial.isLoading)
        assertFalse(initial.loadFailed)

        gate.complete(Unit)
        val loaded = viewModel.awaitState { !it.isLoading }
        assertFalse(loaded.loadFailed)
        assertEquals(0, refreshCalls)
    }

    @Test
    fun `a database load failure sets loadFailed and retryLoad recovers`() = runBlocking {
        var shouldFail = true
        observeList = {
            if (shouldFail) {
                flow<Pair<List<ArticleListItem>, List<Feed>>> { error("Database unavailable") }
            } else {
                flowOf(emptyList<ArticleListItem>() to emptyList<Feed>())
            }
        }
        val viewModel = viewModel()

        val failed = viewModel.awaitState { it.loadFailed }
        assertTrue(failed.loadFailed)
        assertFalse(failed.isLoading)

        shouldFail = false
        viewModel.retryLoad()

        val recovered = viewModel.awaitState { !it.isLoading && !it.loadFailed }
        assertTrue(recovered.articles.isEmpty())
        assertFalse(recovered.hasFeeds)
        assertEquals(0, refreshCalls)
    }

    @Test
    fun `a failed refresh sets the generic error state`() = runBlocking {
        feedRepository.add("Example", "https://example.com/rss")
        refreshAction = { error("Unexpected") }
        val viewModel = viewModel()

        val state = viewModel.awaitState { it.refreshFailed }

        assertTrue(state.refreshFailed)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `manual taps during a refresh do not start another one`() = runBlocking {
        feedRepository.add("Example", "https://example.com/rss")
        val gate = CompletableDeferred<Unit>()
        refreshAction = {
            gate.await()
            RefreshSummary(feedCount = 1, newArticleCount = 0, failures = emptyList())
        }
        val viewModel = viewModel()
        viewModel.awaitState { it.isRefreshing }

        viewModel.refresh()
        viewModel.refresh()

        assertEquals(1, refreshCalls)
        gate.complete(Unit)
        assertFalse(viewModel.awaitState { !it.isRefreshing }.isRefreshing)
    }

    private suspend fun ArticleListViewModel.awaitState(
        predicate: (ArticleListUiState) -> Boolean,
    ): ArticleListUiState = withTimeout(AWAIT_TIMEOUT_MS) { uiState.first(predicate) }

    private companion object {
        const val NOW = 1_800_000_000_000L
        const val AWAIT_TIMEOUT_MS = 5_000L
    }
}
