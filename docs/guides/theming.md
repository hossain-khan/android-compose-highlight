# Theming

`compose-highlight` ships four Highlight.js themes out of the box. Themes are backed by CSS files bundled in the library's assets and lazily parsed at first use.

## Built-in themes

| Theme | Style | Helper |
|---|---|---|
| Tomorrow | Light | `rememberTomorrowTheme()` |
| Tomorrow Night | Dark | `rememberTomorrowNightTheme()` |
| Atom One Dark | Dark | `rememberAtomOneDarkTheme()` |
| Atom One Light | Light | `rememberAtomOneLightTheme()` |

## Automatic light/dark switching

Pass a light and dark theme to `HighlightThemeProvider`. It picks the right one based on the system `isSystemInDarkTheme()` value:

```kotlin
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.hossain.highlight.ui.rememberTomorrowTheme

HighlightThemeProvider(
    lightHighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme  = rememberTomorrowNightTheme(),
) {
    SyntaxHighlightedCode(code = snippet, language = "kotlin")
}
```

## Force a specific theme

```kotlin
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.rememberAtomOneLightTheme

HighlightThemeProvider(
    darkTheme           = false,  // always use the light theme
    lightHighlightTheme = rememberAtomOneLightTheme(),
    darkHighlightTheme  = rememberAtomOneLightTheme(),
) { ... }
```

## Custom theme from a Highlight.js CSS file

1. Download any `.css` file from [highlightjs/highlight.js/src/styles](https://github.com/highlightjs/highlight.js/tree/main/src/styles) (or create your own).
2. Place it in `src/main/assets/themes/`.
3. Load it with `HighlightTheme.fromAsset()`:

```kotlin
import androidx.compose.ui.platform.LocalContext
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.HighlightThemeProvider

val theme = HighlightTheme.fromAsset(
    context   = LocalContext.current.applicationContext,
    assetPath = "themes/github-dark.css",
    name      = "github-dark",
)
HighlightThemeProvider(darkHighlightTheme = theme) { ... }
```

!!! note
    Always call `context.applicationContext` — never pass an Activity context to `HighlightTheme` factories, as the theme is retained beyond the Activity's lifecycle.

## Custom theme from raw CSS

Useful when the CSS is fetched remotely or generated at runtime:

```kotlin
import dev.hossain.highlight.engine.HighlightTheme

val rawCss = // ... network fetch or string resource
val theme = HighlightTheme.fromCss(cssText = rawCss, name = "remote-theme")
```

## Custom theme from a color map

Maximum flexibility — derive colors from Material 3 dynamic color, user palettes, or brand colors:

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import dev.hossain.highlight.engine.HighlightTheme

val lightColors = MaterialTheme.colorScheme
val theme = HighlightTheme.fromColorMap(
    name             = "material-dynamic-light",
    colorMap         = mapOf(
        "hljs"          to SpanStyle(color = lightColors.onSurface, background = lightColors.surface),
        "hljs-keyword"  to SpanStyle(color = lightColors.primary, fontWeight = FontWeight.Bold),
        "hljs-string"   to SpanStyle(color = lightColors.tertiary),
        "hljs-comment"  to SpanStyle(color = lightColors.outline, fontStyle = FontStyle.Italic),
    ),
    backgroundColor  = lightColors.surface,
    defaultTextColor = lightColors.onSurface,
)
```

## Providing a theme without `HighlightThemeProvider`

For one-off use — pass the theme directly:

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberTomorrowTheme

val theme = rememberTomorrowTheme()
SyntaxHighlightedCode(
    code     = snippet,
    language = "python",
    theme    = theme,
)
```

This creates a standalone `HighlightEngine` with its own WebView. Use `HighlightThemeProvider` when rendering multiple code blocks on the same screen.

## Highlight both themes simultaneously

For zero-latency theme switching, tokenize once and apply both color maps. Use `produceState` to keep the work off the UI thread:

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import dev.hossain.highlight.ui.rememberHighlightEngine

val engine = rememberHighlightEngine()
val fallback = remember(code) { AnnotatedString(code) }
val (lightAnnotated, darkAnnotated) by produceState(fallback to fallback, code) {
    engine.highlightBothThemes(
        code       = code,
        language   = "kotlin",
        lightTheme = lightTheme,
        darkTheme  = darkTheme,
    ).onSuccess { value = it.light to it.dark }
}
// Switch between lightAnnotated and darkAnnotated instantly
```

See [`rememberHighlightedCodeBothThemes`](../reference/highlight-engine.md) for the ready-made composable helper.
