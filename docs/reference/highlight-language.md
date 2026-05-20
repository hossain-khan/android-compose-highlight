# HighlightLanguage

A pure-Kotlin helper that maps file extensions to Highlight.js language identifiers.

This is a **convenience helper only**. The `language` parameter on `SyntaxHighlightedCode` and `HighlightEngine.highlight()` is still a plain `String` - you can always pass any Highlight.js language name directly. `HighlightLanguage` just makes it easier to resolve the right name from a file extension.

## `fromExtension()`

```kotlin
fun fromExtension(extension: String): String?
```

Returns the Highlight.js language identifier for the given file extension (without a leading dot), or `null` if the extension is not recognized.

The lookup is case-insensitive and locale-safe (uses `Locale.ROOT`).

```kotlin
HighlightLanguage.fromExtension("kt")   // "kotlin"
HighlightLanguage.fromExtension("KT")   // "kotlin"
HighlightLanguage.fromExtension("py")   // "python"
HighlightLanguage.fromExtension("xyz")  // null
```

## Usage - resolve language from a filename

```kotlin
val file = File("MainActivity.kt")
val extension = file.extension          // "kt"
val language = HighlightLanguage.fromExtension(extension) ?: "plaintext"

SyntaxHighlightedCode(
    code     = file.readText(),
    language = language,
)
```

## Supported extensions

Extensions are grouped by language family below.

| Extensions | Language |
|---|---|
| `kt`, `kts` | kotlin |
| `java` | java |
| `py`, `pyw`, `pyi` | python |
| `js`, `mjs`, `cjs`, `jsx` | javascript |
| `ts`, `mts`, `cts`, `tsx` | typescript |
| `c`, `h` | c |
| `cpp`, `cc`, `cxx`, `hpp`, `hh` | cpp |
| `cs` | csharp |
| `rs` | rust |
| `go` | go |
| `swift` | swift |
| `rb`, `rbw` | ruby |
| `php`, `phtml` | php |
| `scala` | scala |
| `groovy` | groovy |
| `gradle` | gradle |
| `dart` | dart |
| `ex`, `exs` | elixir |
| `erl`, `hrl` | erlang |
| `hs`, `lhs` | haskell |
| `fs`, `fsi`, `fsx` | fsharp |
| `ml`, `mli` | ocaml |
| `clj`, `cljs`, `cljc` | clojure |
| `lua` | lua |
| `r` | r |
| `m`, `mm` | objectivec |
| `pl`, `pm` | perl |
| `sh`, `bash`, `zsh` | bash |
| `ps1`, `psm1`, `psd1` | powershell |
| `sql` | sql |
| `html`, `htm` | html |
| `xhtml`, `xml`, `svg`, `xsl` | xml |
| `css` | css |
| `scss` | scss |
| `less` | less |
| `json`, `jsonc` | json |
| `yaml`, `yml` | yaml |
| `toml` | toml |
| `md`, `markdown` | markdown |
| `dockerfile` | dockerfile |
| `makefile`, `mk` | makefile |
| `tex`, `latex` | latex |
| `diff`, `patch` | diff |
| `ini`, `cfg`, `conf` | ini |
| `properties` | properties |
| `vim` | vim |
| `cmake` | cmake |
| `proto` | protobuf |
| `glsl` | glsl |
| `bat`, `cmd` | dos |
| `asm`, `s` | x86asm |
| `graphql`, `gql` | graphql |
| `txt` | plaintext |
| `jl` | julia |
| `nim`, `nims` | nim |
| `vb` | vbnet |
| `vbs` | vbscript |
| `coffee` | coffeescript |
| `wat` | wasm |
| `haml` | haml |
| `hbs`, `handlebars` | handlebars |
| `styl` | stylus |
| `cr` | crystal |
| `elm` | elm |
| `hx` | haxe |
| `scm`, `ss` | scheme |
| `qml` | qml |
| `d` | d |
| `f`, `f90`, `f95`, `for` | fortran |
| `awk` | awk |
| `tcl`, `tk` | tcl |
| `lisp`, `lsp` | lisp |
| `applescript`, `scpt` | applescript |
| `nix` | nix |
| `nginx` | nginx |
| `pgsql` | pgsql |
| `pro` | prolog |
