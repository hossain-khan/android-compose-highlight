# Engine-only Usage

Use `HighlightEngine` directly when you need to:

- Highlight code in a ViewModel or repository layer (outside of Compose)
- Produce `AnnotatedString` for reuse across multiple widgets
- Access raw highlighted HTML for custom rendering
- Query engine metadata (supported languages, Highlight.js version)

## ViewModel example

```kotlin
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightTheme

class CodeViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = HighlightEngine(application.applicationContext)

    private val _highlighted = MutableStateFlow<AnnotatedString?>(null)
    val highlighted: StateFlow<AnnotatedString?> = _highlighted.asStateFlow()

    init {
        viewModelScope.launch { engine.initialize() }
    }

    fun highlight(code: String, language: String, theme: HighlightTheme) {
        viewModelScope.launch {
            engine.highlight(code, language, theme).onSuccess { result ->
                _highlighted.value = result.annotated
            }
        }
    }

    override fun onCleared() {
        engine.close()
    }
}
```

## Using the result in Compose

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults
import dev.hossain.highlight.ui.rememberTomorrowNightTheme

@Composable
fun CodeScreen(code: String, viewModel: CodeViewModel = viewModel()) {
    val highlighted by viewModel.highlighted.collectAsState()
    val theme = rememberTomorrowNightTheme()

    // Ask the ViewModel to highlight whenever the code changes
    LaunchedEffect(code, theme) { viewModel.highlight(code, "kotlin", theme) }

    SelectionContainer {
        Text(
            text  = highlighted ?: AnnotatedString("loading..."),
            style = SyntaxHighlightedCodeDefaults.codeTextStyle,
        )
    }
}
```

## Raw HTML output

Get the raw Highlight.js HTML tokens for custom rendering pipelines:

```kotlin
engine.highlightToHtml("val x = 42", "kotlin").onSuccess { result ->
    // result.html: "<span class=\"hljs-keyword\">val</span> x = <span class=\"hljs-number\">42</span>"
    Log.d("HTML", result.html)
}
```

## Querying engine metadata

```kotlin
engine.supportedLanguages().onSuccess { languages ->
    Log.d("Engine", "Supports ${languages.size} languages")
}

engine.highlightJsVersion().onSuccess { version ->
    Log.d("Engine", "highlight.js $version")
}
```

## Error handling

All suspend methods return `Result<T>`. Failures are wrapped in `HighlightException`:

```kotlin
import dev.hossain.highlight.engine.HighlightException

engine.highlight(code, language, theme)
    .onSuccess { result -> /* use result.annotated */ }
    .onFailure { error ->
        if (error is HighlightException) {
            when (error) {
                is HighlightException.WebViewInitFailed -> { /* WebView not available (Go edition, MDM-disabled, mid-update) */ }
                is HighlightException.JsExecutionFailed -> { /* JS evaluation error */ }
                is HighlightException.HtmlParseFailed   -> { /* HTML parse error */ }
                is HighlightException.ThemeNotFound     -> { /* Theme CSS file missing or contains no parseable rules */ }
                is HighlightException.RendererGone      -> { /* WebView renderer process crashed or was killed by OS LMK */ }
                is HighlightException.EngineDestroyed   -> { /* engine was destroyed or closed while this call was in flight */ }
                is HighlightException.Timeout           -> { /* timed out waiting for WebView */ }
            }
        }
    }
```

!!! note "Automatic renderer recovery"
    If the WebView renderer process terminates (`HighlightException.RendererGone`), the host application
    is protected from crashing. The current in-flight operation fails immediately with `RendererGone`,
    and subsequent highlight calls automatically re-initialize a fresh WebView on demand.

## Important rules

- Always pass `applicationContext` - never an Activity context. The engine retains the context beyond Activity lifecycle.
- Always call `destroy()` to release the WebView when done. Destroying is not permanent: a later call on the same
  engine re-creates the WebView, which then needs another `destroy()`. Don't reuse an engine after destroying it.
- Use `rememberHighlightEngine()` in Compose for automatic lifecycle management.
- `HighlightEngine` is safe to call from any coroutine - all WebView operations are automatically dispatched to the Main thread internally.
