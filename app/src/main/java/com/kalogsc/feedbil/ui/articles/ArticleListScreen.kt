package com.kalogsc.feedbil.ui.articles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalogsc.feedbil.R
import com.kalogsc.feedbil.data.FeedRefreshError
import com.kalogsc.feedbil.data.FeedRefreshFailure
import com.kalogsc.feedbil.data.remote.FeedFetchResult
import com.kalogsc.feedbil.domain.model.Article
import com.kalogsc.feedbil.domain.model.ArticleListItem
import com.kalogsc.feedbil.ui.common.formatDateTime
import com.kalogsc.feedbil.ui.theme.FeedbilTheme

@Composable
fun ArticleListRoute(
    viewModel: ArticleListViewModel,
    onOpenFeeds: () -> Unit,
    onAddFeed: () -> Unit,
    onOpenArticle: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ArticleListScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onDismissFailure = viewModel::dismissRefreshFailure,
        onRetryLoad = viewModel::retryLoad,
        onOpenFeeds = onOpenFeeds,
        onAddFeed = onAddFeed,
        onOpenArticle = onOpenArticle,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    state: ArticleListUiState,
    onRefresh: () -> Unit,
    onDismissFailure: () -> Unit,
    onRetryLoad: () -> Unit,
    onOpenFeeds: () -> Unit,
    onAddFeed: () -> Unit,
    onOpenArticle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFailureDetails by remember { mutableStateOf(false) }
    val refreshingLabel = stringResource(R.string.articles_refreshing)
    val failureMessage = when {
        state.refreshFailed -> stringResource(R.string.refresh_failed_generic)
        state.failures.isNotEmpty() -> pluralStringResource(
            R.plurals.refresh_failed_summary,
            state.failures.size,
            state.failures.size,
        )

        else -> null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.articles_title)) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.isRefreshing) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .semantics { contentDescription = refreshingLabel },
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.action_refresh),
                            )
                        }
                    }
                    IconButton(onClick = onOpenFeeds) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.action_open_feeds),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (state.isRefreshing && state.articles.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = refreshingLabel },
                )
            }
            failureMessage?.let { message ->
                FailureBanner(
                    message = message,
                    hasDetails = state.failures.isNotEmpty(),
                    onDetails = { showFailureDetails = true },
                    onDismiss = onDismissFailure,
                )
            }

            when {
                state.isLoading -> LoadingState(stringResource(R.string.articles_loading))
                state.loadFailed -> ErrorState(onRetry = onRetryLoad)
                !state.hasFeeds -> EmptyState(
                    title = stringResource(R.string.feed_list_empty_title),
                    description = stringResource(R.string.feed_list_empty_description),
                    actionLabel = stringResource(R.string.action_add_feed),
                    onAction = onAddFeed,
                )

                state.articles.isEmpty() && state.isRefreshing ->
                    LoadingState(stringResource(R.string.articles_refreshing))

                state.articles.isEmpty() && failureMessage != null -> ErrorState(onRetry = onRefresh)
                state.articles.isEmpty() -> EmptyState(
                    title = stringResource(R.string.articles_empty_title),
                    description = stringResource(R.string.articles_empty_description),
                    actionLabel = stringResource(R.string.action_refresh),
                    onAction = onRefresh,
                )

                else -> ArticleList(articles = state.articles, onOpenArticle = onOpenArticle)
            }
        }
    }

    if (showFailureDetails && state.failures.isNotEmpty()) {
        FailureDetailsDialog(
            failures = state.failures,
            onDismiss = { showFailureDetails = false },
            onRetry = {
                showFailureDetails = false
                onRefresh()
            },
        )
    }
}

@Composable
private fun FailureBanner(
    message: String,
    hasDetails: Boolean,
    onDetails: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (hasDetails) {
                TextButton(onClick = onDetails) {
                    Text(stringResource(R.string.action_details))
                }
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    }
}

@Composable
private fun FailureDetailsDialog(
    failures: List<FeedRefreshFailure>,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.refresh_failures_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                failures.forEach { failure ->
                    Text(
                        text = stringResource(
                            R.string.refresh_failure_item,
                            failure.feedName,
                            failureReasonText(failure.reason),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.action_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@Composable
private fun failureReasonText(error: FeedRefreshError): String = when (error) {
    is FeedRefreshError.Fetch -> fetchResultText(error.result)
    FeedRefreshError.Storage -> stringResource(R.string.error_articles_storage)
    FeedRefreshError.Unknown -> stringResource(R.string.error_unknown)
}

@Composable
private fun fetchResultText(result: FeedFetchResult): String = when (result) {
    is FeedFetchResult.HttpError -> stringResource(R.string.error_http, result.code)
    FeedFetchResult.InvalidUrl -> stringResource(R.string.error_url_invalid)
    FeedFetchResult.NotAFeed -> stringResource(R.string.error_not_a_feed)
    FeedFetchResult.Network -> stringResource(R.string.error_network)
    FeedFetchResult.Timeout -> stringResource(R.string.error_timeout)
    FeedFetchResult.Unknown -> stringResource(R.string.error_unknown)
    is FeedFetchResult.Success -> stringResource(R.string.error_unknown)
}

@Composable
private fun ArticleList(
    articles: List<ArticleListItem>,
    onOpenArticle: (Long) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items = articles, key = { _, item -> item.article.id }) { index, item ->
            ArticleRow(item = item, onClick = { onOpenArticle(item.article.id) })
            if (index < articles.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ArticleRow(item: ArticleListItem, onClick: () -> Unit) {
    val unread = !item.article.isRead
    val readStateLabel = stringResource(
        if (unread) R.string.article_unread else R.string.article_read,
    )

    ListItem(
        modifier = Modifier
            .clickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.action_open_article),
            )
            .semantics { stateDescription = readStateLabel },
        leadingContent = {
            Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                if (unread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        },
        headlineContent = {
            Text(
                text = item.article.title ?: stringResource(R.string.article_untitled),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = stringResource(
                    R.string.article_list_meta,
                    item.feedName,
                    formatDateTime(item.article.publishedAt),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun LoadingState(label: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyState(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    EmptyState(
        title = stringResource(R.string.articles_error_title),
        description = stringResource(R.string.articles_error_description),
        actionLabel = stringResource(R.string.action_retry),
        onAction = onRetry,
    )
}

@Preview(showBackground = true)
@Composable
private fun ArticleListScreenPreview() {
    FeedbilTheme {
        ArticleListScreen(
            state = ArticleListUiState(
                isLoading = false,
                hasFeeds = true,
                articles = listOf(
                    ArticleListItem(
                        article = Article(
                            id = 1,
                            feedId = 1,
                            guid = null,
                            link = "https://example.com/one",
                            title = "First post",
                            description = null,
                            contentHtml = null,
                            author = null,
                            publishedAt = 1_800_000_000_000,
                        ),
                        feedName = "Kotlin Blog",
                    ),
                    ArticleListItem(
                        article = Article(
                            id = 2,
                            feedId = 2,
                            guid = null,
                            link = "https://example.com/two",
                            title = null,
                            description = null,
                            contentHtml = null,
                            author = null,
                            publishedAt = 1_799_900_000_000,
                            lastReadAt = 1_799_950_000_000,
                        ),
                        feedName = "Android Weekly",
                    ),
                ),
            ),
            onRefresh = {},
            onDismissFailure = {},
            onRetryLoad = {},
            onOpenFeeds = {},
            onAddFeed = {},
            onOpenArticle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleListEmptyPreview() {
    FeedbilTheme {
        ArticleListScreen(
            state = ArticleListUiState(isLoading = false),
            onRefresh = {},
            onDismissFailure = {},
            onRetryLoad = {},
            onOpenFeeds = {},
            onAddFeed = {},
            onOpenArticle = {},
        )
    }
}
