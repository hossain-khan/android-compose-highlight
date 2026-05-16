package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.ui.SyntaxHighlightedCode

/**
 * Showcases all 256 bundled highlight.js 11.11.1 themes.
 *
 * Theme names are discovered at runtime from the sample app's `assets/themes/`
 * directory. A searchable dropdown lets the user pick any theme; the code block below updates live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AllThemesSection() {
    val context = LocalContext.current.applicationContext

    // Discover all .min.css theme names from assets at runtime (root + base16 subdirectory).
    val allThemeNames =
        remember(context) {
            val assets = context.assets
            val root =
                assets
                    .list("themes")
                    ?.filter { it.endsWith(".min.css") }
                    ?.map { it.removeSuffix(".min.css") }
                    ?: emptyList()
            val base16 =
                assets
                    .list("themes/base16")
                    ?.filter { it.endsWith(".min.css") }
                    ?.map { "base16/${it.removeSuffix(".min.css")}" }
                    ?: emptyList()
            (root + base16).sorted()
        }

    var selectedThemeName by rememberSaveable { mutableStateOf("atom-one-dark") }
    var searchQuery by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val filteredThemes =
        remember(searchQuery, allThemeNames) {
            if (searchQuery.isEmpty()) {
                allThemeNames
            } else {
                allThemeNames.filter { it.contains(searchQuery, ignoreCase = true) }
            }
        }

    val theme =
        remember(selectedThemeName, context) {
            HighlightTheme.fromAsset(context, "themes/$selectedThemeName.min.css", selectedThemeName)
        }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Browse all ${allThemeNames.size} bundled highlight.js themes — select one to live-preview it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    expanded = true
                },
                label = { Text("Search theme (${allThemeNames.size} available)") },
                placeholder = { Text(selectedThemeName) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable),
            )

            ExposedDropdownMenu(
                expanded = expanded && filteredThemes.isNotEmpty(),
                onDismissRequest = {
                    expanded = false
                    searchQuery = ""
                },
            ) {
                filteredThemes.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedThemeName = name
                            searchQuery = ""
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        Text(
            text = "Active theme: $selectedThemeName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )

        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = theme,
            showLineNumbers = true,
        )
    }
}
