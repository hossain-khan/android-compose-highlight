package dev.hossain.highlight.engine

/**
 * Exception hierarchy for compose-highlight engine failures.
 *
 * All public [HighlightEngine] methods return `Result<T>` and wrap failures in one of these
 * subtypes rather than throwing directly. Catch [HighlightException] to handle all engine errors
 * in one place, or match individual subtypes for fine-grained handling.
 *
 * @param message Human-readable description of the failure.
 * @param cause The underlying throwable that triggered this exception, if any.
 */
public sealed class HighlightException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * Thrown when the hidden WebView cannot be created or its bridge page fails to load.
     *
     * @param cause The underlying throwable that triggered the initialization failure.
     */
    public class WebViewInitFailed(
        cause: Throwable,
    ) : HighlightException("WebView initialization failed", cause)

    /**
     * Thrown when a JavaScript engine call (e.g. `highlightCode`, `listLanguages`,
     * `hljsVersion`) returns `null` or raises an error.
     *
     * @param cause The underlying throwable from the JavaScript execution failure.
     */
    public class JsExecutionFailed(
        cause: Throwable,
    ) : HighlightException("JavaScript execution failed", cause)

    /**
     * Thrown when a theme CSS asset is found but produces no parseable color rules
     * (e.g. the file is empty or contains only unsupported CSS).
     *
     * Note: a *missing* or *unreadable* asset file throws [java.io.IOException] instead,
     * because the error surfaces before any CSS parsing occurs.
     *
     * @param path The asset path that was resolved but yielded no usable color rules.
     */
    public class ThemeNotFound(
        path: String,
    ) : HighlightException("Theme CSS has no parseable color rules: $path")

    /**
     * Thrown when the HTML parser fails to parse the HTML returned by Highlight.js.
     *
     * @param cause The underlying throwable from the HTML parse failure.
     */
    public class HtmlParseFailed(
        cause: Throwable,
    ) : HighlightException("HTML parsing failed", cause)

    /** Thrown when a highlight call does not complete within [TIMEOUT_SECONDS] seconds. */
    public class Timeout : HighlightException("Highlighting timed out after ${TIMEOUT_SECONDS}s")

    public companion object {
        /** Maximum number of seconds a single highlight call may take before [Timeout] is thrown. */
        public const val TIMEOUT_SECONDS: Long = 5L
    }
}
