# Module compose-highlight

A Jetpack Compose library for beautiful syntax highlighting powered by [Highlight.js](https://highlightjs.org/) running in a hidden WebView. Tokenized HTML is converted to native Compose `AnnotatedString` — no custom lexers, no grammars to maintain: 190+ languages out of the box.

## Quick Start

Wrap your screen (or root composable) in `HighlightThemeProvider`, then place `SyntaxHighlightedCode` anywhere inside it:

```kotlin
// In your Activity or top-level composable
HighlightThemeProvider(
    lightHighlightTheme = HighlightTheme.tomorrow(LocalContext.current),
    darkHighlightTheme  = HighlightTheme.atomOneDark(LocalContext.current),
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

| Factory method              | Style            |
|-----------------------------|------------------|
| `HighlightTheme.tomorrow()` | Light (Base16)   |
| `HighlightTheme.tomorrowNight()` | Dark (Base16) |
| `HighlightTheme.atomOneDark()` | Dark (Atom One) |
| `HighlightTheme.atomOneLight()` | Light (Atom One) |

Custom themes: any Highlight.js CSS file works — load from assets with `HighlightTheme.fromAsset()` or supply raw CSS with `HighlightTheme.fromCss()`.

## Headless / Engine-Only Usage

Use `HighlightEngine` directly when you need an `AnnotatedString` without the built-in composable:

```kotlin
// In a ViewModel or repository
val engine = HighlightEngine(context)

// Optional: warm up the WebView before the first call
engine.initialize()

val result = engine.highlight(
    code     = "SELECT * FROM users WHERE active = 1",
    language = "sql",
    theme    = HighlightTheme.tomorrow(context),
)
result.onSuccess { annotated ->
    // apply annotated string in your own Text() composable
}

// Always destroy to release WebView resources
engine.destroy()
```

When used inside a Composable, prefer `rememberHighlightEngine()` — it handles destruction automatically.

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
    val displayString = if (isDark) result.dark else result.light
}
```

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
    showCopyButton = false,
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
