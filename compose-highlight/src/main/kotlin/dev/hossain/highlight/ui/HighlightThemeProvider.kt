package dev.hossain.highlight.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import dev.hossain.highlight.engine.HighlightTheme

/**
 * CompositionLocal that provides the active [HighlightTheme] to all [SyntaxHighlightedCode]
 * composables in the subtree.
 *
 * Throws a descriptive error if accessed without a [HighlightThemeProvider] ancestor.
 */
val LocalHighlightTheme =
    staticCompositionLocalOf<HighlightTheme> {
        error(
            "No HighlightTheme provided. " +
                "Wrap your content in HighlightThemeProvider { ... }.",
        )
    }

/**
 * Provides [HighlightTheme] to all [SyntaxHighlightedCode] composables in [content].
 *
 * Automatically selects between [lightHighlightTheme] and [darkHighlightTheme] based on
 * the system dark mode setting. Call this once near the root of your composition (e.g. inside
 * your `setContent {}` block or at the top of your screen composable).
 *
 * ## Typical setup
 *
 * ```kotlin
 * // In MainActivity.kt or your root composable:
 * HighlightThemeProvider(
 *     lightHighlightTheme = HighlightTheme.tomorrow(LocalContext.current),
 *     darkHighlightTheme  = HighlightTheme.atomOneDark(LocalContext.current),
 * ) {
 *     // All SyntaxHighlightedCode composables inside here will use
 *     // the correct theme automatically.
 *     MyAppContent()
 * }
 * ```
 *
 * ## Manual override
 *
 * Pass `darkTheme = true/false` to force a specific mode regardless of system setting:
 *
 * ```kotlin
 * HighlightThemeProvider(
 *     darkTheme           = userPrefersDark,
 *     lightHighlightTheme = HighlightTheme.tomorrow(LocalContext.current),
 *     darkHighlightTheme  = HighlightTheme.tomorrowNight(LocalContext.current),
 * ) { ... }
 * ```
 *
 * @param darkTheme Whether to use the dark theme. Defaults to [isSystemInDarkTheme].
 * @param lightHighlightTheme The theme to use in light mode.
 * @param darkHighlightTheme The theme to use in dark mode.
 * @param content The composable content to which the theme is provided.
 */
@Composable
fun HighlightThemeProvider(
    darkTheme: Boolean = isSystemInDarkTheme(),
    lightHighlightTheme: HighlightTheme = HighlightTheme.tomorrow(LocalContext.current),
    darkHighlightTheme: HighlightTheme = HighlightTheme.tomorrowNight(LocalContext.current),
    content: @Composable () -> Unit,
) {
    val activeTheme = if (darkTheme) darkHighlightTheme else lightHighlightTheme
    CompositionLocalProvider(LocalHighlightTheme provides activeTheme) {
        content()
    }
}
