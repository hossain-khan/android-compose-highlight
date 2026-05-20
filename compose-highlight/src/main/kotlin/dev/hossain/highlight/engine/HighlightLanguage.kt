package dev.hossain.highlight.engine

/**
 * Maps file extensions to Highlight.js language identifiers.
 *
 * This is a convenience helper for discoverability only. The `language` parameter used by
 * [dev.hossain.highlight.ui.SyntaxHighlightedCode] and [HighlightEngine.highlight] remains a plain
 * [String], so callers can still pass any Highlight.js language name directly.
 *
 * ## Usage
 *
 * ```kotlin
 * val language = HighlightLanguage.fromExtension("kt") ?: "plaintext"
 *
 * SyntaxHighlightedCode(
 *     code = snippet,
 *     language = language,
 *     theme = rememberTomorrowTheme(),
 * )
 * ```
 */
public object HighlightLanguage {
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
        )

    /**
     * Returns the Highlight.js language identifier for a file extension.
     *
     * @param extension File extension without a leading dot, such as `"kt"` or `"py"`.
     * @return Highlight.js language name such as `"kotlin"`, or `null` if the extension is not
     *   recognised.
     */
    fun fromExtension(extension: String): String? = extensionMap[extension.lowercase()]
}
