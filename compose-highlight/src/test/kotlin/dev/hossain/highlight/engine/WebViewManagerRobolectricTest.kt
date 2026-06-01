package dev.hossain.highlight.engine

import android.content.Context
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.internal.WebViewManager
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

/**
 * Robolectric tests for [WebViewManager] initialization error handling.
 *
 * Uses a custom Robolectric shadow ([ThrowingWebViewShadow]) to simulate a device where
 * [WebView] construction fails (e.g. Android Go, MDM-disabled WebView, mid-update window).
 *
 * Verifies that [HighlightEngine.initialize] returns
 * [HighlightException.WebViewInitFailed] - not [HighlightException.JsExecutionFailed] - when
 * the [WebView] constructor throws a [RuntimeException].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], shadows = [WebViewManagerRobolectricTest.ThrowingWebViewShadow::class])
class WebViewManagerRobolectricTest {
    /**
     * Custom Robolectric shadow that replaces [WebView] construction with a
     * [RuntimeException], simulating an unavailable WebView component.
     *
     * `__constructor__` is the Robolectric convention for shadowing a constructor - it is
     * called by Robolectric's bytecode interception layer in place of the real
     * [WebView]`(Context)` constructor.
     */
    @Implements(WebView::class)
    @Suppress("ktlint:standard:function-naming")
    class ThrowingWebViewShadow {
        @Implementation
        fun __constructor__(context: Context): Unit = throw RuntimeException("WebView is not available on this device")
    }

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        // Replace Dispatchers.Main with an unconfined test dispatcher so that
        // withContext(Dispatchers.Main) in WebViewManager.initialize() runs eagerly
        // without needing to advance the Robolectric main looper manually.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when WebView constructor throws, initialize returns WebViewInitFailed not JsExecutionFailed`() =
        runTest {
            val engine = HighlightEngine(context)
            val result = engine.initialize()
            engine.destroy()

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull())
                .isNotInstanceOf(HighlightException.JsExecutionFailed::class.java)
            assertThat(result.exceptionOrNull())
                .isInstanceOf(HighlightException.WebViewInitFailed::class.java)
        }

    @Test
    fun `when WebView constructor throws, WebViewInitFailed wraps the original RuntimeException`() =
        runTest {
            val engine = HighlightEngine(context)
            val result = engine.initialize()
            engine.destroy()

            val ex = result.exceptionOrNull()
            assertThat(ex).isInstanceOf(HighlightException.WebViewInitFailed::class.java)
            assertThat(ex?.cause).isInstanceOf(RuntimeException::class.java)
        }

    @Test
    fun `when WebView constructor throws, highlightToHtml also returns WebViewInitFailed`() =
        runTest {
            val engine = HighlightEngine(context)
            val result = engine.highlightToHtml("val x = 42", "kotlin")
            engine.destroy()

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull())
                .isInstanceOf(HighlightException.WebViewInitFailed::class.java)
        }
}
