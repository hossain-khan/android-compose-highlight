# HighlightTheme

Represents a syntax highlighting theme backed by a Highlight.js CSS file.

The color map is lazily initialized and cached — CSS parsing happens at most once per theme instance.

## Built-in themes

| Factory | Style | `remember` helper |
|---|---|---|
| `HighlightTheme.tomorrow(context)` | Light | `rememberTomorrowTheme()` |
| `HighlightTheme.tomorrowNight(context)` | Dark | `rememberTomorrowNightTheme()` |
| `HighlightTheme.atomOneDark(context)` | Dark | `rememberAtomOneDarkTheme()` |
| `HighlightTheme.atomOneLight(context)` | Light | `rememberAtomOneLightTheme()` |

Always use the `remember*` helpers inside composables so the CSS is not re-parsed on every recomposition:

```kotlin
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.hossain.highlight.ui.rememberTomorrowTheme

HighlightThemeProvider(
    lightHighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme  = rememberTomorrowNightTheme(),
) { ... }
```

## Custom theme from an asset file

Drop any [Highlight.js CSS theme](https://github.com/highlightjs/highlight.js/tree/main/src/styles) into `src/main/assets/themes/` and load it at runtime:

```kotlin
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.HighlightThemeProvider

// src/main/assets/themes/github.css  <- place the CSS here
val theme = HighlightTheme.fromAsset(
    context   = context,
    assetPath = "themes/github.css",
    name      = "github",
)
HighlightThemeProvider(lightHighlightTheme = theme) { ... }
```

!!! note
    `fromAsset()` is lazy — CSS parsing happens on first use, not at factory-call time.

## Custom theme from raw CSS

```kotlin
import dev.hossain.highlight.engine.HighlightTheme

val css = // ... fetch from network or build programmatically
val theme = HighlightTheme.fromCss(
    cssText = css,
    name    = "my-runtime-theme",
)
```

## Custom theme from a color map

For full control — e.g. deriving colors from Material 3 dynamic color:

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import dev.hossain.highlight.engine.HighlightTheme

val colorMap = mapOf(
    "hljs"          to SpanStyle(color = Color(0xFF24292E), background = Color(0xFFFFFFFF)),
    "hljs-keyword"  to SpanStyle(color = Color(0xFFD73A49), fontWeight = FontWeight.Bold),
    "hljs-string"   to SpanStyle(color = Color(0xFF032F62)),
    "hljs-comment"  to SpanStyle(color = Color(0xFF6A737D), fontStyle = FontStyle.Italic),
)
val theme = HighlightTheme.fromColorMap(
    name             = "my-dynamic-theme",
    colorMap         = colorMap,
    backgroundColor  = Color(0xFFFFFFFF),
    defaultTextColor = Color(0xFF24292E),
)
```

## Theme identity

`HighlightTheme` uses `name` for `equals()` and `hashCode()`. Compose APIs (`remember`, `LaunchedEffect`) detect theme changes by name. **Names must be unique** — do not create two themes with different content but the same name.

## Properties

| Property | Description |
|---|---|
| `name` | Unique display name |
| `colorMap` | Lazily parsed `Map<String, SpanStyle>` (hljs class name to style) |
| `backgroundColor` | Background color from the `.hljs` CSS rule |
| `defaultTextColor` | Default text color from the `.hljs` CSS rule |
