package dev.hossain.highlight.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import dev.hossain.highlight.engine.HighlightEngine
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
 * CompositionLocal that provides the light [HighlightTheme] configured in [HighlightThemeProvider].
 *
 * Use this (along with [LocalDarkHighlightTheme]) when you need both variants in a single
 * composable — for example, inside [rememberHighlightedCodeBothThemes].
 *
 * Throws a descriptive error if accessed without a [HighlightThemeProvider] ancestor.
 */
val LocalLightHighlightTheme =
    staticCompositionLocalOf<HighlightTheme> {
        error(
            "No light HighlightTheme provided. " +
                "Wrap your content in HighlightThemeProvider { ... }.",
        )
    }

/**
 * CompositionLocal that provides the dark [HighlightTheme] configured in [HighlightThemeProvider].
 *
 * Use this (along with [LocalLightHighlightTheme]) when you need both variants in a single
 * composable — for example, inside [rememberHighlightedCodeBothThemes].
 *
 * Throws a descriptive error if accessed without a [HighlightThemeProvider] ancestor.
 */
val LocalDarkHighlightTheme =
    staticCompositionLocalOf<HighlightTheme> {
        error(
            "No dark HighlightTheme provided. " +
                "Wrap your content in HighlightThemeProvider { ... }.",
        )
    }

/**
 * Internal CompositionLocal that carries the shared [HighlightEngine] provided by
 * [HighlightThemeProvider]. Defaults to `null` so that [rememberHighlightEngine] can detect
 * whether it is inside a provider and fall back to creating a standalone engine.
 */
internal val LocalHighlightEngine =
    staticCompositionLocalOf<HighlightEngine?> { null }

/**
 * Provides [HighlightTheme] and a shared [HighlightEngine] to all [SyntaxHighlightedCode]
 * composables in [content].
 *
 * Automatically selects between [lightHighlightTheme] and [darkHighlightTheme] based on
 * the system dark mode setting. Call this once near the root of your composition (e.g. inside
 * your `setContent {}` block or at the top of your screen composable).
 *
 * A single [HighlightEngine] (and thus a single hidden WebView) is created for the entire
 * subtree and destroyed when this composable leaves the composition. This means screens with
 * multiple [SyntaxHighlightedCode] blocks share one WebView instead of creating one per block,
 * saving ~200 ms warm-up time and ~2–4 MB RAM per extra block.
 *
 * ## Typical setup
 *
 * Prefer [rememberTomorrowTheme] and [rememberTomorrowNightTheme] as the default pattern so theme
 * instances stay stable across recompositions:
 *
 * ```kotlin
 * // In MainActivity.kt or your root composable:
 * HighlightThemeProvider(
 *     lightHighlightTheme = rememberTomorrowTheme(),
 *     darkHighlightTheme  = rememberTomorrowNightTheme(),
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
 *     lightHighlightTheme = rememberTomorrowTheme(),
 *     darkHighlightTheme  = rememberTomorrowNightTheme(),
 * ) { ... }
 * ```
 *
 * ## Optional: WebView pre-warming
 *
 * The hidden WebView initializes lazily on the first highlight call. If you want to reduce
 * that first-call latency further, you can pre-warm the WebView renderer process by calling
 * `WebViewCompat.startUpWebView()` (androidx.webkit 1.16+) as early as possible — ideally
 * in `Application.onCreate()` before any Activity is created:
 *
 * ```kotlin
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         // Pre-warm the WebView renderer process so it is ready when the first
 *         // HighlightThemeProvider is composed. Best-effort — safe to ignore failures.
 *         runCatching {
 *             WebViewCompat.startUpWebView(
 *                 applicationContext,
 *                 WebViewStartUpConfig.Builder(mainExecutor).build(),
 *                 WebViewOutcomeReceiver { /* no-op */ },
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * @param darkTheme Whether to use the dark theme. Defaults to [isSystemInDarkTheme].
 * @param lightHighlightTheme The theme to use in light mode. Also provided via
 *   [LocalLightHighlightTheme] to the subtree.
 * @param darkHighlightTheme The theme to use in dark mode. Also provided via
 *   [LocalDarkHighlightTheme] to the subtree.
 * @param content The composable content to which the theme is provided.
 */
@Composable
fun HighlightThemeProvider(
    darkTheme: Boolean = isSystemInDarkTheme(),
    lightHighlightTheme: HighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme: HighlightTheme = rememberTomorrowNightTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // One shared engine for the entire subtree — one WebView, not one per code block.
    val engine = remember { HighlightEngine(context.applicationContext) }
    DisposableEffect(engine) {
        onDispose { engine.destroy() }
    }
    val activeTheme = if (darkTheme) darkHighlightTheme else lightHighlightTheme
    CompositionLocalProvider(
        LocalHighlightTheme provides activeTheme,
        LocalLightHighlightTheme provides lightHighlightTheme,
        LocalDarkHighlightTheme provides darkHighlightTheme,
        LocalHighlightEngine provides engine,
    ) {
        content()
    }
}
