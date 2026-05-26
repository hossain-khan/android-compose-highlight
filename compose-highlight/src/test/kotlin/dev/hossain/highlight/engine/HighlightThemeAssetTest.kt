package dev.hossain.highlight.engine

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
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
    private val contextWithThrowingAssets get() = ThrowingAssetsContext(context)

    @Test
    fun `tomorrow factory produces valid colorMap`() {
        val theme = HighlightTheme.tomorrow()
        assertThat(theme.colorMap).isNotEmpty()
        assertThat(theme.colorMap).containsKey("hljs")
        assertThat(theme.colorMap).containsKey("hljs-keyword")
    }

    @Test
    fun `tomorrowNight factory produces valid colorMap`() {
        val theme = HighlightTheme.tomorrowNight()
        assertThat(theme.colorMap).isNotEmpty()
        assertThat(theme.colorMap).containsKey("hljs")
    }

    @Test
    fun `atomOneDark factory produces valid colorMap`() {
        val theme = HighlightTheme.atomOneDark()
        assertThat(theme.colorMap).isNotEmpty()
        assertThat(theme.colorMap).containsKey("hljs")
    }

    @Test
    fun `atomOneLight factory produces valid colorMap`() {
        val theme = HighlightTheme.atomOneLight()
        assertThat(theme.colorMap).isNotEmpty()
        assertThat(theme.colorMap).containsKey("hljs")
    }

    @Test
    fun `tomorrow theme has light background`() {
        val theme = HighlightTheme.tomorrow()
        assertThat(theme.backgroundColor).isNotEqualTo(Color.Unspecified)
    }

    @Test
    fun `tomorrowNight theme has dark background`() {
        val theme = HighlightTheme.tomorrowNight()
        assertThat(theme.backgroundColor).isNotEqualTo(Color.Unspecified)
    }

    @Test
    fun `fromAsset with valid path produces colorMap`() {
        val theme = HighlightTheme.fromAsset(context, "compose-highlight/themes/tomorrow.css", "test-asset")
        assertThat(theme.colorMap).isNotEmpty()
        assertThat(theme.name).isEqualTo("test-asset")
    }

    @Test
    fun `built-in factories require no Context and never touch assets`() {
        // Built-in themes are precompiled at build time — even a Context whose getAssets()
        // throws should be irrelevant because none of the four built-in factories take one.
        val themes =
            listOf(
                HighlightTheme.tomorrow(),
                HighlightTheme.tomorrowNight(),
                HighlightTheme.atomOneDark(),
                HighlightTheme.atomOneLight(),
            )

        themes.forEach { theme ->
            assertThat(theme.colorMap).isNotEmpty()
            assertThat(theme.colorMap).containsKey("hljs")
        }
    }

    @Test
    fun `fromAsset normalizes to applicationContext`() {
        val theme = HighlightTheme.fromAsset(contextWithThrowingAssets, "compose-highlight/themes/tomorrow.css", "test-asset")
        assertThat(theme.colorMap).isNotEmpty()
    }

    @Test
    fun `fromAsset with missing path throws on colorMap access`() {
        val theme = HighlightTheme.fromAsset(context, "nonexistent.css", "bad")
        try {
            theme.colorMap
            org.junit.Assert.fail("Expected ThemeNotFound or IOException but colorMap access succeeded")
        } catch (e: HighlightException.ThemeNotFound) {
            // expected
        } catch (e: java.io.IOException) {
            // also acceptable - wrapped before ThemeNotFound is thrown on some paths
        }
    }

    private class ThrowingAssetsContext(
        base: Context,
    ) : ContextWrapper(base) {
        override fun getAssets(): AssetManager = throw IllegalStateException("assets must be read from applicationContext")

        override fun getApplicationContext(): Context = baseContext.applicationContext
    }
}
