package dev.hossain.highlight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.engine.ThemedHighlightResult

/**
 * Creates and remembers a [HighlightEngine] scoped to the composition.
 *
 * When called inside a [HighlightThemeProvider], returns the **shared** engine that the provider
 * already manages — no extra WebView is created and no extra lifecycle handling is needed.
 *
 * When called **outside** [HighlightThemeProvider] (e.g. standalone usage without a provider),
 * creates a dedicated engine that is automatically destroyed (releasing the hidden WebView)
 * when the composable leaves the composition via [DisposableEffect].
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyCodeView(code: String) {
 *     val engine = rememberHighlightEngine()
 *     val highlighted by rememberHighlightedCode(code, "kotlin")
 *
 *     Text(text = highlighted ?: AnnotatedString(code))
 * }
 * ```
 *
 * Prefer [rememberHighlightedCode] for simple use cases. Use [rememberHighlightEngine]
 * directly only when you need lower-level control — for example, calling
 * [HighlightEngine.highlightBothThemes] or reading [HighlightEngine.isInitialized].
 */
@Composable
fun rememberHighlightEngine(): HighlightEngine {
    val sharedEngine = LocalHighlightEngine.current
    val context = LocalContext.current

    // Create a standalone engine only when there is no shared engine from HighlightThemeProvider.
    // Using sharedEngine as the remember key: if the provider is added/removed from the tree,
    // the standalone engine is (re)created or released accordingly.
    val standaloneEngine =
        remember(sharedEngine) {
            if (sharedEngine == null) HighlightEngine(context.applicationContext) else null
        }
    DisposableEffect(standaloneEngine) {
        onDispose { standaloneEngine?.destroy() }
    }

    return sharedEngine ?: standaloneEngine!!
}

/**
 * Pre-highlights [code] and remembers the resulting [AnnotatedString].
 *
 * Returns `null` while highlighting is in progress **or** if highlighting failed. Callers
 * should always render a plain-text fallback when the state is `null`.
 *
 * Re-runs automatically when [code], [language], or [theme] changes.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun CodeSnippet(code: String, language: String) {
 *     val highlighted by rememberHighlightedCode(code, language)
 *
 *     // highlighted is null while loading or if highlighting failed;
 *     // fall back to plain text in that case
 *     Text(
 *         text  = highlighted ?: AnnotatedString(code),
 *         style = TextStyle(fontFamily = FontFamily.Monospace),
 *     )
 * }
 * ```
 *
 * ## Theme creation
 *
 * Create [HighlightTheme] instances inside `remember` so CSS parsing does not repeat on every
 * recomposition:
 *
 * ```kotlin
 * val theme = remember(context) { HighlightTheme.tomorrow(context.applicationContext) }
 * val highlighted by rememberHighlightedCode(code, "kotlin", theme)
 * ```
 *
 * For light/dark toggling without re-highlighting, prefer [rememberHighlightedCodeBothThemes].
 *
 * @param code The source code to highlight.
 * @param language The Highlight.js language identifier (e.g. `"python"`, `"kotlin"`).
 * @param theme The theme to apply. Defaults to [LocalHighlightTheme].
 * @param onHighlightComplete Optional callback invoked with a [HighlightResult] when highlighting
 *   succeeds. Fires after the [State] is updated. Not called on failure.
 * @return A [State] holding the highlighted [AnnotatedString], or `null` while loading / on error.
 */
@Composable
fun rememberHighlightedCode(
    code: String,
    language: String,
    theme: HighlightTheme = LocalHighlightTheme.current,
    onHighlightComplete: ((HighlightResult) -> Unit)? = null,
): State<AnnotatedString?> {
    val engine = rememberHighlightEngine()
    val state = remember(code, language, theme) { mutableStateOf<AnnotatedString?>(null) }
    val latestCallback = rememberUpdatedState(onHighlightComplete)

    // In Android Studio Preview, WebView-based highlighting is not available.
    // Skip the LaunchedEffect so the engine is never called; callers will render a fallback.
    if (!LocalInspectionMode.current) {
        LaunchedEffect(code, language, theme) {
            state.value = null
            engine
                .highlight(code, language, theme)
                .onSuccess { result ->
                    state.value = result.annotated
                    latestCallback.value?.invoke(result)
                }
            // On failure: leave state.value = null; caller renders plain fallback
        }
    }

    return state
}

/**
 * Pre-highlights [code] for both light and dark themes in a single JS call.
 *
 * Unlike calling [rememberHighlightedCode] twice, this runs the JavaScript tokeniser **once**
 * and applies two colour maps to the same HTML output. Theme switching after the result is
 * available is instant — no re-highlighting is needed.
 *
 * Returns `null` while highlighting is in progress or if it failed.
 *
 * ## Usage inside a `HighlightThemeProvider`
 *
 * When called inside a [HighlightThemeProvider], light and dark themes are picked up
 * automatically from [LocalLightHighlightTheme] and [LocalDarkHighlightTheme]:
 *
 * ```kotlin
 * HighlightThemeProvider {
 *     val result by rememberHighlightedCodeBothThemes(code = code, language = "kotlin")
 *     val text = if (isDark) result?.dark else result?.light
 *     Text(text = text ?: AnnotatedString(code))
 * }
 * ```
 *
 * ## Usage outside a provider (explicit themes)
 *
 * ```kotlin
 * @Composable
 * fun CodeSnippet(code: String, isDark: Boolean) {
 *     val result by rememberHighlightedCodeBothThemes(
 *         code       = code,
 *         language   = "kotlin",
 *         lightTheme = rememberTomorrowTheme(),
 *         darkTheme  = rememberTomorrowNightTheme(),
 *     )
 *     val text = if (isDark) result?.dark else result?.light
 *     Text(text = text ?: AnnotatedString(code))
 * }
 * ```
 *
 * @param code The source code to highlight.
 * @param language The Highlight.js language identifier (e.g. `"python"`, `"kotlin"`).
 * @param lightTheme Theme to apply for the light variant. Defaults to [LocalLightHighlightTheme]
 *   (available inside [HighlightThemeProvider]). Create inside `remember` to avoid
 *   re-parsing CSS on every recomposition.
 * @param darkTheme Theme to apply for the dark variant. Defaults to [LocalDarkHighlightTheme]
 *   (available inside [HighlightThemeProvider]). Create inside `remember` to avoid
 *   re-parsing CSS on every recomposition.
 * @param onHighlightComplete Optional callback invoked with a [ThemedHighlightResult] when
 *   highlighting succeeds. Fires after the [State] is updated. Not called on failure. Use
 *   `result.durationMs` for timing, `result.light.spanStyles.size` for span count.
 * @return A [State] holding a [ThemedHighlightResult] with both variants (including
 *   [ThemedHighlightResult.durationMs] for timing), or `null` while loading / on error.
 */
@Composable
fun rememberHighlightedCodeBothThemes(
    code: String,
    language: String,
    lightTheme: HighlightTheme = LocalLightHighlightTheme.current,
    darkTheme: HighlightTheme = LocalDarkHighlightTheme.current,
    onHighlightComplete: ((ThemedHighlightResult) -> Unit)? = null,
): State<ThemedHighlightResult?> {
    val engine = rememberHighlightEngine()
    val state = remember(code, language, lightTheme, darkTheme) { mutableStateOf<ThemedHighlightResult?>(null) }
    val latestCallback = rememberUpdatedState(onHighlightComplete)

    // In Android Studio Preview, WebView-based highlighting is not available.
    // Skip the LaunchedEffect so the engine is never called; callers will render a fallback.
    if (!LocalInspectionMode.current) {
        LaunchedEffect(code, language, lightTheme, darkTheme) {
            state.value = null
            engine
                .highlightBothThemes(code, language, lightTheme, darkTheme)
                .onSuccess { result ->
                    state.value = result
                    latestCallback.value?.invoke(result)
                }
            // On failure: leave state.value = null; caller renders plain fallback
        }
    }

    return state
}
