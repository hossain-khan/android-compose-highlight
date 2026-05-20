# Language Selection

Highlight.js requires a language identifier to tokenize code. This guide covers three helpers to make language selection easier.

## Option 1 - Pass the language name directly

The simplest approach - just pass any Highlight.js language name:

```kotlin
SyntaxHighlightedCode(code = snippet, language = "kotlin")
SyntaxHighlightedCode(code = snippet, language = "typescript")
SyntaxHighlightedCode(code = snippet, language = "sql")
```

For a full list of supported language identifiers, call `engine.supportedLanguages()` at runtime. For the extension-to-language mapping, see the [HighlightLanguage reference](../reference/highlight-language.md).

---

## Option 2 - Resolve from a file extension

`HighlightLanguage.fromExtension()` maps a file extension to its Highlight.js identifier:

```kotlin
import dev.hossain.highlight.engine.HighlightLanguage

val language = HighlightLanguage.fromExtension("kt")  // "kotlin"
val unknown  = HighlightLanguage.fromExtension("xyz") // null
```

Typical usage when displaying a file:

```kotlin
val file = File("MainActivity.kt")
val language = HighlightLanguage.fromExtension(file.extension) ?: "plaintext"

SyntaxHighlightedCode(
    code     = file.readText(),
    language = language,
)
```

See the full [extension map](../reference/highlight-language.md#supported-extensions).

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
engine.highlightAuto(code, theme).onSuccess { result ->
    val detected = result.detectedLanguage // e.g. "python", or "" if undetected
    Text(text = result.annotated)
}
```

> Auto-detection inspects all bundled languages, so it is slower than explicit highlighting. Use it as a fallback, not a default.

---

## Putting it together

A practical pattern combining all options:

```kotlin
suspend fun highlight(file: File, theme: HighlightTheme): AnnotatedString {
    // 1. Try resolving from the extension
    val language = HighlightLanguage.fromExtension(file.extension)

    return if (language != null) {
        // 2. Known extension - highlight directly
        engine.highlight(file.readText(), language, theme)
            .getOrNull()?.annotated
    } else {
        // 3. Unknown extension - fall back to auto-detect
        engine.highlightAuto(file.readText(), theme)
            .getOrNull()?.annotated
    } ?: AnnotatedString(file.readText())
}
```
