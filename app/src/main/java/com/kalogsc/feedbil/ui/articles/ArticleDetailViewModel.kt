package com.kalogsc.feedbil.ui.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalogsc.feedbil.ai.ArticleSummarizer
import com.kalogsc.feedbil.ai.ModelManager
import com.kalogsc.feedbil.ai.ModelState
import com.kalogsc.feedbil.ai.SummaryError
import com.kalogsc.feedbil.ai.SummaryInputBuilder
import com.kalogsc.feedbil.ai.SummaryPromptBuilder
import com.kalogsc.feedbil.ai.SummaryResult
import com.kalogsc.feedbil.domain.model.Article
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ArticleDetailUiState(
    val isLoading: Boolean = true,
    val article: Article? = null,
    val notFound: Boolean = false,
    val loadFailed: Boolean = false,
    val modelState: ModelState = ModelState.NotDownloaded,
    val summary: String? = null,
    val summaryLoading: Boolean = false,
    val summaryError: SummaryError? = null,
)

/**
 * Loads one stored article and records the time the user opened it. Summary
 * and model-download actions are independent of reading: a failure here never
 * hides the article. Optional AI dependencies keep the screen usable in tests.
 */
class ArticleDetailViewModel(
    private val loadArticle: suspend (Long) -> Article?,
    private val markRead: suspend (Long) -> Unit,
    private val articleId: Long,
    private val summarizer: ArticleSummarizer? = null,
    private val modelManager: ModelManager? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ArticleDetailUiState(modelState = modelManager?.state?.value ?: ModelState.NotDownloaded),
    )
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        modelManager?.let { manager ->
            viewModelScope.launch {
                manager.state.collect { state ->
                    _uiState.update { it.copy(modelState = state) }
                }
            }
        }
    }

    fun retry() {
        load()
    }

    fun downloadModel() {
        val manager = modelManager ?: return
        // The manager owns the "already downloading" guard against its own state.
        viewModelScope.launch { manager.download() }
    }

    fun summarize() {
        val current = _uiState.value
        val activeSummarizer = summarizer
        if (activeSummarizer == null || current.summaryLoading || current.modelState !is ModelState.Ready) {
            return
        }
        val article = current.article ?: return
        val prepared = SummaryInputBuilder.prepare(article)
        if (prepared == null) {
            _uiState.update { it.copy(summary = null, summaryError = SummaryError.EMPTY_CONTENT) }
            return
        }
        val input = SummaryPromptBuilder.build(prepared)
        _uiState.update { it.copy(summaryLoading = true, summary = null, summaryError = null) }
        viewModelScope.launch {
            try {
                when (val result = activeSummarizer.summarize(input)) {
                    is SummaryResult.Success -> _uiState.update {
                        it.copy(summary = result.text, summaryLoading = false)
                    }

                    is SummaryResult.Failure -> _uiState.update {
                        it.copy(summaryError = result.error, summaryLoading = false)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(summaryError = SummaryError.INFERENCE_FAILED, summaryLoading = false)
                }
            }
        }
    }

    private fun load() {
        _uiState.update {
            it.copy(
                isLoading = true,
                article = null,
                notFound = false,
                loadFailed = false,
                summary = null,
                summaryLoading = false,
                summaryError = null,
            )
        }
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
                // Finishing this write matters for retention and read state, even if the
                // user taps back immediately and clears this ViewModel's scope.
                runCatching {
                    withContext(NonCancellable) { markRead(articleId) }
                }
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
