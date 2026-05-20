package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HighlightLanguageTest {
    @Test
    fun `fromExtension returns kotlin for kt`() {
        assertThat(HighlightLanguage.fromExtension("kt")).isEqualTo("kotlin")
    }

    @Test
    fun `fromExtension returns kotlin for kts`() {
        assertThat(HighlightLanguage.fromExtension("kts")).isEqualTo("kotlin")
    }

    @Test
    fun `fromExtension is case-insensitive for KT`() {
        assertThat(HighlightLanguage.fromExtension("KT")).isEqualTo("kotlin")
    }

    @Test
    fun `fromExtension returns python for py`() {
        assertThat(HighlightLanguage.fromExtension("py")).isEqualTo("python")
    }

    @Test
    fun `fromExtension returns javascript for js`() {
        assertThat(HighlightLanguage.fromExtension("js")).isEqualTo("javascript")
    }

    @Test
    fun `fromExtension returns typescript for ts`() {
        assertThat(HighlightLanguage.fromExtension("ts")).isEqualTo("typescript")
    }

    @Test
    fun `fromExtension returns typescript for tsx`() {
        assertThat(HighlightLanguage.fromExtension("tsx")).isEqualTo("typescript")
    }

    @Test
    fun `fromExtension returns null for unknown extension`() {
        assertThat(HighlightLanguage.fromExtension("unknown")).isNull()
    }

    @Test
    fun `fromExtension returns null for empty string`() {
        assertThat(HighlightLanguage.fromExtension("")).isNull()
    }

    @Test
    fun `fromExtension returns null for dot prefixed extension`() {
        assertThat(HighlightLanguage.fromExtension(".kt")).isNull()
    }

    @Test
    fun `fromExtension returns cpp for cc`() {
        assertThat(HighlightLanguage.fromExtension("cc")).isEqualTo("cpp")
    }

    @Test
    fun `fromExtension returns groovy for gradle`() {
        assertThat(HighlightLanguage.fromExtension("gradle")).isEqualTo("groovy")
    }

    @Test
    fun `fromExtension returns bash for sh`() {
        assertThat(HighlightLanguage.fromExtension("sh")).isEqualTo("bash")
    }
}
