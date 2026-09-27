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

## Header actions

The `actions` slot runs with a `RowScope` receiver and provides a pre-wired `onCopy` action.
Use it to provide custom trailing buttons, multiple actions, or hide trailing actions.

### Custom copy icon

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    actions  = { onCopy ->
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy code")
        }
    },
)
```

### Multiple action buttons

Because `actions` runs within a `RowScope`, multiple action buttons lay out horizontally without requiring a custom `Row`:

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    actions  = { onCopy ->
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit code")
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "Share code")
        }
        SyntaxHighlightedCodeDefaults.CopyButton(onClick = onCopy)
    },
)
```

### Hide trailing actions

Pass `null` to `actions` to hide all trailing actions (including the default copy button) while keeping the language label:

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    actions  = null,
)
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

## Custom header chrome

Use the coarse-grained `header` slot to replace the entire header row inside the card (for example, with a file
title bar, IDE tabs, or custom background):

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    header   = { onCopy ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("MainActivity.kt", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            SyntaxHighlightedCodeDefaults.CopyButton(onClick = onCopy)
        }
    },
)
```

When `header` is provided, `languageLabel` and `actions` are ignored.

### Headerless mode

Pass `header = null` to omit the header row completely in a single parameter:

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    header   = null,
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
    actions = { onCopy ->
        SyntaxHighlightedCodeDefaults.CopyButton(
            onClick            = onCopy,
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
