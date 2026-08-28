package dev.hossain.highlight.ui.internal

import androidx.compose.ui.text.AnnotatedString
import dev.hossain.highlight.engine.HighlightTheme

/**
 * Holds the result of a syntax-highlight call together with the [language] and [theme] that
 * produced it. Stored as local state so composables can detect in-composition whether the
 * cached result is still valid for the current language/theme, eliminating the need for a
 * separate `LaunchedEffect` that resets state asynchronously.
 */
internal data class HighlightSnapshot(
    val annotated: AnnotatedString,
    val language: String,
    val theme: HighlightTheme,
)
