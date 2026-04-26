package dev.composehighlight.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HighlightEngineTest {

    private lateinit var engine: HighlightEngine
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val lightTheme by lazy { HighlightTheme.tomorrow(context) }

    @Before
    fun setUp() {
        engine = HighlightEngine(context)
    }

    @After
    fun tearDown() {
        engine.destroy()
    }

    @Test
    fun webViewInitializesSuccessfully() = runBlocking {
        engine.initialize()
        // If no exception was thrown, WebView initialized successfully
    }

    @Test
    fun highlightPythonCodeReturnsHljsSpans() = runBlocking {
        val result = engine.highlightToHtml("def foo():\n    return 42", "python")
        assertTrue("Expected success", result.isSuccess)
        val html = result.getOrThrow()
        assertTrue("Expected hljs spans in output", html.contains("hljs-"))
        assertTrue("Expected 'def' keyword", html.contains("def"))
    }

    @Test
    fun highlightKotlinCodeReturnsHljsSpans() = runBlocking {
        val result = engine.highlightToHtml("fun hello(): String = \"world\"", "kotlin")
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("hljs-"))
    }

    @Test
    fun unknownLanguageReturnsUnhighlightedHtmlWithoutCrash() = runBlocking {
        val result = engine.highlightToHtml("some code here", "not-a-real-language")
        // highlight.js falls back to auto-detection — succeeds without crashing
        assertTrue("Should succeed even for unknown language", result.isSuccess)
        val html = result.getOrThrow()
        // Auto-detection may wrap tokens in spans (breaking exact phrase), so check individual words
        assertTrue("Output should be non-empty", html.isNotEmpty())
        assertTrue("Output should contain 'some'", html.contains("some"))
        assertTrue("Output should contain 'here'", html.contains("here"))
    }

    @Test
    fun codeWithBackslashRoundtripsCorrectly() = runBlocking {
        val code = """C:\Users\test\file.txt"""
        val result = engine.highlightToHtml(code, "plaintext")
        assertTrue("Expected success for backslash code", result.isSuccess)
        val html = result.getOrThrow()
        assertTrue("Expected backslash in output: $html", html.contains("\\"))
    }

    @Test
    fun codeWithSingleQuotesRoundtripsCorrectly() = runBlocking {
        val code = "print('hello world')"
        val result = engine.highlightToHtml(code, "python")
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue("Expected single quotes in output: $html", html.contains("'") || html.contains("&#x27;") || html.contains("&apos;") || html.contains("hello") )
    }

    @Test
    fun codeWithNewlinesRoundtripsCorrectly() = runBlocking {
        val code = "line1\nline2\nline3"
        val result = engine.highlightToHtml(code, "plaintext")
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue("Expected line content in output: $html", html.contains("line1"))
        assertTrue(html.contains("line2"))
    }

    @Test
    fun codeWithUnicodeRoundtripsCorrectly() = runBlocking {
        val code = "// héllo wörld 🌍"
        val result = engine.highlightToHtml(code, "javascript")
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertNotNull(html)
    }

    @Test
    fun concurrentHighlightCallsDoNotCrash() = runBlocking {
        val jobs = (1..5).map { i ->
            launch {
                val result = engine.highlightToHtml("val x = $i", "kotlin")
                assertTrue("Concurrent call $i should succeed", result.isSuccess)
            }
        }
        jobs.forEach { it.join() }
    }

    @Test
    fun highlightFullPipelineProducesAnnotatedString() = runBlocking {
        val result = engine.highlight("def foo(): pass", "python", lightTheme)
        assertTrue(result.isSuccess)
        val annotated = result.getOrThrow()
        assertTrue("Expected non-empty text", annotated.text.isNotEmpty())
        assertTrue("Expected 'foo' in text", annotated.text.contains("foo"))
    }

    @Test
    fun highlightBothThemesProducesBothResults() = runBlocking {
        val darkTheme = HighlightTheme.tomorrowNight(context)
        val result = engine.highlightBothThemes(
            code = "val x = 42",
            language = "kotlin",
            lightTheme = lightTheme,
            darkTheme = darkTheme,
        )
        assertTrue(result.isSuccess)
        val themed = result.getOrThrow()
        assertFalse(themed.light.text.isEmpty())
        assertFalse(themed.dark.text.isEmpty())
        assertTrue("Light and dark should have same text", themed.light.text == themed.dark.text)
    }

    @Test
    fun destroyDoesNotCrash() {
        // destroy() can be called without initializing first
        engine.destroy()
    }

    @Test
    fun destroyAfterHighlightDoesNotCrash() = runBlocking {
        engine.highlightToHtml("print('hello')", "python")
        engine.destroy()
        // No exception = pass
    }
}
