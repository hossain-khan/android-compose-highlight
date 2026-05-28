@file:OptIn(ExperimentalHighlightApi::class)

package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.SyntaxHighlightedTextEditor

private val DEMO_LANGUAGES =
    listOf(
        "kotlin",
        "javascript",
        "json",
    )

private val INITIAL_CODE_BY_LANGUAGE =
    mapOf(
        "kotlin" to
            """
fun greet(name: String): String {
    return "Hello, ${'$'}name!"
}

data class User(val id: Int, val name: String)

fun main() {
    val user = User(1, "Alice")
    println(greet(user.name))
}
            """.trimIndent(),
        "python" to
            """
def greet(name: str) -> str:
    return f"Hello, {name}!"

class User:
    def __init__(self, id: int, name: str):
        self.id = id
        self.name = name

if __name__ == "__main__":
    user = User(1, "Alice")
    print(greet(user.name))
            """.trimIndent(),
        "javascript" to
            """
function greet(name) {
    return `Hello, ${'$'}{name}!`;
}

class User {
    constructor(id, name) {
        this.id = id;
        this.name = name;
    }
}

const user = new User(1, "Alice");
console.log(greet(user.name));
            """.trimIndent(),
        "sql" to
            """
SELECT
    u.id,
    u.name,
    COUNT(o.id) AS order_count
FROM users u
LEFT JOIN orders o ON o.user_id = u.id
WHERE u.active = 1
GROUP BY u.id, u.name
ORDER BY order_count DESC
LIMIT 10;
            """.trimIndent(),
        "json" to
            """
{
  "user": {
    "id": 1,
    "name": "Alice",
    "active": true,
    "roles": ["admin", "editor"],
    "address": {
      "city": "Vancouver",
      "country": "Canada"
    }
  }
}
            """.trimIndent(),
        "xml" to
            """
<?xml version="1.0" encoding="UTF-8"?>
<users>
  <user id="1" active="true">
    <name>Alice</name>
    <roles>
      <role>admin</role>
      <role>editor</role>
    </roles>
  </user>
</users>
            """.trimIndent(),
    )

/**
 * Demonstrates [SyntaxHighlightedTextEditor] - a live syntax-highlighting code editor.
 *
 * Shows a language selector and an editable [SyntaxHighlightedTextEditor] field. As the
 * user types, syntax highlighting updates in real time with a short debounce.
 */
@Composable
internal fun LiveEditorSection() {
    var selectedLanguage by rememberSaveable { mutableStateOf("kotlin") }
    var editorValue by remember(selectedLanguage) {
        mutableStateOf(TextFieldValue(INITIAL_CODE_BY_LANGUAGE[selectedLanguage] ?: ""))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubSectionHeader("Live Syntax Highlighting Editor")

        // Experimental feature info banner
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = "\u2139\ufe0f Experimental - This feature uses @ExperimentalHighlightApi and may change in future releases.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        Text(
            text = "Edit the code below - syntax highlighting updates as you type.",
            style = MaterialTheme.typography.bodySmall,
        )

        // Language selector chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DEMO_LANGUAGES.forEach { lang ->
                FilterChip(
                    selected = lang == selectedLanguage,
                    onClick = { selectedLanguage = lang },
                    label = { Text(lang) },
                )
            }
        }

        // Live editor
        SyntaxHighlightedTextEditor(
            value = editorValue,
            onValueChange = { editorValue = it },
            language = selectedLanguage,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp),
                    ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(12.dp),
            textStyle =
                TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                ),
        )
    }
}
