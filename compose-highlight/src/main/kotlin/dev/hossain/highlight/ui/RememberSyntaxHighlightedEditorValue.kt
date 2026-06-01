package dev.hossain.highlight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import dev.hossain.highlight.engine.HighlightException
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.internal.applySnapshotSpans
import kotlinx.coroutines.delay

/**
 * Holds the result of a syntax-highlight call together with the [language] and [theme] that
 * produced it. Stored as local state so the composable can detect in-composition whether the
 * cached result is still valid for the current language/theme, eliminating the need for a
 * separate `LaunchedEffect` that resets state asynchronously.
 */
private data class HighlightSnapshot(
    val annotated: AnnotatedString,
    val language: String,
    val theme: HighlightTheme,
)

/**
 * Runs the debounce + syntax-highlight pipeline for a live code editor and returns the
 * display [TextFieldValue] ready to pass directly to `BasicTextField` (or any other text
 * field that accepts [TextFieldValue]).
 *
 * This is the lower-level counterpart to [SyntaxHighlightedTextEditor]: it owns the engine
 * call, the debounce window, and the span-clipping logic, but does **not** render anything.
 * Callers drive their own layout, which makes it easy to:
 * - wrap an `OutlinedTextField` or a third-party editor with syntax highlighting
 * - test the highlight pipeline in isolation without a `Surface`/`BasicTextField` in the tree
 *
 * The returned [TextFieldValue] is recomputed each time a new highlight result arrives.
 * Between updates the previous spans are transferred using a prefix/suffix analysis: spans on
 * unchanged text before the edit are kept as-is, spans on unchanged text after the edit (lines
 * below) are shifted by the length delta, and spans in the edited region are dropped. This means
 * syntax colors on all lines above and below a mid-text edit remain correct during the debounce
 * window, and only the characters being typed are temporarily unstyled.
 *
 * ## Usage - standalone (with BasicTextField)
 *
 * ```kotlin
 * var editorValue by remember { mutableStateOf(TextFieldValue("fun hello() = println(\"Hello!\")")) }
 *
 * HighlightThemeProvider(
 *     lightHighlightTheme = HighlightTheme.tomorrow(),
 *     darkHighlightTheme  = HighlightTheme.tomorrowNight(),
 * ) {
 *     val displayValue = rememberSyntaxHighlightedEditorValue(
 *         value    = editorValue,
 *         language = "kotlin",
 *     )
 *     BasicTextField(
 *         value         = displayValue,
 *         onValueChange = { editorValue = it },
 *     )
 * }
 * ```
 *
 * ## Usage - with SyntaxHighlightedTextEditor (preferred for most cases)
 *
 * For the common case of a themed, bordered editor use [SyntaxHighlightedTextEditor] directly.
 * It calls this function internally and adds the `Surface` + padding + test-tag wrapper.
 *
 * @param value The current [TextFieldValue], including text, cursor position, and selection.
 * @param language Highlight.js language identifier (e.g. `"kotlin"`, `"python"`, `"sql"`).
 * @param theme The highlight theme to apply. Defaults to [LocalHighlightTheme].
 * @param debounceMs Milliseconds to wait after the last keystroke before triggering a new
 *   highlight call. Defaults to 150 ms. If `debounceMs` changes, the new value is used on the
 *   next keystroke. The currently running debounce window is unaffected (the original delay
 *   completes with its captured-at-suspension value).
 * @param onHighlightComplete Optional callback invoked each time a highlight cycle completes
 *   successfully. Receives the resulting [AnnotatedString] with syntax spans applied. Useful
 *   for testing (wait until the first result arrives) and for observing the highlight output
 *   without owning the editor's text state.
 * @param onError Optional callback invoked with the [HighlightException] when a highlight cycle
 *   fails. The editor falls back to plain text on failure regardless of whether this callback
 *   is set - it is purely observational. Use it to log failures, show a snackbar, or record
 *   analytics. Possible failure types: [HighlightException.Timeout],
 *   [HighlightException.JsExecutionFailed], [HighlightException.WebViewInitFailed],
 *   [HighlightException.HtmlParseFailed].
 * @return The [TextFieldValue] with syntax-highlight spans applied, preserving
 *   the original cursor position and selection. Falls back to plain text while a highlight
 *   result is in flight, on error, or when the language/theme has just changed. Because this
 *   function is a non-restartable composable (returns a non-Unit type), all internal `State`
 *   reads (including the highlight snapshot) automatically subscribe the caller's recompose
 *   scope - callers should use it like any other composable helper (`val x = rememberXxx()`).
 */
@ExperimentalHighlightApi
@Composable
fun rememberSyntaxHighlightedEditorValue(
    value: TextFieldValue,
    language: String,
    theme: HighlightTheme = LocalHighlightTheme.current,
    debounceMs: Long = SyntaxHighlightedTextEditorDefaults.DEBOUNCE_MS,
    onHighlightComplete: ((AnnotatedString) -> Unit)? = null,
    onError: ((HighlightException) -> Unit)? = null,
): TextFieldValue {
    val engine = rememberHighlightEngine()
    var highlighted by remember { mutableStateOf<HighlightSnapshot?>(null) }
    // rememberUpdatedState ensures a changed debounceMs or callback is used by the running
    // effect without restarting it (restarting would reset the debounce window mid-keystroke).
    val currentDebounceMs by rememberUpdatedState(debounceMs)
    val currentOnHighlightComplete by rememberUpdatedState(onHighlightComplete)
    val currentOnError by rememberUpdatedState(onError)

    // Re-highlight with debounce whenever the text, language, or theme changes.
    // LaunchedEffect cancels the previous coroutine on each change, so rapid keystrokes
    // naturally coalesce into a single highlight call after the user pauses.
    LaunchedEffect(value.text, language, theme) {
        delay(currentDebounceMs)
        engine
            .highlight(value.text, language, theme)
            .onSuccess { result ->
                highlighted = HighlightSnapshot(result.annotated, language, theme)
                currentOnHighlightComplete?.invoke(result.annotated)
            }.onFailure { error ->
                highlighted = null
                (error as? HighlightException)?.let { currentOnError?.invoke(it) }
            }
    }

    // Merge highlight spans into the TextFieldValue while preserving cursor and selection.
    //
    // Three cases:
    // 1. No snapshot yet, or snapshot is stale (different language/theme) - show plain text.
    //    Stale detection is in-composition: no separate LaunchedEffect needed to clear state.
    // 2. Snapshot text exactly matches current text - apply spans directly (steady state).
    // 3. Text has changed since the last snapshot (user is typing, debounce pending) -
    //    Transfer old spans using prefix+suffix analysis: spans on the unchanged prefix are
    //    kept as-is, spans on the unchanged suffix (lines below the edit) are shifted by the
    //    length delta, and spans covering the edited region are dropped.
    //
    //    For append-at-end the suffix length is zero and all old spans fall in the prefix,
    //    so they carry over unchanged. For mid-text edits, text on lines above and below the
    //    edit stays colored; only characters in the edited region become unstyled until debounce
    //    fires and delivers a fresh highlight result.
    val currentText = value.text
    val snapshot = highlighted
    val annotated =
        when {
            snapshot == null || snapshot.language != language || snapshot.theme != theme -> {
                AnnotatedString(currentText)
            }

            snapshot.annotated.text == currentText -> {
                snapshot.annotated
            }

            else -> {
                applySnapshotSpans(snapshot.annotated, currentText)
            }
        }

    return value.copy(annotatedString = annotated)
}
