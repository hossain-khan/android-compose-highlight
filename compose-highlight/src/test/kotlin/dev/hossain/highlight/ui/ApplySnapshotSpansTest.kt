package dev.hossain.highlight.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * JVM unit tests for [applySnapshotSpans].
 *
 * Verifies that spans from a stale highlight snapshot are correctly transferred onto the
 * current (in-progress) text during the debounce window:
 * - Spans in the unchanged prefix are preserved as-is.
 * - Spans in the unchanged suffix are preserved and shifted by the length delta.
 * - Spans in the edited region are dropped.
 * - Spans that straddle the prefix/edit boundary are clipped to the prefix.
 */
class ApplySnapshotSpansTest {
    private val red = SpanStyle(color = Color.Red)
    private val blue = SpanStyle(color = Color.Blue)
    private val green = SpanStyle(color = Color.Green)

    private fun snapshot(
        text: String,
        vararg spans: Triple<Int, Int, SpanStyle>,
    ): AnnotatedString =
        buildAnnotatedString {
            append(text)
            spans.forEach { (start, end, style) -> addStyle(style, start, end) }
        }

    // ── Append-at-end (no regression) ────────────────────────────────────────

    @Test
    fun `append at end - all old spans preserved`() {
        val snap = snapshot("fun main()", Triple(0, 3, red))
        val result = applySnapshotSpans(snap, "fun main() {")

        assertThat(result.text).isEqualTo("fun main() {")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].start).isEqualTo(0)
        assertThat(result.spanStyles[0].end).isEqualTo(3)
        assertThat(result.spanStyles[0].item).isEqualTo(red)
    }

    @Test
    fun `append at end - multiple spans all preserved`() {
        // "val" in red (0..3), "1" in blue (8..9)
        val snap = snapshot("val x = 1", Triple(0, 3, red), Triple(8, 9, blue))
        val result = applySnapshotSpans(snap, "val x = 10")

        assertThat(result.text).isEqualTo("val x = 10")
        assertThat(result.spanStyles).hasSize(2)
        val starts = result.spanStyles.map { it.start }
        val ends = result.spanStyles.map { it.end }
        assertThat(starts).containsExactlyElementsIn(listOf(0, 8)).inOrder()
        assertThat(ends).containsExactlyElementsIn(listOf(3, 9)).inOrder()
    }

    // ── Insert in the middle ──────────────────────────────────────────────────

    @Test
    fun `insert in middle - prefix span preserved, suffix span shifted`() {
        // "fun" in red (0..3), "main" in blue (4..8)
        val snap = snapshot("fun main()", Triple(0, 3, red), Triple(4, 8, blue))
        // Insert "// " at position 4 -> "fun // main()"
        val result = applySnapshotSpans(snap, "fun // main()")

        assertThat(result.text).isEqualTo("fun // main()")
        // Red "fun" (0..3) is in prefix - preserved as-is.
        val redSpans = result.spanStyles.filter { it.item == red }
        assertThat(redSpans).hasSize(1)
        assertThat(redSpans[0].start).isEqualTo(0)
        assertThat(redSpans[0].end).isEqualTo(3)
        // Blue "main" (4..8) is in unchanged suffix - shifted by delta=3 to (7..11).
        val blueSpans = result.spanStyles.filter { it.item == blue }
        assertThat(blueSpans).hasSize(1)
        assertThat(blueSpans[0].start).isEqualTo(7)
        assertThat(blueSpans[0].end).isEqualTo(11)
    }

    @Test
    fun `insert in middle - span straddling edit point is clipped to prefix`() {
        // Entire word "keyword" in red (0..7), "rest" in blue (8..12)
        val snap = snapshot("keyword rest", Triple(0, 7, red), Triple(8, 12, blue))
        // Insert "X" at position 3 -> "keyXword rest"
        val result = applySnapshotSpans(snap, "keyXword rest")

        assertThat(result.text).isEqualTo("keyXword rest")
        // Red span (0..7) straddles the edit point at 3 - clipped to (0..3).
        val redSpans = result.spanStyles.filter { it.item == red }
        assertThat(redSpans).hasSize(1)
        assertThat(redSpans[0].start).isEqualTo(0)
        assertThat(redSpans[0].end).isEqualTo(3)
        // Blue "rest" (8..12) is in unchanged suffix - shifted by delta=1 to (9..13).
        val blueSpans = result.spanStyles.filter { it.item == blue }
        assertThat(blueSpans).hasSize(1)
        assertThat(blueSpans[0].start).isEqualTo(9)
        assertThat(blueSpans[0].end).isEqualTo(13)
    }

    // ── Delete from the middle ────────────────────────────────────────────────

    @Test
    fun `delete from middle - prefix span preserved, suffix span shifted`() {
        // "val" in red (0..3), "1" in blue (8..9)
        val snap = snapshot("val x = 1", Triple(0, 3, red), Triple(8, 9, blue))
        // Delete "x = " (positions 4..8) -> "val 1"
        val result = applySnapshotSpans(snap, "val 1")

        assertThat(result.text).isEqualTo("val 1")
        // Red (0..3) is in prefix - preserved as-is.
        val redSpans = result.spanStyles.filter { it.item == red }
        assertThat(redSpans).hasSize(1)
        assertThat(redSpans[0].start).isEqualTo(0)
        assertThat(redSpans[0].end).isEqualTo(3)
        // Blue "1" (8..9) is in unchanged suffix - shifted by delta=-4 to (4..5).
        val blueSpans = result.spanStyles.filter { it.item == blue }
        assertThat(blueSpans).hasSize(1)
        assertThat(blueSpans[0].start).isEqualTo(4)
        assertThat(blueSpans[0].end).isEqualTo(5)
    }

    // ── Multi-line: suffix lines stay colored ─────────────────────────────────

    @Test
    fun `multiline - editing a line preserves spans on lines below`() {
        // Simulate the real bug: typing on a middle line should keep colors on lines below.
        // "fun " in red (0..4), "data" in blue (13..17) - with some text between
        val oldText = "fun main()\n}\ndata class"
        // old positions: "fun " 0..4, "data" 13..17
        val snap = snapshot(oldText, Triple(0, 4, red), Triple(13, 17, blue))

        // Insert "// AAAAA" after "fun main()\n}\n" - adds 8 chars at position 13
        val newText = "fun main()\n}\n// AAAAAdata class"
        val result = applySnapshotSpans(snap, newText)

        assertThat(result.text).isEqualTo(newText)
        // Red "fun " (0..4) is in the prefix - preserved as-is.
        val redSpans = result.spanStyles.filter { it.item == red }
        assertThat(redSpans).hasSize(1)
        assertThat(redSpans[0].start).isEqualTo(0)
        assertThat(redSpans[0].end).isEqualTo(4)
        // Blue "data" (13..17) is in the suffix - shifted by delta=8 to (21..25).
        val blueSpans = result.spanStyles.filter { it.item == blue }
        assertThat(blueSpans).hasSize(1)
        assertThat(blueSpans[0].start).isEqualTo(21)
        assertThat(blueSpans[0].end).isEqualTo(25)
    }

    @Test
    fun `multiline - deleting text on a line preserves spans on lines below`() {
        // old: "// AAAAAAA\ndata class", spans: "// " in red (0..3), "data" in blue (11..15)
        val oldText = "// AAAAAAA\ndata class"
        val snap = snapshot(oldText, Triple(0, 3, red), Triple(11, 15, blue))

        // Delete "AAAAAA" -> "// A\ndata class"
        val newText = "// A\ndata class"
        val result = applySnapshotSpans(snap, newText)

        assertThat(result.text).isEqualTo(newText)
        // Red "// " (0..3) is in prefix - preserved.
        val redSpans = result.spanStyles.filter { it.item == red }
        assertThat(redSpans).hasSize(1)
        assertThat(redSpans[0].start).isEqualTo(0)
        assertThat(redSpans[0].end).isEqualTo(3)
        // Blue "data" (11..15) is in suffix - shifted by delta=-6 to (5..9).
        val blueSpans = result.spanStyles.filter { it.item == blue }
        assertThat(blueSpans).hasSize(1)
        assertThat(blueSpans[0].start).isEqualTo(5)
        assertThat(blueSpans[0].end).isEqualTo(9)
    }

    // ── Complete replacement ──────────────────────────────────────────────────

    @Test
    fun `completely different text - no spans`() {
        val snap = snapshot("foo", Triple(0, 3, red))
        val result = applySnapshotSpans(snap, "bar")

        assertThat(result.text).isEqualTo("bar")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `empty current text - no spans`() {
        val snap = snapshot("fun main()", Triple(0, 3, red))
        val result = applySnapshotSpans(snap, "")

        assertThat(result.text).isEqualTo("")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `empty snapshot text - no spans on any current text`() {
        val snap = AnnotatedString("")
        val result = applySnapshotSpans(snap, "anything")

        assertThat(result.text).isEqualTo("anything")
        assertThat(result.spanStyles).isEmpty()
    }
}
