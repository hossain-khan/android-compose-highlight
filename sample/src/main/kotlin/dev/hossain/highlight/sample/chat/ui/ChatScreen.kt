package dev.hossain.highlight.sample.chat.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.sample.chat.model.ChatMessage
import dev.hossain.highlight.sample.chat.state.ChatUiState

private const val TAG = "ChatScreen"

/**
 * Message bubble component for displaying a single chat message.
 *
 * @param message The chat message to display
 */
@Composable
fun MessageBubble(message: ChatMessage) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier =
                Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        color = if (message.role == "user") Color(0xFF1976D2) else Color(0xFF757575),
                        shape = RoundedCornerShape(12.dp),
                    ),
        ) {
            Text(
                text = message.content,
                color = Color.White,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp,
                fontFamily = FontFamily.Default,
            )
        }
    }
}

/**
 * Streaming response bubble component with animated cursor.
 *
 * @param text The current accumulated response text
 * @param isStreaming Whether the response is still streaming
 */
@Composable
fun StreamingResponseBubble(
    text: String,
    isStreaming: Boolean,
) {
    Log.d(TAG, "StreamingResponseBubble composable called with text length=${text.length}, streaming=$isStreaming")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        // Simplified plain text display for debugging
        Column(
            modifier =
                Modifier
                    .background(
                        color = Color(0xFF81C784),
                        shape = RoundedCornerShape(12.dp),
                    ).padding(12.dp)
                    .fillMaxWidth(0.85f),
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth(),
            )
            if (isStreaming) {
                Text(
                    text = "●",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * Error message display component.
 *
 * @param message The error message to display
 */
@Composable
fun ErrorMessage(message: String) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(
                    color = Color(0xFFE57373),
                    shape = RoundedCornerShape(8.dp),
                ),
    ) {
        Text(
            text = "Error: $message",
            color = Color.White,
            modifier = Modifier.padding(12.dp),
            fontSize = 14.sp,
        )
    }
}

/**
 * Main chat screen composable that displays the conversation and handles input.
 *
 * @param uiState The current UI state
 * @param onSendMessage Callback when user sends a message
 * @param onClearConversation Callback to clear conversation
 * @param selectedLanguage Currently selected programming language
 * @param onLanguageChange Callback when language changes
 */
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit,
    onClearConversation: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState) {
        if (uiState != ChatUiState.Idle) {
            // Always scroll to the very end to show streaming content
            listState.scrollToItem(Int.MAX_VALUE)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth(),
    ) {
        // Messages list
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Display conversation history
            when (uiState) {
                ChatUiState.Idle -> {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Ask a programming question to get started",
                                fontSize = 16.sp,
                                color = Color.Gray,
                            )
                        }
                    }
                }

                is ChatUiState.Loading -> {
                    Log.d(TAG, "Rendering Loading state with response length=${uiState.currentResponse.length}")
                    val (historyMessages, currentResponse) =
                        when {
                            uiState.currentResponse.isNotEmpty() -> {
                                // Show empty history and current response while streaming
                                Log.d(TAG, "Response is not empty, will show bubble")
                                emptyList<ChatMessage>() to uiState.currentResponse
                            }

                            else -> {
                                Log.d(TAG, "Response is empty, no bubble")
                                emptyList<ChatMessage>() to ""
                            }
                        }

                    items(historyMessages) { message ->
                        MessageBubble(message)
                    }

                    if (currentResponse.isNotEmpty()) {
                        Log.d(TAG, "Adding streaming bubble to LazyColumn")
                        item(key = "streaming_response") {
                            Log.d(TAG, "Inside item lambda, rendering bubble")
                            StreamingResponseBubble(
                                text = currentResponse,
                                isStreaming = true,
                            )
                        }
                    }
                }

                is ChatUiState.Success -> {
                    items(uiState.allMessages) { message ->
                        MessageBubble(message)
                    }
                }

                is ChatUiState.Error -> {
                    item {
                        ErrorMessage(uiState.message)
                    }
                }
            }
        }

        // Floating input at the bottom
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFFF5F5F5)),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            ChatInputField(
                onSendMessage = onSendMessage,
                onClearConversation = onClearConversation,
                isLoading = uiState is ChatUiState.Loading,
            )
        }
    }
}
