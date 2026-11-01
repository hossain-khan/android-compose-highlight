package dev.hossain.highlight.engine

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration

/**
 * JVM unit tests for [ThemedHighlightResult].
 *
 * Verifies field accessibility, equals/hashCode/toString behavior,
 * and timing-based inequality for the dual-theme highlight result class.
 */
class ThemedHighlightResultTest {
    private val zeroTimings =
        HighlightTimings(
            jsBridge = Duration.ZERO,
            jsonUnescape = Duration.ZERO,
            htmlParse = Duration.ZERO,
            themeParse = Duration.ZERO,
            total = Duration.ZERO,
        )

    @Test
    fun `properties are accessible`() {
        val light = AnnotatedString("light text")
        val dark = AnnotatedString("dark text")
        val result = ThemedHighlightResult(light = light, dark = dark, durationMs = 42, timings = zeroTimings)

        assertThat(result.light.text).isEqualTo("light text")
        assertThat(result.dark.text).isEqualTo("dark text")
        assertThat(result.durationMs).isEqualTo(42)
        assertThat(result.timings).isEqualTo(zeroTimings)
    }

    @Test
    fun `equals and hashCode work for identical content`() {
        val light = AnnotatedString("x")
        val dark = AnnotatedString("y")
        val a = ThemedHighlightResult(light, dark, 10, zeroTimings)
        val b = ThemedHighlightResult(light, dark, 10, zeroTimings)
        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `not equal when durationMs differs`() {
        val light = AnnotatedString("x")
        val dark = AnnotatedString("y")
        val a = ThemedHighlightResult(light, dark, 10, zeroTimings)
        val b = ThemedHighlightResult(light, dark, 20, zeroTimings)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `not equal when timings differ`() {
        val light = AnnotatedString("x")
        val dark = AnnotatedString("y")
        val otherTimings =
            HighlightTimings(
                jsBridge = Duration.parse("5ms"),
                jsonUnescape = Duration.ZERO,
                htmlParse = Duration.ZERO,
                themeParse = Duration.ZERO,
                total = Duration.ZERO,
            )
        val a = ThemedHighlightResult(light, dark, 10, zeroTimings)
        val b = ThemedHighlightResult(light, dark, 10, otherTimings)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `toString contains field values`() {
        val result =
            ThemedHighlightResult(
                light = AnnotatedString("a"),
                dark = AnnotatedString("b"),
                durationMs = 100,
                timings = zeroTimings,
            )
        val str = result.toString()
        assertThat(str).contains("ThemedHighlightResult")
        assertThat(str).contains("durationMs=100")
    }
}
