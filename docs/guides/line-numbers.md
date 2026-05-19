# Line Numbers

`SyntaxHighlightedCode` has a built-in line number gutter controlled by `showLineNumbers`.

## Enable line numbers

```kotlin
SyntaxHighlightedCode(
    code            = snippet,
    language        = "kotlin",
    showLineNumbers = true,
)
```

## Customize gutter width and color

The default gutter is `32.dp` wide. Increase it for snippets with many lines:

```kotlin
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
