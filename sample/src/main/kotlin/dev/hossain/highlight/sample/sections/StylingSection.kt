package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.sample.KOTLIN_EXTENDED_SNIPPET
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.sample.PYTHON_SNIPPET
import dev.hossain.highlight.sample.R
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

/**
 * Interactive styling and header chrome demo:
 * 1. Live [SyntaxHighlightedCode] block whose [CodeBlockStyle], typography, and visibility
 *    are controlled via a [ModalBottomSheet].
 * 2. Real-world header chrome showcase demonstrating macOS IDE window bar, tabbed file viewer,
 *    and clean headerless mode (`header = null`).
 *
 * @param showSheet Whether the configuration bottom sheet is currently visible.
 * @param onDismissSheet Called when the sheet should be dismissed.
 * @param onActionMessage Receives a snackbar message when a custom action button (edit, share) is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StylingSection(
    showSheet: Boolean,
    onDismissSheet: () -> Unit,
    onActionMessage: (String) -> Unit = {},
) {
    // ── Style state ───────────────────────────────────────────────────────────
    var cornerRadius by remember { mutableFloatStateOf(8f) }
    var contentPadding by remember { mutableFloatStateOf(16f) }
    var headerPadding by remember { mutableFloatStateOf(8f) }
    var lineNumberWidth by remember { mutableFloatStateOf(40f) }
    var copyButtonSize by remember { mutableFloatStateOf(32f) }
    var fontSize by remember { mutableFloatStateOf(13f) }
    var lineHeight by remember { mutableFloatStateOf(20f) }
    var selectedFontFamily by remember { mutableStateOf(FontFamily.Monospace) }

    // ── Toggle state ─────────────────────────────────────────────────────────
    var showLineNumbers by remember { mutableStateOf(true) }
    var showLanguageLabel by remember { mutableStateOf(true) }
    var showCopyButton by remember { mutableStateOf(true) }
    var useCustomLanguageLabel by remember { mutableStateOf(false) }
    var useCustomCopyIcon by remember { mutableStateOf(false) }
    var showMultipleActions by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Build style from current state - recomputed on every state change
    val style =
        remember(
            cornerRadius,
            contentPadding,
            headerPadding,
            lineNumberWidth,
            copyButtonSize,
            fontSize,
            lineHeight,
            selectedFontFamily,
        ) {
            CodeBlockStyle(
                shape = RoundedCornerShape(cornerRadius.dp),
                padding = PaddingValues(contentPadding.dp),
                headerPadding = PaddingValues(horizontal = contentPadding.dp, vertical = headerPadding.dp),
                lineNumberWidth = lineNumberWidth.dp,
                copyButtonSize = copyButtonSize.dp,
                textStyle =
                    TextStyle(
                        fontFamily = selectedFontFamily,
                        fontSize = fontSize.sp,
                        lineHeight = lineHeight.sp,
                    ),
            )
        }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ── 1. Live customizable preview ──────────────────────────────────────
        SubSectionHeader("Interactive Style Playground")
        Text(
            text = "Tap \"Customize Style\" button to adjust padding, corners, typography, and actions live.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SyntaxHighlightedCode(
            code = KOTLIN_EXTENDED_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style = style,
            showLineNumbers = showLineNumbers,
            languageLabel =
                if (!showLanguageLabel) {
                    null
                } else if (useCustomLanguageLabel) {
                    {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(text = "Kotlin", style = MaterialTheme.typography.labelMedium)
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.stars_2_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                } else {
                    { SyntaxHighlightedCodeDefaults.LanguageLabel("kotlin") }
                },
            actions =
                if (!showCopyButton) {
                    null
                } else if (showMultipleActions) {
                    { onCopy ->
                        IconButton(onClick = { onActionMessage("Edit action clicked") }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.edit_square_24dp),
                                contentDescription = "Edit code",
                                modifier = Modifier.size(style.copyButtonSize * 0.6f),
                            )
                        }
                        IconButton(onClick = { onActionMessage("Share action clicked") }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.share_24dp),
                                contentDescription = "Share code",
                                modifier = Modifier.size(style.copyButtonSize * 0.6f),
                            )
                        }
                        SyntaxHighlightedCodeDefaults.CopyButton(onClick = onCopy, size = style.copyButtonSize)
                    }
                } else if (useCustomCopyIcon) {
                    { onCopy ->
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(style.copyButtonSize),
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.content_copy_24dp),
                                contentDescription = "Copy code",
                                modifier = Modifier.size(style.copyButtonSize * 0.5f),
                            )
                        }
                    }
                } else {
                    { onCopy -> SyntaxHighlightedCodeDefaults.CopyButton(onClick = onCopy, size = style.copyButtonSize) }
                },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ── 2. Real-world Header Chrome Demos ──────────────────────────────────
        SubSectionHeader("Custom Header Chrome: macOS Window Title Bar")
        Text(
            text = "Using header = { onCopy -> ... } to build macOS-style traffic lights, breadcrumb, and copy button.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = true,
            header = { onCopy ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Traffic light dots
                    Box(Modifier.size(10.dp).background(Color(0xFFFF5F56), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(10.dp).background(Color(0xFFFFBD2E), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(10.dp).background(Color(0xFF27C93F), CircleShape))

                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "src/main/kotlin/WeatherApp.kt",
                        style =
                            TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                    )

                    Spacer(Modifier.weight(1f))
                    SyntaxHighlightedCodeDefaults.CopyButton(onClick = onCopy)
                }
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SubSectionHeader("Custom Header Chrome: Tabbed File Viewer")
        Text(
            text = "Embedding interactive tabs inside header = { onCopy -> ... } to switch between files.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        var selectedFileTab by remember { mutableIntStateOf(0) }
        val tabFiles = listOf("WeatherApp.kt" to ("kotlin" to KOTLIN_SNIPPET), "script.py" to ("python" to PYTHON_SNIPPET))
        val currentFile = tabFiles[selectedFileTab]
        SyntaxHighlightedCode(
            code = currentFile.second.second,
            language = currentFile.second.first,
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = true,
            header = { onCopy ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryTabRow(
                        selectedTabIndex = selectedFileTab,
                        modifier = Modifier.weight(1f),
                    ) {
                        tabFiles.forEachIndexed { index, pair ->
                            Tab(
                                selected = selectedFileTab == index,
                                onClick = { selectedFileTab = index },
                                text = { Text(pair.first, fontSize = 12.sp) },
                            )
                        }
                    }
                    SyntaxHighlightedCodeDefaults.CopyButton(
                        onClick = onCopy,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SubSectionHeader("Headerless Mode (header = null)")
        Text(
            text = "Clean code-only rendering with header = null, ideal for inline docs or chat bubbles.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Documentation Callout: Configuration Example",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                SyntaxHighlightedCode(
                    code = "val apiKey = BuildConfig.API_KEY\nval client = HttpClient(apiKey)",
                    language = "kotlin",
                    header = null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // ── Bottom sheet ──────────────────────────────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissSheet,
            sheetState = sheetState,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Style Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                // ── Toggles ───────────────────────────────────────────────────
                SheetSectionLabel("Visibility & Actions")
                ToggleRow("Show line numbers", showLineNumbers) { showLineNumbers = it }
                ToggleRow("Show language label", showLanguageLabel) { showLanguageLabel = it }
                ToggleRow("Custom language label", useCustomLanguageLabel) { useCustomLanguageLabel = it }
                ToggleRow("Show copy button", showCopyButton) { showCopyButton = it }
                ToggleRow("Custom copy icon", useCustomCopyIcon) { useCustomCopyIcon = it }
                ToggleRow("Multiple actions (edit, share, copy)", showMultipleActions) { showMultipleActions = it }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // ── Typography ─────────────────────────────────────────────────
                SheetSectionLabel("Typography")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedFontFamily == FontFamily.Monospace,
                        onClick = { selectedFontFamily = FontFamily.Monospace },
                        label = { Text("Monospace") },
                    )
                    FilterChip(
                        selected = selectedFontFamily == FontFamily.Serif,
                        onClick = { selectedFontFamily = FontFamily.Serif },
                        label = { Text("Serif") },
                    )
                    FilterChip(
                        selected = selectedFontFamily == FontFamily.SansSerif,
                        onClick = { selectedFontFamily = FontFamily.SansSerif },
                        label = { Text("SansSerif") },
                    )
                }
                SliderRow(
                    label = "Font size",
                    value = fontSize,
                    valueRange = 10f..24f,
                    unit = "sp",
                    onValueChange = { fontSize = it },
                )
                SliderRow(
                    label = "Line height",
                    value = lineHeight,
                    valueRange = 16f..36f,
                    unit = "sp",
                    onValueChange = { lineHeight = it },
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // ── Sliders ───────────────────────────────────────────────────
                SheetSectionLabel("Shape & Spacing")
                SliderRow(
                    label = "Corner radius",
                    value = cornerRadius,
                    valueRange = 0f..32f,
                    unit = "dp",
                    onValueChange = { cornerRadius = it },
                )
                SliderRow(
                    label = "Content padding",
                    value = contentPadding,
                    valueRange = 4f..32f,
                    unit = "dp",
                    onValueChange = { contentPadding = it },
                )
                SliderRow(
                    label = "Header padding",
                    value = headerPadding,
                    valueRange = 2f..20f,
                    unit = "dp",
                    onValueChange = { headerPadding = it },
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                SheetSectionLabel("Dimensions")
                SliderRow(
                    label = "Line number width",
                    value = lineNumberWidth,
                    valueRange = 20f..80f,
                    unit = "dp",
                    onValueChange = { lineNumberWidth = it },
                )
                SliderRow(
                    label = "Copy button size",
                    value = copyButtonSize,
                    valueRange = 16f..48f,
                    unit = "dp",
                    onValueChange = { copyButtonSize = it },
                )
            }
        }
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit,
) {
    val displayValue = remember(value) { value.toInt() }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$displayValue $unit",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
