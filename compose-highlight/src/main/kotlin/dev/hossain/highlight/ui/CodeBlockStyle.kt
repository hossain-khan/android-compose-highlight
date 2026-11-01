package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.engine.HighlightTheme

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
 *
 * For custom styles constructed inline in a composable, wrap them in `remember` to avoid
 * unnecessary recompositions:
 * ```kotlin
 * val myStyle = remember { CodeBlockStyle(padding = PaddingValues(8.dp)) }
 * ```
 */
@Stable
public data class CodeBlockStyle(
    /** Shape applied to the outer container of the code block. */
    public val shape: Shape = SyntaxHighlightedCodeDefaults.shape,
    /** Inner padding between the container edge and the code content area. */
    public val padding: PaddingValues = SyntaxHighlightedCodeDefaults.padding,
    /** Padding for the header row (language label + copy button). */
    public val headerPadding: PaddingValues = SyntaxHighlightedCodeDefaults.headerPadding,
    /**
     * Color of the line number gutter text.
     *
     * Defaults to [Color.Unspecified], which derives the color from the active theme at 40% opacity.
     * Override to use a fixed color.
     */
    public val lineNumberColor: Color = Color.Unspecified,
    /**
     * Minimum width reserved for the line number gutter.
     *
     * The gutter automatically expands beyond this value for code blocks with 1,000+ lines
     * or larger typography to fit multi-digit line numbers without wrapping.
     */
    public val lineNumberWidth: Dp = SyntaxHighlightedCodeDefaults.lineNumberWidth,
    /** Size (width and height) of the copy-to-clipboard button icon. */
    public val copyButtonSize: Dp = SyntaxHighlightedCodeDefaults.copyButtonSize,
    /**
     * Text style applied to the code content (font family, size, line height, etc.).
     *
     * Defaults to [SyntaxHighlightedCodeDefaults.codeTextStyle] - monospace font, 13 sp, 20 sp
     * line height. The theme's foreground color is applied on top of this style at render time,
     * so [TextStyle.color] set here is overridden by the active [HighlightTheme].
     */
    public val textStyle: TextStyle = SyntaxHighlightedCodeDefaults.codeTextStyle,
    /**
     * Background color used when the active theme's CSS has no `.hljs { background: ... }` rule.
     *
     * Applies when [HighlightTheme.backgroundColor] is [Color.Unspecified] - for example when
     * using a custom theme CSS that omits the base `.hljs` rule. Defaults to
     * [SyntaxHighlightedCodeDefaults.fallbackBackgroundColor].
     */
    public val fallbackBackgroundColor: Color = SyntaxHighlightedCodeDefaults.fallbackBackgroundColor,
    /**
     * Text color used when the active theme's CSS has no `.hljs { color: ... }` rule.
     *
     * Applies when [HighlightTheme.defaultTextColor] is [Color.Unspecified] - for example when
     * using a custom theme CSS that omits the base `.hljs` rule. Defaults to
     * [SyntaxHighlightedCodeDefaults.fallbackTextColor].
     */
    public val fallbackTextColor: Color = SyntaxHighlightedCodeDefaults.fallbackTextColor,
) {
    public companion object {
        /** Standard code block with rounded corners and comfortable padding. */
        public val Default: CodeBlockStyle = CodeBlockStyle()

        /** Compact variant with reduced padding for space-constrained layouts. */
        public val Compact: CodeBlockStyle =
            CodeBlockStyle(
                padding = PaddingValues(12.dp),
                headerPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            )
    }
}
