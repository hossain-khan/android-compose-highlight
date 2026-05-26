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
        val light = HighlightTheme.tomorrow()
        val dark = HighlightTheme.tomorrowNight()
        assertThat(light.backgroundColor).isNotEqualTo(dark.backgroundColor)
    }

    @Test
    fun atomOneLightAndAtomOneDarkHaveDifferentBackgroundColors() {
        val light = HighlightTheme.atomOneLight()
        val dark = HighlightTheme.atomOneDark()
        assertThat(light.backgroundColor).isNotEqualTo(dark.backgroundColor)
    }

    @Test
    fun allFourThemesHaveDistinctNames() {
        val names =
            listOf(
                HighlightTheme.tomorrow().name,
                HighlightTheme.tomorrowNight().name,
                HighlightTheme.atomOneDark().name,
                HighlightTheme.atomOneLight().name,
            )
        assertThat(names.toSet()).hasSize(4)
    }
}
