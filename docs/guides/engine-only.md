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

@Composable
fun CodeScreen(viewModel: CodeViewModel = viewModel()) {
    val highlighted by viewModel.highlighted.collectAsState()

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
                is HighlightException.WebViewInitFailed -> /* WebView not available (Go edition, MDM-disabled, mid-update) */
                is HighlightException.JsExecutionFailed -> /* JS evaluation error */
                is HighlightException.HtmlParseFailed   -> /* jsoup parse error, or theme resolution failure (check cause) */
                is HighlightException.Timeout           -> /* timed out waiting for WebView */
                is HighlightException.ThemeNotFound     -> /* unreachable from highlight()/highlightBothThemes()/highlightAuto() - see note below */
            }
        }
    }
```

!!! note "Theme resolution failures are wrapped in `HtmlParseFailed`"
    Failures during theme resolution - including a missing asset file (`IOException`),
    a CSS file that contains no parseable color rules (`ThemeNotFound`), or any other
    exception thrown while parsing the active theme - are caught alongside HTML parsing
    and surfaced as `HighlightException.HtmlParseFailed`. Inspect `error.cause` to
    distinguish the underlying failure. `ThemeNotFound` is never raised directly to
    callers of `engine.highlight()` / `highlightBothThemes()` / `highlightAuto()`.

## Important rules

- Always pass `applicationContext` - never an Activity context. The engine retains the context beyond Activity lifecycle.
- Always call `destroy()` to release the WebView when done. Use `rememberHighlightEngine()` in Compose for automatic lifecycle management.
- `HighlightEngine` is safe to call from any coroutine - all WebView operations are automatically dispatched to the Main thread internally.
