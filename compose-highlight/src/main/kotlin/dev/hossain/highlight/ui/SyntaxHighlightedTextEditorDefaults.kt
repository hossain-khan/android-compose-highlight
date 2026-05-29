package dev.hossain.highlight.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

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
}
