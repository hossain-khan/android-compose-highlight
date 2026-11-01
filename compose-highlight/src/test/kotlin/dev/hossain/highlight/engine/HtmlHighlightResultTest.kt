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
}
