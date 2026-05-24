package dev.hossain.highlight.sample.chat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hossain.highlight.sample.chat.state.ChatViewModel

/**
 * Demo tab entry point for the LLM-based chat streaming feature.
 *
 * This composable integrates the chat functionality into the sample app's demo tabs.
 * It creates a ChatViewModel instance and passes the state to the ChatScreen composable.
 *
 * Features:
 * - Real-time streaming responses from the AI API
 * - Multi-turn conversation support with history
 * - Language context for better responses
 * - Character limit enforcement (2000 characters)
 * - Error handling with user-friendly messages
 * - Session management for conversation tracking
 */
@Composable
fun ChatDemoTab() {
    val viewModel: ChatViewModel = viewModel()

    val uiState by viewModel.uiState
    val selectedLanguage by viewModel.selectedLanguage

    ChatScreen(
        uiState = uiState,
        onSendMessage = { prompt ->
            viewModel.sendMessage(prompt)
        },
        onClearConversation = {
            viewModel.clearConversation()
        },
        selectedLanguage = selectedLanguage,
        onLanguageChange = { language ->
            viewModel.setLanguage(language)
        },
    )
}
