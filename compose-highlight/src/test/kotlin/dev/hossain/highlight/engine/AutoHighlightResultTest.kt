package dev.hossain.highlight.engine

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration

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
    fun `equals hashCode copy and toString contracts`() {
        val original =
            AutoHighlightResult(
                annotated = sampleAnnotated,
                detectedLanguage = "kotlin",
                spanCount = 3,
                durationMs = 55L,
                timings = zeroTimings,
            )
        val equal =
            AutoHighlightResult(
                annotated = sampleAnnotated,
                detectedLanguage = "kotlin",
                spanCount = 3,
                durationMs = 55L,
                timings = zeroTimings,
            )
        val copied = original.copy()
        val overridden = original.copy(detectedLanguage = "python")
        val str = original.toString()

        assertThat(original).isEqualTo(equal)
        assertThat(original.hashCode()).isEqualTo(equal.hashCode())
        assertThat(copied).isEqualTo(original)
        assertThat(overridden.detectedLanguage).isEqualTo("python")
        assertThat(overridden.spanCount).isEqualTo(3)
        assertThat(str).contains("detectedLanguage=kotlin")
        assertThat(str).contains("spanCount=3")
        assertThat(str).contains("durationMs=55")
    }
}
