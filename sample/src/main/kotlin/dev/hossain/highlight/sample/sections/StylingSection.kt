package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode

/**
 * Demonstrates all [CodeBlockStyle] variants:
 * - [CodeBlockStyle.Default] preset
 * - [CodeBlockStyle.Compact] preset
 * - A custom style with explicit shape, padding, and copy-button size
 */
@Composable
internal fun StylingSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Default preset
        SubSectionHeader("CodeBlockStyle.Default")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style = CodeBlockStyle.Default,
            showLineNumbers = true,
        )

        // Compact preset
        SubSectionHeader("CodeBlockStyle.Compact")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style = CodeBlockStyle.Compact,
        )

        // Fully custom style
        SubSectionHeader("Custom style — sharp corners, wide gutter, small copy button")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style =
                CodeBlockStyle(
                    shape = RoundedCornerShape(4.dp),
                    padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    headerPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    lineNumberColor = Color(0xFF888888),
                    lineNumberWidth = 48.dp,
                    copyButtonSize = 24.dp,
                ),
            showLineNumbers = true,
        )
    }
}
