package dev.hossain.highlight.sample.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.sample.chat.model.ChatMessage
import kotlinx.coroutines.delay

/**
 * Displays a single chat message bubble styled according to the message role.
 *
 * User messages are right-aligned with a primary color background.
 * Assistant messages are left-aligned with a surface variant background.
 *
 * @param message The [ChatMessage] to display.
 * @param modifier Optional [Modifier] for the outer row.
 */
@Composable
internal fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == ChatMessage.MessageRole.USER
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape =
                RoundedCornerShape(
                    topStart = if (isUser) 12.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 12.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp,
                ),
            color =
                if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        fontFamily = if (isUser) FontFamily.Default else FontFamily.Monospace,
                        color =
                            if (isUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    ),
            )
        }
    }
}

/**
 * Displays a streaming assistant response bubble with an animated cursor
 * that blinks while content is still being received.
 *
 * @param content The text accumulated so far from the SSE stream.
 * @param modifier Optional [Modifier] for the outer row.
 */
@Composable
internal fun StreamingBubble(
    content: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (content.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp).align(Alignment.Center),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    var showCursor by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(500)
                            showCursor = !showCursor
                        }
                    }
                    Text(
                        text = if (showCursor) "$content▋" else content,
                        style =
                            TextStyle(
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                    )
                }
            }
        }
    }
}

/**
 * Displays a role label (e.g., "You" or "AI") above the message bubble area.
 */
@Composable
internal fun RoleLabel(
    role: ChatMessage.MessageRole,
    modifier: Modifier = Modifier,
) {
    val isUser = role == ChatMessage.MessageRole.USER
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = if (isUser) "You" else "AI",
            style =
                TextStyle(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                ),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
