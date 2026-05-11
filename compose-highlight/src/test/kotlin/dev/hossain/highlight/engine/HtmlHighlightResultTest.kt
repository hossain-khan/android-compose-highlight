package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HtmlHighlightResultTest {
    // ── Construction ─────────────────────────────────────────────────────────

    @Test
    fun `fields are stored as-is`() {
        val result =
            HtmlHighlightResult(
                html = "<span class=\"hljs-keyword\">val</span> x = 42",
                durationMs = 18L,
            )

        assertThat(result.html).contains("hljs-keyword")
        assertThat(result.durationMs).isEqualTo(18L)
    }

    // ── durationMs semantics ─────────────────────────────────────────────────

    @Test
    fun `durationMs of zero is valid`() {
        val result = HtmlHighlightResult(html = "<span>code</span>", durationMs = 0L)

        assertThat(result.durationMs).isEqualTo(0L)
    }

    @Test
    fun `durationMs stores large values without overflow`() {
        val large = 100_000L
        val result = HtmlHighlightResult(html = "", durationMs = large)

        assertThat(result.durationMs).isEqualTo(large)
    }

    // ── data class contracts ─────────────────────────────────────────────────

    @Test
    fun `equals is true for identical instances`() {
        val html = "<span>code</span>"
        val a = HtmlHighlightResult(html = html, durationMs = 10L)
        val b = HtmlHighlightResult(html = html, durationMs = 10L)

        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `equals is false when html differs`() {
        val a = HtmlHighlightResult(html = "<span>foo</span>", durationMs = 10L)
        val b = HtmlHighlightResult(html = "<span>bar</span>", durationMs = 10L)

        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals is false when durationMs differs`() {
        val html = "<span>code</span>"
        val a = HtmlHighlightResult(html = html, durationMs = 10L)
        val b = HtmlHighlightResult(html = html, durationMs = 99L)

        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `hashCode is equal for equal instances`() {
        val html = "<span>code</span>"
        val a = HtmlHighlightResult(html = html, durationMs = 10L)
        val b = HtmlHighlightResult(html = html, durationMs = 10L)

        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `copy preserves all fields when no override`() {
        val original = HtmlHighlightResult(html = "<span>code</span>", durationMs = 10L)
        val copy = original.copy()

        assertThat(copy).isEqualTo(original)
    }

    @Test
    fun `copy can override durationMs`() {
        val original = HtmlHighlightResult(html = "<span>code</span>", durationMs = 10L)
        val updated = original.copy(durationMs = 99L)

        assertThat(updated.durationMs).isEqualTo(99L)
        assertThat(updated.html).isEqualTo(original.html)
    }

    @Test
    fun `toString contains field values`() {
        val result = HtmlHighlightResult(html = "<span>code</span>", durationMs = 42L)
        val str = result.toString()

        assertThat(str).contains("durationMs=42")
    }
}
