# CodeBlockStyle

`CodeBlockStyle` controls the visual presentation of `SyntaxHighlightedCode`.

Full API in Dokka:

- [`CodeBlockStyle`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/-code-block-style/index.html)
- [`SyntaxHighlightedCodeDefaults`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/-syntax-highlighted-code-defaults/index.html)

## When to customize it

- You need denser or more spacious code blocks for your layout.
- You want to align border radius, padding, and header density with your design system.
- You need line-number and copy-button sizing adjustments for accessibility or compact UI.

## Presets

```kotlin
import dev.hossain.highlight.ui.CodeBlockStyle

CodeBlockStyle.Default
CodeBlockStyle.Compact
```

## Typical custom style

```kotlin
val myStyle = CodeBlockStyle(
    shape           = RoundedCornerShape(4.dp),
    padding         = PaddingValues(8.dp),
    headerPadding   = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    lineNumberWidth = 40.dp,
    copyButtonSize  = 24.dp,
)

SyntaxHighlightedCode(code = snippet, language = "bash", style = myStyle)
```

## Typography customization

```kotlin
SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    style    = CodeBlockStyle(
        textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(
            fontSize   = 15.sp,
            lineHeight = 24.sp,
            fontFamily = FontFamily.Serif,
        ),
    ),
)
```

!!! note
    The active `HighlightTheme` applies foreground color at render time. Explicit
    `textStyle.color` is overridden by theme color.

## Recomposition guidance

Wrap inline style creation in `remember` to avoid creating new style objects every recomposition.

```kotlin
val myStyle = remember { CodeBlockStyle(padding = PaddingValues(8.dp)) }
```

## Common pitfalls

- Overriding `textStyle.color` and expecting it to win over theme foreground.
- Using too-small `copyButtonSize` and reducing touch target usability.
- Mismatched `shape` and outer container decoration, causing clipped or inconsistent edges.
