package dev.hossain.highlight.engine

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
data class ThemedHighlightResult(
    val light: AnnotatedString,
    val dark: AnnotatedString,
    val durationMs: Long,
    val timings: HighlightTimings,
)
