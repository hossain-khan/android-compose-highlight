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
 * different selectors. The constants below cover **all official hljs scopes** as
 * documented in the [CSS Classes Reference](https://highlightjs.readthedocs.io/en/latest/css-classes-reference.html).
 *
 * ### Base selector
 * - [BASE] (`"hljs"`) - The root container rule. Provides the default text color and
 *   background for the entire code block. Used by [HighlightTheme.backgroundColor]
 *   and [HighlightTheme.defaultTextColor] to derive theme-level defaults.
 *
 * ### General purpose selectors
 * These appear in nearly all highlight.js themes:
 * - [KEYWORD] - Language keywords (`if`, `for`, `fun`, `def`, etc.)
 * - [BUILT_IN] - Built-in or library objects (constants, classes, functions)
 * - [TYPE] - Data types (`string`, `int`, `array`, etc.)
 * - [LITERAL] - Special identifiers for built-in values (`true`, `false`, `null`, etc.)
 * - [NUMBER] - Numbers, including units and modifiers
 * - [OPERATOR] - Operators: `+`, `-`, `>>`, `|`, `==` (newer scope, not in all themes)
 * - [PUNCTUATION] - Auxiliary punctuation: parentheses, brackets, etc. (newer scope)
 * - [PROPERTY] - Object properties: `obj.prop1.prop2.value` (newer scope)
 * - [REGEXP] - Literal regular expressions
 * - [STRING] - Literal strings and characters
 * - [CHAR] - Base character scope (primary key emitted by ThemeParser)
 * - [CHAR_ESCAPE] - Character escape literals (official scope: `char.escape_`)
 * - [SUBST] - Parsed sections inside literal strings. **Important:** the hljs theme guide
 *   explicitly says "don't forget to style .subst" - it should usually reset to the
 *   default text color.
 * - [SYMBOL] - Symbolic constants, interned strings, goto labels
 * - [VARIABLE] - General variables
 * - [VARIABLE_LANGUAGE] - Variables with special meaning (`this`, `window`, `super`, `self`)
 * - [VARIABLE_CONSTANT] - Constant-value variables (e.g. `MAX_FILES`)
 * - [TITLE] - Name of a class or function
 * - [PARAMS] - Function arguments/parameters at the place of declaration
 * - [COMMENT] - Comments
 * - [DOCTAG] - Documentation markup within comments (e.g. `@params`)
 *
 * ### Title subscopes
 * highlight.js outputs space-separated classes like `class="hljs-title function_"` which
 * are resolved as dot-joined keys by [ThemeParser]:
 * - [TITLE_CLASS] (`"hljs-title.class_"`) - Name of a class, interface, trait, module
 * - [TITLE_CLASS_INHERITED] (`"hljs-title.class_.inherited__"`) - Inherited/extended class name
 * - [TITLE_FUNCTION] (`"hljs-title.function_"`) - Name of a function
 * - [TITLE_FUNCTION_INVOKE] (`"hljs-title.function.invoke_"`) - Function being invoked
 *
 * ### Meta selectors
 * - [META] - Flags, modifiers, annotations, preprocessor directives
 * - [META_KEYWORD] - Keywords inside a meta block (nested, hyphenated in CSS)
 * - [META_STRING] - Strings inside a meta block (nested, hyphenated in CSS)
 * - [META_PROMPT] - REPL or shell prompts
 *
 * ### Tags, attributes, configs
 * - [TAG] - XML/HTML tags
 * - [NAME] - Name of an XML tag, first word in an s-expression
 * - [ATTR] - Attribute names without language semantics (JSON keys, .ini settings)
 * - [ATTRIBUTE] - Attribute names followed by structured values (e.g. CSS properties)
 * - [SECTION] - Section headings in config files or text markup
 *
 * ### CSS selectors
 * - [SELECTOR_TAG] - Tag selectors
 * - [SELECTOR_ID] - `#id` selectors
 * - [SELECTOR_CLASS] - `.class` selectors
 * - [SELECTOR_ATTR] - `[attr]` selectors
 * - [SELECTOR_PSEUDO] - `:pseudo` selectors
 *
 * ### Text markup
 * - [BULLET] - List item bullets
 * - [CODE] - Code blocks
 * - [EMPHASIS] - Emphasis (typically maps to [androidx.compose.ui.text.font.FontStyle.Italic])
 * - [STRONG] - Strong emphasis (typically maps to [androidx.compose.ui.text.font.FontWeight.Bold])
 * - [FORMULA] - Mathematical formulas
 * - [LINK] - Hyperlinks
 * - [QUOTE] - Quotations or blockquotes
 *
 * ### Templates
 * - [TEMPLATE_TAG] - Tags of template languages
 * - [TEMPLATE_VARIABLE] - Variables in template languages
 *
 * ### Diff
 * - [ADDITION] - Added or changed lines
 * - [DELETION] - Deleted lines
 *
 * ### Other
 * - [ATRULE] - At-rule tokens (found in a small number of themes)
 *
 * ### Usage
 *
 * Use these constants when building color maps for [HighlightTheme.fromColorMap] to avoid
 * typos and get IDE autocomplete with full KDoc descriptions:
 *
 * ```kotlin
 * val colorMap = mapOf(
 *     HljsSelectors.BASE     to SpanStyle(color = Color(0xFF24292E), background = Color(0xFFFFFFFF)),
 *     HljsSelectors.KEYWORD  to SpanStyle(color = Color(0xFFD73A49), fontWeight = FontWeight.Bold),
 *     HljsSelectors.STRING   to SpanStyle(color = Color(0xFF032F62)),
 *     HljsSelectors.COMMENT  to SpanStyle(color = Color(0xFF6A737D), fontStyle = FontStyle.Italic),
 * )
 * val theme = HighlightTheme.fromColorMap(name = "my-theme", colorMap = colorMap)
 * ```
 *
 * Internally, [ThemeParser] and [HtmlToAnnotatedString] use these constants to reference
 * known selectors, while token-specific selectors are discovered dynamically from the
 * theme CSS at runtime.
 *
 * @see ThemeParser
 * @see HtmlToAnnotatedString
 * @see HighlightTheme
 * @see [hljs CSS Classes Reference](https://highlightjs.readthedocs.io/en/latest/css-classes-reference.html)
 * @see [hljs Theme Guide](https://highlightjs.readthedocs.io/en/latest/theme-guide.html)
 */
object HljsSelectors {
    // ----- Base -----

    /**
     * The base/root selector (`"hljs"`).
     *
     * Present in all themes. Provides the default text color and background for the
     * entire code block. Used to derive [HighlightTheme.backgroundColor] and
     * [HighlightTheme.defaultTextColor].
     */
    const val BASE = "hljs"

    // ----- General purpose -----

    /** Keyword tokens (`if`, `for`, `fun`, `def`, etc.). */
    const val KEYWORD = "hljs-keyword"

    /** Built-in or library objects (constants, classes, functions). */
    const val BUILT_IN = "hljs-built_in"

    /** Data types (`string`, `int`, `array`, etc.). */
    const val TYPE = "hljs-type"

    /** Special identifiers for built-in values (`true`, `false`, `null`, `None`). */
    const val LITERAL = "hljs-literal"

    /** Numbers, including units and modifiers. */
    const val NUMBER = "hljs-number"

    /**
     * Operators: `+`, `-`, `>>`, `|`, `==`.
     * Newer scope - not present in all themes.
     */
    const val OPERATOR = "hljs-operator"

    /**
     * Auxiliary punctuation: parentheses, brackets, etc.
     * Newer scope - not present in all themes.
     */
    const val PUNCTUATION = "hljs-punctuation"

    /**
     * Object properties: `obj.prop1.prop2.value`.
     * Newer scope - not present in all themes.
     */
    const val PROPERTY = "hljs-property"

    /** Literal regular expressions. */
    const val REGEXP = "hljs-regexp"

    /** Literal strings and characters. */
    const val STRING = "hljs-string"

    /**
     * Character literals - base scope.
     * ThemeParser emits this as the primary key when parsing `.hljs-char.escape_`
     * (via `substringBefore('.')`), so both [CHAR] and [CHAR_ESCAPE] will resolve
     * to the same style from real theme CSS.
     */
    const val CHAR = "hljs-char"

    /**
     * Character escape literals (e.g. `\n`, `\t`).
     * hljs emits `class="hljs-char escape_"` which resolves to the compound key
     * `hljs-char.escape_`. All real theme CSS files use `.hljs-char.escape_`.
     */
    const val CHAR_ESCAPE = "hljs-char.escape_"

    /**
     * Parsed sections inside literal strings.
     * The hljs theme guide explicitly says "don't forget to style .subst" - it should
     * usually reset to the default text color (e.g. `.hljs, .hljs-subst { color: black }`).
     */
    const val SUBST = "hljs-subst"

    /** Symbolic constants, interned strings, goto labels. */
    const val SYMBOL = "hljs-symbol"

    /** General variables (e.g. `$variable` in shell scripts). */
    const val VARIABLE = "hljs-variable"

    /**
     * Variables with special meaning in a language:
     * `this`, `window`, `super`, `self`, etc.
     */
    const val VARIABLE_LANGUAGE = "hljs-variable.language_"

    /** Constant-value variables (e.g. `MAX_FILES`). */
    const val VARIABLE_CONSTANT = "hljs-variable.constant_"

    /** Name of a class or function. */
    const val TITLE = "hljs-title"

    /** Function arguments/parameters at the place of declaration. */
    const val PARAMS = "hljs-params"

    /** Single-line and multi-line comments. */
    const val COMMENT = "hljs-comment"

    /** Documentation markup within comments (e.g. `@params`). */
    const val DOCTAG = "hljs-doctag"

    // ----- Title subscopes -----

    /** Name of a class, interface, trait, or module. */
    const val TITLE_CLASS = "hljs-title.class_"

    /**
     * Name of a class being inherited from, extended, etc.
     * hljs emits `class="hljs-title class_ inherited__"` which resolves to the compound key
     * `hljs-title.class_.inherited__` (note: double underscore on `inherited__`).
     */
    const val TITLE_CLASS_INHERITED = "hljs-title.class_.inherited__"

    /** Name of a function. */
    const val TITLE_FUNCTION = "hljs-title.function_"

    /** Name of a function when being invoked. */
    const val TITLE_FUNCTION_INVOKE = "hljs-title.function.invoke_"

    // ----- Meta -----

    /** Flags, modifiers, annotations, preprocessor directives. */
    const val META = "hljs-meta"

    /** Keywords inside a meta block (nested, hyphenated in CSS). */
    const val META_KEYWORD = "hljs-meta-keyword"

    /** Strings inside a meta block (nested, hyphenated in CSS). */
    const val META_STRING = "hljs-meta-string"

    /** REPL or shell prompts. */
    const val META_PROMPT = "hljs-meta.prompt"

    // ----- Tags, attributes, configs -----

    /** XML/HTML tags. */
    const val TAG = "hljs-tag"

    /** Name of an XML tag, the first word in an s-expression. */
    const val NAME = "hljs-name"

    /**
     * Attribute names without language-defined semantics
     * (JSON keys, .ini settings), also sub-attributes within another highlighted object.
     */
    const val ATTR = "hljs-attr"

    /** Attribute names followed by structured values (e.g. CSS properties). */
    const val ATTRIBUTE = "hljs-attribute"

    /** Section headings in config files or text markup. */
    const val SECTION = "hljs-section"

    // ----- CSS selectors -----

    /** Tag selectors (e.g. `div`, `span`). */
    const val SELECTOR_TAG = "hljs-selector-tag"

    /** `#id` selectors. */
    const val SELECTOR_ID = "hljs-selector-id"

    /** `.class` selectors. */
    const val SELECTOR_CLASS = "hljs-selector-class"

    /** `[attr]` selectors. */
    const val SELECTOR_ATTR = "hljs-selector-attr"

    /** `:pseudo` selectors. */
    const val SELECTOR_PSEUDO = "hljs-selector-pseudo"

    // ----- Text markup -----

    /** List item bullets. */
    const val BULLET = "hljs-bullet"

    /** Code blocks. */
    const val CODE = "hljs-code"

    /** Emphasis (typically maps to [androidx.compose.ui.text.font.FontStyle.Italic]). */
    const val EMPHASIS = "hljs-emphasis"

    /** Strong emphasis (typically maps to [androidx.compose.ui.text.font.FontWeight.Bold]). */
    const val STRONG = "hljs-strong"

    /** Mathematical formulas. */
    const val FORMULA = "hljs-formula"

    /** Hyperlinks. */
    const val LINK = "hljs-link"

    /** Quotations or blockquotes. */
    const val QUOTE = "hljs-quote"

    // ----- Templates -----

    /** Tags of template languages. */
    const val TEMPLATE_TAG = "hljs-template-tag"

    /** Variables in template languages. */
    const val TEMPLATE_VARIABLE = "hljs-template-variable"

    // ----- Diff -----

    /** Added or changed lines. */
    const val ADDITION = "hljs-addition"

    /** Deleted lines. */
    const val DELETION = "hljs-deletion"

    // ----- Other -----

    /**
     * At-rule tokens.
     * Found in a small number of themes.
     */
    const val ATRULE = "hljs-atrule"
}
