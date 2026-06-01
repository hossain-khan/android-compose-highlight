package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * JVM unit tests for [HighlightLanguageInfo].
 *
 * Verifies field storage, equals/hashCode contract, copy behavior,
 * and toString output for the language metadata data class.
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
    fun `copy produces equal instance`() {
        val original = HighlightLanguageInfo(name = "Kotlin", aliases = listOf("kt", "kts"))
        val copied = original.copy()
        assertThat(copied).isEqualTo(original)
    }

    @Test
    fun `copy with override changes the field`() {
        val original = HighlightLanguageInfo(name = "Kotlin", aliases = listOf("kt", "kts"))
        val overridden = original.copy(name = "Java")
        assertThat(overridden.name).isEqualTo("Java")
        assertThat(overridden.aliases).containsExactly("kt", "kts").inOrder()
    }

    @Test
    fun `toString contains class name`() {
        val original = HighlightLanguageInfo(name = "Kotlin", aliases = listOf("kt", "kts"))
        val str = original.toString()
        assertThat(str).contains("name=Kotlin")
        assertThat(str).contains("aliases=[kt, kts]")
    }
}
