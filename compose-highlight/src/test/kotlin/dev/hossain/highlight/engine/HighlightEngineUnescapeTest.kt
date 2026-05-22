package dev.hossain.highlight.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for the package-level [unescapeJsString] function.
 *
 * Covers every escape sequence branch, edge cases, and the key correctness fix:
 * `\\n` (JSON for a literal backslash + 'n') must NOT be decoded as a newline.
 */
class HighlightEngineUnescapeTest {
    @Test
    fun `plain string without escapes is returned unchanged`() {
        assertThat(unescapeJsString("hello world")).isEqualTo("hello world")
    }

    @Test
    fun `surrounding double quotes are stripped`() {
        assertThat(unescapeJsString("\"hello\"")).isEqualTo("hello")
    }

    @Test
    fun `empty quoted string returns empty string`() {
        assertThat(unescapeJsString("\"\"")).isEqualTo("")
    }

    @Test
    fun `empty unquoted string returns empty string`() {
        assertThat(unescapeJsString("")).isEqualTo("")
    }

    @Test
    fun `escaped double quote is unescaped`() {
        assertThat(unescapeJsString("\"say \\\"hi\\\"\"")).isEqualTo("say \"hi\"")
    }

    @Test
    fun `escaped backslash is unescaped`() {
        assertThat(unescapeJsString("\"a\\\\b\"")).isEqualTo("a\\b")
    }

    @Test
    fun `backslash-n is unescaped to newline`() {
        assertThat(unescapeJsString("\"line1\\nline2\"")).isEqualTo("line1\nline2")
    }

    @Test
    fun `backslash-r is unescaped to carriage return`() {
        assertThat(unescapeJsString("\"a\\rb\"")).isEqualTo("a\rb")
    }

    @Test
    fun `backslash-t is unescaped to tab`() {
        assertThat(unescapeJsString("\"a\\tb\"")).isEqualTo("a\tb")
    }

    @Test
    fun `backslash-slash is unescaped to forward slash`() {
        assertThat(unescapeJsString("\"a\\/b\"")).isEqualTo("a/b")
    }

    @Test
    fun `unicode escape u003C decodes to less-than`() {
        assertThat(unescapeJsString("\"\\u003C\"")).isEqualTo("<")
    }

    @Test
    fun `unicode escape u003E decodes to greater-than`() {
        assertThat(unescapeJsString("\"\\u003E\"")).isEqualTo(">")
    }

    @Test
    fun `unicode escape u0026 decodes to ampersand`() {
        assertThat(unescapeJsString("\"\\u0026\"")).isEqualTo("&")
    }

    @Test
    fun `unicode escape lowercase hex digits are decoded`() {
        // \u003c (lowercase) must decode the same as \u003C
        assertThat(unescapeJsString("\"\\u003c\"")).isEqualTo("<")
    }

    @Test
    fun `unicode escape for accented character is decoded`() {
        // \u00e9 -> 'é'
        assertThat(unescapeJsString("\"\\u00e9\"")).isEqualTo("é")
    }

    // ----- Key correctness fix (regression guard) -----

    @Test
    fun `literal backslash followed by n is not converted to newline`() {
        // JSON "\\n" represents a literal backslash + 'n', NOT a newline.
        // The old sequential replace() decoded this incorrectly as a newline.
        assertThat(unescapeJsString("\"\\\\n\"")).isEqualTo("\\n")
    }

    @Test
    fun `literal backslash followed by t is not converted to tab`() {
        assertThat(unescapeJsString("\"\\\\t\"")).isEqualTo("\\t")
    }

    @Test
    fun `literal backslash followed by backslash decodes to two backslashes`() {
        assertThat(unescapeJsString("\"\\\\\\\\\"")).isEqualTo("\\\\")
    }

    // ----- Edge cases -----

    @Test
    fun `unknown escape sequence passes backslash through`() {
        // \q is not a valid JSON escape - the backslash is emitted literally.
        assertThat(unescapeJsString("\"\\q\"")).isEqualTo("\\q")
    }

    @Test
    fun `trailing lone backslash is passed through`() {
        // A backslash at the very last position has no following char - passed through.
        assertThat(unescapeJsString("\"a\\\"")).isEqualTo("a\\")
    }

    @Test
    fun `incomplete unicode escape is passed through`() {
        // \u with fewer than 4 hex digits - backslash passed through, no crash.
        assertThat(unescapeJsString("\"\\u00\"")).isEqualTo("\\u00")
    }

    @Test
    fun `realistic highlight js html snippet is correctly unescaped`() {
        // Simulates output from evaluateJavascript for a highlighted Kotlin snippet.
        val input = "\"<span class=\\\"hljs-keyword\\\">fun</span>\\n\""
        assertThat(unescapeJsString(input))
            .isEqualTo("<span class=\"hljs-keyword\">fun</span>\n")
    }

    @Test
    fun `html chars escaped as unicode sequences are decoded`() {
        // highlight.js encodes <, >, & as \u003C etc. in JS string output.
        val input = "\"\\u003Cdiv\\u003E\\u0026amp;\\u003C/div\\u003E\""
        assertThat(unescapeJsString(input)).isEqualTo("<div>&amp;</div>")
    }

    // ----- Surrogate pair / supplementary Unicode (emoji, characters above U+FFFF) -----

    @Test
    fun `surrogate pair for grinning face emoji decodes to single emoji character`() {
        // U+1F600 (😀) is encoded as \uD83D\uDE00 in UTF-16.
        // evaluateJavascript returns surrogate pairs as two consecutive \uXXXX sequences.
        val input = "\"\\uD83D\\uDE00\""
        assertThat(unescapeJsString(input)).isEqualTo("😀")
    }

    @Test
    fun `surrogate pair for bug emoji decodes to single emoji character`() {
        // U+1F41B (🐛) is encoded as \uD83D\uDC1B.
        val input = "\"\\uD83D\\uDC1B\""
        assertThat(unescapeJsString(input)).isEqualTo("🐛")
    }

    @Test
    fun `emoji surrounded by plain text decodes correctly`() {
        // "hello 😀 world" - surrogate pair embedded in normal ASCII text.
        val input = "\"hello \\uD83D\\uDE00 world\""
        assertThat(unescapeJsString(input)).isEqualTo("hello 😀 world")
    }

    @Test
    fun `lone high surrogate without following low surrogate is passed through as-is`() {
        // \uD83D alone (no \uDC00-\uDFFF following) - should not crash and should emit the char.
        val input = "\"\\uD83D\""
        val result = unescapeJsString(input)
        // No crash; the lone surrogate char is emitted (best-effort).
        assertThat(result).hasLength(1)
        assertThat(result[0].code).isEqualTo(0xD83D)
    }

    @Test
    fun `high surrogate followed by non-surrogate unicode is not combined`() {
        // \uD83D followed by \u0041 ('A') - should emit the lone high surrogate then 'A'.
        val input = "\"\\uD83D\\u0041\""
        val result = unescapeJsString(input)
        assertThat(result).hasLength(2)
        assertThat(result[0].code).isEqualTo(0xD83D)
        assertThat(result[1]).isEqualTo('A')
    }

    @Test
    fun `multiple emoji in sequence decode correctly`() {
        // Two emoji back-to-back: 😀🐛 -> \uD83D\uDE00\uD83D\uDC1B
        val input = "\"\\uD83D\\uDE00\\uD83D\\uDC1B\""
        assertThat(unescapeJsString(input)).isEqualTo("😀🐛")
    }

    @Test
    fun `emoji in code comment context decodes correctly`() {
        // Simulates: // TODO: fix this 🐛
        val input = "\"// TODO: fix this \\uD83D\\uDC1B\""
        assertThat(unescapeJsString(input)).isEqualTo("// TODO: fix this 🐛")
    }
}
