package com.kalogsc.feedbil.ui.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalogsc.feedbil.data.ArticleRepository
import com.kalogsc.feedbil.data.FeedRefreshFailure
import com.kalogsc.feedbil.data.FeedRepository
import com.kalogsc.feedbil.data.RefreshSummary
import com.kalogsc.feedbil.domain.model.ArticleListItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArticleListUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val articles: List<ArticleListItem> = emptyList(),
    val hasFeeds: Boolean = false,
    val isRefreshing: Boolean = false,
    val failures: List<FeedRefreshFailure> = emptyList(),
    val refreshFailed: Boolean = false,
)

/**
 * Observes the combined article list and refreshes feeds once when the app
 * launches. After that the only refresh trigger is the user's explicit action.
 */
class ArticleListViewModel(
    private val articleRepository: ArticleRepository,
    private val feedRepository: FeedRepository,
    private val refreshAll: suspend () -> RefreshSummary,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleListUiState())
    val uiState: StateFlow<ArticleListUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var autoRefreshStarted = false

    init {
        observeArticlesAndFeeds()
    }

    /** Restarts observation after a database load failure. */
    fun retryLoad() {
        observeArticlesAndFeeds()
    }

    /** Manual refresh; also used once for the automatic launch refresh. */
    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isRefreshing = true, failures = emptyList(), refreshFailed = false)
            }
            try {
                val summary = refreshAll()
                _uiState.update {
                    it.copy(isRefreshing = false, failures = summary.failures)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(isRefreshing = false, refreshFailed = true) }
            }
        }
    }

    fun dismissRefreshFailure() {
        _uiState.update { it.copy(failures = emptyList(), refreshFailed = false) }
    }

    private fun observeArticlesAndFeeds() {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        observeJob = viewModelScope.launch {
            combine(
                articleRepository.observeAllWithFeed(),
                feedRepository.observeAll(),
            ) { articles, feeds -> articles to feeds }
                .catch {
                    _uiState.update { state -> state.copy(isLoading = false, loadFailed = true) }
                }
                .collect { (articles, feeds) ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            loadFailed = false,
                            articles = articles,
                            hasFeeds = feeds.isNotEmpty(),
                        )
                    }
                    maybeAutoRefresh(feeds.isNotEmpty())
                }
        }
    }

    private fun maybeAutoRefresh(hasFeeds: Boolean) {
        if (autoRefreshStarted) return
        autoRefreshStarted = true
        if (hasFeeds) refresh()
    }
}
