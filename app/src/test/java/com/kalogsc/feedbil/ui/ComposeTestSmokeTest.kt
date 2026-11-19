package com.kalogsc.feedbil.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies that Compose UI tests can run on the JVM with Robolectric.
 *
 * SDK 28 is pinned for Compose tests because Espresso's idle strategy calls
 * `InputManager.getInstance()`, which Robolectric does not provide on newer
 * SDK levels. The app's minSdk is 26, so this is a supported device level.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ComposeTestSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersText() {
        composeTestRule.setContent { Text("smoke") }

        composeTestRule.onNodeWithText("smoke").assertIsDisplayed()
    }
}
