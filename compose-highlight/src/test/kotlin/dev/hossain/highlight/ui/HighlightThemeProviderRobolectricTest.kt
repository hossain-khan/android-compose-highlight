package dev.hossain.highlight.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class HighlightThemeProviderRobolectricTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun providesActiveThemeToDescendants() {
        var capturedTheme: HighlightTheme? = null
        composeTestRule.setContent {
            HighlightThemeProvider {
                capturedTheme = LocalHighlightTheme.current
            }
        }
        composeTestRule.waitForIdle()
        assertThat(capturedTheme).isNotNull()
    }

    @Test
    fun providesBothLightAndDarkThemes() {
        var lightTheme: HighlightTheme? = null
        var darkTheme: HighlightTheme? = null
        composeTestRule.setContent {
            HighlightThemeProvider {
                lightTheme = LocalLightHighlightTheme.current
                darkTheme = LocalDarkHighlightTheme.current
            }
        }
        composeTestRule.waitForIdle()
        assertThat(lightTheme).isNotNull()
        assertThat(darkTheme).isNotNull()
        assertThat(lightTheme).isNotEqualTo(darkTheme)
    }

    @Test
    fun selectsDarkThemeWhenDarkModeIsTrue() {
        var activeTheme: HighlightTheme? = null
        var darkTheme: HighlightTheme? = null
        composeTestRule.setContent {
            HighlightThemeProvider(darkTheme = true) {
                activeTheme = LocalHighlightTheme.current
                darkTheme = LocalDarkHighlightTheme.current
            }
        }
        composeTestRule.waitForIdle()
        assertThat(activeTheme).isEqualTo(darkTheme)
    }

    @Test
    fun selectsLightThemeWhenDarkModeIsFalse() {
        var activeTheme: HighlightTheme? = null
        var lightTheme: HighlightTheme? = null
        composeTestRule.setContent {
            HighlightThemeProvider(darkTheme = false) {
                activeTheme = LocalHighlightTheme.current
                lightTheme = LocalLightHighlightTheme.current
            }
        }
        composeTestRule.waitForIdle()
        assertThat(activeTheme).isEqualTo(lightTheme)
    }

    @Test(expected = IllegalStateException::class)
    fun throwsWithoutProvider() {
        composeTestRule.setContent {
            // Accessing LocalHighlightTheme without a provider should throw
            @Suppress("UNUSED_VARIABLE")
            val theme = LocalHighlightTheme.current
        }
        composeTestRule.waitForIdle()
    }
}
