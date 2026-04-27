package dev.composehighlight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import dev.composehighlight.engine.HighlightEngine
import dev.composehighlight.engine.HighlightTheme

/**
 * Creates and remembers a [HighlightEngine] scoped to the composition.
 *
 * PRD fix: [DisposableEffect] calls [HighlightEngine.destroy] when the composable leaves
 * the composition, releasing the hidden WebView resources.
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
 * Returns `null` while the async highlighting is in progress (i.e. on the first frame).
 * Re-runs when [code], [language], or [theme] changes.
 *
 * @param code The source code to highlight.
 * @param language The Highlight.js language identifier (e.g. `"python"`, `"kotlin"`).
 * @param theme The theme to apply. Defaults to [LocalHighlightTheme].
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
