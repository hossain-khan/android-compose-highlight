package dev.hossain.highlight.screenshot

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot regression coverage for the two non-happy-path render states of
 * [SyntaxHighlightedCode]: the error fallback (highlight failed, plain code shown) and the
 * loading placeholder (highlight in flight, custom placeholder shown).
 *
 * Both run on the Tomorrow theme so visual diffs reflect the rendering path being exercised,
 * not theme variation.
 *
 * Goldens live under `src/test/snapshots/images/{fallback,placeholder}_*.png`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = "w400dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ErrorAndPlaceholderScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fallback_after_highlight_error() {
        // Inject a null JS response, which triggers HighlightException.JsExecutionFailed
        // inside the engine. The composable falls back to plain `AnnotatedString(code)`
        // wrapped by the Surface and themed background/text colors.
        val snippet = TestSnippets.load("kotlin_sample")
        composeTestRule.captureHighlightedScreenshot(
            name = "fallback_after_highlight_error",
            jsResponse = JsResponse.Failure,
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

    @Test
    fun custom_placeholder_while_loading() {
        // Skip the JS callback entirely so the engine stays mid-flight. The composable
        // shows its placeholder slot for the duration of the capture, demonstrating that
        // a caller-supplied placeholder renders inside the Surface with the line-number
        // gutter (when enabled) preserved for layout stability.
        val snippet = TestSnippets.load("kotlin_sample")
        composeTestRule.captureHighlightedScreenshot(
            name = "custom_placeholder_while_loading",
            jsResponse = JsResponse.NoResponse,
        ) {
            ScreenshotScaffold {
                SyntaxHighlightedCode(
                    code = snippet.code,
                    language = snippet.language,
                    theme = HighlightTheme.tomorrow(),
                    showLineNumbers = true,
                    placeholder = { code ->
                        Text(
                            text = code,
                            modifier = Modifier.padding(0.dp),
                            style =
                                TextStyle(
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                ),
                        )
                    },
                )
            }
        }
    }
}
