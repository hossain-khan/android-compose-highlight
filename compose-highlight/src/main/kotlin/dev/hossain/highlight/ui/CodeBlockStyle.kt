package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual style configuration for [SyntaxHighlightedCode].
 *
 * Use [Default] for a standard code block with rounded corners and comfortable padding.
 * Use [Compact] for tighter padding in space-constrained layouts.
 *
 * Default values for all properties are available via [SyntaxHighlightedCodeDefaults].
 *
 * ## Using presets
 *
 * ```kotlin
 * SyntaxHighlightedCode(code = snippet, language = "json", style = CodeBlockStyle.Default)
 * SyntaxHighlightedCode(code = snippet, language = "json", style = CodeBlockStyle.Compact)
 * ```
 *
 * ## Custom style
 *
 * ```kotlin
 * val myStyle = CodeBlockStyle(
 *     shape           = RoundedCornerShape(4.dp),
 *     padding         = PaddingValues(8.dp),
 *     headerPadding   = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
 *     lineNumberWidth = 40.dp,
 *     copyButtonSize  = 24.dp,
 * )
 * SyntaxHighlightedCode(code = snippet, language = "bash", style = myStyle)
 * ```
 *
 * ## Custom typography
 *
 * Override [textStyle] to change the font, size, or line height of the code text:
 *
 * ```kotlin
 * SyntaxHighlightedCode(
 *     code     = snippet,
 *     language = "kotlin",
 *     style    = CodeBlockStyle(
 *         textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontSize = 15.sp),
 *     ),
 * )
 * ```
 *
 * The [lineNumberColor] defaults to `Color.Unspecified`, which derives the color from the
 * active theme at 40% opacity. Override it to use a fixed color.
 */
data class CodeBlockStyle(
    val shape: Shape = SyntaxHighlightedCodeDefaults.shape,
    val padding: PaddingValues = SyntaxHighlightedCodeDefaults.padding,
    val headerPadding: PaddingValues = SyntaxHighlightedCodeDefaults.headerPadding,
    /** Color.Unspecified means derive from the active theme. */
    val lineNumberColor: Color = Color.Unspecified,
    val lineNumberWidth: Dp = SyntaxHighlightedCodeDefaults.lineNumberWidth,
    val copyButtonSize: Dp = SyntaxHighlightedCodeDefaults.copyButtonSize,
    /**
     * Text style applied to the code content (font family, size, line height, etc.).
     *
     * Defaults to [SyntaxHighlightedCodeDefaults.codeTextStyle] — monospace font, 13 sp, 20 sp
     * line height. The theme's foreground color is applied on top of this style at render time,
     * so [TextStyle.color] set here is overridden by the active [dev.hossain.highlight.engine.HighlightTheme].
     */
    val textStyle: TextStyle = SyntaxHighlightedCodeDefaults.codeTextStyle,
) {
    companion object {
        /** Standard code block with rounded corners and comfortable padding. */
        val Default = CodeBlockStyle()

        /** Compact variant with reduced padding for space-constrained layouts. */
        val Compact =
            CodeBlockStyle(
                padding = PaddingValues(12.dp),
                headerPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            )
    }
}
