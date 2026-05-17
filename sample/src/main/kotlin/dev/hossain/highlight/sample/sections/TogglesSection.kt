package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.sample.PYTHON_SNIPPET
import dev.hossain.highlight.ui.SyntaxHighlightedCode

/**
 * Demonstrates every [SyntaxHighlightedCode] visibility option:
 * - `showLineNumbers` × `languageLabelContent` (2×2)
 * - `copyButtonContent` on/off
 */
@Composable
internal fun TogglesSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubSectionHeader("showLineNumbers=false, languageLabelContent=default")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = false,
        )

        SubSectionHeader("showLineNumbers=true, languageLabelContent=default")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = true,
        )

        SubSectionHeader("showLineNumbers=false, languageLabelContent=null (hidden)")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = false,
            languageLabelContent = null,
        )

        SubSectionHeader("showLineNumbers=true, languageLabelContent=null (hidden)")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = true,
            languageLabelContent = null,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("copyButtonContent=default")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
        )

        SubSectionHeader("copyButtonContent=null (hidden)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            copyButtonContent = null,
        )
    }
}
