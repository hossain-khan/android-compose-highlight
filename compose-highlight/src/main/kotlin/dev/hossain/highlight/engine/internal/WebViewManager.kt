package dev.hossain.highlight.engine.internal

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.webkit.WebViewAssetLoader
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

/**
 * Internal manager responsible for creating and initializing the hidden WebView.
 *
 * A WebView singleton with lazy initialization,
 * loaded via [WebViewAssetLoader] using the `appassets.androidplatform.net` scheme.
 *
 * ## Why `https://appassets.androidplatform.net`?
 *
 * WebView blocks many features (including `<script>` tag execution) on `file://` URLs due to
 * the [Same-Origin Policy](https://developer.mozilla.org/en-US/docs/Web/Security/Same-origin_policy).
 * [WebViewAssetLoader] solves this by serving local app assets over a real `https://` URL.
 *
 * `appassets.androidplatform.net` is the default domain reserved by Android specifically for
 * this purpose - it is safe to use and will never conflict with a real website.
 * The [WebViewAssetLoader] intercepts requests to this domain inside [WebViewClient.shouldInterceptRequest]
 * and maps `/assets/` → the app's `assets/` folder, so no real network call is ever made.
 *
 * Official docs: https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
 *
 * ## Security hardening
 *
 * To minimize attack surface and prevent unwanted resource or network access:
 * - [android.webkit.WebSettings.setAllowFileAccess] is set to `false` to disable `file://` filesystem access.
 *   Official guidance: https://developer.android.com/privacy-and-security/risks/webview-unsafe-file-inclusion
 * - [android.webkit.WebSettings.setAllowContentAccess] is set to `false` to disable `content://` ContentProvider access.
 * - [android.webkit.WebSettings.setBlockNetworkLoads] is set to `true` to block outbound network requests.
 *   Official docs: https://developer.android.com/reference/android/webkit/WebSettings#setBlockNetworkLoads(boolean)
 * - [android.webkit.WebSettings.setSafeBrowsingEnabled] is set to `false` on API 26+ to avoid Safe Browsing
 *   initialization overhead and network telemetry on an internal, trusted asset-only WebView.
 *   Official docs: https://developer.android.com/reference/android/webkit/WebSettings#setSafeBrowsingEnabled(boolean)
 * - [WebViewClient.shouldOverrideUrlLoading] returns `true` to reject unexpected page navigation.
 *   Official docs: https://developer.android.com/reference/android/webkit/WebViewClient#shouldOverrideUrlLoading(android.webkit.WebView,%20android.webkit.WebResourceRequest)
 * - [WebViewClient.shouldInterceptRequest] rejects any request not targeting `https://appassets.androidplatform.net`
 *   with an HTTP 403 Forbidden response.
 * - `bridge.html` enforces a strict Content Security Policy (`default-src 'none'; script-src 'self' 'unsafe-inline';`).
 *
 * ## What bridge.html does
 *
 * `bridge.html` is a minimal HTML page (loaded once at startup) that:
 * 1. Loads the bundled `highlight.min.js` library
 * 2. Defines multiple highlight JS function like, `highlightCode(code, lang) → HTML string`
 *
 * After the page finishes loading, [HighlightEngine] calls `highlightCode()` via
 * [android.webkit.WebView.evaluateJavascript] for every syntax-highlight request,
 * getting back HTML with `<span class="hljs-*">` tokens that are then converted to
 * an [androidx.compose.ui.text.AnnotatedString] by [HtmlToAnnotatedString].
 *
 * ## Threading invariants and lifecycle state machine
 *
 * The [WebView] itself is not thread-safe - the platform requires construction, JS evaluation,
 * and destruction to all happen on the thread that created it. This manager pins that to the
 * Main thread.
 *
 * **State machine and generation tracking:**
 *
 * Lifecycle transitions are modeled explicitly via [State] (`IDLE`, `INITIALIZING`, `READY`,
 * `FAILED`, `DESTROYED`) and coordinated across threads using an internal synchronization lock
 * and a monotonic generation counter:
 * - `IDLE` - Initial state, or state after a renderer crash has torn down the dead WebView.
 * - `INITIALIZING` - [initialize] is constructing the [WebView] on the Main thread or loading `bridge.html`.
 * - `READY` - `bridge.html` finished loading (`onPageFinished`), and the WebView is ready for JS calls.
 * - `FAILED` - Construction or `loadUrl` threw [HighlightException.WebViewInitFailed]; retryable by subsequent [initialize] calls.
 * - `DESTROYED` - [destroy] was invoked; pending waiters fail with [HighlightException.EngineDestroyed].
 *
 * A request ticket is captured upon calling [initialize] and compared against `generation` inside
 * the Main-thread execution. If [destroy] ran while construction was queued or in progress, the
 * generation mismatch causes the newly created WebView to be discarded and destroyed immediately
 * without publishing, leaving no orphaned live instances.
 *
 * **Method dispatch:**
 * - [initialize] is `suspend` and dispatches its critical section to [Dispatchers.Main] via
 *   `withContext`. The [WebView] is constructed and `bridge.html` is loaded on Main.
 * - [destroy] runs synchronously from any thread. It updates the state machine, fails pending
 *   waiters with [HighlightException.EngineDestroyed], and posts `wv.destroy()` to Main (or
 *   invokes it directly if already on Main).
 * - [getReadyWebView] is `suspend` and awaited from any thread. If destroyed, it fails immediately
 *   with [HighlightException.EngineDestroyed] instead of hanging.
 */
internal class WebViewManager(
    private val context: Context,
) {
    private enum class State {
        IDLE,
        INITIALIZING,
        READY,
        FAILED,
        DESTROYED,
    }

    private val lock = Any()

    @Volatile
    private var state: State = State.IDLE

    @GuardedBy("lock")
    private var generation: Int = 0

    @Volatile
    private var webView: WebView? = null

    private val _isInitialized = MutableStateFlow(false)

    /**
     * `true` once the WebView has been created and bridge.html has finished loading.
     *
     * This is a [StateFlow] so that Compose callers can observe initialization reactively:
     * ```kotlin
     * val isReady by engine.isInitialized.collectAsState()
     * ```
     */
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * Mutable so it can be reset when [initialize] is called after [destroy].
     * Accessed only on the Main thread (inside [initialize]) or awaited from any thread.
     *
     * Marked `@Volatile` so that a write by [destroy] (any thread) is immediately visible to
     * [getReadyWebView] and [initialize] running on another thread (ARM weak memory model).
     */
    @Volatile
    private var readyDeferred = CompletableDeferred<WebView>()

    /**
     * Optional callback notified on the Main thread when [WebViewClient.onRenderProcessGone]
     * is triggered. [HighlightEngine] uses this to fail any in-flight JavaScript continuation
     * with [HighlightException.RendererGone] without waiting for a timeout.
     */
    internal var onRendererGone: ((didCrash: Boolean) -> Unit)? = null

    /** Returns the ready WebView. Suspends until bridge.html has finished loading. */
    suspend fun getReadyWebView(): WebView = readyDeferred.await()

    /**
     * Returns the underlying [WebView] **synchronously** for test-only inspection, or `null`
     * if [initialize] has not yet been called. Tests use this to reach into the hidden WebView
     * via Robolectric's `Shadows.shadowOf(...)` without reflection.
     *
     * Production code must not call this. Use [getReadyWebView] (suspending) instead.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    internal fun webViewForTest(): WebView? = webView

    /**
     * Creates the WebView on the Main thread and loads bridge.html.
     * Safe to call multiple times - idempotent after first call.
     * Safe to call after [destroy] - re-creates the WebView and a fresh deferred.
     */
    suspend fun initialize() {
        if (state == State.READY) return

        val requestTicket: Int
        synchronized(lock) {
            if (state == State.READY) return
            requestTicket = generation
        }

        withContext(Dispatchers.Main) {
            val targetGeneration: Int
            val deferred: CompletableDeferred<WebView>

            synchronized(lock) {
                // If destroy() was called after this initialize() call was invoked,
                // generation will be greater than requestTicket, and state will be DESTROYED.
                if (state == State.DESTROYED && generation > requestTicket) {
                    return@withContext
                }
                if (state == State.READY) return@withContext
                if (state == State.INITIALIZING && webView != null) return@withContext

                state = State.INITIALIZING
                generation++
                targetGeneration = generation
                _isInitialized.value = false

                if (readyDeferred.isCompleted) {
                    readyDeferred = CompletableDeferred()
                }
                deferred = readyDeferred
            }

            val assetLoader =
                WebViewAssetLoader
                    .Builder()
                    .setDomain("appassets.androidplatform.net")
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                    .build()

            val view =
                try {
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            // Hardened settings: disable file and content access since assets are loaded
                            // over https:// via WebViewAssetLoader.
                            // Official docs: https://developer.android.com/privacy-and-security/risks/webview-unsafe-file-inclusion
                            allowFileAccess = false
                            allowContentAccess = false
                            // Block any outbound network loads - bridge.html only needs local bundled assets.
                            // Official docs: https://developer.android.com/reference/android/webkit/WebSettings#setBlockNetworkLoads(boolean)
                            blockNetworkLoads = true
                            // Disable Safe Browsing for local, trusted asset-only WebView to avoid unnecessary
                            // initialization overhead and network lookups.
                            // Official docs: https://developer.android.com/reference/android/webkit/WebSettings#setSafeBrowsingEnabled(boolean)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = false
                            }
                        }
                        webViewClient =
                            object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): Boolean {
                                    // Reject any unexpected page navigation away from bridge.html.
                                    // Official docs: https://developer.android.com/reference/android/webkit/WebViewClient#shouldOverrideUrlLoading(android.webkit.WebView,%20android.webkit.WebResourceRequest)
                                    return true
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): WebResourceResponse? {
                                    val url = request.url
                                    // Reject requests to unexpected origins or schemes.
                                    // Official docs: https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
                                    if (url.scheme != "https" || url.host != "appassets.androidplatform.net") {
                                        return WebResourceResponse(
                                            "text/plain",
                                            "utf-8",
                                            403,
                                            "Forbidden",
                                            emptyMap(),
                                            ByteArrayInputStream(ByteArray(0)),
                                        )
                                    }
                                    return assetLoader.shouldInterceptRequest(url)
                                }

                                override fun onPageFinished(
                                    view: WebView,
                                    url: String,
                                ) {
                                    synchronized(lock) {
                                        if (state != State.INITIALIZING || generation != targetGeneration || view !== webView) {
                                            return
                                        }
                                        state = State.READY
                                        _isInitialized.value = true
                                        if (!deferred.isCompleted) {
                                            deferred.complete(view)
                                        }
                                    }
                                }

                                @RequiresApi(Build.VERSION_CODES.O)
                                override fun onRenderProcessGone(
                                    view: WebView,
                                    detail: RenderProcessGoneDetail,
                                ): Boolean = handleRenderProcessGone(view, detail)
                            }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    val initEx = HighlightException.WebViewInitFailed(e)
                    synchronized(lock) {
                        if (generation == targetGeneration) {
                            state = State.FAILED
                            _isInitialized.value = false
                            deferred.completeExceptionally(initEx)
                        }
                    }
                    throw initEx
                }

            synchronized(lock) {
                if (state == State.DESTROYED || generation != targetGeneration) {
                    // destroy() was called while WebView was being constructed on Main.
                    // Clean up and discard the newly created instance immediately.
                    try {
                        view.destroy()
                    } catch (_: Throwable) {
                        // Suppress secondary cleanup exceptions.
                    }
                    return@withContext
                }
                webView = view
            }

            try {
                // Serves local assets over https:// via WebViewAssetLoader - required for
                // Same-Origin Policy compliance so that highlight.min.js can execute.
                // https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
                view.loadUrl("https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val initEx = HighlightException.WebViewInitFailed(e)
                synchronized(lock) {
                    if (generation == targetGeneration) {
                        state = State.FAILED
                        _isInitialized.value = false
                        webView = null
                        deferred.completeExceptionally(initEx)
                    }
                }
                try {
                    view.destroy()
                } catch (_: Throwable) {
                    // Suppress secondary cleanup exceptions.
                }
                throw initEx
            }
        }
    }

    /**
     * Handles WebView renderer process termination (crash or OS low-memory eviction).
     *
     * Invoked from [WebViewClient.onRenderProcessGone] on the Main thread. Returns `true`
     * to signal to Android that the host application handles termination and must not be killed.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleRenderProcessGone(
        view: WebView,
        detail: RenderProcessGoneDetail,
    ): Boolean {
        val didCrash = detail.didCrash()
        val isCurrentWebView: Boolean

        synchronized(lock) {
            isCurrentWebView = (view === webView)
            if (isCurrentWebView) {
                webView = null
                _isInitialized.value = false
                state = State.IDLE
                generation++

                val exception = HighlightException.RendererGone(didCrash)
                if (!readyDeferred.isCompleted) {
                    readyDeferred.completeExceptionally(exception)
                } else {
                    readyDeferred =
                        CompletableDeferred<WebView>().apply {
                            completeExceptionally(exception)
                        }
                }
            }
        }

        // The platform requires that the terminated WebView be destroyed.
        // onRenderProcessGone runs on Main, so view.destroy() can be called directly.
        try {
            view.destroy()
        } catch (_: Throwable) {
            // Suppress secondary exceptions during dead WebView destruction.
        }

        if (isCurrentWebView) {
            onRendererGone?.invoke(didCrash)
        }

        return true
    }

    /** Destroys the WebView and releases resources. Safe to call from any thread. */
    fun destroy() {
        val viewToDestroy: WebView?
        synchronized(lock) {
            if (state == State.DESTROYED) return
            state = State.DESTROYED
            generation++
            _isInitialized.value = false

            viewToDestroy = webView
            webView = null

            // Fail any pending waiter with EngineDestroyed, then replace with an already-completed
            // deferred so subsequent getReadyWebView() calls fail deterministically instead of hanging.
            if (!readyDeferred.isCompleted) {
                readyDeferred.completeExceptionally(HighlightException.EngineDestroyed())
            } else {
                readyDeferred =
                    CompletableDeferred<WebView>().apply {
                        completeExceptionally(HighlightException.EngineDestroyed())
                    }
            }
        }

        if (viewToDestroy != null) {
            // WebView.destroy() must be called on the thread that created it (Main).
            if (Looper.myLooper() == Looper.getMainLooper()) {
                try {
                    viewToDestroy.destroy()
                } catch (_: Throwable) {
                    // Suppress secondary cleanup exceptions.
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    try {
                        viewToDestroy.destroy()
                    } catch (_: Throwable) {
                        // Suppress secondary cleanup exceptions.
                    }
                }
            }
        }
    }
}
