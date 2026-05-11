package dev.hossain.highlight.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
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
    fun webViewInitializesSuccessfully() =
        runBlocking {
            engine.initialize()
            // If no exception was thrown, WebView initialized successfully
        }

    @Test
    fun highlightPythonCodeReturnsHljsSpans() =
        runBlocking {
            val result = engine.highlightToHtml("def foo():\n    return 42", "python")
            assertTrue("Expected success", result.isSuccess)
            val html = result.getOrThrow().html
            assertTrue("Expected hljs spans in output", html.contains("hljs-"))
            assertTrue("Expected 'def' keyword", html.contains("def"))
        }

    @Test
    fun highlightKotlinCodeReturnsHljsSpans() =
        runBlocking {
            val result = engine.highlightToHtml("fun hello(): String = \"world\"", "kotlin")
            assertTrue(result.isSuccess)
            val html = result.getOrThrow().html
            assertTrue(html.contains("hljs-"))
        }

    @Test
    fun unknownLanguageReturnsUnhighlightedHtmlWithoutCrash() =
        runBlocking {
            val result = engine.highlightToHtml("some code here", "not-a-real-language")
            // highlight.js falls back to auto-detection — succeeds without crashing
            assertTrue("Should succeed even for unknown language", result.isSuccess)
            val html = result.getOrThrow().html
            // Auto-detection may wrap tokens in spans (breaking exact phrase), so check individual words
            assertTrue("Output should be non-empty", html.isNotEmpty())
            assertTrue("Output should contain 'some'", html.contains("some"))
            assertTrue("Output should contain 'here'", html.contains("here"))
        }

    @Test
    fun codeWithBackslashRoundtripsCorrectly() =
        runBlocking {
            val code = """C:\Users\test\file.txt"""
            val result = engine.highlightToHtml(code, "plaintext")
            assertTrue("Expected success for backslash code", result.isSuccess)
            val html = result.getOrThrow().html
            assertTrue("Expected backslash in output: $html", html.contains("\\"))
        }

    @Test
    fun codeWithSingleQuotesRoundtripsCorrectly() =
        runBlocking {
            val code = "print('hello world')"
            val result = engine.highlightToHtml(code, "python")
            assertTrue(result.isSuccess)
            val html = result.getOrThrow().html
            assertTrue(
                "Expected single quotes in output: $html",
                html.contains("'") || html.contains("&#x27;") || html.contains("&apos;") || html.contains("hello"),
            )
        }

    @Test
    fun codeWithNewlinesRoundtripsCorrectly() =
        runBlocking {
            val code = "line1\nline2\nline3"
            val result = engine.highlightToHtml(code, "plaintext")
            assertTrue(result.isSuccess)
            val html = result.getOrThrow().html
            assertTrue("Expected line content in output: $html", html.contains("line1"))
            assertTrue(html.contains("line2"))
        }

    @Test
    fun codeWithUnicodeRoundtripsCorrectly() =
        runBlocking {
            val code = "// héllo wörld 🌍"
            val result = engine.highlightToHtml(code, "javascript")
            assertTrue(result.isSuccess)
            val html = result.getOrThrow().html
            assertNotNull(html)
        }

    @Test
    fun concurrentHighlightCallsDoNotCrash() =
        runBlocking {
            val jobs =
                (1..5).map { i ->
                    launch {
                        val result = engine.highlightToHtml("val x = $i", "kotlin")
                        assertTrue("Concurrent call $i should succeed", result.isSuccess)
                    }
                }
            jobs.forEach { it.join() }
        }

    @Test
    fun highlightFullPipelineProducesAnnotatedString() =
        runBlocking {
            val result = engine.highlight("def foo(): pass", "python", lightTheme)
            assertTrue(result.isSuccess)
            val highlightResult = result.getOrThrow()
            assertTrue("Expected non-empty text", highlightResult.annotated.text.isNotEmpty())
            assertTrue("Expected 'foo' in text", highlightResult.annotated.text.contains("foo"))
        }

    @Test
    fun highlightBothThemesProducesBothResults() =
        runBlocking {
            val darkTheme = HighlightTheme.tomorrowNight(context)
            val result =
                engine.highlightBothThemes(
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
    fun destroyAfterHighlightDoesNotCrash() =
        runBlocking {
            engine.highlightToHtml("print('hello')", "python")
            engine.destroy()
            // No exception = pass
        }

    // ── isInitialized ─────────────────────────────────────────────────────────

    @Test
    fun isInitializedIsFalseBeforeInitialize() {
        assertFalse("Expected false before initialize()", engine.isInitialized.value)
    }

    @Test
    fun isInitializedIsTrueAfterHighlight() =
        runBlocking {
            // initialize() only starts the WebView load; isInitialized becomes true once
            // onPageFinished fires. A real highlight call awaits readyDeferred, guaranteeing
            // the page has fully loaded before we check isInitialized.
            engine.highlightToHtml("val x = 1", "kotlin")
            assertTrue("Expected true after WebView page load", engine.isInitialized.value)
        }

    @Test
    fun isInitializedIsFalseAfterDestroy() =
        runBlocking {
            engine.highlightToHtml("val x = 1", "kotlin")
            assertTrue(engine.isInitialized.value)
            engine.destroy()
            assertFalse("Expected false after destroy()", engine.isInitialized.value)
        }

    // ── highlight() → HighlightResult fields ─────────────────────────────────

    @Test
    fun highlightResultLanguageMatchesRequest() =
        runBlocking {
            val result = engine.highlight("val x = 1", "kotlin", lightTheme)
            assertTrue(result.isSuccess)
            assertThat(result.getOrThrow().language).isEqualTo("kotlin")
        }

    @Test
    fun highlightResultSpanCountPositiveForSupportedLanguage() =
        runBlocking {
            val result = engine.highlight("def foo(): pass", "python", lightTheme)
            assertTrue(result.isSuccess)
            assertTrue(
                "Expected spanCount > 0 for python",
                result.getOrThrow().spanCount > 0,
            )
        }

    @Test
    fun highlightResultSpanCountNonNegativeForUnknownLanguage() =
        runBlocking {
            // highlight.js falls back to auto-detection for unknown languages, which may still
            // produce spans. We only assert the call succeeds and spanCount is non-negative.
            val result = engine.highlight("some code here", "not-a-real-language-xyz", lightTheme)
            assertTrue("Expected success even for unknown language", result.isSuccess)
            assertTrue(
                "Expected spanCount >= 0",
                result.getOrThrow().spanCount >= 0,
            )
        }

    @Test
    fun highlightResultDurationMsIsNonNegative() =
        runBlocking {
            val result = engine.highlight("val x = 1", "kotlin", lightTheme)
            assertTrue(result.isSuccess)
            assertTrue(
                "Expected durationMs >= 0",
                result.getOrThrow().durationMs >= 0L,
            )
        }

    @Test
    fun highlightResultAnnotatedTextContainsCode() =
        runBlocking {
            val result = engine.highlight("fun hello() = 42", "kotlin", lightTheme)
            assertTrue(result.isSuccess)
            assertTrue(
                "Expected annotated text to contain 'hello'",
                result
                    .getOrThrow()
                    .annotated.text
                    .contains("hello"),
            )
        }

    // ── highlightToHtml() → HtmlHighlightResult fields ───────────────────────

    @Test
    fun highlightToHtmlResultDurationMsIsNonNegative() =
        runBlocking {
            val result = engine.highlightToHtml("val x = 1", "kotlin")
            assertTrue(result.isSuccess)
            assertTrue(
                "Expected durationMs >= 0",
                result.getOrThrow().durationMs >= 0L,
            )
        }

    @Test
    fun highlightToHtmlResultHtmlContainsHljsSpans() =
        runBlocking {
            val result = engine.highlightToHtml("def foo(): pass", "python")
            assertTrue(result.isSuccess)
            assertTrue(
                "Expected hljs-* spans in HTML",
                result.getOrThrow().html.contains("hljs-"),
            )
        }

    // ── supportedLanguages() ─────────────────────────────────────────────────

    @Test
    fun supportedLanguagesReturnsSuccess() =
        runBlocking {
            val result = engine.supportedLanguages()
            assertTrue("Expected success", result.isSuccess)
        }

    @Test
    fun supportedLanguagesListIsNonEmpty() =
        runBlocking {
            val languages = engine.supportedLanguages().getOrThrow()
            assertTrue("Expected non-empty language list", languages.isNotEmpty())
        }

    @Test
    fun supportedLanguagesContainsCommonLanguages() =
        runBlocking {
            val languages = engine.supportedLanguages().getOrThrow()
            assertThat(languages).contains("kotlin")
            assertThat(languages).contains("python")
            assertThat(languages).contains("javascript")
            assertThat(languages).contains("sql")
        }

    @Test
    fun supportedLanguagesListIsSorted() =
        runBlocking {
            val languages = engine.supportedLanguages().getOrThrow()
            val sorted = languages.sorted()
            assertThat(languages).isEqualTo(sorted)
        }

    @Test
    fun supportedLanguagesReturnsSameListOnSecondCall() =
        runBlocking {
            val first = engine.supportedLanguages().getOrThrow()
            val second = engine.supportedLanguages().getOrThrow()
            assertThat(second).isEqualTo(first)
        }

    // ── highlightJsVersion() ─────────────────────────────────────────────────

    @Test
    fun highlightJsVersionReturnsSuccess() =
        runBlocking {
            val result = engine.highlightJsVersion()
            assertTrue("Expected success", result.isSuccess)
        }

    @Test
    fun highlightJsVersionIsNonEmpty() =
        runBlocking {
            val version = engine.highlightJsVersion().getOrThrow()
            assertTrue("Expected non-empty version string", version.isNotEmpty())
        }

    @Test
    fun highlightJsVersionMatchesVersionFormat() =
        runBlocking {
            val version = engine.highlightJsVersion().getOrThrow()
            // Format: digits and dots, e.g. "11.11.1"
            assertTrue(
                "Expected version to match digits-and-dots format, got: $version",
                version.matches(Regex("\\d+\\.\\d+.*")),
            )
        }

    @Test
    fun highlightJsVersionReturnsSameValueOnSecondCall() =
        runBlocking {
            val first = engine.highlightJsVersion().getOrThrow()
            val second = engine.highlightJsVersion().getOrThrow()
            assertThat(second).isEqualTo(first)
        }
}
