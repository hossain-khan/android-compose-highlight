package dev.hossain.highlight.sample.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Chat input field component with send button.
 * Styled like production chat apps (WhatsApp, Telegram) with floating bottom input.
 *
 * @param onSendMessage Callback when send button is pressed
 * @param onClearConversation Callback when clear conversation button is pressed
 * @param isLoading Whether a message is currently being processed
 */
@Composable
fun ChatInputField(
    onSendMessage: (String) -> Unit,
    onClearConversation: () -> Unit,
    isLoading: Boolean = false,
) {
    var input by remember { mutableStateOf("") }
    val maxChars = 2000

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                ).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Input field
        OutlinedTextField(
            value = input,
            onValueChange = { newValue ->
                if (newValue.length <= maxChars) {
                    input = newValue
                }
            },
            modifier =
                Modifier
                    .weight(1f),
            placeholder = { Text("Message...") },
            enabled = !isLoading,
            maxLines = 3,
            singleLine = false,
        )

        // Send button with icon
        IconButton(
            onClick = {
                onSendMessage(input)
                input = ""
            },
            enabled = input.isNotEmpty() && !isLoading,
            modifier = Modifier.padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send message",
                tint = Color(0xFF00BCD4),
            )
        }
    }
}
