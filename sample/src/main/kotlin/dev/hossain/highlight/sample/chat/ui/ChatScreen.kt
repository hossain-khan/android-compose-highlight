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

    // Pre-compute display items so we can use the items() DSL and derive the scroll target.
    // Declared before the auto-scroll LaunchedEffect so displayItems.size can be used directly.
    val displayItems =
        remember(viewModel.messages, isStreaming, viewModel.streamingContent) {
            buildDisplayItems(
                messages = viewModel.messages,
                isStreaming = isStreaming,
                streamingContent = viewModel.streamingContent,
            )
        }

    // Auto-scroll to the bottom when new messages arrive or streaming content updates.
    // Uses displayItems.size to avoid duplicating the item-count logic.
    LaunchedEffect(displayItems.size) {
        if (displayItems.isNotEmpty()) {
            listState.animateScrollToItem(displayItems.size - 1)
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
            if (displayItems.isEmpty()) {
                item(key = "empty-hint") {
                    EmptyConversationHint()
                }
            } else {
                items(items = displayItems, key = { it.key }) { displayItem ->
                    when (displayItem) {
                        is ChatDisplayItem.RoleLabel -> {
                            RoleLabel(role = displayItem.role)
                        }

                        is ChatDisplayItem.Message -> {
                            MessageBubble(message = displayItem.message)
                        }

                        is ChatDisplayItem.StreamingLabel -> {
                            RoleLabel(role = ChatMessage.MessageRole.ASSISTANT)
                        }

                        is ChatDisplayItem.Streaming -> {
                            StreamingBubble(
                                content = displayItem.content,
                                isStreaming = isStreaming,
                            )
                        }
                    }
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

/**
 * Represents a single renderable item in the chat [LazyColumn].
 *
 * Separating the display model from the raw [ChatMessage] list lets us
 * pre-compute role labels and the streaming row with stable, unique keys
 * before passing everything to the `items()` DSL.
 */
private sealed class ChatDisplayItem {
    abstract val key: String

    data class RoleLabel(
        val role: ChatMessage.MessageRole,
        override val key: String,
    ) : ChatDisplayItem()

    data class Message(
        val message: ChatMessage,
        override val key: String,
    ) : ChatDisplayItem()

    data object StreamingLabel : ChatDisplayItem() {
        override val key = "streaming-label"
    }

    data class Streaming(
        val content: String,
    ) : ChatDisplayItem() {
        override val key = "streaming-bubble"
    }
}

/**
 * Builds the ordered list of [ChatDisplayItem]s from the current conversation state.
 *
 * Role labels are inserted only when the sender changes between consecutive messages,
 * reducing visual repetition.
 */
private fun buildDisplayItems(
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    streamingContent: String,
): List<ChatDisplayItem> {
    val items = mutableListOf<ChatDisplayItem>()
    var lastRole: ChatMessage.MessageRole? = null
    messages.forEachIndexed { index, msg ->
        if (msg.role != lastRole) {
            items.add(ChatDisplayItem.RoleLabel(msg.role, "label-${msg.role}-$index"))
        }
        items.add(ChatDisplayItem.Message(msg, "msg-$index"))
        lastRole = msg.role
    }
    if (isStreaming || streamingContent.isNotEmpty()) {
        items.add(ChatDisplayItem.StreamingLabel)
        items.add(ChatDisplayItem.Streaming(streamingContent))
    }
    return items
}
