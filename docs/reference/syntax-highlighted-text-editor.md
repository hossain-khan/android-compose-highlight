# SyntaxHighlightedTextEditor

!!! warning "Experimental API"
    This composable is annotated with [`@ExperimentalHighlightApi`](#opting-in).
    The API surface may change without a deprecation cycle.

`SyntaxHighlightedTextEditor` is an editable code field (built on `BasicTextField`) with live
syntax highlighting. As users type, highlighting updates after a short debounce while preserving
cursor and selection.

Full API in Dokka:

- [`SyntaxHighlightedTextEditor`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/-syntax-highlighted-text-editor.html)
- [`rememberSyntaxHighlightedEditorValue`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/remember-syntax-highlighted-editor-value.html)
- [`SyntaxHighlightedTextEditorDefaults`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/-syntax-highlighted-text-editor-defaults/index.html)
- [`ExperimentalHighlightApi`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.ui/-experimental-highlight-api/index.html)

## When to use it

- You want an editable code field with built-in async highlighting.
- You want a ready-made `Surface` plus `BasicTextField` wrapper.
- You want to keep editor state (`TextFieldValue`) while highlighting updates in background.

## Key parameters

- `value` and `onValueChange` - editor state and updates.
- `language` - highlight.js language id.
- `theme` - explicit theme or value from `HighlightThemeProvider`.
- `debounceMs` - typing pause before highlight call.
- `contentPadding` and `shape` - layout and clipping of editor surface.
- `onHighlightComplete` and `onError` - observability hooks.

## Opting in

```kotlin
// Option 1 - opt in at the call site
@OptIn(ExperimentalHighlightApi::class)
@Composable
fun MyScreen() {
    SyntaxHighlightedTextEditor(...)
}

// Option 2 - propagate to your own API
@ExperimentalHighlightApi
@Composable
fun MyEditorScreen() {
    SyntaxHighlightedTextEditor(...)
}

// Option 3 - file-wide opt-in (before package statement)
@file:OptIn(ExperimentalHighlightApi::class)
```

## Recommended usage

### With `HighlightThemeProvider` (recommended)

```kotlin
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.SyntaxHighlightedTextEditor
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.hossain.highlight.ui.rememberTomorrowTheme

@OptIn(ExperimentalHighlightApi::class)
@Composable
fun EditorScreen() {
    var editorValue by remember { mutableStateOf(TextFieldValue("fun hello() = println(\"Hello!\")")) }

    HighlightThemeProvider(
        lightHighlightTheme = rememberTomorrowTheme(),
        darkHighlightTheme  = rememberTomorrowNightTheme(),
    ) {
        SyntaxHighlightedTextEditor(
            value          = editorValue,
            onValueChange  = { editorValue = it },
            language       = "kotlin",
            modifier       = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
            shape          = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(12.dp),
        )
    }
}
```

### With an explicit theme (no provider)

```kotlin
@OptIn(ExperimentalHighlightApi::class)
@Composable
fun SqlEditor() {
    var editorValue by remember { mutableStateOf(TextFieldValue("SELECT * FROM users")) }

    SyntaxHighlightedTextEditor(
        value         = editorValue,
        onValueChange = { editorValue = it },
        language      = "sql",
        theme         = rememberTomorrowTheme(),
    )
}
```

## How the highlight pipeline behaves

`SyntaxHighlightedTextEditor` delegates pipeline logic to
`rememberSyntaxHighlightedEditorValue()` and renders the returned value in a `Surface` plus
`BasicTextField` wrapper.

Core behavior:

1. A debounce window waits `debounceMs` after typing stops, then calls `HighlightEngine.highlight()`.
2. Rapid keystrokes cancel prior coroutine work, so only the latest text is highlighted.
3. Stale snapshot checks keep language and theme changes safe.
4. While new results are in flight, previously computed spans are reused where valid, so only the
   actively edited region is briefly unstyled.

## Common pitfalls

- Use `contentPadding` for inner spacing; `.padding()` on `modifier` changes outer layout instead.
- Keep `shape` aligned with any border shape to avoid background bleed at rounded corners.
- Outside `HighlightThemeProvider`, a standalone engine/WebView is created and managed by lifecycle.

## Lower-level helper for custom text fields

Use `rememberSyntaxHighlightedEditorValue()` when you want your own field component
(`OutlinedTextField`, third-party editor, etc.) and only need highlighted `TextFieldValue` output.

```kotlin
val displayValue = rememberSyntaxHighlightedEditorValue(
    value    = editorValue,
    language = "kotlin",
)

OutlinedTextField(
    value         = displayValue,
    onValueChange = { editorValue = it },
    label         = { Text("Code") },
)
```
