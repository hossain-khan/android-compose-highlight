package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * JVM unit tests for [HighlightLanguageInfo].
 *
 * Verifies field storage, equals/hashCode contract,
 * and toString output for the language metadata class.
 */
class HighlightLanguageInfoTest {
    @Test
    fun `fields are stored as-is`() {
        val info = HighlightLanguageInfo(name = "JavaScript", aliases = listOf("js", "jsx"))

        assertThat(info.name).isEqualTo("JavaScript")
        assertThat(info.aliases).containsExactly("js", "jsx").inOrder()
    }

    @Test
    fun `equal instances have same hashCode`() {
        val original = HighlightLanguageInfo(name = "Kotlin", aliases = listOf("kt", "kts"))
        val equal = HighlightLanguageInfo(name = "Kotlin", aliases = listOf("kt", "kts"))
        assertThat(original).isEqualTo(equal)
        assertThat(original.hashCode()).isEqualTo(equal.hashCode())
    }

    @Test
    fun `instances with different fields are not equal`() {
        val kotlin = HighlightLanguageInfo(name = "Kotlin", aliases = listOf("kt", "kts"))
        val java = HighlightLanguageInfo(name = "Java", aliases = listOf("kt", "kts"))
        assertThat(kotlin).isNotEqualTo(java)
    }

    @Test
    fun `toString contains class name`() {
        val original = HighlightLanguageInfo(name = "Kotlin", aliases = listOf("kt", "kts"))
        val str = original.toString()
        assertThat(str).contains("name=Kotlin")
        assertThat(str).contains("aliases=[kt, kts]")
    }
}
