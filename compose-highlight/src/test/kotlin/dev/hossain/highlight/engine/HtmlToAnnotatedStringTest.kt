package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.internal.HtmlToAnnotatedString
import dev.hossain.highlight.screenshot.TestSnippets
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
            // Compound and title keys
            HljsSelectors.TITLE_FUNCTION to SpanStyle(color = Color(0xFF4271ae.toInt())),
            HljsSelectors.TITLE_CLASS to SpanStyle(color = Color(0xFF4271ae.toInt())),
            HljsSelectors.TITLE to SpanStyle(color = Color(0xFF4271ae.toInt())),
            HljsSelectors.OPERATOR to SpanStyle(color = Color(0xFF3e999f.toInt())),
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
            HljsSelectors.TITLE_CLASS to SpanStyle(color = Color(0xFF61AFEF.toInt())),
            HljsSelectors.TITLE to SpanStyle(color = Color(0xFF61AFEF.toInt())),
            HljsSelectors.OPERATOR to SpanStyle(color = Color(0xFF56B6C2.toInt())),
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

    @Test
    fun `convert real-kotlin parses and styles correctly`() {
        val snippet = TestSnippets.load("real-kotlin")
        val result = HtmlToAnnotatedString.convert(snippet.highlightedHtml, colorMap)
        assertThat(result.text).isEqualTo(snippet.code)
        assertThat(result.spanStyles).isNotEmpty()

        // Assert package statement and package keyword
        val packageKeyword = "package dev.zacsweers.metro.compiler.graph"
        val startPkg = result.text.indexOf(packageKeyword)
        assertThat(startPkg).isNotEqualTo(-1)
        val keywordSpans = result.spanStyles.filter { it.start == startPkg && it.end == startPkg + "package".length }
        assertThat(keywordSpans).isNotEmpty()
        assertThat(keywordSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.KEYWORD]?.color)

        // Assert class title GraphTopology
        val className = "GraphTopology"
        val startClass = result.text.indexOf(className)
        assertThat(startClass).isNotEqualTo(-1)
        val classSpans = result.spanStyles.filter { it.start == startClass && it.end == startClass + className.length }
        assertThat(classSpans).isNotEmpty()
        assertThat(classSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.TITLE_CLASS]?.color)

        // Verify comment styling for KDoc line
        val targetComment = "* @property forward Maps each vertex to its dependencies (outgoing edges)."
        val startComment = result.text.indexOf(targetComment)
        assertThat(startComment).isNotEqualTo(-1)
        val commentSpans = result.spanStyles.filter { it.start <= startComment && it.end >= startComment + targetComment.length }
        assertThat(commentSpans).isNotEmpty()
        val commentColor = colorMap[HljsSelectors.COMMENT]?.color
        assertThat(commentSpans.any { it.item.color == commentColor }).isTrue()

        // Verify convertBothThemes matches
        val (light, dark) = HtmlToAnnotatedString.convertBothThemes(snippet.highlightedHtml, colorMap, darkColorMap)
        assertThat(light.text).isEqualTo(snippet.code)
        assertThat(dark.text).isEqualTo(snippet.code)
    }

    @Test
    fun `convert real-c parses and decodes entities correctly`() {
        val snippet = TestSnippets.load("real-c")
        val result = HtmlToAnnotatedString.convert(snippet.highlightedHtml, colorMap)
        assertThat(result.text).isEqualTo(snippet.code)
        assertThat(result.spanStyles).isNotEmpty()

        // Verify entity decoding and string literal styling:
        // `&quot;lc-messages-dir&quot;` -> `"lc-messages-dir"`
        val targetStr = "\"lc-messages-dir\""
        val startStr = result.text.indexOf(targetStr)
        assertThat(startStr).isNotEqualTo(-1)
        val stringSpans = result.spanStyles.filter { it.start == startStr && it.end == startStr + targetStr.length }
        assertThat(stringSpans).isNotEmpty()
        assertThat(stringSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.STRING]?.color)

        // `&#x27;l&#x27;` -> `'l'`
        val charAnchor = ", 'l', "
        val startAnchor = result.text.indexOf(charAnchor)
        assertThat(startAnchor).isNotEqualTo(-1)
        val startChar = startAnchor + 2 // start of `'l'`
        val charSpans = result.spanStyles.filter { it.start == startChar && it.end == startChar + 3 }
        assertThat(charSpans).isNotEmpty()
        assertThat(charSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.STRING]?.color)

        // Verify convertBothThemes matches
        val (light, dark) = HtmlToAnnotatedString.convertBothThemes(snippet.highlightedHtml, colorMap, darkColorMap)
        assertThat(light.text).isEqualTo(snippet.code)
        assertThat(dark.text).isEqualTo(snippet.code)
    }

    @Test
    fun `convert real-rust parses and styles correctly`() {
        val snippet = TestSnippets.load("real-rust")
        val result = HtmlToAnnotatedString.convert(snippet.highlightedHtml, colorMap)
        assertThat(result.text).isEqualTo(snippet.code)
        assertThat(result.spanStyles).isNotEmpty()

        // Verify entity decoding for:
        // `results.push((path, lineno.parse::<u64>()?, line));`
        val targetRustLine = "results.push((path, lineno.parse::<u64>()?, line));"
        val startRustLine = result.text.indexOf(targetRustLine)
        assertThat(startRustLine).isNotEqualTo(-1)

        // Verify `pub fn escape` keyword and function styling
        val pubFnEscape = "pub fn escape"
        val startPubFn = result.text.indexOf(pubFnEscape)
        assertThat(startPubFn).isNotEqualTo(-1)

        val pubSpans = result.spanStyles.filter { it.start == startPubFn && it.end == startPubFn + "pub".length }
        assertThat(pubSpans).isNotEmpty()
        assertThat(pubSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.KEYWORD]?.color)

        val fnSpans =
            result.spanStyles.filter {
                it.start == startPubFn + 4 && it.end == startPubFn + 4 + "fn".length
            }
        assertThat(fnSpans).isNotEmpty()
        assertThat(fnSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.KEYWORD]?.color)

        val escapeName = "escape"
        val startEscapeName = result.text.indexOf(escapeName, startPubFn)
        assertThat(startEscapeName).isNotEqualTo(-1)
        val escapeSpans =
            result.spanStyles.filter {
                it.start == startEscapeName && it.end == startEscapeName + escapeName.length
            }
        assertThat(escapeSpans).isNotEmpty()
        assertThat(escapeSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.TITLE_FUNCTION]?.color)

        // Verify entity decoding and styling for arrow punctuation: `->`
        val arrow = "->"
        val startArrow = result.text.indexOf(arrow, startPubFn)
        assertThat(startArrow).isNotEqualTo(-1)

        // Verify block comment styling
        val blockComment = "This crate provides routines for searching strings for matches"
        val startBlockComment = result.text.indexOf(blockComment)
        assertThat(startBlockComment).isNotEqualTo(-1)
        val commentSpans =
            result.spanStyles.filter {
                it.start <= startBlockComment && it.end >= startBlockComment + blockComment.length
            }
        assertThat(commentSpans).isNotEmpty()
        val commentColor = colorMap[HljsSelectors.COMMENT]?.color
        assertThat(commentSpans.any { it.item.color == commentColor }).isTrue()

        // Verify convertBothThemes matches
        val (light, dark) = HtmlToAnnotatedString.convertBothThemes(snippet.highlightedHtml, colorMap, darkColorMap)
        assertThat(light.text).isEqualTo(snippet.code)
        assertThat(dark.text).isEqualTo(snippet.code)
    }

    @Test
    fun `convert real-go parses and styles correctly`() {
        val snippet = TestSnippets.load("real-go")
        val result = HtmlToAnnotatedString.convert(snippet.highlightedHtml, colorMap)
        assertThat(result.text).isEqualTo(snippet.code)
        assertThat(result.spanStyles).isNotEmpty()

        // Verify func New32 declaration and styling
        val funcDeclaration = "func New32() hash.Hash32 {"
        val startFunc = result.text.indexOf(funcDeclaration)
        assertThat(startFunc).isNotEqualTo(-1)

        // func keyword
        val keywordSpans = result.spanStyles.filter { it.start == startFunc && it.end == startFunc + "func".length }
        assertThat(keywordSpans).isNotEmpty()
        assertThat(keywordSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.KEYWORD]?.color)

        // New32 function name
        val functionName = "New32"
        val startFuncName = result.text.indexOf(functionName, startFunc)
        assertThat(startFuncName).isNotEqualTo(-1)
        val funcNameSpans = result.spanStyles.filter { it.start == startFuncName && it.end == startFuncName + functionName.length }
        assertThat(funcNameSpans).isNotEmpty()
        assertThat(funcNameSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.TITLE_FUNCTION]?.color)

        // FNV-1 comment line
        val fnvComment = "// New32 returns a new 32-bit FNV-1 [hash.Hash]."
        val startFnvComment = result.text.indexOf(fnvComment)
        assertThat(startFnvComment).isNotEqualTo(-1)
        val commentSpans = result.spanStyles.filter { it.start == startFnvComment && it.end == startFnvComment + fnvComment.length }
        assertThat(commentSpans).isNotEmpty()
        assertThat(commentSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.COMMENT]?.color)

        // Verify convertBothThemes matches
        val (light, dark) = HtmlToAnnotatedString.convertBothThemes(snippet.highlightedHtml, colorMap, darkColorMap)
        assertThat(light.text).isEqualTo(snippet.code)
        assertThat(dark.text).isEqualTo(snippet.code)
    }

    @Test
    fun `convert real-csharp parses and styles correctly`() {
        val snippet = TestSnippets.load("real-csharp")
        val result = HtmlToAnnotatedString.convert(snippet.highlightedHtml, colorMap)
        assertThat(result.text).isEqualTo(snippet.code)
        assertThat(result.spanStyles).isNotEmpty()

        // Verify namespace declaration
        val namespaceLine = "namespace Microsoft.EntityFrameworkCore.Diagnostics;"
        val startNamespace = result.text.indexOf(namespaceLine)
        assertThat(startNamespace).isNotEqualTo(-1)
        val nsSpans = result.spanStyles.filter { it.start == startNamespace && it.end == startNamespace + "namespace".length }
        assertThat(nsSpans).isNotEmpty()
        assertThat(nsSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.KEYWORD]?.color)

        // Verify class declaration and class title styling
        val classDeclaration = "public class EventDefinition : EventDefinitionBase"
        val startClassDecl = result.text.indexOf(classDeclaration)
        assertThat(startClassDecl).isNotEqualTo(-1)

        val classKeyword = "class"
        val startClassKeyword = result.text.indexOf(classKeyword, startClassDecl)
        val classKeywordSpans =
            result.spanStyles.filter {
                it.start == startClassKeyword &&
                    it.end == startClassKeyword + classKeyword.length
            }
        assertThat(classKeywordSpans).isNotEmpty()
        assertThat(classKeywordSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.KEYWORD]?.color)

        val className = "EventDefinition"
        val startClassName = result.text.indexOf(className, startClassDecl)
        val classNameSpans = result.spanStyles.filter { it.start == startClassName && it.end == startClassName + className.length }
        assertThat(classNameSpans).isNotEmpty()
        assertThat(classNameSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.TITLE]?.color)

        // Verify XML documentation tags/comments
        val docComment = "/// <summary>"
        val startDocComment = result.text.indexOf(docComment)
        assertThat(startDocComment).isNotEqualTo(-1)
        val docCommentSpans = result.spanStyles.filter { it.start <= startDocComment && it.end >= startDocComment + docComment.length }
        assertThat(docCommentSpans).isNotEmpty()
        val commentColor = colorMap[HljsSelectors.COMMENT]?.color
        assertThat(docCommentSpans.any { it.item.color == commentColor }).isTrue()

        // Verify convertBothThemes matches
        val (light, dark) = HtmlToAnnotatedString.convertBothThemes(snippet.highlightedHtml, colorMap, darkColorMap)
        assertThat(light.text).isEqualTo(snippet.code)
        assertThat(dark.text).isEqualTo(snippet.code)
    }

    @Test
    fun `convert real-sql parses and styles correctly`() {
        val snippet = TestSnippets.load("real-sql")
        val result = HtmlToAnnotatedString.convert(snippet.highlightedHtml, colorMap)
        assertThat(result.text).isEqualTo(snippet.code)
        assertThat(result.spanStyles).isNotEmpty()

        // Verify operator styling
        val sqlModeSetting = "SET sql_mode = 'NO_AUTO_VALUE_ON_ZERO';"
        val startSqlMode = result.text.indexOf(sqlModeSetting)
        assertThat(startSqlMode).isNotEqualTo(-1)

        val setKeyword = "SET"
        val startSet = result.text.indexOf(setKeyword, startSqlMode)
        val setSpans = result.spanStyles.filter { it.start == startSet && it.end == startSet + setKeyword.length }
        assertThat(setSpans).isNotEmpty()
        assertThat(setSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.KEYWORD]?.color)

        val equalsOperator = "="
        val startEquals = result.text.indexOf(equalsOperator, startSqlMode)
        val equalsSpans = result.spanStyles.filter { it.start == startEquals && it.end == startEquals + equalsOperator.length }
        assertThat(equalsSpans).isNotEmpty()
        assertThat(equalsSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.OPERATOR]?.color)

        // Verify SQL comment string/character representation
        val sqlComment = "-- Adminer 4.2.4 MySQL dump"
        val startSqlComment = result.text.indexOf(sqlComment)
        assertThat(startSqlComment).isNotEqualTo(-1)
        val commentSpans = result.spanStyles.filter { it.start == startSqlComment && it.end == startSqlComment + sqlComment.length }
        assertThat(commentSpans).isNotEmpty()
        assertThat(commentSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.COMMENT]?.color)

        // Verify table comment string styling
        val tableComment = "'The identifier of the category.'"
        val startTableComment = result.text.indexOf(tableComment)
        assertThat(startTableComment).isNotEqualTo(-1)
        val commentStringSpans =
            result.spanStyles.filter {
                it.start == startTableComment &&
                    it.end == startTableComment + tableComment.length
            }
        assertThat(commentStringSpans).isNotEmpty()
        assertThat(commentStringSpans[0].item.color).isEqualTo(colorMap[HljsSelectors.STRING]?.color)

        // Verify convertBothThemes matches
        val (light, dark) = HtmlToAnnotatedString.convertBothThemes(snippet.highlightedHtml, colorMap, darkColorMap)
        assertThat(light.text).isEqualTo(snippet.code)
        assertThat(dark.text).isEqualTo(snippet.code)
    }
}
