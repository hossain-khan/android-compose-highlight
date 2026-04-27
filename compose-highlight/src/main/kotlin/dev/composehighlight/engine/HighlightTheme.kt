package dev.composehighlight.engine

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle

/**
 * Represents a syntax highlighting theme backed by a Highlight.js CSS file.
 *
 * The color map is lazily initialized and cached — CSS parsing happens at most once per theme.
 * Background and text colors are derived from the already-parsed [colorMap] (the `.hljs` rule),
 * avoiding double-parsing of the CSS file.
 */
class HighlightTheme private constructor(
    val name: String,
    private val colorMapProvider: () -> Map<String, SpanStyle>,
) {
    /** Lazily-parsed map of hljs class names → [SpanStyle]. Cached forever. */
    val colorMap: Map<String, SpanStyle> by lazy { colorMapProvider() }

    /** Background color from the `.hljs` CSS rule. Unspecified if not present in theme. */
    val backgroundColor: Color by lazy {
        colorMap["hljs"]?.background?.takeIf { it != Color.Unspecified } ?: Color.Unspecified
    }

    /** Default text color from the `.hljs` CSS rule. Unspecified if not present in theme. */
    val defaultTextColor: Color by lazy {
        colorMap["hljs"]?.color?.takeIf { it != Color.Unspecified } ?: Color.Unspecified
    }

    companion object {

        /** Built-in Base16 Tomorrow light theme. */
        fun tomorrow(context: Context): HighlightTheme = HighlightTheme(
            name = "tomorrow",
            colorMapProvider = { ThemeParser.parse(context, "compose-highlight/themes/tomorrow.css") },
        )

        /** Built-in Base16 Tomorrow Night dark theme. */
        fun tomorrowNight(context: Context): HighlightTheme = HighlightTheme(
            name = "tomorrow-night",
            colorMapProvider = { ThemeParser.parse(context, "compose-highlight/themes/tomorrow-night.css") },
        )

        /** Built-in Atom One Dark theme. */
        fun atomOneDark(context: Context): HighlightTheme = HighlightTheme(
            name = "atom-one-dark",
            colorMapProvider = { ThemeParser.parse(context, "compose-highlight/themes/atom-one-dark.css") },
        )

        /** Built-in Atom One Light theme. */
        fun atomOneLight(context: Context): HighlightTheme = HighlightTheme(
            name = "atom-one-light",
            colorMapProvider = { ThemeParser.parse(context, "compose-highlight/themes/atom-one-light.css") },
        )

        /** Custom theme loaded from an asset CSS file path. */
        fun fromAsset(context: Context, assetPath: String, name: String): HighlightTheme = HighlightTheme(
            name = name,
            colorMapProvider = {
                try {
                    ThemeParser.parse(context, assetPath)
                } catch (e: Exception) {
                    throw HighlightException.ThemeNotFound(assetPath)
                }
            },
        )

        /** Custom theme from raw CSS text. */
        fun fromCss(cssText: String, name: String): HighlightTheme = HighlightTheme(
            name = name,
            colorMapProvider = { ThemeParser.parse(cssText) },
        )
    }
}
