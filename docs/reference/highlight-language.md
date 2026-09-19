# HighlightLanguage

`HighlightLanguage` is a pure Kotlin catalog and helper for Highlight.js language identifiers.

It provides synchronous access to the full list of supported languages (`all`), curated primary
languages (`primary`), support checking (`isSupported`), alias normalization (`canonicalName`),
and file extension mapping (`fromExtension`).

It is a convenience API. `SyntaxHighlightedCode` and `HighlightEngine.highlight()` still accept a
plain `String` language id, so you can always pass supported Highlight.js names directly.

Full API in Dokka:

- [`HighlightLanguage`](https://hossain-khan.github.io/android-compose-highlight/api/compose-highlight/dev.hossain.highlight.engine/-highlight-language/index.html)

## When to use it

- You need the list of supported languages synchronously (e.g. for language pickers, sheets, or autocomplete) without initializing a WebView.
- You want to display curated common languages (`primary`) in quick-access UI chips.
- You want to validate whether a language name or alias is supported (`isSupported`).
- You want to normalize common aliases like `kt`, `js`, `ts`, `py`, `sh`, or `html` to canonical grammar names (`canonicalName`).
- You have filenames or file extensions and need a best-effort language id (`fromExtension`).

## Core behavior

### Static Language Catalog

- `HighlightLanguage.all` returns all 194 supported language identifiers in alphabetical order (including `html`).
- `HighlightLanguage.primary` returns 20 curated high-demand languages for pickers and filter chips.

```kotlin
// Full synchronous catalog (194 languages)
val languages = HighlightLanguage.all

// Curated primary languages for chips or pickers
val quickPickers = HighlightLanguage.primary
```

### Alias Normalization & Validation

- `HighlightLanguage.isSupported(language)` returns `true` if the language, alias, or file extension is recognized.
- `HighlightLanguage.canonicalName(nameOrAlias)` resolves an alias or name to its canonical Highlight.js identifier.

```kotlin
HighlightLanguage.isSupported("kt")         // true
HighlightLanguage.isSupported("html")       // true
HighlightLanguage.isSupported("unknown")    // false

HighlightLanguage.canonicalName("kt")       // "kotlin"
HighlightLanguage.canonicalName("KOTLIN")   // "kotlin"
HighlightLanguage.canonicalName("js")       // "javascript"
HighlightLanguage.canonicalName("ts")       // "typescript"
HighlightLanguage.canonicalName("py")       // "python"
HighlightLanguage.canonicalName("sh")       // "bash"
HighlightLanguage.canonicalName("html")     // "html"
HighlightLanguage.canonicalName("unknown")  // null
```

### File Extension Mapping

- `fromExtension(extension)` returns a Highlight.js language id or `null`.
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

- For UI pickers and autocomplete dropdowns, use `HighlightLanguage.all` or `HighlightLanguage.primary` directly.
- Prefer `HighlightLanguage.canonicalName()` to normalize user input or short aliases before storing or querying.
- Prefer mapping by extension only when language is not known from other metadata.
- For unknown or mixed content, consider `HighlightEngine.highlightAuto()` instead.

## Common pitfalls

- Passing extension with dot (`".kt"`) instead of `"kt"`.
- Forgetting a fallback path (`?: "plaintext"`) for unknown extensions.

## Supported extensions

The current extension-to-language map (source: [`HighlightLanguage.kt`](https://github.com/hossain-khan/android-compose-highlight/blob/main/compose-highlight/src/main/kotlin/dev/hossain/highlight/engine/HighlightLanguage.kt)).

| Language | Extensions |
|---|---|
| `kotlin` | `kt`, `kts` |
| `java` | `java` |
| `python` | `py`, `pyw`, `pyi` |
| `javascript` | `js`, `mjs`, `cjs`, `jsx` |
| `typescript` | `ts`, `mts`, `cts`, `tsx` |
| `c` | `c`, `h` |
| `cpp` | `cpp`, `cc`, `cxx`, `hpp`, `hh` |
| `csharp` | `cs` |
| `rust` | `rs` |
| `go` | `go` |
| `swift` | `swift` |
| `ruby` | `rb`, `rbw` |
| `php` | `php`, `phtml` |
| `scala` | `scala` |
| `groovy` | `groovy` |
| `gradle` | `gradle` |
| `dart` | `dart` |
| `elixir` | `ex`, `exs` |
| `erlang` | `erl`, `hrl` |
| `haskell` | `hs`, `lhs` |
| `fsharp` | `fs`, `fsi`, `fsx` |
| `ocaml` | `ml`, `mli` |
| `clojure` | `clj`, `cljs`, `cljc` |
| `lua` | `lua` |
| `r` | `r` |
| `objectivec` | `m`, `mm` |
| `perl` | `pl`, `pm` |
| `bash` | `sh`, `bash`, `zsh` |
| `powershell` | `ps1`, `psm1`, `psd1` |
| `sql` | `sql` |
| `html` | `html`, `htm` |
| `xml` | `xhtml`, `xml`, `svg`, `xsl` |
| `css` | `css` |
| `scss` | `scss` |
| `less` | `less` |
| `json` | `json`, `jsonc` |
| `yaml` | `yaml`, `yml` |
| `toml` | `toml` |
| `markdown` | `md`, `markdown` |
| `dockerfile` | `dockerfile` |
| `makefile` | `makefile`, `mk` |
| `latex` | `tex`, `latex` |
| `diff` | `diff`, `patch` |
| `ini` | `ini`, `cfg`, `conf` |
| `properties` | `properties` |
| `vim` | `vim` |
| `cmake` | `cmake` |
| `protobuf` | `proto` |
| `glsl` | `glsl` |
| `dos` | `bat`, `cmd` |
| `x86asm` | `asm`, `s` |
| `graphql` | `graphql`, `gql` |
| `plaintext` | `txt` |
| `julia` | `jl` |
| `nim` | `nim`, `nims` |
| `vbnet` | `vb` |
| `vbscript` | `vbs` |
| `coffeescript` | `coffee` |
| `wasm` | `wat` |
| `haml` | `haml` |
| `handlebars` | `hbs`, `handlebars` |
| `stylus` | `styl` |
| `crystal` | `cr` |
| `elm` | `elm` |
| `haxe` | `hx` |
| `scheme` | `scm`, `ss` |
| `qml` | `qml` |
| `d` | `d` |
| `fortran` | `f`, `f90`, `f95`, `for` |
| `awk` | `awk` |
| `tcl` | `tcl`, `tk` |
| `lisp` | `lisp`, `lsp` |
| `applescript` | `applescript`, `scpt` |
| `nix` | `nix` |
| `nginx` | `nginx` |
| `pgsql` | `pgsql` |
| `prolog` | `pro` |
