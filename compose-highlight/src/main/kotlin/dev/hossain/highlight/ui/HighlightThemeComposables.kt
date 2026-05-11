package dev.hossain.highlight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.hossain.highlight.engine.HighlightTheme

/**
 * Creates and remembers the built-in Base16 Tomorrow (light) [HighlightTheme].
 *
 * Resolves [LocalContext] internally so callers do not need to pass a [android.content.Context].
 * CSS parsing is performed at most once (lazily on first use) and cached.
 *
 * ```kotlin
 * HighlightThemeProvider(
 *     lightHighlightTheme = rememberTomorrowTheme(),
 *     darkHighlightTheme  = rememberTomorrowNightTheme(),
 * ) { ... }
 * ```
 *
 * @return A stable [HighlightTheme] instance remembered across recompositions.
 */
@Composable
fun rememberTomorrowTheme(): HighlightTheme {
    val context = LocalContext.current
    return remember { HighlightTheme.tomorrow(context.applicationContext) }
}

/**
 * Creates and remembers the built-in Base16 Tomorrow Night (dark) [HighlightTheme].
 *
 * Resolves [LocalContext] internally so callers do not need to pass a [android.content.Context].
 * CSS parsing is performed at most once (lazily on first use) and cached.
 *
 * ```kotlin
 * val result by rememberHighlightedCodeBothThemes(
 *     code       = code,
 *     language   = "kotlin",
 *     lightTheme = rememberTomorrowTheme(),
 *     darkTheme  = rememberTomorrowNightTheme(),
 * )
 * ```
 *
 * @return A stable [HighlightTheme] instance remembered across recompositions.
 */
@Composable
fun rememberTomorrowNightTheme(): HighlightTheme {
    val context = LocalContext.current
    return remember { HighlightTheme.tomorrowNight(context.applicationContext) }
}

/**
 * Creates and remembers the built-in Atom One Dark [HighlightTheme].
 *
 * Resolves [LocalContext] internally so callers do not need to pass a [android.content.Context].
 * CSS parsing is performed at most once (lazily on first use) and cached.
 *
 * ```kotlin
 * HighlightThemeProvider(
 *     darkHighlightTheme = rememberAtomOneDarkTheme(),
 * ) { ... }
 * ```
 *
 * @return A stable [HighlightTheme] instance remembered across recompositions.
 */
@Composable
fun rememberAtomOneDarkTheme(): HighlightTheme {
    val context = LocalContext.current
    return remember { HighlightTheme.atomOneDark(context.applicationContext) }
}

/**
 * Creates and remembers the built-in Atom One Light [HighlightTheme].
 *
 * Resolves [LocalContext] internally so callers do not need to pass a [android.content.Context].
 * CSS parsing is performed at most once (lazily on first use) and cached.
 *
 * ```kotlin
 * HighlightThemeProvider(
 *     lightHighlightTheme = rememberAtomOneLightTheme(),
 * ) { ... }
 * ```
 *
 * @return A stable [HighlightTheme] instance remembered across recompositions.
 */
@Composable
fun rememberAtomOneLightTheme(): HighlightTheme {
    val context = LocalContext.current
    return remember { HighlightTheme.atomOneLight(context.applicationContext) }
}
