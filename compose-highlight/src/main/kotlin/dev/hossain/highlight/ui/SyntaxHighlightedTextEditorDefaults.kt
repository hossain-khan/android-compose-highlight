package dev.hossain.highlight.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

/**
 * Default values used by [SyntaxHighlightedTextEditor] and [rememberSyntaxHighlightedEditorValue].
 *
 * Singletons here let parameter defaults reference a pre-allocated value instead of constructing
 * a fresh instance per call. For composables that recompose on every keystroke (the editor),
 * this avoids minor GC pressure during the worst possible time - while the user is typing.
 *
 * ## Usage
 *
 * ```kotlin
 * // Build on top of the default text style without re-declaring fontFamily.
 * val myEditorStyle = SyntaxHighlightedTextEditorDefaults.DefaultTextStyle.copy(fontSize = 15.sp)
 *
 * SyntaxHighlightedTextEditor(
 *     value = editorValue,
 *     onValueChange = { editorValue = it },
 *     language = "kotlin",
 *     textStyle = myEditorStyle,
 * )
 * ```
 */
@ExperimentalHighlightApi
object SyntaxHighlightedTextEditorDefaults {
    /**
     * Default [TextStyle] for the editor: monospace family. Pre-allocated singleton so the
     * editor's `textStyle` parameter default does not allocate a fresh `TextStyle` per
     * recomposition. Callers can `copy(...)` this to derive customised styles.
     */
    val DefaultTextStyle: TextStyle = TextStyle(fontFamily = FontFamily.Monospace)

    /**
     * Default debounce window in milliseconds after the last keystroke before the editor
     * triggers a new highlight call. 150 ms is a balance between responsiveness and avoiding
     * unnecessary WebView calls on fast typists.
     */
    const val DEBOUNCE_MS: Long = 150L

    /**
     * Default [KeyboardOptions] tuned for source-code input.
     *
     * `BasicTextField`'s built-in default ([KeyboardOptions.Default]) leaves autocorrect and
     * autocapitalization enabled, which mangles identifiers ("fun" -> "Fun", "println" ->
     * "printing") and is wrong for code. The editor opts callers into code-friendly behavior
     * out of the box:
     * - [KeyboardCapitalization.None] - no automatic capitalization at sentence boundaries.
     * - `autoCorrectEnabled = false` - the IME does not rewrite identifiers or symbols.
     * - [KeyboardType.Ascii] - hints the soft keyboard to a programmer-friendly layout.
     *
     * Override at the call site if you need a different keyboard type (e.g. a search input
     * that should still use the language-default IME):
     *
     * ```kotlin
     * SyntaxHighlightedTextEditor(
     *     value = editorValue,
     *     onValueChange = { editorValue = it },
     *     language = "sql",
     *     keyboardOptions = SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions.copy(
     *         imeAction = ImeAction.Search,
     *     ),
     * )
     * ```
     */
    val CodeKeyboardOptions: KeyboardOptions =
        KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
        )
}
