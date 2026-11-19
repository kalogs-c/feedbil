package com.kalogsc.feedbil.ui.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalogsc.feedbil.domain.model.Article
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArticleDetailUiState(
    val isLoading: Boolean = true,
    val article: Article? = null,
    val notFound: Boolean = false,
    val loadFailed: Boolean = false,
)

/** Loads one stored article and records the time the user opened it. */
class ArticleDetailViewModel(
    private val loadArticle: suspend (Long) -> Article?,
    private val markRead: suspend (Long) -> Unit,
    private val articleId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleDetailUiState())
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        _uiState.update { ArticleDetailUiState() }
        viewModelScope.launch {
            try {
                val article = loadArticle(articleId)
                if (article == null) {
                    _uiState.update {
                        it.copy(isLoading = false, notFound = true)
                    }
                    return@launch
                }
                _uiState.update { it.copy(isLoading = false, article = article) }
                runCatching { markRead(articleId) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, article = null, loadFailed = true)
                }
            }
        }
    }
}
