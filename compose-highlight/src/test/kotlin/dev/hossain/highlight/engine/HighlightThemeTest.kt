package dev.hossain.highlight.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.time.Duration

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
        assertThat(theme.colorMap).isNotEmpty()
    }

    @Test
    fun `fromCss extracts keyword color correctly`() {
        val theme = HighlightTheme.fromCss(sampleCss, "test")
        val style = theme.colorMap["hljs-keyword"]
        assertThat(style).isNotNull()
        assertThat(style!!.color).isEqualTo(Color(0xFF8959a8.toInt()))
    }

    @Test
    fun `fromCss extracts base hljs rule`() {
        val theme = HighlightTheme.fromCss(sampleCss, "test")
        assertThat(theme.colorMap["hljs"]).isNotNull()
    }

    @Test
    fun `fromCss produces empty colorMap for blank CSS`() {
        val theme = HighlightTheme.fromCss("", "empty")
        assertThat(theme.colorMap).isEmpty()
    }

    @Test
    fun `fromCss produces empty colorMap for CSS with no hljs selectors`() {
        val theme = HighlightTheme.fromCss("body { color: red; }", "no-hljs")
        assertThat(theme.colorMap).isEmpty()
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
        assertThat(theme.colorMap).hasSize(3)
        assertThat(theme.colorMap["hljs-keyword"]!!.color).isEqualTo(Color.Blue)
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
        assertThat(theme.backgroundColor).isEqualTo(Color.Red)
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
        assertThat(theme.defaultTextColor).isEqualTo(Color.Green)
    }

    @Test
    fun `fromColorMap defensive copy prevents external mutation`() {
        val mutable = mutableMapOf("hljs-keyword" to SpanStyle(color = Color.Blue))
        val theme = HighlightTheme.fromColorMap("copy-test", mutable)
        // Mutate the original map after theme creation
        mutable["hljs-keyword"] = SpanStyle(color = Color.Red)
        // Theme should still have the original color
        assertThat(theme.colorMap["hljs-keyword"]?.color).isEqualTo(Color.Blue)
    }

    @Test
    fun `fromColorMap with empty map has empty colorMap`() {
        val theme = HighlightTheme.fromColorMap("empty", emptyMap())
        assertThat(theme.colorMap).isEmpty()
    }

    // ── backgroundColor / defaultTextColor ───────────────────────────────────

    @Test
    fun `backgroundColor is derived from hljs base rule`() {
        val theme = HighlightTheme.fromCss(sampleCss, "test")
        assertThat(theme.backgroundColor).isEqualTo(Color(0xFFffffff.toInt()))
    }

    @Test
    fun `defaultTextColor is derived from hljs base rule`() {
        val theme = HighlightTheme.fromCss(sampleCss, "test")
        assertThat(theme.defaultTextColor).isEqualTo(Color(0xFF4d4d4c.toInt()))
    }

    @Test
    fun `backgroundColor is Unspecified when hljs rule has no background`() {
        val theme = HighlightTheme.fromCss(".hljs-keyword{color:#8959a8}", "no-bg")
        assertThat(theme.backgroundColor).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `defaultTextColor is Unspecified when hljs rule has no color`() {
        val theme = HighlightTheme.fromCss(".hljs{background:#ffffff}", "no-text")
        assertThat(theme.defaultTextColor).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `backgroundColor from fromColorMap without hljs entry is Unspecified`() {
        val theme =
            HighlightTheme.fromColorMap(
                name = "no-hljs",
                colorMap = mapOf("hljs-keyword" to SpanStyle(color = Color.Blue)),
            )
        assertThat(theme.backgroundColor).isEqualTo(Color.Unspecified)
    }

    // ── colorMap lazy initialization ──────────────────────────────────────────

    @Test
    fun `colorMap returns same instance on repeated access`() {
        val theme = HighlightTheme.fromCss(sampleCss, "lazy-test")
        val first = theme.colorMap
        val second = theme.colorMap
        assertThat(first).isSameInstanceAs(second)
    }

    // ── equals / hashCode / toString ─────────────────────────────────────────

    @Test
    fun `themes with same name are equal`() {
        val a = HighlightTheme.fromCss(sampleCss, "same")
        val b = HighlightTheme.fromColorMap("same", emptyMap())
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `themes with different names are not equal`() {
        val a = HighlightTheme.fromCss(sampleCss, "alpha")
        val b = HighlightTheme.fromCss(sampleCss, "beta")
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `themes with same name have same hashCode`() {
        val a = HighlightTheme.fromCss(sampleCss, "same")
        val b = HighlightTheme.fromColorMap("same", emptyMap())
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `toString contains the theme name`() {
        val theme = HighlightTheme.fromCss(sampleCss, "my-theme")
        assertThat(theme.toString()).contains("my-theme")
    }

    // ── name property ─────────────────────────────────────────────────────────

    @Test
    fun `name property returns the value passed to factory`() {
        val theme = HighlightTheme.fromCss(sampleCss, "expected-name")
        assertThat(theme.name).isEqualTo("expected-name")
    }

    // ── fromColorMap with bold FontWeight entry ───────────────────────────────

    @Test
    fun `fromColorMap preserves FontWeight in SpanStyle`() {
        val map = mapOf("hljs-strong" to SpanStyle(fontWeight = FontWeight.Bold, color = Color.Yellow))
        val theme = HighlightTheme.fromColorMap("bold-test", map)
        assertThat(theme.colorMap["hljs-strong"]?.fontWeight).isEqualTo(FontWeight.Bold)
    }

    // ── theme not equal to non-HighlightTheme ─────────────────────────────────

    @Test
    fun `theme is not equal to non-HighlightTheme object`() {
        val theme = HighlightTheme.fromCss(sampleCss, "test")
        assertThat(theme).isNotEqualTo("test")
        assertThat(theme).isNotEqualTo(null)
    }

    // ── fromCss colorMap entries match ThemeParser directly ──────────────────

    @Test
    fun `fromCss colorMap matches direct ThemeParser output`() {
        val expected = ThemeParser.parse(sampleCss)
        val theme = HighlightTheme.fromCss(sampleCss, "match-test")
        assertThat(theme.colorMap).isEqualTo(expected)
    }

    // ── hljs keyword absent gives null entry ─────────────────────────────────

    @Test
    fun `colorMap returns null for unknown class`() {
        val theme = HighlightTheme.fromCss(sampleCss, "unknown")
        assertThat(theme.colorMap["hljs-does-not-exist"]).isNull()
    }

    // ── timedColorMap concurrent access ──────────────────────────────────────

    @Test
    fun `timedColorMap reports non-zero duration at most once under concurrent access`() =
        runBlocking {
            val theme = HighlightTheme.fromCss(sampleCss, "concurrency-test")
            // Launch 10 concurrent timedColorMap calls on a thread pool.
            val results =
                (1..10)
                    .map {
                        async(Dispatchers.Default) { theme.timedColorMap() }
                    }.awaitAll()
            val nonZeroCount = results.count { (_, duration) -> duration != Duration.ZERO }
            // At most one caller should report the real parse duration.
            assertThat(nonZeroCount).isAtMost(1)
        }

    @Test
    fun `timedColorMap returns Duration ZERO on every call after first initialization`() {
        val theme = HighlightTheme.fromCss(sampleCss, "zero-after-first")
        // Trigger initialization
        theme.timedColorMap()
        // All subsequent calls must return Duration.ZERO
        repeat(5) {
            val (_, duration) = theme.timedColorMap()
            assertThat(duration).isEqualTo(Duration.ZERO)
        }
    }

    @Test
    fun `timedColorMap returns Duration ZERO when colorMap was initialized incidentally`() {
        val theme = HighlightTheme.fromCss(sampleCss, "incidental-init")
        // Simulate incidental access from UI composition before HighlightEngine calls timedColorMap().
        theme.backgroundColor

        val (_, duration) = theme.timedColorMap()

        assertThat(duration).isEqualTo(Duration.ZERO)
    }
}
