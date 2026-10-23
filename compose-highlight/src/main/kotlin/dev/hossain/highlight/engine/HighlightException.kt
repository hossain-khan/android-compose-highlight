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
     * Thrown when a theme CSS asset cannot be found or read, or produces no parseable
     * color rules (e.g. the file is empty or contains only unsupported CSS).
     *
     * @param path The asset path that failed to resolve or yielded no usable color rules.
     * @param cause The underlying throwable that triggered this exception, if any.
     */
    public class ThemeNotFound
        @JvmOverloads
        public constructor(
            path: String,
            cause: Throwable? = null,
        ) : HighlightException("Theme CSS has no parseable color rules: $path", cause)

    /**
     * Thrown when the HTML parser fails to parse the HTML returned by Highlight.js.
     *
     * @param cause The underlying throwable from the HTML parse failure.
     */
    public class HtmlParseFailed(
        cause: Throwable,
    ) : HighlightException("HTML parsing failed", cause)

    /**
     * Thrown when the WebView renderer process terminates unexpectedly, either due to an
     * internal crash or because the system terminated it due to memory pressure.
     *
     * @param didCrash `true` if the renderer process crashed; `false` if it was killed by the
     *   system (e.g. low-memory killer).
     * @param cause The underlying throwable that triggered this exception, if any.
     */
    public class RendererGone
        @JvmOverloads
        public constructor(
            public val didCrash: Boolean = false,
            cause: Throwable? = null,
        ) : HighlightException(
                message =
                    if (didCrash) {
                        "WebView renderer process crashed"
                    } else {
                        "WebView renderer process was terminated by the system (low memory)"
                    },
                cause = cause,
            )

    /** Thrown when a highlight call does not complete within [TIMEOUT_SECONDS] seconds. */
    public class Timeout : HighlightException("Highlighting timed out after ${TIMEOUT_SECONDS}s")

    public companion object {
        /** Maximum number of seconds a single highlight call may take before [Timeout] is thrown. */
        public const val TIMEOUT_SECONDS: Long = 5L
    }
}
