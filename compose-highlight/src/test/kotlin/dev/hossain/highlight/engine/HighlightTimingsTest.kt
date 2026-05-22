package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration

class HighlightTimingsTest {
    private val sampleTimings =
        HighlightTimings(
            jsBridge = Duration.parse("45ms"),
            jsonUnescape = Duration.parse("1ms"),
            htmlParse = Duration.parse("3ms"),
            treeWalk = Duration.parse("2ms"),
            themeParse = Duration.parse("10ms"),
            total = Duration.parse("61ms"),
        )

    // ── Construction ─────────────────────────────────────────────────────────

    @Test
    fun `all fields are stored as-is`() {
        assertThat(sampleTimings.jsBridge).isEqualTo(Duration.parse("45ms"))
        assertThat(sampleTimings.jsonUnescape).isEqualTo(Duration.parse("1ms"))
        assertThat(sampleTimings.htmlParse).isEqualTo(Duration.parse("3ms"))
        assertThat(sampleTimings.treeWalk).isEqualTo(Duration.parse("2ms"))
        assertThat(sampleTimings.themeParse).isEqualTo(Duration.parse("10ms"))
        assertThat(sampleTimings.total).isEqualTo(Duration.parse("61ms"))
    }

    @Test
    fun `Duration ZERO is valid for all fields`() {
        val timings =
            HighlightTimings(
                jsBridge = Duration.ZERO,
                jsonUnescape = Duration.ZERO,
                htmlParse = Duration.ZERO,
                treeWalk = Duration.ZERO,
                themeParse = Duration.ZERO,
                total = Duration.ZERO,
            )

        assertThat(timings.jsBridge).isEqualTo(Duration.ZERO)
        assertThat(timings.total).isEqualTo(Duration.ZERO)
    }

    // ── Duration conversions ──────────────────────────────────────────────────

    @Test
    fun `inWholeMilliseconds converts duration correctly`() {
        assertThat(sampleTimings.jsBridge.inWholeMilliseconds).isEqualTo(45L)
        assertThat(sampleTimings.total.inWholeMilliseconds).isEqualTo(61L)
    }

    @Test
    fun `inWholeNanoseconds converts duration correctly`() {
        assertThat(Duration.parse("1ms").inWholeNanoseconds).isEqualTo(1_000_000L)
    }

    @Test
    fun `themeParse is zero for cache hit scenario`() {
        val cachedTimings = sampleTimings.copy(themeParse = Duration.ZERO)
        assertThat(cachedTimings.themeParse).isEqualTo(Duration.ZERO)
    }
}
