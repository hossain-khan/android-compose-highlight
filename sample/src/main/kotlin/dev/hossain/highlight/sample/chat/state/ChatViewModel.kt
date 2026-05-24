package dev.hossain.highlight.sample.chat.state

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hossain.highlight.sample.chat.model.ChatMessage
import dev.hossain.highlight.sample.chat.network.CodeAIClient
import dev.hossain.highlight.sample.chat.network.CodeAIService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "ChatViewModel"

/**
 * ViewModel for managing chat state and API interactions.
 *
 * Handles:
 * - Conversation history management
 * - Streaming response accumulation
 * - UI state transitions
 * - Error handling and display
 *
 * The ViewModel persists conversation history across configuration changes
 * and provides a stable interface for Compose UI to subscribe to state changes.
 */
class ChatViewModel : ViewModel() {
    private val httpClient = CodeAIClient.create()
    private val service = CodeAIService(httpClient)

    private val _uiState = mutableStateOf<ChatUiState>(ChatUiState.Idle)
    val uiState: State<ChatUiState> = _uiState

    private val _conversationHistory = mutableStateOf<List<ChatMessage>>(emptyList())
    val conversationHistory: State<List<ChatMessage>> = _conversationHistory

    private val _sessionId = mutableStateOf(UUID.randomUUID().toString())
    val sessionId: State<String> = _sessionId

    private val _selectedLanguage = mutableStateOf<String?>(null)
    val selectedLanguage: State<String?> = _selectedLanguage

    /**
     * Updates the selected programming language for context.
     *
     * @param language Programming language name or null
     */
    fun setLanguage(language: String?) {
        _selectedLanguage.value = language
    }

    /**
     * Sends a message and streams the response.
     *
     * Adds the user message to history, makes the API request, and streams tokens
     * into the UI state. The complete response is added to history once streaming completes.
     *
     * @param prompt The user's programming question
     */
    fun sendMessage(prompt: String) {
        Log.d(TAG, "sendMessage called with prompt: '$prompt' (${prompt.length} chars)")

        if (prompt.isBlank()) {
            Log.w(TAG, "Blank prompt, showing error")
            _uiState.value = ChatUiState.Error("Please enter a question")
            return
        }

        if (prompt.length > 2000) {
            Log.w(TAG, "Prompt exceeds 2000 chars")
            _uiState.value = ChatUiState.Error("Question exceeds 2000 character limit")
            return
        }

        Log.d(TAG, "Validation passed, adding user message to history")

        // Add user message to history
        val userMessage = ChatMessage(role = "user", content = prompt)
        val updatedHistory = _conversationHistory.value + userMessage
        _conversationHistory.value = updatedHistory

        // Start streaming response
        Log.d(TAG, "Starting loading state, launching API call")
        _uiState.value = ChatUiState.Loading()

        viewModelScope.launch {
            Log.d(TAG, "In viewModelScope, calling service.chat() with sessionId=${_sessionId.value}")
            service.chat(
                prompt = prompt,
                language = _selectedLanguage.value,
                sessionId = _sessionId.value,
                conversationHistory = updatedHistory.dropLast(1), // Exclude current message for context
                onToken = { token ->
                    Log.v(TAG, "Token received: '$token'")
                    val currentState = _uiState.value
                    if (currentState is ChatUiState.Loading) {
                        val newResponse = currentState.currentResponse + token
                        Log.v(TAG, "Updating state with response: '${newResponse.take(50)}...' (total length: ${newResponse.length})")
                        // Dispatch state update to Main thread to trigger recomposition
                        _uiState.value =
                            ChatUiState.Loading(
                                currentResponse = newResponse,
                            )
                    } else {
                        Log.w(TAG, "Received token but not in Loading state, current: $currentState")
                    }
                },
                onError = { error ->
                    Log.e(TAG, "API error: $error")
                    _uiState.value = ChatUiState.Error(error)
                },
                onComplete = {
                    Log.d(TAG, "Chat completed")
                    val currentState = _uiState.value
                    if (currentState is ChatUiState.Loading) {
                        // Add assistant message to history
                        val assistantMessage =
                            ChatMessage(
                                role = "assistant",
                                content = currentState.currentResponse,
                            )
                        val finalHistory = updatedHistory + assistantMessage
                        _conversationHistory.value = finalHistory
                        _uiState.value = ChatUiState.Success(finalHistory)
                    }
                },
            )
        }
    }

    /**
     * Clears the conversation history and returns to idle state.
     *
     * Also generates a new session ID for the next conversation.
     */
    fun clearConversation() {
        _conversationHistory.value = emptyList()
        _uiState.value = ChatUiState.Idle
        _sessionId.value = UUID.randomUUID().toString()
    }

    /**
     * Resumes a previous conversation with given history.
     *
     * @param messages Previous conversation messages to restore
     */
    fun resumeConversation(messages: List<ChatMessage>) {
        _conversationHistory.value = messages
        _uiState.value = ChatUiState.Success(messages)
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }
}
