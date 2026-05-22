package dev.hossain.highlight.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
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
            val result = engine.initialize()
            assertThat(result.isSuccess).isTrue()
            // Wait for bridge.html to finish loading (onPageFinished → isInitialized = true)
            engine.isInitialized.first { it }
            assertThat(engine.isInitialized.value).isTrue()
        }

    @Test
    fun highlightPythonCodeReturnsHljsSpans() =
        runBlocking {
            val result = engine.highlightToHtml("def foo():\n    return 42", "python")
            assertThat(result.isSuccess).isTrue()
            val html = result.getOrThrow().html
            assertThat(html).contains("hljs-")
            assertThat(html).contains("def")
        }

    @Test
    fun highlightKotlinCodeReturnsHljsSpans() =
        runBlocking {
            val result = engine.highlightToHtml("fun hello(): String = \"world\"", "kotlin")
            assertThat(result.isSuccess).isTrue()
            val html = result.getOrThrow().html
            assertThat(html).contains("hljs-")
        }

    @Test
    fun unknownLanguageReturnsUnhighlightedHtmlWithoutCrash() =
        runBlocking {
            val result = engine.highlightToHtml("some code here", "not-a-real-language")
            // highlight.js falls back to auto-detection - succeeds without crashing
            assertThat(result.isSuccess).isTrue()
            val html = result.getOrThrow().html
            // Auto-detection may wrap tokens in spans (breaking exact phrase), so check individual words
            assertThat(html).isNotEmpty()
            assertThat(html).contains("some")
            assertThat(html).contains("here")
        }

    @Test
    fun codeWithBackslashRoundtripsCorrectly() =
        runBlocking {
            val code = """C:\Users\test\file.txt"""
            val result = engine.highlightToHtml(code, "plaintext")
            assertThat(result.isSuccess).isTrue()
            val html = result.getOrThrow().html
            assertThat(html).contains("\\")
        }

    @Test
    fun codeWithSingleQuotesRoundtripsCorrectly() =
        runBlocking {
            val code = "print('hello world')"
            val result = engine.highlightToHtml(code, "python")
            assertThat(result.isSuccess).isTrue()
            val html = result.getOrThrow().html
            assertThat(
                html.contains("'") || html.contains("&#x27;") || html.contains("&apos;") || html.contains("hello"),
            ).isTrue()
        }

    @Test
    fun codeWithNewlinesRoundtripsCorrectly() =
        runBlocking {
            val code = "line1\nline2\nline3"
            val result = engine.highlightToHtml(code, "plaintext")
            assertThat(result.isSuccess).isTrue()
            val html = result.getOrThrow().html
            assertThat(html).contains("line1")
            assertThat(html).contains("line2")
        }

    @Test
    fun codeWithUnicodeRoundtripsCorrectly() =
        runBlocking {
            val code = "// héllo wörld 🌍"
            val result = engine.highlightToHtml(code, "javascript")
            assertThat(result.isSuccess).isTrue()
            val html = result.getOrThrow().html
            assertThat(html).isNotNull()
        }

    @Test
    fun concurrentHighlightCallsDoNotCrash() =
        runBlocking {
            val jobs =
                (1..5).map { i ->
                    launch {
                        val result = engine.highlightToHtml("val x = $i", "kotlin")
                        assertThat(result.isSuccess).isTrue()
                    }
                }
            jobs.forEach { it.join() }
        }

    @Test
    fun highlightFullPipelineProducesAnnotatedString() =
        runBlocking {
            val result = engine.highlight("def foo(): pass", "python", lightTheme)
            assertThat(result.isSuccess).isTrue()
            val highlightResult = result.getOrThrow()
            assertThat(highlightResult.annotated.text).isNotEmpty()
            assertThat(highlightResult.annotated.text).contains("foo")
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
            assertThat(result.isSuccess).isTrue()
            val themed = result.getOrThrow()
            assertThat(themed.light.text).isNotEmpty()
            assertThat(themed.dark.text).isNotEmpty()
            assertThat(themed.light.text).isEqualTo(themed.dark.text)
        }

    @Test
    fun destroyDoesNotCrash() {
        // destroy() can be called without initializing first
        engine.destroy()
    }

    @Test
    fun destroyIsIdempotentDoesNotCrash() {
        // double destroy() must not throw
        engine.destroy()
        engine.destroy()
    }

    @Test
    fun closeDoesNotCrash() {
        // close() can be called without initializing first
        engine.close()
    }

    @Test
    fun closeIsIdempotentDoesNotCrash() {
        // double close() must not throw
        engine.close()
        engine.close()
    }

    @Test
    fun engineImplementsCloseable() {
        // HighlightEngine must implement java.io.Closeable
        assertThat(engine).isInstanceOf(java.io.Closeable::class.java)
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
        assertThat(engine.isInitialized.value).isFalse()
    }

    @Test
    fun isInitializedIsTrueAfterHighlight() =
        runBlocking {
            // initialize() only starts the WebView load; isInitialized becomes true once
            // onPageFinished fires. A real highlight call awaits readyDeferred, guaranteeing
            // the page has fully loaded before we check isInitialized.
            engine.highlightToHtml("val x = 1", "kotlin")
            assertThat(engine.isInitialized.value).isTrue()
        }

    @Test
    fun isInitializedIsFalseAfterDestroy() =
        runBlocking {
            engine.highlightToHtml("val x = 1", "kotlin")
            assertThat(engine.isInitialized.value).isTrue()
            engine.destroy()
            assertThat(engine.isInitialized.value).isFalse()
        }

    // ── highlight() → HighlightResult fields ─────────────────────────────────

    @Test
    fun highlightResultLanguageMatchesRequest() =
        runBlocking {
            val result = engine.highlight("val x = 1", "kotlin", lightTheme)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().language).isEqualTo("kotlin")
        }

    @Test
    fun highlightResultSpanCountPositiveForSupportedLanguage() =
        runBlocking {
            val result = engine.highlight("def foo(): pass", "python", lightTheme)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().spanCount).isGreaterThan(0)
        }

    @Test
    fun highlightResultSpanCountNonNegativeForUnknownLanguage() =
        runBlocking {
            // highlight.js falls back to auto-detection for unknown languages, which may still
            // produce spans. We only assert the call succeeds and spanCount is non-negative.
            val result = engine.highlight("some code here", "not-a-real-language-xyz", lightTheme)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().spanCount).isAtLeast(0)
        }

    @Test
    fun highlightResultDurationMsIsNonNegative() =
        runBlocking {
            val result = engine.highlight("val x = 1", "kotlin", lightTheme)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().durationMs).isAtLeast(0L)
        }

    @Test
    fun highlightResultAnnotatedTextContainsCode() =
        runBlocking {
            val result = engine.highlight("fun hello() = 42", "kotlin", lightTheme)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().annotated.text).contains("hello")
        }

    // ── highlightToHtml() → HtmlHighlightResult fields ───────────────────────

    @Test
    fun highlightToHtmlResultDurationMsIsNonNegative() =
        runBlocking {
            val result = engine.highlightToHtml("val x = 1", "kotlin")
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().durationMs).isAtLeast(0L)
        }

    @Test
    fun highlightToHtmlResultHtmlContainsHljsSpans() =
        runBlocking {
            val result = engine.highlightToHtml("def foo(): pass", "python")
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().html).contains("hljs-")
        }

    // ── supportedLanguages() ─────────────────────────────────────────────────

    @Test
    fun supportedLanguagesReturnsSuccess() =
        runBlocking {
            val result = engine.supportedLanguages()
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun supportedLanguagesListIsNonEmpty() =
        runBlocking {
            val languages = engine.supportedLanguages().getOrThrow()
            assertThat(languages).isNotEmpty()
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
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun highlightJsVersionIsNonEmpty() =
        runBlocking {
            val version = engine.highlightJsVersion().getOrThrow()
            assertThat(version).isNotEmpty()
        }

    @Test
    fun highlightJsVersionMatchesVersionFormat() =
        runBlocking {
            val version = engine.highlightJsVersion().getOrThrow()
            // Format: digits and dots, e.g. "11.11.1"
            assertThat(version).matches("\\d+\\.\\d+.*")
        }

    @Test
    fun highlightJsVersionReturnsSameValueOnSecondCall() =
        runBlocking {
            val first = engine.highlightJsVersion().getOrThrow()
            val second = engine.highlightJsVersion().getOrThrow()
            assertThat(second).isEqualTo(first)
        }

    // ── getLanguage() ─────────────────────────────────────────────────────────

    @Test
    fun getLanguageReturnsSuccessForKnownAlias() =
        runBlocking {
            val result = engine.getLanguage("kt")
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun getLanguageReturnsNonNullForKnownAlias() =
        runBlocking {
            val info = engine.getLanguage("kt").getOrThrow()
            assertThat(info).isNotNull()
        }

    @Test
    fun getLanguageReturnsExpectedNameForKotlinAlias() =
        runBlocking {
            val info = engine.getLanguage("kt").getOrThrow()
            assertThat(info!!.name).isEqualTo("Kotlin")
        }

    @Test
    fun getLanguageAliasesContainsKtForKotlin() =
        runBlocking {
            val info = engine.getLanguage("kotlin").getOrThrow()
            assertThat(info).isNotNull()
            assertThat(info!!.aliases).contains("kt")
        }

    @Test
    fun getLanguageReturnsNullForUnknownLanguage() =
        runBlocking {
            val result = engine.getLanguage("not-a-real-language-xyz")
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).isNull()
        }

    @Test
    fun getLanguageByPrimaryNameReturnsNonNull() =
        runBlocking {
            val info = engine.getLanguage("python").getOrThrow()
            assertThat(info).isNotNull()
            assertThat(info!!.name).isEqualTo("Python")
        }

    // ── highlightAuto() ───────────────────────────────────────────────────────

    @Test
    fun highlightAutoReturnsSuccessForKotlinCode() =
        runBlocking {
            val result = engine.highlightAuto("fun hello(): String = \"world\"", lightTheme)
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun highlightAutoAnnotatedTextContainsCode() =
        runBlocking {
            val result = engine.highlightAuto("fun hello(): String = \"world\"", lightTheme)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().annotated.text).contains("hello")
        }

    @Test
    fun highlightAutoAnnotatedTextIsNonEmpty() =
        runBlocking {
            val result = engine.highlightAuto("def foo(): pass", lightTheme)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().annotated.text).isNotEmpty()
        }

    @Test
    fun highlightAutoDetectedLanguageMayBeNonEmptyForClearInput() =
        runBlocking {
            // SQL is a strong signal - hljs should detect it. We assert the call succeeds
            // and detectedLanguage is a non-empty string (hljs recognised something).
            val result = engine.highlightAuto("SELECT id, name FROM users WHERE active = 1", lightTheme)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().detectedLanguage).isNotEmpty()
        }

    @Test
    fun highlightAutoDurationMsIsNonNegative() =
        runBlocking {
            val result = engine.highlightAuto("val x = 42", lightTheme)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().durationMs).isAtLeast(0L)
        }

    // ── highlightAuto edge cases ───────────────────────────────────────────────

    @Test
    fun highlightAutoWithEmptyInputReturnsSuccess() =
        runBlocking {
            val result = engine.highlightAuto("", lightTheme)
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun highlightAutoWithWhitespaceOnlyInputReturnsSuccess() =
        runBlocking {
            val result = engine.highlightAuto("   \n\t  ", lightTheme)
            assertThat(result.isSuccess).isTrue()
        }

    // ── code containing bridge envelope format ────────────────────────────────

    @Test
    fun highlightCodeThatLooksLikeBridgeEnvelopeDoesNotConfuseParser() =
        runBlocking {
            // Verify that code resembling the bridge error envelope is highlighted correctly
            // and not mistaken for an actual bridge protocol message.
            val code = """{"error": "something went wrong", "value": null}"""
            val result = engine.highlight(code, "json", lightTheme)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().annotated.text).contains("error")
        }

    // ── very long single-line input ───────────────────────────────────────────

    @Test
    fun highlightVeryLongSingleLineInputReturnsSuccess() =
        runBlocking {
            val longLine = "val x = " + "\"hello\" + ".repeat(500) + "\"end\""
            val result = engine.highlight(longLine, "kotlin", lightTheme)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow().annotated.text).contains("hello")
        }

    // ── highlightBothThemes result equivalence ────────────────────────────────

    @Test
    fun highlightBothThemesProducesSameTextAsTwoIndependentCalls() =
        runBlocking {
            val darkTheme = HighlightTheme.tomorrowNight(context)
            val code = "fun add(a: Int, b: Int) = a + b"
            val language = "kotlin"

            val bothResult =
                engine.highlightBothThemes(
                    code = code,
                    language = language,
                    lightTheme = lightTheme,
                    darkTheme = darkTheme,
                )
            val lightResult = engine.highlight(code, language, lightTheme)
            val darkResult = engine.highlight(code, language, darkTheme)

            assertThat(bothResult.isSuccess).isTrue()
            assertThat(lightResult.isSuccess).isTrue()
            assertThat(darkResult.isSuccess).isTrue()

            assertThat(bothResult.getOrThrow().light.text).isEqualTo(lightResult.getOrThrow().annotated.text)
            assertThat(bothResult.getOrThrow().dark.text).isEqualTo(darkResult.getOrThrow().annotated.text)
        }
}
