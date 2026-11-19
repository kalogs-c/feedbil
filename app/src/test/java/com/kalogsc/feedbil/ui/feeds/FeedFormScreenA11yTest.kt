package com.kalogsc.feedbil.ui.feeds

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.kalogsc.feedbil.R
import com.kalogsc.feedbil.domain.FeedInputError
import com.kalogsc.feedbil.ui.appString
import com.kalogsc.feedbil.ui.theme.FeedbilTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** SDK 28 is required by Compose's Espresso idle strategy under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FeedFormScreenA11yTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun backActionFieldsAndSaveAreLabelled() {
        render(FeedFormUiState())

        composeTestRule.onNodeWithContentDescription(appString(R.string.action_back))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(appString(R.string.feed_form_name_label)).assertIsDisplayed()
        composeTestRule.onNodeWithText(appString(R.string.feed_form_url_label)).assertIsDisplayed()
        composeTestRule.onNodeWithText(appString(R.string.action_save)).assertIsDisplayed()
    }

    @Test
    fun validationErrorsAreDisplayed() {
        render(
            FeedFormUiState(
                nameError = FeedInputError.BLANK_NAME,
                urlError = FeedInputError.INVALID_URL,
            ),
        )

        composeTestRule.onNodeWithText(appString(R.string.error_name_blank)).assertIsDisplayed()
        composeTestRule.onNodeWithText(appString(R.string.error_url_invalid)).assertIsDisplayed()
    }

    @Test
    fun loadingStateHasAccessibleName() {
        render(FeedFormUiState(isLoadingFeed = true))

        composeTestRule.onNodeWithContentDescription(appString(R.string.feed_form_loading))
            .assertIsDisplayed()
    }

    @Test
    fun checkingStateIsLabelled() {
        render(FeedFormUiState(isSaving = true))

        composeTestRule.onNodeWithText(appString(R.string.feed_form_checking)).assertIsDisplayed()
    }

    private fun render(state: FeedFormUiState) {
        composeTestRule.setContent {
            FeedbilTheme {
                FeedFormScreen(
                    state = state,
                    onNameChange = {},
                    onUrlChange = {},
                    onSave = {},
                    onBack = {},
                )
            }
        }
    }
}
