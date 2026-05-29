package dev.hossain.highlight.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightException
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.engine.ThemedHighlightResult
import kotlinx.coroutines.delay

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
 * Built-in themes are precompiled and need no [android.content.Context]:
 *
 * ```kotlin
 * val theme = remember { HighlightTheme.tomorrow() }
 * val highlighted by rememberHighlightedCode(code, "kotlin", theme)
 * ```
 *
 * For custom CSS-backed themes, wrap creation in `remember` so CSS parsing does not repeat on
 * every recomposition. Or use the built-in convenience functions which handle this internally:
 *
 * ```kotlin
 * val highlighted by rememberHighlightedCode(code, "kotlin", rememberTomorrowTheme())
 * ```
 *
 * For light/dark toggling without re-highlighting, prefer [rememberHighlightedCodeBothThemes].
 *
 * @param code The source code to highlight.
 * @param language The Highlight.js language identifier (e.g. `"python"`, `"kotlin"`).
 * @param theme The theme to apply. Defaults to [LocalHighlightTheme].
 * @param onHighlightComplete Optional callback invoked with a [HighlightResult] when highlighting
 *   succeeds. Fires after the [State] is updated. Not called on failure.
 * @param onError Optional callback invoked with the [HighlightException] when highlighting fails.
 *   Use this to log failures, show a snackbar, or record analytics. The plain-text fallback
 *   is always displayed regardless of whether this callback is set - it is purely observational.
 *   Use [rememberUpdatedState] semantics: the latest lambda is always called without restarting
 *   the effect. The [HighlightException] subtypes give you typed error info:
 *   - [HighlightException.Timeout] - JS call did not complete in time
 *   - [HighlightException.JsExecutionFailed] - JavaScript error
 *   - [HighlightException.WebViewInitFailed] - WebView could not be created
 *   - [HighlightException.HtmlParseFailed] - jsoup could not parse the highlight output
 *
 *   ```kotlin
 *   val highlighted by rememberHighlightedCode(
 *       code = myCode,
 *       language = userInput,
 *       onError = { error ->
 *           Log.w("Highlight", "Failed to highlight: ${error.message}")
 *       },
 *   )
 *   ```
 * @return A [State] holding the highlighted [AnnotatedString], or `null` while loading / on error.
 */
@Composable
fun rememberHighlightedCode(
    code: String,
    language: String,
    theme: HighlightTheme = LocalHighlightTheme.current,
    onHighlightComplete: ((HighlightResult) -> Unit)? = null,
    onError: ((HighlightException) -> Unit)? = null,
): State<AnnotatedString?> {
    val engine = rememberHighlightEngine()
    val state = remember(code, language, theme) { mutableStateOf<AnnotatedString?>(null) }
    val latestCallback = rememberUpdatedState(onHighlightComplete)
    val latestErrorCallback = rememberUpdatedState(onError)

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
                }.onFailure { error ->
                    // Invoke onError with the typed HighlightException.
                    // All failures from HighlightEngine are HighlightException subtypes.
                    (error as? HighlightException)?.let { latestErrorCallback.value?.invoke(it) }
                    // Leave state.value = null so the caller renders plain fallback.
                }
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
 * @param onError Optional callback invoked with the [HighlightException] when highlighting fails.
 *   The plain-text fallback is always displayed regardless - this callback is purely observational.
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
    onError: ((HighlightException) -> Unit)? = null,
): State<ThemedHighlightResult?> {
    val engine = rememberHighlightEngine()
    val state = remember(code, language, lightTheme, darkTheme) { mutableStateOf<ThemedHighlightResult?>(null) }
    val latestCallback = rememberUpdatedState(onHighlightComplete)
    val latestErrorCallback = rememberUpdatedState(onError)

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
                }.onFailure { error ->
                    (error as? HighlightException)?.let { latestErrorCallback.value?.invoke(it) }
                    // Leave state.value = null so the caller renders plain fallback.
                }
        }
    }

    return state
}

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
 * Clips the span styles from [snapshotAnnotated] onto [currentText], applying only those spans
 * that fall within the **longest common prefix** of the snapshot text and [currentText].
 *
 * Spans beyond the first edit point are dropped because they are anchored to the old character
 * positions and would map to semantically wrong characters after any insertion or deletion in
 * the middle of the text. Only the unchanged prefix before the edit is guaranteed to be at the
 * same positions in both the old and new text.
 *
 * For the common **append-at-end** case the entire old text is a prefix of the new text, so
 * all old spans carry over correctly and only the newly appended characters are unstyled.
 */
internal fun clipSpansToPrefix(
    snapshotAnnotated: AnnotatedString,
    currentText: String,
): AnnotatedString {
    val prefixLen = snapshotAnnotated.text.commonPrefixWith(currentText).length
    val builder = AnnotatedString.Builder(currentText)
    snapshotAnnotated.spanStyles.forEach { range ->
        val end = range.end.coerceAtMost(prefixLen)
        if (range.start < end) builder.addStyle(range.item, range.start, end)
    }
    return builder.toAnnotatedString()
}

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
 * Between updates the previous spans are preserved and clipped to the current text length,
 * so the editor never flickers or loses color while the user is typing.
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
 *   highlight call. Defaults to 150 ms. If this value changes at runtime the new delay is
 *   picked up on the next highlight cycle without restarting the effect.
 * @param onHighlightComplete Optional callback invoked each time a highlight cycle completes
 *   successfully. Receives the resulting [AnnotatedString] with syntax spans applied. Useful
 *   for testing (wait until the first result arrives) and for observing the highlight output
 *   without owning the editor's text state.
 * @return The [TextFieldValue] with syntax-highlight spans applied, preserving
 *   the original cursor position and selection. Falls back to plain text while a highlight
 *   result is in flight, on error, or when the language/theme has just changed. Because this
 *   function is a non-restartable composable (returns a non-Unit type), all internal [State]
 *   reads (including the highlight snapshot) automatically subscribe the caller's recompose
 *   scope - callers should use it like any other composable helper (`val x = rememberXxx()`).
 */
@ExperimentalHighlightApi
@Composable
fun rememberSyntaxHighlightedEditorValue(
    value: TextFieldValue,
    language: String,
    theme: HighlightTheme = LocalHighlightTheme.current,
    debounceMs: Long = 150L,
    onHighlightComplete: ((AnnotatedString) -> Unit)? = null,
): TextFieldValue {
    val engine = rememberHighlightEngine()
    var highlighted by remember { mutableStateOf<HighlightSnapshot?>(null) }
    // rememberUpdatedState ensures a changed debounceMs or callback is used by the running
    // effect without restarting it (restarting would reset the debounce window mid-keystroke).
    val currentDebounceMs by rememberUpdatedState(debounceMs)
    val currentOnHighlightComplete by rememberUpdatedState(onHighlightComplete)

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
            }.onFailure { highlighted = null }
    }

    // Merge highlight spans into the TextFieldValue while preserving cursor and selection.
    //
    // Three cases:
    // 1. No snapshot yet, or snapshot is stale (different language/theme) - show plain text.
    //    Stale detection is in-composition: no separate LaunchedEffect needed to clear state.
    // 2. Snapshot text exactly matches current text - apply spans directly (steady state).
    // 3. Text has changed since the last snapshot (user is typing, debounce pending) -
    //    Only apply old spans within the unchanged prefix (longest common prefix between
    //    the snapshot text and the current text). Spans at or after the first edit point
    //    are dropped: they are anchored to the old positions and would map to wrong characters
    //    after any insertion or deletion in the middle of the text.
    //
    //    For the common append-at-end case, prefixLen == snapshot text length, so all old
    //    spans carry over correctly and only the new trailing characters are unstyled.
    //    For mid-text insertions or deletions, only the unchanged prefix stays colored;
    //    the edited region and everything after it shows plain text until debounce fires.
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
                clipSpansToPrefix(snapshot.annotated, currentText)
            }
        }

    return value.copy(annotatedString = annotated)
}
