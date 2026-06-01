package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Robolectric tests for [ThemeParser] against 5 real highlight.js theme CSS files.
 *
 * Each theme is chosen to exercise a specific CSS pattern:
 * - monokai-sublime: popular dark theme, all standard 6-digit hex colors (baseline)
 * - panda-syntax-light: light theme, many comma-separated multi-selector rules
 * - docco: named colors (navy, teal) and 3-digit hex #00f for params
 * - tomorrow-night-blue: dark blue theme, base text uses 3-digit #fff
 * - stackoverflow-dark: comment is 3-digit hex #999; keyword selector list includes
 *   descendant .hljs-meta .hljs-keyword alongside standalone .hljs-keyword
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class ThemeParserMoreThemes1Test {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ── monokai-sublime ────────────────────────────────────────────────────────────────────────────
    // Dark theme, all standard 6-digit hex colors. Good baseline for correct round-trip parsing.

    @Test
    fun `parseAsset monokai-sublime background is dark`() {
        val result = ThemeParser.parseAsset(context, "monokai-sublime.min.css")
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFF23241f))
    }

    @Test
    fun `parseAsset monokai-sublime base text color is off-white`() {
        val result = ThemeParser.parseAsset(context, "monokai-sublime.min.css")
        assertThat(result[HljsSelectors.BASE]?.color).isEqualTo(Color(0xFFf8f8f2))
    }

    @Test
    fun `parseAsset monokai-sublime keyword is pink`() {
        // .hljs-attr,.hljs-keyword,.hljs-name,.hljs-selector-tag { color: #f92672 }
        val result = ThemeParser.parseAsset(context, "monokai-sublime.min.css")
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFFf92672))
    }

    @Test
    fun `parseAsset monokai-sublime comment is muted brown`() {
        // .hljs-comment,.hljs-deletion,.hljs-meta { color: #75715e }
        val result = ThemeParser.parseAsset(context, "monokai-sublime.min.css")
        assertThat(result[HljsSelectors.COMMENT]?.color).isEqualTo(Color(0xFF75715e))
    }

    @Test
    fun `parseAsset monokai-sublime string is yellow`() {
        // .hljs-string (among others) { color: #e6db74 }
        val result = ThemeParser.parseAsset(context, "monokai-sublime.min.css")
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFFe6db74))
    }

    // ── panda-syntax-light ─────────────────────────────────────────────────────────────────────────
    // Light theme. Many comma-separated multi-selector rules; tests that all selectors in a
    // comma list are correctly mapped to the same SpanStyle.

    @Test
    fun `parseAsset panda-syntax-light background is light gray`() {
        val result = ThemeParser.parseAsset(context, "panda-syntax-light.min.css")
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFFe6e6e6))
    }

    @Test
    fun `parseAsset panda-syntax-light keyword is magenta`() {
        // .hljs-deletion,.hljs-keyword { color: #d92792 }
        val result = ThemeParser.parseAsset(context, "panda-syntax-light.min.css")
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFFd92792))
    }

    @Test
    fun `parseAsset panda-syntax-light comment is muted gray`() {
        // .hljs-comment,.hljs-quote { color: #676b79 }
        val result = ThemeParser.parseAsset(context, "panda-syntax-light.min.css")
        assertThat(result[HljsSelectors.COMMENT]?.color).isEqualTo(Color(0xFF676b79))
    }

    @Test
    fun `parseAsset panda-syntax-light string is dark teal`() {
        // .hljs-string (among others) { color: #0d7d6c }
        val result = ThemeParser.parseAsset(context, "panda-syntax-light.min.css")
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF0d7d6c))
    }

    // ── docco ──────────────────────────────────────────────────────────────────────────────────────
    // Uses CSS named colors (navy, teal) and a 3-digit hex (#00f) for params.
    // Validates named-color parsing for less-common color names.

    @Test
    fun `parseAsset docco background is near-white`() {
        val result = ThemeParser.parseAsset(context, "docco.min.css")
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFFf8f8ff))
    }

    @Test
    fun `parseAsset docco keyword is brown-red`() {
        // .hljs-keyword,.hljs-literal,.hljs-selector-tag,.hljs-subst { color: #954121 }
        val result = ThemeParser.parseAsset(context, "docco.min.css")
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF954121))
    }

    @Test
    fun `parseAsset docco tag uses named color navy`() {
        // .hljs-attribute,.hljs-name,.hljs-tag { color: navy } - CSS named navy = #000080
        val result = ThemeParser.parseAsset(context, "docco.min.css")
        assertThat(result[HljsSelectors.TAG]?.color).isEqualTo(Color(0xFF000080))
    }

    @Test
    fun `parseAsset docco variable uses named color teal`() {
        // .hljs-template-variable,.hljs-variable { color: teal } - CSS named teal = #008080
        val result = ThemeParser.parseAsset(context, "docco.min.css")
        assertThat(result[HljsSelectors.VARIABLE]?.color).isEqualTo(Color(0xFF008080))
    }

    @Test
    fun `parseAsset docco params uses 3-digit hex #00f`() {
        // .hljs-params { color: #00f } - 3-digit hex expands to #0000ff
        val result = ThemeParser.parseAsset(context, "docco.min.css")
        assertThat(result[HljsSelectors.PARAMS]?.color).isEqualTo(Color(0xFF0000ff))
    }

    // ── tomorrow-night-blue ────────────────────────────────────────────────────────────────────────
    // Dark blue theme. Base text uses 3-digit hex #fff (white). Good contrast theme test.

    @Test
    fun `parseAsset tomorrow-night-blue background is deep blue`() {
        val result = ThemeParser.parseAsset(context, "tomorrow-night-blue.min.css")
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFF002451))
    }

    @Test
    fun `parseAsset tomorrow-night-blue base text is white via 3-digit hex`() {
        // .hljs { color: #fff } - 3-digit hex #fff expands to #ffffff
        val result = ThemeParser.parseAsset(context, "tomorrow-night-blue.min.css")
        assertThat(result[HljsSelectors.BASE]?.color).isEqualTo(Color(0xFFffffff))
    }

    @Test
    fun `parseAsset tomorrow-night-blue keyword is lavender`() {
        // .hljs-keyword,.hljs-selector-tag { color: #ebbbff }
        val result = ThemeParser.parseAsset(context, "tomorrow-night-blue.min.css")
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFFebbbff))
    }

    @Test
    fun `parseAsset tomorrow-night-blue comment is muted blue`() {
        // .hljs-comment,.hljs-quote { color: #7285b7 }
        val result = ThemeParser.parseAsset(context, "tomorrow-night-blue.min.css")
        assertThat(result[HljsSelectors.COMMENT]?.color).isEqualTo(Color(0xFF7285b7))
    }

    // ── stackoverflow-dark ─────────────────────────────────────────────────────────────────────────
    // Dark theme. Comment uses 3-digit hex #999. Keyword selector list contains a descendant
    // selector (.hljs-meta .hljs-keyword) alongside standalone selectors - tests that descendant
    // skipping does not accidentally prevent standalone hljs-keyword from being parsed.

    @Test
    fun `parseAsset stackoverflow-dark background is near-black`() {
        val result = ThemeParser.parseAsset(context, "stackoverflow-dark.min.css")
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFF1c1b1b))
    }

    @Test
    fun `parseAsset stackoverflow-dark keyword is steel blue`() {
        // .hljs-attr,.hljs-doctag,.hljs-keyword,.hljs-meta .hljs-keyword,...{ color:#88aece }
        // The descendant .hljs-meta .hljs-keyword must be skipped, but standalone hljs-keyword
        // must still receive the correct color.
        val result = ThemeParser.parseAsset(context, "stackoverflow-dark.min.css")
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF88aece))
    }

    @Test
    fun `parseAsset stackoverflow-dark comment uses 3-digit hex #999`() {
        // .hljs-comment { color: #999 } - 3-digit hex expands to #999999
        val result = ThemeParser.parseAsset(context, "stackoverflow-dark.min.css")
        assertThat(result[HljsSelectors.COMMENT]?.color).isEqualTo(Color(0xFF999999))
    }

    @Test
    fun `parseAsset stackoverflow-dark string is olive green`() {
        // .hljs-string (among others) { color: #b5bd68 }
        val result = ThemeParser.parseAsset(context, "stackoverflow-dark.min.css")
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFFb5bd68))
    }
}
