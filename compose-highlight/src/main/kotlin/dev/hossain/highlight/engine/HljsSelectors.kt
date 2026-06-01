package dev.hossain.highlight.engine

/**
 * Known highlight.js CSS selector keys used throughout the library.
 *
 * highlight.js wraps each syntax token in a `<span class="hljs-*">` element.
 * These class names are mapped to [androidx.compose.ui.text.SpanStyle] entries
 * by [ThemeParser] when parsing a theme CSS file, and looked up by
 * [HtmlToAnnotatedString] when converting the HTML to an [androidx.compose.ui.text.AnnotatedString].
 *
 * The full set of keys available depends on the theme - different CSS themes define
 * different selectors. The constants below represent the **base** and **common**
 * selectors that appear in most themes.
 *
 * ### Base selector
 * - [BASE] (`"hljs"`) - The root container rule. Provides the default text color and
 *   background for the entire code block. Used by [HighlightTheme.backgroundColor]
 *   and [HighlightTheme.defaultTextColor] to derive theme-level defaults.
 *
 * ### Common token selectors
 * These appear in nearly all highlight.js themes:
 * - [KEYWORD] - Language keywords (`if`, `for`, `fun`, `def`, etc.)
 * - [STRING] - String literals
 * - [NUMBER] - Numeric literals
 * - [COMMENT] - Single-line and multi-line comments
 * - [LITERAL] - Boolean and null literals (`true`, `false`, `null`, `None`)
 * - [TYPE] - Type names and class declarations
 * - [TITLE] - Function and class titles
 * - [NAME] - Tag names (HTML/XML), variable names
 * - [BUILT_IN] - Built-in functions and types
 * - [ATTR] - HTML/XML attributes
 * - [SELECTOR_TAG] - CSS selector tags
 * - [STRONG] - Bold-emphasized tokens (maps to [androidx.compose.ui.text.font.FontWeight.Bold])
 * - [EMPHASIS] - Italic-emphasized tokens (maps to [androidx.compose.ui.text.font.FontStyle.Italic])
 * - [QUOTE] - Block quotes (often shares style with [COMMENT])
 * - [TAG] - Markup tags
 * - [OPERATOR] - Operators (`+`, `-`, `=`, etc.)
 * - [ADDITION] - Added lines in diff output
 *
 * ### Compound selectors
 * highlight.js sometimes outputs space-separated classes like `class="hljs-title function_"`.
 * These are resolved as dot-joined keys:
 * - [TITLE_FUNCTION] (`"hljs-title.function_"`) - Function title tokens
 *
 * ### Usage
 *
 * These constants are used internally by [ThemeParser], [HtmlToAnnotatedString], and
 * [HighlightTheme] to reference the base `"hljs"` selector. Token-specific selectors
 * (like [KEYWORD], [STRING]) are **not** hardcoded in production code - they are
 * discovered dynamically from the theme CSS at runtime. The constants exist here
 * for documentation and for test code that constructs inline color maps.
 *
 * @see ThemeParser
 * @see HtmlToAnnotatedString
 * @see HighlightTheme
 */
internal object HljsSelectors {
    /**
     * The base/root selector (`"hljs"`).
     *
     * Present in all themes. Provides the default text color and background for the
     * entire code block. Used to derive [HighlightTheme.backgroundColor] and
     * [HighlightTheme.defaultTextColor].
     */
    const val BASE = "hljs"

    /** Keyword tokens (`if`, `for`, `fun`, `def`, etc.). */
    const val KEYWORD = "hljs-keyword"

    /** String literals. */
    const val STRING = "hljs-string"

    /** Numeric literals. */
    const val NUMBER = "hljs-number"

    /** Single-line and multi-line comments. */
    const val COMMENT = "hljs-comment"

    /** Boolean and null literals (`true`, `false`, `null`, `None`). */
    const val LITERAL = "hljs-literal"

    /** Type names and class declarations. */
    const val TYPE = "hljs-type"

    /** Function and class titles. */
    const val TITLE = "hljs-title"

    /** Tag names (HTML/XML), variable names. */
    const val NAME = "hljs-name"

    /** Built-in functions and types. */
    const val BUILT_IN = "hljs-built_in"

    /** HTML/XML attributes. */
    const val ATTR = "hljs-attr"

    /** CSS selector tags. */
    const val SELECTOR_TAG = "hljs-selector-tag"

    /** Bold-emphasized tokens (typically maps to [androidx.compose.ui.text.font.FontWeight.Bold]). */
    const val STRONG = "hljs-strong"

    /** Italic-emphasized tokens (typically maps to [androidx.compose.ui.text.font.FontStyle.Italic]). */
    const val EMPHASIS = "hljs-emphasis"

    /** Block quotes (often shares style with [COMMENT]). */
    const val QUOTE = "hljs-quote"

    /** Markup tags. */
    const val TAG = "hljs-tag"

    /** Operators (`+`, `-`, `=`, etc.). */
    const val OPERATOR = "hljs-operator"

    /** Added lines in diff output. */
    const val ADDITION = "hljs-addition"

    /** Compound selector: function title (`"hljs-title.function_"`). */
    const val TITLE_FUNCTION = "hljs-title.function_"

    /** Compound selector: meta keyword (`"hljs-meta .hljs-keyword"` descendant, typically skipped). */
    const val META = "hljs-meta"

    /** Variable references (e.g. `$variable` in shell scripts). */
    const val VARIABLE = "hljs-variable"

    /** Function parameters in declarations. */
    const val PARAMS = "hljs-params"

    /** HTML/XML attributes. */
    const val ATTRIBUTE = "hljs-attribute"

    /** Section titles and headings. */
    const val SECTION = "hljs-section"

    /** Symbol tokens (e.g. Ruby symbols like `:foo`). */
    const val SYMBOL = "hljs-symbol"

    /** Bullet points in list markup. */
    const val BULLET = "hljs-bullet"

    /** Formula tokens in LaTeX/math markup. */
    const val FORMULA = "hljs-formula"

    /** Added/deleted line markers in diff output. */
    const val DELETION = "hljs-deletion"

    /** Regular expression literals. */
    const val REGEXP = "hljs-regexp"

    /** Link URLs in markup. */
    const val LINK = "hljs-link"
}
