package dev.hossain.highlight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.hossain.highlight.engine.HighlightTheme

/**
 * Creates and remembers the built-in Base16 Tomorrow (light) [HighlightTheme].
 *
 * Backed by a precompiled color map generated at build time, so no CSS parsing happens at
 * runtime and no [android.content.Context] is needed.
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
fun rememberTomorrowTheme(): HighlightTheme = remember { HighlightTheme.tomorrow() }

/**
 * Creates and remembers the built-in Base16 Tomorrow Night (dark) [HighlightTheme].
 *
 * Backed by a precompiled color map generated at build time, so no CSS parsing happens at
 * runtime and no [android.content.Context] is needed.
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
fun rememberTomorrowNightTheme(): HighlightTheme = remember { HighlightTheme.tomorrowNight() }

/**
 * Creates and remembers the built-in Atom One Dark [HighlightTheme].
 *
 * Backed by a precompiled color map generated at build time, so no CSS parsing happens at
 * runtime and no [android.content.Context] is needed.
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
fun rememberAtomOneDarkTheme(): HighlightTheme = remember { HighlightTheme.atomOneDark() }

/**
 * Creates and remembers the built-in Atom One Light [HighlightTheme].
 *
 * Backed by a precompiled color map generated at build time, so no CSS parsing happens at
 * runtime and no [android.content.Context] is needed.
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
fun rememberAtomOneLightTheme(): HighlightTheme = remember { HighlightTheme.atomOneLight() }
