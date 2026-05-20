package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HighlightLanguageInfoTest {
    @Test
    fun `fields are stored as-is`() {
        val info = HighlightLanguageInfo(name = "JavaScript", aliases = listOf("js", "jsx"))

        assertThat(info.name).isEqualTo("JavaScript")
        assertThat(info.aliases).containsExactly("js", "jsx").inOrder()
    }

    @Test
    fun `equals hashCode copy and toString contracts`() {
        val original = HighlightLanguageInfo(name = "Kotlin", aliases = listOf("kt", "kts"))
        val equal = HighlightLanguageInfo(name = "Kotlin", aliases = listOf("kt", "kts"))
        val copied = original.copy()
        val overridden = original.copy(name = "Java")
        val str = original.toString()

        assertThat(original).isEqualTo(equal)
        assertThat(original.hashCode()).isEqualTo(equal.hashCode())
        assertThat(copied).isEqualTo(original)
        assertThat(overridden.name).isEqualTo("Java")
        assertThat(overridden.aliases).containsExactly("kt", "kts").inOrder()
        assertThat(str).contains("name=Kotlin")
        assertThat(str).contains("aliases=[kt, kts]")
    }
}
