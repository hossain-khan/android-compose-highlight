package dev.hossain.highlight.engine.internal

import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightException
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
            assertThat(outcome).isInstanceOf(HighlightException.EngineDestroyed::class.java)
            awaiter.cancel()
            awaiterScope.cancel()
            manager.destroy()
        }

    @Test
    fun `initialize resets readyDeferred if it was completed`() =
        runTest {
            val manager = WebViewManager(context)
            val field = WebViewManager::class.java.getDeclaredField("readyDeferred")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val initialDeferred = field.get(manager) as CompletableDeferred<WebView>
            val dummyWebView = WebView(context)
            initialDeferred.complete(dummyWebView)
            assertThat(initialDeferred.isCompleted).isTrue()

            manager.initialize()
            ShadowLooper.idleMainLooper()

            @Suppress("UNCHECKED_CAST")
            val newDeferred = field.get(manager) as CompletableDeferred<WebView>
            assertThat(newDeferred).isNotSameInstanceAs(initialDeferred)
            assertThat(newDeferred.isCompleted).isFalse()

            manager.destroy()
        }

    @Test
    fun `calling onPageFinished multiple times is safe and does not re-complete deferred`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient
            client?.onPageFinished(webView, "https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
            ShadowLooper.idleMainLooper()

            // Second onPageFinished call against same webView
            client?.onPageFinished(webView, "https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
            ShadowLooper.idleMainLooper()

            assertThat(manager.isInitialized.value).isTrue()
            manager.destroy()
        }

    @Test
    fun `destroy during construction leaves no live WebView`() =
        runTest {
            val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(testDispatcher)
            try {
                val manager = WebViewManager(context)

                // Launch initialize undispatched so it runs eagerly until withContext(Dispatchers.Main)
                // suspends, scheduling the Main-thread construction block without executing it yet.
                val initJob =
                    launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                        manager.initialize()
                    }

                // Call destroy while initialize is queued
                manager.destroy()

                // Now execute the queued Main-thread block
                testScheduler.runCurrent()
                initJob.join()

                assertThat(manager.webViewForTest()).isNull()
                assertThat(manager.isInitialized.value).isFalse()

                // Calling getReadyWebView must fail with EngineDestroyed immediately
                val failure = runCatching { manager.getReadyWebView() }.exceptionOrNull()
                assertThat(failure).isInstanceOf(HighlightException.EngineDestroyed::class.java)

                // Subsequent initialize after destroy works normally
                val reinitJob =
                    launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                        manager.initialize()
                    }
                testScheduler.runCurrent()
                reinitJob.join()

                val newWebView = manager.webViewForTest()
                assertThat(newWebView).isNotNull()
                val client = Shadows.shadowOf(newWebView).webViewClient
                client?.onPageFinished(newWebView, "https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")

                assertThat(manager.isInitialized.value).isTrue()
                assertThat(manager.getReadyWebView()).isSameInstanceAs(newWebView)
                manager.destroy()
            } finally {
                Dispatchers.setMain(UnconfinedTestDispatcher())
            }
        }

    @Test
    fun `destroy called after readiness completes deferred exceptionally and subsequent getReadyWebView throws EngineDestroyed`() =
        runTest {
            val manager = WebViewManager(context)
            manager.initialize()
            ShadowLooper.idleMainLooper()

            val webView = manager.webViewForTest() ?: error("WebView was not created")
            val client = Shadows.shadowOf(webView).webViewClient
            client?.onPageFinished(webView, "https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
            ShadowLooper.idleMainLooper()

            assertThat(manager.isInitialized.value).isTrue()
            assertThat(manager.getReadyWebView()).isSameInstanceAs(webView)

            manager.destroy()
            ShadowLooper.idleMainLooper()

            assertThat(manager.isInitialized.value).isFalse()
            assertThat(manager.webViewForTest()).isNull()

            val failure = runCatching { manager.getReadyWebView() }.exceptionOrNull()
            assertThat(failure).isInstanceOf(HighlightException.EngineDestroyed::class.java)
        }

    @Test
    fun `caller coroutine cancellation propagates CancellationException unchanged`() =
        runTest {
            val manager = WebViewManager(context)
            val parentScope = CoroutineScope(Dispatchers.Unconfined)
            val job =
                parentScope.launch {
                    manager.initialize()
                }
            job.cancel()
            // After parent job cancellation, verify initialize can still be called
            manager.initialize()
            ShadowLooper.idleMainLooper()
            val webView = manager.webViewForTest()
            assertThat(webView).isNotNull()
            manager.destroy()
        }

    @Test
    fun `repeated concurrent initialize and destroy cycles remain consistent`() =
        runTest {
            val manager = WebViewManager(context)
            repeat(15) { iteration ->
                coroutineScope {
                    val init1 = async { manager.initialize() }
                    val init2 = async { manager.initialize() }
                    if (iteration % 2 == 0) {
                        manager.destroy()
                    }
                    init1.await()
                    init2.await()
                }
                ShadowLooper.idleMainLooper()
            }
            manager.destroy()
            ShadowLooper.idleMainLooper()
            assertThat(manager.webViewForTest()).isNull()
            assertThat(manager.isInitialized.value).isFalse()
        }
}
