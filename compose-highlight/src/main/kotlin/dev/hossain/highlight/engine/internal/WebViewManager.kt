package dev.hossain.highlight.engine.internal

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.VisibleForTesting
import androidx.webkit.WebViewAssetLoader
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

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
 * ## Threading invariants
 *
 * The [WebView] itself is not thread-safe - the platform requires construction, JS evaluation,
 * and destruction to all happen on the thread that created it. This manager pins that to the
 * Main thread.
 *
 * **Method dispatch:**
 * - [initialize] is `suspend` and dispatches its critical section to [Dispatchers.Main] via
 *   `withContext`. The [WebView] is constructed and `bridge.html` is loaded on Main.
 * - [destroy] runs on the caller's thread (it is `fun`, not `suspend`), but its only Main-thread
 *   contract is that the actual `wv.destroy()` call is posted to the Main looper - field
 *   mutation can happen anywhere.
 * - [getReadyWebView] is `suspend` and can be awaited from any thread. It only calls
 *   [CompletableDeferred.await] on [readyDeferred].
 *
 * **Field write sites:**
 * - [webView] - set on Main inside [initialize]; cleared from any thread inside [destroy].
 *   Marked `@Volatile` so the clear in [destroy] is immediately visible to [initialize] and
 *   [onPageFinished] running on Main.
 * - [readyDeferred] - reassigned on Main inside [initialize] (only when the previous one is
 *   complete) and from any thread inside [destroy]. Marked `@Volatile` for the same reason.
 * - [_isInitialized] - flipped to `true` on Main from `onPageFinished`; flipped to `false` from
 *   any thread inside [destroy]. [MutableStateFlow] handles concurrent writes safely.
 *
 * **Why no [kotlinx.coroutines.sync.Mutex]:**
 *
 * In practice, [initialize] and [destroy] are paired with a single [HighlightEngine] instance
 * whose lifecycle is owned by Compose's `DisposableEffect` (see `HighlightThemeProvider` and
 * `rememberHighlightEngine`). `DisposableEffect.onDispose` runs on the **applier thread**,
 * which is Main, and effects are not re-entered concurrently. So:
 * - Two [initialize] coroutines from the same engine would race on entering Main, but the
 *   `webView != null` early-return after the dispatch makes the second one a no-op.
 * - [destroy] is invoked once per `onDispose`, exactly once per engine lifecycle.
 *
 * The remaining "racy on paper" sequences (a destroy+reassign happening between [initialize]'s
 * `readyDeferred.isCompleted` check and its reassignment, for example) settle into benign
 * end states - the captured local `deferred` in [initialize] guarantees [onPageFinished] always
 * completes the deferred it was paired with, and the `webView == null` guard at the top of
 * [onPageFinished] no-ops cleanly when destroy ran first.
 *
 * If callers ever need to invoke this manager outside the `DisposableEffect` discipline (e.g.
 * a `ViewModel`-owned engine driven by `onCleared`), [destroy] can be called from any thread -
 * the field cleanup and the posted `wv.destroy()` are independent. The threading model still
 * holds; the implicit Main-thread serialization just changes from "Compose applier" to
 * "whatever the caller arranges."
 */
internal class WebViewManager(
    private val context: Context,
) {
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
        if (webView != null) return

        withContext(Dispatchers.Main) {
            if (webView != null) return@withContext

            // Reset the deferred so re-initialization after destroy works correctly.
            // Capture as a local so the WebViewClient closure always completes *this* deferred.
            if (readyDeferred.isCompleted) {
                readyDeferred = CompletableDeferred()
            }
            val deferred = readyDeferred

            val assetLoader =
                WebViewAssetLoader
                    .Builder()
                    .setDomain("appassets.androidplatform.net")
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                    .build()

            val view =
                try {
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient =
                            object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                                override fun onPageFinished(
                                    view: WebView,
                                    url: String,
                                ) {
                                    // Guard against a race where destroy() ran while the page
                                    // was still loading: webView was set to null by destroy(),
                                    // and readyDeferred was replaced with a fresh instance.
                                    // Completing the captured (now-cancelled) deferred would
                                    // leave the new deferred permanently incomplete - hung engine.
                                    // Returning here lets the next initialize() call pick up
                                    // the fresh deferred and complete it normally.
                                    if (webView == null) return
                                    if (!deferred.isCompleted) {
                                        deferred.complete(view)
                                        _isInitialized.value = true
                                    }
                                }
                            }
                        // Serves local assets over https:// via WebViewAssetLoader - required for
                        // Same-Origin Policy compliance so that highlight.min.js can execute.
                        // https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
                        loadUrl("https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
                    }
                } catch (e: Exception) {
                    throw HighlightException.WebViewInitFailed(e)
                }

            webView = view
        }
    }

    /** Destroys the WebView and releases resources. Safe to call from any thread. */
    fun destroy() {
        val wv = webView ?: return
        webView = null
        _isInitialized.value = false
        // Cancel any pending waiter, then reset to drop the strong reference to the
        // destroyed WebView and allow it to be GC'd immediately.
        if (!readyDeferred.isCompleted) {
            readyDeferred.cancel()
        }
        readyDeferred = CompletableDeferred()
        // WebView.destroy() must be called on the thread that created it (Main).
        Handler(Looper.getMainLooper()).post { wv.destroy() }
    }
}
