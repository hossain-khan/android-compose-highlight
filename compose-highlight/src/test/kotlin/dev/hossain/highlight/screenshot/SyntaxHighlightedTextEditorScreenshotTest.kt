package dev.hossain.highlight.screenshot

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.SyntaxHighlightedTextEditor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot regression coverage for [SyntaxHighlightedTextEditor]. Closes the parity gap
 * called out in issue #230 - the editor was the only public composable in the library
 * without screenshot coverage.
 *
 * The matrix is intentionally small (4 goldens). Theme breadth is already covered by
 * [BuiltInThemesScreenshotTest] for the read-only viewer; the colour map is shared between
 * the two composables so we don't need to re-cover all four themes here. What this file
 * pins down is editor-specific rendering: the `Surface` chrome, the `shape` + `contentPadding`
 * interaction, and the unique "plain text while highlight is loading" render state that the
 * read-only viewer doesn't have (the viewer has its own `placeholder` slot for that purpose).
 *
 * Goldens live under `src/test/snapshots/images/editor_*.png`.
 */
@OptIn(ExperimentalHighlightApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = "w400dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SyntaxHighlightedTextEditorScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun editor_default_tomorrow() {
        // Default Surface chrome: themed background, monospace text, no padding, no shape
        // rounding. The editor's most stripped-down look.
        val snippet = TestSnippets.load("kotlin_sample")
        composeTestRule.captureHighlightedScreenshot(
            name = "editor_default_tomorrow",
            fixtureHtml = snippet.highlightedHtml,
            testTag = SYNTAX_HIGHLIGHTED_TEXT_EDITOR_TEST_TAG,
        ) {
            ScreenshotScaffold {
                SyntaxHighlightedTextEditor(
                    value = TextFieldValue(snippet.code),
                    onValueChange = {},
                    language = snippet.language,
                    theme = HighlightTheme.tomorrow(),
                    // Zero debounce: the helper's LaunchedEffect would otherwise wait 150ms
                    // before reaching the engine, blocking the screenshot helper's drain loop.
                    debounceMs = 0L,
                )
            }
        }
    }

    @Test
    fun editor_default_tomorrow_night() {
        // Same as above but with the dark theme. Verifies the editor's `Surface` chrome
        // picks up the theme's background and text colours correctly. Together with
        // editor_default_tomorrow this exercises both light + dark on the same code.
        val snippet = TestSnippets.load("kotlin_sample")
        composeTestRule.captureHighlightedScreenshot(
            name = "editor_default_tomorrow_night",
            fixtureHtml = snippet.highlightedHtml,
            testTag = SYNTAX_HIGHLIGHTED_TEXT_EDITOR_TEST_TAG,
        ) {
            ScreenshotScaffold {
                SyntaxHighlightedTextEditor(
                    value = TextFieldValue(snippet.code),
                    onValueChange = {},
                    language = snippet.language,
                    theme = HighlightTheme.tomorrowNight(),
                    debounceMs = 0L,
                )
            }
        }
    }

    @Test
    fun editor_with_rounded_shape_and_padding() {
        // Pins down the documented `shape` + `contentPadding` interaction (see the editor's
        // KDoc note: padding via modifier shrinks the Surface and leaves a gap; padding via
        // contentPadding fills the bordered area). The shape is wrapped in `remember` so a
        // fresh RoundedCornerShape isn't allocated on each recomposition (per the editor's
        // KDoc warning about Shape stability).
        val snippet = TestSnippets.load("kotlin_sample")
        composeTestRule.captureHighlightedScreenshot(
            name = "editor_with_rounded_shape_and_padding",
            fixtureHtml = snippet.highlightedHtml,
            testTag = SYNTAX_HIGHLIGHTED_TEXT_EDITOR_TEST_TAG,
        ) {
            ScreenshotScaffold {
                val roundedShape = remember { RoundedCornerShape(8.dp) }
                SyntaxHighlightedTextEditor(
                    value = TextFieldValue(snippet.code),
                    onValueChange = {},
                    language = snippet.language,
                    theme = HighlightTheme.tomorrow(),
                    shape = roundedShape,
                    contentPadding = PaddingValues(12.dp),
                    debounceMs = 0L,
                )
            }
        }
    }

    @Test
    fun editor_during_debounce_window() {
        // Captures the editor BEFORE the JS callback fires - the helper's LaunchedEffect is
        // suspended, no snapshot exists, and the editor renders the raw code as plain
        // monospace text inside the Surface (no spans, no fade, no chrome change). This is
        // the editor's unique "loading" render state. Uses JsResponse.NoResponse to keep
        // the callback from firing - same pattern as ErrorAndPlaceholderScreenshotTest's
        // custom_placeholder_while_loading test.
        val snippet = TestSnippets.load("kotlin_sample")
        composeTestRule.captureHighlightedScreenshot(
            name = "editor_during_debounce_window",
            jsResponse = JsResponse.NoResponse,
            testTag = SYNTAX_HIGHLIGHTED_TEXT_EDITOR_TEST_TAG,
        ) {
            ScreenshotScaffold {
                SyntaxHighlightedTextEditor(
                    value = TextFieldValue(snippet.code),
                    onValueChange = {},
                    language = snippet.language,
                    theme = HighlightTheme.tomorrow(),
                    // Zero debounce: the helper's LaunchedEffect would otherwise wait 150ms
                    // before reaching the engine, blocking the screenshot helper's drain loop.
                    debounceMs = 0L,
                )
            }
        }
    }
}
