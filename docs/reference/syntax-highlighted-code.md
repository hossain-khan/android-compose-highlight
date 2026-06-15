# SyntaxHighlightedCode

The primary public composable for rendering syntax-highlighted code blocks in Compose.

It shows plain monospace text immediately, then swaps to highlighted text when async highlighting
completes. This keeps first paint fast and avoids visible flicker.

Full API in Dokka:

- [`SyntaxHighlightedCode`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/-syntax-highlighted-code.html)
- [`HighlightThemeProvider`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/-highlight-theme-provider.html)
- [`rememberHighlightedCode`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-highlighted-code.html)
- [`rememberHighlightedCodeBothThemes`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-highlighted-code-both-themes.html)

## When to use it

- You want a ready-made code block UI with language badge and copy button.
- You want theme-aware code rendering with minimal setup.
- You prefer built-in loading behavior and error fallback over wiring a custom pipeline.

## Key parameters

- `language` - highlight.js language id (`"kotlin"`, `"python"`, `"sql"`, etc).
- `theme` - pass explicitly, or rely on `HighlightThemeProvider`.
- `style` - controls code block visuals (`CodeBlockStyle`).
- `showLineNumbers` - enables line-number gutter.
- `scrollState` - hoisted horizontal scroll state. Pass one when you need to control or observe
  horizontal position from outside the composable.
- `languageLabel` and `copyButton` - slots for header customization, or `null` to hide.
- `placeholder` - custom loading content while highlighting is in progress.
- `onHighlightComplete` and `onError` - observability hooks for metrics and diagnostics.

## Recommended patterns

### Use with `HighlightThemeProvider` (recommended)

```kotlin
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.hossain.highlight.ui.rememberTomorrowTheme

HighlightThemeProvider(
    lightHighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme  = rememberTomorrowNightTheme(),
) {
    SyntaxHighlightedCode(
        code            = snippet,
        language        = "kotlin",
        showLineNumbers = true,
    )
}
```

### With an explicit theme

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberTomorrowTheme

SyntaxHighlightedCode(
    code     = "SELECT * FROM users WHERE active = 1",
    language = "sql",
    theme    = rememberTomorrowTheme(),
)
```

### Hide header elements

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code          = snippet,
    language      = "json",
    languageLabel = null,  // hide language badge
    copyButton    = null,  // hide copy button
)
```

### Custom copy button

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code = snippet,
    language = "kotlin",
    copyButton = { onClick ->
        IconButton(onClick = onClick) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
        }
    },
)
```

### Custom language label

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code = snippet,
    language = "kotlin",
    languageLabel = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("Kotlin", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    },
)
```

### Timing callback

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    onHighlightComplete = { result ->
        Log.d("Perf", "Highlighted in ${result.durationMs}ms, ${result.spanCount} spans")
    },
)
```

### Custom placeholder while loading

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code = snippet,
    language = "kotlin",
    placeholder = { rawCode ->
        Text(
            text = rawCode,
            color = Color.Gray.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
        )
    },
)
```

## Why `HighlightThemeProvider` matters

`HighlightThemeProvider` creates one shared `HighlightEngine` (one hidden WebView) for its subtree.
If you render multiple code blocks on one screen, this avoids creating one WebView per block and
reduces warm-up cost and memory overhead.

### Force a specific theme mode

```kotlin
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.hossain.highlight.ui.rememberTomorrowTheme

HighlightThemeProvider(
    darkTheme           = userPrefersDark,
    lightHighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme  = rememberTomorrowNightTheme(),
) { ... }
```

## Lower-level Compose helpers

Use these when you want custom rendering but still want the library's highlight pipeline.

### `rememberHighlightedCode`

Returns highlighted text as `State<AnnotatedString?>`. Render a plain-text fallback while `null`.
In preview / inspection mode, the helper returns plain text immediately so the composable can
render without starting the WebView-backed engine.

```kotlin
val highlighted by rememberHighlightedCode(code = snippet, language = "kotlin")

Text(text = highlighted ?: AnnotatedString(snippet))
```

Use `onHighlightComplete` for metrics and `onError` for typed failure handling.

```kotlin
val highlighted by rememberHighlightedCode(
    code = snippet,
    language = "kotlin",
    onHighlightComplete = { result ->
        metrics["highlightMs"] = result.durationMs
    },
    onError = { error ->
        Log.d("Highlight", "Failed: ${error.message}")
    },
)
```

### `rememberHighlightedCodeBothThemes`

Highlights once, then returns both light and dark `AnnotatedString` variants so UI theme switching
can happen without extra highlight latency.

```kotlin
val themed by rememberHighlightedCodeBothThemes(
    code = snippet,
    language = "kotlin",
)

Text(
    text = if (isDark) themed?.dark ?: AnnotatedString(snippet)
    else themed?.light ?: AnnotatedString(snippet),
)
```

Inside `HighlightThemeProvider`, light and dark themes default from the provider. Outside a
provider, pass both themes explicitly.
