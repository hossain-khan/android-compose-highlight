package dev.hossain.highlight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightTheme

/**
 * Creates and remembers a [HighlightEngine] scoped to the composition.
 *
 * The engine is created once and automatically destroyed (releasing the hidden WebView)
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
 * directly only when you need lower-level control (e.g. calling [HighlightEngine.highlightBothThemes]).
 */
@Composable
fun rememberHighlightEngine(): HighlightEngine {
    val context = LocalContext.current
    val engine = remember { HighlightEngine(context) }
    DisposableEffect(engine) {
        onDispose { engine.destroy() }
    }
    return engine
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
 * @param code The source code to highlight.
 * @param language The Highlight.js language identifier (e.g. `"python"`, `"kotlin"`).
 * @param theme The theme to apply. Defaults to [LocalHighlightTheme].
 * @return A [State] holding the highlighted [AnnotatedString], or `null` while loading / on error.
 */
@Composable
fun rememberHighlightedCode(
    code: String,
    language: String,
    theme: HighlightTheme = LocalHighlightTheme.current,
): State<AnnotatedString?> {
    val engine = rememberHighlightEngine()
    val state = remember(code, language, theme) { mutableStateOf<AnnotatedString?>(null) }

    LaunchedEffect(code, language, theme) {
        state.value = null
        engine
            .highlight(code, language, theme)
            .onSuccess { state.value = it }
        // On failure: leave state.value = null; caller renders plain fallback
    }

    return state
}
