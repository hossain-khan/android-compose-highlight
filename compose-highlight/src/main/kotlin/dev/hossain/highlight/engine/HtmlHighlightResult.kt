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
 * @property durationMs Pure highlight time in milliseconds — from the start of the
 *   [HighlightEngine.highlightToHtml] call through the JavaScript round-trip.
 *   Excludes coroutine scheduling overhead.
 */
data class HtmlHighlightResult(
    val html: String,
    val durationMs: Long,
)
