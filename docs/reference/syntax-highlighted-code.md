# SyntaxHighlightedCode

The primary public composable. Displays syntax-highlighted code in a styled block.

Shows unstyled monospace code immediately while async highlighting runs, then fades in the highlighted version when ready - no visible flicker.

## Signature

```kotlin
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.CodeBlockStyle

@Composable
fun SyntaxHighlightedCode(
    code: String,
    language: String,
    modifier: Modifier = Modifier,
    theme: HighlightTheme = LocalHighlightTheme.current,
    style: CodeBlockStyle = CodeBlockStyle.Default,
    showLineNumbers: Boolean = false,
    languageLabelContent: (@Composable () -> Unit)? = /* default badge */,
    copyButtonContent: (@Composable (onClick: () -> Unit) -> Unit)? = /* default button */,
    onCopyClick: ((String) -> Unit)? = null,
    onHighlightComplete: ((HighlightResult) -> Unit)? = null,
    onError: ((HighlightException) -> Unit)? = null,
    placeholder: (@Composable (code: String) -> Unit)? = null,
)
```

## Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `code` | `String` | - | Source code to display |
| `language` | `String` | - | Highlight.js language identifier (e.g. `"kotlin"`, `"python"`) |
| `modifier` | `Modifier` | `Modifier` | Modifier for the outer container |
| `theme` | `HighlightTheme` | `LocalHighlightTheme.current` | Theme to use. Throws if no `HighlightThemeProvider` is present and no explicit theme is passed |
| `style` | `CodeBlockStyle` | `CodeBlockStyle.Default` | Visual style - shape, padding, font, copy button size |
| `showLineNumbers` | `Boolean` | `false` | Whether to show a line-number gutter |
| `languageLabelContent` | `(@Composable () -> Unit)?` | Default badge | Header language badge slot. `null` hides it |
| `copyButtonContent` | `(@Composable (onClick: () -> Unit) -> Unit)?` | Default button | Header copy button slot. `null` hides it |
| `onCopyClick` | `((String) -> Unit)?` | `null` | Custom copy handler. If `null`, copies to system clipboard |
| `onHighlightComplete` | `((HighlightResult) -> Unit)?` | `null` | Callback invoked with timing and span count after successful highlighting |
| `onError` | `((HighlightException) -> Unit)?` | `null` | Callback invoked when highlighting fails. Falls back to plain text; purely observational |
| `placeholder` | `(@Composable (code: String) -> Unit)?` | `null` | Composable rendered while highlighting is in progress. `null` shows raw unstyled code (default behavior) |

## Usage

### With `HighlightThemeProvider` (recommended)

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
    code                 = snippet,
    language             = "json",
    languageLabelContent = null,  // hide language badge
    copyButtonContent    = null,  // hide copy button
)
```

### Custom copy button

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code = snippet,
    language = "kotlin",
    copyButtonContent = { onClick ->
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
    languageLabelContent = {
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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

---

## HighlightThemeProvider

Provides a `HighlightTheme` and a shared `HighlightEngine` to all `SyntaxHighlightedCode` composables in its subtree.

```kotlin
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.hossain.highlight.ui.rememberTomorrowTheme

@Composable
fun HighlightThemeProvider(
    darkTheme: Boolean = isSystemInDarkTheme(),
    lightHighlightTheme: HighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme: HighlightTheme = rememberTomorrowNightTheme(),
    content: @Composable () -> Unit,
)
```

Creates **one shared WebView** for the entire subtree. Screens with multiple `SyntaxHighlightedCode` blocks share this single WebView instead of creating one per block - saving ~200 ms warm-up time and ~2-4 MB RAM per extra block.

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
