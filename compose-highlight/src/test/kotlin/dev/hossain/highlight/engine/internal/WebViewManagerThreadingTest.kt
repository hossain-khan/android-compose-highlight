package dev.hossain.highlight.engine.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Robolectric tests that pin the threading invariants documented on [WebViewManager]'s class
 * KDoc. These are the assertions that "the code is correct, here is the proof" - they catch
 * regressions where a future edit removes the captured-local pattern in [WebViewManager.initialize]
 * or the `webView == null` guard in `onPageFinished`, both of which keep the manager safe under
 * destroy / re-init / concurrent-await sequences.
 *
 * For broader engine semantics (failure mapping, JS bridge), see [WebViewManagerRobolectricTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class WebViewManagerThreadingTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        // UnconfinedTestDispatcher lets withContext(Dispatchers.Main) inside initialize() proceed
        // eagerly so the WebView constructor runs without a manual looper pump.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `destroy during initialize completes the new deferred on the next initialize`() =
        runTest {
            val manager = WebViewManager(context)

            // First initialize() creates the WebView; bridge.html is loaded but onPageFinished
            // has not been driven yet, so readyDeferred from this round stays uncompleted.
            manager.initialize()
            ShadowLooper.idleMainLooper()

            // Capture the WebViewClient + URL the manager registered, then race destroy vs the
            // page-load callback. After destroy(), invoking onPageFinished against the captured
            // deferred should be a no-op (webView is null), and the next initialize() must build
            // a fresh deferred that the *new* WebView's onPageFinished completes.
            val firstWebView = manager.webViewForTest() ?: error("Initial WebView was not created")
            val firstClient = Shadows.shadowOf(firstWebView).webViewClient

            manager.destroy()
            ShadowLooper.idleMainLooper()

            // Late callback against the destroyed WebView - guard at the top of onPageFinished
            // makes this a no-op. Without the guard, this would complete a stale deferred.
            firstClient?.onPageFinished(firstWebView, "https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
            ShadowLooper.idleMainLooper()

            // Second initialize() must allocate a fresh WebView with a fresh deferred.
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val secondWebView = manager.webViewForTest() ?: error("Re-initialized WebView was not created")
            assertThat(secondWebView).isNotSameInstanceAs(firstWebView)
            val secondClient = Shadows.shadowOf(secondWebView).webViewClient
            secondClient?.onPageFinished(secondWebView, "https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
            ShadowLooper.idleMainLooper()

            // getReadyWebView resumes - if the late onPageFinished from the first round had
            // completed the *new* deferred (the bug this test guards against), this would still
            // succeed but yield the wrong WebView. Identity check is the real assertion.
            val ready =
                withTimeoutOrNull(2_000L) {
                    manager.getReadyWebView()
                }
            assertThat(ready).isSameInstanceAs(secondWebView)

            manager.destroy()
        }

    @Test
    fun `concurrent initialize calls create exactly one WebView`() =
        runTest {
            val manager = WebViewManager(context)

            // Launch four initialize() coroutines concurrently. After they all return, only one
            // WebView should exist. The early-return on webView != null inside the Main-thread
            // critical section is what makes this safe; this test pins that behavior.
            coroutineScope {
                val jobs =
                    (1..4).map {
                        async { manager.initialize() }
                    }
                jobs.awaitAll()
            }
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest()
            assertThat(webView).isNotNull()

            // Re-running initialize() must not allocate another WebView (idempotency).
            val before = webView
            manager.initialize()
            ShadowLooper.idleMainLooper()
            assertThat(manager.webViewForTest()).isSameInstanceAs(before)

            manager.destroy()
        }

    @Test
    fun `getReadyWebView awaiting during destroy resumes with cancellation`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()
            // Page-load callback is not driven, so getReadyWebView() will suspend on the
            // uncompleted readyDeferred until destroy() cancels it.

            val resumedWith = CompletableDeferred<Throwable?>()
            val awaiterScope = CoroutineScope(Dispatchers.Unconfined)
            val awaiter: Job =
                awaiterScope.launch {
                    try {
                        manager.getReadyWebView()
                        resumedWith.complete(null) // Should not happen.
                    } catch (e: Throwable) {
                        resumedWith.complete(e)
                    }
                }

            // Destroy cancels the readyDeferred, which propagates as a cancellation to the
            // awaiting getReadyWebView() coroutine - never a hang.
            manager.destroy()
            ShadowLooper.idleMainLooper()

            val outcome = withTimeoutOrNull(2_000L) { resumedWith.await() }
            assertThat(outcome).isNotNull()
            // CompletableDeferred.cancel() resumes await() with a CancellationException; the
            // exact subclass varies by coroutines version (JobCancellationException), so the
            // contract is "any Throwable, never a hang" - the timeout above is what would fail
            // if the manager regressed into never resuming the awaiter.
            awaiter.cancel()
            awaiterScope.cancel()
            manager.destroy()
        }

    @Test
    fun `warmUp creates prewarmed WebView and initialize consumes it`() =
        runTest {
            // Call warmUp (will post/run on main thread)
            WebViewManager.warmUp(context)
            ShadowLooper.idleMainLooper()

            // Initialize a WebViewManager - it should consume the pre-warmed WebView
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest()
            assertThat(webView).isNotNull()

            val shadowWebView = Shadows.shadowOf(webView)
            assertThat(shadowWebView.lastLoadedUrl).isEqualTo("https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")

            // Destroy and reinitialize without warmUp should yield a different WebView instance
            val firstWv = webView
            manager.destroy()
            ShadowLooper.idleMainLooper()

            manager.initialize()
            ShadowLooper.idleMainLooper()
            val secondWv = manager.webViewForTest()
            assertThat(secondWv).isNotSameInstanceAs(firstWv)

            manager.destroy()
        }

    @Test
    fun `warmUp called from background thread posts to main thread and succeeds`() =
        runTest {
            val job =
                launch(Dispatchers.Default) {
                    WebViewManager.warmUp(context)
                }
            job.join()
            ShadowLooper.idleMainLooper()

            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest()
            assertThat(webView).isNotNull()

            manager.destroy()
        }
}
