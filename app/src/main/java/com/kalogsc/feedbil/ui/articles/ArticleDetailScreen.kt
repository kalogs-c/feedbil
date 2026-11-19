package com.kalogsc.feedbil.ui.articles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kalogsc.feedbil.R
import com.kalogsc.feedbil.ai.ModelState
import com.kalogsc.feedbil.ai.SummaryError
import com.kalogsc.feedbil.domain.HttpUrlValidator
import com.kalogsc.feedbil.domain.model.Article
import com.kalogsc.feedbil.ui.common.formatDateTime
import com.kalogsc.feedbil.ui.theme.FeedbilTheme

@Composable
fun ArticleDetailRoute(
    viewModel: ArticleDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ArticleDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onSummarize = viewModel::summarize,
        onDownloadModel = viewModel::downloadModel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    state: ArticleDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSummarize: () -> Unit,
    onDownloadModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.article_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val defaultUriHandler = LocalUriHandler.current
        val safeUriHandler = remember(defaultUriHandler) {
            object : UriHandler {
                override fun openUri(uri: String) {
                    runCatching { defaultUriHandler.openUri(uri) }
                }
            }
        }
        CompositionLocalProvider(LocalUriHandler provides safeUriHandler) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when {
                    state.isLoading -> CenteredState {
                        CircularProgressIndicator()
                    }

                    state.notFound -> CenteredState {
                        Text(
                            text = stringResource(R.string.article_detail_not_found),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }

                    state.loadFailed -> CenteredState {
                        Text(
                            text = stringResource(R.string.article_detail_error_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.article_detail_error_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }

                    state.article != null -> ArticleContent(
                        article = state.article,
                        state = state,
                        onSummarize = onSummarize,
                        onDownloadModel = onDownloadModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleContent(
    article: Article,
    state: ArticleDetailUiState,
    onSummarize: () -> Unit,
    onDownloadModel: () -> Unit,
) {
    val blocks = remember(article) { ArticleContentFormatter.format(article) }
    val sourceUrl = remember(article) { HttpUrlValidator.sanitize(article.link) }
    val uriHandler = LocalUriHandler.current
    val imageDescription = stringResource(R.string.article_detail_image)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 600.dp),
        ) {
            Text(
                text = article.title ?: stringResource(R.string.article_untitled),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.article_detail_date, formatDateTime(article.publishedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            article.author?.let { author ->
                Text(
                    text = stringResource(R.string.article_detail_author, author),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            SummarySection(
                state = state,
                onSummarize = onSummarize,
                onDownloadModel = onDownloadModel,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.article_detail_content_source),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            if (blocks.isEmpty()) {
                Text(
                    text = stringResource(R.string.article_detail_no_content),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            blocks.forEach { block ->
                when (block) {
                    is ArticleBlock.Paragraph -> Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    is ArticleBlock.Image -> AsyncImage(
                        model = block.url,
                        contentDescription = block.alt ?: imageDescription,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 1.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (sourceUrl != null) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { uriHandler.openUri(sourceUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_open_original))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummarySection(
    state: ArticleDetailUiState,
    onSummarize: () -> Unit,
    onDownloadModel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
    ) {
        when (val model = state.modelState) {
            is ModelState.Downloading -> {
                Text(
                    text = stringResource(
                        R.string.ai_summary_downloading,
                        (model.bytes * 100 / model.total).toInt().coerceIn(0, 100),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (model.bytes.toFloat() / model.total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            ModelState.NotDownloaded, is ModelState.Failed -> {
                Text(
                    text = stringResource(R.string.ai_summary_download_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ai_summary_download_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onDownloadModel, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.ai_summary_download_button))
                }
                if (model is ModelState.Failed) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.ai_summary_download_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            ModelState.Ready -> {
                state.summary?.let { summary ->
                    Text(
                        text = stringResource(R.string.ai_summary_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(text = summary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                }
                if (state.summaryLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_summary_generating),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                state.summaryError?.let { error ->
                    Text(
                        text = stringResource(summaryErrorMessage(error)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = onSummarize,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.summaryLoading,
                ) {
                    Text(
                        stringResource(
                            if (state.summary == null) R.string.ai_summary_button
                            else R.string.ai_summary_retry,
                        ),
                    )
                }
            }
        }
    }
}

private fun summaryErrorMessage(error: SummaryError): Int = when (error) {
    SummaryError.EMPTY_CONTENT -> R.string.ai_summary_error_empty
    SummaryError.EMPTY_OUTPUT -> R.string.ai_summary_error_empty_output
    SummaryError.INPUT_TOO_LONG -> R.string.ai_summary_error_input
    SummaryError.MODEL_UNAVAILABLE -> R.string.ai_summary_error_model
    SummaryError.INFERENCE_FAILED -> R.string.ai_summary_error_inference
}

@Composable
private fun CenteredState(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = { content() },
    )
}

@Preview(showBackground = true)
@Composable
private fun ArticleDetailScreenPreview() {
    FeedbilTheme {
        ArticleDetailScreen(
            state = ArticleDetailUiState(
                isLoading = false,
                article = Article(
                    id = 1,
                    feedId = 1,
                    guid = null,
                    link = "https://example.com/one",
                    title = "Um título de exemplo",
                    description = "<p>Conteúdo <b>em negrito</b> do feed.</p>",
                    contentHtml = null,
                    author = "Ana",
                    publishedAt = 1_800_000_000_000,
                ),
            ),
            onBack = {},
            onRetry = {},
            onSummarize = {},
            onDownloadModel = {},
        )
    }
}
