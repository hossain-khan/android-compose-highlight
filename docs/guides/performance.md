# Performance

## Shared WebView via `HighlightThemeProvider`

The most impactful optimization is wrapping your content in `HighlightThemeProvider`. Without it, every `SyntaxHighlightedCode` creates its own hidden WebView:

| Setup | WebViews | Warm-up cost |
|---|---|---|
| No provider — 3 blocks | 3 | ~600 ms |
| With provider — 3 blocks | 1 | ~200 ms |

```kotlin
// Wrap once, high up in your composition tree
HighlightThemeProvider(
    lightHighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme  = rememberTomorrowNightTheme(),
) {
    LazyColumn {
        items(snippets) { snippet ->
            SyntaxHighlightedCode(code = snippet.code, language = snippet.lang)
        }
    }
}
```

## Pre-warming the WebView

The WebView initializes lazily on the first `highlight()` call. To hide that cost, warm it up during app start:

```kotlin
// Application.onCreate() — pre-warm the Android WebView renderer process
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            WebViewCompat.startUpWebView(
                applicationContext,
                WebViewStartUpConfig.Builder(mainExecutor).build(),
                WebViewOutcomeReceiver { /* no-op */ },
            )
        }
    }
}
```

Requires `androidx.webkit:webkit:1.16+` (a transitive dependency of this library).

## Pre-warming via `HighlightEngine`

When using `HighlightEngine` directly in a ViewModel, call `initialize()` on a background coroutine before you need the first highlight:

```kotlin
class CodeViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = HighlightEngine(application)

    init {
        viewModelScope.launch {
            engine.initialize()  // warm-up on launch
        }
    }

    suspend fun highlight(code: String, language: String, theme: HighlightTheme) =
        engine.highlight(code, language, theme).getOrNull()?.annotated

    override fun onCleared() { engine.destroy() }
}
```

## Highlight both themes for instant switching

Tokenization is the slow part. Running it twice for light + dark wastes time. Use `highlightBothThemes` to tokenize once and produce both variants:

```kotlin
val result = engine.highlightBothThemes(
    code       = sourceCode,
    language   = "kotlin",
    lightTheme = rememberTomorrowTheme(),
    darkTheme  = rememberTomorrowNightTheme(),
)
// Switch instantly at the call site — no re-highlighting needed
result.onSuccess { themed ->
    val annotated = if (isDark) themed.dark else themed.light
}
```

Inside Compose, use `rememberHighlightedCodeBothThemes(code, language)`.

## Timing callbacks

Monitor per-stage latency with `onHighlightComplete`:

```kotlin
SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    onHighlightComplete = { result ->
        Log.d("HighlightPerf",
            "total=${result.durationMs}ms, " +
            "js=${result.timings.jsBridge.inWholeMilliseconds}ms, " +
            "parse=${result.timings.htmlParse.inWholeMilliseconds}ms, " +
            "spans=${result.spanCount}")
    },
)
```

`HighlightTimings` has individual fields for each pipeline stage:

| Field | Type | Description |
|---|---|---|
| `jsBridge` | `Duration` | WebView JS evaluation |
| `jsonUnescape` | `Duration` | JS string unescaping |
| `htmlParse` | `Duration` | jsoup HTML parsing |
| `treeWalk` | `Duration` | Span tree walk |
| `themeParse` | `Duration` | CSS to SpanStyle (first call only; `Duration.ZERO` on cache hits) |
| `total` | `Duration` | Full elapsed wall-clock time |

Use `.inWholeMilliseconds` to get a `Long`, or `.inWholeNanoseconds` for finer granularity.

## Typical latencies (Pixel 6, release build)

| Stage | First call | Subsequent calls |
|---|---|---|
| WebView warm-up | ~150-200 ms | 0 ms |
| JS evaluation | ~20-50 ms | ~5-15 ms |
| HTML parse + span walk | ~2-5 ms | ~2-5 ms |
| Theme CSS parse | ~10-20 ms (once) | 0 ms (cached) |

## `isInitialized` state flow

Observe engine readiness reactively — for example to show a loading indicator:

```kotlin
val engine       = rememberHighlightEngine()
val isReady by engine.isInitialized.collectAsState()

if (!isReady) {
    CircularProgressIndicator()
} else {
    SyntaxHighlightedCode(code = snippet, language = "kotlin")
}
```
