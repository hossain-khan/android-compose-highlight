package dev.hossain.highlight.build

import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Build-time CSS parser that mirrors the behavior of the runtime
 * `dev.hossain.highlight.engine.ThemeParser` for the four bundled hljs themes.
 *
 * This is intentionally a duplicate of the runtime parser. The runtime version cannot be
 * reused here because:
 * - It depends on `androidx.compose.ui.graphics.Color` and `androidx.compose.ui.text.SpanStyle`,
 *   which are Android-runtime types unavailable in a Gradle build classpath.
 * - It returns a Compose-shaped map that we need to emit as Kotlin source rather than consume.
 *
 * A parity test (`GeneratedThemesParityTest`) compares the output of this build-time parser
 * against the runtime parser for the four bundled themes, so any future divergence fails CI.
 *
 * The parser only handles the subset of CSS hljs themes actually use:
 * - flat top-level `selectors { declarations }` rules,
 * - `/* … */` comments,
 * - `@media` / `@supports` / `@keyframes` blocks (skipped),
 * - hex / rgb() / rgba() / named colors,
 * - `font-weight` (bold/normal/numeric ≥ 600 → bold, < 600 → normal),
 * - `font-style: italic`.
 */
internal object CssThemeParser {
    /** Parses [cssText] into a deterministic, ordered list of (selector, style) entries. */
    fun parse(cssText: String): List<Pair<String, ParsedStyle>> {
        if (cssText.isBlank()) return emptyList()
        val rules = CssParser(cssText).parseStylesheet()
        if (rules.isEmpty()) return emptyList()

        // LinkedHashMap preserves CSS source order, so generated output is deterministic
        // and diff-friendly when the underlying CSS file changes.
        val result = LinkedHashMap<String, ParsedStyle>()
        for (rule in rules) {
            val style = parseDeclarations(rule.declarations) ?: continue
            for (selector in rule.selectors) {
                applyHljsSelector(result, selector, style)
            }
        }
        return result.entries.map { it.key to it.value }
    }

    /**
     * Computes the same 256-bit identity that the runtime built-in factories compute via
     * `contentDigest256("asset", assetPath)`: SHA-256 of `"asset" \0 <assetPath>`.
     *
     * Reproducing this here means generated themes carry the same identity the runtime
     * would have produced - `HighlightTheme.equals` / Compose recomposition keys behave
     * identically before and after this refactor.
     */
    fun assetContentIdentity(assetPath: String): LongArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("asset".toByteArray(Charsets.UTF_8))
        digest.update(byteArrayOf(0))
        digest.update(assetPath.toByteArray(Charsets.UTF_8))
        val buffer = ByteBuffer.wrap(digest.digest())
        return LongArray(SHA256_LONG_COUNT) { buffer.long }
    }

    private fun applyHljsSelector(
        result: LinkedHashMap<String, ParsedStyle>,
        selector: String,
        style: ParsedStyle,
    ) {
        val trimmed = selector.trim()
        if (trimmed.isEmpty()) return
        if ("::" in trimmed) return
        if (PSEUDO_CLASS_REGEX.containsMatchIn(trimmed)) return
        if (trimmed.any { it.isWhitespace() }) return
        if ('>' in trimmed || '+' in trimmed || '~' in trimmed) return
        if (!trimmed.startsWith('.')) return
        val match = HLJS_SELECTOR_REGEX.matchEntire(trimmed) ?: return
        val raw = match.value.trimStart('.')

        result[raw] = mergeStyles(result[raw], style)

        val primary = raw.substringBefore('.')
        if (primary != raw && !result.containsKey(primary)) {
            result[primary] = mergeStyles(result[primary], style)
        }
    }

    private fun mergeStyles(
        existing: ParsedStyle?,
        incoming: ParsedStyle,
    ): ParsedStyle {
        if (existing == null) return incoming
        return ParsedStyle(
            color = incoming.color ?: existing.color,
            fontWeight = incoming.fontWeight ?: existing.fontWeight,
            fontStyleItalic = incoming.fontStyleItalic ?: existing.fontStyleItalic,
            background = incoming.background ?: existing.background,
        )
    }

    private fun parseDeclarations(declarations: String): ParsedStyle? {
        var color: Long? = null
        var fontWeight: ParsedFontWeight? = null
        var fontStyleItalic: Boolean? = null
        var background: Long? = null

        PROP_PATTERN.findAll(declarations).forEach { match ->
            val prop = match.groupValues[1].trim()
            val value = match.groupValues[2].trim()
            when (prop) {
                "color" -> color = parseColor(value)
                "background", "background-color" -> background = parseColor(value)
                "font-weight" -> {
                    val numericWeight = value.toIntOrNull()
                    when {
                        value == "bold" || (numericWeight != null && numericWeight >= 600) ->
                            fontWeight = ParsedFontWeight.Bold

                        value == "normal" || (numericWeight != null && numericWeight < 600) ->
                            fontWeight = ParsedFontWeight.Normal
                    }
                }

                "font-style" -> if (value == "italic") fontStyleItalic = true
            }
        }

        if (color == null && fontWeight == null && fontStyleItalic == null && background == null) return null
        return ParsedStyle(
            color = color,
            fontWeight = fontWeight,
            fontStyleItalic = fontStyleItalic,
            background = background,
        )
    }

    /** Returns ARGB packed into a Long (0xAARRGGBB) or null. */
    private fun parseColor(value: String): Long? {
        val trimmed = value.trim()
        return when {
            trimmed.startsWith("#") -> parseHexColor(trimmed)
            trimmed.startsWith("rgb") -> parseRgbColor(trimmed)
            else -> NAMED_COLORS[trimmed.lowercase()]
        }
    }

    private fun parseHexColor(hex: String): Long? =
        try {
            val cleaned = hex.trimStart('#')
            when (cleaned.length) {
                3 -> {
                    val r = cleaned[0].toString().repeat(2).toInt(16)
                    val g = cleaned[1].toString().repeat(2).toInt(16)
                    val b = cleaned[2].toString().repeat(2).toInt(16)
                    argb(r, g, b, 255)
                }

                4 -> {
                    val r = cleaned[0].toString().repeat(2).toInt(16)
                    val g = cleaned[1].toString().repeat(2).toInt(16)
                    val b = cleaned[2].toString().repeat(2).toInt(16)
                    val a = cleaned[3].toString().repeat(2).toInt(16)
                    argb(r, g, b, a)
                }

                6 -> argb(
                    cleaned.substring(0, 2).toInt(16),
                    cleaned.substring(2, 4).toInt(16),
                    cleaned.substring(4, 6).toInt(16),
                    255,
                )

                8 -> argb(
                    cleaned.substring(2, 4).toInt(16),
                    cleaned.substring(4, 6).toInt(16),
                    cleaned.substring(6, 8).toInt(16),
                    cleaned.substring(0, 2).toInt(16),
                )

                else -> null
            }
        } catch (_: Exception) {
            null
        }

    private fun parseRgbColor(value: String): Long? {
        val inner = value.substringAfter("(").substringBefore(")").trim()
        return if (inner.contains(",")) parseCommaSeparatedRgb(inner) else parseSpaceSeparatedRgb(inner)
    }

    private fun parseCommaSeparatedRgb(inner: String): Long? {
        val parts = inner.split(",").map { it.trim() }
        return when (parts.size) {
            3 -> colorFromRgbStrings(parts[0], parts[1], parts[2], alpha = null)
            4 -> colorFromRgbStrings(parts[0], parts[1], parts[2], alpha = parts[3])
            else -> null
        }
    }

    private fun parseSpaceSeparatedRgb(inner: String): Long? {
        return if (inner.contains("/")) {
            val slashIdx = inner.indexOf("/")
            val colorPart = inner.substring(0, slashIdx).trim()
            val alphaPart = inner.substring(slashIdx + 1).trim()
            val parts = colorPart.split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (parts.size != 3) return null
            colorFromRgbStrings(parts[0], parts[1], parts[2], alpha = alphaPart)
        } else {
            val parts = inner.split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (parts.size != 3) return null
            colorFromRgbStrings(parts[0], parts[1], parts[2], alpha = null)
        }
    }

    private fun colorFromRgbStrings(
        r: String,
        g: String,
        b: String,
        alpha: String?,
    ): Long? {
        val red = parseRgbComponent(r) ?: return null
        val green = parseRgbComponent(g) ?: return null
        val blue = parseRgbComponent(b) ?: return null
        val a = if (alpha != null) parseAlphaComponent(alpha) ?: return null else 255
        return argb(red, green, blue, a)
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
        return if (f <= 1.0f) {
            Math.round(f * 255).coerceIn(0, 255)
        } else {
            value.toIntOrNull()?.coerceIn(0, 255)
        }
    }

    private fun argb(r: Int, g: Int, b: Int, a: Int): Long =
        ((a.toLong() and 0xFF) shl 24) or
            ((r.toLong() and 0xFF) shl 16) or
            ((g.toLong() and 0xFF) shl 8) or
            (b.toLong() and 0xFF)

    private val PROP_PATTERN = Regex("""([\w-]+)\s*:\s*([^;]+)""")
    private val HLJS_SELECTOR_REGEX = Regex("""\.hljs(?:-[\w-]+)?(?:\.[\w][\w-]*)*""")
    private val PSEUDO_CLASS_REGEX = Regex(""":(?!:)[a-zA-Z]""")
    private const val SHA256_LONG_COUNT = 4

    // Same 21 named colors the runtime parser supports.
    private val NAMED_COLORS: Map<String, Long> =
        mapOf(
            "black" to 0xFF000000L,
            "white" to 0xFFFFFFFFL,
            "red" to 0xFFFF0000L,
            "green" to 0xFF008000L,
            "blue" to 0xFF0000FFL,
            "yellow" to 0xFFFFFF00L,
            "orange" to 0xFFFFA500L,
            "purple" to 0xFF800080L,
            "gray" to 0xFF808080L,
            "grey" to 0xFF808080L,
            "silver" to 0xFFC0C0C0L,
            "navy" to 0xFF000080L,
            "teal" to 0xFF008080L,
            "maroon" to 0xFF800000L,
            "olive" to 0xFF808000L,
            "lime" to 0xFF00FF00L,
            "aqua" to 0xFF00FFFFL,
            "cyan" to 0xFF00FFFFL,
            "fuchsia" to 0xFFFF00FFL,
            "magenta" to 0xFFFF00FFL,
            "gold" to 0xFFFFD700L,
        )

    private data class CssRule(val selectors: List<String>, val declarations: String)

    private class CssParser(private val src: String) {
        private var pos: Int = 0
        private val len: Int = src.length

        fun parseStylesheet(): List<CssRule> {
            val rules = mutableListOf<CssRule>()
            skipTrivia()
            while (pos < len) {
                when {
                    startsWithCommentHere() -> skipComment()
                    src[pos] == '@' -> skipAtRule()
                    src[pos] == '}' -> break
                    else -> {
                        val rule = readRule() ?: break
                        rules.add(rule)
                    }
                }
                skipTrivia()
            }
            return rules
        }

        private fun readRule(): CssRule? {
            val selectorsRaw = readUntilOpenBrace() ?: return null
            if (pos >= len || src[pos] != '{') return null
            pos++
            val declarations = readDeclarations()
            if (pos < len && src[pos] == '}') pos++
            return CssRule(splitTopLevelByComma(selectorsRaw), declarations)
        }

        private fun readUntilOpenBrace(): String? {
            val sb = StringBuilder()
            while (pos < len) {
                if (startsWithCommentHere()) {
                    skipComment()
                    continue
                }
                val c = src[pos]
                if (c == '{') return sb.toString()
                if (c == '}') return null
                sb.append(c)
                pos++
            }
            return null
        }

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
                    skipBalancedBlock()
                    continue
                }
                sb.append(c)
                pos++
            }
            return sb.toString()
        }

        private fun skipAtRule() {
            pos++
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

        private fun skipBalancedBlock() {
            if (pos >= len || src[pos] != '{') return
            pos++
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

                    else -> pos++
                }
            }
        }

        private fun startsWithCommentHere(): Boolean = pos + 1 < len && src[pos] == '/' && src[pos + 1] == '*'

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

        private fun skipTrivia() {
            while (pos < len && src[pos].isWhitespace()) pos++
        }

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

                    else -> sb.append(c)
                }
            }
            if (sb.isNotEmpty()) out.add(sb.toString())
            return out
        }
    }
}

/** Runtime-equivalent font weight values that map to FontWeight.Bold / FontWeight.Normal. */
internal enum class ParsedFontWeight {
    Bold,
    Normal,
}

/**
 * Build-time mirror of `androidx.compose.ui.text.SpanStyle` - just the four properties hljs
 * theme CSS actually uses. Unset properties are null (analogous to runtime `Color.Unspecified`
 * or null FontWeight/FontStyle).
 *
 * @property color ARGB packed into a Long (0xAARRGGBB), or null if not set.
 * @property fontWeight Set when `font-weight` resolves to bold/normal; null otherwise.
 * @property fontStyleItalic True when `font-style: italic`; null otherwise.
 * @property background ARGB packed into a Long, or null if not set.
 */
internal data class ParsedStyle(
    val color: Long?,
    val fontWeight: ParsedFontWeight?,
    val fontStyleItalic: Boolean?,
    val background: Long?,
)
