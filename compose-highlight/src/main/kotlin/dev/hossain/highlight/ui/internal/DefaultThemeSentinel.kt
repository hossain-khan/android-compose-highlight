package dev.hossain.highlight.ui.internal

import dev.hossain.highlight.engine.HighlightTheme

/**
 * Sentinel [HighlightTheme] used by [dev.hossain.highlight.ui.SyntaxHighlightedCode] to detect
 * when the caller did not supply an explicit theme parameter.
 *
 * When this sentinel is present, [dev.hossain.highlight.ui.SyntaxHighlightedCode] automatically
 * resolves the active theme from [dev.hossain.highlight.ui.LocalHighlightTheme] and enables
 * dual-theme pre-highlighting when a [dev.hossain.highlight.ui.HighlightThemeProvider] ancestor
 * is present.
 */
internal val DefaultThemeSentinel: HighlightTheme =
    HighlightTheme.fromColorMap(
        name = "default-theme-sentinel",
        colorMap = emptyMap(),
    )
