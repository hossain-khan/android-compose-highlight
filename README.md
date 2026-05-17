[![CI](https://github.com/hossain-khan/android-compose-highlight/actions/workflows/ci.yml/badge.svg)](https://github.com/hossain-khan/android-compose-highlight/actions/workflows/ci.yml) [![codecov](https://codecov.io/github/hossain-khan/android-compose-highlight/graph/badge.svg?token=MHCCHQVSLX)](https://codecov.io/github/hossain-khan/android-compose-highlight) [![GitHub Release](https://img.shields.io/github/v/release/hossain-khan/android-compose-highlight)](https://github.com/hossain-khan/android-compose-highlight/releases/latest)

# Compose Highlight for Android

A Jetpack Compose library for beautiful syntax highlighting — powered by [Highlight.js](https://highlightjs.org/) running in a hidden WebView, converting tokenised HTML output to native Compose `AnnotatedString`. No custom lexers, no bundled grammars to maintain: just drop in the library and highlight any of 190+ languages.


> [!NOTE]
> This project was developed with the assistance of AI coding agents (GitHub Copilot).
> Code, architecture, tests, and documentation were generated or refined through
> AI-assisted pair programming. Review accordingly before using in production.

---

## Quick Start

Wrap your screen (or root composable) in `HighlightThemeProvider`, then place `SyntaxHighlightedCode` anywhere inside it:

```kotlin
HighlightThemeProvider(
    // Uses budled theme, or load CSS theme from assets, or use your custom style map
    lightHighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme  = rememberAtomOneDarkTheme(),
) {
    SyntaxHighlightedCode(
        code            = myCode,
        language        = "python",
        showLineNumbers = true,
    )
}
```

`HighlightThemeProvider` automatically selects the correct theme based on `isSystemInDarkTheme()`. Pass `darkTheme = true/false` to force a specific mode.

- 📚 **API Docs:** https://hossain-khan.github.io/android-compose-highlight/
- 📝 **Blog:** https://hossain.dev/posts/syntax-highlighting-on-android-highlight-js-native-compose-engine/
- 📱 **Sample App:** https://github.com/hossain-khan/android-compose-highlight/tree/main/sample
- 📱 **Alternative Sample App:** https://github.com/hossain-khan/android-syntax-highlighter-compose

### Demo 🎥

| Sample App | Customizations |
| ---- | ----- |
| <video src="https://github.com/user-attachments/assets/10617b72-9b7c-413b-92b5-a939a34ad6af"> | <video src="https://github.com/user-attachments/assets/e97ec831-5531-43d3-8e23-e55498b326c9"> |


---

## Setup

Add the [dependency](https://central.sonatype.com/artifact/dev.hossain/compose-highlight) in your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("dev.hossain:compose-highlight:0.17.2")
}
```

---

## Engine-Only Usage

Use `HighlightEngine` directly if you only need an `AnnotatedString` without the full composable code-block view:

```kotlin
val engine = HighlightEngine(context)
engine.initialize()
    .onFailure { /* handle WebView init failure if needed */ }

val result: Result<AnnotatedString> =
    engine.highlight(code = "val x = 42", language = "kotlin", theme = HighlightTheme.tomorrow(context))

result.onSuccess { annotated ->
    // use annotated string in your own composable
}

// clean up when done
engine.destroy()
```

Or use `highlightBothThemes()` to highlight once and get both light and dark versions in a single WebView call:

```kotlin
val themed = engine.highlightBothThemes(
    code = code,
    language = language,
    lightTheme = HighlightTheme.tomorrow(context),
    darkTheme = HighlightTheme.tomorrowNight(context),
)
themed.onSuccess { (light, dark) ->
    // use light or dark based on system theme
}
```

---

## Composable Helpers

### `rememberHighlightedCode`

Highlights code and returns a `State<AnnotatedString?>`. Re-runs automatically when `code`, `language`, or `theme` changes.

```kotlin
val highlighted by rememberHighlightedCode(
    code     = snippet,
    language = "kotlin",
    onHighlightComplete = { result -> Log.d("Perf", "Highlighted in ${result.durationMs}ms") },
)
Text(text = highlighted ?: AnnotatedString(snippet))
```

### `rememberHighlightedCodeBothThemes`

Highlights once for both light and dark themes in a single JS call. Theme switching after the
initial highlight is instant — no re-highlighting needed.

Inside a `HighlightThemeProvider`, themes are picked up automatically:
```kotlin
HighlightThemeProvider {
    val result by rememberHighlightedCodeBothThemes(code = snippet, language = "kotlin")
    Text(text = (if (isDark) result?.dark else result?.light) ?: AnnotatedString(snippet))
}
```

Outside a provider, pass themes explicitly using the `@Composable` theme helpers:
```kotlin
val result by rememberHighlightedCodeBothThemes(
    code       = snippet,
    language   = "kotlin",
    lightTheme = rememberTomorrowTheme(),
    darkTheme  = rememberTomorrowNightTheme(),
    onHighlightComplete = { result -> Log.d("Perf", "Both themes in ${result.durationMs}ms") },
)
Text(text = (if (isDark) result?.dark else result?.light) ?: AnnotatedString(snippet))
```

---

## Custom Themes

Any Highlight.js CSS theme works. Load from an asset:

```kotlin
val theme = HighlightTheme.fromAsset(context, "themes/my-theme.css", name = "my-theme")
```

Or pass raw CSS directly:

```kotlin
val theme = HighlightTheme.fromCss(cssString, name = "my-inline-theme")
```

Or build a theme from a precomputed color map (useful for Material 3 dynamic color):

```kotlin
val theme = HighlightTheme.fromColorMap(
    name             = "dynamic",
    colorMap         = mapOf(
        "hljs"         to SpanStyle(color = onSurface, background = surface),
        "hljs-keyword" to SpanStyle(color = primary, fontWeight = FontWeight.Bold),
        "hljs-string"  to SpanStyle(color = tertiary),
    ),
    backgroundColor  = surface,
    defaultTextColor = onSurface,
)
```

Community themes are available at [highlightjs/highlight.js/src/styles](https://github.com/highlightjs/highlight.js/tree/main/src/styles).

---

## `SyntaxHighlightedCode` API

See [`SyntaxHighlightedCode` docs](https://hossain-khan.github.io/android-compose-highlight/compose-highlight/dev.hossain.highlight.ui/-syntax-highlighted-code.html) for usage. 

Font family, size, and line height are controlled via `CodeBlockStyle.textStyle`. Start from
[`SyntaxHighlightedCodeDefaults.codeTextStyle`](https://hossain-khan.github.io/android-compose-highlight/compose-highlight/dev.hossain.highlight.ui/-syntax-highlighted-code-defaults/) and override just the properties you need.

```kotlin
SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    style    = CodeBlockStyle(
        textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(
            fontSize   = 15.sp,
            lineHeight = 24.sp,
            fontFamily = FontFamily.Serif,
        ),
    ),
)
```

---

## Architecture

```
SyntaxHighlightedCode  (Compose UI)
    └── rememberHighlightEngine / rememberHighlightedCode
            └── HighlightEngine          (coroutine pipeline)
                    ├── WebViewManager   (hidden WebView + JS bridge)
                    ├── HighlightTheme   (CSS theme model)
                    ├── ThemeParser      (CSS → Map<selector, SpanStyle>)
                    └── HtmlToAnnotatedString  (jsoup → AnnotatedString)
```

The WebView loads `bridge.html` from the library's bundled assets, which in turn loads the full 190+ language Highlight.js bundle. All WebView operations run on the Main thread; callers interact through suspend functions backed by a `Mutex`.

`HighlightThemeProvider` creates a **single shared `HighlightEngine`** (one hidden WebView) for its entire subtree. Screens with multiple `SyntaxHighlightedCode` blocks share one WebView instead of creating one per block, saving ~200 ms warm-up time and ~2–4 MB RAM per extra block.

### Optional: WebView pre-warming

The hidden WebView initializes lazily on first use. To reduce first-call latency, pre-warm the WebView renderer process in your `Application.onCreate()`:

```kotlin
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

Requires `androidx.webkit:webkit:1.16+` (already a transitive dependency of this library).

For full design details see [`docs/prd-compose-syntax-highlight.md`](docs/prd-compose-syntax-highlight.md).

---

## Benchmarks

Microbenchmarks are included in `compose-highlight/src/androidTest/` using the [AndroidX Microbenchmark library](https://developer.android.com/topic/performance/benchmarking/microbenchmark-overview). They measure the three core pipeline stages on a real device.

> [!IMPORTANT]
> The `HighlightEngineBenchmark` class (and the other benchmark classes below) are the **authoritative** source for performance measurement. Use `connectedAndroidTest` results for regression detection and performance comparisons — not the sample app's built-in performance screen, which is exploratory and demo-oriented only (see [Sample App — Performance screen](sample/README.md#performance-screen)).
>
> Note: benchmarks are most reliable when run on a **physical device** in a release build. Debuggable builds (the default for `connectedAndroidTest`) have JIT and coverage overhead that inflates timings — treat debug-build numbers as relative comparisons, not absolute measurements.

### Run

```bash
# Run all microbenchmarks (requires a connected physical device or emulator)
./gradlew :compose-highlight:connectedAndroidTest

# Run only the HighlightEngineBenchmark class
./gradlew :compose-highlight:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.hossain.highlight.benchmark.HighlightEngineBenchmark
```

Results are printed in logcat under the `BENCHMARK` tag.

### Benchmark coverage

| Benchmark | What is measured |
|---|---|
| `ThemeParserBenchmark` | CSS theme parse time (`tomorrow`, `tomorrow-night`) |
| `HtmlToAnnotatedStringBenchmark` | HTML→AnnotatedString conversion for Python, Kotlin, SQL snippets |
| `HighlightEngineBenchmark` | Full WebView JS highlight pipeline for Python, Kotlin, SQL, large Kotlin (WeatherApp), large TypeScript (Zod core) |

Run on your target device to get accurate numbers. Results are printed in logcat under the `BENCHMARK` tag and saved as JSON to device storage.

### Sample results (Pixel 9 Pro XL, debuggable build)

| Test | Median | Min | Max |
|---|---|---|---|
| `highlightPythonToHtml` | 7.5 ms | 1.9 ms | 11.2 ms |
| `highlightKotlinToHtml` | 8.7 ms | 6.0 ms | 11.0 ms |
| `highlightSqlToHtml` | 8.3 ms | 4.6 ms | 10.6 ms |
| `highlightLargeKotlinWeatherAppToHtml` (~150 lines) | 18.8 ms | 11.8 ms | 26.8 ms |
| `highlightLargeTypeScriptZodCoreToHtml` (~200 lines) | 17.6 ms | 11.3 ms | 22.6 ms |

> **Key insight from profiling:** `ThemeParser` and `HtmlToAnnotatedString` are cheap (sub-millisecond to a few ms). The dominant cost is the WebView JS round-trip, which runs off the UI thread and is cached per `rememberHighlightedCode` call. Even large real-world files (~150–200 lines) complete in under 20 ms.

---

## Development Tools

### Memory Leak Detection (Sample App)

The sample app includes [LeakCanary](https://square.github.io/leakcanary/) (`com.squareup.leakcanary:leakcanary-android`) as a `debugImplementation` dependency. LeakCanary installs itself automatically via a `ContentProvider` — no code changes are needed. When a memory leak is detected in a debug build, a notification appears and a heap-dump analysis is shown.

---



## Requirements

- Android minSdk 24+
- Kotlin 2.x
- Jetpack Compose (BOM 2026.05+)

## License

MIT

