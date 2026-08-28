package dev.hossain.highlight.ui

/**
 * Default values used by [StreamingSyntaxHighlightedCode] and [rememberStreamingHighlightedCode].
 *
 * Exposes constants so callers can build on or configure streaming highlight behavior.
 *
 * ## Usage
 *
 * ```kotlin
 * StreamingSyntaxHighlightedCode(
 *     code = streamingCode,
 *     language = "kotlin",
 *     debounceMs = StreamingSyntaxHighlightedCodeDefaults.DEBOUNCE_MS,
 * )
 * ```
 */
@ExperimentalHighlightApi
object StreamingSyntaxHighlightedCodeDefaults {
    /**
     * Default debounce delay in milliseconds before triggering a highlight cycle on streaming text.
     *
     * 200 ms provides a good balance between responsiveness when the stream pauses and avoiding
     * excessive WebView calls during active token generation.
     */
    const val DEBOUNCE_MS: Long = 200L
}
