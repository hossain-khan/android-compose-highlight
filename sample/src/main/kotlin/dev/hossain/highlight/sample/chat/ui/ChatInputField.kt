package dev.hossain.highlight.sample.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Maximum number of characters allowed in the prompt input. */
internal const val MAX_PROMPT_LENGTH = 2000

/**
 * Chat input area consisting of a multi-line text field and a send button.
 *
 * Enforces a [MAX_PROMPT_LENGTH] character limit and shows a counter below the field.
 * The send button is disabled while [isEnabled] is false (e.g., during streaming).
 *
 * @param value The current text field value.
 * @param onValueChange Called when the text changes.
 * @param onSend Called when the user taps Send or submits via the keyboard.
 * @param isEnabled Whether the input and send button are interactive.
 * @param modifier Optional [Modifier] for the outer column.
 */
@Composable
internal fun ChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                if (input.length <= MAX_PROMPT_LENGTH) onValueChange(input)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask a programming question...") },
            minLines = 2,
            maxLines = 5,
            enabled = isEnabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (value.isNotBlank()) onSend() }),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${value.length} / $MAX_PROMPT_LENGTH",
                style =
                    TextStyle(
                        fontSize = 11.sp,
                        color =
                            if (value.length >= MAX_PROMPT_LENGTH) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                    ),
            )
            Button(
                onClick = onSend,
                enabled = isEnabled && value.isNotBlank(),
            ) {
                Text("Send")
            }
        }
    }
}
