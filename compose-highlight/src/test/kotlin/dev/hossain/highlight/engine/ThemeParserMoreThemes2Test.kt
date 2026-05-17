package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Second batch of Robolectric asset tests for [ThemeParser] against 5 more highlight.js themes.
 *
 * Each theme is chosen to exercise a specific CSS pattern:
 * - agate: dark theme with 3-digit hex colors (#888 comment, #ffa attribute)
 * - routeros: keyword has font-weight:700 only (no color) - bold with no color
 *   and 3-digit hex #800 for string tokens
 * - grayscale: keyword has both color and font-weight in the same rule;
 *   string has url() as background value - url() must not break color parsing
 * - xcode: string selector list includes descendant .hljs-meta .hljs-string
 *   alongside standalone .hljs-string - tests descendant skipping
 * - paraiso-light: clean light theme, good general-purpose baseline
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class ThemeParserMoreThemes2Test {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ── agate ──────────────────────────────────────────────────────────────────────────────────────
    // Dark theme. Comment and attribute use 3-digit hex colors (#888, #ffa).

    @Test
    fun `parseAsset agate background is dark gray`() {
        // .hljs { background: #333 } - 3-digit hex expands to #333333
        val result = ThemeParser.parseAsset(context, "agate.min.css")
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFF333333))
    }

    @Test
    fun `parseAsset agate base text is white via 3-digit hex`() {
        // .hljs { color: #fff } - 3-digit hex expands to #ffffff
        val result = ThemeParser.parseAsset(context, "agate.min.css")
        assertThat(result["hljs"]?.color).isEqualTo(Color(0xFFffffff))
    }

    @Test
    fun `parseAsset agate keyword is peach`() {
        // .hljs-built_in,.hljs-keyword,.hljs-literal,.hljs-selector-tag { color: #fcc28c }
        val result = ThemeParser.parseAsset(context, "agate.min.css")
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFFfcc28c))
    }

    @Test
    fun `parseAsset agate comment uses 3-digit hex #888`() {
        // .hljs-code,.hljs-comment,.hljs-formula { color: #888 } - 3-digit expands to #888888
        val result = ThemeParser.parseAsset(context, "agate.min.css")
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xFF888888))
    }

    @Test
    fun `parseAsset agate attribute uses 3-digit hex #ffa`() {
        // .hljs-attribute,.hljs-title,.hljs-type { color: #ffa } - expands to #ffffaa
        val result = ThemeParser.parseAsset(context, "agate.min.css")
        assertThat(result["hljs-attribute"]?.color).isEqualTo(Color(0xFFffffaa))
    }

    // ── routeros ───────────────────────────────────────────────────────────────────────────────────
    // Keyword rule has font-weight:700 only (no color). String uses 3-digit hex #800.

    @Test
    fun `parseAsset routeros background is light gray`() {
        val result = ThemeParser.parseAsset(context, "routeros.min.css")
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFFf0f0f0))
    }

    @Test
    fun `parseAsset routeros keyword is bold with no color`() {
        // .hljs-keyword (among others) { font-weight: 700 } - no color declared for keyword
        val result = ThemeParser.parseAsset(context, "routeros.min.css")
        assertThat(result["hljs-keyword"]?.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `parseAsset routeros string uses 3-digit hex #800`() {
        // .hljs-string (among others) { color: #800 } - 3-digit expands to #880000
        val result = ThemeParser.parseAsset(context, "routeros.min.css")
        assertThat(result["hljs-string"]?.color).isEqualTo(Color(0xFF880000))
    }

    @Test
    fun `parseAsset routeros comment is muted gray`() {
        // .hljs-comment { color: #888 } - 3-digit expands to #888888
        val result = ThemeParser.parseAsset(context, "routeros.min.css")
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xFF888888))
    }

    // ── grayscale ──────────────────────────────────────────────────────────────────────────────────
    // Keyword has both color and font-weight in the same declaration block.
    // String has url() as the background value - url() must not break color parsing.

    @Test
    fun `parseAsset grayscale background is white`() {
        val result = ThemeParser.parseAsset(context, "grayscale.min.css")
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFFffffff))
    }

    @Test
    fun `parseAsset grayscale keyword has both color and bold in same rule`() {
        // .hljs-keyword,.hljs-selector-tag,.hljs-subst { color: #333; font-weight: 700 }
        val result = ThemeParser.parseAsset(context, "grayscale.min.css")
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFF333333))
        assertThat(result["hljs-keyword"]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `parseAsset grayscale comment uses 3-digit hex #777`() {
        // .hljs-comment,.hljs-quote { color: #777 } - 3-digit expands to #777777
        val result = ThemeParser.parseAsset(context, "grayscale.min.css")
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xFF777777))
    }

    @Test
    fun `parseAsset grayscale string color is parsed despite url() background`() {
        // .hljs-string { color: #333; background: url(data:image/png;...) }
        // url() is not a parseable color - background must be Unspecified but color must survive
        val result = ThemeParser.parseAsset(context, "grayscale.min.css")
        assertThat(result["hljs-string"]?.color).isEqualTo(Color(0xFF333333))
        assertThat(result["hljs-string"]?.background).isEqualTo(Color.Unspecified)
    }

    // ── xcode ──────────────────────────────────────────────────────────────────────────────────────
    // String selector list includes descendant .hljs-meta .hljs-string alongside standalone
    // .hljs-string. Tests that descendant skipping does not prevent standalone entry from parsing.

    @Test
    fun `parseAsset xcode background is white`() {
        val result = ThemeParser.parseAsset(context, "xcode.min.css")
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFFffffff))
    }

    @Test
    fun `parseAsset xcode keyword is purple`() {
        // .hljs-attribute,.hljs-keyword,.hljs-literal,.hljs-name,.hljs-selector-tag,.hljs-tag
        //   { color: #aa0d91 }
        val result = ThemeParser.parseAsset(context, "xcode.min.css")
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFFaa0d91))
    }

    @Test
    fun `parseAsset xcode comment is dark green`() {
        // .hljs-comment,.hljs-quote { color: #007400 }
        val result = ThemeParser.parseAsset(context, "xcode.min.css")
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xFF007400))
    }

    @Test
    fun `parseAsset xcode string is red despite descendant selector in rule`() {
        // .hljs-code,.hljs-meta .hljs-string,.hljs-string { color: #c41a16 }
        // The descendant .hljs-meta .hljs-string must be skipped, but standalone .hljs-string
        // must still receive the color.
        val result = ThemeParser.parseAsset(context, "xcode.min.css")
        assertThat(result["hljs-string"]?.color).isEqualTo(Color(0xFFc41a16))
    }

    // ── paraiso-light ──────────────────────────────────────────────────────────────────────────────
    // Clean light theme. Straightforward 6-digit hex colors throughout. Good general baseline.

    @Test
    fun `parseAsset paraiso-light background is warm off-white`() {
        val result = ThemeParser.parseAsset(context, "paraiso-light.min.css")
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFFe7e9db))
    }

    @Test
    fun `parseAsset paraiso-light base text color is dark purple-gray`() {
        val result = ThemeParser.parseAsset(context, "paraiso-light.min.css")
        assertThat(result["hljs"]?.color).isEqualTo(Color(0xFF4f424c))
    }

    @Test
    fun `parseAsset paraiso-light keyword is purple`() {
        // .hljs-keyword,.hljs-selector-tag { color: #815ba4 }
        val result = ThemeParser.parseAsset(context, "paraiso-light.min.css")
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFF815ba4))
    }

    @Test
    fun `parseAsset paraiso-light string is green`() {
        // .hljs-addition,.hljs-bullet,.hljs-string,.hljs-symbol { color: #48b685 }
        val result = ThemeParser.parseAsset(context, "paraiso-light.min.css")
        assertThat(result["hljs-string"]?.color).isEqualTo(Color(0xFF48b685))
    }

    @Test
    fun `parseAsset paraiso-light comment is muted purple`() {
        // .hljs-comment,.hljs-quote { color: #776e71 }
        val result = ThemeParser.parseAsset(context, "paraiso-light.min.css")
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xFF776e71))
    }
}
