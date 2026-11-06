package dev.hossain.highlight.engine.internal

import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.ByteArrayInputStream

/**
 * Robolectric tests for [WebViewManager] bounded bridge initialization and error propagation (#486).
 *
 * Verifies that:
 * - Unresponsive page loads time out cleanly in [WebViewManager.getReadyWebView].
 * - Main-frame and critical subresource failures immediately fail pending waiters with [HighlightException.WebViewInitFailed].
 * - Non-critical subresource failures do not abort initialization.
 * - Failed initialization can be cleanly retried on subsequent calls.
 * - Concurrent callers observe identical results and do not duplicate WebViews.
 * - [HighlightEngine] propagates timeouts and page-load errors without deadlocking the mutex.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class WebViewManagerReadinessTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createMockRequest(
        url: String,
        isMainFrame: Boolean,
    ): WebResourceRequest =
        object : WebResourceRequest {
            override fun getUrl(): Uri = Uri.parse(url)

            override fun isForMainFrame(): Boolean = isMainFrame

            override fun isRedirect(): Boolean = false

            override fun hasGesture(): Boolean = false

            override fun getMethod(): String = "GET"

            override fun getRequestHeaders(): Map<String, String> = emptyMap()
        }

    private fun createMockError(
        errorCode: Int,
        description: String,
    ): WebResourceError = android.webkit.TestWebResourceError(errorCode, description)

    private fun rootCause(throwable: Throwable?): Throwable? = generateSequence(throwable) { it.cause }.find { it !is HighlightException }

    @Test
    fun `getReadyWebView times out when onPageFinished is never called`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            // onPageFinished is never dispatched - timeout should fail fast and clean up
            val failure = runCatching { manager.getReadyWebView(timeoutMs = 50L) }.exceptionOrNull()

            assertThat(failure).isInstanceOf(HighlightException.Timeout::class.java)
            assertThat(manager.isInitialized.value).isFalse()
            assertThat(manager.webViewForTest()).isNull()

            manager.destroy()
        }

    @Test
    fun `onReceivedError for main-frame bridge html fails pending waiters immediately`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient

            val request = createMockRequest(BRIDGE_URL, isMainFrame = true)
            val error = createMockError(-2, "net::ERR_NAME_NOT_RESOLVED")

            client?.onReceivedError(webView, request, error)
            ShadowLooper.idleMainLooper()

            assertThat(manager.isInitialized.value).isFalse()
            assertThat(manager.webViewForTest()).isNull()

            val failure = runCatching { manager.getReadyWebView() }.exceptionOrNull()
            assertThat(failure).isInstanceOf(HighlightException.WebViewInitFailed::class.java)
            assertThat(rootCause(failure)?.message).contains("Failed to load bridge asset")

            manager.destroy()
        }

    @Test
    fun `onReceivedError for critical subresource highlight js fails pending waiters`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient

            val request =
                createMockRequest(
                    "https://$ASSET_DOMAIN$HIGHLIGHT_JS_PATH",
                    isMainFrame = false,
                )
            val error = createMockError(-1, "net::ERR_FAILED")

            client?.onReceivedError(webView, request, error)
            ShadowLooper.idleMainLooper()

            assertThat(manager.isInitialized.value).isFalse()
            assertThat(manager.webViewForTest()).isNull()

            val failure = runCatching { manager.getReadyWebView() }.exceptionOrNull()
            assertThat(failure).isInstanceOf(HighlightException.WebViewInitFailed::class.java)

            manager.destroy()
        }

    @Test
    fun `onReceivedHttpError for bridge html fails pending waiters`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient

            val request = createMockRequest(BRIDGE_URL, isMainFrame = true)
            val response =
                WebResourceResponse(
                    "text/html",
                    "utf-8",
                    404,
                    "Not Found",
                    emptyMap(),
                    ByteArrayInputStream(ByteArray(0)),
                )

            client?.onReceivedHttpError(webView, request, response)
            ShadowLooper.idleMainLooper()

            assertThat(manager.isInitialized.value).isFalse()
            assertThat(manager.webViewForTest()).isNull()

            val failure = runCatching { manager.getReadyWebView() }.exceptionOrNull()
            assertThat(failure).isInstanceOf(HighlightException.WebViewInitFailed::class.java)
            assertThat(rootCause(failure)?.message).contains("404 Not Found")

            manager.destroy()
        }

    @Test
    fun `non-critical subresource error does not abort bridge initialization`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient

            // Simulate 404 on an optional resource (e.g., favicon)
            val request = createMockRequest("https://$ASSET_DOMAIN/favicon.ico", isMainFrame = false)
            val response =
                WebResourceResponse(
                    "image/x-icon",
                    "utf-8",
                    404,
                    "Not Found",
                    emptyMap(),
                    ByteArrayInputStream(ByteArray(0)),
                )

            client?.onReceivedHttpError(webView, request, response)
            ShadowLooper.idleMainLooper()

            // Manager should still be initializing, not failed
            assertThat(manager.webViewForTest()).isNotNull()
            assertThat(manager.isInitialized.value).isFalse()

            // When bridge.html finishes loading, manager becomes ready
            client?.onPageFinished(webView, BRIDGE_URL)
            ShadowLooper.idleMainLooper()

            assertThat(manager.isInitialized.value).isTrue()
            val readyWebView = manager.getReadyWebView()
            assertThat(readyWebView).isSameInstanceAs(webView)

            manager.destroy()
        }

    @Test
    fun `initialization can be retried cleanly after a failure`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val firstWebView = manager.webViewForTest() ?: error("Initial WebView not created")
            val firstClient = Shadows.shadowOf(firstWebView).webViewClient

            // Fail the first attempt via onReceivedError
            val request = createMockRequest(BRIDGE_URL, isMainFrame = true)
            firstClient?.onReceivedError(firstWebView, request, createMockError(-2, "Network fail"))
            ShadowLooper.idleMainLooper()

            assertThat(manager.webViewForTest()).isNull()
            assertThat(manager.isInitialized.value).isFalse()

            // Re-initialize: should create a new WebView and fresh deferred
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val secondWebView = manager.webViewForTest() ?: error("Second WebView not created")
            assertThat(secondWebView).isNotSameInstanceAs(firstWebView)

            val secondClient = Shadows.shadowOf(secondWebView).webViewClient
            secondClient?.onPageFinished(secondWebView, BRIDGE_URL)
            ShadowLooper.idleMainLooper()

            assertThat(manager.isInitialized.value).isTrue()
            val readyWebView = manager.getReadyWebView()
            assertThat(readyWebView).isSameInstanceAs(secondWebView)

            manager.destroy()
        }

    @Test
    fun `initialization can be retried cleanly after a timeout`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val firstWebView = manager.webViewForTest() ?: error("Initial WebView not created")

            // Timeout the first attempt
            val timeoutFailure = runCatching { manager.getReadyWebView(timeoutMs = 30L) }.exceptionOrNull()
            assertThat(timeoutFailure).isInstanceOf(HighlightException.Timeout::class.java)

            assertThat(manager.webViewForTest()).isNull()
            assertThat(manager.isInitialized.value).isFalse()

            // Late onPageFinished against the dead first WebView must not revive it
            val firstClient = Shadows.shadowOf(firstWebView).webViewClient
            firstClient?.onPageFinished(firstWebView, BRIDGE_URL)
            ShadowLooper.idleMainLooper()
            assertThat(manager.isInitialized.value).isFalse()

            // Second initialization attempt
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val secondWebView = manager.webViewForTest() ?: error("Second WebView not created")
            assertThat(secondWebView).isNotSameInstanceAs(firstWebView)

            val secondClient = Shadows.shadowOf(secondWebView).webViewClient
            secondClient?.onPageFinished(secondWebView, BRIDGE_URL)
            ShadowLooper.idleMainLooper()

            assertThat(manager.isInitialized.value).isTrue()
            val ready = manager.getReadyWebView()
            assertThat(ready).isSameInstanceAs(secondWebView)

            manager.destroy()
        }

    @Test
    fun `concurrent getReadyWebView callers observe identical failure on error`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView not created")
            val client = Shadows.shadowOf(webView).webViewClient

            coroutineScope {
                val waiters = (1..3).map { async { runCatching { manager.getReadyWebView() } } }

                val request = createMockRequest(BRIDGE_URL, isMainFrame = true)
                client?.onReceivedError(webView, request, createMockError(-2, "Failed"))
                ShadowLooper.idleMainLooper()

                val results = waiters.awaitAll()
                assertThat(results).hasSize(3)
                results.forEach { result ->
                    assertThat(result.isFailure).isTrue()
                    assertThat(result.exceptionOrNull())
                        .isInstanceOf(HighlightException.WebViewInitFailed::class.java)
                }
            }

            manager.destroy()
        }

    @Test
    fun `HighlightEngine highlightToHtml times out and releases mutex when bridge load hangs`() =
        runTest {
            val engine = HighlightEngine(context)

            // Do not call onPageFinished - the initialize/getReadyWebView call inside highlightToHtml
            // will suspend until the timeout expires.
            // Using a custom short timeout is verified directly on WebViewManager; here we verify
            // that HighlightEngine catches timeout cleanly and returns Result.failure(Timeout).
            val managerField = HighlightEngine::class.java.getDeclaredField("manager")
            managerField.isAccessible = true
            val manager = managerField.get(engine) as WebViewManager

            manager.initialize()
            ShadowLooper.idleMainLooper()

            val failure = runCatching { manager.getReadyWebView(timeoutMs = 50L) }.exceptionOrNull()
            assertThat(failure).isInstanceOf(HighlightException.Timeout::class.java)

            // Subsequent highlightToHtml call fails without deadlock
            val result = engine.highlightToHtml("val x = 1", "kotlin")
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(HighlightException::class.java)

            engine.destroy()
        }
}
