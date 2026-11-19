package com.kalogsc.feedbil.ui.feeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalogsc.feedbil.data.FeedRepository
import com.kalogsc.feedbil.domain.model.Feed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedListUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val feeds: List<Feed> = emptyList(),
    val deleteFailed: Boolean = false,
)

/**
 * Observes the stored feeds, so adding, editing, or removing a feed updates the
 * list without an explicit reload.
 */
class FeedListViewModel(
    private val feedRepository: FeedRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedListUiState())
    val uiState: StateFlow<FeedListUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeFeeds()
    }

    /** Restarts observation after a failed load. */
    fun retry() {
        observeFeeds()
    }

    fun delete(feedId: Long) {
        viewModelScope.launch {
            try {
                feedRepository.delete(feedId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(deleteFailed = true) }
            }
        }
    }

    fun deleteFailureShown() {
        _uiState.update { it.copy(deleteFailed = false) }
    }

    private fun observeFeeds() {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        observeJob = viewModelScope.launch {
            feedRepository.observeAll()
                .catch {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
                .collect { feeds ->
                    _uiState.update {
                        it.copy(isLoading = false, loadFailed = false, feeds = feeds)
                    }
                }
        }
    }
}
