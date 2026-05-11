package dev.hossain.highlight.engine

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HighlightResultTest {
    private val sampleAnnotated = AnnotatedString("fun hello() = \"world\"")

    // ── Construction ─────────────────────────────────────────────────────────

    @Test
    fun `fields are stored as-is`() {
        val result =
            HighlightResult(
                annotated = sampleAnnotated,
                spanCount = 5,
                language = "kotlin",
                durationMs = 42L,
            )

        assertThat(result.annotated).isEqualTo(sampleAnnotated)
        assertThat(result.spanCount).isEqualTo(5)
        assertThat(result.language).isEqualTo("kotlin")
        assertThat(result.durationMs).isEqualTo(42L)
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
            )

        assertThat(result.language).isEqualTo("python")
    }

    @Test
    fun `language field is case-sensitive`() {
        val lower = HighlightResult(sampleAnnotated, 1, "kotlin", 1L)
        val upper = HighlightResult(sampleAnnotated, 1, "Kotlin", 1L)

        assertThat(lower.language).isNotEqualTo(upper.language)
    }

    // ── durationMs semantics ─────────────────────────────────────────────────

    @Test
    fun `durationMs of zero is valid`() {
        val result = HighlightResult(sampleAnnotated, 0, "plaintext", durationMs = 0L)

        assertThat(result.durationMs).isEqualTo(0L)
    }

    @Test
    fun `durationMs stores large values without overflow`() {
        val large = 100_000L
        val result = HighlightResult(sampleAnnotated, 1, "kotlin", durationMs = large)

        assertThat(result.durationMs).isEqualTo(large)
    }

    // ── data class contracts ─────────────────────────────────────────────────

    @Test
    fun `equals is true for identical instances`() {
        val a = HighlightResult(sampleAnnotated, 5, "kotlin", 42L)
        val b = HighlightResult(sampleAnnotated, 5, "kotlin", 42L)

        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `equals is false when spanCount differs`() {
        val a = HighlightResult(sampleAnnotated, 5, "kotlin", 42L)
        val b = HighlightResult(sampleAnnotated, 0, "kotlin", 42L)

        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals is false when language differs`() {
        val a = HighlightResult(sampleAnnotated, 5, "kotlin", 42L)
        val b = HighlightResult(sampleAnnotated, 5, "python", 42L)

        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `equals is false when durationMs differs`() {
        val a = HighlightResult(sampleAnnotated, 5, "kotlin", 10L)
        val b = HighlightResult(sampleAnnotated, 5, "kotlin", 99L)

        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `hashCode is equal for equal instances`() {
        val a = HighlightResult(sampleAnnotated, 5, "kotlin", 42L)
        val b = HighlightResult(sampleAnnotated, 5, "kotlin", 42L)

        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `copy preserves all fields when no override`() {
        val original = HighlightResult(sampleAnnotated, 5, "kotlin", 42L)
        val copy = original.copy()

        assertThat(copy).isEqualTo(original)
    }

    @Test
    fun `copy can override spanCount`() {
        val original = HighlightResult(sampleAnnotated, 5, "kotlin", 42L)
        val updated = original.copy(spanCount = 0)

        assertThat(updated.spanCount).isEqualTo(0)
        assertThat(updated.language).isEqualTo("kotlin")
        assertThat(updated.durationMs).isEqualTo(42L)
    }

    @Test
    fun `copy can override language`() {
        val original = HighlightResult(sampleAnnotated, 5, "kotlin", 42L)
        val updated = original.copy(language = "python")

        assertThat(updated.language).isEqualTo("python")
        assertThat(updated.spanCount).isEqualTo(5)
    }

    @Test
    fun `toString contains field values`() {
        val result = HighlightResult(sampleAnnotated, 3, "kotlin", 55L)
        val str = result.toString()

        assertThat(str).contains("spanCount=3")
        assertThat(str).contains("language=kotlin")
        assertThat(str).contains("durationMs=55")
    }
}
