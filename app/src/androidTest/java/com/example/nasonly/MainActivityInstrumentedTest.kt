package com.example.nasonly

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Basic instrumented test to verify the app starts correctly.
 * This test ensures that the main activity can be launched and initialized.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_launches_successfully() {
        // Test that the app launches without crashing
        // This is a basic smoke test to ensure the activity initializes
        composeTestRule.waitForIdle()
    }
}