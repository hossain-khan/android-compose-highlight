package dev.hossain.highlight.sample.chat.model

import kotlinx.serialization.Serializable

/**
 * Represents a single message in the chat conversation.
 *
 * @param role The role of the message sender: "user" or "assistant"
 * @param content The text content of the message
 */
@Serializable
data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
)
