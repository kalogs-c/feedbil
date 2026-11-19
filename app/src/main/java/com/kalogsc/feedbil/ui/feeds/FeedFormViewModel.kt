package com.kalogsc.feedbil.ui.feeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalogsc.feedbil.data.DuplicateFeedUrlException
import com.kalogsc.feedbil.data.FeedRepository
import com.kalogsc.feedbil.data.remote.FeedFetchResult
import com.kalogsc.feedbil.data.remote.FeedFetcher
import com.kalogsc.feedbil.domain.FeedInputError
import com.kalogsc.feedbil.domain.FeedInputValidator
import com.kalogsc.feedbil.domain.model.Feed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why saving a feed failed, after the form itself was valid. */
sealed interface FeedSubmitError {
    data object DuplicateUrl : FeedSubmitError
    data object NotAFeed : FeedSubmitError
    data class HttpError(val code: Int) : FeedSubmitError
    data object Network : FeedSubmitError
    data object Timeout : FeedSubmitError
    data object InvalidUrl : FeedSubmitError
    data object Unknown : FeedSubmitError
    data object FeedMissing : FeedSubmitError
    data object Storage : FeedSubmitError
}

data class FeedFormUiState(
    val name: String = "",
    val url: String = "",
    val isEditing: Boolean = false,
    val isLoadingFeed: Boolean = false,
    val isSaving: Boolean = false,
    val nameError: FeedInputError? = null,
    val urlError: FeedInputError? = null,
    val submitError: FeedSubmitError? = null,
    val saved: Boolean = false,
)

/**
 * Add/edit form state. Saving validates the fields, rejects a URL already used
 * by another feed, and - when the URL is new or changed - fetches it once to
 * confirm it is a reachable feed before storing anything.
 */
class FeedFormViewModel(
    private val feedRepository: FeedRepository,
    private val feedFetcher: FeedFetcher,
    private val feedId: Long = Feed.UNSAVED_ID,
) : ViewModel() {

    private val isNew = feedId == Feed.UNSAVED_ID

    private val _uiState = MutableStateFlow(
        FeedFormUiState(isEditing = !isNew, isLoadingFeed = !isNew),
    )
    val uiState: StateFlow<FeedFormUiState> = _uiState.asStateFlow()

    /** URL as stored, so renaming a feed never requires network access. */
    private var storedUrl: String? = null

    init {
        if (!isNew) {
            viewModelScope.launch { loadFeed() }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, submitError = null) }
    }

    fun onUrlChange(value: String) {
        _uiState.update { it.copy(url = value, urlError = null, submitError = null) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoadingFeed || state.saved) return

        val nameError = FeedInputValidator.nameError(state.name)
        val urlError = FeedInputValidator.urlError(state.url)
        if (nameError != null || urlError != null) {
            _uiState.update { it.copy(nameError = nameError, urlError = urlError) }
            return
        }

        val name = state.name.trim()
        val url = state.url.trim()

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, submitError = null) }

            val urlTaken = try {
                feedRepository.isUrlTaken(url, exceptFeedId = feedId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                failWith(FeedSubmitError.Storage)
                return@launch
            }
            if (urlTaken) {
                failWith(FeedSubmitError.DuplicateUrl)
                return@launch
            }

            val urlNeedsCheck = isNew || url != storedUrl
            if (urlNeedsCheck) {
                val fetchError = fetchError(url)
                if (fetchError != null) {
                    failWith(fetchError)
                    return@launch
                }
            }

            try {
                val stored = if (isNew) {
                    feedRepository.add(name, url)
                    true
                } else {
                    feedRepository.update(feedId, name, url)
                }
                if (stored) {
                    _uiState.update { it.copy(isSaving = false, saved = true) }
                } else {
                    failWith(FeedSubmitError.FeedMissing)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (duplicate: DuplicateFeedUrlException) {
                failWith(FeedSubmitError.DuplicateUrl)
            } catch (error: Exception) {
                failWith(FeedSubmitError.Storage)
            }
        }
    }

    private suspend fun loadFeed() {
        try {
            val feed = feedRepository.findById(feedId)
            if (feed == null) {
                failWith(FeedSubmitError.FeedMissing)
                return
            }
            storedUrl = feed.url
            _uiState.update {
                it.copy(isLoadingFeed = false, name = feed.name, url = feed.url)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            failWith(FeedSubmitError.Storage)
        }
    }

    private suspend fun fetchError(url: String): FeedSubmitError? =
        when (val result = feedFetcher.fetch(url)) {
            is FeedFetchResult.Success -> null
            FeedFetchResult.InvalidUrl -> FeedSubmitError.InvalidUrl
            FeedFetchResult.NotAFeed -> FeedSubmitError.NotAFeed
            FeedFetchResult.Network -> FeedSubmitError.Network
            FeedFetchResult.Timeout -> FeedSubmitError.Timeout
            FeedFetchResult.Unknown -> FeedSubmitError.Unknown
            is FeedFetchResult.HttpError -> FeedSubmitError.HttpError(result.code)
        }

    private fun failWith(error: FeedSubmitError) {
        _uiState.update { it.copy(isSaving = false, submitError = error) }
    }
}
