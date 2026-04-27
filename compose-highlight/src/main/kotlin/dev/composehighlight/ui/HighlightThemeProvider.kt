package dev.composehighlight.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import dev.composehighlight.engine.HighlightTheme

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
 * the system dark mode setting.
 *
 * @param darkTheme Whether to use the dark theme. Defaults to [isSystemInDarkTheme].
 * @param lightHighlightTheme The theme to use in light mode.
 * @param darkHighlightTheme The theme to use in dark mode.
 *
 * PRD fix: renamed `darkTheme: HighlightTheme` param to `darkHighlightTheme` to avoid
 * name collision with the `darkTheme: Boolean` parameter.
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
