package dev.hossain.highlight.engine

import dev.hossain.highlight.engine.internal.GeneratedLanguages
import java.util.Locale

/**
 * Language catalog and helpers for Highlight.js language identifiers.
 *
 * Provides synchronous access to all 190+ supported languages ([all]), a curated subset
 * of popular languages for quick-access pickers and chips ([primary]), support validation
 * ([isSupported]), alias normalization ([canonicalName]), and file extension mapping
 * ([fromExtension]).
 *
 * This is a convenience helper for discoverability and UI tooling. The `language` parameter used
 * by [dev.hossain.highlight.ui.SyntaxHighlightedCode] and [HighlightEngine.highlight] remains a plain
 * [String], so callers can still pass any Highlight.js language name directly.
 *
 * ## Usage
 *
 * ```kotlin
 * // Synchronous check if a language or alias is supported
 * val supported = HighlightLanguage.isSupported("kt") // true
 *
 * // Resolve an alias to its canonical Highlight.js grammar name
 * val canonical = HighlightLanguage.canonicalName("kt") // "kotlin"
 *
 * // Populate a language picker or filter chips
 * val pickers = HighlightLanguage.primary // ["kotlin", "java", "python", ...]
 *
 * // Map a file extension to a language identifier
 * val language = HighlightLanguage.fromExtension("kt") ?: "plaintext"
 *
 * SyntaxHighlightedCode(
 *     code = snippet,
 *     language = language,
 *     theme = rememberTomorrowLightTheme(),
 * )
 * ```
 */
public object HighlightLanguage {
    /**
     * Complete list of all supported language identifiers in alphabetical order.
     *
     * Contains all 190+ languages supported by the bundled Highlight.js distribution,
     * including `"html"` as a first-class language.
     */
    public val all: List<String> = GeneratedLanguages.ALL

    /**
     * Uppercase alias for [all] to match common constant naming conventions.
     */
    public val ALL: List<String> get() = all

    /**
     * Curated primary languages for quick-access pickers, filter chips, and editors.
     */
    public val primary: List<String> =
        listOf(
            "kotlin",
            "java",
            "python",
            "typescript",
            "javascript",
            "rust",
            "go",
            "swift",
            "c",
            "cpp",
            "csharp",
            "sql",
            "json",
            "yaml",
            "html",
            "css",
            "markdown",
            "bash",
            "xml",
            "dockerfile",
        )

    /**
     * Uppercase alias for [primary] to match common constant naming conventions.
     */
    public val PRIMARY: List<String> get() = primary

    private val canonicalSet: Set<String> by lazy { GeneratedLanguages.ALL.toSet() }

    /**
     * Checks whether the specified language identifier, alias, or file extension is supported.
     *
     * Returns `true` if [language] matches any canonical language in [all], any
     * registered Highlight.js alias (such as `"kt"`, `"py"`, `"js"`, `"ts"`, `"sh"`, `"html"`),
     * or any recognized file extension, case-insensitively.
     *
     * Example:
     * ```kotlin
     * HighlightLanguage.isSupported("kotlin")    // true
     * HighlightLanguage.isSupported("kt")        // true
     * HighlightLanguage.isSupported("HTML")      // true
     * HighlightLanguage.isSupported("unknown")   // false
     * ```
     *
     * @param language Language identifier, alias, or file extension to check.
     * @return `true` if supported, `false` otherwise.
     */
    public fun isSupported(language: String): Boolean = canonicalName(language) != null

    /**
     * Resolves a language identifier, alias, or file extension to its canonical
     * Highlight.js grammar name.
     *
     * Resolution order:
     * 1. If [nameOrAlias] matches a registered Highlight.js alias (such as `"kt"` -> `"kotlin"`,
     *    `"py"` -> `"python"`, `"js"` -> `"javascript"`, `"ts"` -> `"typescript"`,
     *    `"sh"` -> `"bash"`, `"html"` -> `"html"`), returns the mapped canonical identifier.
     * 2. If [nameOrAlias] matches a canonical language in [all] (case-insensitively), returns
     *    the canonical lowercase identifier.
     * 3. If [nameOrAlias] matches a known file extension in [fromExtension], returns that identifier.
     *
     * Returns `null` if the identifier or alias is not recognized.
     *
     * Example:
     * ```kotlin
     * HighlightLanguage.canonicalName("kt")         // "kotlin"
     * HighlightLanguage.canonicalName("KOTLIN")     // "kotlin"
     * HighlightLanguage.canonicalName("html")       // "html"
     * HighlightLanguage.canonicalName("sh")         // "bash"
     * HighlightLanguage.canonicalName("unknown")    // null
     * ```
     *
     * @param nameOrAlias Language identifier, alias, or file extension to resolve.
     * @return Canonical Highlight.js language identifier, or `null` if unrecognized.
     */
    public fun canonicalName(nameOrAlias: String): String? {
        val clean = nameOrAlias.trim().lowercase(Locale.ROOT)
        if (clean.isEmpty()) return null

        GeneratedLanguages.ALIASES[clean]?.let { return it }
        if (canonicalSet.contains(clean)) return clean
        return extensionMap[clean]
    }

    private val extensionMap: Map<String, String> =
        mapOf(
            "kt" to "kotlin",
            "kts" to "kotlin",
            "java" to "java",
            "py" to "python",
            "pyw" to "python",
            "pyi" to "python",
            "js" to "javascript",
            "mjs" to "javascript",
            "cjs" to "javascript",
            "jsx" to "javascript",
            "ts" to "typescript",
            "mts" to "typescript",
            "cts" to "typescript",
            "tsx" to "typescript",
            "c" to "c",
            "h" to "c",
            "cpp" to "cpp",
            "cc" to "cpp",
            "cxx" to "cpp",
            "hpp" to "cpp",
            "hh" to "cpp",
            "cs" to "csharp",
            "rs" to "rust",
            "go" to "go",
            "swift" to "swift",
            "rb" to "ruby",
            "rbw" to "ruby",
            "php" to "php",
            "phtml" to "php",
            "scala" to "scala",
            "groovy" to "groovy",
            "gradle" to "gradle",
            "dart" to "dart",
            "ex" to "elixir",
            "exs" to "elixir",
            "erl" to "erlang",
            "hrl" to "erlang",
            "hs" to "haskell",
            "lhs" to "haskell",
            "fs" to "fsharp",
            "fsi" to "fsharp",
            "fsx" to "fsharp",
            "ml" to "ocaml",
            "mli" to "ocaml",
            "clj" to "clojure",
            "cljs" to "clojure",
            "cljc" to "clojure",
            "lua" to "lua",
            "r" to "r",
            "m" to "objectivec",
            "mm" to "objectivec",
            "pl" to "perl",
            "pm" to "perl",
            "sh" to "bash",
            "bash" to "bash",
            "zsh" to "bash",
            "ps1" to "powershell",
            "psm1" to "powershell",
            "sql" to "sql",
            "html" to "html",
            "htm" to "html",
            "xhtml" to "xml",
            "xml" to "xml",
            "svg" to "xml",
            "xsl" to "xml",
            "css" to "css",
            "scss" to "scss",
            "less" to "less",
            "json" to "json",
            "jsonc" to "json",
            "yaml" to "yaml",
            "yml" to "yaml",
            "toml" to "toml",
            "md" to "markdown",
            "markdown" to "markdown",
            "dockerfile" to "dockerfile",
            "makefile" to "makefile",
            "mk" to "makefile",
            "tex" to "latex",
            "latex" to "latex",
            "diff" to "diff",
            "patch" to "diff",
            "ini" to "ini",
            "cfg" to "ini",
            "conf" to "ini",
            "properties" to "properties",
            "vim" to "vim",
            "cmake" to "cmake",
            "proto" to "protobuf",
            "glsl" to "glsl",
            "bat" to "dos",
            "cmd" to "dos",
            "psd1" to "powershell",
            "asm" to "x86asm",
            "s" to "x86asm",
            "graphql" to "graphql",
            "gql" to "graphql",
            "txt" to "plaintext",
            // ── Additional languages ─────────────────────────────────────────
            "jl" to "julia",
            "nim" to "nim",
            "nims" to "nim",
            "vb" to "vbnet",
            "vbs" to "vbscript",
            "coffee" to "coffeescript",
            "wat" to "wasm",
            "haml" to "haml",
            "hbs" to "handlebars",
            "handlebars" to "handlebars",
            "styl" to "stylus",
            "cr" to "crystal",
            "elm" to "elm",
            "hx" to "haxe",
            "scm" to "scheme",
            "ss" to "scheme",
            "qml" to "qml",
            "d" to "d",
            "f" to "fortran",
            "f90" to "fortran",
            "f95" to "fortran",
            "for" to "fortran",
            "awk" to "awk",
            "tcl" to "tcl",
            "tk" to "tcl",
            "lisp" to "lisp",
            "lsp" to "lisp",
            "applescript" to "applescript",
            "scpt" to "applescript",
            "nix" to "nix",
            "nginx" to "nginx",
            "pgsql" to "pgsql",
            "pro" to "prolog",
        )

    /**
     * Returns the Highlight.js language identifier for a file extension.
     *
     * @param extension File extension without a leading dot, such as `"kt"` or `"py"`.
     * @return Highlight.js language name such as `"kotlin"`, or `null` if the extension is not
     *   recognised.
     */
    fun fromExtension(extension: String): String? = extensionMap[extension.lowercase(Locale.ROOT)]
}

/**
 * Metadata returned by [HighlightEngine.getLanguage].
 *
 * Contains the human-readable display [name] plus any registered [aliases] for the
 * requested language.
 *
 * @property name Human-readable display name from Highlight.js (e.g. `"Kotlin"`, `"Python"`).
 *   This is NOT the language identifier - use [aliases] or the name you passed to
 *   [HighlightEngine.getLanguage] as the identifier for highlighting calls.
 * @property aliases Registered aliases for the language.
 */
data class HighlightLanguageInfo(
    val name: String,
    val aliases: List<String>,
)
