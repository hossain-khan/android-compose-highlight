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
        capture(name = "lang_kotlin", snippetName = "kotlin_sample")
    }

    @Test
    fun lang_python() {
        capture(name = "lang_python", snippetName = "python_sample")
    }

    @Test
    fun lang_json() {
        capture(name = "lang_json", snippetName = "json_sample")
    }

    private fun capture(
        name: String,
        snippetName: String,
    ) {
        val snippet = TestSnippets.load(snippetName)
        composeTestRule.captureHighlightedScreenshot(
            name = name,
            fixtureHtml = snippet.highlightedHtml,
        ) {
            ScreenshotScaffold {
                SyntaxHighlightedCode(
                    code = snippet.code,
                    language = snippet.language,
                    theme = HighlightTheme.tomorrow(),
                )
            }
        }
    }
}
