package dev.hossain.highlight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import dev.hossain.highlight.engine.HighlightException
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.internal.HighlightSnapshot
import dev.hossain.highlight.ui.internal.applySnapshotSpans
import kotlinx.coroutines.delay

/**
 * Runs the debounce + syntax-highlight pipeline for streaming / real-time code (such as LLM responses
 * or live terminal logs) and returns an [AnnotatedString] ready for rendering.
 *
 * This composable is marked **experimental** ([ExperimentalHighlightApi]). Call sites must
 * opt in with `@OptIn(ExperimentalHighlightApi::class)` or propagate the annotation.
 *
 * Unlike [rememberHighlightedCode], which cancels and resets to `null` on every string change,
 * this function preserves syntax highlighting during active streaming using span transfer
 * ([applySnapshotSpans]):
 * 1. **Immediate render (0 ms latency):** As new tokens arrive, the full string is returned immediately.
 * 2. **Continuous styling:** Spans from the last completed highlight run are carried forward onto
 *    the unchanged prefix of the new text, keeping existing lines colored.
 * 3. **Debounced engine calls:** Highlighting is delayed by [debounceMs], coalescing rapid token emissions
 *    into a single highlight engine run when generation pauses or finishes.
 *
 * ## Usage
 *
 * ```kotlin
 * HighlightThemeProvider(
 *     lightHighlightTheme = rememberTomorrowLightTheme(),
 *     darkHighlightTheme  = rememberAtomOneDarkTheme(),
 * ) {
 *     val highlightedCode = rememberStreamingHighlightedCode(
 *         code     = streamingLlmText,
 *         language = "kotlin",
 *     )
 *     Text(text = highlightedCode)
 * }
 * ```
 *
 * @param code The current source code string (actively growing or static).
 * @param language Highlight.js language identifier (e.g. `"kotlin"`, `"python"`, `"json"`).
 * @param theme The highlight theme to apply. Defaults to [LocalHighlightTheme].
 * @param debounceMs Milliseconds to wait after the last text change before triggering a new
 *   highlight call. Defaults to [StreamingSyntaxHighlightedCodeDefaults.DEBOUNCE_MS] (200 ms).
 * @param onHighlightComplete Optional callback invoked with a [HighlightResult] when a highlight
 *   cycle completes successfully. Fires after the snapshot is updated.
 * @param onError Optional callback invoked with the [HighlightException] when highlighting fails.
 *   Falls back to plain text on failure - this callback is purely observational.
 * @return An [AnnotatedString] with syntax highlighting applied and previous spans preserved
 *   across stream updates.
 */
@ExperimentalHighlightApi
@Composable
fun rememberStreamingHighlightedCode(
    code: String,
    language: String,
    theme: HighlightTheme = LocalHighlightTheme.current,
    debounceMs: Long = StreamingSyntaxHighlightedCodeDefaults.DEBOUNCE_MS,
    onHighlightComplete: ((HighlightResult) -> Unit)? = null,
    onError: ((HighlightException) -> Unit)? = null,
): AnnotatedString {
    if (LocalInspectionMode.current) {
        return AnnotatedString(code)
    }

    val engine = rememberHighlightEngine()
    var highlighted by remember { mutableStateOf<HighlightSnapshot?>(null) }
    val currentDebounceMs by rememberUpdatedState(debounceMs)
    val currentOnHighlightComplete by rememberUpdatedState(onHighlightComplete)
    val currentOnError by rememberUpdatedState(onError)

    LaunchedEffect(code, language, theme) {
        if (currentDebounceMs > 0L) {
            delay(currentDebounceMs)
        }
        engine
            .highlight(code, language, theme)
            .onSuccess { result ->
                highlighted = HighlightSnapshot(result.annotated, language, theme)
                currentOnHighlightComplete?.invoke(result)
            }.onFailure { error ->
                highlighted = null
                (error as? HighlightException)?.let { currentOnError?.invoke(it) }
            }
    }

    val snapshot = highlighted
    return when {
        snapshot == null || snapshot.language != language || snapshot.theme != theme -> {
            AnnotatedString(code)
        }

        snapshot.annotated.text == code -> {
            snapshot.annotated
        }

        else -> {
            applySnapshotSpans(snapshot.annotated, code)
        }
    }
}
