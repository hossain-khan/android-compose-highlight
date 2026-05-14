package dev.hossain.highlight.engine

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemedHighlightResultTest {
    @Test
    fun `data class properties are accessible`() {
        val light = AnnotatedString("light text")
        val dark = AnnotatedString("dark text")
        val result = ThemedHighlightResult(light = light, dark = dark, durationMs = 42)

        assertThat(result.light.text).isEqualTo("light text")
        assertThat(result.dark.text).isEqualTo("dark text")
        assertThat(result.durationMs).isEqualTo(42)
    }

    @Test
    fun `equals works for identical content`() {
        val light = AnnotatedString("x")
        val dark = AnnotatedString("y")
        val a = ThemedHighlightResult(light, dark, 10)
        val b = ThemedHighlightResult(light, dark, 10)
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `not equal when durationMs differs`() {
        val light = AnnotatedString("x")
        val dark = AnnotatedString("y")
        val a = ThemedHighlightResult(light, dark, 10)
        val b = ThemedHighlightResult(light, dark, 20)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `destructuring works`() {
        val light = AnnotatedString("light")
        val dark = AnnotatedString("dark")
        val result = ThemedHighlightResult(light, dark, 5)
        val (l, d, ms) = result
        assertThat(l.text).isEqualTo("light")
        assertThat(d.text).isEqualTo("dark")
        assertThat(ms).isEqualTo(5)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val result =
            ThemedHighlightResult(
                light = AnnotatedString("a"),
                dark = AnnotatedString("b"),
                durationMs = 100,
            )
        val copied = result.copy(durationMs = 200)
        assertThat(copied.light.text).isEqualTo("a")
        assertThat(copied.dark.text).isEqualTo("b")
        assertThat(copied.durationMs).isEqualTo(200)
    }
}
