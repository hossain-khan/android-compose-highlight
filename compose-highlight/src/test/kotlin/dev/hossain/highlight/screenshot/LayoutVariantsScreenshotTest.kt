package dev.hossain.highlight.screenshot

import androidx.compose.runtime.Composable
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
 * Screenshot regression coverage for the layout variants supported by [SyntaxHighlightedCode].
 * Each test renders the same Kotlin snippet with the Tomorrow theme so visual diffs only
 * reflect the layout-knob being exercised: line numbers, header visibility, custom placeholder.
 *
 * Goldens live under `src/test/snapshots/images/layout_*.png`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = "w400dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayoutVariantsScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun layout_default() {
        capture(name = "layout_default") {
            SyntaxHighlightedCode(
                code = TestSnippets.KOTLIN_SAMPLE,
                language = "kotlin",
                theme = HighlightTheme.tomorrow(),
            )
        }
    }

    @Test
    fun layout_with_line_numbers() {
        capture(name = "layout_with_line_numbers") {
            SyntaxHighlightedCode(
                code = TestSnippets.KOTLIN_SAMPLE,
                language = "kotlin",
                theme = HighlightTheme.tomorrow(),
                showLineNumbers = true,
            )
        }
    }

    @Test
    fun layout_headerless() {
        // No language label, no copy button - useful for inline code blocks where the header
        // would be visual noise.
        capture(name = "layout_headerless") {
            SyntaxHighlightedCode(
                code = TestSnippets.KOTLIN_SAMPLE,
                language = "kotlin",
                theme = HighlightTheme.tomorrow(),
                languageLabel = null,
                copyButton = null,
            )
        }
    }

    @Test
    fun layout_language_label_only() {
        // Language label visible, copy button hidden.
        capture(name = "layout_language_label_only") {
            SyntaxHighlightedCode(
                code = TestSnippets.KOTLIN_SAMPLE,
                language = "kotlin",
                theme = HighlightTheme.tomorrow(),
                copyButton = null,
            )
        }
    }

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.captureHighlightedScreenshot(
            name = name,
            fixtureHtml = TestHljsFixtures.KOTLIN_SAMPLE_HTML,
        ) {
            ScreenshotScaffold { content() }
        }
    }
}
