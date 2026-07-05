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

## Fallback colors for custom themes

When a custom `HighlightTheme` CSS omits the base `.hljs { background: ...; color: ... }` rule,
`CodeBlockStyle` uses fallback colors so the block still renders correctly:

| Parameter | Default | When it applies |
|---|---|---|
| `fallbackBackgroundColor` | `Color(0xFF1E1E1E)` (dark grey) | `HighlightTheme.backgroundColor` is `Color.Unspecified` |
| `fallbackTextColor` | `Color(0xFFD4D4D4)` (light grey) | `HighlightTheme.defaultTextColor` is `Color.Unspecified` |

Override them to match your app's branding when using a stripped-down custom theme:

```kotlin
val myStyle = CodeBlockStyle(
    fallbackBackgroundColor = Color(0xFF0D1117), // GitHub dark background
    fallbackTextColor       = Color(0xFFC9D1D9), // GitHub dark foreground
)

SyntaxHighlightedCode(code = snippet, language = "kotlin", style = myStyle)
```

!!! note
    Built-in themes (Tomorrow, Atom One, GitHub, Dracula, Alucard) always include a full
    `.hljs` rule, so `fallbackBackgroundColor` and `fallbackTextColor` have no effect when using them.
    They only matter for custom `fromAsset()` or `fromCss()` themes that omit that rule.

## Common pitfalls

- Overriding `textStyle.color` and expecting it to win over theme foreground.
- Using too-small `copyButtonSize` and reducing touch target usability.
- Mismatched `shape` and outer container decoration, causing clipped or inconsistent edges.
- Not providing fallback colors when using a minimal custom theme without a base `.hljs` rule,
  leading to invisible text or a transparent background.
