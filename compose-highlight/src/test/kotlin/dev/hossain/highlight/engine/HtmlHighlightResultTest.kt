package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * JVM unit tests for [HtmlHighlightResult].
 *
 * Verifies field storage and durationMs edge cases (zero, large values)
 * for the raw HTML highlight result data class.
 */
class HtmlHighlightResultTest {
    // ----- Construction -----

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

    // ----- durationMs semantics -----

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

    @Test
    fun `equals hashCode and toString work as expected`() {
        val a = HtmlHighlightResult(html = "<span>x</span>", durationMs = 10L)
        val b = HtmlHighlightResult(html = "<span>x</span>", durationMs = 10L)
        val different = HtmlHighlightResult(html = "<span>y</span>", durationMs = 10L)

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
        assertThat(a).isNotEqualTo(different)
        assertThat(a.toString()).contains("HtmlHighlightResult")
        assertThat(a.toString()).contains("durationMs=10")
    }
}
