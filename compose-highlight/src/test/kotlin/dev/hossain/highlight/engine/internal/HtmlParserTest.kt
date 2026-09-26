package dev.hossain.highlight.engine.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HljsSelectors
import org.junit.Test

class HtmlParserTest {
    private val colorMap =
        mapOf(
            HljsSelectors.KEYWORD to SpanStyle(color = Color.Red),
            HljsSelectors.STRING to SpanStyle(color = Color.Green),
            HljsSelectors.TITLE_FUNCTION to SpanStyle(color = Color.Blue),
        )

    private val darkColorMap =
        mapOf(
            HljsSelectors.KEYWORD to SpanStyle(color = Color.Magenta),
            HljsSelectors.STRING to SpanStyle(color = Color.Cyan),
            HljsSelectors.TITLE_FUNCTION to SpanStyle(color = Color.Yellow),
        )

    private fun parseSingle(html: String): AnnotatedString =
        buildAnnotatedString {
            parseAndBuild(html, colorMap, this)
        }

    private fun parseBoth(html: String): Pair<AnnotatedString, AnnotatedString> {
        val lightBuilder = AnnotatedString.Builder()
        val darkBuilder = AnnotatedString.Builder()
        parseAndBuildBoth(html, colorMap, darkColorMap, lightBuilder, darkBuilder)
        return lightBuilder.toAnnotatedString() to darkBuilder.toAnnotatedString()
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Single Theme: parseAndBuild
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `parseAndBuild handles comments correctly`() {
        val html = "<!-- comment -->hello<!-- another --> world<!-- unterminated"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("hello world")
    }

    @Test
    fun `parseAndBuild handles unclosed opening tag`() {
        val html = "start <span class=\"hljs-keyword\" end"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("start <span class=\"hljs-keyword\" end")
    }

    @Test
    fun `parseAndBuild handles unclosed closing tag`() {
        val html = "<span class=\"hljs-keyword\">hello</span"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("hello")
    }

    @Test
    fun `parseAndBuild handles orphaned closing tag`() {
        val html = "</span>orphan"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("orphan")
    }

    @Test
    fun `parseAndBuild handles mismatched closing tag`() {
        val html = "<div><span class=\"hljs-keyword\">text</div>after"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("textafter")
    }

    @Test
    fun `parseAndBuild handles self-closing tags with and without whitespace`() {
        val html = "line1<br />line2<br/>line3<img src=\"foo.png\"  />line4"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("line1line2line3line4")
    }

    @Test
    fun `parseAndBuild handles non-span tags and bare span tags`() {
        val html = "<div><span>bare</span><code>code</code></div>"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("barecode")
    }

    @Test
    fun `parseAndBuild handles case insensitivity in tag names`() {
        val html = "<SPAN class=\"hljs-keyword\">kw</SPAN><Span class=\"hljs-string\">str</Span>"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("kwstr")
        assertThat(result.spanStyles).hasSize(2)
        assertThat(result.spanStyles[0].item.color).isEqualTo(Color.Red)
        assertThat(result.spanStyles[1].item.color).isEqualTo(Color.Green)
    }

    @Test
    fun `parseAndBuild handles unquoted attribute values`() {
        val html = "<span class=hljs-keyword>kw</span>"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("kw")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.color).isEqualTo(Color.Red)
    }

    @Test
    fun `parseAndBuild handles multiple attributes with unquoted and quoted non-class attributes`() {
        val html = "<span id=myid class=\"hljs-keyword\" data-x='val'>kw</span>"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("kw")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.color).isEqualTo(Color.Red)
    }

    @Test
    fun `parseAndBuild handles unclosed quotes inside attributes`() {
        val html1 = "<span class=\"hljs-keyword>kw</span>"
        val result1 = parseSingle(html1)
        assertThat(result1.text).isEqualTo("<span class=\"hljs-keyword>kw</span>")
    }

    @Test
    fun `extractClassAttrInPlace handles unclosed quotes on class and non-class attributes`() {
        val classAttr = extractClassAttrInPlace("class=\"hljs-keyword", 0, 19)
        assertThat(classAttr).isEqualTo("hljs-keyword")

        val otherAttr = extractClassAttrInPlace("id=\"unclosed", 0, 12)
        assertThat(otherAttr).isEmpty()
    }

    @Test
    fun `parseAndBuild handles uppercase CLASS attribute`() {
        val html = "<span CLASS=\"hljs-keyword\">kw</span><span cLaSs='hljs-string'>str</span>"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("kwstr")
        assertThat(result.spanStyles).hasSize(2)
    }

    @Test
    fun `parseAndBuild handles span with attributes but no class attribute`() {
        val html = "<span id=\"identifier\" title=\"tooltip\">no class</span>"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("no class")
        assertThat(result.spanStyles).isEmpty()
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Dual Theme: parseAndBuildBoth
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `parseAndBuildBoth handles comments`() {
        val html = "<!-- comment -->hello<!-- unterminated"
        val (light, dark) = parseBoth(html)
        assertThat(light.text).isEqualTo("hello")
        assertThat(dark.text).isEqualTo("hello")
    }

    @Test
    fun `parseAndBuildBoth handles unclosed opening tag`() {
        val html = "start <span class=\"hljs-keyword\" end"
        val (light, dark) = parseBoth(html)
        assertThat(light.text).isEqualTo("start <span class=\"hljs-keyword\" end")
        assertThat(dark.text).isEqualTo("start <span class=\"hljs-keyword\" end")
    }

    @Test
    fun `parseAndBuildBoth handles unclosed closing tag`() {
        val html = "<span class=\"hljs-keyword\">hello</span"
        val (light, dark) = parseBoth(html)
        assertThat(light.text).isEqualTo("hello")
        assertThat(dark.text).isEqualTo("hello")
    }

    @Test
    fun `parseAndBuildBoth handles orphaned closing tag`() {
        val html = "</span>orphan"
        val (light, dark) = parseBoth(html)
        assertThat(light.text).isEqualTo("orphan")
        assertThat(dark.text).isEqualTo("orphan")
    }

    @Test
    fun `parseAndBuildBoth handles mismatched closing tag`() {
        val html = "<div><span class=\"hljs-keyword\">text</div>after"
        val (light, dark) = parseBoth(html)
        assertThat(light.text).isEqualTo("textafter")
        assertThat(dark.text).isEqualTo("textafter")
    }

    @Test
    fun `parseAndBuildBoth handles self-closing tags with and without whitespace`() {
        val html = "a<br />b<br/>c<img src=\"x\"  />d"
        val (light, dark) = parseBoth(html)
        assertThat(light.text).isEqualTo("abcd")
        assertThat(dark.text).isEqualTo("abcd")
    }

    @Test
    fun `parseAndBuildBoth handles non-span and bare tags`() {
        val html = "<p><span>text</span></p>"
        val (light, dark) = parseBoth(html)
        assertThat(light.text).isEqualTo("text")
        assertThat(dark.text).isEqualTo("text")
    }

    @Test
    fun `parseAndBuildBoth produces correct light and dark styling`() {
        val html = "<span class=\"hljs-keyword\">val</span> <span class=\"hljs-string\">\"str\"</span>"
        val (light, dark) = parseBoth(html)
        assertThat(light.text).isEqualTo("val \"str\"")
        assertThat(dark.text).isEqualTo("val \"str\"")

        assertThat(light.spanStyles[0].item.color).isEqualTo(Color.Red)
        assertThat(light.spanStyles[1].item.color).isEqualTo(Color.Green)

        assertThat(dark.spanStyles[0].item.color).isEqualTo(Color.Magenta)
        assertThat(dark.spanStyles[1].item.color).isEqualTo(Color.Cyan)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // decodeEntities
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `decodeEntities decodes standard named entities`() {
        assertThat(decodeEntities("&lt;&gt;&amp;&quot;&apos;&nbsp;")).isEqualTo("<>&\"'\u00A0")
    }

    @Test
    fun `decodeEntities passes through unknown named entities`() {
        assertThat(decodeEntities("&xx;&zzz;&copy;")).isEqualTo("&xx;&zzz;&copy;")
    }

    @Test
    fun `decodeEntities handles numeric decimal and hex entities`() {
        assertThat(decodeEntities("&#65;&#x41;&#x61;&#x3C;")).isEqualTo("AAa<")
    }

    @Test
    fun `decodeEntities handles invalid numeric entities gracefully`() {
        assertThat(decodeEntities("&#xZZ;&#x;&#invalid;&#;&#999999999;&#xD800;")).isEqualTo("&#xZZ;&#x;&#invalid;&#;&#999999999;&#xD800;")
    }

    @Test
    fun `decodeEntities handles unclosed ampersand and trailing ampersand`() {
        assertThat(decodeEntities("a & b &")).isEqualTo("a & b &")
        assertThat(decodeEntities("&toolongentityname;")).isEqualTo("&toolongentityname;")
    }

    @Test
    fun `decodeEntities returns plain text unmodified`() {
        assertThat(decodeEntities("hello world")).isEqualTo("hello world")
    }

    // ────────────────────────────────────────────────────────────────────────────
    // resolveStyle
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `resolveStyle resolves single, compound, empty, and unknown classes`() {
        assertThat(resolveStyle("hljs-keyword", colorMap)?.color).isEqualTo(Color.Red)
        assertThat(resolveStyle("hljs-title function_", colorMap)?.color).isEqualTo(Color.Blue)
        assertThat(resolveStyle("", colorMap)).isNull()
        assertThat(resolveStyle("   ", colorMap)).isNull()
        assertThat(resolveStyle("hljs-unknown", colorMap)).isNull()
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Comprehensive Branch Coverage Tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `parseAndBuild handles trailing open angle bracket at EOF`() {
        val result = parseSingle("text<")
        assertThat(result.text).isEqualTo("text<")
    }

    @Test
    fun `parseAndBuildBoth handles trailing open angle bracket at EOF`() {
        val (light, dark) = parseBoth("text<")
        assertThat(light.text).isEqualTo("text<")
        assertThat(dark.text).isEqualTo("text<")
    }

    @Test
    fun `parseAndBuild handles empty tags and closing tag variants`() {
        val result = parseSingle("<><span>text</  span >")
        assertThat(result.text).isEqualTo("text")
    }

    @Test
    fun `parseAndBuildBoth handles empty tags and closing tag variants`() {
        val (light, dark) = parseBoth("<><span>text</  span >")
        assertThat(light.text).isEqualTo("text")
        assertThat(dark.text).isEqualTo("text")
    }

    @Test
    fun `parseAndBuildBoth handles blank and empty class attributes`() {
        val (light, dark) = parseBoth("<span class=\"\">kw1</span><span class=\"   \">kw2</span>")
        assertThat(light.text).isEqualTo("kw1kw2")
        assertThat(dark.text).isEqualTo("kw1kw2")
        assertThat(light.spanStyles).isEmpty()
        assertThat(dark.spanStyles).isEmpty()
    }

    @Test
    fun `parseAndBuild handles attribute shapes with whitespace and quotes`() {
        val html = "<span   id   =   \"x\"   class   =   'hljs-keyword'   disabled   >test</span>"
        val result = parseSingle(html)
        assertThat(result.text).isEqualTo("test")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.color).isEqualTo(Color.Red)
    }

    @Test
    fun `extractClassAttrInPlace handles boolean attributes trailing whitespace and missing values`() {
        assertThat(extractClassAttrInPlace("disabled", 0, 8)).isEmpty()
        assertThat(extractClassAttrInPlace("disabled=", 0, 9)).isEmpty()
        assertThat(extractClassAttrInPlace("   =val", 0, 7)).isEmpty()
        assertThat(extractClassAttrInPlace("class='unclosed", 0, 15)).isEqualTo("unclosed")
        assertThat(extractClassAttrInPlace("id='unclosed", 0, 12)).isEmpty()
        assertThat(extractClassAttrInPlace("class=val id=2", 0, 14)).isEqualTo("val")
        assertThat(extractClassAttrInPlace("class=val", 0, 9)).isEqualTo("val")
        assertThat(extractClassAttrInPlace("   ", 0, 3)).isEmpty()
        // Test equals sign occurring after end
        assertThat(extractClassAttrInPlace("disabled> <span class=foo>", 0, 8)).isEmpty()
        // Test closing quote occurring after end
        assertThat(extractClassAttrInPlace("class=\"unclosed> <span class=\"closed\">", 0, 15)).isEqualTo("unclosed")
    }

    @Test
    fun `parseAndBuild and parseAndBuildBoth handle all closing tag whitespace variants`() {
        val cases =
            listOf(
                "<span></></span>",
                "<span></   ></span>",
                "<span></span   ></span>",
                "<span></  span  ></span>",
                "<span></span>",
                "<   >",
                "< />",
                "<  />",
                "<br   />",
            )
        for (html in cases) {
            parseSingle(html)
            parseBoth(html)
        }
    }

    @Test
    fun `decodeEntities handles all entity discrimination branches`() {
        // len 2: lt, gt, neither, starts with l but not lt, starts with g but not gt
        assertThat(decodeEntities("&lt;&gt;&lx;&gx;&ax;")).isEqualTo("<>&lx;&gx;&ax;")

        // len 3: amp, starts with am but not p, starts with a but not m, neither
        assertThat(decodeEntities("&amp;&amx;&abc;&xyz;")).isEqualTo("&&amx;&abc;&xyz;")

        // len 4: quot, apos, nbsp, starts with q but not quot, starts with a but not apos, starts with n but not nbsp
        assertThat(decodeEntities("&quot;&apos;&nbsp;&quit;&apex;&next;&zzzz;")).isEqualTo("\"'\u00A0&quit;&apex;&next;&zzzz;")

        // Empty entity
        assertThat(decodeEntities("&;")).isEqualTo("&;")
    }

    @Test
    fun `decodeEntities handles all numeric hex and decimal character references`() {
        // Hex entity with digits, lowercase a-f, uppercase A-F
        assertThat(decodeEntities("&#x30;&#x61;&#x41;&#x66;&#x46;")).isEqualTo("0aAfF")

        // Hex entity with non-hex digit
        assertThat(decodeEntities("&#x1G;")).isEqualTo("&#x1G;")

        // Hex entity overflow > 0x10FFFF
        assertThat(decodeEntities("&#x110000;")).isEqualTo("&#x110000;")

        // Decimal entity with char < '0' or > '9'
        assertThat(decodeEntities("&#/;&#:;")).isEqualTo("&#/;&#:;")

        // Decimal entity overflow > 0x10FFFF
        assertThat(decodeEntities("&#1114112;")).isEqualTo("&#1114112;")

        // Surrogate codepoints
        assertThat(decodeEntities("&#xD800;&#xDFFF;")).isEqualTo("&#xD800;&#xDFFF;")
    }
}
