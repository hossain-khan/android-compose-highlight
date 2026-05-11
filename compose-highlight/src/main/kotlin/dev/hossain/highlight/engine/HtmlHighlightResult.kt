package dev.hossain.highlight.engine

/**
 * Result of a successful [HighlightEngine.highlightToHtml] call.
 *
 * Pairs the raw HTML output with the time taken to produce it, so callers that work
 * directly with HTML (e.g. custom renderers, harnesses measuring JS round-trip time)
 * get timing data without having to measure it manually.
 *
 * ## Usage
 *
 * ```kotlin
 * engine.highlightToHtml("val x = 42", "kotlin").onSuccess { result ->
 *     // Raw HTML with <span class="hljs-*"> tokens
 *     renderHtml(result.html)
 *
 *     // JS round-trip time (ms)
 *     log("highlightToHtml took ${result.durationMs} ms")
 * }
 * ```
 *
 * @property html The raw HTML string produced by Highlight.js, containing
 *   `<span class="hljs-*">` tokens that can be styled by a CSS theme.
 * @property durationMs JavaScript round-trip time in milliseconds — measured from immediately
 *   before `evaluateJavascript()` is called (after WebView is ready and the internal mutex is
 *   acquired) through receipt of the result. Excludes WebView warm-up and queue-wait time.
 */
data class HtmlHighlightResult(
    val html: String,
    val durationMs: Long,
)
