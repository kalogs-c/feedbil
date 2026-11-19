package com.kalogsc.feedbil.ui.feeds

import com.kalogsc.feedbil.MainDispatcherRule
import com.kalogsc.feedbil.data.FeedRepository
import com.kalogsc.feedbil.data.local.FeedbilDatabase
import com.kalogsc.feedbil.data.local.createInMemoryDatabase
import com.kalogsc.feedbil.data.local.testContext
import kotlinx.coroutines.flow.first
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
class FeedListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val database: FeedbilDatabase = createInMemoryDatabase(testContext())
    private val repository = FeedRepository(database.feedDao()) { ADDED_AT }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `stored feeds are listed in subscription order`() = runBlocking {
        repository.add("First", "https://first.example.com/rss")
        repository.add("Second", "https://second.example.com/rss")

        val viewModel = FeedListViewModel(repository)

        val state = viewModel.awaitState { !it.isLoading }
        assertEquals(listOf("First", "Second"), state.feeds.map { it.name })
        assertFalse(state.loadFailed)
    }

    @Test
    fun `an empty database produces an empty list`() = runBlocking {
        val viewModel = FeedListViewModel(repository)

        val state = viewModel.awaitState { !it.isLoading }
        assertTrue(state.feeds.isEmpty())
    }

    @Test
    fun `deleting a feed removes it from the list`() = runBlocking {
        val feed = repository.add("Example", "https://example.com/rss")
        val viewModel = FeedListViewModel(repository)
        viewModel.awaitState { it.feeds.isNotEmpty() }

        viewModel.delete(feed.id)

        val state = viewModel.awaitState { it.feeds.isEmpty() }
        assertTrue(state.feeds.isEmpty())
        assertEquals(0, repository.count())
    }

    private suspend fun FeedListViewModel.awaitState(
        predicate: (FeedListUiState) -> Boolean,
    ): FeedListUiState = withTimeout(AWAIT_TIMEOUT_MS) { uiState.first(predicate) }

    private companion object {
        const val ADDED_AT = 1_000L
        const val AWAIT_TIMEOUT_MS = 5_000L
    }
}
