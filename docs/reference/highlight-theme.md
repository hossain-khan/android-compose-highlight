# HighlightTheme

`HighlightTheme` maps `hljs-*` token classes to Compose `SpanStyle` values used by the highlight
pipeline.

Full API in Dokka:

- [`HighlightTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-highlight-theme/index.html)
- [`rememberTomorrowTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-tomorrow-theme.html)
- [`rememberTomorrowNightTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-tomorrow-night-theme.html)
- [`rememberAtomOneDarkTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-atom-one-dark-theme.html)
- [`rememberAtomOneLightTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-atom-one-light-theme.html)

## When to use each theme source

- Built-in themes (`tomorrow`, `tomorrowNight`, `atomOneDark`, `atomOneLight`): fastest setup,
  precompiled maps, no CSS parsing at runtime.
- `fromAsset(...)`: best for shipping a highlight.js CSS file with your app.
- `fromCss(...)`: useful when CSS comes from network, config, or generated text.
- `fromColorMap(...)`: best when you want full programmatic control, for example Material 3
  dynamic color integration.

## Recommended usage in Compose

Use `remember*Theme()` helpers inside composables so the theme instance stays stable across
recompositions.

```kotlin
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.hossain.highlight.ui.rememberTomorrowTheme

HighlightThemeProvider(
    lightHighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme  = rememberTomorrowNightTheme(),
) { ... }
```

## Custom theme from asset CSS

```kotlin
import androidx.compose.ui.platform.LocalContext
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.HighlightThemeProvider

val appContext = LocalContext.current.applicationContext
val theme = HighlightTheme.fromAsset(
    context   = appContext,
    assetPath = "themes/github.css",
    name      = "github",
)

HighlightThemeProvider(lightHighlightTheme = theme) { ... }
```

!!! note
    `fromAsset()` is lazy. CSS parsing happens on first theme usage, not at factory call time.

## Custom theme from raw CSS

```kotlin
val theme = HighlightTheme.fromCss(
    cssText = css,
    name    = "my-runtime-theme",
)
```

## Custom theme from a color map

```kotlin
import dev.hossain.highlight.engine.HljsSelectors

val colorMap = mapOf(
    HljsSelectors.BASE     to SpanStyle(color = Color(0xFF24292E), background = Color(0xFFFFFFFF)),
    HljsSelectors.KEYWORD  to SpanStyle(color = Color(0xFFD73A49), fontWeight = FontWeight.Bold),
    HljsSelectors.STRING   to SpanStyle(color = Color(0xFF032F62)),
    HljsSelectors.COMMENT  to SpanStyle(color = Color(0xFF6A737D), fontStyle = FontStyle.Italic),
)
val theme = HighlightTheme.fromColorMap(
    name             = "my-dynamic-theme",
    colorMap         = colorMap,
    backgroundColor  = Color(0xFFFFFFFF),
    defaultTextColor = Color(0xFF24292E),
)
```

## Theme identity behavior

`HighlightTheme` equality is based on both `name` and content identity. This gives stable
memoization while still triggering re-highlighting when CSS content changes.

- Same name and same CSS content -> equal.
- Same name and different CSS content -> not equal.
- Different names -> not equal.

```kotlin
val light = HighlightTheme.fromCss(lightCss, "custom")
val dark  = HighlightTheme.fromCss(darkCss,  "custom")
light == dark  // false

val a = HighlightTheme.fromCss(css, "custom")
val b = HighlightTheme.fromCss(css, "custom")
a == b         // true
```

## Common pitfalls

- Passing activity context to long-lived themes in non-Compose layers.
- Expecting `fromAsset()` parse failures at construction time rather than first use.
- Recreating custom themes every recomposition instead of `remember`ing stable instances.
