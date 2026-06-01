package dev.hossain.highlight.ui.internal

import androidx.compose.runtime.staticCompositionLocalOf
import dev.hossain.highlight.engine.HighlightEngine

/**
 * Internal CompositionLocal that carries the shared [HighlightEngine] provided by
 * `HighlightThemeProvider`. Defaults to `null` so that `rememberHighlightEngine` can detect
 * whether it is inside a provider and fall back to creating a standalone engine.
 */
internal val LocalHighlightEngine =
    staticCompositionLocalOf<HighlightEngine?> { null }
