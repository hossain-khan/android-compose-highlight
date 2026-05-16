package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class ThemeParserAssetTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `parseAsset loads tomorrow theme from bundled assets`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/tomorrow.css")
        assertThat(result).isNotEmpty()
        assertThat(result).containsKey("hljs")
        assertThat(result).containsKey("hljs-keyword")
    }

    @Test
    fun `parseAsset loads tomorrow-night theme from bundled assets`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/tomorrow-night.css")
        assertThat(result).isNotEmpty()
        assertThat(result).containsKey("hljs")
    }

    @Test
    fun `parseAsset loads atom-one-dark theme from bundled assets`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/atom-one-dark.css")
        assertThat(result).isNotEmpty()
        assertThat(result).containsKey("hljs")
    }

    @Test
    fun `parseAsset loads atom-one-light theme from bundled assets`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/atom-one-light.css")
        assertThat(result).isNotEmpty()
        assertThat(result).containsKey("hljs")
    }

    @Test
    fun `parseAsset throws IOException for missing file`() {
        try {
            ThemeParser.parseAsset(context, "nonexistent.css")
            org.junit.Assert.fail("Expected IOException but none was thrown")
        } catch (e: java.io.IOException) {
            // expected
        }
    }

    @Test
    fun `parse with context returns empty map for missing file`() {
        val result = ThemeParser.parse(context, "nonexistent.css")
        assertThat(result).isEmpty()
    }

    // ── Regression tests: ::selection must not overwrite the real .hljs background ──────────────
    // Both tomorrow.css and tomorrow-night.css contain a `.hljs::selection { background-color: X }`
    // rule. Before the fix, ThemeParser would strip the pseudo-element and store the selection color
    // under the "hljs" key, overwriting the correct theme background.

    @Test
    fun `parseAsset tomorrow theme background is white not selection gray`() {
        // tomorrow.css: .hljs { background: #fff }  ← correct
        //               .hljs::selection { background-color: #d6d6d6 }  ← must NOT win
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/tomorrow.css")
        val hljsStyle = result["hljs"]
        assertThat(hljsStyle).isNotNull()
        assertThat(hljsStyle!!.background).isEqualTo(Color(0xFFffffff))
    }

    @Test
    fun `parseAsset tomorrow-night theme background is dark not selection gray`() {
        // tomorrow-night.css: .hljs { background: #2d2d2d }  ← correct
        //                     .hljs::selection { background-color: #515151 }  ← must NOT win
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/tomorrow-night.css")
        val hljsStyle = result["hljs"]
        assertThat(hljsStyle).isNotNull()
        assertThat(hljsStyle!!.background).isEqualTo(Color(0xFF2d2d2d))
    }

    @Test
    fun `parseAsset atom-one-dark theme background is correct dark color`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/atom-one-dark.css")
        val hljsStyle = result["hljs"]
        assertThat(hljsStyle).isNotNull()
        assertThat(hljsStyle!!.background).isEqualTo(Color(0xFF282c34))
    }

    @Test
    fun `parseAsset atom-one-light theme background is correct light color`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/atom-one-light.css")
        val hljsStyle = result["hljs"]
        assertThat(hljsStyle).isNotNull()
        assertThat(hljsStyle!!.background).isEqualTo(Color(0xFFfafafa))
    }

    // ── 1c-light theme regression - named colors and 4-digit hex ─────────────────────────────────
    // 1c-light uses CSS named colors (red, green) and a 4-digit hex (#444a) for tag color.
    // Before the fix these were all parsed as null, so keywords/comments fell back to default
    // blue text and .hljs-tag had no color.

    @Test
    fun `parseAsset 1c-light theme background is white`() {
        val result = ThemeParser.parseAsset(context, "1c-light.min.css")
        val hljsStyle = result["hljs"]
        assertThat(hljsStyle).isNotNull()
        assertThat(hljsStyle!!.background).isEqualTo(Color(0xFFFFFFFF))
    }

    @Test
    fun `parseAsset 1c-light theme keyword is red`() {
        // .hljs-keyword { color: red } - named color must be parsed to #FF0000
        val result = ThemeParser.parseAsset(context, "1c-light.min.css")
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFFFF0000))
    }

    @Test
    fun `parseAsset 1c-light theme comment is green`() {
        // .hljs-comment { color: green } - CSS named green = #008000
        val result = ThemeParser.parseAsset(context, "1c-light.min.css")
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xFF008000))
    }

    @Test
    fun `parseAsset 1c-light theme tag uses 4-digit hex color`() {
        // .hljs-tag { color: #444a } - 4-digit hex must be parsed to Color(68,68,68,170)
        val result = ThemeParser.parseAsset(context, "1c-light.min.css")
        assertThat(result["hljs-tag"]?.color).isEqualTo(Color(68, 68, 68, 170))
    }

    // ── a11y-light theme regression - @media block overwrites keyword color ───────────────────────
    // a11y-light has a @media (-ms-high-contrast) block with .hljs-keyword { font-weight:700 }.
    // Before the @media-stripping fix, that font-weight-only SpanStyle overwrote the real
    // .hljs-keyword { color:#7928a1 } entry, so keywords showed as default text color (dark).

    @Test
    fun `parseAsset a11y-light theme background is near-white`() {
        val result = ThemeParser.parseAsset(context, "a11y-light.min.css")
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFFfefefe))
    }

    @Test
    fun `parseAsset a11y-light theme keyword is purple not dark text`() {
        // .hljs-keyword { color: #7928a1 } - must survive the @media block override
        val result = ThemeParser.parseAsset(context, "a11y-light.min.css")
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFF7928a1))
    }

    @Test
    fun `parseAsset a11y-light theme string is green named color`() {
        // .hljs-string { color: green } = #008000
        val result = ThemeParser.parseAsset(context, "a11y-light.min.css")
        assertThat(result["hljs-string"]?.color).isEqualTo(Color(0xFF008000))
    }
}
