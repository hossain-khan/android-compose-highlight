package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.sample.PYTHON_SNIPPET
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

/**
 * Demonstrates [SyntaxHighlightedCode] typography customization via [CodeBlockStyle.textStyle]:
 * - `textStyle.copy(fontSize = ...)` variants
 * - `textStyle.copy(lineHeight = ...)` variants
 * - `textStyle.copy(fontFamily = ...)` variants
 */
@Composable
internal fun TypographySection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Font size variants
        SubSectionHeader("textStyle - fontSize = 13.sp (default)")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
        )

        SubSectionHeader("textStyle - fontSize = 15.sp")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            style =
                CodeBlockStyle(
                    textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontSize = 15.sp),
                ),
        )

        SubSectionHeader("textStyle - fontSize = 18.sp")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            style =
                CodeBlockStyle(
                    textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontSize = 18.sp),
                ),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Line height variants
        SubSectionHeader("textStyle - lineHeight = 20.sp (default)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
        )

        SubSectionHeader("textStyle - lineHeight = 28.sp (spacious)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style =
                CodeBlockStyle(
                    textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(lineHeight = 28.sp),
                ),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Font family variants
        SubSectionHeader("textStyle - fontFamily = FontFamily.Monospace (default)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
        )

        SubSectionHeader("textStyle - fontFamily = FontFamily.Serif")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style =
                CodeBlockStyle(
                    textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontFamily = FontFamily.Serif),
                ),
        )

        SubSectionHeader("textStyle - fontFamily = FontFamily.SansSerif")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style =
                CodeBlockStyle(
                    textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontFamily = FontFamily.SansSerif),
                ),
        )
    }
}
