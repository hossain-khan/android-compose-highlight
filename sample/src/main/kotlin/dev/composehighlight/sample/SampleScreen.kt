package dev.composehighlight.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.composehighlight.engine.HighlightTheme
import dev.composehighlight.ui.HighlightThemeProvider
import dev.composehighlight.ui.SyntaxHighlightedCode

private val SAMPLES = listOf(
    "python" to """
def fibonacci(n: int) -> int:
    # Returns the nth Fibonacci number
    if n <= 1:
        return n
    a, b = 0, 1
    for _ in range(n - 1):
        a, b = b, a + b
    return b

# Edge case: special chars  \t \n ' "
result = fibonacci(10)
print(f"Result: {result}")
    """.trimIndent(),

    "kotlin" to """
data class User(val name: String, val age: Int)

fun List<User>.filter(minAge: Int): List<User> =
    filter { it.age >= minAge }

// Unicode: héllo wörld 🌍
val users = listOf(
    User("Alice", 30),
    User("Bob", 25),
)

val adults = users.filter(18)
println(adults)
    """.trimIndent(),

    "javascript" to """
async function fetchUser(id) {
    const response = await fetch(`/api/users/${'$'}{id}`);
    if (!response.ok) {
        throw new Error(`HTTP error: ${'$'}{response.status}`);
    }
    return response.json();
}

// Backslash path: C:\Users\test
const path = 'C:\\Users\\test\\file.txt';
    """.trimIndent(),

    "java" to """
public class BinarySearch {
    public static int search(int[] arr, int target) {
        int left = 0, right = arr.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (arr[mid] == target) return mid;
            if (arr[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return -1;
    }
}
    """.trimIndent(),

    "sql" to """
SELECT
    u.id,
    u.name,
    COUNT(o.id) AS order_count,
    SUM(o.total) AS revenue
FROM users u
LEFT JOIN orders o ON o.user_id = u.id
WHERE u.created_at >= '2024-01-01'
GROUP BY u.id, u.name
HAVING order_count > 0
ORDER BY revenue DESC
LIMIT 10;
    """.trimIndent(),

    "json" to """
{
    "name": "compose-highlight",
    "version": "0.1.0",
    "dependencies": {
        "highlight.js": "^11.11.1"
    },
    "keywords": ["android", "compose", "syntax-highlight"],
    "unicode": "héllo 🌍",
    "escapes": "line1\nline2\ttabbed"
}
    """.trimIndent(),

    "xml" to """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Compose Highlight</string>
    <style name="Theme.App" parent="Theme.Material3.DayNight">
        <item name="colorPrimary">@color/purple_500</item>
    </style>
</resources>
    """.trimIndent(),

    "plaintext" to "",  // empty edge case
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleScreen() {
    val context = LocalContext.current
    var isDark by remember { mutableStateOf(false) }

    HighlightThemeProvider(
        lightHighlightTheme = HighlightTheme.atomOneLight(context),
        darkHighlightTheme = HighlightTheme.atomOneDark(context),
        darkTheme = isDark,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("compose-highlight demo") },
                    actions = {
                        Button(
                            onClick = { isDark = !isDark },
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(if (isDark) "☀ Light" else "🌙 Dark")
                        }
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SAMPLES.forEach { (language, code) ->
                    item(key = language) {
                        if (code.isEmpty()) {
                            SectionHeader("$language (empty edge case)")
                            SyntaxHighlightedCode(
                                code = "",
                                language = language,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            SectionHeader(language)
                            SyntaxHighlightedCode(
                                code = code,
                                language = language,
                                modifier = Modifier.fillMaxWidth(),
                                showLineNumbers = language == "python",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title.uppercase())
    }
}
