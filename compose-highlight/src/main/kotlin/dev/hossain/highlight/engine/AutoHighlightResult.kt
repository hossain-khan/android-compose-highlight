package dev.hossain.highlight.engine

import androidx.compose.runtime.Immutable
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
@Immutable
public class AutoHighlightResult(
    public val annotated: AnnotatedString,
    public val detectedLanguage: String,
    public val spanCount: Int,
    public val durationMs: Long,
    public val timings: HighlightTimings,
) {
    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AutoHighlightResult) return false
        return annotated == other.annotated &&
            detectedLanguage == other.detectedLanguage &&
            spanCount == other.spanCount &&
            durationMs == other.durationMs &&
            timings == other.timings
    }

    public override fun hashCode(): Int {
        var result = annotated.hashCode()
        result = 31 * result + detectedLanguage.hashCode()
        result = 31 * result + spanCount
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + timings.hashCode()
        return result
    }

    public override fun toString(): String =
        "AutoHighlightResult(annotated=$annotated, detectedLanguage=$detectedLanguage, spanCount=$spanCount, durationMs=$durationMs, timings=$timings)"
}
