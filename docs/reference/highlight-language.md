# HighlightLanguage

`HighlightLanguage` is a pure Kotlin helper that maps file extensions to highlight.js language
identifiers.

It is a convenience API. `SyntaxHighlightedCode` and `HighlightEngine.highlight()` still accept a
plain `String` language id, so you can always pass supported highlight.js names directly.

Full API in Dokka:

- [`HighlightLanguage`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-highlight-language/index.html)

## When to use it

- You have filenames or file extensions and need a best-effort language id.
- You want locale-safe, case-insensitive extension lookup.
- You want a simple fallback to `plaintext` for unknown extensions.

## Core behavior

- `fromExtension(extension)` returns a highlight.js language id or `null`.
- Lookup is case-insensitive and locale-safe (`Locale.ROOT`).
- Pass extension without a leading dot.

```kotlin
HighlightLanguage.fromExtension("kt")   // "kotlin"
HighlightLanguage.fromExtension("KT")   // "kotlin"
HighlightLanguage.fromExtension("py")   // "python"
HighlightLanguage.fromExtension("xyz")  // null
```

## Recommended usage pattern

```kotlin
val file = File("MainActivity.kt")
val language = HighlightLanguage.fromExtension(file.extension) ?: "plaintext"

SyntaxHighlightedCode(
    code     = file.readText(),
    language = language,
)
```

## Practical guidance

- Prefer mapping by extension only when language is not known from other metadata.
- For user-selected languages, pass the chosen id directly.
- For unknown or mixed content, consider `HighlightEngine.highlightAuto()` instead.

## Common pitfalls

- Passing extension with dot (`".kt"`) instead of `"kt"`.
- Assuming the helper mirrors every upstream highlight.js alias at all times.
- Forgetting a fallback path (`?: "plaintext"`) for unknown extensions.
