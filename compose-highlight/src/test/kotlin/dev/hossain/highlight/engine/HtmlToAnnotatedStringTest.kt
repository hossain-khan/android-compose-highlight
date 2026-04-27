package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlToAnnotatedStringTest {
    private val baseColor = Color(0xFF4D4D4C.toInt())
    private val colorMap =
        mapOf(
            "hljs" to SpanStyle(color = baseColor, background = Color(0xFFFFFFFF.toInt())),
            "hljs-keyword" to SpanStyle(color = Color(0xFF8959a8.toInt())),
            "hljs-string" to SpanStyle(color = Color(0xFF718c00.toInt())),
            "hljs-number" to SpanStyle(color = Color(0xFFf5871f.toInt())),
            "hljs-comment" to SpanStyle(color = Color(0xFF8e908c.toInt())),
            "hljs-strong" to SpanStyle(color = Color(0xFFeab700.toInt()), fontWeight = FontWeight.Bold),
            // Compound key
            "hljs-title.function_" to SpanStyle(color = Color(0xFF4271ae.toInt())),
        )

    /** colorMap without a base .hljs entry — for tests that verify pre-base-style behavior. */
    private val colorMapNoBase =
        colorMap.filterKeys { it != "hljs" }

    @Test
    fun `convert simple keyword span produces colored span`() {
        val html = """<span class="hljs-keyword">if</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertEquals("if", result.text)
        val spans = result.spanStyles
        assertEquals(1, spans.size)
        assertEquals(Color(0xFF8959a8.toInt()), spans[0].item.color)
        assertEquals(0, spans[0].start)
        assertEquals(2, spans[0].end)
    }

    @Test
    fun `convert applies base hljs text color as full-range span`() {
        val html = """plain <span class="hljs-keyword">if</span> more"""
        val result = HtmlToAnnotatedString.convert(html, colorMap)
        assertEquals("plain if more", result.text)
        // One base span covering the full range + one keyword span
        val fullRangeSpans = result.spanStyles.filter { it.start == 0 && it.end == result.text.length }
        assertEquals("Expected one full-range base span", 1, fullRangeSpans.size)
        assertEquals(baseColor, fullRangeSpans[0].item.color)
    }

    @Test
    fun `convert without hljs base entry applies no full-range span`() {
        val html = """plain <span class="hljs-keyword">if</span> more"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        val fullRangeSpans = result.spanStyles.filter { it.start == 0 && it.end == result.text.length }
        assertEquals("No full-range span expected without base hljs entry", 0, fullRangeSpans.size)
    }

    @Test
    fun `convert preserves plain text outside spans`() {
        val html = """def <span class="hljs-keyword">if</span> x"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertEquals("def if x", result.text)
    }

    @Test
    fun `convert handles nested spans`() {
        val html = """<span class="hljs-string">"hello <span class="hljs-keyword">world</span>"</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertEquals(""""hello world"""", result.text)
        // Should have 2 spans: string wrapping all, keyword for inner
        assertTrue("Expected at least 2 spans", result.spanStyles.size >= 2)
    }

    @Test
    fun `convert preserves whitespace in text nodes`() {
        val html = """<span class="hljs-keyword">def</span>    foo()"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        // Whitespace between keyword and function name must be preserved
        assertTrue("Expected 4 spaces preserved", result.text.contains("    "))
    }

    @Test
    fun `convert returns plain text for HTML with no recognized classes`() {
        val html = """<span class="unknown-class">hello</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertEquals("hello", result.text)
        assertTrue("No spans expected for unknown class", result.spanStyles.isEmpty())
    }

    @Test
    fun `convert empty HTML returns empty AnnotatedString`() {
        val result = HtmlToAnnotatedString.convert("", colorMap)
        assertEquals("", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `convert blank HTML returns empty AnnotatedString`() {
        val result = HtmlToAnnotatedString.convert("   ", emptyMap())
        assertEquals("", result.text)
    }

    @Test
    fun `convert multiple sequential spans produce correct spans`() {
        val html = """<span class="hljs-keyword">def</span> <span class="hljs-string">"hi"</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertEquals("""def "hi"""", result.text)
        assertEquals(2, result.spanStyles.size)
    }

    @Test
    fun `convert handles compound class with space-separated hljs classes`() {
        // hljs sometimes outputs class="hljs-title function_" (space-separated)
        val html = """<span class="hljs-title function_">myFunc</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertEquals("myFunc", result.text)
        // Should find hljs-title.function_ in colorMap
        assertEquals(1, result.spanStyles.size)
        assertEquals(Color(0xFF4271ae.toInt()), result.spanStyles[0].item.color)
    }

    @Test
    fun `convert with empty color map produces plain text`() {
        val html = """<span class="hljs-keyword">return</span>"""
        val result = HtmlToAnnotatedString.convert(html, emptyMap())
        assertEquals("return", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `convert handles unicode characters`() {
        val html = """<span class="hljs-string">"héllo wörld 🌍"</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertEquals(""""héllo wörld 🌍"""", result.text)
        assertEquals(1, result.spanStyles.size)
    }
}
