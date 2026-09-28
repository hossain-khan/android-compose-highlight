@file:OptIn(ExperimentalHighlightApi::class)

package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.sample.R
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.StreamingSyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedTextEditor
import dev.hossain.highlight.ui.rememberTomorrowLightTheme
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import kotlinx.coroutines.delay

private const val README_URL =
    "https://github.com/hossain-khan/android-compose-highlight/blob/main/README.md"

/**
 * Demonstrates the exact Quick Start snippet from README.md to ensure
 * that it compiles cleanly and runs interactively.
 */
@Composable
internal fun ReadmeQuickStartSection() {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "README Quick Start",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text =
                            "Runs the exact code snippet published in README.md for validation purposes " +
                                "to ensure it compiles and functions correctly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = { uriHandler.openUri(README_URL) },
                ) {
                    Text("README.md")
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.open_in_new_24dp),
                        contentDescription = "Open README.md on GitHub",
                        modifier = Modifier.size(16.dp).padding(start = 4.dp),
                    )
                }
            }
        }

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
            SubSectionHeader("1. Static Code Block (SyntaxHighlightedCode)")
            SyntaxHighlightedCode(
                code = "data class Config(val enableHighlight: Boolean = true)",
                language = "kotlin",
                showLineNumbers = true,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 2. Real-time / LLM Streaming Code Block (zero-flicker progressive rendering)
            SubSectionHeader("2. Real-time / LLM Streaming (StreamingSyntaxHighlightedCode)")
            StreamingSyntaxHighlightedCode(
                code = streamedText,
                language = "typescript",
                showLineNumbers = true,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 3. Interactive Code Editor (debounced inline syntax highlighting)
            SubSectionHeader("3. Interactive Code Editor (SyntaxHighlightedTextEditor)")
            var editorValue by remember { mutableStateOf(TextFieldValue("val x = 42")) }
            SyntaxHighlightedTextEditor(
                value = editorValue,
                onValueChange = { editorValue = it },
                language = "kotlin",
            )
        }
    }
}
