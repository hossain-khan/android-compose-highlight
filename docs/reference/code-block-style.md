# CodeBlockStyle

Visual style configuration for `SyntaxHighlightedCode`.

## Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `shape` | `Shape` | `RoundedCornerShape(8.dp)` | Shape applied to the outer container |
| `padding` | `PaddingValues` | `PaddingValues(16.dp)` | Inner padding between container edge and code content |
| `headerPadding` | `PaddingValues` | `PaddingValues(horizontal=16.dp, vertical=8.dp)` | Padding for the header row (language badge + copy button) |
| `lineNumberColor` | `Color` | `Color.Unspecified` | Line number gutter text color. Unspecified = theme foreground at 40% opacity |
| `lineNumberWidth` | `Dp` | `32.dp` | Width reserved for the line number gutter |
| `copyButtonSize` | `Dp` | `32.dp` | Width and height of the copy button touch target |
| `textStyle` | `TextStyle` | See below | Font family, size, line height for the code text |

The default `textStyle` is `SyntaxHighlightedCodeDefaults.codeTextStyle`: monospace font, 13 sp size, 20 sp line height.

!!! note
    The theme's foreground color is applied on top of `textStyle.color` at render time — any explicit `color` you set here is overridden by the active `HighlightTheme`.

## Presets

```kotlin
import dev.hossain.highlight.ui.CodeBlockStyle

// Standard - rounded corners, 16 dp padding
CodeBlockStyle.Default

// Compact - reduced padding for space-constrained layouts
CodeBlockStyle.Compact
```

## Custom style

```kotlin
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode

val myStyle = CodeBlockStyle(
    shape           = RoundedCornerShape(4.dp),
    padding         = PaddingValues(8.dp),
    headerPadding   = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    lineNumberWidth = 40.dp,
    copyButtonSize  = 24.dp,
)
SyntaxHighlightedCode(code = snippet, language = "bash", style = myStyle)
```

## Custom typography

```kotlin
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

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

## Copy button size

```kotlin
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code  = snippet,
    language = "kotlin",
    style = CodeBlockStyle(copyButtonSize = 48.dp),
)
```

Wrap inline styles in `remember` to avoid unnecessary recompositions:

```kotlin
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.ui.CodeBlockStyle

val myStyle = remember { CodeBlockStyle(padding = PaddingValues(8.dp)) }
```

---

## SyntaxHighlightedCodeDefaults

Object providing default constants and helper composables.

| Member | Description |
|---|---|
| `codeTextStyle` | Default `TextStyle`: monospace, 13 sp, 20 sp line height |
| `shape` | Default shape: `RoundedCornerShape(8.dp)` |
| `padding` | Default padding: `PaddingValues(16.dp)` |
| `headerPadding` | Default header padding: `PaddingValues(horizontal=16.dp, vertical=8.dp)` |
| `lineNumberWidth` | Default gutter width: `32.dp` |
| `copyButtonSize` | Default copy button size: `32.dp` |
| `CopyButton(onClick, tint, contentDescription, size)` | Default copy button composable (renders `⧉` icon) |
| `LanguageLabel(language, color, fontSize)` | Default language badge composable |

### Using `CopyButton` with custom accessibility label

```kotlin
import androidx.compose.ui.res.stringResource
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

SyntaxHighlightedCode(
    code = snippet,
    language = "kotlin",
    copyButtonContent = { onClick ->
        SyntaxHighlightedCodeDefaults.CopyButton(
            onClick            = onClick,
            contentDescription = stringResource(R.string.copy_code_label),
        )
    },
)
```

### Toggling the language label at runtime

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

var showLabel by remember { mutableStateOf(true) }

SyntaxHighlightedCode(
    code = snippet,
    language = "kotlin",
    languageLabelContent = if (showLabel) {
        { SyntaxHighlightedCodeDefaults.LanguageLabel("kotlin") }
    } else null,
)
```
