package dev.hossain.highlight.sample.perf

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import dev.hossain.highlight.sample.CodeSample
import dev.hossain.highlight.sample.R
import dev.hossain.highlight.sample.loadCodeSamples
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberAtomOneDarkTheme
import dev.hossain.highlight.ui.rememberAtomOneLightTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration

/** Timing and size metrics captured for a single code block after highlighting completes. */
internal data class HighlightMetrics(
    val language: String,
    val charCount: Int,
    val lineCount: Int,
    val timings: HighlightTimings,
) {
    val highlightMs: Long get() = timings.total.inWholeMilliseconds
}

/**
 * Performance benchmark screen that renders every loaded code sample in a scrollable list and
 * measures per-block highlighting timing, code size, and overall heap usage.
 *
 * - Each block shows a metric card: highlight time, line count, char count.
 * - The sticky summary header shows aggregate stats (avg/min/max time, heap snapshot).
 * - The ↺ Re-run button resets all metrics and forces a fresh highlight pass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfScreen() {
    val context = LocalContext.current
    val codeSamples by produceState(initialValue = emptyList<CodeSample>(), context) {
        value = withContext(Dispatchers.IO) { loadCodeSamples(context) }
    }
    var isDark by remember { mutableStateOf(true) }

    val metricsMap = remember { mutableStateMapOf<String, HighlightMetrics>() }
    var runId by remember { mutableIntStateOf(0) }

    // Take a heap snapshot once all blocks have reported their timing.
    var heapSnapshotKb by remember { mutableStateOf<Long?>(null) }
    if (codeSamples.isNotEmpty() && metricsMap.size == codeSamples.size && heapSnapshotKb == null) {
        val rt = Runtime.getRuntime()
        heapSnapshotKb = (rt.totalMemory() - rt.freeMemory()) / 1024
    }

    HighlightThemeProvider(
        lightHighlightTheme = rememberAtomOneLightTheme(),
        darkHighlightTheme = rememberAtomOneDarkTheme(),
        darkTheme = isDark,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Perf Benchmark") },
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.finish() }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.arrow_back_24dp),
                                contentDescription = "Navigate back",
                            )
                        }
                    },
                    actions = {
                        // Light/dark toggle - also resets the benchmark since theme affects timing
                        IconButton(onClick = {
                            isDark = !isDark
                            metricsMap.clear()
                            heapSnapshotKb = null
                            runId++
                        }) {
                            Icon(
                                imageVector =
                                    ImageVector.vectorResource(
                                        if (isDark) R.drawable.light_mode_24dp else R.drawable.mode_night_24dp,
                                    ),
                                contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
                            )
                        }
                        // Re-run benchmark
                        IconButton(onClick = {
                            metricsMap.clear()
                            heapSnapshotKb = null
                            runId++
                        }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.readiness_score_24dp),
                                contentDescription = "Re-run benchmark",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(innerPadding),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "summary") {
                    Spacer(modifier = Modifier.height(4.dp))
                    SummaryHeader(
                        metricsMap = metricsMap,
                        totalSamples = codeSamples.size,
                        heapSnapshotKb = heapSnapshotKb,
                    )
                }

                items(codeSamples, key = { it.displayLabel }) { sample ->
                    val metrics = metricsMap[sample.displayLabel]
                    Column {
                        Text(
                            text = sample.displayLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        key(runId) {
                            SyntaxHighlightedCode(
                                code = sample.code,
                                language = sample.language,
                                modifier = Modifier.fillMaxWidth(),
                                showLineNumbers = true,
                                onHighlightComplete = { result ->
                                    metricsMap[sample.displayLabel] =
                                        HighlightMetrics(
                                            language = sample.language,
                                            charCount = sample.code.length,
                                            lineCount = sample.code.lines().size,
                                            timings = result.timings,
                                        )
                                },
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        PerfMetricCard(metrics = metrics)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

/**
 * Sticky summary card shown at the top of the list.
 *
 * Displays: completed/total count, avg/min/max highlight time, and heap snapshot once all blocks
 * have completed.
 */
@Composable
private fun SummaryHeader(
    metricsMap: Map<String, HighlightMetrics>,
    totalSamples: Int,
    heapSnapshotKb: Long?,
) {
    val completed = metricsMap.size
    val times = metricsMap.values.map { it.highlightMs }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Benchmark Summary",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(6.dp))

            SummaryRow(label = "Completed", value = "$completed / $totalSamples blocks")

            if (times.isNotEmpty()) {
                SummaryRow(label = "Avg time", value = "${times.average().toLong()} ms")
                SummaryRow(label = "Fastest", value = "${times.min()} ms")
                SummaryRow(label = "Slowest", value = "${times.max()} ms")
                SummaryRow(label = "Total time", value = "${times.sum()} ms")
            } else {
                SummaryRow(label = "Timing", value = "Waiting for highlights…")
            }

            heapSnapshotKb?.let {
                SummaryRow(label = "Heap used", value = "$it KB  (snapshot after all blocks)")
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * Metric card shown below each code block.
 *
 * Top row shows total highlight time, line count, and char count.
 * Below the divider, a per-stage pipeline breakdown is shown using [HighlightTimings]:
 * JS bridge, HTML parse, tree walk, and theme parse (only when non-zero - first call only).
 */
@Composable
private fun PerfMetricCard(metrics: HighlightMetrics?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (metrics != null) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        if (metrics != null) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Top row: total time, lines, chars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetricChip(
                        icon = ImageVector.vectorResource(R.drawable.timer_24dp),
                        value = "${metrics.highlightMs} ms",
                        label = "total",
                    )
                    MetricChip(
                        icon = ImageVector.vectorResource(R.drawable.format_line_spacing_24dp),
                        value = "${metrics.lineCount}",
                        label = "lines",
                    )
                    MetricChip(
                        icon = ImageVector.vectorResource(R.drawable.type_specimen_24dp),
                        value = "${metrics.charCount}",
                        label = "chars",
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(6.dp))

                // Pipeline breakdown
                Text(
                    text = "Pipeline breakdown",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                PipelineRow(label = "JS bridge", duration = metrics.timings.jsBridge)
                PipelineRow(label = "JSON unescape", duration = metrics.timings.jsonUnescape)
                PipelineRow(label = "HTML parse", duration = metrics.timings.htmlParse)
                PipelineRow(label = "Tree walk", duration = metrics.timings.treeWalk)
                if (metrics.timings.themeParse > Duration.ZERO) {
                    PipelineRow(
                        label = "Theme parse (first use)",
                        duration = metrics.timings.themeParse,
                        highlight = true,
                    )
                }
            }
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Highlighting…",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

/** Single row in the pipeline breakdown table. */
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
            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
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
