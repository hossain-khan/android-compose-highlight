package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.engine.HighlightException
import dev.hossain.highlight.engine.HighlightTheme

/**
 * This composable is marked **experimental** ([ExperimentalHighlightApi]). Call sites must
 * opt in with `@OptIn(ExperimentalHighlightApi::class)` or propagate the annotation. The API
 * surface (parameters, defaults, behavior) may change in future releases.
 *
 * A syntax-highlighted code editor composable built on [BasicTextField].
 *
 * As the user types, the visible text is re-highlighted in the background using the
 * same [HighlightEngine] pipeline as [SyntaxHighlightedCode]. Keystrokes are debounced
 * by [debounceMs] to avoid firing a highlight call on every character. While a new
 * highlight result is in flight, the previously highlighted spans (or plain text on
 * first render) remain visible with no flicker.
 *
 * Cursor position and selection are always preserved: the highlighting only replaces the
 * [AnnotatedString] content inside the [TextFieldValue], never the cursor or selection.
 *
 * This composable reads the active theme from [LocalHighlightTheme], so a
 * [HighlightThemeProvider] ancestor **must** exist, or you must pass an explicit [theme].
 *
 * For custom layout or third-party text fields, use [rememberSyntaxHighlightedEditorValue]
 * directly to obtain the highlighted [TextFieldValue] without the `Surface` wrapper.
 *
 * ## Usage - inside HighlightThemeProvider (recommended)
 *
 * ```kotlin
 * HighlightThemeProvider(
 *     lightHighlightTheme = HighlightTheme.tomorrow(),
 *     darkHighlightTheme  = HighlightTheme.tomorrowNight(),
 * ) {
 *     var editorValue by remember { mutableStateOf(TextFieldValue("fun hello() = println(\"Hello!\")")) }
 *     SyntaxHighlightedTextEditor(
 *         value       = editorValue,
 *         onValueChange = { editorValue = it },
 *         language    = "kotlin",
 *         modifier    = Modifier.fillMaxWidth().border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
 *         shape       = RoundedCornerShape(8.dp),
 *         contentPadding = PaddingValues(12.dp),
 *     )
 * }
 * ```
 *
 * ## Usage - with an explicit theme
 *
 * ```kotlin
 * var editorValue by remember { mutableStateOf(TextFieldValue("SELECT * FROM users")) }
 * SyntaxHighlightedTextEditor(
 *     value         = editorValue,
 *     onValueChange = { editorValue = it },
 *     language      = "sql",
 *     theme         = HighlightTheme.tomorrow(),
 * )
 * ```
 *
 * @param value The current [TextFieldValue], including text, cursor position, and selection.
 * @param onValueChange Called whenever the user edits the text or moves the cursor.
 * @param language Highlight.js language identifier (e.g. `"kotlin"`, `"python"`, `"sql"`).
 * @param modifier Modifier applied to the outer [Surface] container (background, border, size, etc.).
 *   Do **not** include padding here - use [contentPadding] instead. Padding applied via [modifier]
 *   would shrink the Surface layout area, leaving a gap between the border and the theme background.
 * @param contentPadding Padding applied *inside* the [Surface], between the background edge and
 *   the text. Defaults to [PaddingValues] of 0.dp (no padding). Use this instead of adding
 *   `.padding()` to [modifier] so the theme background fills the full bordered area.
 * @param shape Shape used to clip the Surface background. Must match the shape used in any
 *   `.border()` applied via [modifier] so the background and border align. Defaults to
 *   [RectangleShape] (no rounding).
 * @param theme The highlight theme to apply. Defaults to [LocalHighlightTheme].
 * @param textStyle Text style for the editor. Defaults to a monospace style. The theme's
 *   foreground color is applied on top of this style when a highlight result is available.
 * @param debounceMs Milliseconds to wait after the last keystroke before triggering a new
 *   highlight call. Defaults to 150 ms - a good balance between responsiveness and avoiding
 *   unnecessary WebView calls on fast typists. If `debounceMs` changes, the new value is used
 *   on the next keystroke. The currently running debounce window is unaffected (the original
 *   delay completes with its captured-at-suspension value).
 * @param onHighlightComplete Optional callback invoked each time a highlight cycle completes
 *   successfully. Receives the resulting [AnnotatedString] with syntax spans applied. Useful
 *   for testing (wait until the first result arrives) and for observing the highlight output
 *   without owning the editor's text state. Defaults to `null` (no callback).
 * @param onError Optional callback invoked with the [HighlightException] when a highlight cycle
 *   fails. The editor falls back to plain text on failure regardless of whether this callback
 *   is set - it is purely observational. Use it to log failures, show a snackbar, or record
 *   analytics. Defaults to `null` (no callback).
 *
 * **Note on [shape]:** if you pass a custom [Shape] (e.g. `RoundedCornerShape(8.dp)`), wrap
 * it in `remember` at the call site so that a new instance is not created on every
 * recomposition, which would defeat Compose's skipping optimisation.
 */
@ExperimentalHighlightApi
@Composable
fun SyntaxHighlightedTextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    shape: Shape = RectangleShape,
    theme: HighlightTheme = LocalHighlightTheme.current,
    textStyle: TextStyle = SyntaxHighlightedTextEditorDefaults.DefaultTextStyle,
    debounceMs: Long = SyntaxHighlightedTextEditorDefaults.DEBOUNCE_MS,
    onHighlightComplete: ((AnnotatedString) -> Unit)? = null,
    onError: ((HighlightException) -> Unit)? = null,
) {
    val displayValue =
        rememberSyntaxHighlightedEditorValue(
            value = value,
            language = language,
            theme = theme,
            debounceMs = debounceMs,
            onHighlightComplete = onHighlightComplete,
            onError = onError,
        )

    val backgroundColor =
        remember(theme) {
            theme.backgroundColor.takeIf { it != Color.Unspecified }
                ?: SyntaxHighlightedCodeDefaults.fallbackBackgroundColor
        }
    val textColor =
        remember(theme) {
            theme.defaultTextColor.takeIf { it != Color.Unspecified }
                ?: SyntaxHighlightedCodeDefaults.fallbackTextColor
        }
    val themedTextStyle = remember(theme, textStyle) { textStyle.copy(color = textColor) }

    Surface(
        modifier = modifier.testTag("syntax-highlighted-text-editor"),
        shape = shape,
        color = backgroundColor,
        contentColor = textColor,
    ) {
        BasicTextField(
            value = displayValue,
            onValueChange = onValueChange,
            textStyle = themedTextStyle,
            modifier = Modifier.padding(contentPadding),
        )
    }
}
