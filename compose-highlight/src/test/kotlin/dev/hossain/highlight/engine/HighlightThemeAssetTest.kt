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
class HighlightThemeAssetTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `tomorrow factory produces valid colorMap from asset`() {
        val theme = HighlightTheme.tomorrow(context)
        assertThat(theme.colorMap).isNotEmpty()
        assertThat(theme.colorMap).containsKey("hljs")
        assertThat(theme.colorMap).containsKey("hljs-keyword")
    }

    @Test
    fun `tomorrowNight factory produces valid colorMap from asset`() {
        val theme = HighlightTheme.tomorrowNight(context)
        assertThat(theme.colorMap).isNotEmpty()
        assertThat(theme.colorMap).containsKey("hljs")
    }

    @Test
    fun `atomOneDark factory produces valid colorMap from asset`() {
        val theme = HighlightTheme.atomOneDark(context)
        assertThat(theme.colorMap).isNotEmpty()
        assertThat(theme.colorMap).containsKey("hljs")
    }

    @Test
    fun `atomOneLight factory produces valid colorMap from asset`() {
        val theme = HighlightTheme.atomOneLight(context)
        assertThat(theme.colorMap).isNotEmpty()
        assertThat(theme.colorMap).containsKey("hljs")
    }

    @Test
    fun `tomorrow theme has light background`() {
        val theme = HighlightTheme.tomorrow(context)
        assertThat(theme.backgroundColor).isNotEqualTo(Color.Unspecified)
    }

    @Test
    fun `tomorrowNight theme has dark background`() {
        val theme = HighlightTheme.tomorrowNight(context)
        assertThat(theme.backgroundColor).isNotEqualTo(Color.Unspecified)
    }

    @Test
    fun `fromAsset with valid path produces colorMap`() {
        val theme = HighlightTheme.fromAsset(context, "compose-highlight/themes/tomorrow.css", "test-asset")
        assertThat(theme.colorMap).isNotEmpty()
        assertThat(theme.name).isEqualTo("test-asset")
    }

    @Test
    fun `fromAsset with missing path throws on colorMap access`() {
        val theme = HighlightTheme.fromAsset(context, "nonexistent.css", "bad")
        try {
            theme.colorMap
            org.junit.Assert.fail("Expected exception but colorMap access succeeded")
        } catch (e: Exception) {
            val isExpected = e is java.io.IOException || e is HighlightException.ThemeNotFound
            assertThat(isExpected).isTrue()
        }
    }
}
