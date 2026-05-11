package dev.hossain.highlight.sample

import android.content.Context
import dev.hossain.highlight.engine.HighlightTheme

/**
 * A single syntax-highlighting demo sample.
 *
 * @param language Highlight.js language identifier (e.g. `"kotlin"`, `"python"`).
 * @param displayLabel Human-readable label shown in the UI (e.g. `"WeatherApp.kt"`).
 * @param code The source code to highlight.
 */
internal data class CodeSample(
    val language: String,
    val displayLabel: String,
    val code: String,
)

/**
 * Loads all demo code samples from `assets/samples/`.
 *
 * Files are named with a numeric prefix for ordering (e.g. `01_fibonacci.py`).
 * The prefix is stripped for the display label. The file extension is mapped to
 * a Highlight.js language identifier via [extensionToLanguage].
 *
 * Adding a new sample only requires dropping a file into `assets/samples/` —
 * no Kotlin changes needed.
 */
internal fun loadCodeSamples(context: Context): List<CodeSample> =
    context.assets
        .list("samples")
        .orEmpty()
        .sorted()
        .mapNotNull { filename ->
            runCatching {
                val code =
                    context.assets
                        .open("samples/$filename")
                        .bufferedReader()
                        .use { it.readText() }
                val ext = filename.substringAfterLast(".", missingDelimiterValue = "")
                val language = extensionToLanguage(ext)
                val displayLabel = filename.substringAfter("_")
                CodeSample(language = language, displayLabel = displayLabel, code = code)
            }.getOrNull()
        }

private fun extensionToLanguage(ext: String): String =
    when (ext) {
        "py" -> "python"
        "kt" -> "kotlin"
        "js" -> "javascript"
        "java" -> "java"
        "sql" -> "sql"
        "json" -> "json"
        "xml" -> "xml"
        "ts" -> "typescript"
        "rs" -> "rust"
        "go" -> "go"
        "swift" -> "swift"
        "cpp" -> "cpp"
        "cs" -> "csharp"
        "sh" -> "bash"
        "css" -> "css"
        "txt" -> "plaintext"
        else -> ext
    }

/** A named pair of light/dark [HighlightTheme]s for the theme picker. */
internal data class ThemePair(
    val name: String,
    val light: HighlightTheme,
    val dark: HighlightTheme,
)

// Short snippets used in demo sections (not loaded from assets — intentionally inline).
internal val KOTLIN_SNIPPET =
    """
data class User(val name: String, val age: Int)

fun List<User>.adults(): List<User> =
    filter { it.age >= 18 }
    """.trimIndent()

internal val PYTHON_SNIPPET =
    """
def fibonacci(n: int) -> int:
    if n <= 1:
        return n
    a, b = 0, 1
    for _ in range(n - 1):
        a, b = b, a + b
    return b
    """.trimIndent()
