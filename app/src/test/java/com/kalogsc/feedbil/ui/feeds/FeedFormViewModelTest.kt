package com.kalogsc.feedbil.ui.feeds

import com.kalogsc.feedbil.MainDispatcherRule
import com.kalogsc.feedbil.data.FeedRepository
import com.kalogsc.feedbil.data.local.FeedbilDatabase
import com.kalogsc.feedbil.data.local.createInMemoryDatabase
import com.kalogsc.feedbil.data.local.testContext
import com.kalogsc.feedbil.data.remote.FeedFetcher
import com.kalogsc.feedbil.domain.FeedInputError
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.exception.RssParsingException
import com.prof18.rssparser.model.RssChannel
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
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
class FeedFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val database: FeedbilDatabase = createInMemoryDatabase(testContext())
    private val repository = FeedRepository(database.feedDao()) { ADDED_AT }

    private val fetchedUrls = mutableListOf<String>()
    private var fetchOutcome: () -> RssChannel = { channel() }
    private val fetcher = FeedFetcher { url ->
        fetchedUrls += url
        fetchOutcome()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `blank fields are reported without fetching`() = runBlocking {
        val viewModel = FeedFormViewModel(repository, fetcher)

        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(FeedInputError.BLANK_NAME, state.nameError)
        assertEquals(FeedInputError.BLANK_URL, state.urlError)
        assertTrue(fetchedUrls.isEmpty())
        assertEquals(0, repository.count())
    }

    @Test
    fun `a url used by another feed is rejected before fetching`() = runBlocking {
        repository.add("Existing", EXISTING_URL)
        val viewModel = FeedFormViewModel(repository, fetcher)
        viewModel.onNameChange("New")
        viewModel.onUrlChange(EXISTING_URL)

        viewModel.save()

        val state = viewModel.awaitState { it.submitError != null }
        assertEquals(FeedSubmitError.DuplicateUrl, state.submitError)
        assertTrue(fetchedUrls.isEmpty())
        assertEquals(1, repository.count())
    }

    @Test
    fun `an unreachable feed is not stored`() = runBlocking {
        fetchOutcome = { throw UnknownHostException("offline") }
        val viewModel = FeedFormViewModel(repository, fetcher)
        viewModel.onNameChange("Example")
        viewModel.onUrlChange("https://example.com/rss")

        viewModel.save()

        val state = viewModel.awaitState { it.submitError != null }
        assertEquals(FeedSubmitError.Network, state.submitError)
        assertEquals(0, repository.count())
    }

    @Test
    fun `an http status is surfaced with its code`() = runBlocking {
        fetchOutcome = { throw HttpException(code = 404, message = "Not Found") }
        val viewModel = FeedFormViewModel(repository, fetcher)
        viewModel.onNameChange("Example")
        viewModel.onUrlChange("https://example.com/rss")

        viewModel.save()

        val state = viewModel.awaitState { it.submitError != null }
        assertEquals(FeedSubmitError.HttpError(404), state.submitError)
        assertEquals(0, repository.count())
    }

    @Test
    fun `a valid feed is trimmed, verified, and stored`() = runBlocking {
        val viewModel = FeedFormViewModel(repository, fetcher)
        viewModel.onNameChange("  Example Blog  ")
        viewModel.onUrlChange("  https://example.com/rss  ")

        viewModel.save()

        val state = viewModel.awaitState { it.saved }
        assertTrue(state.saved)
        assertFalse(state.isSaving)
        assertEquals(listOf("https://example.com/rss"), fetchedUrls)
        assertEquals(
            listOf("Example Blog" to "https://example.com/rss"),
            repository.getAll().map { it.name to it.url },
        )
    }

    @Test
    fun `renaming a feed does not fetch again`() = runBlocking {
        val feed = repository.add("Old name", EXISTING_URL)
        val viewModel = FeedFormViewModel(repository, fetcher, feed.id)
        viewModel.awaitState { !it.isLoadingFeed }

        viewModel.onNameChange("New name")
        viewModel.save()

        viewModel.awaitState { it.saved }
        assertTrue(fetchedUrls.isEmpty())
        assertEquals("New name", repository.findById(feed.id)?.name)
    }

    @Test
    fun `a changed url is verified before saving`() = runBlocking {
        val feed = repository.add("Name", EXISTING_URL)
        fetchOutcome = { throw RssParsingException("not a feed", cause = null) }
        val viewModel = FeedFormViewModel(repository, fetcher, feed.id)
        viewModel.awaitState { !it.isLoadingFeed }

        viewModel.onUrlChange("https://other.example.com/rss")
        viewModel.save()

        val state = viewModel.awaitState { it.submitError != null }
        assertEquals(FeedSubmitError.NotAFeed, state.submitError)
        assertEquals(listOf("https://other.example.com/rss"), fetchedUrls)
        assertEquals(EXISTING_URL, repository.findById(feed.id)?.url)
    }

    @Test
    fun `editing a feed that no longer exists reports it`() = runBlocking {
        val viewModel = FeedFormViewModel(repository, fetcher, feedId = 99L)

        val state = viewModel.awaitState { it.submitError != null }
        assertEquals(FeedSubmitError.FeedMissing, state.submitError)
    }

    private suspend fun FeedFormViewModel.awaitState(
        predicate: (FeedFormUiState) -> Boolean,
    ): FeedFormUiState = withTimeout(AWAIT_TIMEOUT_MS) { uiState.first(predicate) }

    private fun channel() = RssChannel(
        title = "Example Blog",
        link = null,
        description = null,
        image = null,
        lastBuildDate = null,
        updatePeriod = null,
        items = emptyList(),
        itunesChannelData = null,
        youtubeChannelData = null,
    )

    private companion object {
        const val ADDED_AT = 1_000L
        const val AWAIT_TIMEOUT_MS = 5_000L
        const val EXISTING_URL = "https://existing.example.com/rss"
    }
}
