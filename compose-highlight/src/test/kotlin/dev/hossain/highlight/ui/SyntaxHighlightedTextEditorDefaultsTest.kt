package dev.hossain.highlight.ui

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * JVM unit tests for [SyntaxHighlightedTextEditorDefaults].
 *
 * Verifies the constants ship with values appropriate for source-code editing - in particular,
 * that [SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions] disables the IME behaviors that
 * would mangle identifiers as the user types (autocorrect, autocapitalization).
 */
@OptIn(ExperimentalHighlightApi::class)
class SyntaxHighlightedTextEditorDefaultsTest {
    @Test
    fun `CodeKeyboardOptions disables autocorrect`() {
        // Without this, the IME rewrites identifiers ("fun" -> "Fun", "println" -> "printing")
        // as the user types - the failure mode that motivates the code-friendly default.
        assertThat(SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions.autoCorrectEnabled).isFalse()
    }

    @Test
    fun `CodeKeyboardOptions disables autocapitalization`() {
        // Sentence-boundary capitalization would turn `class` into `Class` after a period in a
        // string literal, comment, etc. Off by default for code.
        assertThat(SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions.capitalization)
            .isEqualTo(KeyboardCapitalization.None)
    }

    @Test
    fun `CodeKeyboardOptions uses Ascii keyboard type`() {
        // Hints the soft keyboard to a programmer-friendly layout (no language-specific
        // suggestions, easier access to symbols on most IMEs).
        assertThat(SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions.keyboardType)
            .isEqualTo(KeyboardType.Ascii)
    }

    @Test
    fun `DEBOUNCE_MS is 150`() {
        // Locked in as the documented default - changes are user-visible (faster typists feel
        // the difference) so the constant should not drift accidentally.
        assertThat(SyntaxHighlightedTextEditorDefaults.DEBOUNCE_MS).isEqualTo(150L)
    }
}
