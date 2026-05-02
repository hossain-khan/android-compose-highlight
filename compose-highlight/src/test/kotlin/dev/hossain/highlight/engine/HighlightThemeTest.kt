package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightThemeTest {
    // Minimal CSS with .hljs base rule plus a keyword rule
    private val sampleCss =
        """
        .hljs{color:#4d4d4c;background:#ffffff}
        .hljs-keyword{color:#8959a8}
        .hljs-string{color:#718c00}
        """.trimIndent()

    // ── fromCss ──────────────────────────────────────────────────────────────

    @Test
    fun `fromCss produces non-empty colorMap for valid CSS`() {
        val theme = HighlightTheme.fromCss(sampleCss, "test")
        assertTrue("Expected non-empty color map", theme.colorMap.isNotEmpty())
    }

    @Test
    fun `fromCss extracts keyword color correctly`() {
        val theme = HighlightTheme.fromCss(sampleCss, "test")
        val style = theme.colorMap["hljs-keyword"]
        assertNotNull("hljs-keyword should be present", style)
        assertEquals(Color(0xFF8959a8.toInt()), style!!.color)
    }

    @Test
    fun `fromCss extracts base hljs rule`() {
        val theme = HighlightTheme.fromCss(sampleCss, "test")
        assertNotNull("hljs base rule should be present", theme.colorMap["hljs"])
    }

    @Test
    fun `fromCss produces empty colorMap for blank CSS`() {
        val theme = HighlightTheme.fromCss("", "empty")
        assertTrue("Empty CSS should produce empty colorMap", theme.colorMap.isEmpty())
    }

    @Test
    fun `fromCss produces empty colorMap for CSS with no hljs selectors`() {
        val theme = HighlightTheme.fromCss("body { color: red; }", "no-hljs")
        assertTrue("CSS without hljs selectors should produce empty colorMap", theme.colorMap.isEmpty())
    }

    // ── fromColorMap ──────────────────────────────────────────────────────────

    @Test
    fun `fromColorMap preserves all entries`() {
        val map =
            mapOf(
                "hljs" to SpanStyle(color = Color.Black, background = Color.White),
                "hljs-keyword" to SpanStyle(color = Color.Blue),
                "hljs-string" to SpanStyle(color = Color.Green),
            )
        val theme = HighlightTheme.fromColorMap(name = "custom", colorMap = map)
        assertEquals(3, theme.colorMap.size)
        assertEquals(Color.Blue, theme.colorMap["hljs-keyword"]!!.color)
    }

    @Test
    fun `fromColorMap with explicit backgroundColor overrides hljs background`() {
        val map = mapOf("hljs" to SpanStyle(color = Color.Black, background = Color.White))
        val theme =
            HighlightTheme.fromColorMap(
                name = "override-bg",
                colorMap = map,
                backgroundColor = Color.Red,
            )
        assertEquals(Color.Red, theme.backgroundColor)
    }

    @Test
    fun `fromColorMap with explicit defaultTextColor overrides hljs color`() {
        val map = mapOf("hljs" to SpanStyle(color = Color.Black, background = Color.White))
        val theme =
            HighlightTheme.fromColorMap(
                name = "override-text",
                colorMap = map,
                defaultTextColor = Color.Green,
            )
        assertEquals(Color.Green, theme.defaultTextColor)
    }

    @Test
    fun `fromColorMap defensive copy prevents external mutation`() {
        val mutable = mutableMapOf("hljs-keyword" to SpanStyle(color = Color.Blue))
        val theme = HighlightTheme.fromColorMap("copy-test", mutable)
        // Mutate the original map after theme creation
        mutable["hljs-keyword"] = SpanStyle(color = Color.Red)
        // Theme should still have the original color
        assertEquals(Color.Blue, theme.colorMap["hljs-keyword"]?.color)
    }

    @Test
    fun `fromColorMap with empty map has empty colorMap`() {
        val theme = HighlightTheme.fromColorMap("empty", emptyMap())
        assertTrue(theme.colorMap.isEmpty())
    }

    // ── backgroundColor / defaultTextColor ───────────────────────────────────

    @Test
    fun `backgroundColor is derived from hljs base rule`() {
        val theme = HighlightTheme.fromCss(sampleCss, "test")
        assertEquals(Color(0xFFffffff.toInt()), theme.backgroundColor)
    }

    @Test
    fun `defaultTextColor is derived from hljs base rule`() {
        val theme = HighlightTheme.fromCss(sampleCss, "test")
        assertEquals(Color(0xFF4d4d4c.toInt()), theme.defaultTextColor)
    }

    @Test
    fun `backgroundColor is Unspecified when hljs rule has no background`() {
        val theme = HighlightTheme.fromCss(".hljs-keyword{color:#8959a8}", "no-bg")
        assertEquals(Color.Unspecified, theme.backgroundColor)
    }

    @Test
    fun `defaultTextColor is Unspecified when hljs rule has no color`() {
        val theme = HighlightTheme.fromCss(".hljs{background:#ffffff}", "no-text")
        assertEquals(Color.Unspecified, theme.defaultTextColor)
    }

    @Test
    fun `backgroundColor from fromColorMap without hljs entry is Unspecified`() {
        val theme =
            HighlightTheme.fromColorMap(
                name = "no-hljs",
                colorMap = mapOf("hljs-keyword" to SpanStyle(color = Color.Blue)),
            )
        assertEquals(Color.Unspecified, theme.backgroundColor)
    }

    // ── colorMap lazy initialization ──────────────────────────────────────────

    @Test
    fun `colorMap returns same instance on repeated access`() {
        val theme = HighlightTheme.fromCss(sampleCss, "lazy-test")
        val first = theme.colorMap
        val second = theme.colorMap
        assertSame("colorMap should be the same cached instance", first, second)
    }

    // ── equals / hashCode / toString ─────────────────────────────────────────

    @Test
    fun `themes with same name are equal`() {
        val a = HighlightTheme.fromCss(sampleCss, "same")
        val b = HighlightTheme.fromColorMap("same", emptyMap())
        assertEquals(a, b)
    }

    @Test
    fun `themes with different names are not equal`() {
        val a = HighlightTheme.fromCss(sampleCss, "alpha")
        val b = HighlightTheme.fromCss(sampleCss, "beta")
        assertNotEquals(a, b)
    }

    @Test
    fun `themes with same name have same hashCode`() {
        val a = HighlightTheme.fromCss(sampleCss, "same")
        val b = HighlightTheme.fromColorMap("same", emptyMap())
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `toString contains the theme name`() {
        val theme = HighlightTheme.fromCss(sampleCss, "my-theme")
        assertTrue("toString should include the name", theme.toString().contains("my-theme"))
    }

    // ── name property ─────────────────────────────────────────────────────────

    @Test
    fun `name property returns the value passed to factory`() {
        val theme = HighlightTheme.fromCss(sampleCss, "expected-name")
        assertEquals("expected-name", theme.name)
    }

    // ── fromColorMap with bold FontWeight entry ───────────────────────────────

    @Test
    fun `fromColorMap preserves FontWeight in SpanStyle`() {
        val map = mapOf("hljs-strong" to SpanStyle(fontWeight = FontWeight.Bold, color = Color.Yellow))
        val theme = HighlightTheme.fromColorMap("bold-test", map)
        assertEquals(FontWeight.Bold, theme.colorMap["hljs-strong"]?.fontWeight)
    }

    // ── theme not equal to non-HighlightTheme ─────────────────────────────────

    @Test
    fun `theme is not equal to non-HighlightTheme object`() {
        val theme = HighlightTheme.fromCss(sampleCss, "test")
        assertNotEquals(theme, "test")
        assertNotEquals(theme, null)
    }

    // ── fromCss colorMap entries match ThemeParser directly ──────────────────

    @Test
    fun `fromCss colorMap matches direct ThemeParser output`() {
        val expected = ThemeParser.parse(sampleCss)
        val theme = HighlightTheme.fromCss(sampleCss, "match-test")
        assertEquals(expected, theme.colorMap)
    }

    // ── hljs keyword absent gives null entry ─────────────────────────────────

    @Test
    fun `colorMap returns null for unknown class`() {
        val theme = HighlightTheme.fromCss(sampleCss, "unknown")
        assertNull(theme.colorMap["hljs-does-not-exist"])
    }
}
