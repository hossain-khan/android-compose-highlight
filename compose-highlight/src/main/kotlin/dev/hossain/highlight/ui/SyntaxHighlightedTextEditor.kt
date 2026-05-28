package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.engine.HighlightTheme
import kotlinx.coroutines.delay

/**
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
 * @param theme The highlight theme to apply. Defaults to [LocalHighlightTheme].
 * @param textStyle Text style for the editor. Defaults to a monospace style. The theme's
 *   foreground color is applied on top of this style when a highlight result is available.
 * @param debounceMs Milliseconds to wait after the last keystroke before triggering a new
 *   highlight call. Defaults to 150 ms - a good balance between responsiveness and avoiding
 *   unnecessary WebView calls on fast typists.
 */
@Composable
fun SyntaxHighlightedTextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    theme: HighlightTheme = LocalHighlightTheme.current,
    textStyle: TextStyle = TextStyle(fontFamily = FontFamily.Monospace),
    debounceMs: Long = 150L,
) {
    val engine = rememberHighlightEngine()
    var highlighted by remember { mutableStateOf<AnnotatedString?>(null) }

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

    // Merge the theme foreground color into the caller-supplied text style so unspanned
    // characters (newly typed, not yet highlighted) match the theme foreground.
    val themedTextStyle = remember(theme, textStyle) { textStyle.copy(color = textColor) }

    // Re-highlight with debounce whenever the text, language, or theme changes.
    // LaunchedEffect cancels the previous coroutine on each change, so rapid keystrokes
    // naturally coalesce into a single highlight call after the user pauses.
    LaunchedEffect(value.text, language, theme) {
        delay(debounceMs)
        engine
            .highlight(value.text, language, theme)
            .onSuccess { result -> highlighted = result.annotated }
            .onFailure { highlighted = null }
    }

    // Merge highlight spans into the TextFieldValue while preserving cursor and selection.
    //
    // Three cases:
    // 1. No highlight result yet - show plain text (first render or error).
    // 2. Highlight text exactly matches current text - apply spans directly (steady state).
    // 3. Text has changed since the last highlight result (user is typing, debounce pending) -
    //    clip old spans to the new text length and apply them. Spans before any edit point
    //    remain correctly colored; only the newly typed characters briefly have no span.
    //    This gives the illusion of live highlighting - 90%+ of the block stays colored while
    //    only the new characters wait for the next debounced highlight call.
    val currentText = value.text
    val annotated =
        when {
            highlighted == null -> {
                AnnotatedString(currentText)
            }

            highlighted!!.text == currentText -> {
                highlighted!!
            }

            else -> {
                // Reuse old spans clipped to the new text length so the cursor offset
                // is always in bounds, preserving editability.
                val builder = AnnotatedString.Builder(currentText)
                highlighted!!.spanStyles.forEach { range ->
                    val start = range.start.coerceAtMost(currentText.length)
                    val end = range.end.coerceAtMost(currentText.length)
                    if (start < end) builder.addStyle(range.item, start, end)
                }
                builder.toAnnotatedString()
            }
        }
    val displayValue = value.copy(annotatedString = annotated)

    Surface(
        modifier = modifier,
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
