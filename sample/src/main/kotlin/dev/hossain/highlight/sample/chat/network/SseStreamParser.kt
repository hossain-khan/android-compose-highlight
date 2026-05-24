package dev.hossain.highlight.sample.chat.network

import dev.hossain.highlight.sample.chat.model.CodeChatResponse
import kotlinx.serialization.json.Json

/**
 * Utility for parsing Server-Sent Events (SSE) format from the streaming API.
 *
 * SSE format:
 * ```
 * data: {"choices":[{"delta":{"content":"token"}}]}
 * data: [DONE]
 * ```
 */
object SseStreamParser {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    /**
     * Parses a single SSE line and extracts the token content if present.
     *
     * @param line A line from the SSE stream
     * @return The token content if this line contains a valid SSE data event, null otherwise
     * @throws IllegalArgumentException If the JSON in the SSE data is malformed
     */
    fun parseToken(line: String): String? {
        // Skip empty lines and non-data lines
        if (line.isBlank() || !line.startsWith("data:")) {
            return null
        }

        val jsonData = line.substring(5).trim() // Remove "data:" prefix

        // Check for stream completion marker
        if (jsonData == "[DONE]") {
            return null
        }

        return try {
            val response = json.decodeFromString<CodeChatResponse>(jsonData)
            response.getToken()
        } catch (e: Exception) {
            // Log malformed JSON but don't crash - just skip this line
            null
        }
    }

    /**
     * Checks if a line marks the end of the SSE stream.
     *
     * @param line A line from the SSE stream
     * @return True if this is the [DONE] marker, false otherwise
     */
    fun isStreamComplete(line: String): Boolean = line.trim() == "data: [DONE]"
}
