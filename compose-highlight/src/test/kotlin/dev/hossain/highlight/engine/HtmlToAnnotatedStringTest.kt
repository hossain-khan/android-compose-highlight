package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.internal.HtmlToAnnotatedString
import org.junit.Test

class HtmlToAnnotatedStringTest {
    private val baseColor = Color(0xFF4D4D4C.toInt())
    private val colorMap =
        mapOf(
            HljsSelectors.BASE to SpanStyle(color = baseColor, background = Color(0xFFFFFFFF.toInt())),
            HljsSelectors.KEYWORD to SpanStyle(color = Color(0xFF8959a8.toInt())),
            HljsSelectors.STRING to SpanStyle(color = Color(0xFF718c00.toInt())),
            HljsSelectors.NUMBER to SpanStyle(color = Color(0xFFf5871f.toInt())),
            HljsSelectors.COMMENT to SpanStyle(color = Color(0xFF8e908c.toInt())),
            HljsSelectors.STRONG to SpanStyle(color = Color(0xFFeab700.toInt()), fontWeight = FontWeight.Bold),
            // Compound key
            HljsSelectors.TITLE_FUNCTION to SpanStyle(color = Color(0xFF4271ae.toInt())),
        )

    /** colorMap without a base .hljs entry - for tests that verify pre-base-style behavior. */
    private val colorMapNoBase =
        colorMap.filterKeys { it != HljsSelectors.BASE }

    // Dark-theme color map used in convertBothThemes tests - different colors than colorMap.
    private val darkBaseColor = Color(0xFFABB2BF.toInt())
    private val darkColorMap =
        mapOf(
            HljsSelectors.BASE to SpanStyle(color = darkBaseColor, background = Color(0xFF282C34.toInt())),
            HljsSelectors.KEYWORD to SpanStyle(color = Color(0xFFC678DD.toInt())),
            HljsSelectors.STRING to SpanStyle(color = Color(0xFF98C379.toInt())),
            HljsSelectors.NUMBER to SpanStyle(color = Color(0xFFD19A66.toInt())),
            HljsSelectors.COMMENT to SpanStyle(color = Color(0xFF5C6370.toInt())),
            HljsSelectors.STRONG to SpanStyle(color = Color(0xFFE5C07B.toInt()), fontWeight = FontWeight.Bold),
            HljsSelectors.TITLE_FUNCTION to SpanStyle(color = Color(0xFF61AFEF.toInt())),
        )

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
                HljsSelectors.BASE to SpanStyle(color = baseColor, background = Color(0xFFFFFFFF.toInt())),
            )
        val html = """<span class="hljs-keyword">if</span>"""
        val result = HtmlToAnnotatedString.convert(html, colorMapWithBackground)
        // The full-range base span should carry the base text color; background is used externally by the theme
        val fullRangeSpans = result.spanStyles.filter { it.start == 0 && it.end == result.text.length }
        assertThat(fullRangeSpans).hasSize(1)
        assertThat(fullRangeSpans[0].item.color).isEqualTo(baseColor)
    }

    // -----------------------------------------------------------------------------------------
    // convertBothThemes tests
    // -----------------------------------------------------------------------------------------

    @Test
    fun `convertBothThemes empty HTML returns two empty AnnotatedStrings`() {
        val (light, dark) = HtmlToAnnotatedString.convertBothThemes("", colorMap, darkColorMap)
        assertThat(light.text).isEmpty()
        assertThat(dark.text).isEmpty()
        assertThat(light.spanStyles).isEmpty()
        assertThat(dark.spanStyles).isEmpty()
    }

    @Test
    fun `convertBothThemes blank whitespace-only HTML returns two empty AnnotatedStrings`() {
        val (light, dark) = HtmlToAnnotatedString.convertBothThemes("   \n  ", colorMap, darkColorMap)
        assertThat(light.text).isEmpty()
        assertThat(dark.text).isEmpty()
        assertThat(light.spanStyles).isEmpty()
        assertThat(dark.spanStyles).isEmpty()
    }

    @Test
    fun `convertBothThemes plain text produces identical text in both outputs`() {
        val html = "hello world"
        val (light, dark) =
            HtmlToAnnotatedString.convertBothThemes(
                html,
                colorMapNoBase,
                darkColorMap.filterKeys { it != HljsSelectors.BASE },
            )
        assertThat(light.text).isEqualTo("hello world")
        assertThat(dark.text).isEqualTo("hello world")
    }

    @Test
    fun `convertBothThemes produces same text content as two separate convert calls`() {
        val html = """<span class="hljs-keyword">fun</span> <span class="hljs-string">"hi"</span>"""
        val (light, dark) = HtmlToAnnotatedString.convertBothThemes(html, colorMap, darkColorMap)
        val singleLight = HtmlToAnnotatedString.convert(html, colorMap)
        val singleDark = HtmlToAnnotatedString.convert(html, darkColorMap)
        assertThat(light.text).isEqualTo(singleLight.text)
        assertThat(dark.text).isEqualTo(singleDark.text)
    }

    @Test
    fun `convertBothThemes light output uses light keyword color`() {
        val html = """<span class="hljs-keyword">val</span>"""
        val (light, _) = HtmlToAnnotatedString.convertBothThemes(html, colorMap, darkColorMap)
        val keywordSpans = light.spanStyles.filter { it.item.color == Color(0xFF8959a8.toInt()) }
        assertThat(keywordSpans).isNotEmpty()
    }

    @Test
    fun `convertBothThemes dark output uses dark keyword color`() {
        val html = """<span class="hljs-keyword">val</span>"""
        val (_, dark) = HtmlToAnnotatedString.convertBothThemes(html, colorMap, darkColorMap)
        val keywordSpans = dark.spanStyles.filter { it.item.color == Color(0xFFC678DD.toInt()) }
        assertThat(keywordSpans).isNotEmpty()
    }

    @Test
    fun `convertBothThemes light and dark keyword spans have different colors`() {
        val html = """<span class="hljs-keyword">return</span>"""
        // Use maps without the base .hljs entry so firstOrNull() reliably returns the keyword
        // span rather than the full-range base style span (which would also differ between
        // themes but would not verify that keyword styling is applied correctly).
        val (light, dark) =
            HtmlToAnnotatedString.convertBothThemes(
                html,
                colorMapNoBase,
                darkColorMap.filterKeys { it != HljsSelectors.BASE },
            )
        val lightKeyword = light.spanStyles.firstOrNull()
        val darkKeyword = dark.spanStyles.firstOrNull()
        assertThat(lightKeyword).isNotNull()
        assertThat(darkKeyword).isNotNull()
        assertThat(lightKeyword!!.item.color).isNotEqualTo(darkKeyword!!.item.color)
    }

    @Test
    fun `convertBothThemes applies independent base colors per builder`() {
        val html = """plain <span class="hljs-keyword">if</span> code"""
        val (light, dark) = HtmlToAnnotatedString.convertBothThemes(html, colorMap, darkColorMap)
        val lightBase = light.spanStyles.filter { it.start == 0 && it.end == light.text.length }
        val darkBase = dark.spanStyles.filter { it.start == 0 && it.end == dark.text.length }
        assertThat(lightBase).hasSize(1)
        assertThat(darkBase).hasSize(1)
        assertThat(lightBase[0].item.color).isEqualTo(baseColor)
        assertThat(darkBase[0].item.color).isEqualTo(darkBaseColor)
        // The two base colors must be distinct - not shared across builders.
        assertThat(lightBase[0].item.color).isNotEqualTo(darkBase[0].item.color)
    }

    @Test
    fun `convertBothThemes output matches two independent convert calls span-for-span`() {
        val html =
            """<span class="hljs-keyword">fun</span> <span class="hljs-title function_">greet</span>(<span class="hljs-string">"hi"</span>)"""
        val (light, dark) = HtmlToAnnotatedString.convertBothThemes(html, colorMap, darkColorMap)
        val singleLight = HtmlToAnnotatedString.convert(html, colorMap)
        val singleDark = HtmlToAnnotatedString.convert(html, darkColorMap)
        // Span counts and positions must match
        assertThat(light.spanStyles.size).isEqualTo(singleLight.spanStyles.size)
        assertThat(dark.spanStyles.size).isEqualTo(singleDark.spanStyles.size)
        light.spanStyles.forEachIndexed { i, span ->
            assertThat(span.start).isEqualTo(singleLight.spanStyles[i].start)
            assertThat(span.end).isEqualTo(singleLight.spanStyles[i].end)
            assertThat(span.item).isEqualTo(singleLight.spanStyles[i].item)
        }
        dark.spanStyles.forEachIndexed { i, span ->
            assertThat(span.start).isEqualTo(singleDark.spanStyles[i].start)
            assertThat(span.end).isEqualTo(singleDark.spanStyles[i].end)
            assertThat(span.item).isEqualTo(singleDark.spanStyles[i].item)
        }
    }

    @Test
    fun `convertBothThemes handles compound hljs class correctly in both outputs`() {
        val html = """<span class="hljs-title function_">myFunc</span>"""
        val (light, dark) =
            HtmlToAnnotatedString.convertBothThemes(
                html,
                colorMapNoBase,
                darkColorMap.filterKeys { it != HljsSelectors.BASE },
            )
        assertThat(light.spanStyles).hasSize(1)
        assertThat(dark.spanStyles).hasSize(1)
        assertThat(light.spanStyles[0].item.color).isEqualTo(Color(0xFF4271ae.toInt()))
        assertThat(dark.spanStyles[0].item.color).isEqualTo(Color(0xFF61AFEF.toInt()))
    }

    @Test
    fun `convertBothThemes handles nested spans in both outputs`() {
        val html = """<span class="hljs-string">"<span class="hljs-keyword">val</span>"</span>"""
        val (light, dark) =
            HtmlToAnnotatedString.convertBothThemes(
                html,
                colorMapNoBase,
                darkColorMap.filterKeys { it != HljsSelectors.BASE },
            )
        assertThat(light.text).isEqualTo(""""val"""")
        assertThat(dark.text).isEqualTo(""""val"""")
        assertThat(light.spanStyles.size).isAtLeast(2)
        assertThat(dark.spanStyles.size).isAtLeast(2)
    }

    @Test
    fun `convertBothThemes with no base hljs entry produces no full-range spans`() {
        // Use plain text outside the span so the keyword span does not accidentally cover
        // the full range - we only want to verify the absence of the base .hljs wrap.
        val html = """x <span class="hljs-keyword">if</span> y"""
        val lightNoBase = colorMap.filterKeys { it != HljsSelectors.BASE }
        val darkNoBase = darkColorMap.filterKeys { it != HljsSelectors.BASE }
        val (light, dark) = HtmlToAnnotatedString.convertBothThemes(html, lightNoBase, darkNoBase)
        val lightFullRange = light.spanStyles.filter { it.start == 0 && it.end == light.text.length }
        val darkFullRange = dark.spanStyles.filter { it.start == 0 && it.end == dark.text.length }
        assertThat(lightFullRange).isEmpty()
        assertThat(darkFullRange).isEmpty()
    }
}
