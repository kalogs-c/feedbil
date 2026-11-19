package com.kalogsc.feedbil.ui.articles

import com.kalogsc.feedbil.MainDispatcherRule
import com.kalogsc.feedbil.data.ArticleRepository
import com.kalogsc.feedbil.data.local.FeedbilDatabase
import com.kalogsc.feedbil.data.local.articleEntity
import com.kalogsc.feedbil.data.local.createInMemoryDatabase
import com.kalogsc.feedbil.data.local.feedEntity
import com.kalogsc.feedbil.data.local.testContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArticleDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: FeedbilDatabase
    private lateinit var repository: ArticleRepository
    private var feedId = 0L

    @Before
    fun setUp() {
        database = createInMemoryDatabase(testContext())
        repository = ArticleRepository(database) { NOW }
        feedId = runBlocking { database.feedDao().insert(feedEntity()) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun viewModel(articleId: Long) = ArticleDetailViewModel(
        loadArticle = repository::getById,
        markRead = repository::markRead,
        articleId = articleId,
    )

    private fun storeArticle(lastReadAt: Long? = null): Long = runBlocking {
        database.articleDao().insertAll(
            listOf(
                articleEntity(
                    feedId = feedId,
                    link = "https://example.com/story",
                    publishedAt = NOW,
                    lastReadAt = lastReadAt,
                ),
            ),
        ).single()
    }

    @Test
    fun `loads the article and records when it was opened`() = runBlocking {
        val articleId = storeArticle()
        val viewModel = viewModel(articleId)

        val state = viewModel.awaitState { !it.isLoading }

        assertNotNull(state.article)
        assertEquals("https://example.com/story", state.article?.link)
        assertFalse(state.notFound)
        val stored = repository.getById(articleId)!!
        assertTrue(stored.isRead)
        assertEquals(NOW, stored.lastReadAt)
    }

    @Test
    fun `opening again updates the last read time`() = runBlocking {
        val articleId = storeArticle(lastReadAt = NOW - 1_000)

        viewModel(articleId).awaitState { !it.isLoading }

        assertEquals(NOW, repository.getById(articleId)!!.lastReadAt)
    }

    @Test
    fun `a missing article is reported without crashing`() = runBlocking {
        val viewModel = viewModel(999L)

        val state = viewModel.awaitState { !it.isLoading }

        assertTrue(state.notFound)
        assertEquals(null, state.article)
    }

    @Test
    fun `a storage failure is reported without crashing`() = runBlocking {
        val viewModel = ArticleDetailViewModel(
            loadArticle = { error("Storage unavailable") },
            markRead = {},
            articleId = 1L,
        )

        val state = viewModel.awaitState { !it.isLoading }

        assertTrue(state.loadFailed)
        assertEquals(null, state.article)
    }

    private suspend fun ArticleDetailViewModel.awaitState(
        predicate: (ArticleDetailUiState) -> Boolean,
    ): ArticleDetailUiState = withTimeout(AWAIT_TIMEOUT_MS) { uiState.first(predicate) }

    private companion object {
        const val NOW = 1_800_000_000_000L
        const val AWAIT_TIMEOUT_MS = 5_000L
    }
}
