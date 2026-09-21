package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.internal.withHtmlParsingErrorHandling
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class CustomThemeResolutionTest {
    private val sampleCss =
        """
        .hljs {
            background: #282c34;
            color: #abb2bf;
        }
        .hljs-keyword {
            color: #c678dd;
        }
        """.trimIndent()

    @Test
    fun `built-in themes have immediate colors and are resolved immediately`() {
        val theme = HighlightTheme.tomorrow()
        assertThat(theme.hasImmediateColors).isTrue()
        assertThat(theme.isResolved).isTrue()
        assertThat(theme.backgroundColor).isNotEqualTo(Color.Unspecified)
        assertThat(theme.defaultTextColor).isNotEqualTo(Color.Unspecified)
    }

    @Test
    fun `fromColorMap themes have immediate colors and are resolved immediately`() {
        val colorMap =
            mapOf(
                HljsSelectors.BASE to SpanStyle(background = Color.Black, color = Color.White),
                HljsSelectors.KEYWORD to SpanStyle(color = Color.Red),
            )
        val theme = HighlightTheme.fromColorMap("custom-map", colorMap)
        assertThat(theme.hasImmediateColors).isTrue()
        assertThat(theme.isResolved).isTrue()
        assertThat(theme.backgroundColor).isEqualTo(Color.Black)
        assertThat(theme.defaultTextColor).isEqualTo(Color.White)
    }

    @Test
    fun `fromCss with explicit colors has immediate colors before colorMap resolution`() {
        val theme =
            HighlightTheme.fromCss(
                cssText = sampleCss,
                name = "explicit-css",
                backgroundColor = Color.Cyan,
                defaultTextColor = Color.Yellow,
            )
        assertThat(theme.hasImmediateColors).isTrue()
        assertThat(theme.isResolved).isFalse()
        assertThat(theme.backgroundColor).isEqualTo(Color.Cyan)
        assertThat(theme.defaultTextColor).isEqualTo(Color.Yellow)

        // Resolving colorMap afterwards retains the explicit colors or parses styles
        assertThat(theme.colorMap).isNotEmpty()
        assertThat(theme.isResolved).isTrue()
        assertThat(theme.backgroundColor).isEqualTo(Color.Cyan)
        assertThat(theme.defaultTextColor).isEqualTo(Color.Yellow)
    }

    @Test
    fun `fromCss without explicit colors is unresolved and does not force parse on hasImmediateColors check`() {
        val theme = HighlightTheme.fromCss(sampleCss, "lazy-css")
        assertThat(theme.isResolved).isFalse()
        assertThat(theme.hasImmediateColors).isFalse()

        // Checking properties directly outside composition should resolve colorMap safely
        assertThat(theme.backgroundColor).isEqualTo(Color(0xFF282c34.toInt()))
        assertThat(theme.defaultTextColor).isEqualTo(Color(0xFFabb2bf.toInt()))
        assertThat(theme.isResolved).isTrue()
        assertThat(theme.hasImmediateColors).isTrue()
    }

    @Test
    fun `fromAsset with missing file throws ThemeNotFound with cause when resolved by colorMap`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val theme = HighlightTheme.fromAsset(context, "non-existent-theme-path.css", "missing-theme")

        assertThat(theme.isResolved).isFalse()
        assertThat(theme.hasImmediateColors).isFalse()

        try {
            theme.colorMap
            throw AssertionError("Expected ThemeNotFound to be thrown")
        } catch (e: HighlightException.ThemeNotFound) {
            assertThat(e.path).isEqualTo("non-existent-theme-path.css")
            assertThat(e.cause).isInstanceOf(IOException::class.java)
        }
    }

    @Test
    fun `withHtmlParsingErrorHandling preserves ThemeNotFound rather than wrapping in HtmlParseFailed`() =
        runTest {
            val ex = HighlightException.ThemeNotFound("custom-missing.css", IOException("File not found"))
            val result =
                withHtmlParsingErrorHandling<Unit> {
                    throw ex
                }

            assertThat(result.isFailure).isTrue()
            val failure = result.exceptionOrNull()
            assertThat(failure).isSameInstanceAs(ex)
            assertThat(failure).isInstanceOf(HighlightException.ThemeNotFound::class.java)
            assertThat(failure).isNotInstanceOf(HighlightException.HtmlParseFailed::class.java)
        }

    @Test
    fun `fromCss with empty or malformed CSS produces empty map without throwing on parse`() {
        val emptyTheme = HighlightTheme.fromCss("", "empty-theme")
        assertThat(emptyTheme.colorMap).isEmpty()
        assertThat(emptyTheme.backgroundColor).isEqualTo(Color.Unspecified)
        assertThat(emptyTheme.defaultTextColor).isEqualTo(Color.Unspecified)

        val malformedTheme = HighlightTheme.fromCss("not valid css at all {;;;}", "malformed-theme")
        assertThat(malformedTheme.colorMap).isEmpty()
        assertThat(malformedTheme.backgroundColor).isEqualTo(Color.Unspecified)
        assertThat(malformedTheme.defaultTextColor).isEqualTo(Color.Unspecified)
    }
}
