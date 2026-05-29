package dev.hossain.highlight.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * JVM unit tests for [clipSpansToPrefix].
 *
 * Each test exercises the span-clipping logic directly so that the
 * stale-span-during-debounce fix can be verified without a connected device.
 */
class ClipSpansToPrefixTest {
    private val red = SpanStyle(color = Color.Red)
    private val blue = SpanStyle(color = Color.Blue)

    // Builds an AnnotatedString that mirrors what ThemeParser/HtmlToAnnotatedString produces:
    // a full highlighted text with one or more spans.
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
        // Snapshot: "fun " highlighted (0..4) in red
        val snap = snapshot("fun main()", Triple(0, 3, red))
        // User typed " {" after - new text is longer at the end
        val result = clipSpansToPrefix(snap, "fun main() {")

        assertThat(result.text).isEqualTo("fun main() {")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].start).isEqualTo(0)
        assertThat(result.spanStyles[0].end).isEqualTo(3)
        assertThat(result.spanStyles[0].item).isEqualTo(red)
    }

    @Test
    fun `append at end - multiple spans all preserved`() {
        // "val x = 1" - "val" in red (0..3), "1" in blue (8..9)
        val snap = snapshot("val x = 1", Triple(0, 3, red), Triple(8, 9, blue))
        // User appended "0" => "val x = 10"
        val result = clipSpansToPrefix(snap, "val x = 10")

        assertThat(result.text).isEqualTo("val x = 10")
        assertThat(result.spanStyles).hasSize(2)
        val starts = result.spanStyles.map { it.start }
        val ends = result.spanStyles.map { it.end }
        assertThat(starts).containsExactlyElementsIn(listOf(0, 8)).inOrder()
        assertThat(ends).containsExactlyElementsIn(listOf(3, 9)).inOrder()
    }

    // ── Insert in the middle ──────────────────────────────────────────────────

    @Test
    fun `insert in middle - spans after insert point are dropped`() {
        // Snapshot: "fun main()" - "fun" in red (0..3), "main" in blue (4..8)
        val snap = snapshot("fun main()", Triple(0, 3, red), Triple(4, 8, blue))
        // User inserted "// " at position 4 => "fun // main()"
        val result = clipSpansToPrefix(snap, "fun // main()")

        // Prefix is "fun " (length 4) - only the red span fits entirely within [0, 4)
        assertThat(result.text).isEqualTo("fun // main()")
        // Only the red span (0..3) is within the prefix; blue (4..8) is dropped
        val redSpans = result.spanStyles.filter { it.item == red }
        val blueSpans = result.spanStyles.filter { it.item == blue }
        assertThat(redSpans).hasSize(1)
        assertThat(blueSpans).isEmpty()
    }

    @Test
    fun `insert in middle - span straddling edit point is clipped to prefix`() {
        // Snapshot: "keyword" - entire word in red (0..7)
        val snap = snapshot("keyword rest", Triple(0, 7, red))
        // User inserted "X" at position 3 => "keyXword rest"
        // Prefix is "key" (length 3); the red span (0..7) straddles the edit point
        val result = clipSpansToPrefix(snap, "keyXword rest")

        assertThat(result.text).isEqualTo("keyXword rest")
        // Span is clipped to (0..3) - only the prefix part
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].start).isEqualTo(0)
        assertThat(result.spanStyles[0].end).isEqualTo(3)
    }

    // ── Delete from the middle ────────────────────────────────────────────────

    @Test
    fun `delete from middle - spans beyond deletion point are dropped`() {
        // Snapshot: "val x = 1" - "val" in red (0..3), "1" in blue (8..9)
        val snap = snapshot("val x = 1", Triple(0, 3, red), Triple(8, 9, blue))
        // User deleted "x = " (positions 4..8) => "val 1"
        // Prefix is "val " (length 4)
        val result = clipSpansToPrefix(snap, "val 1")

        assertThat(result.text).isEqualTo("val 1")
        // Red span (0..3) is within prefix and preserved; blue (8..9) is beyond => dropped
        val redSpans = result.spanStyles.filter { it.item == red }
        val blueSpans = result.spanStyles.filter { it.item == blue }
        assertThat(redSpans).hasSize(1)
        assertThat(blueSpans).isEmpty()
    }

    // ── Complete replacement ──────────────────────────────────────────────────

    @Test
    fun `completely different text - no spans`() {
        val snap = snapshot("foo", Triple(0, 3, red))
        val result = clipSpansToPrefix(snap, "bar")

        // No common prefix => no spans
        assertThat(result.text).isEqualTo("bar")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `empty current text - no spans`() {
        val snap = snapshot("fun main()", Triple(0, 3, red))
        val result = clipSpansToPrefix(snap, "")

        assertThat(result.text).isEqualTo("")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `empty snapshot text - no spans on any current text`() {
        val snap = AnnotatedString("")
        val result = clipSpansToPrefix(snap, "anything")

        assertThat(result.text).isEqualTo("anything")
        assertThat(result.spanStyles).isEmpty()
    }
}
