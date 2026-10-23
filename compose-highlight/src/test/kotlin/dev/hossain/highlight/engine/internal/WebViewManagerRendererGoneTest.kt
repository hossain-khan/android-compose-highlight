package dev.hossain.highlight.engine.internal

import android.content.Context
import android.webkit.RenderProcessGoneDetail
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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

/**
 * Robolectric tests for [WebViewManager] renderer process termination handling ([android.webkit.WebViewClient.onRenderProcessGone]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class WebViewManagerRendererGoneTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Suppress("DEPRECATION")
    private fun createDetail(didCrash: Boolean): RenderProcessGoneDetail =
        object : RenderProcessGoneDetail() {
            override fun didCrash(): Boolean = didCrash

            override fun rendererPriorityAtExit(): Int = 0
        }

    @Test
    fun `onRenderProcessGone returns true and resets isInitialized to false`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient ?: error("WebViewClient is null")

            client.onPageFinished(webView, "https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
            ShadowLooper.idleMainLooper()
            assertThat(manager.isInitialized.value).isTrue()

            val handled = client.onRenderProcessGone(webView, createDetail(didCrash = false))
            assertThat(handled).isTrue()
            assertThat(manager.isInitialized.value).isFalse()
            assertThat(manager.webViewForTest()).isNull()

            manager.destroy()
        }

    @Test
    fun `onRenderProcessGone fails pending readyDeferred with RendererGone when page load was in flight`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient ?: error("WebViewClient is null")

            val readyDeferred =
                async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    try {
                        manager.getReadyWebView()
                        null
                    } catch (e: HighlightException.RendererGone) {
                        e
                    }
                }

            val handled = client.onRenderProcessGone(webView, createDetail(didCrash = true))
            assertThat(handled).isTrue()

            val exception = readyDeferred.await()
            assertThat(exception).isNotNull()
            assertThat(exception?.didCrash).isTrue()

            manager.destroy()
        }

    @Test
    fun `onRenderProcessGone notifies onRendererGone callback`() =
        runTest {
            val manager = WebViewManager(context)
            var reportedCrash: Boolean? = null
            manager.onRendererGone = { didCrash ->
                reportedCrash = didCrash
            }

            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient ?: error("WebViewClient is null")

            client.onRenderProcessGone(webView, createDetail(didCrash = true))
            assertThat(reportedCrash).isTrue()

            manager.destroy()
        }

    @Test
    fun `subsequent initialize after onRenderProcessGone creates a fresh WebView and succeeds`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val firstWebView = manager.webViewForTest() ?: error("Initial WebView was not created")
            val firstClient = Shadows.shadowOf(firstWebView).webViewClient ?: error("WebViewClient is null")

            firstClient.onPageFinished(firstWebView, "https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
            ShadowLooper.idleMainLooper()
            assertThat(manager.isInitialized.value).isTrue()

            // Simulate renderer termination
            firstClient.onRenderProcessGone(firstWebView, createDetail(didCrash = false))
            assertThat(manager.webViewForTest()).isNull()
            assertThat(manager.isInitialized.value).isFalse()

            // Re-initialize
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val secondWebView = manager.webViewForTest() ?: error("Re-initialized WebView was not created")
            assertThat(secondWebView).isNotSameInstanceAs(firstWebView)

            val secondClient = Shadows.shadowOf(secondWebView).webViewClient ?: error("Second WebViewClient is null")
            secondClient.onPageFinished(secondWebView, "https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
            ShadowLooper.idleMainLooper()

            assertThat(manager.isInitialized.value).isTrue()
            assertThat(manager.getReadyWebView()).isSameInstanceAs(secondWebView)

            manager.destroy()
        }

    @Test
    fun `onRenderProcessGone for unknown or previous WebView still returns true without altering active state`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val activeWebView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(activeWebView).webViewClient ?: error("WebViewClient is null")
            client.onPageFinished(activeWebView, "https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
            ShadowLooper.idleMainLooper()

            // Construct an unrelated WebView
            val staleWebView = android.webkit.WebView(context)
            val handled = client.onRenderProcessGone(staleWebView, createDetail(didCrash = true))

            assertThat(handled).isTrue()
            assertThat(manager.isInitialized.value).isTrue()
            assertThat(manager.webViewForTest()).isSameInstanceAs(activeWebView)

            manager.destroy()
        }
}
