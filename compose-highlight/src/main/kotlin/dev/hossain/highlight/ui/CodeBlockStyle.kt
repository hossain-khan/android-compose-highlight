package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual style configuration for [SyntaxHighlightedCode].
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
