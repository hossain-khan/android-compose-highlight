# Line Numbers

`SyntaxHighlightedCode` has a built-in line number gutter controlled by `showLineNumbers`.

## Enable line numbers

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code            = snippet,
    language        = "kotlin",
    showLineNumbers = true,
)
```

## Customize gutter width and color

The default gutter is `32.dp` wide. Increase it for snippets with many lines:

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code            = snippet,
    language        = "kotlin",
    showLineNumbers = true,
    style = CodeBlockStyle(
        lineNumberWidth = 48.dp,
        lineNumberColor = Color(0xFF888888),
    ),
)
```

When `lineNumberColor` is `Color.Unspecified` (the default), the line numbers inherit the active theme's foreground color at reduced opacity, so they remain readable against any theme background without manual configuration.

## Typical line count thresholds

| Lines | Recommended `lineNumberWidth` |
|---|---|
| 1-99 | `32.dp` (default) |
| 100-999 | `48.dp` |
| 1000+ | `56.dp` |

## Combining with scroll

For long snippets, pair with a `verticalScroll` modifier:

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code            = longSnippet,
    language        = "kotlin",
    showLineNumbers = true,
    modifier        = Modifier
        .fillMaxWidth()
        .heightIn(max = 400.dp)
        .verticalScroll(rememberScrollState()),
)
```
