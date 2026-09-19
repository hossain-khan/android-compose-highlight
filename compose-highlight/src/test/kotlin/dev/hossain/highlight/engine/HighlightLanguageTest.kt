package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * JVM unit tests for [HighlightLanguage.fromExtension].
 *
 * Verifies that file extension-to-language mappings return the expected
 * highlight.js language identifiers, handle case-insensitivity, and return
 * null for unknown or malformed inputs.
 */
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
    fun `fromExtension returns gradle for gradle`() {
        assertThat(HighlightLanguage.fromExtension("gradle")).isEqualTo("gradle")
    }

    @Test
    fun `fromExtension returns bash for sh`() {
        assertThat(HighlightLanguage.fromExtension("sh")).isEqualTo("bash")
    }

    @Test
    fun `all returns non-empty sorted list of languages including html`() {
        val all = HighlightLanguage.all
        assertThat(all).isNotEmpty()
        assertThat(all.size).isAtLeast(190)
        assertThat(all).contains("html")
        assertThat(all).contains("xml")
        assertThat(all).contains("kotlin")
        assertThat(all).contains("javascript")
        assertThat(all).contains("python")

        // Verify alphabetical sorting
        assertThat(all).isInOrder()
    }

    @Test
    fun `ALL uppercase alias matches all`() {
        assertThat(HighlightLanguage.ALL).isEqualTo(HighlightLanguage.all)
    }

    @Test
    fun `primary contains curated high-demand languages`() {
        val primary = HighlightLanguage.primary
        assertThat(primary).isNotEmpty()
        assertThat(primary).contains("kotlin")
        assertThat(primary).contains("java")
        assertThat(primary).contains("python")
        assertThat(primary).contains("typescript")
        assertThat(primary).contains("javascript")
        assertThat(primary).contains("rust")
        assertThat(primary).contains("go")
        assertThat(primary).contains("swift")
        assertThat(primary).contains("c")
        assertThat(primary).contains("cpp")
        assertThat(primary).contains("csharp")
        assertThat(primary).contains("sql")
        assertThat(primary).contains("json")
        assertThat(primary).contains("yaml")
        assertThat(primary).contains("html")
        assertThat(primary).contains("css")
        assertThat(primary).contains("markdown")
        assertThat(primary).contains("bash")
        assertThat(primary).contains("xml")
        assertThat(primary).contains("dockerfile")
    }

    @Test
    fun `primary languages are all contained in all`() {
        val allSet = HighlightLanguage.all.toSet()
        for (lang in HighlightLanguage.primary) {
            assertThat(allSet).contains(lang)
        }
    }

    @Test
    fun `PRIMARY uppercase alias matches primary`() {
        assertThat(HighlightLanguage.PRIMARY).isEqualTo(HighlightLanguage.primary)
    }

    @Test
    fun `canonicalName resolves canonical language names`() {
        assertThat(HighlightLanguage.canonicalName("kotlin")).isEqualTo("kotlin")
        assertThat(HighlightLanguage.canonicalName("python")).isEqualTo("python")
        assertThat(HighlightLanguage.canonicalName("html")).isEqualTo("html")
        assertThat(HighlightLanguage.canonicalName("xml")).isEqualTo("xml")
        assertThat(HighlightLanguage.canonicalName("bash")).isEqualTo("bash")
    }

    @Test
    fun `canonicalName is case-insensitive and trims whitespace`() {
        assertThat(HighlightLanguage.canonicalName("  Kotlin  ")).isEqualTo("kotlin")
        assertThat(HighlightLanguage.canonicalName("PYTHON")).isEqualTo("python")
        assertThat(HighlightLanguage.canonicalName("HTML")).isEqualTo("html")
    }

    @Test
    fun `canonicalName resolves Highlight js aliases to canonical grammar names`() {
        assertThat(HighlightLanguage.canonicalName("kt")).isEqualTo("kotlin")
        assertThat(HighlightLanguage.canonicalName("kts")).isEqualTo("kotlin")
        assertThat(HighlightLanguage.canonicalName("py")).isEqualTo("python")
        assertThat(HighlightLanguage.canonicalName("js")).isEqualTo("javascript")
        assertThat(HighlightLanguage.canonicalName("ts")).isEqualTo("typescript")
        assertThat(HighlightLanguage.canonicalName("tsx")).isEqualTo("typescript")
        assertThat(HighlightLanguage.canonicalName("sh")).isEqualTo("bash")
        assertThat(HighlightLanguage.canonicalName("zsh")).isEqualTo("bash")
        assertThat(HighlightLanguage.canonicalName("c#")).isEqualTo("csharp")
        assertThat(HighlightLanguage.canonicalName("c++")).isEqualTo("cpp")
        assertThat(HighlightLanguage.canonicalName("cc")).isEqualTo("cpp")
        assertThat(HighlightLanguage.canonicalName("yml")).isEqualTo("yaml")
        assertThat(HighlightLanguage.canonicalName("docker")).isEqualTo("dockerfile")
        assertThat(HighlightLanguage.canonicalName("golang")).isEqualTo("go")
        assertThat(HighlightLanguage.canonicalName("htm")).isEqualTo("html")
    }

    @Test
    fun `canonicalName resolves file extensions not in aliases`() {
        assertThat(HighlightLanguage.canonicalName("pyw")).isEqualTo("python")
        assertThat(HighlightLanguage.canonicalName("gradle")).isEqualTo("gradle")
    }

    @Test
    fun `canonicalName returns null for unknown or empty input`() {
        assertThat(HighlightLanguage.canonicalName("unknown_language_xyz")).isNull()
        assertThat(HighlightLanguage.canonicalName("")).isNull()
        assertThat(HighlightLanguage.canonicalName("   ")).isNull()
    }

    @Test
    fun `isSupported returns true for canonical languages and aliases`() {
        assertThat(HighlightLanguage.isSupported("kotlin")).isTrue()
        assertThat(HighlightLanguage.isSupported("kt")).isTrue()
        assertThat(HighlightLanguage.isSupported("KOTLIN")).isTrue()
        assertThat(HighlightLanguage.isSupported("html")).isTrue()
        assertThat(HighlightLanguage.isSupported("HTML")).isTrue()
        assertThat(HighlightLanguage.isSupported("py")).isTrue()
        assertThat(HighlightLanguage.isSupported("js")).isTrue()
        assertThat(HighlightLanguage.isSupported("sh")).isTrue()
    }

    @Test
    fun `isSupported returns false for unknown languages`() {
        assertThat(HighlightLanguage.isSupported("unknown_lang")).isFalse()
        assertThat(HighlightLanguage.isSupported("")).isFalse()
        assertThat(HighlightLanguage.isSupported("   ")).isFalse()
    }
}
