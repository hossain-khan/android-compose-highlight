# Theming

`compose-highlight` ships eight Highlight.js themes out of the box. Those built-ins are precompiled into Kotlin constants at build time, so they do not parse CSS at runtime.

## Built-in themes

| Theme | Style | Helper |
| --- | --- | --- |
| Tomorrow | Light | `rememberTomorrowLightTheme()` |
| Tomorrow Night | Dark | `rememberTomorrowNightTheme()` |
| Atom One Dark | Dark | `rememberAtomOneDarkTheme()` |
| Atom One Light | Light | `rememberAtomOneLightTheme()` |
| GitHub | Light | `rememberGithubLightTheme()` |
| GitHub Dark | Dark | `rememberGithubDarkTheme()` |
| Dracula | Dark | `rememberDraculaDarkTheme()` (or `rememberAlucardDarkTheme()`) |
| Alucard | Light | `rememberAlucardLightTheme()` (or `rememberDraculaLightTheme()`) |

!!! tip "Dracula and Alucard Aliases"
    Dracula (Dark) and Alucard (Light) are designed as a pair (since *Alucard* is *Dracula* spelled backward 😅).
    To make them easier to find, the library provides convenience aliases under both naming schemes:

    * **Dracula Light**: `rememberDraculaLightTheme()` (alias for `rememberAlucardLightTheme()`) or `HighlightTheme.draculaLight()`
    * **Alucard Dark**: `rememberAlucardDarkTheme()` (alias for `rememberDraculaDarkTheme()`) or `HighlightTheme.alucardDark()`

Built-in themes (`tomorrow`, `tomorrowNight`, `atomOneDark`, `atomOneLight`, `githubLight`, `githubDark`, `draculaDark`, `alucardLight`) are fast-path themes:

- No `Context` required
- No runtime CSS parsing
- Color maps are read from precompiled color maps bundled with the library

## Automatic light/dark switching

Pass a light and dark theme to `HighlightThemeProvider`. It picks the right one based on the system `isSystemInDarkTheme()` value:

```kotlin
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.hossain.highlight.ui.rememberTomorrowLightTheme

HighlightThemeProvider(
    lightHighlightTheme = rememberTomorrowLightTheme(),
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
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.HighlightThemeProvider

val theme = HighlightTheme.fromAsset(
    context   = LocalContext.current.applicationContext,
    assetPath = "themes/github-dark.css",
    name      = "github-dark",
)
HighlightThemeProvider(darkHighlightTheme = theme) { ... }
```

`fromAsset()` is lazy. CSS parsing happens on first use (`theme.colorMap`), not at factory-call time.

!!! note
    `HighlightTheme.fromAsset()` normalizes the passed `Context` to `applicationContext` internally.
    Passing `applicationContext` at call sites is still recommended for clarity.

## Custom theme from raw CSS

Useful when the CSS is fetched remotely or generated at runtime:

```kotlin
import dev.hossain.highlight.engine.HighlightTheme

val rawCss = // ... network fetch or string resource
val theme = HighlightTheme.fromCss(cssText = rawCss, name = "remote-theme")
```

## Custom theme from a color map

Maximum flexibility - derive colors from Material 3 dynamic color, user palettes, or brand colors.
Use `HljsSelectors` constants for known hljs scope keys:

```kotlin
import dev.hossain.highlight.engine.HljsSelectors
import dev.hossain.highlight.engine.HighlightTheme

val lightColors = MaterialTheme.colorScheme
val theme = HighlightTheme.fromColorMap(
    name             = "material-dynamic-light",
    colorMap         = mapOf(
        HljsSelectors.BASE         to SpanStyle(color = lightColors.onSurface, background = lightColors.surface),
        HljsSelectors.KEYWORD      to SpanStyle(color = lightColors.primary, fontWeight = FontWeight.Bold),
        HljsSelectors.STRING       to SpanStyle(color = lightColors.tertiary),
        HljsSelectors.COMMENT      to SpanStyle(color = lightColors.outline, fontStyle = FontStyle.Italic),
    ),
    backgroundColor  = lightColors.surface,
    defaultTextColor = lightColors.onSurface,
)
```

## Providing a theme without `HighlightThemeProvider`

For one-off use - pass the theme directly:

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberTomorrowLightTheme

val theme = rememberTomorrowLightTheme()
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
import dev.hossain.highlight.ui.rememberHighlightEngine
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.hossain.highlight.ui.rememberTomorrowLightTheme

val engine     = rememberHighlightEngine()
val lightTheme = rememberTomorrowLightTheme()
val darkTheme  = rememberTomorrowNightTheme()
val fallback   = remember(code) { AnnotatedString(code) }
val themedPair by produceState(fallback to fallback, code, lightTheme, darkTheme) {
    engine.highlightBothThemes(
        code       = code,
        language   = "kotlin",
        lightTheme = lightTheme,
        darkTheme  = darkTheme,
    ).onSuccess { value = it.light to it.dark }
}
val (lightAnnotated, darkAnnotated) = themedPair
// Switch between lightAnnotated and darkAnnotated instantly
```

See [`rememberHighlightedCodeBothThemes`](../reference/syntax-highlighted-code#rememberhighlightedcodeboththemes) for the ready-made composable helper.
