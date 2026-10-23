package dev.hossain.highlight.engine.internal

import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightException
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowWebView

/**
 * Verifies that failures during [WebView.loadUrl] in [WebViewManager.initialize]
 * are cleanly handled: wrapped into [HighlightException.WebViewInitFailed],
 * failing pending waiters without hanging, and discarding the unusable [WebView].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], shadows = [WebViewManagerLoadUrlFailureTest.ThrowingLoadUrlShadow::class])
class WebViewManagerLoadUrlFailureTest {
    @Implements(WebView::class)
    class ThrowingLoadUrlShadow : ShadowWebView() {
        @Implementation
        override fun loadUrl(url: String): Unit = throw RuntimeException("Simulated loadUrl failure")
    }

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when loadUrl throws, initialize throws WebViewInitFailed and resets state without hanging`() =
        runTest {
            val manager = WebViewManager(context)

            val failure = runCatching { manager.initialize() }.exceptionOrNull()
            assertThat(failure).isInstanceOf(HighlightException.WebViewInitFailed::class.java)
            assertThat(failure?.cause).hasMessageThat().isEqualTo("Simulated loadUrl failure")

            // Ensure webView reference was cleared and not leaked
            assertThat(manager.webViewForTest()).isNull()
            assertThat(manager.isInitialized.value).isFalse()

            // Ensure getReadyWebView fails immediately with the init failure instead of hanging
            val readyFailure = runCatching { manager.getReadyWebView() }.exceptionOrNull()
            assertThat(readyFailure).isInstanceOf(HighlightException.WebViewInitFailed::class.java)

            manager.destroy()
        }
}
