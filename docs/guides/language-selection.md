# Language Selection

Highlight.js requires a language identifier to tokenize code. This guide covers four options for language selection, including three helper APIs.

## Option 1 - Pass the language name directly

The simplest approach - just pass any Highlight.js language name:

```kotlin
SyntaxHighlightedCode(code = snippet, language = "kotlin")
SyntaxHighlightedCode(code = snippet, language = "typescript")
SyntaxHighlightedCode(code = snippet, language = "sql")
```

For the complete list of supported language identifiers, use `HighlightLanguage.all` (available synchronously) or call `engine.supportedLanguages()` at runtime. For the extension-to-language mapping and alias normalization, see the [HighlightLanguage reference](../reference/highlight-language).

---

## Option 2 - Resolve from a file extension or alias

`HighlightLanguage.canonicalName()` normalizes language names and short aliases (e.g. `kt` -> `kotlin`, `js` -> `javascript`, `sh` -> `bash`), while `HighlightLanguage.fromExtension()` maps file extensions:

```kotlin
import dev.hossain.highlight.engine.HighlightLanguage

// Normalize aliases or check support synchronously
val language = HighlightLanguage.canonicalName("kt") // "kotlin"
val supported = HighlightLanguage.isSupported("html") // true

// Map a file extension
val fromExt = HighlightLanguage.fromExtension("kt")  // "kotlin"
val unknown = HighlightLanguage.fromExtension("xyz") // null
```

Typical usage when displaying a file:

```kotlin
// Use an absolute path - relative paths resolve against "/" on Android
val file = File(LocalContext.current.filesDir, "MainActivity.kt")
val language = HighlightLanguage.fromExtension(file.extension) ?: "plaintext"
// Read off the main thread
val code by produceState("", file) { value = withContext(Dispatchers.IO) { file.readText() } }

SyntaxHighlightedCode(
    code     = code,
    language = language,
)
```

See the full [extension map](../reference/highlight-language#supported-extensions).

---

## Option 3 - Look up a language by name or alias

`HighlightEngine.getLanguage()` validates that a language is recognized by the bundled Highlight.js and returns its metadata:

```kotlin
engine.getLanguage("ts").onSuccess { info ->
    if (info != null) {
        println(info.name)    // "TypeScript"
        println(info.aliases) // [ts, tsx, mts, cts]
    } else {
        println("Not recognized")
    }
}
```

Useful when accepting user-supplied language input and wanting to validate it before passing it to `highlight()`.

---

## Option 4 - Auto-detect the language

When the language is completely unknown, use `highlightAuto()` to let Highlight.js guess:

```kotlin
var detected  by remember { mutableStateOf("") }
var annotated by remember(code) { mutableStateOf(AnnotatedString(code)) }

LaunchedEffect(code, theme) {
    engine.highlightAuto(code, theme).onSuccess { result ->
        detected  = result.detectedLanguage // e.g. "python", or "" if undetected
        annotated = result.annotated
    }
}
Text(text = annotated)
```

> Auto-detection inspects all bundled languages, so it is slower than explicit highlighting. Use it as a fallback, not a default.

---

## Putting it together

A practical pattern combining all options:

```kotlin
// `engine` is a HighlightEngine, e.g. a ViewModel property
suspend fun highlight(file: File, theme: HighlightTheme): AnnotatedString {
    val code = withContext(Dispatchers.IO) { file.readText() }
    // 1. Try resolving from the extension
    val language = HighlightLanguage.fromExtension(file.extension)

    return if (language != null) {
        // 2. Known extension - highlight directly
        engine.highlight(code, language, theme)
            .getOrNull()?.annotated
    } else {
        // 3. Unknown extension - fall back to auto-detect
        engine.highlightAuto(code, theme)
            .getOrNull()?.annotated
    } ?: AnnotatedString(code)
}
```
