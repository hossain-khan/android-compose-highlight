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

internal val KOTLIN_EXTENDED_SNIPPET =
    """
data class User(val name: String, val age: Int)

fun List<User>.adults(): List<User> =
    filter { it.age >= 18 }

// Sample API endpoint to manage users
const val API = "https://api.example.com"

val httpApiClient = MyHttpClient(API)
httpApiClient.get("/users")
    .onSuccess { response ->
        println("Fetched users: ${'$'}{response.body}")
    }
    .onFailure { error ->
        println("Error fetching users: ${'$'}{error.message}")
    }
    """.trimIndent()

/**
 * Kotlin snippet demonstrating [HighlightTheme.fromAsset] — intentionally shown
 * inside a [dev.hossain.highlight.ui.SyntaxHighlightedCode] block so the demo
 * highlights its own loading code ("eat your own dog food").
 */
internal val FROM_ASSET_SNIPPET =
    """
// Save any highlight.js CSS theme file in your app's assets:
//   app/src/main/assets/themes/dracula.min.css

val theme = HighlightTheme.fromAsset(
    context   = context.applicationContext,
    assetPath = "themes/dracula.min.css",
    name      = "dracula",
)

// Pass the theme directly to the composable
SyntaxHighlightedCode(
    code     = sourceCode,
    language = "kotlin",
    theme    = theme,
)

// Or wrap a subtree so all code blocks share one theme
HighlightThemeProvider(
    lightHighlightTheme = lightTheme,
    darkHighlightTheme  = theme,
) {
    SyntaxHighlightedCode(code = sourceCode, language = "kotlin")
    SyntaxHighlightedCode(code = otherCode,  language = "python")
}
    """.trimIndent()

internal val JAVASCRIPT_EXTENDED_SNIPPET =
    """
// --- Classes & inheritance ---
class Animal {
  #name; // private field
  constructor(name) { this.#name = name; }
  get name() { return this.#name; }
  speak() { return `${'$'}{this.#name} makes a noise.`; }
}

class Dog extends Animal {
  speak() { return `${'$'}{this.name} barks.`; }
}

// --- Destructuring, spread, rest ---
const { name: dogName, ...rest } = { name: "Rex", age: 4, breed: "Lab" };
const merged = { ...rest, owner: "Alice" };

// --- Async / await with error handling ---
const API_URL = "https://api.example.com";

async function fetchUsers(page = 1) {
  try {
    const res = await fetch(`${'$'}{API_URL}/users?page=${'$'}{page}`);
    if (!res.ok) throw new Error(`HTTP ${'$'}{res.status}`);
    const { data, total } = await res.json();
    return data.filter(u => u.active).map(({ id, email }) => ({ id, email }));
  } catch (err) {
    console.error("fetchUsers failed:", err.message);
    return [];
  }
}

// --- Generator function ---
function* range(start, end, step = 1) {
  for (let i = start; i < end; i += step) yield i;
}

// --- Promises & chaining ---
Promise.all([fetchUsers(1), fetchUsers(2)])
  .then(([page1, page2]) => [...page1, ...page2])
  .then(all => all.sort((a, b) => a.id - b.id))
  .catch(console.error)
  .finally(() => console.log("done"));

// --- Proxy & Reflect ---
const handler = {
  get(target, key) {
    return key in target ? target[key] : `unknown key: ${'$'}{key}`;
  },
};
const safe = new Proxy({}, handler);

// --- Tagged template literal ---
function highlight(strings, ...values) {
  return strings.reduce((acc, str, i) =>
    `${'$'}{acc}${'$'}{str}<b>${'$'}{values[i] ?? ""}</b>`, "");
}
const msg = highlight`Hello, ${'$'}{"world"}! You have ${'$'}{42} messages.`;

// --- Symbol & WeakMap ---
const _cache = new WeakMap();
const ID = Symbol("id");

export { Animal, Dog, fetchUsers, range };
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
