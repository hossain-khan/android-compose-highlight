package dev.hossain.highlight.engine

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString

/**
 * Holds both light and dark [AnnotatedString] results from a single highlight call.
 * Used by [HighlightEngine.highlightBothThemes].
 *
 * ```kotlin
 * val result by rememberHighlightedCodeBothThemes(
 *     code       = code,
 *     language   = "kotlin",
 *     lightTheme = rememberTomorrowLightTheme(),
 *     darkTheme  = rememberTomorrowNightTheme(),
 * )
 * val text = if (isDark) result?.dark else result?.light
 * Text(text = text ?: AnnotatedString(code))
 * ```
 *
 * @property light Syntax-highlighted [AnnotatedString] styled with the light theme.
 * @property dark Syntax-highlighted [AnnotatedString] styled with the dark theme.
 * @property durationMs Pure highlight time in milliseconds - covers the JS call and a single
 *   HTML conversion pass (light and dark outputs are produced together in one pass). Excludes
 *   coroutine-scheduling overhead.
 * @property timings Per-layer timing breakdown for this highlight call. Always populated.
 *   See [HighlightTimings] for the full stage breakdown.
 */
@Immutable
public class ThemedHighlightResult(
    public val light: AnnotatedString,
    public val dark: AnnotatedString,
    public val durationMs: Long,
    public val timings: HighlightTimings,
) {
    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThemedHighlightResult) return false
        return light == other.light &&
            dark == other.dark &&
            durationMs == other.durationMs &&
            timings == other.timings
    }

    public override fun hashCode(): Int {
        var result = light.hashCode()
        result = 31 * result + dark.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + timings.hashCode()
        return result
    }

    public override fun toString(): String = "ThemedHighlightResult(light=$light, dark=$dark, durationMs=$durationMs, timings=$timings)"
}
