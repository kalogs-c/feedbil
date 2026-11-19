package com.kalogsc.feedbil.ui.articles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kalogsc.feedbil.R
import com.kalogsc.feedbil.domain.model.Article
import com.kalogsc.feedbil.ui.appString
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
class ArticleDetailScreenA11yTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun topBarActionsAreLabelled() {
        render(ArticleDetailUiState(isLoading = false, article = article()))

        composeTestRule.onNodeWithContentDescription(appString(R.string.action_back))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(appString(R.string.action_share))
            .assertIsDisplayed()
        composeTestRule.onNode(
            hasStateDescription(appString(R.string.share_menu_collapsed)),
        ).assertIsDisplayed()
    }

    @Test
    fun shareMenuExposesExpandedState() {
        render(ArticleDetailUiState(isLoading = false, article = article()))

        composeTestRule.onNodeWithContentDescription(appString(R.string.action_share)).performClick()

        composeTestRule.onNode(
            hasStateDescription(appString(R.string.share_menu_expanded)),
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(appString(R.string.share_ai_action)).assertIsDisplayed()
    }

    @Test
    fun loadingStateHasAccessibleName() {
        render(ArticleDetailUiState(isLoading = true))

        composeTestRule.onNodeWithContentDescription(appString(R.string.article_detail_loading))
            .assertIsDisplayed()
    }

    @Test
    fun articleContentShowsSourceCaptionAndOriginalLink() {
        render(ArticleDetailUiState(isLoading = false, article = article()))

        composeTestRule.onNodeWithText("Título do artigo").assertIsDisplayed()
        composeTestRule.onNodeWithText(appString(R.string.article_detail_content_source))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Texto do feed.").assertIsDisplayed()
        composeTestRule.onNodeWithText(appString(R.string.action_open_original)).assertIsDisplayed()
    }

    private fun render(state: ArticleDetailUiState) {
        composeTestRule.setContent {
            FeedbilTheme {
                ArticleDetailScreen(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onSummarize = {},
                    onDownloadModel = {},
                    onShareNormal = {},
                    onShareAi = {},
                    onDismissAiShareFallback = {},
                )
            }
        }
    }

    private fun article() = Article(
        id = 1,
        feedId = 1,
        guid = null,
        link = "https://example.com/article",
        title = "Título do artigo",
        description = "<p>Texto do feed.</p>",
        contentHtml = null,
        author = null,
        publishedAt = 1_800_000_000_000,
    )
}
