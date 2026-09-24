package dev.hossain.highlight.engine.internal

import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Robolectric tests for security hardening in [WebViewManager].
 *
 * Verifies that:
 * 1. WebSettings have file/content access disabled, network loads blocked, and safe browsing disabled.
 * 2. WebViewClient blocks unexpected navigation via shouldOverrideUrlLoading.
 * 3. WebViewClient rejects unexpected origins/schemes with HTTP 403 Forbidden in shouldInterceptRequest.
 * 4. WebViewClient accepts valid appassets requests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class WebViewManagerSecurityTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createFakeRequest(url: Uri): WebResourceRequest =
        object : WebResourceRequest {
            override fun getUrl(): Uri = url

            override fun isForMainFrame(): Boolean = false

            override fun isRedirect(): Boolean = false

            override fun hasGesture(): Boolean = false

            override fun getMethod(): String = "GET"

            override fun getRequestHeaders(): Map<String, String> = emptyMap()
        }

    @Test
    fun `WebView settings are hardened against file access and network loads`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val settings = webView.settings

            assertThat(settings.javaScriptEnabled).isTrue()
            assertThat(settings.allowFileAccess).isFalse()
            assertThat(settings.allowContentAccess).isFalse()
            assertThat(settings.blockNetworkLoads).isTrue()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertThat(settings.safeBrowsingEnabled).isFalse()
            }

            manager.destroy()
        }

    @Test
    fun `shouldOverrideUrlLoading rejects any page navigation`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient ?: error("WebViewClient was not set")

            val externalRequest = createFakeRequest(Uri.parse("https://example.com/page"))
            val shouldOverride = client.shouldOverrideUrlLoading(webView, externalRequest)

            assertThat(shouldOverride).isTrue()

            manager.destroy()
        }

    @Test
    fun `shouldInterceptRequest rejects unexpected host with 403 Forbidden`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient ?: error("WebViewClient was not set")

            val maliciousRequest = createFakeRequest(Uri.parse("https://evil.com/script.js"))
            val response = client.shouldInterceptRequest(webView, maliciousRequest)

            assertThat(response).isNotNull()
            assertThat(response?.statusCode).isEqualTo(403)
            assertThat(response?.reasonPhrase).isEqualTo("Forbidden")

            manager.destroy()
        }

    @Test
    fun `shouldInterceptRequest rejects non-https scheme with 403 Forbidden`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient ?: error("WebViewClient was not set")

            val httpRequest =
                createFakeRequest(Uri.parse("http://appassets.androidplatform.net/assets/compose-highlight/bridge.html"))
            val response = client.shouldInterceptRequest(webView, httpRequest)

            assertThat(response).isNotNull()
            assertThat(response?.statusCode).isEqualTo(403)
            assertThat(response?.reasonPhrase).isEqualTo("Forbidden")

            manager.destroy()
        }

    @Test
    fun `shouldInterceptRequest delegates valid appassets request to assetLoader`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient ?: error("WebViewClient was not set")

            val validRequest =
                createFakeRequest(Uri.parse("https://appassets.androidplatform.net/assets/compose-highlight/bridge.html"))
            val response = client.shouldInterceptRequest(webView, validRequest)

            assertThat(response).isNotNull()
            assertThat(response?.data).isNotNull()

            manager.destroy()
        }
}
