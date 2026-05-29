# SyntaxHighlightedTextEditor

!!! warning "Experimental API"
    This composable is annotated with [`@ExperimentalHighlightApi`](#experimentalhighlightapi).
    Its parameter surface may change or be removed in a future release without a deprecation cycle.
    Callers must explicitly opt in - see [Opting in](#opting-in).

An editable code field built on `BasicTextField` with live syntax highlighting. As the user types,
highlighting updates in the background after a short debounce. Cursor position and text selection
are always preserved.

## Signature

```kotlin
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.SyntaxHighlightedTextEditor

@ExperimentalHighlightApi
@Composable
fun SyntaxHighlightedTextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    shape: Shape = RectangleShape,
    theme: HighlightTheme = LocalHighlightTheme.current,
    textStyle: TextStyle = TextStyle(fontFamily = FontFamily.Monospace),
    debounceMs: Long = 150L,
    onHighlightComplete: ((AnnotatedString) -> Unit)? = null,
)
```

## Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `value` | `TextFieldValue` | - | Current value including text, cursor position, and selection |
| `onValueChange` | `(TextFieldValue) -> Unit` | - | Called whenever the user edits the text or moves the cursor |
| `language` | `String` | - | Highlight.js language identifier (e.g. `"kotlin"`, `"python"`, `"sql"`) |
| `modifier` | `Modifier` | `Modifier` | Applied to the outer `Surface`. Do **not** add `.padding()` here - use `contentPadding` |
| `contentPadding` | `PaddingValues` | `PaddingValues(0.dp)` | Padding applied inside the `Surface`, between the background edge and the text |
| `shape` | `Shape` | `RectangleShape` | Clips the `Surface` background. Must match any `.border()` shape in `modifier` |
| `theme` | `HighlightTheme` | `LocalHighlightTheme.current` | Theme to use. Throws if no `HighlightThemeProvider` is present and no explicit theme is passed |
| `textStyle` | `TextStyle` | Monospace | Text style for the editor. The theme's foreground color is merged on top |
| `debounceMs` | `Long` | `150` | Milliseconds to wait after the last keystroke before triggering a highlight call |
| `onHighlightComplete` | `((AnnotatedString) -> Unit)?` | `null` | Called each time a highlight cycle completes. Receives the highlighted `AnnotatedString`. Useful for testing or observing the output without owning the text state |

## Opting in

The composable is marked `@ExperimentalHighlightApi`. Use one of the following patterns:

```kotlin
// Option 1 - opt in at the call site
@OptIn(ExperimentalHighlightApi::class)
@Composable
fun MyScreen() {
    SyntaxHighlightedTextEditor(...)
}

// Option 2 - propagate the requirement to your own API
@ExperimentalHighlightApi
@Composable
fun MyEditorScreen() {
    SyntaxHighlightedTextEditor(...)
}

// Option 3 - opt in for an entire file (place before the package statement)
@file:OptIn(ExperimentalHighlightApi::class)
```

## Usage

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
            value         = editorValue,
            onValueChange = { editorValue = it },
            language      = "kotlin",
            modifier      = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
            shape         = RoundedCornerShape(8.dp),
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
        theme         = HighlightTheme.tomorrow(),
    )
}
```

## How it works

`SyntaxHighlightedTextEditor` delegates all pipeline logic to [`rememberSyntaxHighlightedEditorValue()`](#remembersyntaxhighlightededitorvalue)
and renders the result in a `Surface` + `BasicTextField` layout.

Inside `rememberSyntaxHighlightedEditorValue`:

1. A `LaunchedEffect` keyed on `(value.text, language, theme)` waits for `debounceMs` milliseconds,
   then calls `HighlightEngine.highlight()`. Rapid keystrokes cancel the previous coroutine so only
   one call fires after the user pauses.
2. The result is stored as a `HighlightSnapshot(annotated, language, theme)` - a private data class
   that bundles the highlighted text together with the language and theme that produced it.
3. Stale snapshot detection is **in-composition**: if `snapshot.language != language` or
   `snapshot.theme != theme`, the composable falls back to plain text immediately - no separate
   `LaunchedEffect` is needed to clear state.
4. While a new result is in flight the composable uses one of three display strategies:
   - **No cached result** - plain monospace text (first render or after language/theme change)
   - **Cached text matches current text** - applies the full cached span set (steady state)
   - **Text changed since last result** - clips old spans to the new text length; characters
     before the edit point stay correctly colored, only newly typed characters are briefly unstyled

## Notes

- **`contentPadding` vs `.padding()` on modifier** - padding added via `modifier` shrinks the
  `Surface` layout area, leaving a gap between the border and the background. Always use
  `contentPadding` to ensure the theme background fills the full bordered area.
- **`shape` must match the border shape** - `Surface` clips its background to `shape`. If you
  draw a rounded border via `modifier`, pass the same `RoundedCornerShape` as `shape` so the
  background does not bleed past the rounded corners.
- **Cursor and selection are always preserved** - highlighting calls `value.copy(annotatedString = ...)`
  which keeps the `selection` and `composition` fields untouched.
- **Uses the shared WebView** when called inside a `HighlightThemeProvider`. Outside a provider a
  standalone `HighlightEngine` (and WebView) is created and destroyed with the composable's lifecycle.

---

## rememberSyntaxHighlightedEditorValue

A lower-level `@Composable` helper that runs the debounce + highlight pipeline and returns the
display `TextFieldValue` directly - without rendering any layout. Use this when you want to bring
your own text field (`OutlinedTextField`, a third-party editor, etc.) instead of the built-in
`Surface` + `BasicTextField` wrapper.

`SyntaxHighlightedTextEditor` calls this function internally. They share the same behavior.

### Signature

```kotlin
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.rememberSyntaxHighlightedEditorValue

@ExperimentalHighlightApi
@Composable
fun rememberSyntaxHighlightedEditorValue(
    value: TextFieldValue,
    language: String,
    theme: HighlightTheme = LocalHighlightTheme.current,
    debounceMs: Long = 150L,
    onHighlightComplete: ((AnnotatedString) -> Unit)? = null,
): TextFieldValue
```

The returned `TextFieldValue` is recomputed each time a new highlight result arrives. Because this
function returns a non-Unit type, the Compose compiler marks it **non-restartable** - all internal
state reads automatically subscribe the caller's recompose scope. Use it like any other composable
helper:

```kotlin
val displayValue = rememberSyntaxHighlightedEditorValue(
    value    = editorValue,
    language = "kotlin",
)
```

### Usage - custom text field

```kotlin
@OptIn(ExperimentalHighlightApi::class)
@Composable
fun MyEditor() {
    var editorValue by remember { mutableStateOf(TextFieldValue("fun hello() = println(\"Hello!\")")) }

    HighlightThemeProvider(
        lightHighlightTheme = rememberTomorrowTheme(),
        darkHighlightTheme  = rememberTomorrowNightTheme(),
    ) {
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
}
```

---

## ExperimentalHighlightApi

`@RequiresOptIn` annotation used to mark APIs that are not yet stable. Callers must explicitly opt
in with `@OptIn(ExperimentalHighlightApi::class)` or propagate the annotation to their own API.

Currently applied to:

- `SyntaxHighlightedTextEditor`
- `rememberSyntaxHighlightedEditorValue`

APIs marked `@ExperimentalHighlightApi` may change signature, behavior, or be removed in any
future release without a deprecation cycle.
