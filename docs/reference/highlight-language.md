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
