# HighlightTheme

`HighlightTheme` maps `hljs-*` token classes to Compose `SpanStyle` values used by the highlight
pipeline.

Full API in Dokka:

- [`HighlightTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-highlight-theme/index.html)
- [`HighlightThemeDescriptor`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-highlight-theme-descriptor/index.html)
- [`rememberTomorrowLightTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-tomorrow-light-theme.html)
- [`rememberTomorrowNightTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-tomorrow-night-theme.html)
- [`rememberAtomOneDarkTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-atom-one-dark-theme.html)
- [`rememberAtomOneLightTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-atom-one-light-theme.html)
- [`rememberGithubLightTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-github-light-theme.html)
- [`rememberGithubDarkTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-github-dark-theme.html)
- [`rememberDraculaDarkTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-dracula-dark-theme.html)
- [`rememberAlucardLightTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-alucard-light-theme.html)
- [`rememberDraculaLightTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-dracula-light-theme.html) (alias)
- [`rememberAlucardDarkTheme`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-alucard-dark-theme.html) (alias)

## When to use each theme source

- Built-in themes (`tomorrow`, `tomorrowNight`, `atomOneDark`, `atomOneLight`, `githubLight`, `githubDark`, `draculaDark`, `alucardLight`) and their aliases: fastest setup,
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
import dev.hossain.highlight.ui.rememberTomorrowLightTheme

HighlightThemeProvider(
    lightHighlightTheme = rememberTomorrowLightTheme(),
    darkHighlightTheme  = rememberTomorrowNightTheme(),
) { ... }
```

Other bundled pairs now available:

- `rememberGithubLightTheme()` + `rememberGithubDarkTheme()`
- `rememberAlucardLightTheme()` + `rememberDraculaDarkTheme()` (or aliases `rememberDraculaLightTheme()` + `rememberAlucardDarkTheme()`)

## Theme discovery and persistence

When building user-customizable theme pickers (e.g. `LazyRow`, `DropdownMenu`) or storing user theme preferences
in `DataStore` or `SharedPreferences`, use the discovery APIs on `HighlightTheme.Companion`:

- `HighlightTheme.bundled`: List of all 8 bundled theme descriptors in canonical order.
- `HighlightTheme.bundledLight`: Precomputed list filtered to light themes (`isLight == true`).
- `HighlightTheme.bundledDark`: Precomputed list filtered to dark themes (`isDark == true`).
- `HighlightTheme.findBundledById(id)`: O(1) lookup returning the matching `HighlightThemeDescriptor?` by stable ID.

Each [`HighlightThemeDescriptor`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-highlight-theme-descriptor/index.html)
provides metadata:

- `id`: Stable string identifier (`"tomorrow"`, `"tomorrow-night"`, `"atom-one-light"`,
  `"atom-one-dark"`, `"github"`, `"github-dark"`, `"alucard"`, `"dracula"`).
- `displayName`: Human-readable name for UI presentation (`"Tomorrow"`, `"Tomorrow Night"`, `"Dracula"`, etc.).
- `isDark` / `isLight`: Mode indicators for filtering or matching the current system theme.
- `theme` / `create()`: Lazily created and cached `HighlightTheme` instance.

### Building a theme picker in Compose

```kotlin
LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(HighlightTheme.bundledDark) { descriptor ->
        ThemeChip(
            label = descriptor.displayName,
            selected = selectedThemeId == descriptor.id,
            onClick = { onThemeSelected(descriptor.id) },
        )
    }
}
```

### Persisting and restoring themes

Save `descriptor.id` into your persistence store. On app launch or restore, look up the descriptor and safely fall
back if the ID is unrecognized or missing:

```kotlin
val savedId = preferences.getString("theme_id", "tomorrow")
val activeTheme = HighlightTheme.findBundledById(savedId)?.create()
    ?: if (isSystemInDarkTheme()) HighlightTheme.tomorrowNight()
    else HighlightTheme.tomorrow()
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
import dev.hossain.highlight.engine.HighlightTheme

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
