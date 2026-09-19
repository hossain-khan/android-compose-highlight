package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HighlightThemeDescriptorTest {
    @Test
    fun `bundled contains all eight built-in themes`() {
        val descriptors = HighlightTheme.bundled

        assertThat(descriptors).hasSize(8)
        assertThat(descriptors.map { it.id })
            .containsExactly(
                "tomorrow",
                "tomorrow-night",
                "atom-one-light",
                "atom-one-dark",
                "github",
                "github-dark",
                "alucard",
                "dracula",
            ).inOrder()
    }

    @Test
    fun `bundledLight contains only light themes`() {
        val lightThemes = HighlightTheme.bundledLight

        assertThat(lightThemes).hasSize(4)
        assertThat(lightThemes.all { it.isLight && !it.isDark }).isTrue()
        assertThat(lightThemes.map { it.id })
            .containsExactly(
                "tomorrow",
                "atom-one-light",
                "github",
                "alucard",
            ).inOrder()
    }

    @Test
    fun `bundledDark contains only dark themes`() {
        val darkThemes = HighlightTheme.bundledDark

        assertThat(darkThemes).hasSize(4)
        assertThat(darkThemes.all { it.isDark && !it.isLight }).isTrue()
        assertThat(darkThemes.map { it.id })
            .containsExactly(
                "tomorrow-night",
                "atom-one-dark",
                "github-dark",
                "dracula",
            ).inOrder()
    }

    @Test
    fun `findBundledById resolves exact canonical IDs`() {
        for (descriptor in HighlightTheme.bundled) {
            val found = HighlightTheme.findBundledById(descriptor.id)
            assertThat(found).isNotNull()
            assertThat(found).isEqualTo(descriptor)
        }
    }

    @Test
    fun `findBundledById resolves legacy aliases`() {
        val githubLight = HighlightTheme.findBundledById("github-light")
        assertThat(githubLight).isNotNull()
        assertThat(githubLight!!.id).isEqualTo("github")

        val draculaDark = HighlightTheme.findBundledById("dracula-dark")
        assertThat(draculaDark).isNotNull()
        assertThat(draculaDark!!.id).isEqualTo("dracula")

        val alucardLight = HighlightTheme.findBundledById("alucard-light")
        assertThat(alucardLight).isNotNull()
        assertThat(alucardLight!!.id).isEqualTo("alucard")
    }

    @Test
    fun `findBundledById returns null for unrecognized IDs`() {
        assertThat(HighlightTheme.findBundledById("non-existent")).isNull()
        assertThat(HighlightTheme.findBundledById("")).isNull()
        assertThat(HighlightTheme.findBundledById("monokai")).isNull()
    }

    @Test
    fun `descriptor theme creates and caches HighlightTheme instance`() {
        val descriptor = HighlightTheme.findBundledById("tomorrow")!!

        val theme1 = descriptor.theme
        val theme2 = descriptor.create()

        assertThat(theme1).isNotNull()
        assertThat(theme1.name).isEqualTo("tomorrow")
        assertThat(theme1.colorMap).isNotEmpty()
        // Referential identity check - cached instance
        assertThat(theme1).isSameInstanceAs(theme2)
    }

    @Test
    fun `descriptors with same id are equal regardless of closure instance`() {
        val desc1 = HighlightThemeDescriptor("test-id", "Test 1", isDark = false) { HighlightTheme.tomorrow() }
        val desc2 = HighlightThemeDescriptor("test-id", "Test 2", isDark = true) { HighlightTheme.draculaDark() }
        val desc3 = HighlightThemeDescriptor("other-id", "Test 1", isDark = false) { HighlightTheme.tomorrow() }

        assertThat(desc1).isEqualTo(desc2)
        assertThat(desc1.hashCode()).isEqualTo(desc2.hashCode())
        assertThat(desc1).isNotEqualTo(desc3)
    }

    @Test
    fun `toString returns formatted string`() {
        val descriptor = HighlightTheme.findBundledById("dracula")!!
        assertThat(descriptor.toString()).isEqualTo(
            "HighlightThemeDescriptor(id=dracula, displayName=Dracula, isDark=true)",
        )
    }
}
