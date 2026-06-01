package dev.hossain.highlight.engine

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration

/**
 * JVM unit tests for [AutoHighlightResult].
 *
 * Verifies field storage, detectedLanguage edge cases, equals/hashCode
 * contract, copy behavior, and toString output for the auto-detection
 * highlight result data class.
 */
class AutoHighlightResultTest {
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

    @Test
    fun `fields are stored as-is`() {
        val result =
            AutoHighlightResult(
                annotated = sampleAnnotated,
                detectedLanguage = "kotlin",
                spanCount = 5,
                durationMs = 42L,
                timings = zeroTimings,
            )

        assertThat(result.annotated).isEqualTo(sampleAnnotated)
        assertThat(result.detectedLanguage).isEqualTo("kotlin")
        assertThat(result.spanCount).isEqualTo(5)
        assertThat(result.durationMs).isEqualTo(42L)
        assertThat(result.timings).isEqualTo(zeroTimings)
    }

    @Test
    fun `detectedLanguage can be empty string`() {
        val result =
            AutoHighlightResult(
                annotated = AnnotatedString("plain text"),
                detectedLanguage = "",
                spanCount = 0,
                durationMs = 10L,
                timings = zeroTimings,
            )

        assertThat(result.detectedLanguage).isEmpty()
    }

    @Test
    fun `equal instances have same hashCode`() {
        val a =
            AutoHighlightResult(
                annotated = sampleAnnotated,
                detectedLanguage = "kotlin",
                spanCount = 3,
                durationMs = 55L,
                timings = zeroTimings,
            )
        val b =
            AutoHighlightResult(
                annotated = sampleAnnotated,
                detectedLanguage = "kotlin",
                spanCount = 3,
                durationMs = 55L,
                timings = zeroTimings,
            )
        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `copy produces equal instance`() {
        val original =
            AutoHighlightResult(
                annotated = sampleAnnotated,
                detectedLanguage = "kotlin",
                spanCount = 3,
                durationMs = 55L,
                timings = zeroTimings,
            )
        val copied = original.copy()
        assertThat(copied).isEqualTo(original)
    }

    @Test
    fun `copy with override changes the field`() {
        val original =
            AutoHighlightResult(
                annotated = sampleAnnotated,
                detectedLanguage = "kotlin",
                spanCount = 3,
                durationMs = 55L,
                timings = zeroTimings,
            )
        val overridden = original.copy(detectedLanguage = "python")
        assertThat(overridden.detectedLanguage).isEqualTo("python")
        assertThat(overridden.spanCount).isEqualTo(3)
    }

    @Test
    fun `toString contains class name`() {
        val original =
            AutoHighlightResult(
                annotated = sampleAnnotated,
                detectedLanguage = "kotlin",
                spanCount = 3,
                durationMs = 55L,
                timings = zeroTimings,
            )
        val str = original.toString()
        assertThat(str).contains("detectedLanguage=kotlin")
        assertThat(str).contains("spanCount=3")
        assertThat(str).contains("durationMs=55")
    }
}
