package dev.hossain.highlight.engine

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
object ThemeParser {
    /**
     * Parses a CSS theme file from assets into a color map.
     * Results are not cached here — callers should use [lazy] to cache per theme.
     *
     * Silently returns an empty map on any error. Use [parseAsset] if you need to
     * distinguish between a missing file and an empty/unparseable theme.
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
                    .readText()
            parse(css)
        } catch (e: Exception) {
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
                .readText()
        return parse(css)
    }

    /**
     * Parses CSS text directly into a color map.
     * Extracts [SpanStyle] for each `.hljs-*` selector block.
     */
    fun parse(cssText: String): Map<String, SpanStyle> {
        if (cssText.isBlank()) return emptyMap()

        // Strip CSS comments first so that @ signs inside comment blocks
        // (e.g. author emails like @ericwbailey) are not mistaken for at-rules.
        val withoutComments = cssText.replace(Regex("""/\*[^*]*\*+(?:[^/*][^*]*\*+)*/"""), "")

        // Strip @at-rules and their entire content blocks (e.g. @media, @supports, @keyframes).
        // Without this, inner rules like `.hljs-keyword { font-weight:700 }` inside a
        // @media block would overwrite the real color entry parsed from the main stylesheet.
        // The pattern handles one level of nested braces (sufficient for all known hljs themes).
        val withoutAtRules =
            withoutComments.replace(
                Regex("""@[a-zA-Z][^{]*\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}"""),
                "",
            )

        if (withoutAtRules.isBlank()) return emptyMap()

        val result = mutableMapOf<String, SpanStyle>()
        // Match each CSS rule block: selectors { declarations }
        val rulePattern = Regex("""([^{}]+)\{([^{}]*)\}""")

        // Matches a full hljs class selector including multi-hyphen names and dot-joined compound classes.
        // Examples: .hljs, .hljs-keyword, .hljs-template-tag, .hljs-meta-keyword, .hljs-title.function_
        // Stops at whitespace (descendant combinator) and at a second .hljs (which would be a new selector token).
        val selectorPattern = Regex("""\.hljs[-\w]*(?:\.(?!hljs)[\w][-\w.]*)*""")

        rulePattern.findAll(withoutAtRules).forEach { matchResult ->
            val selectorsPart = matchResult.groupValues[1]
            val declarations = matchResult.groupValues[2]

            val spanStyle = parseDeclarations(declarations) ?: return@forEach

            // Split into individual selectors (comma-separated) and process each independently.
            // This prevents descendant selectors like `.hljs-meta .hljs-keyword` from
            // overwriting the standalone `.hljs-keyword` entry with a context-specific style.
            selectorsPart.split(",").forEach { individualSelector ->
                val trimmed = individualSelector.trim()

                // Skip pseudo-element and pseudo-class selectors (::selection, :hover, etc.).
                // Without this check, a rule like `.hljs::selection { background: #aabbcc }`
                // strips the pseudo-element and incorrectly overwrites `result["hljs"]`
                // with the selection-highlight color instead of the real background color.
                if (trimmed.contains("::") || trimmed.contains(Regex(""":(?!:)[a-z]"""))) return@forEach

                // Skip descendant/combinator selectors - any selector containing whitespace
                // is context-specific (e.g. `.hljs mark`, `.hljs a`, `.hljs-meta .hljs-keyword`)
                // and must not overwrite a standalone class entry.
                if (trimmed.contains(" ")) return@forEach

                val matches = selectorPattern.findAll(trimmed).toList()

                // Skip context-specific descendant selectors entirely.
                // If a selector has two separate .hljs-* tokens (separated by whitespace),
                // it's a descendant rule that only applies in a specific nested context.
                if (matches.size >= 2) return@forEach

                matches.forEach { selectorMatch ->
                    val raw = selectorMatch.value.trimStart('.')
                    // Merge with any existing entry so that split rules for the same selector
                    // accumulate correctly (e.g. a theme may set `background` in one rule and
                    // `color` in a separate rule for `.hljs`; both values must be retained).
                    // Later-rule values take precedence over earlier ones (CSS cascade).
                    result[raw] = mergeSpanStyle(result[raw], spanStyle)
                    // Also store under the primary class for compound selectors
                    // e.g. "hljs-title.function_" → also store "hljs-title" as fallback
                    val primary = raw.substringBefore('.')
                    if (primary != raw && !result.containsKey(primary)) {
                        result[primary] = mergeSpanStyle(result[primary], spanStyle)
                    }
                }
            }
        }

        return result
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

        val propPattern = Regex("""([\w-]+)\s*:\s*([^;]+)""")
        propPattern.findAll(declarations).forEach { match ->
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
            return (pct / 100f * 255).toInt().coerceIn(0, 255)
        }
        val f = value.toFloatOrNull() ?: return null
        // CSS alpha is 0.0-1.0; values > 1 are treated as 0-255 integer
        return if (f <= 1.0f) (f * 255).roundToInt().coerceIn(0, 255) else f.toInt().coerceIn(0, 255)
    }
}
