package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.sample.PYTHON_SNIPPET
import dev.hossain.highlight.ui.SyntaxHighlightedCode

/**
 * Demonstrates the `placeholder` slot of [SyntaxHighlightedCode]:
 * - Null placeholder (default) - shows raw code while loading.
 * - Dimmed text placeholder - renders the raw code at reduced opacity while loading.
 */
@Composable
internal fun PlaceholderSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubSectionHeader("Default (no placeholder)")
        Text(
            text = "Raw unstyled code is shown until highlighting completes (default behavior).",
            style = TextStyle(fontSize = 13.sp),
        )
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("Dimmed text placeholder")
        Text(
            text = "A placeholder renders the raw code at reduced opacity while highlighting runs.",
            style = TextStyle(fontSize = 13.sp),
        )
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            placeholder = { rawCode ->
                Text(
                    text = rawCode,
                    modifier = Modifier.padding(12.dp),
                    style =
                        TextStyle(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        ),
                )
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("Italic loading label placeholder")
        Text(
            text = "A simple 'Highlighting...' label shown while the engine warms up.",
            style = TextStyle(fontSize = 13.sp),
        )
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            placeholder = { _ ->
                Text(
                    text = "Highlighting...",
                    modifier = Modifier.padding(12.dp),
                    style =
                        TextStyle(
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        ),
                )
            },
        )
    }
}
