package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for the package-level [escapeForJs] function.
 *
 * Covers standard escapes, the U+2028/U+2029 line-terminator fix (required for
 * pre-ES2019 WebView compatibility on Android < 10), and edge cases.
 */
class HighlightEngineEscapeTest {
    @Test
    fun `plain string without special chars is returned unchanged`() {
        assertThat(escapeForJs("hello world")).isEqualTo("hello world")
    }

    @Test
    fun `empty string is returned unchanged`() {
        assertThat(escapeForJs("")).isEqualTo("")
    }

    @Test
    fun `backslash is doubled`() {
        assertThat(escapeForJs("a\\b")).isEqualTo("a\\\\b")
    }

    @Test
    fun `single quote is escaped`() {
        assertThat(escapeForJs("it's")).isEqualTo("it\\'s")
    }

    @Test
    fun `newline is escaped`() {
        assertThat(escapeForJs("line1\nline2")).isEqualTo("line1\\nline2")
    }

    @Test
    fun `carriage return is escaped`() {
        assertThat(escapeForJs("a\rb")).isEqualTo("a\\rb")
    }

    @Test
    fun `U+2028 line separator is escaped to unicode escape sequence`() {
        // U+2028 is a JS line terminator in pre-ES2019 engines (pre-Android 10 WebView).
        // Without escaping, it causes an unterminated string literal / SyntaxError.
        assertThat(escapeForJs("hello\u2028world")).isEqualTo("hello\\u2028world")
    }

    @Test
    fun `U+2029 paragraph separator is escaped to unicode escape sequence`() {
        // U+2029 is a JS line terminator in pre-ES2019 engines (pre-Android 10 WebView).
        assertThat(escapeForJs("hello\u2029world")).isEqualTo("hello\\u2029world")
    }

    @Test
    fun `string with only U+2028 is escaped`() {
        assertThat(escapeForJs("\u2028")).isEqualTo("\\u2028")
    }

    @Test
    fun `string with only U+2029 is escaped`() {
        assertThat(escapeForJs("\u2029")).isEqualTo("\\u2029")
    }

    @Test
    fun `multiple U+2028 and U+2029 in one string are all escaped`() {
        assertThat(escapeForJs("\u2028a\u2029b\u2028")).isEqualTo("\\u2028a\\u2029b\\u2028")
    }

    @Test
    fun `backslash is doubled before U+2028 escape to avoid double-escaping`() {
        // A literal backslash immediately before U+2028 must produce \\ followed by \u2028,
        // not \\\u2028 (which would be a malformed sequence).
        assertThat(escapeForJs("\\\u2028")).isEqualTo("\\\\\u005cu2028")
    }

    @Test
    fun `all special chars combined are escaped correctly`() {
        val input = "a\\\n'\r\u2028\u2029b"
        val expected = "a\\\\\\n\\'\\r\\u2028\\u2029b"
        assertThat(escapeForJs(input)).isEqualTo(expected)
    }
}
