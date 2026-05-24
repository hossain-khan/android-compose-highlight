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

    // Pre-compiled patterns for the two field names used in the API response.
    // The capture group `((?:[^"\\]|\\.)*)` matches a JSON string value:
    //   [^"\\]  - any character that is not a quote or backslash
    //   \\.     - a backslash followed by any character (escape sequence)
    //   (?:...)*- zero or more repetitions (non-capturing group)
    private val TOKEN_FIELD_PATTERN = Regex(""""token"\s*:\s*"((?:[^"\\]|\\.)*)"""")
    private val CONTENT_FIELD_PATTERN = Regex(""""content"\s*:\s*"((?:[^"\\]|\\.)*)"""")

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
        return extractJsonStringField(dataContent, TOKEN_FIELD_PATTERN)
            ?: extractJsonStringField(dataContent, CONTENT_FIELD_PATTERN)
    }

    private fun extractJsonStringField(
        json: String,
        pattern: Regex,
    ): String? {
        val match = pattern.find(json) ?: return null
        val rawValue = match.groupValues[1]
        // Return null for empty strings to avoid emitting empty tokens
        return if (rawValue.isEmpty()) null else unescapeJsonString(rawValue)
    }

    private fun unescapeJsonString(escaped: String): String {
        // Single-pass RFC 8259 compliant unescape.
        val sb = StringBuilder(escaped.length)
        var i = 0
        while (i < escaped.length) {
            val ch = escaped[i]
            if (ch == '\\' && i + 1 < escaped.length) {
                when (escaped[i + 1]) {
                    '"' -> {
                        sb.append('"')
                        i += 2
                    }

                    '\\' -> {
                        sb.append('\\')
                        i += 2
                    }

                    '/' -> {
                        sb.append('/')
                        i += 2
                    }

                    'b' -> {
                        sb.append('\b')
                        i += 2
                    }

                    'f' -> {
                        sb.append('\u000C')
                        i += 2
                    }

                    'n' -> {
                        sb.append('\n')
                        i += 2
                    }

                    'r' -> {
                        sb.append('\r')
                        i += 2
                    }

                    't' -> {
                        sb.append('\t')
                        i += 2
                    }

                    'u' -> {
                        if (i + 5 < escaped.length) {
                            val hex = escaped.substring(i + 2, i + 6)
                            sb.append(hex.toInt(16).toChar())
                            i += 6
                        } else {
                            sb.append(ch)
                            i++
                        }
                    }

                    else -> {
                        sb.append(ch)
                        i++
                    }
                }
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }
}
