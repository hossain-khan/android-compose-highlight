package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.internal.ThemeParser
import org.junit.Test

/**
 * Edge-case unit tests for [ThemeParser.parse] that exercise defensive parser branches
 * and color-format paths not covered by [ThemeParserTest].
 *
 * Tests in this file deliberately probe:
 * - Malformed CSS (stray braces, unterminated comments, `{` inside declarations).
 * - At-rule shapes (`@charset "utf-8";`, `@media` with comment in prelude).
 * - Selector splitting with parenthesised functional notation (`:is(...)`).
 * - Combinator selectors (`>`, `+`, `~`) which must not produce entries.
 * - Trailing-comma selector lists and empty rule bodies.
 * - Hex / rgb() error paths (invalid digits, wrong arity, non-finite alpha).
 * - Compound-selector primary-head fallback semantics.
 *
 * The parser is deliberately permissive (no exceptions for malformed input); these tests
 * pin behaviour so silent failures don't regress into crashes or vice versa.
 */
class ThemeParserEdgeCasesTest {
    // ----- Malformed / defensive CSS shapes -----

    @Test
    fun `parse stops at stray top-level closing brace`() {
        val css = "} .hljs-keyword { color: #ff0000 }"
        val result = ThemeParser.parse(css)
        // Stray `}` aborts top-level parsing; no rules should be picked up.
        assertThat(result).isEmpty()
    }

    @Test
    fun `parse recovers from unbalanced opening rule and continues`() {
        // Rule with no closing brace at EOF: parser consumes through end-of-input but should
        // not throw. The dangling rule may or may not be recorded; the important thing is the
        // earlier valid rule is preserved.
        val css = ".hljs-keyword { color: #112233 } .hljs-string { color: #445566"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF112233))
    }

    @Test
    fun `parse handles unterminated comment at EOF without throwing`() {
        val css = ".hljs-keyword { color: #112233 } /* unterminated"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF112233))
    }

    @Test
    fun `parse handles charset at-rule terminated by semicolon`() {
        val css = "@charset \"utf-8\"; .hljs-keyword { color: #112233 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF112233))
    }

    @Test
    fun `parse handles import at-rule with semicolon`() {
        val css = "@import url(\"foo.css\"); .hljs-string { color: #445566 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF445566))
    }

    @Test
    fun `parse handles comment inside at-rule prelude`() {
        // Comment between `@media` and the opening brace of its block.
        val css =
            "@media /* dark only */ (prefers-color-scheme: dark) { .hljs-keyword { color: red } }" +
                ".hljs-string { color: #445566 }"
        val result = ThemeParser.parse(css)
        // Inside the at-rule should be ignored; outer rule wins.
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF445566))
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse handles comment inside selector list`() {
        val css = ".hljs-keyword /* mid-selector */ { color: #112233 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF112233))
    }

    @Test
    fun `parse handles comment inside declarations block`() {
        val css = ".hljs-keyword { /* note */ color: #112233 /* trailing */ }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF112233))
    }

    @Test
    fun `parse handles comment inside at-rule body`() {
        val css =
            "@media print { /* skip me */ .hljs-keyword { color: red } } " +
                ".hljs-string { color: #445566 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF445566))
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse skips empty declaration block without crashing`() {
        val css = ".hljs-keyword {} .hljs-string { color: #445566 }"
        val result = ThemeParser.parse(css)
        // Empty body produces no entry for keyword but the next rule still parses.
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF445566))
    }

    @Test
    fun `parse returns empty map when CSS contains only at-rules`() {
        val css = "@charset \"utf-8\"; @import url(\"a.css\"); @media print { .x { color: red } }"
        val result = ThemeParser.parse(css)
        assertThat(result).isEmpty()
    }

    // ----- Selector splitting and rejection -----

    @Test
    fun `parse skips trailing comma yielding empty selector`() {
        // Trailing comma produces an empty selector segment; applyHljsSelector must reject it.
        val css = ".hljs-keyword, { color: #112233 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF112233))
    }

    @Test
    fun `parse rejects child combinator selector`() {
        val css = ".hljs > .hljs-keyword { color: red } .hljs-string { color: #445566 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF445566))
    }

    @Test
    fun `parse rejects adjacent sibling combinator selector`() {
        val css = ".hljs+.hljs-keyword { color: red } .hljs-string { color: #445566 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF445566))
    }

    @Test
    fun `parse rejects general sibling combinator selector`() {
        val css = ".hljs~.hljs-keyword { color: red } .hljs-string { color: #445566 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF445566))
    }

    @Test
    fun `parse splits selector list with parenthesised commas correctly`() {
        // `:is(.foo, .bar)` would never be authored against hljs classes, but the splitter
        // must keep its commas inside the parentheses so the surrounding list still pairs up.
        // Here the first entry is the rejected `:is(...)` selector and the second is a valid
        // hljs selector that must still receive its color.
        val css = ":is(.a, .b), .hljs-keyword { color: #112233 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF112233))
    }

    @Test
    fun `parse handles unbalanced closing paren in selector without crashing`() {
        // Defensive: stray `)` should not put the splitter into a negative depth.
        val css = ").hljs-keyword { color: #112233 }"
        val result = ThemeParser.parse(css)
        // Unbalanced selector either gets rejected or interpreted leniently; either way no crash.
        assertThat(result).doesNotContainKey(HljsSelectors.STRING)
    }

    // ----- Compound selector primary-head fallback -----

    @Test
    fun `parse compound selector publishes primary head when no explicit primary entry exists`() {
        // .hljs-title.function_ should also seed `hljs-title` so a token with only the primary
        // class still gets some color (CSS cascade fallback approximation).
        val css = ".hljs-title.function_ { color: #112233 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TITLE_FUNCTION]?.color).isEqualTo(Color(0xFF112233))
        assertThat(result[HljsSelectors.TITLE]?.color).isEqualTo(Color(0xFF112233))
    }

    @Test
    fun `parse compound selector does not overwrite explicit primary head`() {
        // Explicit `.hljs-title` rule comes first; the later compound rule must not clobber it.
        val css =
            ".hljs-title { color: #aabbcc }" +
                ".hljs-title.function_ { color: #112233 }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.TITLE]?.color).isEqualTo(Color(0xFFaabbcc))
        assertThat(result[HljsSelectors.TITLE_FUNCTION]?.color).isEqualTo(Color(0xFF112233))
    }

    // ----- Hex color error paths -----

    @Test
    fun `parse returns no color for invalid hex digits`() {
        val css = ".hljs-keyword { color: #GGGGGG }"
        val result = ThemeParser.parse(css)
        // parseHexColor catches NumberFormatException -> returns null -> declarations have no
        // actionable property -> entry is not created.
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for hex with unsupported length`() {
        // 5-digit hex is not a CSS color. parseHexColor returns null.
        val css = ".hljs-keyword { color: #abcde }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for empty hex token`() {
        val css = ".hljs-keyword { color: # }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    // ----- rgb() error paths -----

    @Test
    fun `parse returns no color for rgb with single component`() {
        val css = ".hljs-keyword { color: rgb(255) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for rgb with two components`() {
        val css = ".hljs-keyword { color: rgb(255, 128) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for rgb with five components`() {
        val css = ".hljs-keyword { color: rgb(1, 2, 3, 0.5, extra) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for space-separated rgb with two components`() {
        val css = ".hljs-keyword { color: rgb(255 0) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for space-separated rgb with too few components before slash`() {
        val css = ".hljs-keyword { color: rgb(255 0 / 0.5) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for rgb with non-numeric component`() {
        val css = ".hljs-keyword { color: rgb(abc, 0, 0) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for rgb with non-numeric percentage component`() {
        val css = ".hljs-keyword { color: rgb(abc%, 0%, 0%) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for rgba with non-numeric alpha`() {
        val css = ".hljs-keyword { color: rgba(0, 0, 0, xyz) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for rgba with non-numeric percentage alpha`() {
        val css = ".hljs-keyword { color: rgba(0, 0, 0, abc%) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for rgba with non-finite percentage alpha`() {
        // "Infinity".toFloatOrNull() yields POSITIVE_INFINITY -> parseAlphaComponent must reject.
        val css = ".hljs-keyword { color: rgba(0, 0, 0, Infinity%) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for rgba with non-finite float alpha`() {
        val css = ".hljs-keyword { color: rgba(0, 0, 0, NaN) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse returns no color for rgba with fractional alpha greater than 1 and not integer`() {
        // Float >1.0: hits the integer-fallback branch; "2.5" is not an integer -> null.
        val css = ".hljs-keyword { color: rgba(0, 0, 0, 2.5) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]).isNull()
    }

    @Test
    fun `parse coerces rgb percentage above 100 down to 255`() {
        // parseRgbComponent clamps to 0..255; 200% rounds to 510 then coerces.
        val css = ".hljs-keyword { color: rgb(200%, 0%, 0%) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(255, 0, 0))
    }

    @Test
    fun `parse coerces percentage alpha above 100 down to 255`() {
        val css = ".hljs-keyword { color: rgba(0, 0, 0, 150%) }"
        val result = ThemeParser.parse(css)
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0, 0, 0, 255))
    }
}
