package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.sample.R
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode

/**
 * Interactive styling demo — a single live [SyntaxHighlightedCode] block whose [CodeBlockStyle]
 * and visibility flags are controlled via a [ModalBottomSheet]. Changes in the sheet are
 * immediately reflected in the code block behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StylingSection() {
    // ── Style state ───────────────────────────────────────────────────────────
    var cornerRadius by remember { mutableFloatStateOf(8f) }
    var contentPadding by remember { mutableFloatStateOf(16f) }
    var headerPadding by remember { mutableFloatStateOf(8f) }
    var lineNumberWidth by remember { mutableFloatStateOf(40f) }
    var copyButtonSize by remember { mutableFloatStateOf(32f) }

    // ── Toggle state ─────────────────────────────────────────────────────────
    var showLineNumbers by remember { mutableStateOf(true) }
    var showLanguageLabel by remember { mutableStateOf(true) }
    var showCopyButton by remember { mutableStateOf(true) }

    // ── Bottom sheet ─────────────────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showSheet by remember { mutableStateOf(false) }

    // Build style from current state — recomputed on every state change so the
    // code block behind the sheet updates live.
    val style =
        remember(cornerRadius, contentPadding, headerPadding, lineNumberWidth, copyButtonSize) {
            CodeBlockStyle(
                shape = RoundedCornerShape(cornerRadius.dp),
                padding = PaddingValues(contentPadding.dp),
                headerPadding = PaddingValues(horizontal = contentPadding.dp, vertical = headerPadding.dp),
                lineNumberWidth = lineNumberWidth.dp,
                copyButtonSize = copyButtonSize.dp,
            )
        }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── Live preview code block ───────────────────────────────────────────
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style = style,
            showLineNumbers = showLineNumbers,
            showLanguageLabel = showLanguageLabel,
            showCopyButton = showCopyButton,
        )

        // ── Open sheet button ─────────────────────────────────────────────────
        FilledTonalIconButton(
            onClick = { showSheet = true },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Icon(
                painter = painterResource(R.drawable.tune_24dp),
                contentDescription = "Customize style",
            )
        }
    }

    // ── Bottom sheet ──────────────────────────────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
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
                SheetSectionLabel("Visibility")
                ToggleRow("Show line numbers", showLineNumbers) { showLineNumbers = it }
                ToggleRow("Show language label", showLanguageLabel) { showLanguageLabel = it }
                ToggleRow("Show copy button", showCopyButton) { showCopyButton = it }

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
