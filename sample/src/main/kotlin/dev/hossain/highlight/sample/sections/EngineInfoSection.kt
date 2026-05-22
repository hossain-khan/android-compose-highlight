package dev.hossain.highlight.sample.sections

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.sample.R
import dev.hossain.highlight.ui.rememberHighlightEngine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Showcases [dev.hossain.highlight.engine.HighlightEngine.highlightJsVersion] and
 * [dev.hossain.highlight.engine.HighlightEngine.supportedLanguages]:
 * - Displays the bundled Highlight.js version string.
 * - Lists every supported language identifier in a searchable, scrollable list.
 * - Tap a language to copy its identifier to the clipboard.
 */
@Composable
internal fun EngineInfoSection() {
    val engine = rememberHighlightEngine()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    var version by remember { mutableStateOf<String?>(null) }
    var languages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var versionError by remember { mutableStateOf<String?>(null) }
    var languagesError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Fire both requests in parallel.
        coroutineScope {
            launch {
                engine
                    .highlightJsVersion()
                    .onSuccess { version = it }
                    .onFailure { versionError = it.message }
            }
            launch {
                engine
                    .supportedLanguages()
                    .onSuccess { languages = it }
                    .onFailure { languagesError = it.message }
            }
        }
        isLoading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ── Version card ──────────────────────────────────────────────────
        SubSectionHeader("Bundled Highlight.js version")
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (isLoading) {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
                Text(
                    text = version?.let { "hljs $it" } ?: "Loading…",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style =
                        TextStyle(
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                        ),
                )
            }
        }

        // ── Error messages ────────────────────────────────────────────────
        versionError?.let {
            Text(
                text = "⚠ Version: $it",
                style = TextStyle(color = MaterialTheme.colorScheme.error, fontSize = 12.sp),
            )
        }
        languagesError?.let {
            Text(
                text = "⚠ Languages: $it",
                style = TextStyle(color = MaterialTheme.colorScheme.error, fontSize = 12.sp),
            )
        }

        // ── Language count badge ──────────────────────────────────────────
        SubSectionHeader("Supported languages")
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text =
                    if (isLoading) {
                        "Loading…"
                    } else if (languages.isEmpty()) {
                        "No languages available"
                    } else {
                        "${languages.size} languages supported"
                    },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    ),
            )
        }

        // ── Searchable language list ──────────────────────────────────────
        if (languages.isNotEmpty()) {
            LanguageSearchAndList(
                languages = languages,
                onCopy = { lang ->
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("language", lang)))
                    }
                },
            )
        }
    }
}

/**
 * Searchable list of language identifiers with copy-to-clipboard on tap.
 */
@Composable
private fun LanguageSearchAndList(
    languages: List<String>,
    onCopy: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var copiedLang by remember { mutableStateOf<String?>(null) }

    if (copiedLang != null) {
        LaunchedEffect(copiedLang) {
            delay(1500)
            copiedLang = null
        }
    }

    val filtered =
        remember(languages, query) {
            if (query.isBlank()) {
                languages
            } else {
                languages.filter { it.contains(query, ignoreCase = true) }
            }
        }

    SubSectionHeader("Tap to copy language identifier")

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Filter ${languages.size} languages…") },
        leadingIcon = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.search_24dp),
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Text(
                    text = "✕",
                    modifier = Modifier.clickable { query = "" }.padding(8.dp),
                    style = TextStyle(fontSize = 16.sp, color = LocalContentColor.current.copy(alpha = 0.6f)),
                )
            }
        },
        singleLine = true,
    )

    OutlinedCard(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        if (filtered.isEmpty() && query.isNotBlank()) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "No languages match \"$query\"",
                    style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 13.sp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(filtered, key = { it }) { lang ->
                    LanguageChip(
                        language = lang,
                        isCopied = lang == copiedLang,
                        onClick = {
                            copiedLang = lang
                            onCopy(lang)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(
    language: String,
    isCopied: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = language,
            modifier = Modifier.weight(1f),
            style =
                TextStyle(
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
        Text(
            text = if (isCopied) "✓" else "⧉",
            modifier = Modifier.width(18.dp),
            style =
                TextStyle(
                    fontSize = 14.sp,
                    color = if (isCopied) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.4f),
                ),
        )
    }
}
