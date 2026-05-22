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
    fun `backslash followed by U+2028 is escaped correctly`() {
        // Input: one backslash + U+2028.
        // Expected: two backslashes (the backslash doubled) + the 6-char literal \u2028
        // i.e. the string "\\" + "\\u2028" = three backslashes followed by u2028.
        assertThat(escapeForJs("\\\u2028")).isEqualTo("\\\\" + "\\u2028")
    }

    @Test
    fun `tab is escaped`() {
        assertThat(escapeForJs("a\tb")).isEqualTo("a\\tb")
    }

    @Test
    fun `null byte is escaped to unicode escape sequence`() {
        // U+0000 can silently truncate the JS string in some WebView V8 versions.
        assertThat(escapeForJs("a\u0000b")).isEqualTo("a\\u0000b")
    }

    @Test
    fun `ANSI escape U+001B is escaped to unicode escape sequence`() {
        // U+001B is the ANSI escape character common in terminal output (e.g. color codes).
        assertThat(escapeForJs("\u001b[32m")).isEqualTo("\\u001b[32m")
    }

    @Test
    fun `control chars U+0001 through U+0008 are escaped`() {
        // SOH through BS
        assertThat(escapeForJs("\u0001")).isEqualTo("\\u0001")
        assertThat(escapeForJs("\u0008")).isEqualTo("\\u0008")
    }

    @Test
    fun `control chars U+000B and U+000C are escaped`() {
        // VT (vertical tab) and FF (form feed)
        assertThat(escapeForJs("\u000B")).isEqualTo("\\u000b")
        assertThat(escapeForJs("\u000C")).isEqualTo("\\u000c")
    }

    @Test
    fun `control chars U+000E through U+001F are escaped`() {
        // SO through US - spot-check a few
        assertThat(escapeForJs("\u000E")).isEqualTo("\\u000e")
        assertThat(escapeForJs("\u001F")).isEqualTo("\\u001f")
    }

    @Test
    fun `tab newline and carriage return are NOT escaped by control char regex`() {
        // \t (U+0009), \n (U+000A), \r (U+000D) are handled by explicit replacements,
        // not the control char regex, so they produce \t, \n, \r (not \u0009 etc.).
        assertThat(escapeForJs("\t")).isEqualTo("\\t")
        assertThat(escapeForJs("\n")).isEqualTo("\\n")
        assertThat(escapeForJs("\r")).isEqualTo("\\r")
    }

    @Test
    fun `all special chars combined are escaped correctly`() {
        val input = "a\\\n'\r\t\u0000\u001b\u2028\u2029b"
        val expected = "a\\\\\\n\\'\\r\\t\\u0000\\u001b\\u2028\\u2029b"
        assertThat(escapeForJs(input)).isEqualTo(expected)
    }
}
