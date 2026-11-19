package com.kalogsc.feedbil.ui.articles

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.kalogsc.feedbil.R
import com.kalogsc.feedbil.domain.model.Article
import com.kalogsc.feedbil.domain.model.ArticleListItem
import com.kalogsc.feedbil.ui.appString
import com.kalogsc.feedbil.ui.hasRole
import com.kalogsc.feedbil.ui.hasStateDescription
import com.kalogsc.feedbil.ui.theme.FeedbilTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** SDK 28 is required by Compose's Espresso idle strategy under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ArticleListScreenA11yTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun topBarActionsHaveAccessibleNames() {
        render(ArticleListUiState(isLoading = false, hasFeeds = true))

        composeTestRule.onNodeWithContentDescription(appString(R.string.action_refresh))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(appString(R.string.action_open_feeds))
            .assertIsDisplayed()
    }

    @Test
    fun refreshingProgressHasAccessibleName() {
        render(
            ArticleListUiState(
                isLoading = false,
                hasFeeds = true,
                isRefreshing = true,
                articles = listOf(article(id = 1, read = false)),
            ),
        )

        // The top-bar spinner and the linear bar both announce the same state.
        composeTestRule.onAllNodesWithContentDescription(appString(R.string.articles_refreshing))
            .assertCountEquals(2)
    }

    @Test
    fun articleRowsExposeButtonRoleAndReadState() {
        render(
            ArticleListUiState(
                isLoading = false,
                hasFeeds = true,
                articles = listOf(
                    article(id = 1, read = false),
                    article(id = 2, read = true),
                ),
            ),
        )

        composeTestRule.onNode(
            hasRole(Role.Button) and hasStateDescription(appString(R.string.article_unread)),
        ).assertIsDisplayed()
        composeTestRule.onNode(
            hasRole(Role.Button) and hasStateDescription(appString(R.string.article_read)),
        ).assertIsDisplayed()
    }

    @Test
    fun loadingStateIsLabelled() {
        render(ArticleListUiState(isLoading = true))

        composeTestRule.onNodeWithText(appString(R.string.articles_loading)).assertIsDisplayed()
    }

    @Test
    fun emptyStateIsLabelled() {
        render(ArticleListUiState(isLoading = false, hasFeeds = false))

        composeTestRule.onNodeWithText(appString(R.string.feed_list_empty_title)).assertIsDisplayed()
    }

    @Test
    fun errorStateIsLabelled() {
        render(ArticleListUiState(isLoading = false, loadFailed = true))

        composeTestRule.onNodeWithText(appString(R.string.articles_error_title)).assertIsDisplayed()
    }

    private fun render(state: ArticleListUiState) {
        composeTestRule.setContent {
            FeedbilTheme {
                ArticleListScreen(
                    state = state,
                    onRefresh = {},
                    onDismissFailure = {},
                    onRetryLoad = {},
                    onOpenFeeds = {},
                    onAddFeed = {},
                    onOpenArticle = {},
                )
            }
        }
    }

    private fun article(id: Long, read: Boolean) = ArticleListItem(
        article = Article(
            id = id,
            feedId = 1,
            guid = null,
            link = "https://example.com/$id",
            title = "Title $id",
            description = null,
            contentHtml = null,
            author = null,
            publishedAt = 1_800_000_000_000,
            lastReadAt = if (read) 1_800_000_000_000 else null,
        ),
        feedName = "Feed",
    )
}
