package com.kalogsc.feedbil.ui.feeds

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kalogsc.feedbil.R
import com.kalogsc.feedbil.domain.model.Feed
import com.kalogsc.feedbil.ui.appString
import com.kalogsc.feedbil.ui.hasRole
import com.kalogsc.feedbil.ui.theme.FeedbilTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** SDK 28 is required by Compose's Espresso idle strategy under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FeedListScreenA11yTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun iconActionsHaveAccessibleNames() {
        render(FeedListUiState(isLoading = false, feeds = listOf(feed())))

        composeTestRule.onNodeWithContentDescription(appString(R.string.action_back))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(appString(R.string.action_add_feed))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(appString(R.string.action_delete_feed))
            .assertIsDisplayed()
    }

    @Test
    fun loadingStateHasAccessibleName() {
        render(FeedListUiState(isLoading = true))

        composeTestRule.onNodeWithContentDescription(appString(R.string.feeds_loading))
            .assertIsDisplayed()
    }

    @Test
    fun feedRowIsAButtonAndEditActionWorks() {
        val edited = mutableListOf<Long>()
        render(FeedListUiState(isLoading = false, feeds = listOf(feed())), onEditFeed = { edited += it })

        // The feed row itself exposes the button role, not just its text.
        composeTestRule.onNode(
            hasRole(Role.Button) and hasText("Kotlin Blog", substring = true),
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Kotlin Blog").performClick()
        assertEquals(listOf(1L), edited)
    }

    private fun render(state: FeedListUiState, onEditFeed: (Long) -> Unit = {}) {
        composeTestRule.setContent {
            FeedbilTheme {
                FeedListScreen(
                    state = state,
                    onAddFeed = {},
                    onEditFeed = onEditFeed,
                    onBack = {},
                    onDeleteFeed = {},
                    onRetry = {},
                    onDeleteFailureShown = {},
                )
            }
        }
    }

    private fun feed() = Feed(
        id = 1,
        name = "Kotlin Blog",
        url = "https://example.com/rss",
        addedAt = 0,
    )
}
