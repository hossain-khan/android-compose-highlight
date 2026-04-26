package dev.composehighlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeParserTest {

    // CSS sampled from tomorrow.css (Base16 Tomorrow light theme)
    private val tomorrowCssSample = """
        .hljs{color:#4d4d4c;background:#ffffff}
        .hljs-comment{color:#8e908c}
        .hljs-keyword,.hljs-type{color:#8959a8}
        .hljs-string,.hljs-addition{color:#718c00}
        .hljs-number{color:#f5871f}
        .hljs-title.function_{color:#4271ae}
        .hljs-strong{font-weight:bold;color:#eab700}
        .hljs-emphasis{font-style:italic;color:#8959a8}
    """.trimIndent()

    @Test
    fun `parse returns non-empty map for valid CSS`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        assertTrue("Expected non-empty map", result.isNotEmpty())
    }

    @Test
    fun `parse extracts color for hljs-comment`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result["hljs-comment"]
        assertNotNull("hljs-comment should be present", style)
        assertEquals(Color(0xFF8e908c.toInt()), style!!.color)
    }

    @Test
    fun `parse extracts color for hljs-keyword from compound selector`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result["hljs-keyword"]
        assertNotNull("hljs-keyword should be present from compound selector", style)
        assertEquals(Color(0xFF8959a8.toInt()), style!!.color)
    }

    @Test
    fun `parse extracts color for hljs-string`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result["hljs-string"]
        assertNotNull("hljs-string should be present", style)
        assertEquals(Color(0xFF718c00.toInt()), style!!.color)
    }

    @Test
    fun `parse extracts hljs background color`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result["hljs"]
        assertNotNull("hljs base rule should be present", style)
        assertEquals(Color(0xFFffffff.toInt()), style!!.background)
    }

    @Test
    fun `parse extracts font-weight bold`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result["hljs-strong"]
        assertNotNull("hljs-strong should be present", style)
        assertEquals(FontWeight.Bold, style!!.fontWeight)
        assertEquals(Color(0xFFeab700.toInt()), style.color)
    }

    @Test
    fun `parse extracts font-style italic`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result["hljs-emphasis"]
        assertNotNull("hljs-emphasis should be present", style)
        assertEquals(FontStyle.Italic, style!!.fontStyle)
    }

    @Test
    fun `parse handles compound selector hljs-title-function_`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        // ".hljs-title.function_" should produce "hljs-title.function_" key
        val style = result["hljs-title.function_"]
        assertNotNull("Compound selector key hljs-title.function_ should be present", style)
        assertEquals(Color(0xFF4271ae.toInt()), style!!.color)
    }

    @Test
    fun `parse returns empty map for empty CSS`() {
        val result = ThemeParser.parse("")
        assertTrue("Empty CSS should produce empty map", result.isEmpty())
    }

    @Test
    fun `parse returns empty map for blank CSS`() {
        val result = ThemeParser.parse("   \n  \t  ")
        assertTrue("Blank CSS should produce empty map", result.isEmpty())
    }

    @Test
    fun `parse returns empty map for CSS with no hljs selectors`() {
        val css = "body { color: red; } .foo { background: blue; }"
        val result = ThemeParser.parse(css)
        assertTrue("CSS without .hljs selectors should produce empty map", result.isEmpty())
    }

    @Test
    fun `parse ignores selectors with no actionable properties`() {
        val css = ".hljs-operator { opacity: 0.7 }"
        val result = ThemeParser.parse(css)
        // opacity is not a supported property, so no entry should be created
        assertNull(result["hljs-operator"])
    }

    @Test
    fun `parse handles 3-digit hex colors`() {
        val css = ".hljs-comment { color: #abc }"
        val result = ThemeParser.parse(css)
        val style = result["hljs-comment"]
        assertNotNull(style)
        // #abc expands to #aabbcc
        assertEquals(Color(0xFFaabbcc.toInt()), style!!.color)
    }

    @Test
    fun `parse handles minified CSS without whitespace`() {
        val minified = ".hljs{color:#4d4d4c;background:#fff}.hljs-keyword{color:#8959a8}"
        val result = ThemeParser.parse(minified)
        assertNotNull(result["hljs"])
        assertNotNull(result["hljs-keyword"])
    }
}
