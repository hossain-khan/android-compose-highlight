package dev.hossain.highlight.engine.internal

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Parses Highlight.js CSS theme files into a map of hljs class names -> [SpanStyle].
 *
 * ## Why this exists
 *
 * Highlight.js tokenizes source code into HTML where each token is wrapped in a
 * `<span class="hljs-keyword">` (or similar class). It assigns **class names only** - it does
 * not apply colors. Colors come from the companion CSS theme file, where rules like
 * `.hljs-keyword { color: #7928a1 }` define the appearance of each token type.
 *
 * Compose cannot consume CSS directly, so [ThemeParser] bridges the gap: it reads the CSS
 * file and produces a `Map<String, SpanStyle>` (e.g. `"hljs-keyword" -> SpanStyle(color=...)`)
 * that `HtmlToAnnotatedString` uses to apply the correct color to each token when building
 * the final [androidx.compose.ui.text.AnnotatedString].
 *
 * hljs theme CSS files follow a strict, predictable flat-rule format so a regex-based parser
 * is sufficient. At-rule blocks (e.g. `@media`, `@supports`) are stripped before parsing to
 * prevent inner rules from overwriting top-level color declarations.
 */
internal object ThemeParser {
    /**
     * Parses a CSS theme file from assets into a color map.
     * Results are not cached here - callers should use [lazy] to cache per theme.
     *
     * Silently returns an empty map on **I/O error or missing asset** (any [IOException] from
     * [android.content.res.AssetManager.open] or the read). Parser bugs - any exception thrown
     * by [parse] itself - propagate so they surface in tests and instrumented runs instead of
     * being silently swallowed.
     *
     * Use [parseAsset] if you need to distinguish between a missing file and an
     * empty/unparseable theme.
     */
    fun parse(
        context: Context,
        cssAssetPath: String,
    ): Map<String, SpanStyle> =
        try {
            val css =
                context.assets
                    .open(cssAssetPath)
                    .bufferedReader()
                    .use { it.readText() }
            parse(css)
        } catch (e: IOException) {
            // Missing or unreadable asset - documented silent path. Anything else
            // (e.g. a parser bug raising IllegalStateException) propagates so it cannot
            // masquerade as ThemeNotFound at the HighlightTheme.fromAsset layer.
            emptyMap()
        }

    /**
     * Parses a CSS theme file from assets into a color map.
     * Unlike [parse], this overload **throws** if the file cannot be opened, so callers
     * can distinguish a missing file from a successfully-parsed (but empty) theme.
     *
     * @throws java.io.IOException if the asset file cannot be opened.
     */
    fun parseAsset(
        context: Context,
        cssAssetPath: String,
    ): Map<String, SpanStyle> {
        val css =
            context.assets
                .open(cssAssetPath)
                .bufferedReader()
                .use { it.readText() }
        return parse(css)
    }

    /**
     * Parses CSS text directly into a color map.
     * Extracts [SpanStyle] for each `.hljs-*` selector block.
     *
     * Implementation: a small recursive-descent parser over a hand-written tokenizer.
     * Highlight.js theme CSS uses a strict, predictable subset of CSS - flat top-level rules,
     * occasional `@media` / `@supports` / `@keyframes` blocks, no nested rules - so a tiny
     * grammar handles every known theme without pulling in a full CSS engine.
     *
     * The parser:
     * 1. Skips comments and at-rule blocks (so `@media` rules can't clobber main rules).
     * 2. Walks each top-level `selectors { declarations }` rule.
     * 3. Filters individual selectors: only standalone `.hljs[-...]` chains are accepted;
     *    descendant combinators, pseudo-elements (`::selection`), and pseudo-classes (`:hover`)
     *    are skipped.
     * 4. Delegates declaration parsing to [parseDeclarations] (unchanged).
     * 5. Merges into the result map via [mergeSpanStyle] so split rules for the same selector
     *    accumulate (CSS cascade behaviour).
     */
    fun parse(cssText: String): Map<String, SpanStyle> {
        if (cssText.isBlank()) return emptyMap()
        val rules = CssParser(cssText).parseStylesheet()
        if (rules.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, SpanStyle>()
        for (rule in rules) {
            val spanStyle = parseDeclarations(rule.declarations) ?: continue
            for (selector in rule.selectors) {
                applyHljsSelector(result, selector, spanStyle)
            }
        }
        return result
    }

    /**
     * Applies [spanStyle] to [result] for [selector] if it is an acceptable standalone
     * `.hljs[-...]` (or compound `.hljs-x.y`) selector. Selectors with descendant combinators,
     * pseudo-elements, or pseudo-classes are silently ignored - they describe context-specific
     * styling and must not overwrite the base entries.
     */
    private fun applyHljsSelector(
        result: MutableMap<String, SpanStyle>,
        selector: String,
        spanStyle: SpanStyle,
    ) {
        val trimmed = selector.trim()
        if (trimmed.isEmpty()) return

        // Reject pseudo-elements (::selection) and pseudo-classes (:hover).
        // A bare `:` followed by an identifier is a pseudo-class; `::` is a pseudo-element.
        if ("::" in trimmed) return
        if (PSEUDO_CLASS_REGEX.containsMatchIn(trimmed)) return

        // Reject descendant / combinator selectors (whitespace, `>`, `+`, `~`).
        // E.g. `.hljs mark`, `.hljs > a`, `.hljs-meta .hljs-keyword` are all skipped.
        if (trimmed.any { it.isWhitespace() }) return
        if ('>' in trimmed || '+' in trimmed || '~' in trimmed) return

        // Must start with `.` (class selector) and the leading class must be `hljs` or `hljs-…`.
        if (!trimmed.startsWith('.')) return
        val match = HLJS_SELECTOR_REGEX.matchEntire(trimmed) ?: return
        val raw = match.value.trimStart('.')

        // Merge so that split rules for the same selector accumulate correctly.
        // Later rule values take precedence (CSS cascade).
        result[raw] = mergeSpanStyle(result[raw], spanStyle)

        // Compound key (e.g. "hljs-title.function_") also publishes its primary head
        // (e.g. "hljs-title") as a fallback if no explicit entry exists yet.
        val primary = raw.substringBefore('.')
        if (primary != raw && !result.containsKey(primary)) {
            result[primary] = mergeSpanStyle(result[primary], spanStyle)
        }
    }

    // Validates a full hljs selector chain: `.hljs`, `.hljs-keyword`, `.hljs-title.function_`, …
    // Stops if a non-leading class doesn't start with `.` or is itself `hljs` (which would mean
    // two separate hljs tokens - still ok in compound form, but we already rejected whitespace).
    private val HLJS_SELECTOR_REGEX = Regex("""\.hljs(?:-[\w-]+)?(?:\.[\w][\w-]*)*""")

    // Matches a single colon followed by a pseudo-class identifier, but NOT `::` (pseudo-element).
    private val PSEUDO_CLASS_REGEX = Regex(""":(?!:)[a-zA-Z]""")

    // Matches one CSS declaration: `<property>: <value>` up to the next `;` or end of block.
    // Used in [parseDeclarations] - kept module-level so a fresh Regex isn't compiled per rule
    // (a typical theme has dozens of rules and this would otherwise allocate one Regex each).
    private val PROP_PATTERN = Regex("""([\w-]+)\s*:\s*([^;]+)""")

    // Whitespace splitter for the modern `rgb(R G B / A)` and `rgb(R G B)` color forms. Hoisted
    // to module level so [parseSpaceSeparatedRgb] doesn't compile a fresh Regex on every color
    // value parsed.
    private val WHITESPACE_REGEX = Regex("\\s+")

    /** A single CSS rule: comma-separated selector list and the raw declarations block. */
    private data class CssRule(
        val selectors: List<String>,
        val declarations: String,
    )

    /**
     * Hand-written CSS tokenizer + parser for the subset used by highlight.js themes.
     *
     * Grammar (informal): a stylesheet is a sequence of comments, at-rules, or top-level rules.
     * A rule is `selector_list { declarations }`. A selector list is comma-separated selectors.
     * An at-rule is `@ident prelude { ...nested... }` or `@ident prelude;`.
     *
     * Rejected at the rule-application stage rather than here: pseudo-elements/classes,
     * descendant combinators. Declarations are returned verbatim so [parseDeclarations] can
     * keep its existing regex-based approach unchanged.
     */
    private class CssParser(
        private val src: String,
    ) {
        private var pos: Int = 0
        private val len: Int = src.length

        fun parseStylesheet(): List<CssRule> {
            val rules = mutableListOf<CssRule>()
            skipTrivia()
            while (pos < len) {
                when {
                    startsWithCommentHere() -> {
                        skipComment()
                    }

                    src[pos] == '@' -> {
                        skipAtRule()
                    }

                    src[pos] == '}' -> {
                        // Stray closing brace: stop. Defensive - should not happen in valid CSS.
                        break
                    }

                    else -> {
                        val rule = readRule() ?: break
                        rules.add(rule)
                    }
                }
                skipTrivia()
            }
            return rules
        }

        /** Reads a single top-level rule (selector list + declarations). */
        private fun readRule(): CssRule? {
            val selectorsRaw = readUntilOpenBrace() ?: return null
            // Consume the '{'
            if (pos >= len || src[pos] != '{') return null
            pos++
            val declarations = readDeclarations()
            // Consume the '}'
            if (pos < len && src[pos] == '}') pos++
            val selectors = splitTopLevelByComma(selectorsRaw)
            return CssRule(selectors, declarations)
        }

        /**
         * Reads characters until the next top-level `{`. Skips comments (which may contain `{`
         * or `}`). Returns the raw text (with comments removed) or `null` if EOF is reached
         * before a `{` is found.
         */
        private fun readUntilOpenBrace(): String? {
            val sb = StringBuilder()
            while (pos < len) {
                if (startsWithCommentHere()) {
                    skipComment()
                    continue
                }
                val c = src[pos]
                if (c == '{') return sb.toString()
                if (c == '}') return null // Unbalanced - abort
                sb.append(c)
                pos++
            }
            return null
        }

        /** Reads declarations until the matching top-level `}`. Strips comments. */
        private fun readDeclarations(): String {
            val sb = StringBuilder()
            while (pos < len) {
                if (startsWithCommentHere()) {
                    skipComment()
                    continue
                }
                val c = src[pos]
                if (c == '}') break
                if (c == '{') {
                    // Defensive - declarations should not contain `{`. If they do, skip the
                    // nested block to keep the outer parser in a sane state.
                    skipBalancedBlock()
                    continue
                }
                sb.append(c)
                pos++
            }
            return sb.toString()
        }

        /**
         * Skips a complete `@…{…}` at-rule (including any nested blocks). hljs themes use one
         * level of nesting at most (`@media { .hljs-x {…} }`); we still walk arbitrary nesting
         * defensively.
         */
        private fun skipAtRule() {
            // Skip the '@' and the prelude up to '{' or ';'.
            pos++ // '@'
            while (pos < len) {
                if (startsWithCommentHere()) {
                    skipComment()
                    continue
                }
                val c = src[pos]
                if (c == ';') {
                    pos++
                    return
                }
                if (c == '{') {
                    skipBalancedBlock()
                    return
                }
                pos++
            }
        }

        /** Assumes [pos] is on `{`. Consumes it and everything up to and including the matching `}`. */
        private fun skipBalancedBlock() {
            if (pos >= len || src[pos] != '{') return
            pos++ // opening '{'
            var depth = 1
            while (pos < len && depth > 0) {
                if (startsWithCommentHere()) {
                    skipComment()
                    continue
                }
                when (src[pos]) {
                    '{' -> {
                        depth++
                        pos++
                    }

                    '}' -> {
                        depth--
                        pos++
                    }

                    else -> {
                        pos++
                    }
                }
            }
        }

        private fun startsWithCommentHere(): Boolean = pos + 1 < len && src[pos] == '/' && src[pos + 1] == '*'

        /** Assumes [pos] points at the start of a CSS comment. Skips through the closing comment marker. */
        private fun skipComment() {
            pos += 2
            while (pos + 1 < len) {
                if (src[pos] == '*' && src[pos + 1] == '/') {
                    pos += 2
                    return
                }
                pos++
            }
            pos = len
        }

        /** Skips whitespace only (comments are handled at use-site so they're stripped from declarations). */
        private fun skipTrivia() {
            while (pos < len && src[pos].isWhitespace()) pos++
        }

        /**
         * Splits a selector list by top-level commas. CSS Selectors Level 3 doesn't permit
         * parentheses in non-functional selectors, but `:is(...)`, `:where(...)`, `:not(...)`
         * etc. would. hljs themes don't use these, so a parenthesis-aware split is enough
         * insurance against future themes adopting them.
         */
        private fun splitTopLevelByComma(raw: String): List<String> {
            val out = mutableListOf<String>()
            val sb = StringBuilder()
            var paren = 0
            for (c in raw) {
                when {
                    c == '(' -> {
                        paren++
                        sb.append(c)
                    }

                    c == ')' -> {
                        if (paren > 0) paren--
                        sb.append(c)
                    }

                    c == ',' && paren == 0 -> {
                        out.add(sb.toString())
                        sb.clear()
                    }

                    else -> {
                        sb.append(c)
                    }
                }
            }
            if (sb.isNotEmpty()) out.add(sb.toString())
            return out
        }
    }

    /**
     * Merges two [SpanStyle] instances, with [incoming] taking precedence over [existing].
     * Values that are [Color.Unspecified] or null in [incoming] fall back to [existing].
     * This allows multiple CSS rules targeting the same selector to accumulate correctly
     * instead of the last rule silently discarding properties set by earlier rules.
     */
    private fun mergeSpanStyle(
        existing: SpanStyle?,
        incoming: SpanStyle,
    ): SpanStyle {
        if (existing == null) return incoming
        return SpanStyle(
            color = if (incoming.color != Color.Unspecified) incoming.color else existing.color,
            fontWeight = incoming.fontWeight ?: existing.fontWeight,
            fontStyle = incoming.fontStyle ?: existing.fontStyle,
            background = if (incoming.background != Color.Unspecified) incoming.background else existing.background,
        )
    }

    private fun parseDeclarations(declarations: String): SpanStyle? {
        var color: Color? = null
        var fontWeight: FontWeight? = null
        var fontStyle: FontStyle? = null
        var background: Color? = null

        PROP_PATTERN.findAll(declarations).forEach { match ->
            val prop = match.groupValues[1].trim()
            val value = match.groupValues[2].trim()
            when (prop) {
                "color" -> {
                    color = parseColor(value)
                }

                "background", "background-color" -> {
                    background = parseColor(value)
                }

                "font-weight" -> {
                    val numericWeight = value.toIntOrNull()
                    when {
                        value == "bold" || (numericWeight != null && numericWeight >= 600) -> fontWeight = FontWeight.Bold
                        value == "normal" || (numericWeight != null && numericWeight < 600) -> fontWeight = FontWeight.Normal
                    }
                }

                "font-style" -> {
                    if (value == "italic") fontStyle = FontStyle.Italic
                }
            }
        }

        // Return null if nothing actionable was parsed
        if (color == null && fontWeight == null && fontStyle == null && background == null) return null

        return SpanStyle(
            color = color ?: Color.Unspecified,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            background = background ?: Color.Unspecified,
        )
    }

    // CSS named colors used in highlight.js themes (standard CSS Color Level 4 values).
    private val namedColors =
        mapOf(
            "black" to Color(0xFF000000),
            "white" to Color(0xFFFFFFFF),
            "red" to Color(0xFFFF0000),
            "green" to Color(0xFF008000),
            "blue" to Color(0xFF0000FF),
            "yellow" to Color(0xFFFFFF00),
            "orange" to Color(0xFFFFA500),
            "purple" to Color(0xFF800080),
            "gray" to Color(0xFF808080),
            "grey" to Color(0xFF808080),
            "silver" to Color(0xFFC0C0C0),
            "navy" to Color(0xFF000080),
            "teal" to Color(0xFF008080),
            "maroon" to Color(0xFF800000),
            "olive" to Color(0xFF808000),
            "lime" to Color(0xFF00FF00),
            "aqua" to Color(0xFF00FFFF),
            "cyan" to Color(0xFF00FFFF),
            "fuchsia" to Color(0xFFFF00FF),
            "magenta" to Color(0xFFFF00FF),
            "gold" to Color(0xFFFFD700),
        )

    private fun parseColor(value: String): Color? {
        val trimmed = value.trim()
        return when {
            trimmed.startsWith("#") -> parseHexColor(trimmed)
            trimmed.startsWith("rgb") -> parseRgbColor(trimmed)
            else -> namedColors[trimmed.lowercase()]
        }
    }

    private fun parseHexColor(hex: String): Color? =
        try {
            val cleaned = hex.trimStart('#')
            when (cleaned.length) {
                // 3-digit: #rgb - expand each digit to two (e.g. #f06 = #ff0066)
                3 -> {
                    val r = cleaned[0].toString().repeat(2).toInt(16)
                    val g = cleaned[1].toString().repeat(2).toInt(16)
                    val b = cleaned[2].toString().repeat(2).toInt(16)
                    Color(r, g, b)
                }

                // 4-digit: #rgba - expand each digit to two (e.g. #444a = #44 44 44 aa)
                4 -> {
                    val r = cleaned[0].toString().repeat(2).toInt(16)
                    val g = cleaned[1].toString().repeat(2).toInt(16)
                    val b = cleaned[2].toString().repeat(2).toInt(16)
                    val a = cleaned[3].toString().repeat(2).toInt(16)
                    Color(r, g, b, a)
                }

                6 -> {
                    Color(
                        red = cleaned.substring(0, 2).toInt(16),
                        green = cleaned.substring(2, 4).toInt(16),
                        blue = cleaned.substring(4, 6).toInt(16),
                    )
                }

                8 -> {
                    Color(
                        red = cleaned.substring(2, 4).toInt(16),
                        green = cleaned.substring(4, 6).toInt(16),
                        blue = cleaned.substring(6, 8).toInt(16),
                        alpha = cleaned.substring(0, 2).toInt(16),
                    )
                }

                else -> {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }

    private fun parseRgbColor(value: String): Color? {
        val inner = value.substringAfter("(").substringBefore(")").trim()
        return if (inner.contains(",")) {
            parseCommaSeparatedRgb(inner)
        } else {
            parseSpaceSeparatedRgb(inner)
        }
    }

    private fun parseCommaSeparatedRgb(inner: String): Color? {
        val parts = inner.split(",").map { it.trim() }
        return when (parts.size) {
            3 -> colorFromRgbStrings(parts[0], parts[1], parts[2], alpha = null)
            4 -> colorFromRgbStrings(parts[0], parts[1], parts[2], alpha = parts[3])
            else -> null
        }
    }

    private fun parseSpaceSeparatedRgb(inner: String): Color? {
        return if (inner.contains("/")) {
            val slashIdx = inner.indexOf("/")
            val colorPart = inner.substring(0, slashIdx).trim()
            val alphaPart = inner.substring(slashIdx + 1).trim()
            val parts = colorPart.split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
            if (parts.size != 3) return null
            colorFromRgbStrings(parts[0], parts[1], parts[2], alpha = alphaPart)
        } else {
            val parts = inner.split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
            if (parts.size != 3) return null
            colorFromRgbStrings(parts[0], parts[1], parts[2], alpha = null)
        }
    }

    private fun colorFromRgbStrings(
        r: String,
        g: String,
        b: String,
        alpha: String?,
    ): Color? {
        val red = parseRgbComponent(r) ?: return null
        val green = parseRgbComponent(g) ?: return null
        val blue = parseRgbComponent(b) ?: return null
        val a = if (alpha != null) parseAlphaComponent(alpha) ?: return null else 255
        return Color(red, green, blue, a)
    }

    private fun parseRgbComponent(value: String): Int? {
        if (value.endsWith("%")) {
            val pct = value.dropLast(1).toFloatOrNull() ?: return null
            return (pct / 100f * 255).toInt().coerceIn(0, 255)
        }
        return value.toIntOrNull()?.coerceIn(0, 255)
    }

    private fun parseAlphaComponent(value: String): Int? {
        if (value.endsWith("%")) {
            val pct = value.dropLast(1).toFloatOrNull() ?: return null
            if (!pct.isFinite()) return null
            return (pct / 100f * 255).toInt().coerceIn(0, 255)
        }
        val f = value.toFloatOrNull() ?: return null
        if (!f.isFinite()) return null
        // CSS alpha is 0.0-1.0; values > 1 are treated as 0-255 integer
        return if (f <= 1.0f) {
            (f * 255).roundToInt().coerceIn(0, 255)
        } else {
            value.toIntOrNull()?.coerceIn(0, 255)
        }
    }
}
