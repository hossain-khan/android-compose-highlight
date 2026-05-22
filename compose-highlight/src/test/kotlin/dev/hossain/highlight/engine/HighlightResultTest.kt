package dev.hossain.highlight.engine

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration

class HighlightResultTest {
    private val sampleAnnotated = AnnotatedString("fun hello() = \"world\"")
    private val zeroTimings =
        HighlightTimings(
            jsBridge = Duration.ZERO,
            jsonUnescape = Duration.ZERO,
            htmlParse = Duration.ZERO,
            treeWalk = Duration.ZERO,
            themeParse = Duration.ZERO,
            total = Duration.ZERO,
        )

    // ── Construction ─────────────────────────────────────────────────────────

    @Test
    fun `fields are stored as-is`() {
        val result =
            HighlightResult(
                annotated = sampleAnnotated,
                spanCount = 5,
                language = "kotlin",
                durationMs = 42L,
                timings = zeroTimings,
            )

        assertThat(result.annotated).isEqualTo(sampleAnnotated)
        assertThat(result.spanCount).isEqualTo(5)
        assertThat(result.language).isEqualTo("kotlin")
        assertThat(result.durationMs).isEqualTo(42L)
        assertThat(result.timings).isEqualTo(zeroTimings)
    }

    // ── spanCount semantics ───────────────────────────────────────────────────

    @Test
    fun `spanCount zero signals silent failure`() {
        val result =
            HighlightResult(
                annotated = AnnotatedString("plain text"),
                spanCount = 0,
                language = "not-a-real-language",
                durationMs = 10L,
                timings = zeroTimings,
            )

        assertThat(result.spanCount).isEqualTo(0)
    }

    @Test
    fun `spanCount positive signals successful highlighting`() {
        val result =
            HighlightResult(
                annotated = sampleAnnotated,
                spanCount = 3,
                language = "kotlin",
                durationMs = 20L,
                timings = zeroTimings,
            )

        assertThat(result.spanCount).isGreaterThan(0)
    }

    // ── language field ───────────────────────────────────────────────────────

    @Test
    fun `language field preserves requested identifier exactly`() {
        val result =
            HighlightResult(
                annotated = sampleAnnotated,
                spanCount = 2,
                language = "python",
                durationMs = 15L,
                timings = zeroTimings,
            )

        assertThat(result.language).isEqualTo("python")
    }

    @Test
    fun `language field is case-sensitive`() {
        val lower = HighlightResult(sampleAnnotated, 1, "kotlin", 1L, zeroTimings)
        val upper = HighlightResult(sampleAnnotated, 1, "Kotlin", 1L, zeroTimings)

        assertThat(lower.language).isNotEqualTo(upper.language)
    }

    // ── durationMs semantics ─────────────────────────────────────────────────

    @Test
    fun `durationMs of zero is valid`() {
        val result = HighlightResult(sampleAnnotated, 0, "plaintext", durationMs = 0L, timings = zeroTimings)

        assertThat(result.durationMs).isEqualTo(0L)
    }

    @Test
    fun `durationMs stores large values without overflow`() {
        val large = 100_000L
        val result = HighlightResult(sampleAnnotated, 1, "kotlin", durationMs = large, timings = zeroTimings)

        assertThat(result.durationMs).isEqualTo(large)
    }

    // ── timings field ─────────────────────────────────────────────────────────

    @Test
    fun `timings field is stored as-is`() {
        val timings =
            HighlightTimings(
                jsBridge = Duration.parse("45ms"),
                jsonUnescape = Duration.parse("1ms"),
                htmlParse = Duration.parse("3ms"),
                treeWalk = Duration.parse("2ms"),
                themeParse = Duration.parse("10ms"),
                total = Duration.parse("61ms"),
            )
        val result =
            HighlightResult(
                annotated = sampleAnnotated,
                spanCount = 5,
                language = "kotlin",
                durationMs = 61L,
                timings = timings,
            )

        assertThat(result.timings).isEqualTo(timings)
        assertThat(result.timings.jsBridge).isEqualTo(Duration.parse("45ms"))
        assertThat(result.timings.total).isEqualTo(Duration.parse("61ms"))
    }
}
