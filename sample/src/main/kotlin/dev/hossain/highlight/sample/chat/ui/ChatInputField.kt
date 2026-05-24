package dev.hossain.highlight.sample.chat.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TAG = "ChatInputField"

/**
 * Language selector dropdown component.
 *
 * @param selectedLanguage Currently selected language or null
 * @param onLanguageChange Callback when language selection changes
 * @param isEnabled Whether the selector is enabled
 */
@Composable
fun LanguageSelector(
    selectedLanguage: String?,
    onLanguageChange: (String?) -> Unit,
    isEnabled: Boolean = true,
) {
    var showMenu by remember { mutableStateOf(false) }

    val languages =
        listOf(
            "kotlin",
            "python",
            "javascript",
            "typescript",
            "java",
            "go",
            "rust",
            "swift",
            "csharp",
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Language: ${selectedLanguage ?: "Any"}",
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp),
        )

        Box {
            OutlinedButton(
                onClick = { showMenu = true },
                enabled = isEnabled,
            ) {
                Text(selectedLanguage ?: "Select Language")
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language) },
                        onClick = {
                            onLanguageChange(language)
                            showMenu = false
                        },
                    )
                }

                DropdownMenuItem(
                    text = { Text("Clear") },
                    onClick = {
                        onLanguageChange(null)
                        showMenu = false
                    },
                )
            }
        }
    }
}

/**
 * Chat input field component with send and clear buttons.
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
    val charCount = input.length
    val maxChars = 2000

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    .fillMaxWidth(),
            placeholder = { Text("Ask about programming...") },
            label = { Text("Question") },
            enabled = !isLoading,
            maxLines = 4,
            supportingText = {
                Text(
                    text = "$charCount / $maxChars",
                    fontSize = 12.sp,
                )
            },
        )

        // Buttons row
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    Log.d(TAG, "Send button clicked with input: '$input'")
                    onSendMessage(input)
                    input = ""
                },
                enabled = input.isNotEmpty() && !isLoading,
                modifier = Modifier.weight(1f),
            ) {
                Text("Send")
            }

            OutlinedButton(
                onClick = {
                    onClearConversation()
                    input = ""
                },
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
            ) {
                Text("Clear")
            }
        }
    }
}
