package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Default values used by [SyntaxHighlightedCode] and [CodeBlockStyle].
 *
 * Expose these constants so callers can build on them without hard-coding magic numbers:
 *
 * ```kotlin
 * // Use the library default text style with only fontSize overridden
 * val myStyle = CodeBlockStyle(
 *     textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontSize = 15.sp),
 * )
 *
 * // Start from Compact but widen the line-number gutter
 * val myCompact = CodeBlockStyle.Compact.copy(
 *     lineNumberWidth = SyntaxHighlightedCodeDefaults.lineNumberWidth + 16.dp,
 * )
 * ```
 */
object SyntaxHighlightedCodeDefaults {
    /**
     * Default [TextStyle] applied to the code text: monospace font, 13 sp size, 20 sp line height.
     *
     * Pass a copy to [CodeBlockStyle.textStyle] to override just the properties you care about:
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
     */
    val codeTextStyle: TextStyle =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )

    /** Default corner radius for the code block container. */
    val shape: Shape = RoundedCornerShape(8.dp)

    /** Default inner padding for the code content area. */
    val padding: PaddingValues = PaddingValues(16.dp)

    /** Default padding for the header row (language label + copy button). */
    val headerPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

    /** Default width reserved for the line-number gutter. */
    val lineNumberWidth: Dp = 32.dp

    /** Default size (width and height) of the copy button. */
    val copyButtonSize: Dp = 32.dp
}
