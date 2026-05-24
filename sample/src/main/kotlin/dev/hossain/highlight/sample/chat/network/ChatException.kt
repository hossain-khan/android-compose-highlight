package dev.hossain.highlight.sample.chat.network

/**
 * Sealed class representing errors that can occur during AI chat API interactions.
 */
internal sealed class ChatException(
    message: String,
) : Exception(message) {
    /** The API returned HTTP 429 - rate limit exceeded (150 requests/day). */
    data object RateLimitExceeded :
        ChatException("Rate limit exceeded (150 requests/day). Please try again later.")

    /** The API returned a non-success HTTP status code. */
    data class ApiError(
        val code: Int,
        val body: String,
    ) : ChatException("API error $code: $body")

    /** A network connectivity or IO error occurred. */
    data class NetworkError(
        override val cause: Throwable,
    ) : ChatException("Network error: ${cause.message}")
}
