package dev.hossain.highlight.ui

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * JVM unit tests for [calculateVerticalCursorMovement].
 *
 * Verifies vertical line navigation through newline-delimited text when horizontal
 * scrolling is enabled in [SyntaxHighlightedTextEditor].
 */
@OptIn(ExperimentalHighlightApi::class)
class SyntaxHighlightedTextEditorVerticalCursorMovementTest {
    @Test
    fun `empty text returns TextRange Zero`() {
        val result =
            calculateVerticalCursorMovement(
                text = "",
                selection = TextRange(0),
                isUp = true,
                isShiftPressed = false,
            )
        assertThat(result).isEqualTo(TextRange.Zero)
    }

    @Test
    fun `moving up from second line preserves column when previous line is long enough`() {
        // "line 1\nline 2" -> line 1 is 0..5 (\n at 6), line 2 is 7..12
        val text = "line 1\nline 2"
        // Cursor at 'e' in "line 2": index 10 (column 3, since line starts at 7)
        val selection = TextRange(10)
        val result =
            calculateVerticalCursorMovement(
                text = text,
                selection = selection,
                isUp = true,
                isShiftPressed = false,
            )
        // In line 1 (starts at 0), column 3 is index 3 ('e')
        assertThat(result).isEqualTo(TextRange(3))
    }

    @Test
    fun `moving up clamps to previous line length when previous line is shorter`() {
        // "ab\nhello world"
        // Line 0: "ab" (length 2, indices 0..1, \n at 2)
        // Line 1: "hello world" (starts at 3)
        val text = "ab\nhello world"
        // Cursor at column 5 on Line 1: index 3 + 5 = 8
        val selection = TextRange(8)
        val result =
            calculateVerticalCursorMovement(
                text = text,
                selection = selection,
                isUp = true,
                isShiftPressed = false,
            )
        // Previous line has length 2, so target column is 2 (index 2, right before \n)
        assertThat(result).isEqualTo(TextRange(2))
    }

    @Test
    fun `moving up on first line moves cursor to start of document`() {
        val text = "hello world\nline 2"
        // Cursor at column 5 on first line: index 5
        val selection = TextRange(5)
        val result =
            calculateVerticalCursorMovement(
                text = text,
                selection = selection,
                isUp = true,
                isShiftPressed = false,
            )
        assertThat(result).isEqualTo(TextRange(0))
    }

    @Test
    fun `moving up when already at start of document stays at zero`() {
        val text = "hello world\nline 2"
        val selection = TextRange(0)
        val result =
            calculateVerticalCursorMovement(
                text = text,
                selection = selection,
                isUp = true,
                isShiftPressed = false,
            )
        assertThat(result).isEqualTo(TextRange(0))
    }

    @Test
    fun `moving down from first line preserves column when next line is long enough`() {
        val text = "hello\nworld 123"
        // Line 0: "hello" (starts at 0, \n at 5)
        // Line 1: "world 123" (starts at 6)
        val selection = TextRange(2) // column 2 on Line 0
        val result =
            calculateVerticalCursorMovement(
                text = text,
                selection = selection,
                isUp = false,
                isShiftPressed = false,
            )
        // In line 1, column 2 is index 6 + 2 = 8 ('r')
        assertThat(result).isEqualTo(TextRange(8))
    }

    @Test
    fun `moving down clamps to next line length when next line is shorter`() {
        val text = "hello world\nhi"
        // Line 0: "hello world" (starts at 0, length 11, \n at 11)
        // Line 1: "hi" (starts at 12, length 2)
        val selection = TextRange(8) // column 8
        val result =
            calculateVerticalCursorMovement(
                text = text,
                selection = selection,
                isUp = false,
                isShiftPressed = false,
            )
        // Next line has length 2, so target is 12 + 2 = 14
        assertThat(result).isEqualTo(TextRange(14))
    }

    @Test
    fun `moving down on last line moves cursor to end of document`() {
        val text = "line 1\nline 2"
        // Cursor at column 2 on line 2 (starts at 7, cursor at 9)
        val selection = TextRange(9)
        val result =
            calculateVerticalCursorMovement(
                text = text,
                selection = selection,
                isUp = false,
                isShiftPressed = false,
            )
        assertThat(result).isEqualTo(TextRange(text.length))
    }

    @Test
    fun `moving down when already at end of document stays at end`() {
        val text = "line 1\nline 2"
        val selection = TextRange(text.length)
        val result =
            calculateVerticalCursorMovement(
                text = text,
                selection = selection,
                isUp = false,
                isShiftPressed = false,
            )
        assertThat(result).isEqualTo(TextRange(text.length))
    }

    @Test
    fun `shift pressed expands selection upward while preserving anchor`() {
        val text = "line 1\nline 2"
        // Anchor at 10, cursor at 10
        val selection = TextRange(10)
        val result =
            calculateVerticalCursorMovement(
                text = text,
                selection = selection,
                isUp = true,
                isShiftPressed = true,
            )
        // Anchor preserved at 10, end moved to 3
        assertThat(result).isEqualTo(TextRange(start = 10, end = 3))
    }

    @Test
    fun `shift pressed expands selection downward while preserving anchor`() {
        val text = "line 1\nline 2"
        val selection = TextRange(2)
        val result =
            calculateVerticalCursorMovement(
                text = text,
                selection = selection,
                isUp = false,
                isShiftPressed = true,
            )
        assertThat(result).isEqualTo(TextRange(start = 2, end = 9))
    }

    @Test
    fun `single line document handles up and down`() {
        val text = "single line text"
        val mid = TextRange(6)

        val upResult =
            calculateVerticalCursorMovement(
                text = text,
                selection = mid,
                isUp = true,
                isShiftPressed = false,
            )
        assertThat(upResult).isEqualTo(TextRange(0))

        val downResult =
            calculateVerticalCursorMovement(
                text = text,
                selection = mid,
                isUp = false,
                isShiftPressed = false,
            )
        assertThat(downResult).isEqualTo(TextRange(text.length))
    }
}
