package dev.hossain.highlight.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
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
 * this purpose — it is safe to use and will never conflict with a real website.
 * The [WebViewAssetLoader] intercepts requests to this domain inside [WebViewClient.shouldInterceptRequest]
 * and maps `/assets/` → the app's `assets/` folder, so no real network call is ever made.
 *
 * Official docs: https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
 *
 * ## What bridge.html does
 *
 * `bridge.html` is a minimal HTML page (loaded once at startup) that:
 * 1. Loads the bundled `highlight.min.js` library
 * 2. Defines a single JS function: `highlightCode(code, lang) → HTML string`
 *
 * After the page finishes loading, [HighlightEngine] calls `highlightCode()` via
 * [android.webkit.WebView.evaluateJavascript] for every syntax-highlight request,
 * getting back HTML with `<span class="hljs-*">` tokens that are then converted to
 * an [androidx.compose.ui.text.AnnotatedString] by [HtmlToAnnotatedString].
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
     * Creates the WebView on the Main thread and loads bridge.html.
     * Safe to call multiple times — idempotent after first call.
     * Safe to call after [destroy] — re-creates the WebView and a fresh deferred.
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
