package dev.hossain.highlight.sample.chat.network

/**
 * Utility for parsing Server-Sent Events (SSE) streams.
 *
 * SSE format uses lines prefixed with `data: ` to carry event payloads.
 * Each event ends with a blank line. The special marker `[DONE]` signals
 * end of stream.
 *
 * Example SSE stream:
 * ```
 * data: {"token":"Hello","done":false}
 * data: {"token":" world","done":false}
 * data: [DONE]
 * ```
 */
internal object SseStreamParser {
    private const val DATA_PREFIX = "data: "
    private const val DONE_MARKER = "[DONE]"

    /**
     * Returns true if [line] is an SSE data line (starts with `data: `).
     */
    fun isDataLine(line: String): Boolean = line.startsWith(DATA_PREFIX)

    /**
     * Extracts the payload from an SSE data line by removing the `data: ` prefix.
     */
    fun extractDataContent(line: String): String = line.removePrefix(DATA_PREFIX)

    /**
     * Returns true if [dataContent] is the end-of-stream marker `[DONE]`.
     */
    fun isDoneMarker(dataContent: String): Boolean = dataContent.trim() == DONE_MARKER

    /**
     * Parses a streaming token from an SSE data JSON payload.
     *
     * Tries the `token` field first (e.g., `{"token":"hello","done":false}`),
     * then falls back to `content` (e.g., `{"content":"hello"}`).
     *
     * Returns `null` if the payload is a done marker or no token field is found.
     */
    fun parseToken(dataContent: String): String? {
        if (isDoneMarker(dataContent)) return null
        return extractJsonStringField(dataContent, "token")
            ?: extractJsonStringField(dataContent, "content")
    }

    private fun extractJsonStringField(
        json: String,
        fieldName: String,
    ): String? {
        val pattern = Regex(""""$fieldName"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val match = pattern.find(json) ?: return null
        val rawValue = match.groupValues[1]
        // Return null for empty strings to avoid emitting empty tokens
        return if (rawValue.isEmpty()) null else unescapeJsonString(rawValue)
    }

    private fun unescapeJsonString(escaped: String): String =
        escaped
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
}
