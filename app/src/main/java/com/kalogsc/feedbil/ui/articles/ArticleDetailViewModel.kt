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
import com.kalogsc.feedbil.domain.HttpUrlValidator
import com.kalogsc.feedbil.domain.ShareContent
import com.kalogsc.feedbil.domain.model.Article
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
    val showAiShareFallback: Boolean = false,
)

/**
 * Loads one stored article and records the time the user opened it. Summary,
 * model-download, and share actions are independent of reading: a failure here
 * never hides the article. Optional AI dependencies keep the screen usable in
 * tests.
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

    /**
     * One-shot share requests. A SharedFlow (no replay) avoids resending the
     * same text after recomposition or after returning from the share sheet.
     */
    private val _shareContent = MutableSharedFlow<ShareContent>(extraBufferCapacity = 1)
    val shareContent: SharedFlow<ShareContent> = _shareContent.asSharedFlow()

    /**
     * Source URL waiting for the summary currently being generated. This lets
     * "share with AI" piggyback on a generation started by the summary button
     * instead of silently doing nothing.
     */
    private var pendingAiShareUrl: String? = null

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
        if (_uiState.value.modelState !is ModelState.Ready) return
        generateSummary()
    }

    /** Shares the title and source URL. This action never runs inference. */
    fun shareNormal() {
        val article = _uiState.value.article ?: return
        val url = HttpUrlValidator.sanitize(article.link) ?: return
        // Choosing normal share cancels a waiting AI share, so inference
        // finishing later cannot open a second chooser.
        pendingAiShareUrl = null
        _uiState.update { it.copy(showAiShareFallback = false) }
        _shareContent.tryEmit(ShareContent.Normal(title = article.title, url = url))
    }

    /**
     * Shares a concise summary and the source URL. An already generated summary
     * is reused; otherwise one is generated if the model is ready. When that is
     * impossible, the UI offers the normal share as a fallback instead.
     */
    fun shareWithAi() {
        val current = _uiState.value
        val article = current.article ?: return
        val url = HttpUrlValidator.sanitize(article.link) ?: return
        if (pendingAiShareUrl != null) return

        current.summary?.let { summary ->
            _shareContent.tryEmit(ShareContent.Ai(summary, url))
            return
        }
        if (current.modelState !is ModelState.Ready || summarizer == null) {
            _uiState.update { it.copy(showAiShareFallback = true) }
            return
        }
        pendingAiShareUrl = url
        // A generation already in flight will flush the pending share when it
        // finishes; otherwise start one here.
        if (!current.summaryLoading) generateSummary()
    }

    fun dismissAiShareFallback() {
        _uiState.update { it.copy(showAiShareFallback = false) }
    }

    /**
     * Runs the shared prepare/prompt/inference path for both the button and AI
     * sharing, so the two actions always produce the same on-device summary.
     */
    private fun generateSummary() {
        val current = _uiState.value
        val activeSummarizer = summarizer ?: return
        if (current.summaryLoading) return
        val article = current.article ?: return
        val prepared = SummaryInputBuilder.prepare(article)
        if (prepared == null) {
            setSummaryFailure(SummaryError.EMPTY_CONTENT)
            return
        }
        val input = SummaryPromptBuilder.build(prepared)
        _uiState.update { it.copy(summaryLoading = true, summary = null, summaryError = null) }
        viewModelScope.launch {
            try {
                when (val result = activeSummarizer.summarize(input)) {
                    is SummaryResult.Success -> setSummarySuccess(result.text)
                    is SummaryResult.Failure -> setSummaryFailure(result.error)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                setSummaryFailure(SummaryError.INFERENCE_FAILED)
            }
        }
    }

    private fun setSummarySuccess(text: String) {
        if (!viewModelScope.isActive) return
        _uiState.update { it.copy(summary = text, summaryLoading = false) }
        pendingAiShareUrl?.let { url ->
            pendingAiShareUrl = null
            _shareContent.tryEmit(ShareContent.Ai(text, url))
        }
    }

    private fun setSummaryFailure(error: SummaryError) {
        val wasAiSharePending = pendingAiShareUrl != null
        pendingAiShareUrl = null
        _uiState.update {
            it.copy(
                summary = null,
                summaryError = error,
                summaryLoading = false,
                showAiShareFallback = wasAiSharePending,
            )
        }
    }

    private fun load() {
        pendingAiShareUrl = null
        _uiState.update {
            it.copy(
                isLoading = true,
                article = null,
                notFound = false,
                loadFailed = false,
                summary = null,
                summaryLoading = false,
                summaryError = null,
                showAiShareFallback = false,
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
