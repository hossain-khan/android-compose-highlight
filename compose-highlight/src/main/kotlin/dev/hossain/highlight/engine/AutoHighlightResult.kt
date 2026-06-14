package dev.hossain.highlight.engine

import androidx.compose.ui.text.AnnotatedString

/**
 * Result of a successful [HighlightEngine.highlightAuto] call.
 *
 * This mirrors [HighlightResult], but stores the language detected by Highlight.js instead of a
 * caller-supplied language. Automatic detection is convenient for unknown snippets, but it is
 * usually slower and less accurate than passing an explicit language.
 *
 * ## Usage
 *
 * ```kotlin
 * engine.highlightAuto(code, theme).onSuccess { result ->
 *     SyntaxHighlightedCode(
 *         code = code,
 *         language = result.detectedLanguage,
 *         theme = theme,
 *     )
 *
 *     log("Detected language: ${result.detectedLanguage}")
 *     log("Highlight took ${result.durationMs} ms")
 * }
 * ```
 *
 * `detectedLanguage` may be an empty string if Highlight.js could not determine a language.
 *
 * @property annotated The syntax-highlighted [AnnotatedString] ready for rendering.
 * @property detectedLanguage Highlight.js detected language identifier, or an empty string when
 *   auto-detection could not determine one.
 * @property spanCount Number of syntax-highlighted token spans. A value of `0` indicates
 *   that no tokens were found. Excludes the base theme `.hljs` span if one was applied.
 * @property durationMs Pure highlight time in milliseconds. Equals
 *   `timings.total.inWholeMilliseconds`.
 * @property timings Per-layer timing breakdown for this highlight call. Always populated.
 */
data class AutoHighlightResult(
    val annotated: AnnotatedString,
    val detectedLanguage: String,
    val spanCount: Int,
    val durationMs: Long,
    val timings: HighlightTimings,
)
