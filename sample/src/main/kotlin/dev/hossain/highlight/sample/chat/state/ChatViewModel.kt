package dev.hossain.highlight.sample.chat.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hossain.highlight.sample.chat.model.ChatMessage
import dev.hossain.highlight.sample.chat.model.CodeChatRequest
import dev.hossain.highlight.sample.chat.network.ChatException
import dev.hossain.highlight.sample.chat.network.CodeAIService
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for the LLM chat streaming demo.
 *
 * Manages conversation state, SSE token accumulation, and lifecycle-aware
 * coroutine execution via [viewModelScope].
 *
 * Usage example:
 * ```kotlin
 * val viewModel: ChatViewModel = viewModel()
 *
 * // Send a message
 * viewModel.sendMessage(prompt = "How do I reverse a list?", language = "kotlin")
 *
 * // Observe state
 * val messages = viewModel.messages
 * val streaming = viewModel.streamingContent
 * val state = viewModel.uiState
 * ```
 */
internal class ChatViewModel : ViewModel() {
    private val service = CodeAIService()

    /** The full conversation history (committed user + assistant messages). */
    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set

    /**
     * The currently accumulating assistant response text during streaming.
     * Empty when not streaming.
     */
    var streamingContent by mutableStateOf("")
        private set

    /** Current UI state - [ChatUiState.Idle], [ChatUiState.Streaming], or [ChatUiState.Error]. */
    var uiState by mutableStateOf<ChatUiState>(ChatUiState.Idle)
        private set

    private var sessionId: String = generateSessionId()

    /**
     * Sends a user message to the AI API and accumulates the streaming response.
     *
     * Does nothing if [uiState] is already [ChatUiState.Streaming].
     *
     * @param prompt The user's question (max 2000 characters).
     * @param language The programming language context for the question.
     */
    fun sendMessage(
        prompt: String,
        language: String,
    ) {
        if (uiState == ChatUiState.Streaming) return

        val userMessage = ChatMessage(ChatMessage.MessageRole.USER, prompt)
        // Capture all existing messages as conversation history before appending the new user
        // message - the API receives prior turns as context, not the current prompt itself.
        val history = messages.toList()
        messages = messages + userMessage
        streamingContent = ""
        uiState = ChatUiState.Streaming

        val request =
            CodeChatRequest(
                prompt = prompt,
                language = language,
                sessionId = sessionId,
                conversationHistory = history,
            )

        viewModelScope.launch {
            try {
                service.streamChat(request).collect { token ->
                    streamingContent += token
                }
                // Streaming complete - commit the full assistant message
                val fullResponse = streamingContent
                messages = messages + ChatMessage(ChatMessage.MessageRole.ASSISTANT, fullResponse)
                streamingContent = ""
                uiState = ChatUiState.Idle
            } catch (e: ChatException.RateLimitExceeded) {
                streamingContent = ""
                uiState = ChatUiState.Error(e.message ?: "Rate limit exceeded")
            } catch (e: ChatException.ApiError) {
                streamingContent = ""
                uiState = ChatUiState.Error("API error (${e.code}): ${e.body.take(200)}")
            } catch (e: ChatException.NetworkError) {
                streamingContent = ""
                uiState = ChatUiState.Error("Network error: ${e.cause.message ?: "Unknown"}")
            } catch (e: Exception) {
                streamingContent = ""
                uiState = ChatUiState.Error("Error: ${e.message ?: "Unknown error"}")
            }
        }
    }

    /**
     * Clears all messages and resets the conversation to a fresh state.
     *
     * Also generates a new session ID so the next conversation starts fresh
     * without any context from the previous one.
     */
    fun clearConversation() {
        messages = emptyList()
        streamingContent = ""
        uiState = ChatUiState.Idle
        sessionId = generateSessionId()
    }

    /**
     * Dismisses the current error and returns to [ChatUiState.Idle].
     */
    fun dismissError() {
        if (uiState is ChatUiState.Error) {
            uiState = ChatUiState.Idle
        }
    }

    private fun generateSessionId(): String = UUID.randomUUID().toString()
}
