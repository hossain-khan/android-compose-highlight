package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.build.CssThemeParser
import dev.hossain.highlight.build.ParsedFontWeight
import dev.hossain.highlight.build.ParsedStyle
import dev.hossain.highlight.engine.internal.ThemeParser
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Parity safety net for build-time theme precompilation and CSS parser conformance.
 *
 * The build emits `GeneratedThemes.kt` from the eight bundled CSS files using [CssThemeParser] in
 * `buildSrc/` that mirrors the runtime [ThemeParser]. This test verifies the two stay in
 * sync by:
 * - Loading each bundled CSS file with the runtime parser at test time.
 * - Comparing the resulting `Map<String, SpanStyle>` against the precompiled `GeneratedThemes.*`.
 * - Asserting the runtime-computed `contentDigest256("asset", path)` matches the embedded
 *   `*_IDENTITY` literal so [HighlightTheme] equality and Compose recomposition keys stay
 *   consistent before and after the refactor.
 * - Running a shared conformance corpus of CSS rules (8-digit hex `#RRGGBBAA`, `!important`,
 *   `bolder`/`lighter`, `oblique`, `rgb()`/`rgba()`, named colors, cascade merging, selectors,
 *   and malformed CSS) through both [CssThemeParser] and [ThemeParser] to ensure future bundled
 *   themes using supported syntax cannot generate a silently different map.
 *
 * If this test fails after editing the runtime parser, regenerate the precompiled file:
 *   `./gradlew :compose-highlight:generateThemes`
 *
 * If it fails after editing the buildSrc parser, the buildSrc parser has drifted from the
 * runtime parser - fix the divergence rather than rubber-stamping the test.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class GeneratedThemesParityTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `tomorrow precompiled map equals runtime parse`() {
        assertParity("compose-highlight/themes/tomorrow.css", GeneratedThemes.TOMORROW)
    }

    @Test
    fun `tomorrowNight precompiled map equals runtime parse`() {
        assertParity("compose-highlight/themes/tomorrow-night.css", GeneratedThemes.TOMORROW_NIGHT)
    }

    @Test
    fun `atomOneDark precompiled map equals runtime parse`() {
        assertParity("compose-highlight/themes/atom-one-dark.css", GeneratedThemes.ATOM_ONE_DARK)
    }

    @Test
    fun `atomOneLight precompiled map equals runtime parse`() {
        assertParity("compose-highlight/themes/atom-one-light.css", GeneratedThemes.ATOM_ONE_LIGHT)
    }

    @Test
    fun `githubLight precompiled map equals runtime parse`() {
        assertParity("compose-highlight/themes/github.css", GeneratedThemes.GITHUB_LIGHT)
    }

    @Test
    fun `githubDark precompiled map equals runtime parse`() {
        assertParity("compose-highlight/themes/github-dark.css", GeneratedThemes.GITHUB_DARK)
    }

    @Test
    fun `draculaDark precompiled map equals runtime parse`() {
        assertParity("compose-highlight/themes/dracula.css", GeneratedThemes.DRACULA_DARK)
    }

    @Test
    fun `alucardLight precompiled map equals runtime parse`() {
        assertParity("compose-highlight/themes/alucard.css", GeneratedThemes.ALUCARD_LIGHT)
    }

    // ----- Identity parity -----
    // HighlightTheme.equals compares (name, contentIdentity). The runtime fromAsset factory
    // computes contentIdentity via contentDigest256("asset", path); the buildSrc generator
    // reproduces that same digest and embeds it in a LongArray literal. If those two computations
    // ever drift, the equality check below fails - the test is a real parity assertion, not
    // a tautology over two copies of the same constant.

    @Test
    fun `tomorrow identity matches runtime fromAsset hash`() {
        assertThat(HighlightTheme.tomorrow())
            .isEqualTo(HighlightTheme.fromAsset(context, "compose-highlight/themes/tomorrow.css", "tomorrow"))
    }

    @Test
    fun `tomorrowNight identity matches runtime fromAsset hash`() {
        assertThat(HighlightTheme.tomorrowNight())
            .isEqualTo(HighlightTheme.fromAsset(context, "compose-highlight/themes/tomorrow-night.css", "tomorrow-night"))
    }

    @Test
    fun `atomOneDark identity matches runtime fromAsset hash`() {
        assertThat(HighlightTheme.atomOneDark())
            .isEqualTo(HighlightTheme.fromAsset(context, "compose-highlight/themes/atom-one-dark.css", "atom-one-dark"))
    }

    @Test
    fun `atomOneLight identity matches runtime fromAsset hash`() {
        assertThat(HighlightTheme.atomOneLight())
            .isEqualTo(HighlightTheme.fromAsset(context, "compose-highlight/themes/atom-one-light.css", "atom-one-light"))
    }

    @Test
    fun `githubLight identity matches runtime fromAsset hash`() {
        assertThat(HighlightTheme.githubLight())
            .isEqualTo(HighlightTheme.fromAsset(context, "compose-highlight/themes/github.css", "github"))
    }

    @Test
    fun `githubDark identity matches runtime fromAsset hash`() {
        assertThat(HighlightTheme.githubDark())
            .isEqualTo(HighlightTheme.fromAsset(context, "compose-highlight/themes/github-dark.css", "github-dark"))
    }

    @Test
    fun `draculaDark identity matches runtime fromAsset hash`() {
        assertThat(HighlightTheme.draculaDark())
            .isEqualTo(HighlightTheme.fromAsset(context, "compose-highlight/themes/dracula.css", "dracula"))
    }

    @Test
    fun `alucardLight identity matches runtime fromAsset hash`() {
        assertThat(HighlightTheme.alucardLight())
            .isEqualTo(HighlightTheme.fromAsset(context, "compose-highlight/themes/alucard.css", "alucard"))
    }

    @Test
    fun `different built-in themes are not equal`() {
        // Sanity: every built-in carries a distinct identity digest, so the equality check above
        // is meaningful - it fails if the buildSrc digest collides with anything other than the
        // matching runtime-computed value.
        assertThat(HighlightTheme.tomorrow()).isNotEqualTo(HighlightTheme.tomorrowNight())
        assertThat(HighlightTheme.atomOneLight()).isNotEqualTo(HighlightTheme.atomOneDark())
        assertThat(HighlightTheme.githubLight()).isNotEqualTo(HighlightTheme.githubDark())
        assertThat(HighlightTheme.alucardLight()).isNotEqualTo(HighlightTheme.draculaDark())
        assertThat(HighlightTheme.githubLight()).isNotEqualTo(HighlightTheme.tomorrow())
    }

    private fun assertParity(
        assetPath: String,
        precompiled: Map<String, SpanStyle>,
    ) {
        val runtimeParsed = ThemeParser.parseAsset(context, assetPath)
        // Same set of keys
        assertThat(precompiled.keys).isEqualTo(runtimeParsed.keys)
        // Same SpanStyle for each key - SpanStyle is a data class so structural equality works
        for ((key, expected) in runtimeParsed) {
            assertThat(precompiled[key]).isEqualTo(expected)
        }
    }

    // ----- Shared CSS parser conformance corpus -----

    @Test
    fun `eight-digit hex uses CSS RRGGBBAA ordering in both parsers`() {
        val css =
            """
            .hljs { color: #11223344; background: #ff000080 }
            .hljs-keyword { color: #abc }
            .hljs-string { color: #444a }
            .hljs-comment { color: #78bb65 }
            """.trimIndent()
        val map = assertCorpusParity(css)
        assertThat(map[HljsSelectors.BASE]?.color).isEqualTo(Color(0x11, 0x22, 0x33, 0x44))
        assertThat(map[HljsSelectors.BASE]?.background).isEqualTo(Color(255, 0, 0, 128))
        assertThat(map[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(0xAA, 0xBB, 0xCC, 0xFF))
        assertThat(map[HljsSelectors.STRING]?.color).isEqualTo(Color(0x44, 0x44, 0x44, 0xAA))
        assertThat(map[HljsSelectors.COMMENT]?.color).isEqualTo(Color(0x78, 0xBB, 0x65, 0xFF))
    }

    @Test
    fun `important bolder lighter and oblique behave identically in both parsers`() {
        val css =
            """
            .hljs-title {
                color: #78bb65 !important;
                background-color: #112233 !IMPORTANT;
                font-weight: bolder !Important;
                font-style: oblique !important;
            }
            .hljs-comment {
                color: #888888;
                font-weight: lighter;
                font-style: italic;
            }
            .hljs-strong { font-weight: 600 }
            .hljs-emphasis { font-weight: 500 }
            """.trimIndent()
        val map = assertCorpusParity(css)
        assertThat(map[HljsSelectors.TITLE]).isEqualTo(
            SpanStyle(
                color = Color(0xFF78BB65),
                background = Color(0xFF112233),
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
            ),
        )
        assertThat(map[HljsSelectors.COMMENT]).isEqualTo(
            SpanStyle(
                color = Color(0xFF888888),
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
            ),
        )
        assertThat(map[HljsSelectors.STRONG]?.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(map[HljsSelectors.EMPHASIS]?.fontWeight).isEqualTo(FontWeight.Normal)
    }

    @Test
    fun `rgb rgba and named colors behave identically in both parsers`() {
        val css =
            """
            .hljs { background: rgb(30 30 30); color: rgb(212, 212, 212) }
            .hljs-keyword { color: rgba(255, 0, 128, 0.75) }
            .hljs-string { color: rgb(100% 0% 50% / 50%) }
            .hljs-comment { color: rgba(255 0 128 / 0.5) }
            .hljs-number { color: rgba(0, 128, 255, 200) }
            .hljs-literal { color: GOLD }
            .hljs-type { color: rebeccapurple }
            """.trimIndent()
        val map = assertCorpusParity(css)
        assertThat(map[HljsSelectors.BASE]?.background).isEqualTo(Color(30, 30, 30))
        assertThat(map[HljsSelectors.BASE]?.color).isEqualTo(Color(212, 212, 212))
        assertThat(map[HljsSelectors.KEYWORD]?.color).isEqualTo(Color(255, 0, 128, 191))
        assertThat(map[HljsSelectors.STRING]?.color).isEqualTo(Color(255, 0, 127, 127))
        assertThat(map[HljsSelectors.COMMENT]?.color).isEqualTo(Color(255, 0, 128, 128))
        assertThat(map[HljsSelectors.NUMBER]?.color).isEqualTo(Color(0, 128, 255, 200))
        assertThat(map[HljsSelectors.LITERAL]?.color).isEqualTo(Color(0xFFFFD700))
        assertThat(map[HljsSelectors.TYPE]).isNull()
    }

    @Test
    fun `selectors cascade merging and unsupported forms behave identically in both parsers`() {
        val corpus =
            listOf(
                // Compound selector + primary-head fallback + split-rule cascade
                """
                .hljs { background: #2e3440 }
                .hljs, .hljs-subst { color: #d8dee9 }
                .hljs-title.function_ { color: #4271ae }
                .hljs-title { font-weight: bold }
                """.trimIndent(),
                // Explicit primary head before compound selector must not be overwritten
                """
                .hljs-title { color: #aabbcc }
                .hljs-title.function_ { color: #112233 }
                """.trimIndent(),
                // Unsupported selectors, at-rules, comments, variables, and non-SpanStyle properties
                """
                @charset "utf-8";
                @import url("other.css");
                /* top-level comment */
                .hljs { color: #7ea2b4; background: #161b1d }
                .hljs ::selection, .hljs::selection { background-color: #516d7b }
                .hljs:hover { background: #ff0000 }
                .hljs-meta .hljs-keyword { color: #8959a8 }
                .hljs > .hljs-keyword { color: red }
                .hljs + .hljs-string { color: blue }
                .hljs ~ .hljs-number { color: green }
                :is(.a, .b), .hljs-keyword { color: #112233 }
                .hljs-string { color: var(--hljs-string); text-decoration: underline; opacity: 0.8 }
                .hljs-comment { color: inherit; font-style: oblique }
                @media screen and (prefers-color-scheme: dark) {
                    .hljs-keyword { color: #ffffff }
                }
                """.trimIndent(),
                // Malformed CSS edge cases
                "",
                "   \n\t  ",
                "} .hljs-keyword { color: #ff0000 }",
                ".hljs-keyword { color: #112233 } .hljs-string { color: #445566",
                ".hljs-keyword { color: #112233; { nested: 1; } font-weight: bolder }",
                ".hljs-keyword { color: #GGGGGG } .hljs-string { color: #abcde } .hljs-number { color: # }",
                ".hljs-keyword { color: rgb(255, 0) } .hljs-string { color: rgba(0, 0, 0, NaN) }",
                ".hljs-keyword { color: rgba(0, 0, 0, Infinity%) } .hljs-string { color: rgba(0, 0, 0, 2.5) }",
            )
        for (css in corpus) {
            assertCorpusParity(css)
        }
    }

    private fun assertCorpusParity(css: String): Map<String, SpanStyle> {
        val runtimeMap = ThemeParser.parse(css)
        val buildTimeMap =
            CssThemeParser
                .parse(css)
                .associate { (selector, style) -> selector to style.toSpanStyle() }
        assertThat(buildTimeMap.keys).containsExactlyElementsIn(runtimeMap.keys).inOrder()
        for ((key, expected) in runtimeMap) {
            assertThat(buildTimeMap[key]).isEqualTo(expected)
        }
        return runtimeMap
    }

    private fun ParsedStyle.toSpanStyle(): SpanStyle =
        SpanStyle(
            color = color?.let { Color(it) } ?: Color.Unspecified,
            fontWeight =
                when (fontWeight) {
                    ParsedFontWeight.Bold -> FontWeight.Bold
                    ParsedFontWeight.Normal -> FontWeight.Normal
                    null -> null
                },
            fontStyle = if (fontStyleItalic == true) FontStyle.Italic else null,
            background = background?.let { Color(it) } ?: Color.Unspecified,
        )
}
