package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration

/**
 * JVM unit tests for [HighlightTimings].
 *
 * Verifies that all timing fields are stored correctly, Duration.ZERO
 * is valid for all fields, and duration unit conversions work as expected.
 */
class HighlightTimingsTest {
    private val sampleTimings =
        HighlightTimings(
            jsBridge = Duration.parse("45ms"),
            jsonUnescape = Duration.parse("1ms"),
            htmlParse = Duration.parse("3ms"),
            themeParse = Duration.parse("10ms"),
            total = Duration.parse("59ms"),
        )

    // ----- Construction -----

    @Test
    fun `all fields are stored as-is`() {
        assertThat(sampleTimings.jsBridge).isEqualTo(Duration.parse("45ms"))
        assertThat(sampleTimings.jsonUnescape).isEqualTo(Duration.parse("1ms"))
        assertThat(sampleTimings.htmlParse).isEqualTo(Duration.parse("3ms"))
        assertThat(sampleTimings.themeParse).isEqualTo(Duration.parse("10ms"))
        assertThat(sampleTimings.total).isEqualTo(Duration.parse("59ms"))
    }

    @Test
    fun `Duration ZERO is valid for all fields`() {
        val timings =
            HighlightTimings(
                jsBridge = Duration.ZERO,
                jsonUnescape = Duration.ZERO,
                htmlParse = Duration.ZERO,
                themeParse = Duration.ZERO,
                total = Duration.ZERO,
            )

        assertThat(timings.jsBridge).isEqualTo(Duration.ZERO)
        assertThat(timings.total).isEqualTo(Duration.ZERO)
    }

    // ----- Duration conversions -----

    @Test
    fun `inWholeMilliseconds converts duration correctly`() {
        assertThat(sampleTimings.jsBridge.inWholeMilliseconds).isEqualTo(45L)
        assertThat(sampleTimings.total.inWholeMilliseconds).isEqualTo(59L)
    }

    @Test
    fun `inWholeNanoseconds converts duration correctly`() {
        assertThat(Duration.parse("1ms").inWholeNanoseconds).isEqualTo(1_000_000L)
    }

    @Test
    fun `themeParse is zero for cache hit scenario`() {
        val cachedTimings =
            HighlightTimings(
                jsBridge = sampleTimings.jsBridge,
                jsonUnescape = sampleTimings.jsonUnescape,
                htmlParse = sampleTimings.htmlParse,
                themeParse = Duration.ZERO,
                total = sampleTimings.total,
            )
        assertThat(cachedTimings.themeParse).isEqualTo(Duration.ZERO)
        assertThat(cachedTimings).isNotEqualTo(sampleTimings)
    }

    @Test
    fun `equals hashCode and toString work as expected`() {
        val equalTimings =
            HighlightTimings(
                jsBridge = Duration.parse("45ms"),
                jsonUnescape = Duration.parse("1ms"),
                htmlParse = Duration.parse("3ms"),
                themeParse = Duration.parse("10ms"),
                total = Duration.parse("59ms"),
            )
        assertThat(sampleTimings).isEqualTo(equalTimings)
        assertThat(sampleTimings.hashCode()).isEqualTo(equalTimings.hashCode())
        assertThat(sampleTimings.toString()).contains("HighlightTimings")
    }
}
