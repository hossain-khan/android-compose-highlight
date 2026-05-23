# HighlightEngine

The core engine that manages the hidden WebView and executes Highlight.js highlighting.

For most use cases, use `SyntaxHighlightedCode` inside a `HighlightThemeProvider` - it handles the engine lifecycle automatically. Use `HighlightEngine` directly only when you need lower-level control, such as highlighting in a ViewModel or outside of Compose.

## Lifecycle

The engine holds a hidden WebView resource and implements `Closeable`. Always call `close()` (or `destroy()`) when done. Inside Compose, use `rememberHighlightEngine()` which calls `destroy()` automatically via `DisposableEffect`. Since highlighting APIs are `suspend`, prefer coroutine-friendly `try/finally` cleanup for scoped usage:

```kotlin
val engine = HighlightEngine(context.applicationContext)
try {
    val result = engine.highlight(code, "kotlin", theme)
} finally {
    engine.close()
}
```

## Methods

| Method | Description |
|---|---|
| `suspend initialize(): Result<Unit>` | Warms up the WebView. Optional - `highlight()` also initializes on first call. Returns `Result.failure(WebViewInitFailed(...))` if WebView is unavailable |
| `suspend highlight(code, language, theme)` | Full pipeline: tokenize, apply theme, return `Result<HighlightResult>` (access `.annotated` for the `AnnotatedString`) |
| `suspend highlightBothThemes(code, language, lightTheme, darkTheme)` | Highlight once, return `Result<ThemedHighlightResult>` (access `.light` and `.dark` for the `AnnotatedString` variants) |
| `suspend highlightToHtml(code, language)` | Returns `Result<HtmlHighlightResult>` (access `.html` for the HTML string, `.jsBridgeDuration` and `.jsonUnescapeDuration` for timing) |
| `suspend supportedLanguages()` | Returns the list of languages the bundled Highlight.js supports |
| `suspend highlightJsVersion()` | Returns the bundled Highlight.js version string |
| `suspend getLanguage(nameOrAlias)` | Returns `Result<HighlightLanguageInfo?>` - null success means language not recognized |
| `suspend highlightAuto(code, theme)` | Auto-detect language and highlight, returns `Result<AutoHighlightResult>` |
| `fun destroy()` | Releases the WebView and clears caches. Idempotent - safe to call multiple times |
| `fun close()` | Alias for `destroy()`. Implements `Closeable` for IDE resource-leak inspections and explicit cleanup |
| `val isInitialized: StateFlow<Boolean>` | Observe WebView readiness reactively |

All suspend methods return `Result<T>` - never throw. Wrap failures in `HighlightException`.

## Usage in a ViewModel

```kotlin
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightTheme

class CodeViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = HighlightEngine(application.applicationContext)

    init {
        viewModelScope.launch {
            // Optional warm-up to reduce first-call latency
            engine.initialize()
        }
    }

    suspend fun highlight(code: String, language: String, theme: HighlightTheme): AnnotatedString? {
        return engine.highlight(code, language, theme)
            .getOrNull()
            ?.annotated
    }

    override fun onCleared() {
        engine.close()
    }
}
```

## Usage in Compose (lower-level)

```kotlin
import dev.hossain.highlight.ui.rememberHighlightEngine
import dev.hossain.highlight.ui.rememberTomorrowTheme

@Composable
fun MyCodeBlock(code: String) {
    val engine = rememberHighlightEngine()
    val theme  = rememberTomorrowTheme()
    var highlighted by remember(code) { mutableStateOf<AnnotatedString?>(null) }

    LaunchedEffect(code) {
        engine.highlight(code, "kotlin", theme).onSuccess { highlighted = it.annotated }
    }
    Text(text = highlighted ?: AnnotatedString(code))
}
```

## Highlight both themes (instant switching)

Tokenizes once, applies two color maps - theme switching has zero extra latency:

```kotlin
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.hossain.highlight.ui.rememberTomorrowTheme

engine.highlightBothThemes(
    code       = sourceCode,
    language   = "typescript",
    lightTheme = rememberTomorrowTheme(),
    darkTheme  = rememberTomorrowNightTheme(),
).onSuccess { result ->
    val display = if (isDark) result.dark else result.light
}
```

## `HighlightResult`

| Property | Description |
|---|---|
| `annotated` | The highlighted `AnnotatedString` |
| `spanCount` | Number of style spans. `0` = silent failure (unsupported language or empty code) |
| `language` | The language identifier that was highlighted |
| `durationMs` | Total wall-clock time in milliseconds |
| `timings` | Per-stage `HighlightTimings` (jsBridge, jsonUnescape, htmlParse, treeWalk, themeParse, total) |

## `ThemedHighlightResult`

Returned by `highlightBothThemes()`. Holds both light and dark variants produced from a single JS tokenization pass.

| Property | Description |
|---|---|
| `light` | Syntax-highlighted `AnnotatedString` styled with the light theme |
| `dark` | Syntax-highlighted `AnnotatedString` styled with the dark theme |
| `durationMs` | Total wall-clock time in milliseconds for the full highlight call |
| `timings` | Per-stage `HighlightTimings` breakdown (same fields as `HighlightResult.timings`) |

## `HighlightLanguageInfo`

Returned by `getLanguage()`. Contains the human-readable display name and registered aliases for a language.

| Property | Description |
|---|---|
| `name` | Human-readable display name (e.g. `"Kotlin"`, `"Python"`) - not the language identifier |
| `aliases` | Registered Highlight.js aliases (e.g. `["kt", "kts"]`) |

```kotlin
engine.getLanguage("kt").onSuccess { info ->
    if (info != null) {
        println("Name: ${info.name}")       // "Kotlin"
        println("Aliases: ${info.aliases}") // [kt, kts]
    } else {
        println("Language not found")
    }
}
```

## `AutoHighlightResult`

Returned by `highlightAuto()`. Contains the highlighted output and the language hljs detected.

| Property | Description |
|---|---|
| `annotated` | The highlighted `AnnotatedString` |
| `detectedLanguage` | Language hljs guessed (may be an empty string if detection failed) |
| `spanCount` | Number of style spans applied |
| `durationMs` | Total wall-clock time in milliseconds |
| `timings` | Per-stage `HighlightTimings` breakdown |

## Language lookup and auto-detection

### `getLanguage()`

```kotlin
// Check if a language is recognized and inspect its aliases
engine.getLanguage("ts").onSuccess { info ->
    if (info != null) {
        // info.name    = "TypeScript"
        // info.aliases = [ts, tsx, mts, cts]
    }
}
```

### `highlightAuto()`

Use when the language is unknown - hljs will attempt to detect it:

```kotlin
engine.highlightAuto(code, theme).onSuccess { result ->
    val language = result.detectedLanguage // e.g. "python" or "" if undetected
    Text(text = result.annotated)
}
```

## `rememberHighlightEngine`

```kotlin
@Composable
fun rememberHighlightEngine(): HighlightEngine
```

- **Inside `HighlightThemeProvider`**: returns the shared engine (no extra WebView).
- **Outside a provider**: creates a standalone engine, destroys it when the composable leaves composition.
