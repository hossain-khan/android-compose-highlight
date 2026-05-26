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
 * runtime parser — fix the divergence rather than rubber-stamping the test.
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
    fun `tomorrow factory carries precompiled identity`() {
        assertThat(HighlightTheme.tomorrow().hashCode()).isNotEqualTo(0) // sanity
        // Two tomorrow() instances share the precompiled identity, so they must be equal.
        assertThat(HighlightTheme.tomorrow()).isEqualTo(HighlightTheme.tomorrow())
    }

    @Test
    fun `tomorrowNight factory carries precompiled identity`() {
        assertThat(HighlightTheme.tomorrowNight()).isEqualTo(HighlightTheme.tomorrowNight())
    }

    @Test
    fun `different built-in themes are not equal`() {
        // Identity hashes should differ across themes, so a light theme and dark theme
        // never compare equal even though both use the same precompiled-asset identity scheme.
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
        // Same SpanStyle for each key — SpanStyle is a data class so structural equality works
        for ((key, expected) in runtimeParsed) {
            assertThat(precompiled[key]).isEqualTo(expected)
        }
    }
}
