package dev.composehighlight.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Internal manager responsible for creating and initializing the hidden WebView.
 *
 * Mirrors Perplexity's `ra/d` class: a WebView singleton with lazy initialization,
 * loaded via [WebViewAssetLoader] using the `appassets.androidplatform.net` scheme.
 */
internal class WebViewManager(private val context: Context) {

    private var webView: WebView? = null
    private val readyDeferred = CompletableDeferred<WebView>()

    /** Returns the ready WebView. Suspends until bridge.html has finished loading. */
    suspend fun getReadyWebView(): WebView = readyDeferred.await()

    /**
     * Creates the WebView on the Main thread and loads bridge.html.
     * Safe to call multiple times — idempotent after first call.
     */
    suspend fun initialize() {
        if (webView != null) return

        withContext(Dispatchers.Main) {
            if (webView != null) return@withContext

            val assetLoader = WebViewAssetLoader.Builder()
                .setDomain("appassets.androidplatform.net")
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()

            val view = WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                    override fun onPageFinished(view: WebView, url: String) {
                        if (!readyDeferred.isCompleted) {
                            readyDeferred.complete(view)
                        }
                    }
                }
                loadUrl("https://appassets.androidplatform.net/assets/compose-highlight/bridge.html")
            }

            webView = view
        }
    }

    /** Destroys the WebView and releases resources. Safe to call from any thread. */
    fun destroy() {
        val wv = webView ?: return
        webView = null
        // WebView.destroy() must be called on the thread that created it (Main).
        Handler(Looper.getMainLooper()).post { wv.destroy() }
    }
}
