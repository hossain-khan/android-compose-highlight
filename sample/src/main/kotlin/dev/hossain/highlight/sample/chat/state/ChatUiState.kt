package dev.hossain.highlight.sample.chat.state

/**
 * Sealed class representing the possible UI states of the chat demo screen.
 */
internal sealed class ChatUiState {
    /** No active request - the user can send a new message. */
    data object Idle : ChatUiState()

    /** A response is currently streaming in from the API. */
    data object Streaming : ChatUiState()

    /** The last request failed with an error message. */
    data class Error(
        val message: String,
    ) : ChatUiState()
}
