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

1. The composable maintains a `highlighted: AnnotatedString?` state and a `TextFieldValue` for the
   field itself.
2. A `LaunchedEffect` keyed on `(value.text, language, theme)` waits for `debounceMs` milliseconds,
   then calls `HighlightEngine.highlight()`. Rapid keystrokes cancel the previous coroutine so only
   one call fires after the user pauses.
3. When `language` or `theme` changes, a separate `LaunchedEffect` immediately clears the cached
   spans so stale highlights from a prior language or theme are never shown.
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

## ExperimentalHighlightApi

`@RequiresOptIn` annotation used to mark APIs that are not yet stable. Callers must explicitly opt
in with `@OptIn(ExperimentalHighlightApi::class)` or propagate the annotation to their own API.

Currently applied to:

- `SyntaxHighlightedTextEditor`

APIs marked `@ExperimentalHighlightApi` may change signature, behavior, or be removed in any
future release without a deprecation cycle.
