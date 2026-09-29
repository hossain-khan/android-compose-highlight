package dev.hossain.highlight.sample.sections

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.engine.AutoHighlightResult
import dev.hossain.highlight.engine.HighlightLanguage
import dev.hossain.highlight.engine.HighlightLanguageInfo
import dev.hossain.highlight.sample.R
import dev.hossain.highlight.ui.LocalHighlightTheme
import dev.hossain.highlight.ui.rememberHighlightEngine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EXTENSION_CHIPS = listOf("kt", "py", "rs", "ts", "sql", "wat", "elm", "nix", "pro")
private val LANGUAGE_CHIPS = listOf("kotlin", "ts", "cr", "py", "glsl", "pgsql")

private val SAMPLE_PRESETS =
    listOf(
        "Python" to
            """
            import json
            from pathlib import Path

            class Config:
                def __init__(self, path: str):
                    self.path = Path(path)

                def load(self) -> dict:
                    with open(self.path) as f:
                        return json.load(f)
            """.trimIndent(),
        "JSON" to
            """
            {
              "name": "compose-highlight",
              "version": "0.39.0",
              "bundledLanguages": 190,
              "isAwesome": true
            }
            """.trimIndent(),
        "SQL" to
            """
            SELECT u.id, u.name, count(o.id) AS order_count
            FROM users u
            LEFT JOIN orders o ON u.id = o.user_id
            WHERE u.active = true
            GROUP BY u.id, u.name
            ORDER BY order_count DESC;
            """.trimIndent(),
        "Kotlin" to
            """
            data class User(val id: Long, val name: String)

            fun formatGreeting(user: User): String {
                return "Hello, ${'$'}{user.name} (id: ${'$'}{user.id})!"
            }
            """.trimIndent(),
    )

/**
 * Unified language hub combining:
 * 1. Bundled Highlight.js engine information and searchable supported languages.
 * 2. Compile-time [HighlightLanguage] static catalog & file extension resolver.
 * 3. Runtime [dev.hossain.highlight.engine.HighlightEngine.getLanguage] metadata lookup.
 * 4. Interactive [dev.hossain.highlight.engine.HighlightEngine.highlightAuto] auto-detection playground.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LanguageCatalogSection(modifier: Modifier = Modifier) {
    val engine = rememberHighlightEngine()
    val theme = LocalHighlightTheme.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    // Engine Info State
    var version by remember { mutableStateOf<String?>(null) }
    var supportedLanguages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingEngineInfo by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                engine.highlightJsVersion().onSuccess { version = it }
            }
            launch {
                engine.supportedLanguages().onSuccess { supportedLanguages = it }
            }
        }
        isLoadingEngineInfo = false
    }

    // Static catalog input
    var catalogInput by remember { mutableStateOf("kt") }
    val catalogIsSupported = remember(catalogInput) { HighlightLanguage.isSupported(catalogInput) }
    val catalogCanonicalName = remember(catalogInput) { HighlightLanguage.canonicalName(catalogInput) }

    // Extension input
    var extensionInput by remember { mutableStateOf("kt") }
    val normalizedExtension = extensionInput.trim().trimStart('.')
    val resolvedFromExtension = remember(normalizedExtension) { HighlightLanguage.fromExtension(normalizedExtension) }

    // getLanguage input
    var lookupLangInput by remember { mutableStateOf("kotlin") }
    var lookupLangInfo by remember { mutableStateOf<HighlightLanguageInfo?>(null) }
    var lookupNotFound by remember { mutableStateOf(false) }

    // highlightAuto playground state
    var autoCodeInput by remember { mutableStateOf(SAMPLE_PRESETS[0].second) }
    var autoResult by remember { mutableStateOf<AutoHighlightResult?>(null) }
    var autoError by remember { mutableStateOf<String?>(null) }
    var autoRunning by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text =
                "Explore the bundled language ecosystem: compile-time catalog queries, " +
                    "runtime engine capabilities, file extension resolution, and automatic language detection.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── Bundled Engine Overview ──────────────────────────────────────────
        SubSectionHeader("Engine & Bundled Version")
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isLoadingEngineInfo) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = version?.let { "Highlight.js $it" } ?: "Loading engine…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text =
                            if (supportedLanguages.isNotEmpty()) {
                                "${supportedLanguages.size} runtime languages"
                            } else {
                                "${HighlightLanguage.all.size} compile-time languages"
                            },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── Interactive highlightAuto Playground ────────────────────────────
        SubSectionHeader("Interactive highlightAuto() Playground")
        Text(
            text =
                "Paste or type arbitrary code without specifying a language. " +
                    "Highlight.js analyzes the syntax structure and detects the most probable language.",
            style = TextStyle(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "Quick Presets:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SAMPLE_PRESETS.forEach { (label, code) ->
                FilterChip(
                    selected = autoCodeInput == code,
                    onClick = {
                        autoCodeInput = code
                        autoResult = null
                        autoError = null
                    },
                    label = { Text(label) },
                )
            }
        }

        OutlinedTextField(
            value = autoCodeInput,
            onValueChange = {
                autoCodeInput = it
                autoResult = null
                autoError = null
            },
            label = { Text("Code to auto-detect") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 8,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
        )

        Button(
            enabled = !autoRunning && autoCodeInput.isNotBlank(),
            onClick = {
                scope.launch {
                    autoRunning = true
                    autoResult = null
                    autoError = null
                    try {
                        engine
                            .highlightAuto(autoCodeInput, theme)
                            .onSuccess { result -> autoResult = result }
                            .onFailure { error -> autoError = error.message ?: "Detection failed" }
                    } finally {
                        autoRunning = false
                    }
                }
            },
        ) {
            Text(if (autoRunning) "Detecting..." else "Auto-detect and highlight")
        }

        autoResult?.let { result ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = theme.backgroundColor.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = result.annotated,
                    modifier = Modifier.padding(16.dp),
                    style =
                        TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = theme.defaultTextColor.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSurface,
                        ),
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "Detected Language: \"${result.detectedLanguage}\"",
                        style =
                            TextStyle(
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    )
                    Text(
                        text = "Tokens (spanCount): ${result.spanCount}   |   Duration: ${result.durationMs}ms",
                        style =
                            TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    )
                }
            }
        }

        autoError?.let { err ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = "Error: $err",
                    modifier = Modifier.padding(12.dp),
                    style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer),
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── HighlightLanguage Static Catalog & Normalization ────────────────
        SubSectionHeader("HighlightLanguage (Compile-Time Catalog)")
        Text(
            text =
                "Synchronous catalog of ${HighlightLanguage.all.size} languages " +
                    "with instant alias normalization and zero bridge overhead.",
            style = TextStyle(fontSize = 13.sp),
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HighlightLanguage.primary.take(10).forEach { lang ->
                FilterChip(
                    selected = catalogInput == lang,
                    onClick = { catalogInput = lang },
                    label = { Text(lang) },
                )
            }
        }

        OutlinedTextField(
            value = catalogInput,
            onValueChange = { catalogInput = it },
            label = { Text("Language or alias (e.g. kt, js, html, py, sh)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            color =
                if (catalogIsSupported) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = "isSupported(\"$catalogInput\") = $catalogIsSupported",
                    style =
                        TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color =
                                if (catalogIsSupported) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                        ),
                )
                Text(
                    text = "canonicalName(\"$catalogInput\") = ${catalogCanonicalName?.let { "\"$it\"" } ?: "null"}",
                    style =
                        TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color =
                                if (catalogIsSupported) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                        ),
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── File Extension Resolver ──────────────────────────────────────────
        SubSectionHeader("HighlightLanguage.fromExtension()")
        Text(
            text = "Resolves file extensions to Highlight.js identifiers.",
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
                if (resolvedFromExtension != null) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
        ) {
            Text(
                text =
                    if (resolvedFromExtension != null) {
                        "fromExtension(\"$normalizedExtension\") = \"$resolvedFromExtension\""
                    } else {
                        "fromExtension(\"$normalizedExtension\") = null (not recognized)"
                    },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style =
                    TextStyle(
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color =
                            if (resolvedFromExtension != null) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                    ),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── Runtime Metadata Lookup ──────────────────────────────────────────
        SubSectionHeader("engine.getLanguage()")
        Text(
            text = "Queries the runtime engine for registered aliases and canonical metadata.",
            style = TextStyle(fontSize = 13.sp),
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LANGUAGE_CHIPS.forEach { lang ->
                FilterChip(
                    selected = lookupLangInput == lang,
                    onClick = {
                        lookupLangInput = lang
                        lookupLangInfo = null
                        lookupNotFound = false
                    },
                    label = { Text(lang) },
                )
            }
        }

        OutlinedTextField(
            value = lookupLangInput,
            onValueChange = {
                lookupLangInput = it
                lookupLangInfo = null
                lookupNotFound = false
            },
            label = { Text("Language name or alias") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                scope.launch {
                    lookupLangInfo = null
                    lookupNotFound = false
                    engine.getLanguage(lookupLangInput.trim()).onSuccess { info ->
                        if (info != null) lookupLangInfo = info else lookupNotFound = true
                    }
                }
            },
        ) {
            Text("Look up")
        }

        lookupLangInfo?.let { info ->
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

        if (lookupNotFound) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = "\"${lookupLangInput.trim()}\" is not recognized by bundled Highlight.js",
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

        // ── Searchable Language List ─────────────────────────────────────────
        if (supportedLanguages.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SubSectionHeader("All Supported Languages (${supportedLanguages.size})")
            LanguageSearchAndList(
                languages = supportedLanguages,
                onCopy = { lang ->
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("language", lang)))
                    }
                },
            )
        }
    }
}

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

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        TextField(
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
            colors =
                TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
        )
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth().height(320.dp)) {
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
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    copiedLang = lang
                                    onCopy(lang)
                                }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = lang,
                            modifier = Modifier.weight(1f),
                            style =
                                TextStyle(
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color =
                                        if (lang == copiedLang) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                ),
                        )
                        if (lang == copiedLang) {
                            Icon(
                                painter = painterResource(R.drawable.check_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.copy_content_alt_rounded),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = LocalContentColor.current.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }
    }
}
