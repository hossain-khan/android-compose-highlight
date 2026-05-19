# Customization

## Language label

The language label is a composable slot — replace it with any `@Composable`:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    languageLabelContent = {
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

SyntaxHighlightedCode(code = snippet, language = "kotlin", languageLabelContent = null)
```

## Copy button

### Custom icon

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    copyButtonContent = { onClick ->
        IconButton(onClick = onClick) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy code")
        }
    },
)
```

### Hide copy button

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedCode

SyntaxHighlightedCode(code = snippet, language = "kotlin", copyButtonContent = null)
```

### Custom copy feedback (Snackbar, Toast, etc.)

```kotlin
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import kotlinx.coroutines.launch

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
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import androidx.compose.ui.res.stringResource
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

SyntaxHighlightedCode(
    code     = snippet,
    language = "kotlin",
    copyButtonContent = { onClick ->
        SyntaxHighlightedCodeDefaults.CopyButton(
            onClick            = onClick,
            contentDescription = stringResource(R.string.copy_code_a11y),
        )
    },
)
```
