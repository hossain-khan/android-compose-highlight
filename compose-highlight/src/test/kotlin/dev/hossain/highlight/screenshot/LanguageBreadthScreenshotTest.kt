package dev.hossain.highlight.screenshot

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot regression coverage for token-class breadth across languages. Each test pins the
 * Tomorrow theme so visual diffs reflect the language tokenizer's output, not theme variation.
 *
 * Goldens live under `src/test/snapshots/images/lang_*.png`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = "w400dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LanguageBreadthScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun lang_kotlin() {
        capture(
            name = "lang_kotlin",
            code = TestSnippets.KOTLIN_SAMPLE,
            language = "kotlin",
            fixtureHtml = TestHljsFixtures.KOTLIN_SAMPLE_HTML,
        )
    }

    @Test
    fun lang_python() {
        capture(
            name = "lang_python",
            code = TestSnippets.PYTHON_SAMPLE,
            language = "python",
            fixtureHtml = TestHljsFixtures.PYTHON_SAMPLE_HTML,
        )
    }

    @Test
    fun lang_json() {
        capture(
            name = "lang_json",
            code = TestSnippets.JSON_SAMPLE,
            language = "json",
            fixtureHtml = TestHljsFixtures.JSON_SAMPLE_HTML,
        )
    }

    private fun capture(
        name: String,
        code: String,
        language: String,
        fixtureHtml: String,
    ) {
        composeTestRule.captureHighlightedScreenshot(
            name = name,
            fixtureHtml = fixtureHtml,
        ) {
            ScreenshotScaffold {
                SyntaxHighlightedCode(
                    code = code,
                    language = language,
                    theme = HighlightTheme.tomorrow(),
                )
            }
        }
    }
}
