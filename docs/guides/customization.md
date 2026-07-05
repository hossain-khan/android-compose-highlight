# Customization

## Language label

The language label is a composable slot - replace it with any `@Composable`:

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    languageLabel = {
        Text(
            text     = "Kotlin",
            color    = Color.White,
            fontSize = 11.sp,
            modifier = Modifier
                .background(Color(0xFF7F52FF), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    },
)
```

Pass `null` to hide the label entirely:

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(code = snippet, language = "kotlin", languageLabel = null)
```

### Toggle the default label at runtime

Use `SyntaxHighlightedCodeDefaults.LanguageLabel` to keep the default look while
toggling visibility - no need to reconstruct the styling yourself:

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

var showLabel by remember { mutableStateOf(true) }

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    languageLabel = if (showLabel) {
        { SyntaxHighlightedCodeDefaults.LanguageLabel("kotlin") }
    } else null,
)
```

## Copy button

### Custom icon

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    copyButton = { onClick ->
        IconButton(onClick = onClick) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy code")
        }
    },
)
```

### Hide copy button

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(code = snippet, language = "kotlin", copyButton = null)
```

### Custom copy feedback (Snackbar, Toast, etc.)

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

val snackbarHostState = remember { SnackbarHostState() }
val scope             = rememberCoroutineScope()

SyntaxHighlightedCode(
    code        = snippet,
    language    = "kotlin",
    onCopyClick = { copiedText ->
        scope.launch { snackbarHostState.showSnackbar("Copied!") }
    },
)
```

!!! note
    When `onCopyClick` is `null` (the default), the button copies to the system clipboard automatically. Supply `onCopyClick` only to add custom feedback or to override the copy behavior.

### Adjust copy button touch target

```kotlin
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    style    = CodeBlockStyle(copyButtonSize = 48.dp),
)
```

## Block shape and padding

```kotlin
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    style    = CodeBlockStyle(
        shape         = RoundedCornerShape(4.dp),
        padding       = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        headerPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ),
)
```

## Custom font

```kotlin
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

val firaCode = FontFamily(Font(R.font.fira_code))

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    style    = CodeBlockStyle(
        textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(
            fontFamily = firaCode,
            fontSize   = 14.sp,
        ),
    ),
)
```

## Line number styling

```kotlin
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code            = snippet,
    language        = "kotlin",
    showLineNumbers = true,
    style           = CodeBlockStyle(
        lineNumberWidth = 40.dp,
        lineNumberColor = Color(0xFF888888),
    ),
)
```

## Compose modifier

Apply any modifier to the outer container:

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 300.dp)
        .verticalScroll(rememberScrollState()),
)
```

## Accessibility

The code text is fully selectable (via `SelectionContainer`) and the copy button uses `contentDescription` for accessibility. To provide a localized description:

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    copyButton = { onClick ->
        SyntaxHighlightedCodeDefaults.CopyButton(
            onClick            = onClick,
            contentDescription = stringResource(R.string.copy_code_a11y),
        )
    },
)
```

## Fallback background and text colors

When you use a custom `HighlightTheme` (via `fromAsset()` or `fromCss()`) whose CSS omits the
base `.hljs { background: ...; color: ... }` rule, the block would otherwise render with a
transparent background and invisible text. Override the two fallback parameters on `CodeBlockStyle`
to control what is shown in that case:

```kotlin
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    style    = CodeBlockStyle(
        fallbackBackgroundColor = Color(0xFF0D1117),
        fallbackTextColor       = Color(0xFFC9D1D9),
    ),
)
```

!!! note
    Built-in themes (Tomorrow, Atom One, GitHub, Dracula, Alucard) always include a full
    `.hljs` rule, so these fallback colors have no visible effect when using them.
