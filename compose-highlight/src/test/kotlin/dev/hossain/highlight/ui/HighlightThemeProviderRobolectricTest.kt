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
    fun `provides active theme to descendants`() {
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
    fun `provides both light and dark themes`() {
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
    fun `selects dark theme when dark mode is true`() {
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
    fun `selects light theme when dark mode is false`() {
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

    @Test
    fun `throws without provider`() {
        // Accessing LocalHighlightTheme without a provider should throw IllegalStateException.
        // The exception propagates through waitForIdle() in the Robolectric test runner.
        val thrown =
            runCatching {
                composeTestRule.setContent {
                    @Suppress("UNUSED_VARIABLE")
                    val theme = LocalHighlightTheme.current
                }
                composeTestRule.waitForIdle()
            }
        assertThat(thrown.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `rememberDraculaLightTheme returns same theme as rememberAlucardLightTheme`() {
        var draculaLightTheme: HighlightTheme? = null
        var alucardLightTheme: HighlightTheme? = null
        composeTestRule.setContent {
            draculaLightTheme = rememberDraculaLightTheme()
            alucardLightTheme = rememberAlucardLightTheme()
        }
        composeTestRule.waitForIdle()
        assertThat(draculaLightTheme).isEqualTo(alucardLightTheme)
    }

    @Test
    fun `rememberAlucardDarkTheme returns same theme as rememberDraculaDarkTheme`() {
        var alucardDarkTheme: HighlightTheme? = null
        var draculaDarkTheme: HighlightTheme? = null
        composeTestRule.setContent {
            alucardDarkTheme = rememberAlucardDarkTheme()
            draculaDarkTheme = rememberDraculaDarkTheme()
        }
        composeTestRule.waitForIdle()
        assertThat(alucardDarkTheme).isEqualTo(draculaDarkTheme)
    }
}
