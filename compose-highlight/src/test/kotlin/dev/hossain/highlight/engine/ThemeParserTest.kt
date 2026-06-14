package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.internal.ThemeParser
import org.junit.Test

/**
 * JVM unit tests for [ThemeParser.parse] using inline CSS strings (no Android context required).
 *
 * These tests cover the core parsing logic and edge cases:
 * - Basic hex color parsing (`#rrggbb`, 3-digit `#rgb`, 4-digit `#rgba`, 8-digit `#rrggbbaa`)
 * - `rgb()` and `rgba()` color values including decimal alpha (e.g. `rgba(149,165,166,.8)`)
 * - CSS named colors (e.g. `color: red`)
 * - `font-weight: bold` / `700` / `bolder` / `lighter` and `font-style: italic` / `oblique`
 * - Comma-separated selector lists (e.g. `.hljs-keyword, .hljs-type`)
 * - Compound dot-joined selectors (e.g. `.hljs-title.function_`)
 * - Descendant selectors with two `.hljs-*` tokens skipped (e.g. `.hljs-meta .hljs-keyword`)
 * - Descendant selectors with non-hljs HTML elements skipped (e.g. `.hljs mark`, `.hljs a`)
 * - Pseudo-class / pseudo-element selectors skipped (e.g. `.hljs::selection`, `.hljs-link:hover`)
 * - `@media` and other at-rules stripped before parsing
 * - SpanStyle merge: multiple rules targeting the same selector accumulate properties
 *   (e.g. background set in rule 1 is preserved when rule 2 adds color)
 */
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
        val style = result[HljsSelectors.COMMENT]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(0xFF8e908c.toInt()))
    }

    @Test
    fun `parse extracts color for hljs-keyword from compound selector`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result[HljsSelectors.KEYWORD]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(0xFF8959a8.toInt()))
    }

    @Test
    fun `parse extracts color for hljs-string`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result[HljsSelectors.STRING]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(0xFF718c00.toInt()))
    }

    @Test
    fun `parse extracts hljs background color`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result[HljsSelectors.BASE]
        assertThat(style).isNotNull()
        assertThat(style!!.background).isEqualTo(Color(0xFFffffff.toInt()))
    }

    @Test
    fun `parse extracts font-weight bold`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result[HljsSelectors.STRONG]
        assertThat(style).isNotNull()
        assertThat(style!!.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(style.color).isEqualTo(Color(0xFFeab700.toInt()))
    }

    @Test
    fun `parse extracts font-style italic`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        val style = result[HljsSelectors.EMPHASIS]
        assertThat(style).isNotNull()
        assertThat(style!!.fontStyle).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun `parse handles compound selector hljs-title-function_`() {
        val result = ThemeParser.parse(tomorrowCssSample)
        // ".hljs-title.function_" should produce HljsSelectors.TITLE_FUNCTION key
        val style = result[HljsSelectors.TITLE_FUNCTION]
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
        assertThat(result[HljsSelectors.OPERATOR]).isNull()
    }

    @Test
    fun `parse handles 3-digit hex colors`() {
        val css = ".hljs-comment { color: #abc }"
        val result = ThemeParser.parse(css)
        val style = result[HljsSelectors.COMMENT]
        assertThat(style).isNotNull()
        // #abc expands to #aabbcc
        assertThat(style!!.color).isEqualTo(Color(0xFFaabbcc.toInt()))
    }

    @Test
    fun `parse handles minified CSS without whitespace`() {
        val minified = ".hljs{color:#4d4d4c;background:#fff}.hljs-keyword{color:#8959a8}"
        val result = ThemeParser.parse(minified)
        assertThat(result[HljsSelectors.BASE]).isNotNull()
        assertThat(result[HljsSelectors.KEYWORD]).isNotNull()
    }

    @Test
    fun `parse handles rgb color format`() {
        val css = ".hljs-comment { color: rgb(142, 144, 140) }"
        val result = ThemeParser.parse(css)
        val style = result[HljsSelectors.COMMENT]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(142, 144, 140))
    }

    @Test
    fun `parse handles rgba with fractional alpha`() {
        val css = ".hljs-keyword { color: rgba(255, 0, 128, 0.75); }"
        val result = ThemeParser.parse(css)
        val style = result[HljsSelectors.KEYWORD]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(255, 0, 128, 191)) // 0.75 * 255 ≈ 191
    }

    @Test
    fun `parse handles rgba with integer alpha`() {
        val css = ".hljs-string { color: rgba(0, 128, 255, 200); }"
        val result = ThemeParser.parse(css)
        val style = result[HljsSelectors.STRING]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(0, 128, 255, 200))
    }

    @Test
    fun `parse handles rgba with leading-dot fractional alpha`() {
        val css = ".hljs-comment { color: rgba(255, 0, 0, .75); }"
        val result = ThemeParser.parse(css)
        val style = result[HljsSelectors.COMMENT]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(255, 0, 0, 191)) // 0.75 * 255 ≈ 191
    }

    @Test
    fun `parse handles background-color property`() {
        val css = ".hljs { background-color: #1e1e1e }"
        val result = ThemeParser.parse(css)
        val style = result[HljsSelectors.BASE]
        assertThat(style).isNotNull()
        assertThat(style!!.background).isEqualTo(Color(0xFF1e1e1e.toInt()))
    }

    @Test
    fun `parse treats font-weight 700 as bold`() {
        val css = ".hljs-strong { font-weight: 700; color: #eab700 }"
        val result = ThemeParser.parse(css)
        val style = result[HljsSelectors.STRONG]
        assertThat(style).isNotNull()
        assertThat(style!!.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `parse treats font-weight 600 as bold`() {
        val css = ".hljs-strong { font-weight: 600 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.STRONG]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `parse treats font-weight 800 as bold`() {
        val css = ".hljs-strong { font-weight: 800 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.STRONG]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `parse treats font-weight 900 as bold`() {
        val css = ".hljs-strong { font-weight: 900 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.STRONG]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `parse treats font-weight 400 as normal`() {
        val css = ".hljs-comment { font-weight: 400; color: #888 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.COMMENT]?.fontWeight).isEqualTo(FontWeight.Normal)
    }

    @Test
    fun `parse treats font-weight 100 as normal`() {
        val css = ".hljs-comment { font-weight: 100; color: #888 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.COMMENT]?.fontWeight).isEqualTo(FontWeight.Normal)
    }

    @Test
    fun `parse treats font-weight normal keyword as normal`() {
        val css = ".hljs-comment { font-weight: normal; color: #888 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.COMMENT]?.fontWeight).isEqualTo(FontWeight.Normal)
    }

    @Test
    fun `parse treats font-weight bolder keyword as bold`() {
        val css = ".hljs-strong { font-weight: bolder }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.STRONG]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `parse treats font-weight lighter keyword as normal`() {
        val css = ".hljs-comment { font-weight: lighter; color: #888 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.COMMENT]?.fontWeight).isEqualTo(FontWeight.Normal)
    }

    @Test
    fun `parse treats font-style oblique as italic`() {
        val css = ".hljs-emphasis { font-style: oblique }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.EMPHASIS]?.fontStyle).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun `parse handles 8-digit hex color`() {
        // CSS 8-digit hex uses #RRGGBBAA order.
        // #ff000080 = red 255, green 0, blue 0, alpha 128.
        val css = ".hljs-comment { color: #ff000080 }"
        val result = ThemeParser.parse(css)
        val style = result[HljsSelectors.COMMENT]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(255, 0, 0, 128))
    }

    @Test
    fun `parse ignores descendant selector with two hljs tokens`() {
        // ".hljs-meta .hljs-keyword" is a descendant selector and should be skipped
        val css = ".hljs-meta .hljs-keyword { color: #8959a8 }"
        val result = ThemeParser.parse(css)
        // The descendant selector should not create an entry for hljs-keyword
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
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
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFF161b1d))
    }

    @Test
    fun `parse skips pseudo-class selectors`() {
        // :focus, :hover etc. should not pollute the color map
        val css = ".hljs { background: #1e1e1e } .hljs:hover { background: #ff0000 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFF1e1e1e))
    }

    // ----- Named color tests -----

    @Test
    fun `parse handles CSS named color red`() {
        val css = ".hljs-keyword { color: red }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFFFF0000))
    }

    @Test
    fun `parse handles CSS named color green`() {
        val css = ".hljs-comment { color: green }"
        val result = ThemeParser.parse(css)
        // CSS named 'green' = #008000 (not #00ff00)
        assertThat(result[HljsSelectors.COMMENT]?.color).isEqualTo(Color(0xFF008000))
    }

    @Test
    fun `parse handles CSS named color white as background`() {
        val css = ".hljs { color: blue; background: white }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFFFFFFFF))
    }

    @Test
    fun `parse handles CSS named colors grey and silver`() {
        val css = ".hljs-comment { color: grey } .hljs-tag { color: silver }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.COMMENT]?.color).isEqualTo(Color(0xFF808080))
        assertThat(result[HljsSelectors.TAG]?.color).isEqualTo(Color(0xFFC0C0C0))
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
        assertThat(result2[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF000080))
        assertThat(result2[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF808000))
    }

    @Test
    fun `parse handles CSS named color gold and orange`() {
        val css = ".hljs-number { color: gold } .hljs-literal { color: orange }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.NUMBER]?.color).isEqualTo(Color(0xFFFFD700))
        assertThat(result[HljsSelectors.LITERAL]?.color).isEqualTo(Color(0xFFFFA500))
    }

    @Test
    fun `parse is case-insensitive for named colors`() {
        val css = ".hljs-keyword { color: RED } .hljs-string { color: Green }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFFFF0000))
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF008000))
    }

    @Test
    fun `parse ignores unknown named colors`() {
        val css = ".hljs-keyword { color: inherit } .hljs-string { color: var(--my-color) }"
        val result = ThemeParser.parse(css)
        // inherit and CSS variables are not parseable - entries should have no color set
        // (they may still appear if font-weight/style was set, but color should be Unspecified)
        result[HljsSelectors.KEYWORD]?.let { assertThat(it.color).isEqualTo(Color.Unspecified) }
        result[HljsSelectors.STRING]?.let { assertThat(it.color).isEqualTo(Color.Unspecified) }
    }

    // ----- 4-digit hex color tests -----

    @Test
    fun `parse handles 4-digit hex color #rgba`() {
        // #444a = R:0x44=68, G:0x44=68, B:0x44=68, A:0xaa=170
        val css = ".hljs-tag { color: #444a }"
        val result = ThemeParser.parse(css)
        val style = result[HljsSelectors.TAG]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(68, 68, 68, 170))
    }

    @Test
    fun `parse handles 4-digit hex fully opaque #rrggbbff`() {
        // #ff0f = R:0xff=255, G:0x00=0 (wait: #f, #f, #0, #f)
        // #f0af = R:0xff=255, G:0x00=0, B:0xaa=170, A:0xff=255
        val css = ".hljs-keyword { color: #f00f }"
        val result = ThemeParser.parse(css)
        val style = result[HljsSelectors.KEYWORD]
        assertThat(style).isNotNull()
        // #f00f = R:0xff, G:0x00, B:0x00, A:0xff
        assertThat(style!!.color).isEqualTo(Color(255, 0, 0, 255))
    }

    // ----- 1c-light theme regression -----

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
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFFFFFFFF))
        assertThat(result[HljsSelectors.BASE]?.color).isEqualTo(Color(0xFF0000FF))
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFFFF0000))
        assertThat(result[HljsSelectors.COMMENT]?.color).isEqualTo(Color(0xFF008000))
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF000000))
    }

    // ----- a11y-light theme regression -----

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
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF7928a1))
    }

    @Test
    fun `parse strips @media block entirely leaving main rules intact`() {
        val css =
            ".hljs{background:#fff;color:#000}" +
                ".hljs-string{color:#718c00}" +
                "@media print{.hljs-string{color:black}}" +
                ".hljs-number{color:#f5871f}"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF718c00))
        assertThat(result[HljsSelectors.NUMBER]?.color).isEqualTo(Color(0xFFf5871f))
    }

    // ----- descendant non-hljs selector regression -----

    @Test
    fun `parse descendant selector with non-hljs element does not overwrite base entry`() {
        // agate has: .hljs { background:#333 } then later .hljs mark { background:#555 }
        // Before the whitespace-check fix, .hljs mark was treated as a standalone .hljs rule
        // and overwrote the real background with #555555.
        val css =
            ".hljs{background:#333;color:#fff}" +
                ".hljs-keyword{color:#fcc28c}" +
                ".hljs mark{background:#555;color:inherit}"
        val result = ThemeParser.parse(css)
        // Real background must survive - .hljs mark must not overwrite it
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFF333333))
        assertThat(result[HljsSelectors.BASE]?.color).isEqualTo(Color(0xFFffffff))
    }

    @Test
    fun `parse descendant selector with non-hljs anchor does not affect base entry`() {
        // Some themes have .hljs a { color: inherit } for link styling inside code blocks
        val css =
            ".hljs{background:#fff;color:#000}" +
                ".hljs a{color:inherit}"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.BASE]?.color).isEqualTo(Color(0xFF000000))
    }

    @Test
    fun `parse split rules for same selector merges SpanStyle preserving earlier properties`() {
        // e.g. nord theme: .hljs{background:#2e3440} followed by .hljs,.hljs-subst{color:#d8dee9}
        // Without merge, the second rule would overwrite result[HljsSelectors.BASE], losing the background.
        val css =
            ".hljs{background:#2e3440}" +
                ".hljs,.hljs-subst{color:#d8dee9}"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFF2E3440))
        assertThat(result[HljsSelectors.BASE]?.color).isEqualTo(Color(0xFFD8DEE9))
    }

    @Test
    fun `parse later rule overrides earlier rule for same property`() {
        // A later explicit color overrides an earlier color for the same key.
        val css =
            ".hljs-keyword{color:#aabbcc}" +
                ".hljs-keyword{color:#112233}"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF112233))
    }

    @Test
    fun `parse split rules accumulate font-weight and color independently`() {
        val css =
            ".hljs-title{color:#78bb65}" +
                ".hljs-title{font-weight:700}"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TITLE]?.color).isEqualTo(Color(0xFF78BB65))
        assertThat(result[HljsSelectors.TITLE]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `parse handles important relative font weight and oblique together`() {
        val css =
            ".hljs-title{" +
                "color:#78bb65 !important;" +
                "font-weight:bolder !important;" +
                "font-style:oblique !important" +
                "}"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TITLE]?.color).isEqualTo(Color(0xFF78BB65))
        assertThat(result[HljsSelectors.TITLE]?.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result[HljsSelectors.TITLE]?.fontStyle).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun `parse handles case-insensitive important keyword`() {
        val css = ".hljs-title { color: #78bb65 !IMPORTANT; font-weight: bold !Important }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TITLE]?.color).isEqualTo(Color(0xFF78BB65))
        assertThat(result[HljsSelectors.TITLE]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    // ----- CSS4 rgb() space-separated syntax -----

    @Test
    fun `parse handles CSS4 rgb space-separated without alpha`() {
        val css = ".hljs-keyword { color: rgb(255 0 128) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(255, 0, 128))
    }

    @Test
    fun `parse handles CSS4 rgb space-separated with slash alpha float`() {
        val css = ".hljs-keyword { color: rgb(255 0 128 / 0.5) }"
        val result = ThemeParser.parse(css)
        // 0.5 * 255 = 127.5, roundToInt = 128
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(255, 0, 128, 128))
    }

    @Test
    fun `parse handles CSS4 rgba space-separated with slash alpha float`() {
        // CSS4 allows rgba() with space-separated values just like rgb()
        val css = ".hljs-comment { color: rgba(255 0 128 / 0.5) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.COMMENT]?.color).isEqualTo(Color(255, 0, 128, 128))
    }

    @Test
    fun `parse handles CSS4 rgb with percentage channel values`() {
        // 100% = 255, 0% = 0, 50% = 127
        val css = ".hljs-string { color: rgb(100% 0% 50%) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(255, 0, 127))
    }

    @Test
    fun `parse handles CSS4 rgb with percentage channels and percentage alpha`() {
        // 100%=255, 0%=0, 50%=127, alpha 50%=127
        val css = ".hljs-string { color: rgb(100% 0% 50% / 50%) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(255, 0, 127, 127))
    }

    @Test
    fun `parse handles CSS4 rgb black edge case`() {
        val css = ".hljs { color: rgb(0 0 0) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.BASE]?.color).isEqualTo(Color(0, 0, 0))
    }

    @Test
    fun `parse handles CSS4 rgb opaque white with slash alpha 1`() {
        val css = ".hljs { color: rgb(255 255 255 / 1) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.BASE]?.color).isEqualTo(Color(255, 255, 255, 255))
    }

    @Test
    fun `parse handles CSS4 rgb transparent black with slash alpha 0`() {
        val css = ".hljs { color: rgb(0 0 0 / 0) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.BASE]?.color).isEqualTo(Color(0, 0, 0, 0))
    }

    @Test
    fun `parse handles synthetic CSS4 hljs theme snippet with space-separated rgb`() {
        // Synthetic snippet representative of a theme that adopted CSS Color Level 4 syntax
        val css =
            ".hljs{background:rgb(30 30 30);color:rgb(212 212 212)}" +
                ".hljs-keyword{color:rgb(86 156 214)}" +
                ".hljs-string{color:rgb(206 145 120)}" +
                ".hljs-comment{color:rgb(106 153 85 / 0.8)}"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(30, 30, 30))
        assertThat(result[HljsSelectors.BASE]?.color).isEqualTo(Color(212, 212, 212))
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(86, 156, 214))
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(206, 145, 120))
        // 0.8 * 255 = 204
        assertThat(result[HljsSelectors.COMMENT]?.color).isEqualTo(Color(106, 153, 85, 204))
    }
}
