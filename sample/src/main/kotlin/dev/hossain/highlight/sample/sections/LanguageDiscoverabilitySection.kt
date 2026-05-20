package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.engine.AutoHighlightResult
import dev.hossain.highlight.engine.HighlightLanguage
import dev.hossain.highlight.engine.HighlightLanguageInfo
import dev.hossain.highlight.ui.rememberHighlightEngine
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.hossain.highlight.ui.rememberTomorrowTheme
import kotlinx.coroutines.launch

private val PYTHON_SNIPPET =
    """
import json
from pathlib import Path

class Config:
    def __init__(self, path: str):
        self.path = Path(path)

    def load(self) -> dict:
        with open(self.path) as f:
            return json.load(f)

config = Config("settings.json")
print(config.load())
    """.trimIndent()

private val EXTENSION_CHIPS = listOf("kt", "py", "rs", "ts", "sql", "wat", "elm", "nix", "pro")
private val LANGUAGE_CHIPS = listOf("kotlin", "ts", "cr", "py", "glsl", "pgsql")

/**
 * Demonstrates the language discoverability helpers:
 * - [HighlightLanguage.fromExtension] for extension-to-language mapping
 * - [dev.hossain.highlight.engine.HighlightEngine.getLanguage] for language metadata lookup
 * - [dev.hossain.highlight.engine.HighlightEngine.highlightAuto] for auto-detection
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LanguageDiscoverabilitySection(
    isDark: Boolean = true,
    onAutoResultReady: (suspend () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val engine = rememberHighlightEngine()
    val theme = if (isDark) rememberTomorrowNightTheme() else rememberTomorrowTheme()
    val scope = rememberCoroutineScope()

    var extensionInput by remember { mutableStateOf("kt") }
    val resolvedLanguage =
        remember(extensionInput) {
            HighlightLanguage.fromExtension(extensionInput.trim())
        }

    var langInput by remember { mutableStateOf("kotlin") }
    var langInfo by remember { mutableStateOf<HighlightLanguageInfo?>(null) }
    var langNotFound by remember { mutableStateOf(false) }
    var langError by remember { mutableStateOf<String?>(null) }

    var autoResult by remember { mutableStateOf<AutoHighlightResult?>(null) }
    var autoError by remember { mutableStateOf<String?>(null) }
    var autoRunning by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SubSectionHeader("HighlightLanguage.fromExtension()")
        Text(
            text =
                "Resolves a file extension to the Highlight.js language identifier. " +
                    "Tap a chip or type your own extension below.",
            style = TextStyle(fontSize = 13.sp),
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EXTENSION_CHIPS.forEach { ext ->
                FilterChip(
                    selected = extensionInput == ext,
                    onClick = { extensionInput = ext },
                    label = { Text(".$ext") },
                )
            }
        }

        OutlinedTextField(
            value = extensionInput,
            onValueChange = { extensionInput = it },
            label = { Text("File extension (without dot)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            color =
                if (resolvedLanguage != null) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
        ) {
            Text(
                text =
                    if (resolvedLanguage != null) {
                        "fromExtension(\"${extensionInput.trim()}\") = \"$resolvedLanguage\""
                    } else {
                        "fromExtension(\"${extensionInput.trim()}\") = null  (not recognized)"
                    },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style =
                    TextStyle(
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color =
                            if (resolvedLanguage != null) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                    ),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("engine.getLanguage()")
        Text(
            text =
                "Looks up a language by name or alias and returns its display name and " +
                    "registered aliases. Tap a chip or type a name/alias, then tap Look up.",
            style = TextStyle(fontSize = 13.sp),
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LANGUAGE_CHIPS.forEach { lang ->
                FilterChip(
                    selected = langInput == lang,
                    onClick = {
                        langInput = lang
                        langInfo = null
                        langNotFound = false
                        langError = null
                    },
                    label = { Text(lang) },
                )
            }
        }

        OutlinedTextField(
            value = langInput,
            onValueChange = {
                langInput = it
                langInfo = null
                langNotFound = false
                langError = null
            },
            label = { Text("Language name or alias") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                scope.launch {
                    langInfo = null
                    langNotFound = false
                    langError = null
                    engine
                        .getLanguage(langInput.trim())
                        .onSuccess { info ->
                            if (info != null) {
                                langInfo = info
                            } else {
                                langNotFound = true
                            }
                        }.onFailure { error -> langError = error.message ?: "Error" }
                }
            },
        ) {
            Text("Look up")
        }

        langInfo?.let { info ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "name    = \"${info.name}\"",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                    Text(
                        text = "aliases = ${info.aliases}",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }

        if (langNotFound) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = "\"${langInput.trim()}\" is not recognized by the bundled Highlight.js",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style =
                        TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                )
            }
        }

        langError?.let { error ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style =
                        TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("engine.highlightAuto()")
        Text(
            text =
                "Passes a code snippet to Highlight.js without specifying a language. " +
                    "hljs inspects the content and guesses the language.",
            style = TextStyle(fontSize = 13.sp),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = PYTHON_SNIPPET,
                modifier = Modifier.padding(12.dp),
                style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
            )
        }

        Button(
            enabled = !autoRunning,
            onClick = {
                scope.launch {
                    autoRunning = true
                    autoResult = null
                    autoError = null
                    engine
                        .highlightAuto(PYTHON_SNIPPET, theme)
                        .onSuccess { result ->
                            autoResult = result
                            onAutoResultReady?.invoke()
                        }.onFailure { error -> autoError = error.message ?: "Error" }
                    autoRunning = false
                }
            },
        ) {
            Text(if (autoRunning) "Detecting..." else "Auto-detect and highlight")
        }

        autoResult?.let { result ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = theme.backgroundColor,
            ) {
                Text(
                    text = result.annotated,
                    modifier = Modifier.padding(16.dp),
                    style =
                        TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        ),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "detectedLanguage = \"${result.detectedLanguage}\"",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                    Text(
                        text = "spanCount        = ${result.spanCount}",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                    Text(
                        text = "durationMs       = ${result.durationMs} ms",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }

        autoError?.let { error ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style =
                        TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                )
            }
        }
    }
}
