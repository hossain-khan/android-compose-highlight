package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Third batch of Robolectric asset tests for [ThemeParser] against 5 more highlight.js themes.
 *
 * Each theme is chosen to exercise a distinct CSS pattern:
 * - **arduino-light**: `rgba()` color with decimal alpha (e.g. `rgba(149,165,166,.8)`)
 * - **nord**: split `.hljs` rules (background + color in separate blocks) verified via
 *   SpanStyle merge; `rgba()` for `background-color`; language-scoped descendant selectors skipped
 * - **cybertopia-cherry**: CSS custom properties (`var(--hljs-*)`) gracefully ignored;
 *   font-style/font-weight still parsed when color is a variable
 * - **an-old-hope**: dark Star Wars theme — confidence baseline with 6-digit hex colors
 * - **atom-one-dark**: very popular theme; `.hljs-class .hljs-title` descendant skipped;
 *   `text-decoration:underline` gracefully ignored
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class ThemeParserMoreThemes3Test {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ── arduino-light ────────────────────────────────────────────────────────

    @Test
    fun `parseAsset arduino-light base hljs background and color`() {
        val result = ThemeParser.parseAsset(context, "arduino-light.min.css")
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFFFFFFFF))
        assertThat(result["hljs"]?.color).isEqualTo(Color(0xFF434F54))
    }

    @Test
    fun `parseAsset arduino-light comment rgba with decimal alpha`() {
        val result = ThemeParser.parseAsset(context, "arduino-light.min.css")
        // rgba(149,165,166,.8) → alpha = round(0.8 * 255) = 204 = 0xCC
        // ARGB: 0xCC95A5A6
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xCC95A5A6))
    }

    @Test
    fun `parseAsset arduino-light keyword color`() {
        val result = ThemeParser.parseAsset(context, "arduino-light.min.css")
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFF00979D))
    }

    @Test
    fun `parseAsset arduino-light section uses 3-digit hex with bold`() {
        val result = ThemeParser.parseAsset(context, "arduino-light.min.css")
        // .hljs-section,.hljs-title{color:#800;font-weight:700} → #800 expands to #880000
        assertThat(result["hljs-section"]?.color).isEqualTo(Color(0xFF880000))
        assertThat(result["hljs-section"]?.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result["hljs-title"]?.color).isEqualTo(Color(0xFF880000))
        assertThat(result["hljs-title"]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `parseAsset arduino-light emphasis is italic and strong is bold`() {
        val result = ThemeParser.parseAsset(context, "arduino-light.min.css")
        assertThat(result["hljs-emphasis"]?.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(result["hljs-strong"]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `parseAsset arduino-light meta descendant selector does not pollute standalone meta`() {
        val result = ThemeParser.parseAsset(context, "arduino-light.min.css")
        // .hljs-meta .hljs-keyword{color:#728e00} has a space → skipped
        // .hljs-meta{color:#434f54} is standalone → stored
        assertThat(result["hljs-meta"]?.color).isEqualTo(Color(0xFF434F54))
    }

    // ── nord ─────────────────────────────────────────────────────────────────

    @Test
    fun `parseAsset nord base hljs has both background and color despite split rules`() {
        val result = ThemeParser.parseAsset(context, "nord.min.css")
        // .hljs{background:#2e3440} and .hljs,.hljs-subst{color:#d8dee9} are two separate rules.
        // SpanStyle merge must preserve background from rule 1 when rule 2 adds color.
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFF2E3440))
        assertThat(result["hljs"]?.color).isEqualTo(Color(0xFFD8DEE9))
    }

    @Test
    fun `parseAsset nord addition background uses rgba half-alpha`() {
        val result = ThemeParser.parseAsset(context, "nord.min.css")
        // .hljs-addition{background-color:rgba(163,190,140,.5)} → alpha=round(0.5*255)=128=0x80
        // ARGB: 0x80A3BE8C
        assertThat(result["hljs-addition"]?.background).isEqualTo(Color(0x80A3BE8C))
    }

    @Test
    fun `parseAsset nord deletion background uses rgba half-alpha`() {
        val result = ThemeParser.parseAsset(context, "nord.min.css")
        // .hljs-deletion{background-color:rgba(191,97,106,.5)} → alpha=128=0x80
        // ARGB: 0x80BF616A
        assertThat(result["hljs-deletion"]?.background).isEqualTo(Color(0x80BF616A))
    }

    @Test
    fun `parseAsset nord keyword color`() {
        val result = ThemeParser.parseAsset(context, "nord.min.css")
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFF81A1C1))
    }

    @Test
    fun `parseAsset nord language-scoped descendant selectors do not overwrite token colors`() {
        val result = ThemeParser.parseAsset(context, "nord.min.css")
        // language-css .hljs-keyword{color:#d08770} has a space → skipped
        // Top-level .hljs-keyword{color:#81a1c1} wins
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFF81A1C1))
    }

    @Test
    fun `parseAsset nord string and comment colors`() {
        val result = ThemeParser.parseAsset(context, "nord.min.css")
        assertThat(result["hljs-string"]?.color).isEqualTo(Color(0xFFA3BE8C))
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xFF4C566A))
    }

    // ── cybertopia-cherry ─────────────────────────────────────────────────────

    @Test
    fun `parseAsset cybertopia-cherry CSS custom properties do not crash the parser`() {
        // Should not throw
        val result = ThemeParser.parseAsset(context, "cybertopia-cherry.min.css")
        assertThat(result).isNotNull()
    }

    @Test
    fun `parseAsset cybertopia-cherry base hljs not in map because var() colors are unresolvable`() {
        val result = ThemeParser.parseAsset(context, "cybertopia-cherry.min.css")
        // .hljs{color:var(--hljs-mono-1);background:var(--hljs-bg)} → both null → not stored
        assertThat(result.containsKey("hljs")).isFalse()
    }

    @Test
    fun `parseAsset cybertopia-cherry font-style italic parsed even when color uses var()`() {
        val result = ThemeParser.parseAsset(context, "cybertopia-cherry.min.css")
        // .hljs-code,.hljs-comment,.hljs-quote{color:var(...);font-style:italic}
        // color=null but fontStyle=Italic → rule IS stored
        assertThat(result["hljs-comment"]?.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(result["hljs-quote"]?.fontStyle).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun `parseAsset cybertopia-cherry strong is bold`() {
        val result = ThemeParser.parseAsset(context, "cybertopia-cherry.min.css")
        assertThat(result["hljs-strong"]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    // ── an-old-hope ───────────────────────────────────────────────────────────

    @Test
    fun `parseAsset an-old-hope base hljs background and color`() {
        val result = ThemeParser.parseAsset(context, "an-old-hope.min.css")
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFF1C1D21))
        assertThat(result["hljs"]?.color).isEqualTo(Color(0xFFC0C5CE))
    }

    @Test
    fun `parseAsset an-old-hope keyword and string colors`() {
        val result = ThemeParser.parseAsset(context, "an-old-hope.min.css")
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFFB45EA4))
        assertThat(result["hljs-string"]?.color).isEqualTo(Color(0xFF4FB4D7))
    }

    @Test
    fun `parseAsset an-old-hope comment and attribute colors`() {
        val result = ThemeParser.parseAsset(context, "an-old-hope.min.css")
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xFFB6B18B))
        assertThat(result["hljs-attribute"]?.color).isEqualTo(Color(0xFFEE7C2B))
    }

    // ── atom-one-dark ─────────────────────────────────────────────────────────

    @Test
    fun `parseAsset atom-one-dark base hljs background and color`() {
        val result = ThemeParser.parseAsset(context, "atom-one-dark.min.css")
        assertThat(result["hljs"]?.color).isEqualTo(Color(0xFFABB2BF))
        assertThat(result["hljs"]?.background).isEqualTo(Color(0xFF282C34))
    }

    @Test
    fun `parseAsset atom-one-dark keyword color`() {
        val result = ThemeParser.parseAsset(context, "atom-one-dark.min.css")
        assertThat(result["hljs-keyword"]?.color).isEqualTo(Color(0xFFC678DD))
    }

    @Test
    fun `parseAsset atom-one-dark string color not overwritten by hljs-meta descendant`() {
        val result = ThemeParser.parseAsset(context, "atom-one-dark.min.css")
        // Selector list includes ".hljs-meta .hljs-string" (has space → skipped)
        // Top-level ".hljs-string" in the same rule still gets #98c379
        assertThat(result["hljs-string"]?.color).isEqualTo(Color(0xFF98C379))
    }

    @Test
    fun `parseAsset atom-one-dark hljs-class hljs-title descendant skipped built_in gets color`() {
        val result = ThemeParser.parseAsset(context, "atom-one-dark.min.css")
        // .hljs-built_in,.hljs-class .hljs-title,.hljs-title.class_{color:#e6c07b}
        // .hljs-class .hljs-title has space → skipped
        // .hljs-built_in gets #e6c07b
        assertThat(result["hljs-built_in"]?.color).isEqualTo(Color(0xFFE6C07B))
    }

    @Test
    fun `parseAsset atom-one-dark text-decoration does not erase existing color for hljs-link`() {
        val result = ThemeParser.parseAsset(context, "atom-one-dark.min.css")
        // .hljs-bullet,.hljs-link,...{color:#61aeee} stores hljs-link with color
        // .hljs-link{text-decoration:underline} → parseDeclarations returns null (not parsed) → skipped
        // The earlier color must not be overwritten
        assertThat(result["hljs-link"]?.color).isEqualTo(Color(0xFF61AEEE))
    }

    @Test
    fun `parseAsset atom-one-dark comment is italic and has color`() {
        val result = ThemeParser.parseAsset(context, "atom-one-dark.min.css")
        assertThat(result["hljs-comment"]?.color).isEqualTo(Color(0xFF5C6370))
        assertThat(result["hljs-comment"]?.fontStyle).isEqualTo(FontStyle.Italic)
    }
}
