package dev.hossain.highlight.sample.chat.state

import dev.hossain.highlight.sample.chat.model.ChatMessage

/**
 * Represents the UI state for the chat screen.
 *
 * This sealed class handles all possible states during chat interaction,
 * including idle, loading, success, and error states.
 */
sealed class ChatUiState {
    /**
     * Initial state or after clearing the conversation.
     */
    data object Idle : ChatUiState()

    /**
     * Currently streaming a response from the API.
     *
     * @param currentResponse The accumulated response text so far
     */
    data class Loading(
        val currentResponse: String = "",
    ) : ChatUiState()

    /**
     * Successfully received and displayed a response.
     *
     * @param allMessages Complete conversation history
     */
    data class Success(
        val allMessages: List<ChatMessage>,
    ) : ChatUiState()

    /**
     * An error occurred during the chat.
     *
     * @param message Error message to display to user
     */
    data class Error(
        val message: String,
    ) : ChatUiState()
}
