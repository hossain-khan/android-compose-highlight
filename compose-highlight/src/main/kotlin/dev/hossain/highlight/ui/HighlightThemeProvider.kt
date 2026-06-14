package dev.hossain.highlight.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.internal.LocalHighlightEngine

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
 * composable - for example, inside [rememberHighlightedCodeBothThemes].
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
 * composable - for example, inside [rememberHighlightedCodeBothThemes].
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
 * Provides [HighlightTheme] and a shared [HighlightEngine] to all [SyntaxHighlightedCode]
 * composables in [content].
 *
 * Automatically selects between [lightHighlightTheme] and [darkHighlightTheme] based on
 * the system dark mode setting. Place this at the top of your screen composable (or inside your
 * `setContent {}` block) so that all code blocks on the screen share the same engine.
 *
 * A single [HighlightEngine] (and thus a single hidden WebView) is created for the entire
 * subtree and destroyed when this composable leaves the composition. This means all
 * [SyntaxHighlightedCode] blocks inside share one WebView instead of creating one per block.
 * Measured on a Pixel 8 Pro debug build (cold start, 17 blocks): with provider takes ~224 ms
 * total and ~15 MB heap vs ~866 ms and ~34 MB heap without - roughly 4x faster and 55% less heap.
 * Each extra standalone engine adds ~37 ms average latency and ~1-2 MB heap while on-screen.
 *
 * **1 provider = 1 WebView.** Place it above all the code blocks you want to share.
 *
 * ## Typical setup
 *
 * Prefer [rememberTomorrowTheme] and [rememberTomorrowNightTheme] as the default pattern so theme
 * instances stay stable across recompositions:
 *
 * ```kotlin
 * // At the top of your screen composable:
 * HighlightThemeProvider(
 *     lightHighlightTheme = rememberTomorrowTheme(),
 *     darkHighlightTheme  = rememberTomorrowNightTheme(),
 * ) {
 *     // All SyntaxHighlightedCode composables inside here share 1 WebView
 *     // and use the correct theme automatically.
 *     MyScreenContent()
 * }
 * ```
 *
 * ## Without a provider
 *
 * Without `HighlightThemeProvider`, each [SyntaxHighlightedCode] (or [rememberHighlightEngine]
 * call) creates its own [HighlightEngine] and hidden WebView. For a screen with 17 code blocks
 * that measured ~866 ms total highlight time and ~34 MB heap vs ~224 ms and ~15 MB with a
 * provider (Pixel 8 Pro, cold start, debug build). Wrapping the screen in a single provider
 * reduces this to 1 WebView regardless of how many blocks are inside.
 *
 * ## Multiple screens
 *
 * For apps with multiple screens, place one provider at the top of each screen composable.
 * Each screen's engine is automatically destroyed when the screen leaves the composition,
 * so you never hold a WebView alive for screens not currently shown.
 *
 * ```kotlin
 * // Screen A
 * HighlightThemeProvider(...) { ScreenAContent() }  // 1 WebView, destroyed when A is gone
 *
 * // Screen B
 * HighlightThemeProvider(...) { ScreenBContent() }  // 1 WebView, destroyed when B is gone
 * ```
 *
 * ## Manual dark/light override
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
 * `WebViewCompat.startUpWebView()` (androidx.webkit 1.16+) as early as possible - ideally
 * in `Application.onCreate()` before any Activity is created:
 *
 * ```kotlin
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         // Pre-warm the WebView renderer process so it is ready when the first
 *         // HighlightThemeProvider is composed. Best-effort - safe to ignore failures.
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
 * @param content The composable content to which the theme and shared engine are provided.
 */
@Composable
fun HighlightThemeProvider(
    darkTheme: Boolean = isSystemInDarkTheme(),
    lightHighlightTheme: HighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme: HighlightTheme = rememberTomorrowNightTheme(),
    content: @Composable () -> Unit,
) {
    val activeTheme = if (darkTheme) darkHighlightTheme else lightHighlightTheme
    if (LocalInspectionMode.current) {
        CompositionLocalProvider(
            LocalHighlightTheme provides activeTheme,
            LocalLightHighlightTheme provides lightHighlightTheme,
            LocalDarkHighlightTheme provides darkHighlightTheme,
        ) {
            content()
        }
    } else {
        val context = LocalContext.current
        // One shared engine for the entire subtree - one WebView, not one per code block.
        val engine = remember { HighlightEngine(context.applicationContext) }
        DisposableEffect(engine) {
            onDispose { engine.destroy() }
        }
        CompositionLocalProvider(
            LocalHighlightTheme provides activeTheme,
            LocalLightHighlightTheme provides lightHighlightTheme,
            LocalDarkHighlightTheme provides darkHighlightTheme,
            LocalHighlightEngine provides engine,
        ) {
            content()
        }
    }
}

/**
 * Creates and remembers the built-in Base16 Tomorrow (light) [HighlightTheme].
 *
 * Backed by a precompiled color map generated at build time, so no CSS parsing happens at
 * runtime and no [android.content.Context] is needed.
 *
 * ```kotlin
 * HighlightThemeProvider(
 *     lightHighlightTheme = rememberTomorrowTheme(),
 *     darkHighlightTheme  = rememberAtomOneDarkTheme(),
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

@Preview(showBackground = true)
@Composable
private fun HighlightThemeProviderPreview() {
    HighlightThemeProvider(
        lightHighlightTheme = rememberTomorrowTheme(),
        darkHighlightTheme = rememberTomorrowNightTheme(),
    ) {
        SyntaxHighlightedCode(
            code = "val list = listOf(1, 2, 3)\nval sum = list.sum()",
            language = "kotlin",
        )
    }
}
