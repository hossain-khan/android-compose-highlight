package dev.hossain.highlight.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HighlightThemeBuiltInTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

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
