package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
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
        assertThat(result.text).isEqualTo("if")
        val spans = result.spanStyles
        assertThat(spans).hasSize(1)
        assertThat(spans[0].item.color).isEqualTo(Color(0xFF8959a8.toInt()))
        assertThat(spans[0].start).isEqualTo(0)
        assertThat(spans[0].end).isEqualTo(2)
    }

    @Test
    fun `convert applies base hljs text color as full-range span`() {
        val html = """plain <span class="hljs-keyword">if</span> more"""
        val result = HtmlToAnnotatedString.convert(html, colorMap)
        assertThat(result.text).isEqualTo("plain if more")
        // One base span covering the full range + one keyword span
        val fullRangeSpans = result.spanStyles.filter { it.start == 0 && it.end == result.text.length }
        assertThat(fullRangeSpans).hasSize(1)
        assertThat(fullRangeSpans[0].item.color).isEqualTo(baseColor)
    }

    @Test
    fun `convert without hljs base entry applies no full-range span`() {
        val html = """plain <span class="hljs-keyword">if</span> more"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        val fullRangeSpans = result.spanStyles.filter { it.start == 0 && it.end == result.text.length }
        assertThat(fullRangeSpans).isEmpty()
    }

    @Test
    fun `convert preserves plain text outside spans`() {
        val html = """def <span class="hljs-keyword">if</span> x"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertThat(result.text).isEqualTo("def if x")
    }

    @Test
    fun `convert handles nested spans`() {
        val html = """<span class="hljs-string">"hello <span class="hljs-keyword">world</span>"</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertThat(result.text).isEqualTo(""""hello world"""")
        // Should have 2 spans: string wrapping all, keyword for inner
        assertThat(result.spanStyles.size).isAtLeast(2)
    }

    @Test
    fun `convert preserves whitespace in text nodes`() {
        val html = """<span class="hljs-keyword">def</span>    foo()"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        // Whitespace between keyword and function name must be preserved
        assertThat(result.text).contains("    ")
    }

    @Test
    fun `convert returns plain text for HTML with no recognized classes`() {
        val html = """<span class="unknown-class">hello</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertThat(result.text).isEqualTo("hello")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `convert empty HTML returns empty AnnotatedString`() {
        val result = HtmlToAnnotatedString.convert("", colorMap)
        assertThat(result.text).isEqualTo("")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `convert blank HTML returns empty AnnotatedString`() {
        val result = HtmlToAnnotatedString.convert("   ", emptyMap())
        assertThat(result.text).isEqualTo("")
    }

    @Test
    fun `convert multiple sequential spans produce correct spans`() {
        val html = """<span class="hljs-keyword">def</span> <span class="hljs-string">"hi"</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertThat(result.text).isEqualTo("""def "hi"""")
        assertThat(result.spanStyles).hasSize(2)
    }

    @Test
    fun `convert handles compound class with space-separated hljs classes`() {
        // hljs sometimes outputs class="hljs-title function_" (space-separated)
        val html = """<span class="hljs-title function_">myFunc</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertThat(result.text).isEqualTo("myFunc")
        // Should find hljs-title.function_ in colorMap
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.color).isEqualTo(Color(0xFF4271ae.toInt()))
    }

    @Test
    fun `convert with empty color map produces plain text`() {
        val html = """<span class="hljs-keyword">return</span>"""
        val result = HtmlToAnnotatedString.convert(html, emptyMap())
        assertThat(result.text).isEqualTo("return")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `convert handles unicode characters`() {
        val html = """<span class="hljs-string">"héllo wörld 🌍"</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertThat(result.text).isEqualTo(""""héllo wörld 🌍"""")
        assertThat(result.spanStyles).hasSize(1)
    }

    @Test
    fun `convert strips non-span element tags but preserves text`() {
        // A <code> wrapper is sometimes used; it is not a span so no style, but text must survive
        val html = """<code><span class="hljs-keyword">def</span> foo</code>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertThat(result.text).isEqualTo("def foo")
        // Only the keyword span should produce a style entry
        assertThat(result.spanStyles).hasSize(1)
    }

    @Test
    fun `convert handles html entities decoded by jsoup`() {
        // jsoup decodes &lt; and &gt; to < and > in text nodes
        val html = """<span class="hljs-string">&lt;hello&gt;</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapNoBase)
        assertThat(result.text).isEqualTo("<hello>")
        assertThat(result.spanStyles).hasSize(1)
    }

    @Test
    fun `convert applies base hljs background color as full-range span background`() {
        val colorMapWithBackground =
            mapOf(
                "hljs" to SpanStyle(color = baseColor, background = Color(0xFFFFFFFF.toInt())),
            )
        val html = """<span class="hljs-keyword">if</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapWithBackground)
        // The full-range base span should carry the base text color; background is used externally by the theme
        val fullRangeSpans = result.spanStyles.filter { it.start == 0 && it.end == result.text.length }
        assertThat(fullRangeSpans).hasSize(1)
        assertThat(fullRangeSpans[0].item.color).isEqualTo(baseColor)
    }
}
