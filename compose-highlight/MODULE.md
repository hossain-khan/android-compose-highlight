# Module compose-highlight

A Jetpack Compose library for beautiful syntax highlighting powered by [Highlight.js](https://highlightjs.org/) running in a hidden WebView. Tokenized HTML is converted to native Compose `AnnotatedString` — no custom lexers, no grammars to maintain: 190+ languages out of the box.

## Quick Start

Wrap your screen (or root composable) in `HighlightThemeProvider`, then place `SyntaxHighlightedCode` anywhere inside it:

```kotlin
// In your Activity or top-level composable
HighlightThemeProvider(
    lightHighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme  = rememberAtomOneDarkTheme(),
) {
    SyntaxHighlightedCode(
        code     = """fun greet(name: String) = "Hello, ${'$'}name!"""",
        language = "kotlin",
        showLineNumbers = true,
    )
}
```

`HighlightThemeProvider` automatically selects the correct theme based on `isSystemInDarkTheme()`.

## Supported Built-in Themes

| `@Composable` helper | Context factory | Style |
|---|---|---|
| `rememberTomorrowTheme()` | `HighlightTheme.tomorrow(context)` | Light (Base16) |
| `rememberTomorrowNightTheme()` | `HighlightTheme.tomorrowNight(context)` | Dark (Base16) |
| `rememberAtomOneDarkTheme()` | `HighlightTheme.atomOneDark(context)` | Dark (Atom One) |
| `rememberAtomOneLightTheme()` | `HighlightTheme.atomOneLight(context)` | Light (Atom One) |

The `@Composable` helpers resolve `LocalContext` internally — no need to pass a `Context`.

Custom themes: any Highlight.js CSS file works — load from assets with `HighlightTheme.fromAsset()` or supply raw CSS with `HighlightTheme.fromCss()`.

## Headless / Engine-Only Usage

Use `HighlightEngine` directly when you need an `AnnotatedString` without the built-in composable:

```kotlin
// In a ViewModel or repository
val engine = HighlightEngine(context)

// Optional: warm up the WebView before the first call
engine.initialize()
    .onFailure { /* handle WebView init failure if needed */ }

val result = engine.highlight(
    code     = "SELECT * FROM users WHERE active = 1",
    language = "sql",
    theme    = HighlightTheme.tomorrow(context),
)
result.onSuccess { r ->
    // r.annotated   — AnnotatedString ready for Text()
    // r.spanCount   — 0 signals a silent failure (unsupported language / empty input)
    // r.durationMs  — total JS pipeline time in ms (equals r.timings.total.inWholeMilliseconds)
    // r.timings     — per-stage Duration breakdown (jsBridge, htmlParse, treeWalk, etc.)
    // r.language    — the language identifier that was requested
}

// Always destroy to release WebView resources
engine.destroy()
```

When used inside a Composable, prefer `rememberHighlightEngine()` — it handles destruction automatically.

## Reactive Initialization State

`HighlightEngine.isInitialized` is a `StateFlow<Boolean>` — observe it in Compose to react when the hidden WebView finishes loading:

```kotlin
val engine = rememberHighlightEngine()
val isReady by engine.isInitialized.collectAsState()

if (isReady) {
    Text("WebView warm — first highlight will be fast")
}
```

## Raw HTML Output with Timing

Use `highlightToHtml()` when you need the raw `<span class="hljs-*">` HTML (e.g. for a custom renderer or benchmarking):

```kotlin
engine.highlightToHtml(code, "kotlin").onSuccess { result ->
    renderCustomHtml(result.html)
    log("JS round-trip: ${result.durationMs} ms")  // true JS-only time
}
```

`HtmlHighlightResult` carries `.html` and `.durationMs` (measured after WebView is ready and the internal mutex is acquired — excludes warm-up and queue-wait time).

## Engine Introspection

```kotlin
engine.highlightJsVersion().onSuccess { v -> log("hljs $v") }
engine.supportedLanguages().onSuccess { langs -> log("${langs.size} languages") }
```

## Theme Switching Without Extra JS Round-Trips

`highlightBothThemes()` tokenizes code once and applies two color maps, so live light/dark switching is instant:

```kotlin
val themed = engine.highlightBothThemes(
    code       = sourceCode,
    language   = "typescript",
    lightTheme = HighlightTheme.tomorrow(context),
    darkTheme  = HighlightTheme.tomorrowNight(context),
)
themed.onSuccess { result ->
    // ThemedHighlightResult — both variants pre-computed, no extra JS call
    val displayString = if (isDark) result.dark else result.light
    // result.durationMs — total pipeline time in ms; result.timings has per-stage breakdown
}
```

Inside a Composable, use `rememberHighlightedCodeBothThemes()` for the same behaviour with automatic state management:

```kotlin
HighlightThemeProvider(lightHighlightTheme = ..., darkHighlightTheme = ...) {
    // Themes default to LocalLightHighlightTheme / LocalDarkHighlightTheme — no explicit args needed
    val result by rememberHighlightedCodeBothThemes(
        code     = sourceCode,
        language = "kotlin",
        onHighlightComplete = { r ->
            // r.timings has per-stage Duration fields: jsBridge, htmlParse, treeWalk, etc.
            log("done in ${r.durationMs} ms (JS: ${r.timings.jsBridge.inWholeMilliseconds}ms)")
        },
    )
    val text = if (isSystemInDarkTheme()) result?.dark else result?.light
    if (text != null) Text(text)
}
```

## CompositionLocals

`HighlightThemeProvider` exposes three CompositionLocals:

| Local | Type | Content |
|---|---|---|
| `LocalHighlightTheme` | `HighlightTheme` | Active theme (light or dark based on system) |
| `LocalLightHighlightTheme` | `HighlightTheme` | Always the light variant |
| `LocalDarkHighlightTheme` | `HighlightTheme` | Always the dark variant |

`LocalLightHighlightTheme` and `LocalDarkHighlightTheme` are useful when a composable needs both variants simultaneously (e.g. `rememberHighlightedCodeBothThemes`).

## Custom Styling

Pass a `CodeBlockStyle` to control padding, shape, line-number column width, and copy-button size:

```kotlin
val compact = CodeBlockStyle(
    shape   = RoundedCornerShape(4.dp),
    padding = PaddingValues(8.dp),
)
SyntaxHighlightedCode(
    code     = snippet,
    language = "json",
    style    = compact,
    copyButton = null,   // hide the copy button
)
```

## Architecture

```
SyntaxHighlightedCode      (Compose UI)
 └── rememberHighlightEngine / rememberHighlightedCode
       └── HighlightEngine            (coroutine pipeline)
             ├── WebViewManager       (hidden WebView + JS bridge)
             ├── HighlightTheme       (CSS theme model, lazy-parsed)
             ├── ThemeParser          (CSS → Map<selector, SpanStyle>)
             └── HtmlToAnnotatedString (jsoup → AnnotatedString)
```

All WebView operations run on the Main thread; callers interact via `suspend` functions backed by a `Mutex`. The WebView loads `bridge.html` from the library's bundled assets which in turn loads the full Highlight.js bundle — no network requests at runtime.
