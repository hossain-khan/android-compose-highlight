package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual style configuration for [SyntaxHighlightedCode].
 *
 * Use [Default] for a standard code block with rounded corners and comfortable padding.
 * Use [Compact] for tighter padding in space-constrained layouts.
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
 * The [lineNumberColor] defaults to `Color.Unspecified`, which derives the color from the
 * active theme at 40% opacity. Override it to use a fixed color.
 */
data class CodeBlockStyle(
    val shape: Shape = RoundedCornerShape(8.dp),
    val padding: PaddingValues = PaddingValues(16.dp),
    val headerPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    /** Color.Unspecified means derive from the active theme. */
    val lineNumberColor: Color = Color.Unspecified,
    val lineNumberWidth: Dp = 32.dp,
    val copyButtonSize: Dp = 32.dp,
) {
    companion object {
        val Default = CodeBlockStyle()
        val Compact =
            CodeBlockStyle(
                padding = PaddingValues(12.dp),
                headerPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            )
    }
}
