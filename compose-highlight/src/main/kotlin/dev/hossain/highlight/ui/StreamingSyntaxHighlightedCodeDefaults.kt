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

    /**
     * Default minimum throttle interval in milliseconds between consecutive newline-triggered highlight runs.
     *
     * 150 ms prevents overloading the JavaScript engine when rapid or consecutive newlines (`\n\n`) arrive,
     * while allowing completed lines to be progressively styled in the background as streaming continues.
     */
    const val MIN_THROTTLE_MS: Long = 150L
}
