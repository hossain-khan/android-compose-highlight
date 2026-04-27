package dev.hossain.highlight.engine

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle

/**
 * Represents a syntax highlighting theme backed by a Highlight.js CSS file.
 *
 * The color map is lazily initialized and cached — CSS parsing happens at most once per theme.
 * Background and text colors are derived from the already-parsed [colorMap] (the `.hljs` rule),
 * avoiding double-parsing of the CSS file.
 *
 * ## Built-in themes
 *
 * ```kotlin
 * // Light themes
 * HighlightTheme.tomorrow(context)
 * HighlightTheme.atomOneLight(context)
 *
 * // Dark themes
 * HighlightTheme.tomorrowNight(context)
 * HighlightTheme.atomOneDark(context)
 * ```
 *
 * ## Custom theme from an asset file
 *
 * Any Highlight.js CSS theme can be bundled in your app's `assets/` folder and loaded at runtime.
 * This is the recommended way to ship additional themes with your app.
 *
 * ```kotlin
 * // Place your .css file in src/main/assets/themes/github.css
 * val theme = HighlightTheme.fromAsset(
 *     context   = context,
 *     assetPath = "themes/github.css",
 *     name      = "github",
 * )
 * ```
 *
 * Note: `fromAsset()` is lazy — CSS parsing (and any `ThemeNotFound` error) occurs when the
 * theme is first applied, not at factory-call time.
 *
 * ## Custom theme from raw CSS
 *
 * ```kotlin
 * val theme = HighlightTheme.fromCss(
 *     cssText = rawCssString,
 *     name    = "my-inline-theme",
 * )
 * ```
 *
 * ## Custom theme from a precomputed color map
 *
 * For maximum control — e.g. deriving colors from Material 3 dynamic color or any other
 * source — you can supply the color map directly:
 *
 * ```kotlin
 * val colorMap: Map<String, SpanStyle> = mapOf(
 *     "hljs"          to SpanStyle(color = Color(0xFF24292E), background = Color(0xFFFFFFFF)),
 *     "hljs-keyword"  to SpanStyle(color = Color(0xFFD73A49), fontWeight = FontWeight.Bold),
 *     "hljs-string"   to SpanStyle(color = Color(0xFF032F62)),
 *     // ... add more token types as needed
 * )
 * val theme = HighlightTheme.fromColorMap(
 *     name            = "my-dynamic-theme",
 *     colorMap        = colorMap,
 *     backgroundColor = Color(0xFFFFFFFF),
 *     defaultTextColor = Color(0xFF24292E),
 * )
 * ```
 *
 * Any valid Highlight.js CSS theme works with `fromAsset` / `fromCss`. Community themes are at
 * [highlightjs/highlight.js/src/styles](https://github.com/highlightjs/highlight.js/tree/main/src/styles).
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
        fun tomorrow(context: Context): HighlightTheme =
            HighlightTheme(
                name = "tomorrow",
                colorMapProvider = { ThemeParser.parse(context, "compose-highlight/themes/tomorrow.css") },
            )

        /** Built-in Base16 Tomorrow Night dark theme. */
        fun tomorrowNight(context: Context): HighlightTheme =
            HighlightTheme(
                name = "tomorrow-night",
                colorMapProvider = { ThemeParser.parse(context, "compose-highlight/themes/tomorrow-night.css") },
            )

        /** Built-in Atom One Dark theme. */
        fun atomOneDark(context: Context): HighlightTheme =
            HighlightTheme(
                name = "atom-one-dark",
                colorMapProvider = { ThemeParser.parse(context, "compose-highlight/themes/atom-one-dark.css") },
            )

        /** Built-in Atom One Light theme. */
        fun atomOneLight(context: Context): HighlightTheme =
            HighlightTheme(
                name = "atom-one-light",
                colorMapProvider = { ThemeParser.parse(context, "compose-highlight/themes/atom-one-light.css") },
            )

        /** Custom theme loaded from an asset CSS file path. Throws [HighlightException.ThemeNotFound] if the file is missing or unreadable. */
        fun fromAsset(
            context: Context,
            assetPath: String,
            name: String,
        ): HighlightTheme =
            HighlightTheme(
                name = name,
                colorMapProvider = {
                    val map = ThemeParser.parseAsset(context, assetPath)
                    if (map.isEmpty()) throw HighlightException.ThemeNotFound(assetPath)
                    map
                },
            )

        /** Custom theme from raw CSS text. */
        fun fromCss(
            cssText: String,
            name: String,
        ): HighlightTheme =
            HighlightTheme(
                name = name,
                colorMapProvider = { ThemeParser.parse(cssText) },
            )

        /**
         * Custom theme from a precomputed color map.
         *
         * Use this when deriving colors from Material 3 dynamic color, app branding, or any
         * non-CSS source. The [colorMap] keys are Highlight.js class names without the leading
         * dot (e.g. `"hljs-keyword"`, `"hljs-string"`, `"hljs"`). The `"hljs"` entry is used
         * to derive [HighlightTheme.backgroundColor] and [HighlightTheme.defaultTextColor]; you
         * can also override those explicitly via [backgroundColor] and [defaultTextColor].
         *
         * @param name Display name for the theme.
         * @param colorMap Map of hljs class name → [SpanStyle].
         * @param backgroundColor Optional explicit background color. If null, derived from `colorMap["hljs"]`.
         * @param defaultTextColor Optional explicit default text color. If null, derived from `colorMap["hljs"]`.
         */
        fun fromColorMap(
            name: String,
            colorMap: Map<String, SpanStyle>,
            backgroundColor: Color? = null,
            defaultTextColor: Color? = null,
        ): HighlightTheme {
            val theme = HighlightTheme(name = name, colorMapProvider = { colorMap })
            // Override background/text colors if explicitly provided
            return if (backgroundColor != null || defaultTextColor != null) {
                HighlightTheme(
                    name = name,
                    colorMapProvider = {
                        val base = colorMap.toMutableMap()
                        val existing = base["hljs"] ?: SpanStyle()
                        base["hljs"] =
                            existing.copy(
                                background = backgroundColor ?: existing.background,
                                color = defaultTextColor ?: existing.color,
                            )
                        base
                    },
                )
            } else {
                theme
            }
        }
    }
}
