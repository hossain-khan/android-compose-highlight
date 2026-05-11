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
 * Demonstrates every [SyntaxHighlightedCode] boolean flag combination:
 * - `showLineNumbers` × `showLanguageLabel` (2×2)
 * - `showCopyButton` on/off
 */
@Composable
internal fun TogglesSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubSectionHeader("showLineNumbers=false, showLanguageLabel=true (defaults)")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = false,
            showLanguageLabel = true,
        )

        SubSectionHeader("showLineNumbers=true, showLanguageLabel=true")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = true,
            showLanguageLabel = true,
        )

        SubSectionHeader("showLineNumbers=false, showLanguageLabel=false")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = false,
            showLanguageLabel = false,
        )

        SubSectionHeader("showLineNumbers=true, showLanguageLabel=false")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = true,
            showLanguageLabel = false,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("showCopyButton=true (default)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            showCopyButton = true,
        )

        SubSectionHeader("showCopyButton=false")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            showCopyButton = false,
        )
    }
}
