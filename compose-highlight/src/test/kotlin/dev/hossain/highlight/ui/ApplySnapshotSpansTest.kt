package dev.hossain.highlight.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.ui.internal.applySnapshotSpans
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
 * - Spans that straddle prefix + changed + suffix preserve BOTH unchanged tails.
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

    // ----- Append at end (no regression) -----

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

    // ----- Insert in the middle -----

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
    fun `insert in middle - span straddling edit keeps both unchanged tails`() {
        // Entire word "keyword" in red (0..7), "rest" in blue (8..12). The user inserts "X"
        // at position 3 -> "keyXword rest". Because "word rest" is the longest common suffix,
        // the algorithm recognises that BOTH the prefix part of "keyword" ("key", positions
        // 0..3) and its suffix part ("word", positions 3..7 in the old text -> 4..8 in the
        // new text) are unchanged in their content. The new fourth `when` branch preserves
        // both tails. The blue suffix span on "rest" shifts by delta=1.
        val snap = snapshot("keyword rest", Triple(0, 7, red), Triple(8, 12, blue))
        val result = applySnapshotSpans(snap, "keyXword rest")

        assertThat(result.text).isEqualTo("keyXword rest")
        // Red span (0..7) straddles all three regions in the new text.
        // Prefix tail keeps original coordinates: (0..prefixLen=3).
        // Suffix tail: oldChangedEnd=3 + delta=1 -> 4, range.end=7 + delta=1 -> 8 -> (4..8).
        val redSpans = result.spanStyles.filter { it.item == red }
        assertThat(redSpans).hasSize(2)
        val redRanges = redSpans.map { it.start to it.end }
        assertThat(redRanges).containsExactly(0 to 3, 4 to 8).inOrder()
        // Blue "rest" (8..12) is in unchanged suffix - shifted by delta=1 to (9..13).
        val blueSpans = result.spanStyles.filter { it.item == blue }
        assertThat(blueSpans).hasSize(1)
        assertThat(blueSpans[0].start).isEqualTo(9)
        assertThat(blueSpans[0].end).isEqualTo(13)
    }

    @Test
    fun `insert in middle - span ending at the edit point is clipped to prefix only`() {
        // Same shape as above but the red span doesn't extend past the edit. With the new
        // four-branch when, this case still hits the prefix-to-changed branch (NOT the new
        // three-region branch). Verifies the third branch is still reachable.
        // Snapshot: "key rest" with red on "key" (0..3), blue on "rest" (4..8).
        val snap = snapshot("key rest", Triple(0, 3, red), Triple(4, 8, blue))
        // Insert "X" at position 2 -> "keXy rest". prefixLen=2, suffixLen=6 (" rest" + "y"),
        // wait: old[2]='y' vs new[3]='y' so the 'y' goes into the suffix.
        // Walking back: old[7]='t'=new[8]='t', old[6]='s'=new[7]='s', ..., old[2]='y'=new[3]='y'.
        // So suffixLen=6 (chars "y rest"). oldChangedEnd = 8 - 6 = 2. delta=1.
        // Red span (0..3) has start=0 < prefixLen=2 AND end=3 > oldChangedEnd=2 -> three-region branch.
        // Prefix tail: (0..2). Suffix tail: oldChangedEnd=2 + delta=1 -> 3, end=3 + delta=1 -> 4. (3..4).
        val result = applySnapshotSpans(snap, "keXy rest")

        assertThat(result.text).isEqualTo("keXy rest")
        val redSpans = result.spanStyles.filter { it.item == red }
        assertThat(redSpans).hasSize(2)
        val redRanges = redSpans.map { it.start to it.end }
        assertThat(redRanges).containsExactly(0 to 2, 3 to 4).inOrder()
    }

    @Test
    fun `span covering all three regions keeps prefix and suffix tails`() {
        // Regression test for the suffix-tail loss bug. A single span covers prefix +
        // changed + suffix. Old behaviour: the span was clipped to (0..prefixLen) and
        // the suffix tail was silently dropped, causing colour flicker on the unchanged
        // trailing portion of large tokens (multi-line strings, block comments, template
        // literals). New behaviour: emit BOTH the prefix tail at original coordinates and
        // the suffix tail shifted by delta.
        //
        // Setup: "AAA-BBB-CCC" with one green span (0..11) covering everything.
        val snap = snapshot("AAA-BBB-CCC", Triple(0, 11, green))
        // User edits the middle "BBB" to "XXX" -> "AAA-XXX-CCC" (same length, delta=0).
        // Algorithm: prefixLen=4 (matches "AAA-"), suffixLen=4 (matches "-CCC"),
        //   oldChangedEnd=7. Span (0..11) hits the new fourth branch.
        val result = applySnapshotSpans(snap, "AAA-XXX-CCC")

        assertThat(result.text).isEqualTo("AAA-XXX-CCC")
        // Two green spans: prefix tail (0..4) and suffix tail (7..11).
        val greenSpans = result.spanStyles.filter { it.item == green }
        assertThat(greenSpans).hasSize(2)
        val ranges = greenSpans.map { it.start to it.end }
        assertThat(ranges).containsExactly(0 to 4, 7 to 11).inOrder()
    }

    @Test
    fun `span covering all three regions with insertion shifts suffix tail by delta`() {
        // Same shape as the test above but with a length-changing edit, so delta != 0.
        // Setup: "abcXYZdef" with one red span (0..9) covering everything.
        val snap = snapshot("abcXYZdef", Triple(0, 9, red))
        // User replaces "XYZ" with "MMMMM" -> "abcMMMMMdef" (delta=+2).
        // prefixLen=3, suffixLen=3, oldChangedEnd=6, currentText.length=11.
        val result = applySnapshotSpans(snap, "abcMMMMMdef")

        assertThat(result.text).isEqualTo("abcMMMMMdef")
        val redSpans = result.spanStyles.filter { it.item == red }
        assertThat(redSpans).hasSize(2)
        // Prefix tail keeps original coordinates (0..3).
        // Suffix tail: oldChangedEnd=6 + delta=2 -> 8, range.end=9 + delta=2 -> 11.
        val ranges = redSpans.map { it.start to it.end }
        assertThat(ranges).containsExactly(0 to 3, 8 to 11).inOrder()
    }

    // ----- Delete from the middle -----

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

    // ----- Multi-line: suffix lines stay colored -----

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

    // ----- Complete replacement -----

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

    // ----- Prepend, identical, both-empty, edge cases -----

    @Test
    fun `prepend at start - all spans shift by delta`() {
        // The symmetric counterpart of `append at end`. Snapshot has spans starting at the
        // very front; current text has new content prepended. Every span should fall into
        // the unchanged-suffix branch and shift by delta.
        val snap = snapshot("foo", Triple(0, 3, red))
        // Insert "// " at position 0 -> "// foo".
        // prefixLen=0 (empty common prefix), suffixLen=3 (entire "foo" matches).
        // Span (0..3): end=3 > prefixLen=0, start=0 >= oldChangedEnd=0 -> suffix branch.
        // Shifted by delta=+3 -> (3..6).
        val result = applySnapshotSpans(snap, "// foo")

        assertThat(result.text).isEqualTo("// foo")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].start).isEqualTo(3)
        assertThat(result.spanStyles[0].end).isEqualTo(6)
        assertThat(result.spanStyles[0].item).isEqualTo(red)
    }

    @Test
    fun `identical text - all spans transfer cleanly at original offsets`() {
        // No-op short-circuit. prefixLen walks the entire string, suffixLen clamps to 0,
        // every span hits the prefix branch and is applied verbatim. Guards a future
        // refactor against accidentally introducing a copy that loses spans on no-op.
        val snap = snapshot("hello world", Triple(0, 5, red), Triple(6, 11, blue))
        val result = applySnapshotSpans(snap, "hello world")

        assertThat(result.text).isEqualTo("hello world")
        assertThat(result.spanStyles).hasSize(2)
        val ranges = result.spanStyles.map { Triple(it.start, it.end, it.item) }
        assertThat(ranges)
            .containsExactly(
                Triple(0, 5, red),
                Triple(6, 11, blue),
            ).inOrder()
    }

    @Test
    fun `both empty strings - returns empty with no spans`() {
        // minLen=0 so the prefix walk never enters its loop, and the backward suffix walk
        // never enters either. No spans to iterate. Defensive guard against an off-by-one
        // regression in either loop's termination condition.
        val snap = AnnotatedString("")
        val result = applySnapshotSpans(snap, "")

        assertThat(result.text).isEqualTo("")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `span starting in changed region extending into suffix is dropped`() {
        // Symmetric to the four-branch fix from #228, but verifies the deliberately-dropped
        // case. A span whose start position is invalidated by the edit cannot be partially
        // revived even if its end lies in the unchanged suffix - the start coordinate is
        // unsafe. The fresh highlight result will arrive shortly via debounce.
        //
        // Setup: "abcdef" with green span (2..5). User replaces "bcd" with "XY"
        // -> "aXYef". prefixLen=1 ('a'), suffixLen=2 ('ef'), clamp=2, oldChangedEnd=4.
        // Span (2..5): end=5 > prefixLen=1, start=2 < oldChangedEnd=4 (not in suffix),
        // start=2 >= prefixLen=1 (not in prefix or straddling prefix-suffix). Falls
        // through to the implicit drop branch.
        val snap = snapshot("abcdef", Triple(2, 5, green))
        val result = applySnapshotSpans(snap, "aXYef")

        assertThat(result.text).isEqualTo("aXYef")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `zero width span at prefix boundary is preserved`() {
        // Compose's AnnotatedString accepts zero-width SpanStyles. The first when-branch
        // applies any span where range.end <= prefixLen as-is, including zero-width ones.
        // Verifies that a (start == end) span exactly at the prefix boundary survives the
        // transfer. (Zero-width spans inside the changed region are dropped via the
        // length guards in branches 2 and 4.)
        val snap = snapshot("abc", Triple(2, 2, red))
        // Insert "X" at position 2 -> "abXc". prefixLen=2 ('ab'), suffixLen=1 ('c'),
        // oldChangedEnd=2. Span (2..2): end=2 <= prefixLen=2 -> first branch, applied as-is.
        val result = applySnapshotSpans(snap, "abXc")

        assertThat(result.text).isEqualTo("abXc")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].start).isEqualTo(2)
        assertThat(result.spanStyles[0].end).isEqualTo(2)
    }

    @Test
    fun `zero width span strictly inside changed region is dropped`() {
        // A zero-width span where prefixLen < start < oldChangedEnd lies entirely in the
        // changed region. None of the four when-branches accept it (end > prefixLen,
        // start < oldChangedEnd, start >= prefixLen, range.end == start so range.end !>
        // oldChangedEnd). Falls through to the implicit drop branch.
        val snap = snapshot("abcd", Triple(2, 2, red))
        // Replace "bc" with "Z" -> "aZd". prefixLen=1 ('a'), suffixLen=1 ('d'),
        // clamp=1, oldChangedEnd=3. Span (2..2): end=2 > prefixLen=1, start=2 < oldChangedEnd=3.
        // Three-region branch: start=2 < prefixLen=1? No. Third branch: same condition. Dropped.
        val result = applySnapshotSpans(snap, "aZd")

        assertThat(result.text).isEqualTo("aZd")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `edit smaller than prefix plus suffix clamps overlap`() {
        // Exercises the prefix+suffix overlap clamp inside applySnapshotSpans. Without it,
        // prefixLen+suffixLen could exceed min(oldLen, newLen), causing the suffix range to
        // overlap the prefix range and double-cover characters in the result.
        //
        // Setup: "abab" with red span (0..4). User deletes the second "ab" -> "ab" (delta=-2).
        // prefixLen walks 'a'=='a', 'b'=='b', minLen=2 reached -> prefixLen=2.
        // Backward walk: old[3]='b'=new[1]='b', old[2]='a'=new[0]='a', old[1]='b' vs (none) ->
        // rawSuffixLen=2.
        // Without the clamp: oldChangedEnd would be 4-2=2, and the four-branch case would emit
        // prefix (0..2) AND suffix (oldChangedEnd+delta=0..range.end+delta=2) = (0..2) -
        // double-covering the same characters.
        // With the clamp: min(2, 4-2=2, 2-2=0) = 0. oldChangedEnd=4. Span (0..4):
        //   end=4 > prefixLen=2, start=0 < oldChangedEnd=4. Three-region branch:
        //   start=0 < prefixLen=2 && end=4 > oldChangedEnd=4? No (4 > 4 is false).
        //   Third branch: start=0 < prefixLen=2 -> clip to (0..2). Result has exactly one
        //   span at (0..2), no double-coverage.
        val snap = snapshot("abab", Triple(0, 4, red))
        val result = applySnapshotSpans(snap, "ab")

        assertThat(result.text).isEqualTo("ab")
        // Exactly one red span; the clamp prevented the suffix branch from also emitting
        // an overlapping (0..2) range.
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].start).isEqualTo(0)
        assertThat(result.spanStyles[0].end).isEqualTo(2)
        assertThat(result.spanStyles[0].item).isEqualTo(red)
    }
}
