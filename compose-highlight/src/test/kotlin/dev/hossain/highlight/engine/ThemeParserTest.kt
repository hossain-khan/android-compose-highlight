package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemeParserTest {
    // CSS sampled from tomorrow.css (Base16 Tomorrow light theme)
    private val tomorrowCssSample =
        """
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
        assertThat(result).isNotEmpty()
    }

    @Test
    fun `parse extracts color for hljs-comment`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result["hljs-comment"]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(0xFF8e908c.toInt()))
    }

    @Test
    fun `parse extracts color for hljs-keyword from compound selector`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result["hljs-keyword"]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(0xFF8959a8.toInt()))
    }

    @Test
    fun `parse extracts color for hljs-string`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result["hljs-string"]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(0xFF718c00.toInt()))
    }

    @Test
    fun `parse extracts hljs background color`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result["hljs"]
        assertThat(style).isNotNull()
        assertThat(style!!.background).isEqualTo(Color(0xFFffffff.toInt()))
    }

    @Test
    fun `parse extracts font-weight bold`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result["hljs-strong"]
        assertThat(style).isNotNull()
        assertThat(style!!.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(style.color).isEqualTo(Color(0xFFeab700.toInt()))
    }

    @Test
    fun `parse extracts font-style italic`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result["hljs-emphasis"]
        assertThat(style).isNotNull()
        assertThat(style!!.fontStyle).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun `parse handles compound selector hljs-title-function_`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        // ".hljs-title.function_" should produce "hljs-title.function_" key
        val style = result["hljs-title.function_"]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(0xFF4271ae.toInt()))
    }

    @Test
    fun `parse returns empty map for empty CSS`() {
        val result = ThemeParser.parse("")
        assertThat(result).isEmpty()
    }

    @Test
    fun `parse returns empty map for blank CSS`() {
        val result = ThemeParser.parse("   \n  \t  ")
        assertThat(result).isEmpty()
    }

    @Test
    fun `parse returns empty map for CSS with no hljs selectors`() {
        val css = "body { color: red; } .foo { background: blue; }"
        val result = ThemeParser.parse(css)
        assertThat(result).isEmpty()
    }

    @Test
    fun `parse ignores selectors with no actionable properties`() {
        val css = ".hljs-operator { opacity: 0.7 }"
        val result = ThemeParser.parse(css)
        // opacity is not a supported property, so no entry should be created
        assertThat(result["hljs-operator"]).isNull()
    }

    @Test
    fun `parse handles 3-digit hex colors`() {
        val css = ".hljs-comment { color: #abc }"
        val result = ThemeParser.parse(css)
        val style = result["hljs-comment"]
        assertThat(style).isNotNull()
        // #abc expands to #aabbcc
        assertThat(style!!.color).isEqualTo(Color(0xFFaabbcc.toInt()))
    }

    @Test
    fun `parse handles minified CSS without whitespace`() {
        val minified = ".hljs{color:#4d4d4c;background:#fff}.hljs-keyword{color:#8959a8}"
        val result = ThemeParser.parse(minified)
        assertThat(result["hljs"]).isNotNull()
        assertThat(result["hljs-keyword"]).isNotNull()
    }

    @Test
    fun `parse handles rgb color format`() {
        val css = ".hljs-comment { color: rgb(142, 144, 140) }"
        val result = ThemeParser.parse(css)
        val style = result["hljs-comment"]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(142, 144, 140))
    }

    @Test
    fun `parse handles rgba with fractional alpha`() {
        val css = ".hljs-keyword { color: rgba(255, 0, 128, 0.75); }"
        val result = ThemeParser.parse(css)
        val style = result["hljs-keyword"]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(255, 0, 128, 191)) // 0.75 * 255 ≈ 191
    }

    @Test
    fun `parse handles rgba with integer alpha`() {
        val css = ".hljs-string { color: rgba(0, 128, 255, 200); }"
        val result = ThemeParser.parse(css)
        val style = result["hljs-string"]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(0, 128, 255, 200))
    }

    @Test
    fun `parse handles rgba with leading-dot fractional alpha`() {
        val css = ".hljs-comment { color: rgba(255, 0, 0, .75); }"
        val result = ThemeParser.parse(css)
        val style = result["hljs-comment"]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(255, 0, 0, 191)) // 0.75 * 255 ≈ 191
    }

    @Test
    fun `parse handles background-color property`() {
        val css = ".hljs { background-color: #1e1e1e }"
        val result = ThemeParser.parse(css)
        val style = result["hljs"]
        assertThat(style).isNotNull()
        assertThat(style!!.background).isEqualTo(Color(0xFF1e1e1e.toInt()))
    }

    @Test
    fun `parse treats font-weight 700 as bold`() {
        val css = ".hljs-strong { font-weight: 700; color: #eab700 }"
        val result = ThemeParser.parse(css)
        val style = result["hljs-strong"]
        assertThat(style).isNotNull()
        assertThat(style!!.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `parse handles 8-digit hex color`() {
        // 8-digit hex: first two digits are alpha (AARRGGBB), rest are RGB
        val css = ".hljs-comment { color: #ff8e908c }"
        val result = ThemeParser.parse(css)
        val style = result["hljs-comment"]
        assertThat(style).isNotNull()
    }

    @Test
    fun `parse ignores descendant selector with two hljs tokens`() {
        // ".hljs-meta .hljs-keyword" is a descendant selector and should be skipped
        val css = ".hljs-meta .hljs-keyword { color: #8959a8 }"
        val result = ThemeParser.parse(css)
        // The descendant selector should not create an entry for hljs-keyword
        assertThat(result["hljs-keyword"]).isNull()
    }

    @Test
    fun `parse does not overwrite hljs background with pseudo-element selection color`() {
        // Regression: .hljs::selection has a different background (selection highlight) that
        // must NOT overwrite the real .hljs background derived from the base rule.
        val css =
            ".hljs { color: #7ea2b4; background: #161b1d } " +
                ".hljs ::selection, .hljs::selection { background-color: #516d7b; color: #7ea2b4 }"
        val result = ThemeParser.parse(css)
        // The real background color must be preserved
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFF161b1d))
    }

    @Test
    fun `parse skips pseudo-class selectors`() {
        // :focus, :hover etc. should not pollute the color map
        val css = ".hljs { background: #1e1e1e } .hljs:hover { background: #ff0000 }"
        val result = ThemeParser.parse(css)
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFF1e1e1e))
    }

    // ── Named color tests ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse handles CSS named color red`() {
        val css = ".hljs-keyword { color: red }"
        val result = ThemeParser.parse(css)
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFFFF0000))
    }

    @Test
    fun `parse handles CSS named color green`() {
        val css = ".hljs-comment { color: green }"
        val result = ThemeParser.parse(css)
        // CSS named 'green' = #008000 (not #00ff00)
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xFF008000))
    }

    @Test
    fun `parse handles CSS named color white as background`() {
        val css = ".hljs { color: blue; background: white }"
        val result = ThemeParser.parse(css)
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFFFFFFFF))
    }

    @Test
    fun `parse handles CSS named colors grey and silver`() {
        val css = ".hljs-comment { color: grey } .hljs-tag { color: silver }"
        val result = ThemeParser.parse(css)
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xFF808080))
        assertThat(result["hljs-tag"]?.color).isEqualTo(Color(0xFFC0C0C0))
    }

    @Test
    fun `parse handles CSS named colors navy olive teal maroon`() {
        val css = ".a { color: navy } .b { color: olive } .c { color: teal } .d { color: maroon }"
        val result = ThemeParser.parse(css)
        // None are hljs selectors so result should be empty - just verify parsing doesn't crash
        assertThat(result).isEmpty()
        // Verify via valid hljs selectors
        val css2 = ".hljs-keyword { color: navy } .hljs-string { color: olive }"
        val result2 = ThemeParser.parse(css2)
        assertThat(result2["hljs-keyword"]?.color).isEqualTo(Color(0xFF000080))
        assertThat(result2["hljs-string"]?.color).isEqualTo(Color(0xFF808000))
    }

    @Test
    fun `parse handles CSS named color gold and orange`() {
        val css = ".hljs-number { color: gold } .hljs-literal { color: orange }"
        val result = ThemeParser.parse(css)
        assertThat(result["hljs-number"]?.color).isEqualTo(Color(0xFFFFD700))
        assertThat(result["hljs-literal"]?.color).isEqualTo(Color(0xFFFFA500))
    }

    @Test
    fun `parse is case-insensitive for named colors`() {
        val css = ".hljs-keyword { color: RED } .hljs-string { color: Green }"
        val result = ThemeParser.parse(css)
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFFFF0000))
        assertThat(result["hljs-string"]?.color).isEqualTo(Color(0xFF008000))
    }

    @Test
    fun `parse ignores unknown named colors`() {
        val css = ".hljs-keyword { color: inherit } .hljs-string { color: var(--my-color) }"
        val result = ThemeParser.parse(css)
        // inherit and CSS variables are not parseable - entries should have no color set
        // (they may still appear if font-weight/style was set, but color should be Unspecified)
        result["hljs-keyword"]?.let { assertThat(it.color).isEqualTo(Color.Unspecified) }
        result["hljs-string"]?.let { assertThat(it.color).isEqualTo(Color.Unspecified) }
    }

    // ── 4-digit hex color tests ────────────────────────────────────────────────────────────────────

    @Test
    fun `parse handles 4-digit hex color #rgba`() {
        // #444a = R:0x44=68, G:0x44=68, B:0x44=68, A:0xaa=170
        val css = ".hljs-tag { color: #444a }"
        val result = ThemeParser.parse(css)
        val style = result["hljs-tag"]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(68, 68, 68, 170))
    }

    @Test
    fun `parse handles 4-digit hex fully opaque #rrggbbff`() {
        // #ff0f = R:0xff=255, G:0x00=0 (wait: #f, #f, #0, #f)
        // #f0af = R:0xff=255, G:0x00=0, B:0xaa=170, A:0xff=255
        val css = ".hljs-keyword { color: #f00f }"
        val result = ThemeParser.parse(css)
        val style = result["hljs-keyword"]
        assertThat(style).isNotNull()
        // #f00f = R:0xff, G:0x00, B:0x00, A:0xff
        assertThat(style!!.color).isEqualTo(Color(255, 0, 0, 255))
    }

    // ── 1c-light theme regression ──────────────────────────────────────────────────────────────────

    @Test
    fun `parse 1c-light theme keyword is red not default blue`() {
        // 1c-light has: .hljs { color:#00f } and .hljs-keyword { color: red }
        // Before the named-color fix, hljs-keyword was missing from the map
        // and would fall back to the default blue text color.
        val css =
            "pre code.hljs{display:block}" +
                ".hljs{color:#00f;background:#fff}" +
                ".hljs-comment{color:green}" +
                ".hljs-keyword,.hljs-name,.hljs-function{color:red}" +
                ".hljs-string,.hljs-number{color:#000}"
        val result = ThemeParser.parse(css)
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFFFFFFFF))
        assertThat(result["hljs"]?.color).isEqualTo(Color(0xFF0000FF))
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFFFF0000))
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xFF008000))
        assertThat(result["hljs-string"]?.color).isEqualTo(Color(0xFF000000))
    }

    // ── a11y-light theme regression ────────────────────────────────────────────────────────────────

    @Test
    fun `parse a11y-light @media block does not overwrite keyword color`() {
        // a11y-light has a @media (-ms-high-contrast) block that contains:
        //   .hljs-keyword { font-weight: 700 }
        // Before the @media-stripping fix, this SpanStyle (font-weight only, no color) would
        // overwrite the real .hljs-keyword { color:#7928a1 } entry, making keywords appear
        // in the default text color instead of purple.
        val css =
            ".hljs{background:#fefefe;color:#545454}" +
                ".hljs-keyword,.hljs-selector-tag{color:#7928a1}" +
                "@media screen and (-ms-high-contrast:active){" +
                ".hljs-keyword,.hljs-selector-tag{font-weight:700}" +
                "}"
        val result = ThemeParser.parse(css)
        // Color must be the purple from the real rule, not lost due to @media override
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFF7928a1))
    }

    @Test
    fun `parse strips @media block entirely leaving main rules intact`() {
        val css =
            ".hljs{background:#fff;color:#000}" +
                ".hljs-string{color:#718c00}" +
                "@media print{.hljs-string{color:black}}" +
                ".hljs-number{color:#f5871f}"
        val result = ThemeParser.parse(css)
        assertThat(result["hljs-string"]?.color).isEqualTo(Color(0xFF718c00))
        assertThat(result["hljs-number"]?.color).isEqualTo(Color(0xFFf5871f))
    }
}
