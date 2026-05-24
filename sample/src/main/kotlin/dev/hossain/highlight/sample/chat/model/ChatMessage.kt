package dev.hossain.highlight.sample.chat.model

/**
 * Represents a single chat message in the conversation history.
 *
 * @property role The role of the message sender (user or assistant).
 * @property content The text content of the message.
 */
internal data class ChatMessage(
    val role: MessageRole,
    val content: String,
) {
    /** The role of the message sender. */
    enum class MessageRole {
        USER,
        ASSISTANT,
    }
}
