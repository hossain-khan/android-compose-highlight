package dev.hossain.highlight.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot regression coverage for the four bundled built-in themes. Each test renders the
 * same Kotlin code snippet so visual diffs across themes only reflect color-map differences.
 *
 * Goldens live under `src/test/snapshots/images/theme_*.png`.
 *
 * The `qualifiers = "w400dp-h800dp-xhdpi"` Robolectric config pins screen metrics so font
 * hinting is stable across machines; `@GraphicsMode(NATIVE)` is mandatory for Roborazzi.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = "w400dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BuiltInThemesScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theme_tomorrow() {
        captureWithTheme(name = "theme_tomorrow", theme = HighlightTheme.tomorrow())
    }

    @Test
    fun theme_tomorrow_night() {
        captureWithTheme(name = "theme_tomorrow_night", theme = HighlightTheme.tomorrowNight())
    }

    @Test
    fun theme_atom_one_dark() {
        captureWithTheme(name = "theme_atom_one_dark", theme = HighlightTheme.atomOneDark())
    }

    @Test
    fun theme_atom_one_light() {
        captureWithTheme(name = "theme_atom_one_light", theme = HighlightTheme.atomOneLight())
    }

    private fun captureWithTheme(
        name: String,
        theme: HighlightTheme,
    ) {
        val snippet = TestSnippets.load("kotlin_sample")
        composeTestRule.captureHighlightedScreenshot(
            name = name,
            fixtureHtml = snippet.highlightedHtml,
        ) {
            ScreenshotScaffold {
                SyntaxHighlightedCode(
                    code = snippet.code,
                    language = snippet.language,
                    theme = theme,
                )
            }
        }
    }
}

/**
 * Wraps screenshot test content in a Material 3 Surface so the background outside the code
 * block is a deterministic, neutral color instead of transparent (which would render as
 * whatever Skia defaults to and produce noisy diffs).
 */
@androidx.compose.runtime.Composable
internal fun ScreenshotScaffold(content: @androidx.compose.runtime.Composable () -> Unit) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
        ) {
            Box(modifier = Modifier.background(Color.White).padding(16.dp)) {
                content()
            }
        }
    }
}
