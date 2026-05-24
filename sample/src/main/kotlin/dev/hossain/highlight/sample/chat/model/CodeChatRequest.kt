package dev.hossain.highlight.sample.chat.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request model for the AI code chat API.
 *
 * Sent to `POST https://hossain.dev/api/ai-code-chat`
 *
 * @param prompt The programming question (max 2000 characters)
 * @param language Optional primary programming language (e.g., "kotlin", "python", "typescript")
 * @param sessionId Optional session UUID for tracking conversations
 * @param conversationHistory Optional list of previous messages for multi-turn conversations
 */
@Serializable
data class CodeChatRequest(
    val prompt: String,
    val language: String? = null,
    @SerialName("sessionId")
    val sessionId: String? = null,
    @SerialName("conversationHistory")
    val conversationHistory: List<ChatMessage>? = null,
)
