@file:OptIn(ExperimentalHighlightApi::class)

package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.StreamingSyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedTextEditor
import dev.hossain.highlight.ui.rememberTomorrowLightTheme
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import kotlinx.coroutines.delay

/**
 * Demonstrates the exact Quick Start snippet from README.md to ensure
 * that it compiles cleanly and runs interactively.
 */
@Composable
internal fun ReadmeQuickStartSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubSectionHeader("README Quick Start")
        Text(
            text = "Runs the exact code snippet published in README.md (Quick Start).",
            style = MaterialTheme.typography.bodySmall,
        )

        var streamedText by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            val sampleTypeScript =
                """
                interface ApiResponse<T> {
                  data: T;
                  status: number;
                  message?: string;
                }
                """.trimIndent()
            while (true) {
                streamedText = ""
                for (char in sampleTypeScript) {
                    delay(35)
                    streamedText += char
                }
                delay(2000)
            }
        }

        HighlightThemeProvider(
            lightHighlightTheme = rememberTomorrowLightTheme(),
            darkHighlightTheme = rememberTomorrowNightTheme(),
        ) {
            // 1. Static Code Block (for docs, snippets, guides)
            SyntaxHighlightedCode(
                code = "data class Config(val enableHighlight: Boolean = true)",
                language = "kotlin",
                showLineNumbers = true,
            )

            // 2. Real-time / LLM Streaming Code Block (zero-flicker progressive rendering)
            StreamingSyntaxHighlightedCode(
                code = streamedText,
                language = "typescript",
                showLineNumbers = true,
            )

            // 3. Interactive Code Editor (debounced inline syntax highlighting)
            var editorValue by remember { mutableStateOf(TextFieldValue("val x = 42")) }
            SyntaxHighlightedTextEditor(
                value = editorValue,
                onValueChange = { editorValue = it },
                language = "kotlin",
            )
        }
    }
}
