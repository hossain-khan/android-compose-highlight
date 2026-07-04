# Performance

## Shared WebView via `HighlightThemeProvider`

The most impactful optimization is wrapping your content in `HighlightThemeProvider`. Without it, every `SyntaxHighlightedCode` creates its own hidden WebView:

| Setup | WebViews | Avg per block | Total (17 blocks) | Heap (cold start) |
|---|---|---|---|---|
| No provider - 17 blocks | 17 | ~50 ms | ~866 ms | ~34 MB |
| With provider - 17 blocks | 1 | ~13 ms | ~224 ms | ~15 MB |

Measured on a Pixel 8 Pro, debug build, cold start. With provider is roughly **4x faster** and uses **~55% less heap**.

Each extra standalone engine adds roughly **~37 ms** to average highlight time and **~1-2 MB** of additional heap per engine while the screen is active.

```kotlin
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.hossain.highlight.ui.rememberTomorrowLightTheme

// Wrap once, high up in your composition tree
HighlightThemeProvider(
    lightHighlightTheme = rememberTomorrowLightTheme(),
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
// Application.onCreate() - pre-warm the Android WebView renderer process
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
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightTheme

class CodeViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = HighlightEngine(application.applicationContext)

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
import dev.hossain.highlight.engine.HighlightTheme

// Outside Compose: use the non-@Composable HighlightTheme.tomorrow* factories.
// highlightBothThemes is a suspend fun, so call it from a coroutine.
viewModelScope.launch {
    val result = engine.highlightBothThemes(
        code       = sourceCode,
        language   = "kotlin",
        lightTheme = HighlightTheme.tomorrow(),
        darkTheme  = HighlightTheme.tomorrowNight(),
    )
    // Switch instantly at the call site - no re-highlighting needed
    result.onSuccess { themed ->
        val annotated = if (isDark) themed.dark else themed.light
    }
}
```

Inside Compose, use `rememberHighlightedCodeBothThemes(code, language)` - or pass
`rememberTomorrowLightTheme()` / `rememberTomorrowNightTheme()` to `highlightBothThemes`
when you already have an `engine` from `rememberHighlightEngine()`.

## Timing callbacks

Monitor per-stage latency with `onHighlightComplete`:

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    onHighlightComplete = { result ->
        Log.d(
            "HighlightPerf",
            "total=${result.durationMs}ms, " +
            "js=${result.timings.jsBridge.inWholeMilliseconds}ms, " +
            "parse=${result.timings.htmlParse.inWholeMilliseconds}ms, " +
            "spans=${result.spanCount}",
        )
    },
)
```

[`HighlightTimings`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-highlight-timings/index.html) has individual fields for each pipeline stage:

| Field | Type | Description |
|---|---|---|
| `jsBridge` | `Duration` | WebView JS evaluation |
| `jsonUnescape` | `Duration` | JS string unescaping |
| `htmlParse` | `Duration` | Custom HTML parsing |
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

Observe engine readiness reactively - for example to show a loading indicator:

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberHighlightEngine

val engine       = rememberHighlightEngine()
val isReady by engine.isInitialized.collectAsState()

if (!isReady) {
    CircularProgressIndicator()
} else {
    SyntaxHighlightedCode(code = snippet, language = "kotlin")
}
```
