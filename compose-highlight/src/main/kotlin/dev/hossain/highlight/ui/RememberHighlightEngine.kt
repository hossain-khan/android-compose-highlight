package dev.hossain.highlight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.ui.internal.LocalHighlightEngine

/**
 * Creates and remembers a [HighlightEngine] scoped to the composition.
 *
 * When called inside a [HighlightThemeProvider], returns the **shared** engine that the provider
 * already manages - no extra WebView is created and no extra lifecycle handling is needed.
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
 * directly only when you need lower-level control - for example, calling
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
