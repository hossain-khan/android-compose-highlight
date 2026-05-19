# HighlightEngine

The core engine that manages the hidden WebView and executes Highlight.js highlighting.

For most use cases, use `SyntaxHighlightedCode` inside a `HighlightThemeProvider` — it handles the engine lifecycle automatically. Use `HighlightEngine` directly only when you need lower-level control, such as highlighting in a ViewModel or outside of Compose.

## Lifecycle

The engine holds a hidden WebView resource. Always call `destroy()` when done. Inside Compose, use `rememberHighlightEngine()` which calls `destroy()` automatically via `DisposableEffect`.

## Methods

| Method | Description |
|---|---|
| `suspend initialize()` | Warms up the WebView. Optional — `highlight()` also initializes on first call |
| `suspend highlight(code, language, theme)` | Full pipeline: tokenize, apply theme, return `Result<HighlightResult>` (access `.annotated` for the `AnnotatedString`) |
| `suspend highlightBothThemes(code, language, lightTheme, darkTheme)` | Highlight once, return `Result<ThemedHighlightResult>` (access `.light` and `.dark` for the `AnnotatedString` variants) |
| `suspend highlightToHtml(code, language)` | Returns `Result<HtmlHighlightResult>` (access `.html` for the HTML string, `.jsBridgeDuration` and `.jsonUnescapeDuration` for timing) |
| `suspend supportedLanguages()` | Returns the list of languages the bundled Highlight.js supports |
| `suspend highlightJsVersion()` | Returns the bundled Highlight.js version string |
| `fun destroy()` | Releases the WebView and clears caches |
| `val isInitialized: StateFlow<Boolean>` | Observe WebView readiness reactively |

All suspend methods return `Result<T>` — never throw. Wrap failures in `HighlightException`.

## Usage in a ViewModel

```kotlin
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightTheme
import kotlinx.coroutines.launch

class CodeViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = HighlightEngine(application)

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
        engine.destroy()
    }
}
```

## Usage in Compose (lower-level)

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
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

Tokenizes once, applies two color maps — theme switching has zero extra latency:

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

## `rememberHighlightEngine`

```kotlin
@Composable
fun rememberHighlightEngine(): HighlightEngine
```

- **Inside `HighlightThemeProvider`**: returns the shared engine (no extra WebView).
- **Outside a provider**: creates a standalone engine, destroys it when the composable leaves composition.
