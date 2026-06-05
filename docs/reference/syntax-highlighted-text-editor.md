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
- `keyboardOptions` - keyboard / IME behavior. Defaults to `SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions` (autocorrect off, autocapitalization off, Ascii keyboard) - the right defaults for source code. Override via `.copy(imeAction = ...)` to keep the code-friendly settings while customising one field.
- `cursorBrush` - text cursor color. Defaults to `null`, which derives the cursor from the theme's text color so it stays visible on both light and dark themes. Pass an explicit `Brush` (e.g. `SolidColor(MaterialTheme.colorScheme.primary)`) to override.
- `onHighlightComplete` and `onError` - observability hooks. `onHighlightComplete` receives a `HighlightResult` (timing, span count, language); see the [API docs](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-highlight-result/index.html).

For anything else `BasicTextField` exposes (`enabled`, `readOnly`, `singleLine`, `maxLines`, `decorationBox`, etc.) drop one level down to [`rememberSyntaxHighlightedEditorValue`](#lower-level-helper-for-custom-text-fields) and render your own field - the editor is intentionally opinionated.

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

### Customize the editor text style

`SyntaxHighlightedTextEditorDefaults.DefaultTextStyle` is the pre-allocated singleton
the editor uses by default (monospace family). Build on top of it with `.copy(...)`
to override only what you need - using the singleton avoids allocating a fresh
`TextStyle` on every recomposition while typing.

```kotlin
import dev.hossain.highlight.ui.SyntaxHighlightedTextEditorDefaults

@OptIn(ExperimentalHighlightApi::class)
@Composable
fun LargerEditor() {
    var editorValue by remember { mutableStateOf(TextFieldValue("")) }
    val editorStyle = SyntaxHighlightedTextEditorDefaults.DefaultTextStyle.copy(fontSize = 15.sp)

    SyntaxHighlightedTextEditor(
        value         = editorValue,
        onValueChange = { editorValue = it },
        language      = "kotlin",
        textStyle     = editorStyle,
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

## Why aren't `enabled`, `readOnly`, `singleLine`, etc. exposed?

`SyntaxHighlightedTextEditor` is a deliberate convenience composable: it ships code-friendly
defaults (`keyboardOptions`, `cursorBrush`, monospace `textStyle`, debounce window, theme-aware
surface) and exposes only the parameters needed to tune them. It is **not** a parameterized
clone of `BasicTextField`.

For any other `BasicTextField` parameter you might want - `enabled`, `readOnly`, `singleLine`,
`maxLines`, `minLines`, `keyboardActions`, `visualTransformation`, `onTextLayout`,
`interactionSource`, `decorationBox` - drop one level down to
[`rememberSyntaxHighlightedEditorValue`](#lower-level-helper-for-custom-text-fields) and render
your own field. The helper returns a `TextFieldValue` with highlighting applied and cursor /
selection preserved; you compose it into whatever field shape you need.

This split keeps the convenience composable's surface small (and stable - it doesn't grow as
Compose Foundation adds new `BasicTextField` parameters) while still giving you full control
when you need it.

## Lower-level helper for custom text fields

Use `rememberSyntaxHighlightedEditorValue()` when you want your own field component
(`OutlinedTextField`, third-party editor, etc.) and only need highlighted `TextFieldValue` output.

```kotlin
@OptIn(ExperimentalHighlightApi::class)
@Composable
fun MyCustomEditor() {
    var editorValue by remember { mutableStateOf(TextFieldValue("")) }
    val displayValue = rememberSyntaxHighlightedEditorValue(
        value    = editorValue,
        language = "kotlin",
    )

    OutlinedTextField(
        value         = displayValue,
        onValueChange = { editorValue = it },
        label         = { Text("Code") },
    )
}
```
