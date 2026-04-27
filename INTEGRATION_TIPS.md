# compose-highlight — Integration Tips & Gotchas

Tips and known pitfalls discovered through real-world integration of this library.

---

## 1. Base text color is embedded in the `AnnotatedString` (v0.5.0+)

**Fixed in v0.5.0.** `HtmlToAnnotatedString.convert()` now wraps the entire result in the
`.hljs` base text color as an outer span. Plain tokens (identifiers, whitespace, newlines) inherit
the theme color rather than `LocalContentColor` from the ambient `MaterialTheme`.

This means the result of `rememberHighlightedCode` or `rememberHighlightedCodeBothThemes` is
self-contained — you can pass it directly to `Text()` without specifying an explicit `color`:

```kotlin
val highlighted by rememberHighlightedCode(code, "kotlin")
Text(text = highlighted ?: AnnotatedString(code))  // base color is embedded ✅
```

**Workaround for versions < v0.5.0:** Explicitly pass the theme's default text color:

```kotlin
val textColor = activeTheme.defaultTextColor
    .takeIf { it != Color.Unspecified }
    ?: Color(0xFFCCCCCC)

Text(
    text  = annotatedCode,
    color = textColor,    // ← anchors base color to the theme, not system LocalContentColor
    ...
)
```

**Note:** `SyntaxHighlightedCode` has always handled this correctly regardless of version.

---

## 2. Use `rememberHighlightedCodeBothThemes()` for instant theme toggling

If your UI offers a light/dark toggle, call `rememberHighlightedCodeBothThemes()` instead of
calling `rememberHighlightedCode()` twice or re-running on every toggle:

```kotlin
val themedResult by rememberHighlightedCodeBothThemes(
    code       = code,
    language   = "kotlin",
    lightTheme = HighlightTheme.tomorrow(context),
    darkTheme  = HighlightTheme.tomorrowNight(context),
)

// Switching between light and dark is instant — no re-highlight needed.
val annotatedCode = if (isDark) themedResult?.dark else themedResult?.light
val activeTheme   = if (isDark) darkTheme else lightTheme
```

The JS tokenizer runs **once**; two color maps are applied to the same HTML output. Toggling
`isDark` is a simple `AnnotatedString` swap with no WebView round-trip.

---

## 3. `HighlightTheme` identity is name-based — names must be unique

`HighlightTheme.equals()` / `hashCode()` use only the `name` field (since v0.4.0). Compose APIs
(`remember`, `LaunchedEffect`, `key`) rely on this for change detection.

- ✅ Built-in factory methods (`tomorrow()`, `atomOneDark()`, …) assign fixed unique names.
- ⚠️ If you create custom themes via `fromAsset()` / `fromCss()` / `fromColorMap()`, make sure
  each has a **distinct** `name`. Two themes with the same name will be treated as equal even if
  their color maps differ, and `LaunchedEffect` will silently skip re-highlighting.

---

## 4. `HighlightTheme` objects are lightweight — create them in `remember`

`HighlightTheme` is lazy: CSS parsing only happens when `colorMap` is first accessed. The
constructor itself is cheap. Create themes inside `remember(context)` or `remember(themePair)` so
they survive recompositions without re-parsing:

```kotlin
val (lightTheme, darkTheme) = remember(selectedPair) {
    when (selectedPair) {
        Pair.TOMORROW -> HighlightTheme.tomorrow(context) to HighlightTheme.tomorrowNight(context)
        Pair.ATOM_ONE -> HighlightTheme.atomOneLight(context) to HighlightTheme.atomOneDark(context)
    }
}
```

Do **not** create themes directly in the composable body (outside `remember`) — that allocates a
new object on every recomposition and defeats `rememberHighlightedCodeBothThemes`'s key-based
caching.

---

## 5. `onHighlightComplete` for timing metrics

Both `rememberHighlightedCode` and `rememberHighlightedCodeBothThemes` expose an
`onHighlightComplete: ((Long) -> Unit)?` callback that fires once with the elapsed milliseconds
after the JS round-trip and color-map conversion complete:

```kotlin
var highlightMs by remember { mutableStateOf<Long?>(null) }

val themedResult by rememberHighlightedCodeBothThemes(
    ...
    onHighlightComplete = { ms -> highlightMs = ms },
)

// Display:  "⏱ 42ms"
Text(text = highlightMs?.let { "⏱ ${it}ms" } ?: "⏱ …")
```

The callback fires once per highlight run (i.e. when code, language, or themes change). It does
**not** fire again when the user toggles between light and dark because that is an instant
`AnnotatedString` swap, not a new highlight run.
