# HighlightEngine

`HighlightEngine` is the low-level API that owns the hidden WebView and runs the highlight.js
pipeline.

For most UI use cases, prefer `SyntaxHighlightedCode` inside `HighlightThemeProvider`. Use
`HighlightEngine` directly when you need highlighting outside composables, such as in a ViewModel
or service layer.

Full API in Dokka:

- [`HighlightEngine`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-highlight-engine/index.html)
- [`HighlightResult`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-highlight-result/index.html)
- [`ThemedHighlightResult`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-themed-highlight-result/index.html)
- [`HighlightLanguageInfo`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-highlight-language-info/index.html)
- [`AutoHighlightResult`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-auto-highlight-result/index.html)
- [`rememberHighlightEngine`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-highlight-engine.html)

## When to use it

- You need highlighting in non-UI layers.
- You want direct control over engine initialization and lifetime.
- You need APIs like language lookup, auto-detection, or HTML-only output.

## Lifecycle and cleanup

The engine holds a hidden WebView and implements `Closeable`. Always call `close()` (or
`destroy()`) when done.

```kotlin
val engine = HighlightEngine(context.applicationContext)
try {
    val result = engine.highlight(code, "kotlin", theme)
} finally {
    engine.close()
}
```

In Compose, use `rememberHighlightEngine()` so cleanup is handled automatically.

## Core operations

- `initialize()` - optional warm-up to reduce first highlight latency.
- `highlight()` - highlight one language and return `Result<HighlightResult>` containing the `AnnotatedString` and timings.
- `highlightBothThemes()` - tokenize once and generate light and dark variants.
- `highlightToHtml()` - get highlighted HTML directly.
- `supportedLanguages()` and `highlightJsVersion()` - runtime capability introspection.
- `getLanguage()` and `highlightAuto()` - language metadata and auto-detection workflows.

All suspend APIs return `Result<T>` and report failures as `HighlightException`.

## Usage in a ViewModel

```kotlin
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightTheme

class CodeViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = HighlightEngine(application.applicationContext)

    init {
        viewModelScope.launch {
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

## Highlight both themes for instant switching

`highlightBothThemes()` tokenizes once and applies both color maps, so theme switching does not
trigger another JS tokenization pass.

```kotlin
import dev.hossain.highlight.engine.HighlightTheme

// Outside Compose: use the non-@Composable factories on HighlightTheme.
// highlightBothThemes is a suspend fun, so call it from a coroutine.
viewModelScope.launch {
    engine.highlightBothThemes(
        code       = sourceCode,
        language   = "typescript",
        lightTheme = HighlightTheme.tomorrow(),
        darkTheme  = HighlightTheme.tomorrowNight(),
    ).onSuccess { result ->
        val display = if (isDark) result.dark else result.light
    }
}
```

!!! note
    Inside `@Composable` functions, prefer `rememberTomorrowTheme()` /
    `rememberTomorrowNightTheme()` so the theme is remembered across recompositions.
    The `HighlightTheme.tomorrow()` and `HighlightTheme.tomorrowNight()` factories above
    are the non-composable equivalents - safe to call from a ViewModel or repository.

## Language lookup and auto-detection

Use `getLanguage()` to validate names and inspect aliases:

```kotlin
engine.getLanguage("ts").onSuccess { info ->
    if (info != null) {
        // info.name    = "TypeScript"
        // info.aliases = [ts, tsx, mts, cts]
    }
}
```

Use `highlightAuto()` when language is unknown:

```kotlin
engine.highlightAuto(code, theme).onSuccess { result ->
    val language = result.detectedLanguage
    Text(text = result.annotated)
}
```

## Common pitfalls

- Forgetting `applicationContext` can leak short-lived contexts.
- Forgetting `close()` in non-Compose code can keep WebView resources alive.
- Treating `Result.failure` as exceptional flow from the API point of view; this is expected
  error signaling for this library.
