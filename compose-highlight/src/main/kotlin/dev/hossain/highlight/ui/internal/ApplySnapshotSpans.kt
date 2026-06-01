package dev.hossain.highlight.ui.internal

import androidx.compose.ui.text.AnnotatedString

/**
 * Transfers span styles from [snapshotAnnotated] onto [currentText], preserving spans in
 * the **unchanged prefix and suffix** regions and discarding spans that cover the edited region.
 *
 * The text is split into three regions using the longest common prefix and suffix:
 * - **Prefix** (before the edit point): spans are at the same positions in both strings -
 *   apply as-is.
 * - **Suffix** (after the edit point, unchanged trailing text): spans have shifted by
 *   `currentText.length - snapshotAnnotated.text.length` - apply with that offset.
 * - **Changed region** (between prefix and suffix): spans whose start lies here are dropped;
 *   the start position is invalidated by the edit, so partial revival is unsafe.
 * - **Prefix-to-suffix straddling spans** (start in prefix, end past the changed region into
 *   the suffix): both unchanged tails are kept. The prefix tail uses its original coordinates;
 *   the suffix tail shifts by delta. Without this case, large spans like multi-line strings or
 *   block comments would lose their colour on the unchanged trailing portion during the
 *   debounce window.
 * - **Prefix-to-changed straddling spans** (start in prefix, end in the changed region): clipped
 *   to the prefix boundary so the unedited leading portion of the token stays coloured.
 *
 * This keeps syntax colors correct on all lines **above and below** a mid-text edit during the
 * debounce window, not just lines before the edit. For the common **append-at-end** case the
 * suffix length is zero and the prefix equals the entire old text, so all old spans carry over
 * unchanged with no regression.
 */
internal fun applySnapshotSpans(
    snapshotAnnotated: AnnotatedString,
    currentText: String,
): AnnotatedString {
    val oldText = snapshotAnnotated.text

    // Use index-based loops instead of commonPrefixWith()/reversed() to avoid allocating
    // intermediate String copies. commonPrefixWith returns a new substring, and reversed()
    // copies the entire string before comparing - for large editor content (5-20 KB) this
    // produces ~4 temporary strings totalling 2x the document size on every recomposition
    // during the debounce window, increasing GC pressure while the user is typing.
    // Index loops are O(n) with zero allocations and identical behavior.
    var prefixLen = 0
    val minLen = minOf(oldText.length, currentText.length)
    while (prefixLen < minLen && oldText[prefixLen] == currentText[prefixLen]) {
        prefixLen++
    }

    // Walk backwards from both ends to find the common suffix length.
    var rawSuffixLen = 0
    var oldIdx = oldText.length - 1
    var newIdx = currentText.length - 1
    while (oldIdx >= 0 && newIdx >= 0 && oldText[oldIdx] == currentText[newIdx]) {
        rawSuffixLen++
        oldIdx--
        newIdx--
    }

    // Clamp so prefix + suffix <= min(oldLen, newLen), preventing overlap when the edit
    // is smaller than the surrounding unchanged regions.
    val suffixLen =
        rawSuffixLen
            .coerceAtMost(oldText.length - prefixLen)
            .coerceAtMost(currentText.length - prefixLen)

    val oldChangedEnd = oldText.length - suffixLen // first suffix char in old text
    val delta = currentText.length - oldText.length

    val builder = AnnotatedString.Builder(currentText)
    snapshotAnnotated.spanStyles.forEach { range ->
        when {
            // Entirely within the unchanged prefix - apply as-is.
            range.end <= prefixLen -> {
                builder.addStyle(range.item, range.start, range.end)
            }

            // Entirely within the unchanged suffix - shift by delta.
            range.start >= oldChangedEnd -> {
                val newStart = (range.start + delta).coerceAtLeast(0)
                val newEnd = (range.end + delta).coerceAtMost(currentText.length)
                if (newStart < newEnd) builder.addStyle(range.item, newStart, newEnd)
            }

            // Starts in the prefix AND extends past the changed region into the suffix.
            // Both unchanged tails are recoverable: the prefix tail keeps its original
            // coordinates, and the suffix tail shifts by delta. Without this branch, the
            // suffix tail would be silently dropped - visible as colour flicker on the
            // right-hand side of multi-line strings, block comments, and template
            // literals while the user is mid-edit.
            range.start < prefixLen && range.end > oldChangedEnd -> {
                builder.addStyle(range.item, range.start, prefixLen)
                val suffixStart = oldChangedEnd + delta
                val suffixEnd = (range.end + delta).coerceAtMost(currentText.length)
                if (suffixStart < suffixEnd) builder.addStyle(range.item, suffixStart, suffixEnd)
            }

            // Starts in the prefix but ends in the changed region - clip to prefix.
            range.start < prefixLen -> {
                builder.addStyle(range.item, range.start, prefixLen)
            }
            // Starts in the changed region - drop. The start position is invalidated by
            // the edit, so even if the end is in the suffix the span is unsafe to revive
            // partially. The fresh highlight result will arrive shortly via debounce.
        }
    }
    return builder.toAnnotatedString()
}
