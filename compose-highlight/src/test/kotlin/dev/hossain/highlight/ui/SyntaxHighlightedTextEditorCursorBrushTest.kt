package dev.hossain.highlight.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * JVM unit tests for [resolveEditorCursorBrush].
 *
 * Pins the contract that `SyntaxHighlightedTextEditor`'s `cursorBrush` parameter:
 * - `null` (the default) falls back to a [SolidColor] of the theme-derived text color so the
 *   cursor stays visible on both light and dark themes.
 * - A caller-supplied [Brush] passes through verbatim - the editor never overrides an explicit
 *   brush.
 *
 * `BasicTextField`'s built-in default is `SolidColor(Color.Black)`, which is invisible on
 * dark themes. The fallback in this resolver is what protects the editor against that bug -
 * if a future edit removes it, these tests fail.
 */
@OptIn(ExperimentalHighlightApi::class)
class SyntaxHighlightedTextEditorCursorBrushTest {
    @Test
    fun `null cursorBrush resolves to SolidColor of textColor`() {
        // Light theme - text color is dark gray. Cursor must use the same color or it disappears.
        val textColor = Color(0xFF24292E)
        val resolved = resolveEditorCursorBrush(cursorBrush = null, textColor = textColor)
        assertThat(resolved).isEqualTo(SolidColor(textColor))
    }

    @Test
    fun `null cursorBrush on dark theme uses light textColor (fixes BasicTextField default)`() {
        // Dark theme - text color is near-white. BasicTextField's own default
        // (SolidColor(Color.Black)) would be invisible here. The resolver must propagate the
        // theme's text color so the cursor stays visible.
        val darkThemeTextColor = Color(0xFFE0E0E0)
        val resolved = resolveEditorCursorBrush(cursorBrush = null, textColor = darkThemeTextColor)
        assertThat(resolved).isEqualTo(SolidColor(darkThemeTextColor))
    }

    @Test
    fun `explicit cursorBrush is returned unchanged - resolver does not override`() {
        // When the caller passes an explicit Brush, the resolver returns it verbatim - the
        // editor never overrides a deliberate caller choice (e.g. matching the host app's
        // accent color via SolidColor(MaterialTheme.colorScheme.primary)).
        val customBrush: Brush = SolidColor(Color(0xFF6200EE))
        val resolved =
            resolveEditorCursorBrush(
                cursorBrush = customBrush,
                textColor = Color(0xFF24292E), // Would be the fallback - must NOT be used.
            )
        assertThat(resolved).isSameInstanceAs(customBrush)
    }

    @Test
    fun `explicit non-SolidColor brush is returned unchanged`() {
        // A linear-gradient Brush should also pass through. The resolver doesn't inspect the
        // brush type; "non-null" is the only condition.
        val gradient: Brush =
            Brush.horizontalGradient(
                colors = listOf(Color.Red, Color.Blue),
            )
        val resolved =
            resolveEditorCursorBrush(
                cursorBrush = gradient,
                textColor = Color.Black,
            )
        assertThat(resolved).isSameInstanceAs(gradient)
    }

    @Test
    fun `null cursorBrush with Color Unspecified textColor still produces a valid Brush`() {
        // Defensive: the editor's textColor is itself a fallback chain (theme.defaultTextColor
        // ?: style.fallbackTextColor), but if both somehow failed and Color.Unspecified slipped
        // through, the resolver must still hand back a non-null Brush. SolidColor of an
        // unspecified color is technically valid; this test pins that the resolver doesn't
        // crash or null-out in that path.
        val resolved =
            resolveEditorCursorBrush(
                cursorBrush = null,
                textColor = Color.Unspecified,
            )
        assertThat(resolved).isNotNull()
        assertThat(resolved).isInstanceOf(SolidColor::class.java)
    }
}
