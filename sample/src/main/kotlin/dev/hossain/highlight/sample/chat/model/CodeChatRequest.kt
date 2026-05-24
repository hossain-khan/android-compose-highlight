package dev.hossain.highlight.sample.chat.model

/**
 * Represents a request to the AI code chat API.
 *
 * @property prompt The user's current question or prompt.
 * @property language The programming language context for the question.
 * @property sessionId A unique session identifier for conversation continuity.
 * @property conversationHistory The prior conversation messages for multi-turn context.
 */
internal data class CodeChatRequest(
    val prompt: String,
    val language: String,
    val sessionId: String,
    val conversationHistory: List<ChatMessage>,
) {
    /**
     * Serializes this request to a JSON string for use as an HTTP request body.
     */
    fun toJson(): String {
        val historyJson =
            conversationHistory.joinToString(",") { msg ->
                """{"role":${jsonString(msg.role.name.lowercase())},"content":${jsonString(msg.content)}}"""
            }
        return buildString {
            append("{")
            append("\"prompt\":${jsonString(prompt)},")
            append("\"language\":${jsonString(language)},")
            append("\"sessionId\":${jsonString(sessionId)},")
            append("\"conversationHistory\":[$historyJson]")
            append("}")
        }
    }

    private fun jsonString(text: String): String {
        val escaped =
            text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\u000C", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        // Escape remaining control characters (U+0000 through U+001F) as \uXXXX.
        return "\"${escaped.replace(Regex("[\\u0000-\\u001F]")) { "\\u${it.value[0].code.toString(16).padStart(4, '0')}" }}\""
    }
}
