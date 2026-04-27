# compose-highlight

A Jetpack Compose library for beautiful syntax highlighting — powered by [Highlight.js](https://highlightjs.org/) running in a hidden WebView, converting tokenised HTML output to native Compose `AnnotatedString`. No custom lexers, no bundled grammars to maintain: just drop in the library and highlight any of 190+ languages.

Inspired by the approach used in production apps such as Claude, Perplexity, and ChatGPT on Android.

---

## Quick Start

```kotlin
// One line — that's it
SyntaxHighlightedCode(code = myCode, language = "kotlin")
```

Wrap your UI in `HighlightThemeProvider` to apply light/dark themes automatically:

```kotlin
HighlightThemeProvider(
    lightHighlightTheme = HighlightTheme.tomorrow(context),
    darkHighlightTheme = HighlightTheme.tomorrowNight(context),
) {
    SyntaxHighlightedCode(
        code = myCode,
        language = "python",
        showLineNumbers = true,
    )
}
```

---

## Setup

Add the `:compose-highlight` module to your project (local module for now; Maven publishing coming soon):

```kotlin
// settings.gradle.kts
include(":compose-highlight")
project(":compose-highlight").projectDir = file("../compose-highlight")

// app/build.gradle.kts
dependencies {
    implementation(project(":compose-highlight"))
}
```

The library requires `minSdk = 24`.

---

## Engine-Only Usage

Use `HighlightEngine` directly if you only need an `AnnotatedString` without the full composable:

```kotlin
val engine = HighlightEngine(context)
engine.initialize() // warms up the WebView

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

## Custom Themes

Any Highlight.js CSS theme works. Load from an asset:

```kotlin
val theme = HighlightTheme.fromAsset(context, "themes/my-theme.css")
```

Or pass raw CSS directly:

```kotlin
val theme = HighlightTheme.fromCss(cssString)
```

---

## `SyntaxHighlightedCode` API

| Parameter | Type | Default | Description |
|---|---|---|---|
| `code` | `String` | required | Source code to display |
| `language` | `String` | required | Highlight.js language ID |
| `modifier` | `Modifier` | `Modifier` | Outer container modifier |
| `theme` | `HighlightTheme` | `LocalHighlightTheme` | Theme override |
| `style` | `CodeBlockStyle` | `CodeBlockStyle.Default` | Visual style (padding, shape, etc.) |
| `showLineNumbers` | `Boolean` | `false` | Show line-number gutter |
| `showLanguageLabel` | `Boolean` | `true` | Show language badge in header |
| `showCopyButton` | `Boolean` | `true` | Show copy-to-clipboard button |
| `onCopyClick` | `((String) -> Unit)?` | `null` | Custom copy handler |
| `fontFamily` | `FontFamily` | `Monospace` | Code font |
| `fontSize` | `TextUnit` | `13.sp` | Code font size |
| `lineHeight` | `TextUnit` | `20.sp` | Code line height |

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

The WebView loads `bridge.html` from the library's bundled assets, which in turn loads the full 192-language Highlight.js bundle. All WebView operations run on the Main thread; callers interact through suspend functions backed by a `Mutex`.

For full design details see [`docs/prd-compose-syntax-highlight.md`](docs/prd-compose-syntax-highlight.md).

---

## Benchmarks

Microbenchmarks are included in `compose-highlight/src/androidTest/` using the [AndroidX Microbenchmark library](https://developer.android.com/topic/performance/benchmarking/microbenchmark-overview). They measure the three core pipeline stages on a real device.

### Run

```bash
./gradlew :compose-highlight:connectedAndroidTest
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



- Android minSdk 24+
- Kotlin 2.x
- Jetpack Compose (BOM 2026.03+)

## License

MIT

---

> [!NOTE]
> This project was developed with the assistance of AI coding agents (GitHub Copilot).
> Code, architecture, tests, and documentation were generated or refined through
> AI-assisted pair programming. Review accordingly before using in production.
