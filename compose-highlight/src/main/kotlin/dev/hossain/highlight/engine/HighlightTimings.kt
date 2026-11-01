package dev.hossain.highlight.engine

import androidx.compose.runtime.Immutable
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
 * | [htmlParse] | HTML parsing into a node tree and building the AnnotatedString |
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
 *     println("Theme parse:  ${t.themeParse}")       // non-zero on first call only
 *     println("Total:        ${t.total}")            // full elapsed wall-clock time
 *     // Or get raw numbers:
 *     val jsBridgeMs = t.jsBridge.inWholeMilliseconds
 *     val htmlParseNs = t.htmlParse.inWholeNanoseconds
 * }
 * ```
 *
 * @property jsBridge Time for the `evaluateJavascript()` round-trip - from immediately before
 *   the call to when the JS result callback fires. Excludes WebView warm-up and mutex-wait time.
 * @property jsonUnescape Time for `unescapeJsString` - the character-by-character pass that
 *   strips JSON encoding from the string returned by the JS engine.
 * @property htmlParse Time for parsing the highlight.js HTML output and building the
 *   [androidx.compose.ui.text.AnnotatedString] in a single SAX-style pass.
 * @property themeParse Time for theme CSS parsing on first use of a
 *   [HighlightTheme]. [Duration.ZERO] on all subsequent calls for the same theme instance
 *   because the color map is lazily computed and cached.
 * @property total Full elapsed wall-clock time for the [HighlightEngine.highlight] call.
 *   Measured end-to-end and includes overhead not captured by the individual stage fields
 *   (WebView initialization wait, mutex queuing, dispatcher hops). Use this for overall
 *   latency; use the individual stage fields to identify which layer is the bottleneck.
 *   Equals [HighlightResult.durationMs] converted to [Duration].
 */
@Immutable
public class HighlightTimings(
    public val jsBridge: Duration,
    public val jsonUnescape: Duration,
    public val htmlParse: Duration,
    public val themeParse: Duration,
    public val total: Duration,
) {
    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HighlightTimings) return false
        return jsBridge == other.jsBridge &&
            jsonUnescape == other.jsonUnescape &&
            htmlParse == other.htmlParse &&
            themeParse == other.themeParse &&
            total == other.total
    }

    public override fun hashCode(): Int {
        var result = jsBridge.hashCode()
        result = 31 * result + jsonUnescape.hashCode()
        result = 31 * result + htmlParse.hashCode()
        result = 31 * result + themeParse.hashCode()
        result = 31 * result + total.hashCode()
        return result
    }

    public override fun toString(): String =
        "HighlightTimings(jsBridge=$jsBridge, jsonUnescape=$jsonUnescape, htmlParse=$htmlParse, themeParse=$themeParse, total=$total)"
}
