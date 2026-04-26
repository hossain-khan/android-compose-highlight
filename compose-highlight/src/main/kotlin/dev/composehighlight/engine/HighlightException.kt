package dev.composehighlight.engine

/**
 * Exception hierarchy for compose-highlight engine failures.
 * Modeled on Perplexity's `MarkdownException$HighlightJsFailed` pattern.
 */
sealed class HighlightException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class WebViewInitFailed(cause: Throwable) :
        HighlightException("WebView initialization failed", cause)

    class JsExecutionFailed(cause: Throwable) :
        HighlightException("JavaScript execution failed", cause)

    class ThemeNotFound(path: String) :
        HighlightException("Theme CSS not found: $path")

    class HtmlParseFailed(cause: Throwable) :
        HighlightException("HTML parsing failed", cause)

    class Timeout :
        HighlightException("Highlighting timed out after ${TIMEOUT_SECONDS}s")

    companion object {
        const val TIMEOUT_SECONDS = 5L
    }
}
