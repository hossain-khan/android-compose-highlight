package dev.hossain.highlight.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HighlightThemeBuiltInTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // ── tomorrow ─────────────────────────────────────────────────────────────

    @Test
    fun tomorrowProducesNonNullTheme() {
        val theme = HighlightTheme.tomorrow(context)
        assertThat(theme).isNotNull()
    }

    @Test
    fun tomorrowColorMapIsNonEmpty() {
        val theme = HighlightTheme.tomorrow(context)
        assertThat(theme.colorMap).isNotEmpty()
    }

    @Test
    fun tomorrowContainsBaseHljsRule() {
        val theme = HighlightTheme.tomorrow(context)
        assertThat(theme.colorMap).containsKey("hljs")
    }

    // ── tomorrowNight ─────────────────────────────────────────────────────────

    @Test
    fun tomorrowNightProducesNonNullTheme() {
        val theme = HighlightTheme.tomorrowNight(context)
        assertThat(theme).isNotNull()
    }

    @Test
    fun tomorrowNightColorMapIsNonEmpty() {
        val theme = HighlightTheme.tomorrowNight(context)
        assertThat(theme.colorMap).isNotEmpty()
    }

    @Test
    fun tomorrowNightContainsBaseHljsRule() {
        val theme = HighlightTheme.tomorrowNight(context)
        assertThat(theme.colorMap).containsKey("hljs")
    }

    // ── atomOneDark ───────────────────────────────────────────────────────────

    @Test
    fun atomOneDarkProducesNonNullTheme() {
        val theme = HighlightTheme.atomOneDark(context)
        assertThat(theme).isNotNull()
    }

    @Test
    fun atomOneDarkColorMapIsNonEmpty() {
        val theme = HighlightTheme.atomOneDark(context)
        assertThat(theme.colorMap).isNotEmpty()
    }

    @Test
    fun atomOneDarkContainsBaseHljsRule() {
        val theme = HighlightTheme.atomOneDark(context)
        assertThat(theme.colorMap).containsKey("hljs")
    }

    // ── atomOneLight ──────────────────────────────────────────────────────────

    @Test
    fun atomOneLightProducesNonNullTheme() {
        val theme = HighlightTheme.atomOneLight(context)
        assertThat(theme).isNotNull()
    }

    @Test
    fun atomOneLightColorMapIsNonEmpty() {
        val theme = HighlightTheme.atomOneLight(context)
        assertThat(theme.colorMap).isNotEmpty()
    }

    @Test
    fun atomOneLightContainsBaseHljsRule() {
        val theme = HighlightTheme.atomOneLight(context)
        assertThat(theme.colorMap).containsKey("hljs")
    }

    // ── light vs dark contrast ────────────────────────────────────────────────

    @Test
    fun tomorrowAndTomorrowNightHaveDifferentBackgroundColors() {
        val light = HighlightTheme.tomorrow(context)
        val dark = HighlightTheme.tomorrowNight(context)
        assertThat(light.backgroundColor).isNotEqualTo(dark.backgroundColor)
    }

    @Test
    fun atomOneLightAndAtomOneDarkHaveDifferentBackgroundColors() {
        val light = HighlightTheme.atomOneLight(context)
        val dark = HighlightTheme.atomOneDark(context)
        assertThat(light.backgroundColor).isNotEqualTo(dark.backgroundColor)
    }

    @Test
    fun allFourThemesHaveDistinctNames() {
        val names =
            listOf(
                HighlightTheme.tomorrow(context).name,
                HighlightTheme.tomorrowNight(context).name,
                HighlightTheme.atomOneDark(context).name,
                HighlightTheme.atomOneLight(context).name,
            )
        assertThat(names.toSet()).hasSize(4)
    }
}
