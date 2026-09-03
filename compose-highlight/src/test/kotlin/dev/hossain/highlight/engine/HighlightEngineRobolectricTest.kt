package dev.hossain.highlight.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class HighlightEngineRobolectricTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun createReadyEngine(): HighlightEngine {
        val engine = HighlightEngine(context)
        engine.initialize()
        ShadowLooper.idleMainLooper()

        val webView = engine.webViewForTest() ?: error("WebView was not created")
        val client = Shadows.shadowOf(webView).webViewClient
        client?.onPageFinished(webView, "https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
        ShadowLooper.idleMainLooper()
        return engine
    }

    private fun TestScope.respondToJs(
        engine: HighlightEngine,
        responseJson: String?,
    ) {
        testScheduler.runCurrent()
        ShadowLooper.idleMainLooper()
        val webView = engine.webViewForTest() ?: error("WebView not found")
        val shadow = Shadows.shadowOf(webView)
        val callback = shadow.lastEvaluatedJavascriptCallback ?: error("lastEvaluatedJavascriptCallback is null")
        callback.onReceiveValue(responseJson)
        ShadowLooper.idleMainLooper()
        testScheduler.runCurrent()
    }

    @Test
    fun `initialize warms up webView and sets isInitialized to true`() =
        runTest {
            val engine = HighlightEngine(context)
            assertThat(engine.isInitialized.value).isFalse()

            engine.initialize()
            ShadowLooper.idleMainLooper()

            val webView = engine.webViewForTest() ?: error("WebView not created")
            val client = Shadows.shadowOf(webView).webViewClient
            client?.onPageFinished(webView, "https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
            ShadowLooper.idleMainLooper()

            assertThat(engine.isInitialized.value).isTrue()
            engine.destroy()
        }

    @Test
    fun `highlightToHtml returns valid HtmlHighlightResult on successful JS evaluation`() =
        runTest {
            val engine = createReadyEngine()
            val sampleCode = "val x = 42"
            val expectedHtml = """<span class="hljs-keyword">val</span> x = <span class="hljs-number">42</span>"""
            val innerJson =
                JSONObject()
                    .apply {
                        put("html", expectedHtml)
                        put("relevance", 10)
                    }.toString()
            val rawResult = JSONObject.quote(innerJson)

            val deferred = async { engine.highlightToHtml(sampleCode, "kotlin") }
            respondToJs(engine, rawResult)

            val result = deferred.await()
            assertThat(result.isSuccess).isTrue()
            val htmlResult = result.getOrThrow()
            assertThat(htmlResult.html).isEqualTo(expectedHtml)
            assertThat(htmlResult.jsBridgeDuration).isAtLeast(kotlin.time.Duration.ZERO)

            engine.destroy()
        }

    @Test
    fun `highlightToHtml returns JsExecutionFailed when JS returns error JSON`() =
        runTest {
            val engine = createReadyEngine()
            val innerJson =
                JSONObject()
                    .apply {
                        put("error", true)
                        put("message", "Unknown language")
                    }.toString()
            val rawResult = JSONObject.quote(innerJson)

            val deferred = async { engine.highlightToHtml("val x = 42", "unknown_lang") }
            respondToJs(engine, rawResult)

            val result = deferred.await()
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(HighlightException.JsExecutionFailed::class.java)

            engine.destroy()
        }

    @Test
    fun `highlightToHtml returns JsExecutionFailed when JS evaluation returns null`() =
        runTest {
            val engine = createReadyEngine()

            val deferred = async { engine.highlightToHtml("val x = 42", "kotlin") }
            respondToJs(engine, "null")

            val result = deferred.await()
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(HighlightException.JsExecutionFailed::class.java)

            engine.destroy()
        }

    @Test
    fun `highlight returns AnnotatedString with theme styles applied`() =
        runTest {
            val engine = createReadyEngine()
            val sampleCode = "val x = 42"
            val expectedHtml = """<span class="hljs-keyword">val</span> x = 42"""
            val innerJson =
                JSONObject()
                    .apply {
                        put("html", expectedHtml)
                        put("relevance", 5)
                    }.toString()
            val rawResult = JSONObject.quote(innerJson)
            val theme = HighlightTheme.tomorrow()

            val deferred = async { engine.highlight(sampleCode, "kotlin", theme) }
            respondToJs(engine, rawResult)

            val result = deferred.await()
            assertThat(result.isSuccess).isTrue()
            val highlightResult = result.getOrThrow()
            assertThat(highlightResult.annotated.text).isEqualTo("val x = 42")
            assertThat(highlightResult.spanCount).isGreaterThan(0)

            engine.destroy()
        }

    @Test
    fun `highlightBothThemes produces both light and dark AnnotatedStrings`() =
        runTest {
            val engine = createReadyEngine()
            val sampleCode = "val x = 42"
            val expectedHtml = """<span class="hljs-keyword">val</span> x = 42"""
            val innerJson =
                JSONObject()
                    .apply {
                        put("html", expectedHtml)
                        put("relevance", 5)
                    }.toString()
            val rawResult = JSONObject.quote(innerJson)

            val deferred =
                async {
                    engine.highlightBothThemes(
                        code = sampleCode,
                        language = "kotlin",
                        lightTheme = HighlightTheme.tomorrow(),
                        darkTheme = HighlightTheme.tomorrowNight(),
                    )
                }
            respondToJs(engine, rawResult)

            val result = deferred.await()
            assertThat(result.isSuccess).isTrue()
            val both = result.getOrThrow()
            assertThat(both.light.text).isEqualTo("val x = 42")
            assertThat(both.dark.text).isEqualTo("val x = 42")

            engine.destroy()
        }

    @Test
    fun `highlightAuto detects language and returns AutoHighlightResult`() =
        runTest {
            val engine = createReadyEngine()
            val sampleCode = "def hello(): pass"
            val expectedHtml =
                """<span class="hljs-keyword">def</span> <span class="hljs-title function_">hello</span>(): <span class="hljs-keyword">pass</span>"""
            val innerJson =
                JSONObject()
                    .apply {
                        put("html", expectedHtml)
                        put("language", "python")
                        put("relevance", 7)
                    }.toString()
            val rawResult = JSONObject.quote(innerJson)

            val deferred = async { engine.highlightAuto(sampleCode, HighlightTheme.tomorrow()) }
            respondToJs(engine, rawResult)

            val result = deferred.await()
            assertThat(result.isSuccess).isTrue()
            val autoResult = result.getOrThrow()
            assertThat(autoResult.detectedLanguage).isEqualTo("python")
            assertThat(autoResult.annotated.text).isEqualTo("def hello(): pass")

            engine.destroy()
        }

    @Test
    fun `highlightAuto returns JsExecutionFailed when JS returns error JSON`() =
        runTest {
            val engine = createReadyEngine()
            val innerJson =
                JSONObject()
                    .apply {
                        put("error", true)
                        put("message", "Auto highlight failed")
                    }.toString()
            val rawResult = JSONObject.quote(innerJson)

            val deferred = async { engine.highlightAuto("test code", HighlightTheme.tomorrow()) }
            respondToJs(engine, rawResult)

            val result = deferred.await()
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(HighlightException.JsExecutionFailed::class.java)

            engine.destroy()
        }

    @Test
    fun `highlightAuto returns JsExecutionFailed when JS returns null`() =
        runTest {
            val engine = createReadyEngine()

            val deferred = async { engine.highlightAuto("test code", HighlightTheme.tomorrow()) }
            respondToJs(engine, "null")

            val result = deferred.await()
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(HighlightException.JsExecutionFailed::class.java)

            engine.destroy()
        }

    @Test
    fun `supportedLanguages parses array and caches result`() =
        runTest {
            val engine = createReadyEngine()
            val languagesJson = """["kotlin","python","javascript","rust","swift"]"""

            val deferred1 = async { engine.supportedLanguages() }
            respondToJs(engine, languagesJson)

            val result1 = deferred1.await()
            assertThat(result1.isSuccess).isTrue()
            assertThat(result1.getOrThrow()).containsExactly("javascript", "kotlin", "python", "rust", "swift")

            // Second call should return cached list immediately without invoking JS evaluation
            val result2 = engine.supportedLanguages()
            assertThat(result2.isSuccess).isTrue()
            assertThat(result2.getOrThrow()).containsExactly("javascript", "kotlin", "python", "rust", "swift")

            engine.destroy()
        }

    @Test
    fun `supportedLanguages returns JsExecutionFailed when JS returns null`() =
        runTest {
            val engine = createReadyEngine()

            val deferred = async { engine.supportedLanguages() }
            respondToJs(engine, "null")

            val result = deferred.await()
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(HighlightException.JsExecutionFailed::class.java)

            engine.destroy()
        }

    @Test
    fun `getLanguage parses LanguageInfo when language is registered`() =
        runTest {
            val engine = createReadyEngine()
            val innerJson =
                JSONObject()
                    .apply {
                        put("name", "Kotlin")
                        put("aliases", org.json.JSONArray(listOf("kt", "kts")))
                    }.toString()
            val rawResult = JSONObject.quote(innerJson)

            val deferred = async { engine.getLanguage("kotlin") }
            respondToJs(engine, rawResult)

            val result = deferred.await()
            assertThat(result.isSuccess).isTrue()
            val info = result.getOrThrow()
            assertThat(info).isNotNull()
            assertThat(info?.name).isEqualTo("Kotlin")
            assertThat(info?.aliases).containsExactly("kt", "kts")

            engine.destroy()
        }

    @Test
    fun `getLanguage returns null when language is not found`() =
        runTest {
            val engine = createReadyEngine()

            val deferred = async { engine.getLanguage("nonexistent_lang") }
            respondToJs(engine, "null")

            val result = deferred.await()
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).isNull()

            engine.destroy()
        }

    @Test
    fun `getLanguage returns JsExecutionFailed when JS returns error JSON`() =
        runTest {
            val engine = createReadyEngine()
            val innerJson =
                JSONObject()
                    .apply {
                        put("error", true)
                        put("message", "Error in getLanguage")
                    }.toString()
            val rawResult = JSONObject.quote(innerJson)

            val deferred = async { engine.getLanguage("some_lang") }
            respondToJs(engine, rawResult)

            val result = deferred.await()
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(HighlightException.JsExecutionFailed::class.java)

            engine.destroy()
        }

    @Test
    fun `highlightJsVersion returns version and caches value`() =
        runTest {
            val engine = createReadyEngine()
            val rawResult = JSONObject.quote("11.12.0")

            val deferred1 = async { engine.highlightJsVersion() }
            respondToJs(engine, rawResult)

            val result1 = deferred1.await()
            assertThat(result1.isSuccess).isTrue()
            assertThat(result1.getOrThrow()).isEqualTo("11.12.0")

            // Second call uses cached version
            val result2 = engine.highlightJsVersion()
            assertThat(result2.isSuccess).isTrue()
            assertThat(result2.getOrThrow()).isEqualTo("11.12.0")

            engine.destroy()
        }

    @Test
    fun `highlightJsVersion returns JsExecutionFailed when JS returns null`() =
        runTest {
            val engine = createReadyEngine()

            val deferred = async { engine.highlightJsVersion() }
            respondToJs(engine, "null")

            val result = deferred.await()
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(HighlightException.JsExecutionFailed::class.java)

            engine.destroy()
        }

    @Test
    fun `destroy and close release resources safely`() =
        runTest {
            val engine = createReadyEngine()
            assertThat(engine.isInitialized.value).isTrue()

            engine.close()
            assertThat(engine.isInitialized.value).isFalse()

            // Calling close again is a no-op
            engine.close()
            engine.destroy()
        }
}
