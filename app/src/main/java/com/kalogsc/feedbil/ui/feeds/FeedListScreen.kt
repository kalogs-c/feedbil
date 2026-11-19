package com.kalogsc.feedbil.ui.feeds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalogsc.feedbil.R
import com.kalogsc.feedbil.domain.model.Feed
import com.kalogsc.feedbil.ui.common.formatDateTime
import com.kalogsc.feedbil.ui.theme.FeedbilTheme

@Composable
fun FeedListRoute(
    viewModel: FeedListViewModel,
    onAddFeed: () -> Unit,
    onEditFeed: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FeedListScreen(
        state = state,
        onAddFeed = onAddFeed,
        onEditFeed = onEditFeed,
        onBack = onBack,
        onDeleteFeed = viewModel::delete,
        onRetry = viewModel::retry,
        onDeleteFailureShown = viewModel::deleteFailureShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedListScreen(
    state: FeedListUiState,
    onAddFeed: () -> Unit,
    onEditFeed: (Long) -> Unit,
    onBack: () -> Unit,
    onDeleteFeed: (Long) -> Unit,
    onRetry: () -> Unit,
    onDeleteFailureShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailedMessage = stringResource(R.string.delete_feed_failed)
    var feedPendingDeletion by remember { mutableStateOf<Feed?>(null) }

    LaunchedEffect(state.deleteFailed) {
        if (state.deleteFailed) {
            snackbarHostState.showSnackbar(deleteFailedMessage)
            onDeleteFailureShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feeds_title)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFeed) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.action_add_feed),
                )
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when {
            state.isLoading -> LoadingState(contentModifier)
            state.loadFailed -> ErrorState(onRetry = onRetry, modifier = contentModifier)
            state.feeds.isEmpty() -> EmptyState(contentModifier)
            else -> FeedList(
                feeds = state.feeds,
                onEditFeed = onEditFeed,
                onDeleteRequest = { feedPendingDeletion = it },
                modifier = contentModifier,
            )
        }
    }

    feedPendingDeletion?.let { feed ->
        AlertDialog(
            onDismissRequest = { feedPendingDeletion = null },
            title = { Text(stringResource(R.string.delete_feed_title, feed.name)) },
            text = { Text(stringResource(R.string.delete_feed_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFeed(feed.id)
                        feedPendingDeletion = null
                    },
                ) {
                    Text(stringResource(R.string.action_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { feedPendingDeletion = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun FeedList(
    feeds: List<Feed>,
    onEditFeed: (Long) -> Unit,
    onDeleteRequest: (Feed) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Room for the floating action button above the last row.
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        itemsIndexed(feeds, key = { _, feed -> feed.id }) { index, feed ->
            FeedRow(
                feed = feed,
                onEdit = { onEditFeed(feed.id) },
                onDelete = { onDeleteRequest(feed) },
            )
            if (index < feeds.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun FeedRow(
    feed: Feed,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = feed.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = feed.url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (feed.lastFetchedAt == null) {
                        stringResource(R.string.feed_never_updated)
                    } else {
                        stringResource(
                            R.string.feed_last_updated,
                            formatDateTime(feed.lastFetchedAt),
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete_feed),
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.action_edit_feed),
                onClick = onEdit,
            ),
    )
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.feed_list_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.feed_list_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.feed_list_error_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.feed_list_error_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.action_retry))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedListScreenPreview() {
    FeedbilTheme {
        FeedListScreen(
            state = FeedListUiState(
                isLoading = false,
                feeds = listOf(
                    Feed(id = 1, name = "Kotlin Blog", url = "https://blog.example.com/rss", addedAt = 0),
                    Feed(id = 2, name = "Android Weekly", url = "https://weekly.example.com/feed.xml", addedAt = 0),
                ),
            ),
            onAddFeed = {},
            onEditFeed = {},
            onBack = {},
            onDeleteFeed = {},
            onRetry = {},
            onDeleteFailureShown = {},
        )
    }
}
