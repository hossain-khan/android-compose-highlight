package dev.composehighlight.engine

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

private const val TAG = "ComposeHighlight"

/**
 * Parses Highlight.js CSS theme files into a map of hljs class names → [SpanStyle].
 *
 * The parsing logic is modeled on Perplexity's production CSS parser (obfuscated class `gn/d.c()`).
 * hljs theme CSS files follow a strict, predictable format so a regex-based parser is sufficient.
 */
object ThemeParser {

    /**
     * Parses a CSS theme file from assets into a color map.
     * Results are not cached here — callers should use [lazy] to cache per theme.
     */
    fun parse(context: Context, cssAssetPath: String): Map<String, SpanStyle> {
        return try {
            val css = context.assets.open(cssAssetPath).bufferedReader().readText()
            Log.d(TAG, "ThemeParser: loaded CSS from '$cssAssetPath' (${css.length} chars)")
            parse(css)
        } catch (e: Exception) {
            Log.e(TAG, "ThemeParser: FAILED to open asset '$cssAssetPath': ${e.message}", e)
            emptyMap()
        }
    }

    /**
     * Parses CSS text directly into a color map.
     * Extracts [SpanStyle] for each `.hljs-*` selector block.
     */
    fun parse(cssText: String): Map<String, SpanStyle> {
        if (cssText.isBlank()) return emptyMap()

        val result = mutableMapOf<String, SpanStyle>()
        // Match each CSS rule block: selectors { declarations }
        val rulePattern = Regex("""([^{}]+)\{([^{}]*)\}""")

        // Matches a full hljs class selector including multi-hyphen names and dot-joined compound classes.
        // Examples: .hljs, .hljs-keyword, .hljs-template-tag, .hljs-meta-keyword, .hljs-title.function_
        // Stops at whitespace (descendant combinator) and at a second .hljs (which would be a new selector token).
        val selectorPattern = Regex("""\.hljs[-\w]*(?:\.(?!hljs)[\w][-\w.]*)*""")

        rulePattern.findAll(cssText).forEach { matchResult ->
            val selectorsPart = matchResult.groupValues[1]
            val declarations = matchResult.groupValues[2]

            val spanStyle = parseDeclarations(declarations) ?: return@forEach

            // Split into individual selectors (comma-separated) and process each independently.
            // This prevents descendant selectors like `.hljs-meta .hljs-keyword` from
            // overwriting the standalone `.hljs-keyword` entry with a context-specific style.
            selectorsPart.split(",").forEach { individualSelector ->
                val trimmed = individualSelector.trim()
                val matches = selectorPattern.findAll(trimmed).toList()

                // Skip context-specific descendant selectors entirely.
                // If a selector has two separate .hljs-* tokens (separated by whitespace),
                // it's a descendant rule that only applies in a specific nested context.
                if (matches.size >= 2) return@forEach

                matches.forEach { selectorMatch ->
                    val raw = selectorMatch.value.trimStart('.')
                    result[raw] = spanStyle
                    // Also store under the primary class for compound selectors
                    // e.g. "hljs-title.function_" → also store "hljs-title" as fallback
                    val primary = raw.substringBefore('.')
                    if (primary != raw && !result.containsKey(primary)) {
                        result[primary] = spanStyle
                    }
                }
            }
        }

        return result
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
                "color" -> color = parseColor(value)
                "background", "background-color" -> background = parseColor(value)
                "font-weight" -> if (value == "bold" || value == "700") fontWeight = FontWeight.Bold
                "font-style" -> if (value == "italic") fontStyle = FontStyle.Italic
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

    private fun parseColor(value: String): Color? {
        val trimmed = value.trim()
        return when {
            trimmed.startsWith("#") -> parseHexColor(trimmed)
            trimmed.startsWith("rgb") -> parseRgbColor(trimmed)
            else -> null
        }
    }

    private fun parseHexColor(hex: String): Color? {
        return try {
            val cleaned = hex.trimStart('#')
            when (cleaned.length) {
                3 -> {
                    val r = cleaned[0].toString().repeat(2).toInt(16)
                    val g = cleaned[1].toString().repeat(2).toInt(16)
                    val b = cleaned[2].toString().repeat(2).toInt(16)
                    Color(r, g, b)
                }
                6 -> Color(
                    red = cleaned.substring(0, 2).toInt(16),
                    green = cleaned.substring(2, 4).toInt(16),
                    blue = cleaned.substring(4, 6).toInt(16),
                )
                8 -> Color(
                    red = cleaned.substring(2, 4).toInt(16),
                    green = cleaned.substring(4, 6).toInt(16),
                    blue = cleaned.substring(6, 8).toInt(16),
                    alpha = cleaned.substring(0, 2).toInt(16),
                )
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRgbColor(value: String): Color? {
        return try {
            val nums = Regex("""\d+""").findAll(value).map { it.value.toInt() }.toList()
            when (nums.size) {
                3 -> Color(nums[0], nums[1], nums[2])
                4 -> Color(nums[0], nums[1], nums[2], nums[3])
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
