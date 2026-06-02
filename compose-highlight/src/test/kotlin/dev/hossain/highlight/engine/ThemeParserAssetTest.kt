package dev.hossain.highlight.engine

import android.content.ContextWrapper
import android.content.res.AssetManager
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.internal.ThemeParser
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
        assertThat(result).containsKey(HljsSelectors.BASE)
        assertThat(result).containsKey(HljsSelectors.KEYWORD)
    }

    @Test
    fun `parseAsset loads tomorrow-night theme from bundled assets`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/tomorrow-night.css")
        assertThat(result).isNotEmpty()
        assertThat(result).containsKey(HljsSelectors.BASE)
    }

    @Test
    fun `parseAsset loads atom-one-dark theme from bundled assets`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/atom-one-dark.css")
        assertThat(result).isNotEmpty()
        assertThat(result).containsKey(HljsSelectors.BASE)
    }

    @Test
    fun `parseAsset loads atom-one-light theme from bundled assets`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/atom-one-light.css")
        assertThat(result).isNotEmpty()
        assertThat(result).containsKey(HljsSelectors.BASE)
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

    @Test
    fun `parse with context propagates non-IOException instead of swallowing as empty map`() {
        // Issue #275 - the silent overload's catch was previously `catch (Exception)`, which
        // would swallow parser bugs and asset-system errors as "empty map", masquerading at the
        // HighlightTheme.fromAsset layer as ThemeNotFound. After the narrow to `catch (IOException)`,
        // anything that isn't an IOException must propagate.
        //
        // Easiest way to inject a non-IOException is a ContextWrapper that throws from getAssets()
        // before AssetManager.open() is even reached. IllegalStateException is not an IOException,
        // so post-fix the narrow catch lets it through; pre-fix the broad catch would have caught it.
        val brokenContext =
            object : ContextWrapper(context) {
                override fun getAssets(): AssetManager = throw IllegalStateException("simulated non-IO failure")
            }
        try {
            ThemeParser.parse(brokenContext, "tomorrow.css")
            org.junit.Assert.fail("Expected IllegalStateException to propagate, but the silent overload swallowed it")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().contains("simulated non-IO failure")
        }
    }

    @Test
    fun `parse silent overload returns the same map as parseAsset for valid input`() {
        // Confirms the catch narrow didn't accidentally turn the silent overload into an
        // unconditional emptyMap() - parsing still happens for the happy path. If a future
        // edit moved the catch to wrap parse(css) too broadly, this would fail.
        val silent = ThemeParser.parse(context, "compose-highlight/themes/tomorrow.css")
        val throwing = ThemeParser.parseAsset(context, "compose-highlight/themes/tomorrow.css")
        assertThat(silent).isEqualTo(throwing)
        assertThat(silent).isNotEmpty()
    }

    // ----- Regression tests: ::selection must not overwrite the real .hljs background -----
    // Both tomorrow.css and tomorrow-night.css contain a `.hljs::selection { background-color: X }`
    // rule. Before the fix, ThemeParser would strip the pseudo-element and store the selection color
    // under the HljsSelectors.BASE key, overwriting the correct theme background.

    @Test
    fun `parseAsset tomorrow theme background is white not selection gray`() {
        // tomorrow.css: .hljs { background: #fff }  ← correct
        //               .hljs::selection { background-color: #d6d6d6 }  ← must NOT win
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/tomorrow.css")
        val hljsStyle = result[HljsSelectors.BASE]
        assertThat(hljsStyle).isNotNull()
        assertThat(hljsStyle!!.background).isEqualTo(Color(0xFFffffff))
    }

    @Test
    fun `parseAsset tomorrow-night theme background is dark not selection gray`() {
        // tomorrow-night.css: .hljs { background: #2d2d2d }  ← correct
        //                     .hljs::selection { background-color: #515151 }  ← must NOT win
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/tomorrow-night.css")
        val hljsStyle = result[HljsSelectors.BASE]
        assertThat(hljsStyle).isNotNull()
        assertThat(hljsStyle!!.background).isEqualTo(Color(0xFF2d2d2d))
    }

    @Test
    fun `parseAsset atom-one-dark theme background is correct dark color`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/atom-one-dark.css")
        val hljsStyle = result[HljsSelectors.BASE]
        assertThat(hljsStyle).isNotNull()
        assertThat(hljsStyle!!.background).isEqualTo(Color(0xFF282c34))
    }

    @Test
    fun `parseAsset atom-one-light theme background is correct light color`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/atom-one-light.css")
        val hljsStyle = result[HljsSelectors.BASE]
        assertThat(hljsStyle).isNotNull()
        assertThat(hljsStyle!!.background).isEqualTo(Color(0xFFfafafa))
    }

    // ----- 1c-light theme regression - named colors and 4-digit hex -----
    // 1c-light uses CSS named colors (red, green) and a 4-digit hex (#444a) for tag color.
    // Before the fix these were all parsed as null, so keywords/comments fell back to default
    // blue text and .hljs-tag had no color.

    @Test
    fun `parseAsset 1c-light theme background is white`() {
        val result = ThemeParser.parseAsset(context, "1c-light.min.css")
        val hljsStyle = result[HljsSelectors.BASE]
        assertThat(hljsStyle).isNotNull()
        assertThat(hljsStyle!!.background).isEqualTo(Color(0xFFFFFFFF))
    }

    @Test
    fun `parseAsset 1c-light theme keyword is red`() {
        // .hljs-keyword { color: red } - named color must be parsed to #FF0000
        val result = ThemeParser.parseAsset(context, "1c-light.min.css")
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFFFF0000))
    }

    @Test
    fun `parseAsset 1c-light theme comment is green`() {
        // .hljs-comment { color: green } - CSS named green = #008000
        val result = ThemeParser.parseAsset(context, "1c-light.min.css")
        assertThat(result[HljsSelectors.COMMENT]?.color).isEqualTo(Color(0xFF008000))
    }

    @Test
    fun `parseAsset 1c-light theme tag uses 4-digit hex color`() {
        // .hljs-tag { color: #444a } - 4-digit hex must be parsed to Color(68,68,68,170)
        val result = ThemeParser.parseAsset(context, "1c-light.min.css")
        assertThat(result[HljsSelectors.TAG]?.color).isEqualTo(Color(68, 68, 68, 170))
    }

    // ----- a11y-light theme regression - @media block overwrites keyword color -----
    // a11y-light has a @media (-ms-high-contrast) block with .hljs-keyword { font-weight:700 }.
    // Before the @media-stripping fix, that font-weight-only SpanStyle overwrote the real
    // .hljs-keyword { color:#7928a1 } entry, so keywords showed as default text color (dark).

    @Test
    fun `parseAsset a11y-light theme background is near-white`() {
        val result = ThemeParser.parseAsset(context, "a11y-light.min.css")
        assertThat(result[HljsSelectors.BASE]?.background).isEqualTo(Color(0xFFfefefe))
    }

    @Test
    fun `parseAsset a11y-light theme keyword is purple not dark text`() {
        // .hljs-keyword { color: #7928a1 } - must survive the @media block override
        val result = ThemeParser.parseAsset(context, "a11y-light.min.css")
        assertThat(result[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xFF7928a1))
    }

    @Test
    fun `parseAsset a11y-light theme string is green named color`() {
        // .hljs-string { color: green } = #008000
        val result = ThemeParser.parseAsset(context, "a11y-light.min.css")
        assertThat(result[HljsSelectors.STRING]?.color).isEqualTo(Color(0xFF008000))
    }
}
