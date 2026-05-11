package dev.hossain.highlight.engine

/**
 * Exception hierarchy for compose-highlight engine failures.
 *
 * All public [HighlightEngine] methods return `Result<T>` and wrap failures in one of these
 * subtypes rather than throwing directly. Catch [HighlightException] to handle all engine errors
 * in one place, or match individual subtypes for fine-grained handling.
 */
sealed class HighlightException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** Thrown when the hidden WebView cannot be created or its bridge page fails to load. */
    class WebViewInitFailed(
        cause: Throwable,
    ) : HighlightException("WebView initialization failed", cause)

    /**
     * Thrown when a JavaScript engine call (e.g. `highlightCode`, `listLanguages`,
     * `hljsVersion`) returns `null` or raises an error.
     */
    class JsExecutionFailed(
        cause: Throwable,
    ) : HighlightException("JavaScript execution failed", cause)

    /**
     * Thrown when a theme CSS asset is found but produces no parseable color rules
     * (e.g. the file is empty or contains only unsupported CSS).
     *
     * Note: a *missing* or *unreadable* asset file throws [java.io.IOException] instead,
     * because the error surfaces before any CSS parsing occurs.
     */
    class ThemeNotFound(
        path: String,
    ) : HighlightException("Theme CSS not found: $path")

    /** Thrown when jsoup fails to parse the HTML returned by Highlight.js. */
    class HtmlParseFailed(
        cause: Throwable,
    ) : HighlightException("HTML parsing failed", cause)

    /** Thrown when a highlight call does not complete within [TIMEOUT_SECONDS] seconds. */
    class Timeout : HighlightException("Highlighting timed out after ${TIMEOUT_SECONDS}s")

    companion object {
        /** Maximum number of seconds a single highlight call may take before [Timeout] is thrown. */
        const val TIMEOUT_SECONDS = 5L
    }
}
