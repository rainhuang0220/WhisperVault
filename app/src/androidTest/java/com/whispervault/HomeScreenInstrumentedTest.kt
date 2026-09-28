package com.whispervault

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whispervault.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenLaunchesWithBrandAndActions() {
        composeRule.onNodeWithText("WhisperVault").assertIsDisplayed()
        composeRule.onNodeWithText("Select audio files").assertIsDisplayed()
        composeRule.onNodeWithText("Select ggml model").assertIsDisplayed()
    }
}
