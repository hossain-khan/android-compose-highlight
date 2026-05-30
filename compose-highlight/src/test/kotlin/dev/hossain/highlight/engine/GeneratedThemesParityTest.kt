package dev.hossain.highlight.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Parity safety net for build-time theme precompilation.
 *
 * The build emits `GeneratedThemes.kt` from the four bundled CSS files using a parser in
 * `buildSrc/` that mirrors the runtime [ThemeParser]. This test verifies the two stay in
 * sync by:
 * - Loading each bundled CSS file with the runtime parser at test time.
 * - Comparing the resulting `Map<String, SpanStyle>` against the precompiled `GeneratedThemes.*`.
 * - Asserting the runtime-computed `contentDigest64("asset", path)` matches the embedded
 *   `*_IDENTITY` literal so [HighlightTheme] equality and Compose recomposition keys stay
 *   consistent before and after the refactor.
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

    // ── Identity parity ───────────────────────────────────────────────────────────────────────────
    // HighlightTheme.equals compares (name, contentIdentity). The runtime fromAsset factory
    // computes contentIdentity via contentDigest64("asset", path); the buildSrc generator
    // reproduces that same hash and embeds it as a Long literal. If those two computations
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
    fun `different built-in themes are not equal`() {
        // Sanity: every built-in carries a distinct identity hash, so the equality check above
        // is meaningful - it fails if the buildSrc hash collides with anything other than the
        // matching runtime-computed value.
        assertThat(HighlightTheme.tomorrow()).isNotEqualTo(HighlightTheme.tomorrowNight())
        assertThat(HighlightTheme.atomOneLight()).isNotEqualTo(HighlightTheme.atomOneDark())
    }

    private fun assertParity(
        assetPath: String,
        precompiled: Map<String, androidx.compose.ui.text.SpanStyle>,
    ) {
        val runtimeParsed = ThemeParser.parseAsset(context, assetPath)
        // Same set of keys
        assertThat(precompiled.keys).isEqualTo(runtimeParsed.keys)
        // Same SpanStyle for each key - SpanStyle is a data class so structural equality works
        for ((key, expected) in runtimeParsed) {
            assertThat(precompiled[key]).isEqualTo(expected)
        }
    }
}
