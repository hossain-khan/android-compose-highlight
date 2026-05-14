package dev.hossain.highlight.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class ThemeParserAssetTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `parseAsset loads tomorrow theme from bundled assets`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/tomorrow.css")
        assertThat(result).isNotEmpty()
        assertThat(result).containsKey("hljs")
        assertThat(result).containsKey("hljs-keyword")
    }

    @Test
    fun `parseAsset loads tomorrow-night theme from bundled assets`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/tomorrow-night.css")
        assertThat(result).isNotEmpty()
        assertThat(result).containsKey("hljs")
    }

    @Test
    fun `parseAsset loads atom-one-dark theme from bundled assets`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/atom-one-dark.css")
        assertThat(result).isNotEmpty()
        assertThat(result).containsKey("hljs")
    }

    @Test
    fun `parseAsset loads atom-one-light theme from bundled assets`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/atom-one-light.css")
        assertThat(result).isNotEmpty()
        assertThat(result).containsKey("hljs")
    }

    @Test
    fun `parseAsset throws IOException for missing file`() {
        try {
            ThemeParser.parseAsset(context, "nonexistent.css")
            org.junit.Assert.fail("Expected IOException but none was thrown")
        } catch (e: java.io.IOException) {
            // expected
        }
    }

    @Test
    fun `parse with context returns empty map for missing file`() {
        val result = ThemeParser.parse(context, "nonexistent.css")
        assertThat(result).isEmpty()
    }

    @Test
    fun `parseAsset tomorrow theme has background color`() {
        val result = ThemeParser.parseAsset(context, "compose-highlight/themes/tomorrow.css")
        val hljsStyle = result["hljs"]
        assertThat(hljsStyle).isNotNull()
        assertThat(hljsStyle!!.background).isNotNull()
    }
}
