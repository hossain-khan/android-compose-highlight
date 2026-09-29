package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.engine.HighlightThemeDescriptor
import dev.hossain.highlight.engine.HljsSelectors
import dev.hossain.highlight.sample.JAVASCRIPT_EXTENDED_SNIPPET
import dev.hossain.highlight.sample.R
import dev.hossain.highlight.ui.SyntaxHighlightedCode

private val PREVIEW_CODE_SNIPPET =
    """
    // Fibonacci sequence calculation
    fun fibonacci(n: Int): Long {
        if (n <= 1) return n.toLong()
        return fibonacci(n - 1) + fibonacci(n - 2)
    }
    """.trimIndent()

private val API_USAGE_CODE =
    """
    // 1. Enumerate bundled themes (for theme pickers or settings)
    val allThemes: List<HighlightThemeDescriptor> = HighlightTheme.bundled
    val lightThemes = HighlightTheme.bundledLight
    val darkThemes = HighlightTheme.bundledDark

    // 2. Read descriptor metadata
    val id: String = descriptor.id              // e.g. "dracula"
    val name: String = descriptor.displayName   // e.g. "Dracula"
    val isDark: Boolean = descriptor.isDark     // true / false
    val theme: HighlightTheme = descriptor.theme // Cached theme instance

    // 3. Restore persisted theme with safe fallback
    val savedId = preferences.getString("theme_id", "tomorrow")
    val activeTheme = HighlightTheme.findBundledById(savedId)?.create()
        ?: if (isSystemInDarkTheme()) HighlightTheme.tomorrowNight()
        else HighlightTheme.tomorrow()
    """.trimIndent()

private enum class ThemeFilter {
    ALL,
    LIGHT,
    DARK,
}

private enum class GallerySubTab(
    val label: String,
) {
    BUILT_IN("Built-in Themes"),
    ALL_ASSETS("All Asset Themes"),
}

/**
 * Unified theme browser showcasing:
 * 1. Curated built-in themes with metadata, ID resolution, swatches, and live preview cards.
 * 2. Full 200+ asset themes catalog discovered at runtime from sample assets.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeGallerySection(modifier: Modifier = Modifier) {
    var selectedSubTab by rememberSaveable { mutableStateOf(GallerySubTab.BUILT_IN) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            GallerySubTab.entries.forEachIndexed { index, subTab ->
                SegmentedButton(
                    selected = selectedSubTab == subTab,
                    onClick = { selectedSubTab = subTab },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = GallerySubTab.entries.size),
                ) {
                    Text(
                        text =
                            when (subTab) {
                                GallerySubTab.BUILT_IN -> "${subTab.label} (${HighlightTheme.bundled.size})"
                                GallerySubTab.ALL_ASSETS -> subTab.label
                            },
                    )
                }
            }
        }

        when (selectedSubTab) {
            GallerySubTab.BUILT_IN -> BuiltInThemesView()
            GallerySubTab.ALL_ASSETS -> AllAssetThemesView()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuiltInThemesView() {
    var selectedFilter by remember { mutableStateOf(ThemeFilter.ALL) }
    var testIdInput by remember { mutableStateOf("dracula") }

    val displayedThemes =
        remember(selectedFilter) {
            when (selectedFilter) {
                ThemeFilter.ALL -> HighlightTheme.bundled
                ThemeFilter.LIGHT -> HighlightTheme.bundledLight
                ThemeFilter.DARK -> HighlightTheme.bundledDark
            }
        }

    val resolvedDescriptor =
        remember(testIdInput) {
            HighlightTheme.findBundledById(testIdInput.trim())
        }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text =
                "First-party theme discovery and descriptor APIs. " +
                    "Enumerate bundled themes, read metadata (display name, dark/light mode), " +
                    "and safely restore persisted themes using stable identifiers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // API Quick Reference
        SubSectionHeader("API Quick Reference")
        SyntaxHighlightedCode(
            code = API_USAGE_CODE,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = HighlightTheme.atomOneDark(),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Interactive ID Resolver
        SubSectionHeader("Interactive ID Resolver (HighlightTheme.findBundledById)")
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Test how stable theme IDs resolve in O(1):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = testIdInput,
                    onValueChange = { testIdInput = it },
                    label = { Text("Theme ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Quick suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        "tomorrow" to "Light",
                        "dracula" to "Dark",
                        "atom-one-dark" to "Dark",
                        "github" to "Light",
                        "alucard" to "Light",
                        "monokai" to "Unknown",
                    ).forEach { (id, tag) ->
                        FilterChip(
                            selected = testIdInput == id,
                            onClick = { testIdInput = id },
                            label = { Text("$id ($tag)") },
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color =
                        if (resolvedDescriptor != null) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (resolvedDescriptor != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.check_24dp),
                                    contentDescription = "Resolved",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Resolved: ${resolvedDescriptor.displayName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                ThemeModeBadge(isDark = resolvedDescriptor.isDark)
                            }
                            Text(
                                text = "ID: \"${resolvedDescriptor.id}\"",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = "Unrecognized ID: \"${testIdInput.trim()}\"",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = "findBundledById returns null. Safe fallback applied in app logic.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Bundled Themes Catalog
        SubSectionHeader("Bundled Themes Catalog (${displayedThemes.size} themes)")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedFilter == ThemeFilter.ALL,
                onClick = { selectedFilter = ThemeFilter.ALL },
                label = { Text("All (${HighlightTheme.bundled.size})") },
            )
            FilterChip(
                selected = selectedFilter == ThemeFilter.LIGHT,
                onClick = { selectedFilter = ThemeFilter.LIGHT },
                label = { Text("Light (${HighlightTheme.bundledLight.size})") },
            )
            FilterChip(
                selected = selectedFilter == ThemeFilter.DARK,
                onClick = { selectedFilter = ThemeFilter.DARK },
                label = { Text("Dark (${HighlightTheme.bundledDark.size})") },
            )
        }

        displayedThemes.forEach { descriptor ->
            ThemeDescriptorCard(descriptor = descriptor)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeDescriptorCard(descriptor: HighlightThemeDescriptor) {
    val theme = descriptor.theme

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = descriptor.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Text(
                            text = "id: \"${descriptor.id}\"",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                ThemeModeBadge(isDark = descriptor.isDark)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ColorSwatch(label = "bg", color = theme.backgroundColor)
                ColorSwatch(label = "text", color = theme.defaultTextColor)
                theme.colorMap[HljsSelectors.KEYWORD]?.color?.let { color ->
                    ColorSwatch(label = "keyword", color = color)
                }
                theme.colorMap[HljsSelectors.STRING]?.color?.let { color ->
                    ColorSwatch(label = "string", color = color)
                }
            }

            SyntaxHighlightedCode(
                code = PREVIEW_CODE_SNIPPET,
                language = "kotlin",
                modifier = Modifier.fillMaxWidth(),
                theme = theme,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllAssetThemesView() {
    val context = LocalContext.current.applicationContext

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
            text =
                "Browse all ${allThemeNames.size} highlight.js themes included in this sample app's assets. " +
                    "Select any theme to live-preview it.",
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
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
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
            code = JAVASCRIPT_EXTENDED_SNIPPET,
            language = "javascript",
            modifier = Modifier.fillMaxWidth(),
            theme = theme,
            showLineNumbers = true,
        )
    }
}

@Composable
private fun ThemeModeBadge(
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (isDark) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
    val contentColor =
        if (isDark) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }
    val iconRes =
        if (isDark) {
            R.drawable.mode_night_24dp
        } else {
            R.drawable.light_mode_24dp
        }
    val label = if (isDark) "Dark" else "Light"

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = if (color == Color.Unspecified) Color.Transparent else color,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(12.dp),
        ) {}
        Text(
            text = "$label: ${color.toHex()}",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Color.toHex(): String =
    if (this == Color.Unspecified) {
        "None"
    } else {
        val r = (red * 255).toInt()
        val g = (green * 255).toInt()
        val b = (blue * 255).toInt()
        String.format("#%02X%02X%02X", r, g, b)
    }
