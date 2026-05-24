package dev.hossain.highlight.sample.chat.model

import kotlinx.serialization.Serializable

/**
 * Response model for Server-Sent Events (SSE) from the AI code chat API.
 *
 * The API streams responses in the format:
 * ```
 * data: {"choices":[{"delta":{"content":"token"}}]}
 * data: {"choices":[{"delta":{"content":" here"}}]}
 * ...
 * data: [DONE]
 * ```
 */
@Serializable
data class CodeChatResponse(
    val choices: List<Choice>? = null,
    val error: String? = null,
) {
    /**
     * Represents a single choice in the response.
     */
    @Serializable
    data class Choice(
        val delta: Delta? = null,
    )

    /**
     * Represents the delta (incremental content update).
     */
    @Serializable
    data class Delta(
        val content: String? = null,
    )

    /**
     * Extracts the token content from this response, if available.
     *
     * @return The content string, or null if not present in this response
     */
    fun getToken(): String? = choices?.firstOrNull()?.delta?.content
}
