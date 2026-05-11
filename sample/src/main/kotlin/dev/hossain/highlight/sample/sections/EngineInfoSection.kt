package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.ui.rememberHighlightEngine

/**
 * Showcases [dev.hossain.highlight.engine.HighlightEngine.highlightJsVersion] and
 * [dev.hossain.highlight.engine.HighlightEngine.supportedLanguages]:
 * - Displays the bundled Highlight.js version string.
 * - Lists every supported language identifier in a scrollable card.
 */
@Composable
internal fun EngineInfoSection() {
    val engine = rememberHighlightEngine()

    var version by remember { mutableStateOf<String?>(null) }
    var languages by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        engine
            .highlightJsVersion()
            .onSuccess { version = it }
            .onFailure { errorMessage = "Version error: ${it.message}" }

        engine
            .supportedLanguages()
            .onSuccess { languages = it }
            .onFailure { errorMessage = (errorMessage?.plus("\n") ?: "") + "Languages error: ${it.message}" }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ── Version card ──────────────────────────────────────────────────
        SubSectionHeader("Bundled Highlight.js version")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(8.dp),
        ) {
            Text(
                text = version?.let { "hljs.versionString = \"$it\"" } ?: "Loading…",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
        }

        errorMessage?.let {
            Text(
                text = it,
                style = TextStyle(color = MaterialTheme.colorScheme.error, fontSize = 12.sp),
            )
        }

        // ── Language count badge ──────────────────────────────────────────
        SubSectionHeader("Supported languages — hljs.listLanguages()")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(8.dp),
        ) {
            Text(
                text = if (languages.isEmpty()) "Loading…" else "${languages.size} languages supported",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium,
                    ),
            )
        }

        // ── Language list ─────────────────────────────────────────────────
        if (languages.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    languages.forEachIndexed { index, lang ->
                        Text(
                            text = "${index + 1}. $lang",
                            style =
                                TextStyle(
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                        )
                    }
                }
            }
        }
    }
}
