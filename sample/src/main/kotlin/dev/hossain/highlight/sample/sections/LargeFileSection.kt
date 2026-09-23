package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.engine.HighlightTimings
import dev.hossain.highlight.sample.R
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration

private const val LARGE_FILE_ASSET_PATH = "large_samples/underscore-esm.js"
private const val LARGE_FILE_LANGUAGE = "javascript"
private const val LARGE_FILE_LABEL = "underscore-esm.js"

/**
 * Demo section rendering a large source file (~71 KB, 2,170+ lines of JavaScript)
 * with real-time performance and pipeline timing breakdown metrics shown at the top.
 */
@Composable
internal fun LargeFileSection(
    onCopyClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var runId by remember { mutableIntStateOf(0) }
    var timings by remember(runId) { mutableStateOf<HighlightTimings?>(null) }

    val code by produceState<String?>(initialValue = null, context) {
        value =
            withContext(Dispatchers.IO) {
                runCatching {
                    context.assets
                        .open(LARGE_FILE_ASSET_PATH)
                        .bufferedReader()
                        .use { it.readText() }
                }.getOrNull()
            }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader("Large File Performance Demo")

        Text(
            text =
                "Highlights Underscore.js non-minified ES module build ($LARGE_FILE_LABEL) " +
                    "to benchmark real-world parsing and rendering latency on production-scale source files.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val currentCode = code
        if (currentCode == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Loading $LARGE_FILE_LABEL from assets...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            LargeFileMetricsCard(
                timings = timings,
                lineCount = currentCode.lines().size,
                charCount = currentCode.length,
                byteSize = currentCode.toByteArray().size,
                onReRun = {
                    timings = null
                    runId++
                },
            )

            key(runId) {
                SyntaxHighlightedCode(
                    code = currentCode,
                    language = LARGE_FILE_LANGUAGE,
                    modifier = Modifier.fillMaxWidth(),
                    showLineNumbers = true,
                    onHighlightComplete = { result ->
                        timings = result.timings
                    },
                    onCopyClick = onCopyClick,
                    copyButton = { onClick ->
                        SyntaxHighlightedCodeDefaults.CopyButton(
                            onClick = onClick,
                            contentDescription = "Copy code",
                        )
                    },
                )
            }
        }
    }
}

/**
 * Pinned metrics card displayed above the code block.
 *
 * Shows overall highlight time, code volume (lines/chars/bytes), and the detailed
 * pipeline timing breakdown (JS bridge, JSON unescape, HTML parsing, theme parsing).
 */
@Composable
private fun LargeFileMetricsCard(
    timings: HighlightTimings?,
    lineCount: Int,
    charCount: Int,
    byteSize: Int,
    onReRun: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (timings != null) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Performance Metrics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color =
                        if (timings != null) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )

                FilledTonalButton(
                    onClick = onReRun,
                    enabled = timings != null,
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.readiness_score_24dp),
                        contentDescription = "Re-run highlight pass",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Re-highlight", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary metrics chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetricChip(
                    icon = ImageVector.vectorResource(R.drawable.timer_24dp),
                    value = timings?.total?.inWholeMilliseconds?.let { "$it ms" } ?: "Working...",
                    label = "Total time",
                )
                MetricChip(
                    icon = ImageVector.vectorResource(R.drawable.format_line_spacing_24dp),
                    value = "$lineCount",
                    label = "Lines",
                )
                MetricChip(
                    icon = ImageVector.vectorResource(R.drawable.type_specimen_24dp),
                    value = "$charCount",
                    label = "Characters",
                )
                MetricChip(
                    icon = ImageVector.vectorResource(R.drawable.code_xml_24dp),
                    value = "${byteSize / 1024} KB",
                    label = "File size",
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f),
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (timings != null) {
                Text(
                    text = "Pipeline Stage Breakdown",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                PipelineRow(label = "JS Bridge (evaluateJavascript)", duration = timings.jsBridge)
                PipelineRow(label = "JSON Unescape pass", duration = timings.jsonUnescape)
                PipelineRow(label = "HTML Parse to AnnotatedString", duration = timings.htmlParse)
                if (timings.themeParse > Duration.ZERO) {
                    PipelineRow(
                        label = "Theme CSS parse (initial load)",
                        duration = timings.themeParse,
                        highlight = true,
                    )
                }
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Highlighting 2,170+ lines of JavaScript...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PipelineRow(
    label: String,
    duration: Duration,
    highlight: Boolean = false,
) {
    val ms = duration.inWholeMilliseconds
    val us = duration.inWholeMicroseconds
    val msRounded = (us + 500) / 1000
    val approxMs = if (msRounded > 0) "~${msRounded}ms" else "<1ms"
    val valueText =
        when {
            ms >= 1 -> "$ms ms"
            us >= 1 -> "${us}µs ($approxMs)"
            else -> "${duration.inWholeNanoseconds} ns"
        }
    val color =
        if (highlight) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 11.sp, color = color)
        Text(text = valueText, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
private fun MetricChip(
    icon: ImageVector,
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.height(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
        )
    }
}
