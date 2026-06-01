package dev.hossain.highlight.engine

import kotlin.time.Duration

/**
 * Per-layer timing breakdown for a single highlight call.
 *
 * Each field is a [Duration] measured via `measureTimedValue` at each pipeline stage.
 * All fields are always populated - [themeParse] is non-zero only on the first call for a
 * given [HighlightTheme] (the CSS parse is lazy and cached; subsequent calls report
 * [Duration.ZERO]).
 *
 * ## Pipeline stages
 *
 * | Stage | Description |
 * |---|---|
 * | [jsBridge] | `evaluateJavascript()` round-trip into WebView running highlight.js |
 * | [jsonUnescape] | `unescapeJsString()` pass over the returned JSON string |
 * | [htmlParse] | `Jsoup.parseBodyFragment()` - HTML to DOM |
 * | [treeWalk] | DOM node walk + `SpanStyle` lookup from theme color map |
 * | [themeParse] | Theme CSS parsing - first-use only; [Duration.ZERO] on cache hits |
 * | [total] | End-to-end time for the full highlight call |
 *
 * ## Usage
 *
 * ```kotlin
 * engine.highlight(code, "kotlin", theme).onSuccess { result ->
 *     val t = result.timings
 *     println("JS bridge:    ${t.jsBridge}")        // e.g. "45ms"
 *     println("JSON unescape:${t.jsonUnescape}")     // e.g. "1ms"
 *     println("HTML parse:   ${t.htmlParse}")        // e.g. "3ms"
 *     println("Tree walk:    ${t.treeWalk}")         // e.g. "2ms"
 *     println("Theme parse:  ${t.themeParse}")       // non-zero on first call only
 *     println("Total:        ${t.total}")            // full elapsed wall-clock time
 *     // Or get raw numbers:
 *     val jsBridgeMs = t.jsBridge.inWholeMilliseconds
 *     val treeWalkNs = t.treeWalk.inWholeNanoseconds
 * }
 * ```
 *
 * @property jsBridge Time for the `evaluateJavascript()` round-trip - from immediately before
 *   the call to when the JS result callback fires. Excludes WebView warm-up and mutex-wait time.
 * @property jsonUnescape Time for `unescapeJsString` - the character-by-character pass that
 *   strips JSON encoding from the string returned by the JS engine.
 * @property htmlParse Time for `Jsoup.parseBodyFragment()` - parsing the highlight.js HTML
 *   output into a DOM tree.
 * @property treeWalk Time for the DOM tree walk and [androidx.compose.ui.text.SpanStyle]
 *   application loop that builds the [androidx.compose.ui.text.AnnotatedString].
 * @property themeParse Time for theme CSS parsing on first use of a
 *   [HighlightTheme]. [Duration.ZERO] on all subsequent calls for the same theme instance
 *   because the color map is lazily computed and cached.
 * @property total Full elapsed wall-clock time for the [HighlightEngine.highlight] call.
 *   Measured end-to-end and includes overhead not captured by the individual stage fields
 *   (WebView initialization wait, mutex queuing, dispatcher hops). Use this for overall
 *   latency; use the individual stage fields to identify which layer is the bottleneck.
 *   Equals [HighlightResult.durationMs] converted to [Duration].
 */
data class HighlightTimings(
    val jsBridge: Duration,
    val jsonUnescape: Duration,
    val htmlParse: Duration,
    val treeWalk: Duration,
    val themeParse: Duration,
    val total: Duration,
)
