package dev.hossain.highlight.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightException
import dev.hossain.highlight.engine.HighlightResult
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
 * ## Scope
 *
 * This is a deliberate convenience composable, not a parameterized clone of [BasicTextField].
 * It exposes presentation knobs ([modifier], [contentPadding], [shape], [theme], [textStyle]),
 * code-friendly behavior tuning ([keyboardOptions], [cursorBrush]), highlight controls
 * ([debounceMs]), and observability ([onHighlightComplete], [onError]) - and nothing else.
 *
 * For any other [BasicTextField] parameter (`enabled`, `readOnly`, `singleLine`, `maxLines`,
 * `keyboardActions`, `visualTransformation`, `onTextLayout`, `interactionSource`, `decorationBox`,
 * etc.) drop one level down to [rememberSyntaxHighlightedEditorValue] and render your own field.
 * The helper returns a [TextFieldValue] with highlighting applied and cursor / selection
 * preserved; you compose it into whatever field shape you need:
 *
 * ```kotlin
 * val displayValue = rememberSyntaxHighlightedEditorValue(
 *     value    = editorValue,
 *     language = "kotlin",
 * )
 * BasicTextField(
 *     value         = displayValue,
 *     onValueChange = { editorValue = it },
 *     enabled       = uiEnabled,
 *     readOnly      = isReadOnly,
 *     singleLine    = true,
 *     // ...any other BasicTextField parameter
 * )
 * ```
 *
 * This split keeps the convenience composable's surface small (and stable - it doesn't grow as
 * Compose Foundation adds new `BasicTextField` parameters) while still giving callers full
 * control when they need it.
 *
 * ## Usage - inside HighlightThemeProvider (recommended)
 *
 * ```kotlin
 * HighlightThemeProvider(
 *     lightHighlightTheme = rememberTomorrowTheme(),
 *     darkHighlightTheme  = rememberTomorrowNightTheme(),
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
 *     theme         = rememberTomorrowTheme(),
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
 * @param keyboardOptions Keyboard options forwarded to the underlying [BasicTextField]. Defaults
 *   to [SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions], which disables autocorrect and
 *   autocapitalization and uses an Ascii keyboard - the right defaults for source code. Override
 *   if you need a different keyboard type or IME action; for example, copy the default and add
 *   `imeAction = ImeAction.Search` to keep the code-friendly settings while customising one
 *   field. Note that `BasicTextField`'s own [KeyboardOptions.Default] leaves autocorrect on, so
 *   passing `KeyboardOptions.Default` here will mangle identifiers as the user types.
 * @param cursorBrush Optional [Brush] used to paint the editor's text cursor. When `null`
 *   (the default), the cursor uses a [SolidColor] derived from the theme's `defaultTextColor`,
 *   so the cursor stays visible on both light and dark themes. Pass an explicit [Brush] to
 *   override - for example, `SolidColor(MaterialTheme.colorScheme.primary)` to match the host
 *   app's accent color. Note that [BasicTextField]'s own default is `SolidColor(Color.Black)`,
 *   which is invisible on dark themes.
 * @param debounceMs Milliseconds to wait after the last keystroke before triggering a new
 *   highlight call. Defaults to 150 ms - a good balance between responsiveness and avoiding
 *   unnecessary WebView calls on fast typists. If `debounceMs` changes, the new value is used
 *   on the next keystroke. The currently running debounce window is unaffected (the original
 *   delay completes with its captured-at-suspension value).
 * @param onHighlightComplete Optional callback invoked with a [HighlightResult] when a highlight
 *   cycle completes successfully. Fires after the snapshot is updated. Not called on failure.
 *   The result carries the highlighted [AnnotatedString], `spanCount`, `language`, `durationMs`,
 *   and per-layer [HighlightTimings] - matching the shape used by [rememberHighlightedCode] so
 *   callers can move between read-only and editable APIs without changing their callback shape.
 *   Defaults to `null` (no callback).
 * @param onError Optional callback invoked with the [HighlightException] when a highlight cycle
 *   fails. The editor falls back to plain text on failure regardless of whether this callback
 *   is set - it is purely observational. Use it to log failures, show a snackbar, or record
 *   analytics. Defaults to `null` (no callback).
 * @param indentation Indentation string to insert when the Tab key is pressed. Defaults to 4 spaces.
 * @param autoIndentEnabled Whether to automatically copy the indentation of the previous line when
 *   inserting a newline. Defaults to true.
 * @param tabKeyInterceptionEnabled Whether to intercept the hardware Tab key to insert spaces
 *   instead of shifting focus to the next view. Defaults to true. Note that arrow keys (Up,
 *   Down, Left, Right) are also intercepted to prevent focus from escaping the editor when
 *   boundaries are reached.
 * @param horizontalScrollState Hoisted scroll state for horizontal scrolling. When non-null,
 *   horizontal scrolling is enabled and code lines will not wrap. Defaults to `null` (wrapping enabled).
 * @param verticalScrollState Hoisted scroll state for vertical scrolling. When non-null,
 *   vertical scrolling is enabled. Defaults to `null` (disabled).
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
    keyboardOptions: KeyboardOptions = SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions,
    cursorBrush: Brush? = null,
    debounceMs: Long = SyntaxHighlightedTextEditorDefaults.DEBOUNCE_MS,
    onHighlightComplete: ((HighlightResult) -> Unit)? = null,
    onError: ((HighlightException) -> Unit)? = null,
    indentation: String = SyntaxHighlightedTextEditorDefaults.DEFAULT_INDENTATION,
    autoIndentEnabled: Boolean = SyntaxHighlightedTextEditorDefaults.AUTO_INDENT_ENABLED,
    tabKeyInterceptionEnabled: Boolean = SyntaxHighlightedTextEditorDefaults.TAB_KEY_INTERCEPTION_ENABLED,
    horizontalScrollState: ScrollState? = null,
    verticalScrollState: ScrollState? = null,
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

    // When the caller passes null, derive the cursor color from the theme so it stays visible on
    // both light and dark themes. BasicTextField's own default (SolidColor(Color.Black)) is
    // invisible on dark themes, which is the bug this defaulting avoids.
    val resolvedCursorBrush =
        remember(cursorBrush, textColor) {
            resolveEditorCursorBrush(cursorBrush, textColor)
        }

    // Wrap frequently changing state in stable State objects so the lambdas below can be
    // remembered once and still see the latest values when invoked.
    val currentValue = rememberUpdatedState(value)
    val currentOnValueChange = rememberUpdatedState(onValueChange)
    val currentAutoIndentEnabled = rememberUpdatedState(autoIndentEnabled)
    val currentTabKeyInterceptionEnabled = rememberUpdatedState(tabKeyInterceptionEnabled)
    val currentIndentation = rememberUpdatedState(indentation)

    val handleValueChange: (TextFieldValue) -> Unit =
        remember {
            { newValue ->
                if (currentAutoIndentEnabled.value) {
                    val oldText = currentValue.value.text
                    val newText = newValue.text

                    // Detect single character insertion where the new char is newline '\n'
                    if (newText.length == oldText.length + 1 &&
                        newValue.selection.start == currentValue.value.selection.start + 1 &&
                        newText[currentValue.value.selection.start] == '\n'
                    ) {
                        val cursorPosition = currentValue.value.selection.start
                        if (cursorPosition > 0) {
                            val indent = extractLeadingWhitespace(oldText, cursorPosition)
                            if (indent.isNotEmpty()) {
                                val autoIndentedText =
                                    newText.substring(0, newValue.selection.start) +
                                        indent +
                                        newText.substring(newValue.selection.start)
                                val autoIndentedSelection = TextRange(newValue.selection.start + indent.length)
                                currentOnValueChange.value(
                                    newValue.copy(text = autoIndentedText, selection = autoIndentedSelection),
                                )
                            } else {
                                currentOnValueChange.value(newValue)
                            }
                        } else {
                            currentOnValueChange.value(newValue)
                        }
                    } else {
                        currentOnValueChange.value(newValue)
                    }
                } else {
                    currentOnValueChange.value(newValue)
                }
            }
        }

    val previewKeyModifier =
        remember(tabKeyInterceptionEnabled, autoIndentEnabled) {
            if (tabKeyInterceptionEnabled || autoIndentEnabled) {
                Modifier.onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                    when (keyEvent.key) {
                        Key.Tab -> {
                            if (currentTabKeyInterceptionEnabled.value) {
                                val text = currentValue.value.text
                                val selection = currentValue.value.selection
                                val newText = text.replaceRange(selection.min, selection.max, currentIndentation.value)
                                val newSelection = TextRange(selection.min + currentIndentation.value.length)
                                currentOnValueChange.value(currentValue.value.copy(text = newText, selection = newSelection))
                                true
                            } else {
                                false
                            }
                        }

                        Key.Enter -> {
                            if (currentAutoIndentEnabled.value && currentValue.value.composition == null) {
                                val text = currentValue.value.text
                                val selection = currentValue.value.selection
                                val cursorPosition = selection.min

                                val indent = extractLeadingWhitespace(text, cursorPosition)

                                val insertion = "\n" + indent
                                val newText = text.replaceRange(selection.min, selection.max, insertion)
                                val newSelection = TextRange(selection.min + insertion.length)

                                currentOnValueChange.value(currentValue.value.copy(text = newText, selection = newSelection))
                                true
                            } else {
                                false
                            }
                        }

                        else -> {
                            false
                        }
                    }
                }
            } else {
                Modifier
            }
        }

    val focusNavigationModifier =
        Modifier.focusProperties {
            up = FocusRequester.Cancel
            down = FocusRequester.Cancel
            left = FocusRequester.Cancel
            right = FocusRequester.Cancel
        }

    val scrollModifier =
        Modifier
            .then(if (verticalScrollState != null) Modifier.verticalScroll(verticalScrollState) else Modifier)
            .then(if (horizontalScrollState != null) Modifier.horizontalScroll(horizontalScrollState) else Modifier)

    Surface(
        modifier = modifier.testTag("syntax-highlighted-text-editor"),
        shape = shape,
        color = backgroundColor,
        contentColor = textColor,
    ) {
        BasicTextField(
            value = displayValue,
            onValueChange = handleValueChange,
            textStyle = themedTextStyle,
            keyboardOptions = keyboardOptions,
            cursorBrush = resolvedCursorBrush,
            modifier =
                previewKeyModifier
                    .then(focusNavigationModifier)
                    .then(scrollModifier)
                    .padding(contentPadding),
        )
    }
}

/**
 * Resolves the editor's effective cursor brush: caller-supplied [cursorBrush] when non-null,
 * otherwise a [SolidColor] of the theme-derived [textColor] so the cursor stays visible on
 * both light and dark themes.
 *
 * Extracted as `internal` so the null-fallback contract can be unit-tested without standing up a
 * full Compose UI test harness. Behavior is intentionally trivial; the test exists to lock in
 * the contract, not to exercise a complex algorithm.
 */
internal fun resolveEditorCursorBrush(
    cursorBrush: Brush?,
    textColor: Color,
): Brush = cursorBrush ?: SolidColor(textColor)

private fun extractLeadingWhitespace(
    text: String,
    beforeIndex: Int,
): String {
    if (beforeIndex <= 0) return ""
    var lineStart = beforeIndex - 1
    while (lineStart >= 0 && text[lineStart] != '\n') {
        lineStart--
    }
    lineStart++ // Move past '\n' or start at index 0

    var indentEnd = lineStart
    while (indentEnd < beforeIndex && (text[indentEnd] == ' ' || text[indentEnd] == '\t')) {
        indentEnd++
    }
    return text.substring(lineStart, indentEnd)
}

@OptIn(ExperimentalHighlightApi::class)
@Preview(showBackground = true)
@Composable
private fun SyntaxHighlightedTextEditorPreview() {
    SyntaxHighlightedTextEditor(
        value = TextFieldValue("fun main() {\n    println(\"Hello world!\")\n}"),
        onValueChange = {},
        language = "kotlin",
        theme = rememberTomorrowTheme(),
    )
}
