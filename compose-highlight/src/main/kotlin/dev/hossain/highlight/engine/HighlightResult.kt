package dev.hossain.highlight.engine

import androidx.compose.ui.text.AnnotatedString

/**
 * Result of a successful [HighlightEngine.highlight] call.
 *
 * Provides richer observability than a bare [AnnotatedString]: callers can inspect
 * whether highlighting actually produced tokens ([spanCount] > 0), measure pure
 * highlight time ([durationMs]) without coroutine-scheduling overhead, and drill into
 * per-layer timing via [timings].
 *
 * ## Usage
 *
 * ```kotlin
 * engine.highlight(code, "kotlin", theme).onSuccess { result ->
 *     // Render the highlighted text
 *     displayText = result.annotated
 *
 *     // Detect silent failures: a spanCount of 0 means the highlighter returned
 *     // plain text - either the language was not recognised or the code was empty.
 *     if (result.spanCount == 0) {
 *         log("Warning: no highlight tokens produced for language '${result.language}'")
 *     }
 *
 *     // Log pure engine time (excludes coroutine scheduling overhead)
 *     log("Highlighted in ${result.durationMs} ms")
 *
 *     // Drill into per-layer timing
 *     log("JS bridge:    ${result.timings.jsBridge}")
 *     log("JSON unescape:${result.timings.jsonUnescape}")
 *     log("HTML parse:   ${result.timings.htmlParse}")
 *     log("Theme parse:  ${result.timings.themeParse}")  // non-zero on first call only
 * }
 * ```
 *
 * ## Via the composable callback
 *
 * ```kotlin
 * SyntaxHighlightedCode(
 *     code = snippet,
 *     language = "kotlin",
 *     onHighlightComplete = { result ->
 *         metricsMap[result.language] = HighlightMetrics(
 *             spanCount = result.spanCount,
 *             durationMs = result.durationMs,
 *             jsBridgeMs = result.timings.jsBridge.inWholeMilliseconds,
 *         )
 *     },
 * )
 * ```
 *
 * @property annotated The syntax-highlighted [AnnotatedString] ready for rendering.
 * @property spanCount Number of syntax-highlighted token spans. A value of `0`
 *   indicates that no tokens were found - the language may be unsupported or the code
 *   was empty (silent failure). Excludes the base theme `.hljs` span if one was applied.
 * @property language The Highlight.js language identifier that was requested
 *   (e.g. `"kotlin"`, `"python"`).
 * @property durationMs Pure highlight time in milliseconds - from the start of the
 *   [HighlightEngine.highlight] call through HTML conversion. Excludes coroutine
 *   scheduling overhead that the caller would observe. Equals
 *   `timings.total.inWholeMilliseconds`.
 * @property timings Per-layer timing breakdown for this highlight call. Always populated.
 *   See [HighlightTimings] for the full stage breakdown.
 */
data class HighlightResult(
    val annotated: AnnotatedString,
    val spanCount: Int,
    val language: String,
    val durationMs: Long,
    val timings: HighlightTimings,
)
