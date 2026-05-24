package dev.hossain.highlight.sample.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.sample.chat.model.ChatMessage
import dev.hossain.highlight.sample.chat.state.ChatUiState
import dev.hossain.highlight.sample.chat.state.ChatViewModel

/** Programming languages available in the language selector dropdown. */
private val SUPPORTED_LANGUAGES =
    listOf(
        "kotlin",
        "java",
        "python",
        "javascript",
        "typescript",
        "swift",
        "go",
        "rust",
        "cpp",
        "csharp",
        "bash",
    )

/**
 * Main chat screen composable.
 *
 * Displays:
 * - Conversation history in a scrollable [LazyColumn]
 * - Streaming response bubble with animated cursor while the API responds
 * - Language selector dropdown for choosing the coding language context
 * - Input field with 2000-character limit and Send button
 * - Error banner for API/network errors
 * - Clear conversation button to reset the chat
 *
 * @param viewModel The [ChatViewModel] managing conversation state.
 * @param modifier Optional [Modifier] for the root column.
 */
@Composable
internal fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var inputText by rememberSaveable { mutableStateOf("") }
    var selectedLanguage by rememberSaveable { mutableStateOf("kotlin") }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val isStreaming = viewModel.uiState == ChatUiState.Streaming

    // Auto-scroll to the bottom when new messages arrive or streaming content updates
    LaunchedEffect(viewModel.messages.size, viewModel.streamingContent) {
        val totalItems = viewModel.messages.size + (if (viewModel.streamingContent.isNotEmpty() || isStreaming) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // ── Header row: language selector + clear button ──────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                OutlinedButton(
                    onClick = { showLanguageMenu = true },
                ) {
                    Text(selectedLanguage)
                }
                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false },
                ) {
                    SUPPORTED_LANGUAGES.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang) },
                            onClick = {
                                selectedLanguage = lang
                                showLanguageMenu = false
                            },
                        )
                    }
                }
            }
            TextButton(
                onClick = { viewModel.clearConversation() },
                enabled = viewModel.messages.isNotEmpty() || viewModel.streamingContent.isNotEmpty(),
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("Clear")
            }
        }

        HorizontalDivider()

        // ── Error banner ──────────────────────────────────────────────────
        val currentState = viewModel.uiState
        if (currentState is ChatUiState.Error) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currentState.message,
                        style =
                            TextStyle(
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.dismissError() }) {
                        Text(
                            text = "Dismiss",
                            style = TextStyle(color = MaterialTheme.colorScheme.onErrorContainer),
                        )
                    }
                }
            }
        }

        // ── Conversation list ─────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (viewModel.messages.isEmpty() && !isStreaming) {
                item {
                    EmptyConversationHint()
                }
            }

            var lastRole: ChatMessage.MessageRole? = null
            viewModel.messages.forEachIndexed { index, msg ->
                if (msg.role != lastRole) {
                    item(key = "label-${msg.role}-$index") {
                        RoleLabel(role = msg.role)
                    }
                }
                item(key = "msg-$index") {
                    MessageBubble(message = msg)
                }
                lastRole = msg.role
            }

            // Show the streaming bubble when a response is in progress
            if (isStreaming || viewModel.streamingContent.isNotEmpty()) {
                item(key = "streaming-label") {
                    RoleLabel(role = ChatMessage.MessageRole.ASSISTANT)
                }
                item(key = "streaming-bubble") {
                    StreamingBubble(content = viewModel.streamingContent, isStreaming = isStreaming)
                }
            }
        }

        HorizontalDivider()

        // ── Input area ────────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(4.dp))
        InfoLabel(selectedLanguage = selectedLanguage)
        ChatInputField(
            value = inputText,
            onValueChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(
                        prompt = inputText.trim(),
                        language = selectedLanguage,
                    )
                    inputText = ""
                }
            },
            isEnabled = !isStreaming,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun EmptyConversationHint() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Ask an AI about coding",
            style =
                TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
        Text(
            text = "Select a language and type a programming question.\nThe AI will respond with streaming tokens via SSE.",
            style =
                TextStyle(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                ),
        )
        Text(
            text = "Rate limit: 150 req/day. Max prompt: 2000 chars.",
            style =
                TextStyle(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                ),
        )
    }
}

@Composable
private fun InfoLabel(selectedLanguage: String) {
    Text(
        text = "Language: $selectedLanguage",
        style =
            TextStyle(
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
            ),
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
