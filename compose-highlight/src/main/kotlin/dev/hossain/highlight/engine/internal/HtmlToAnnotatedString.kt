package dev.hossain.highlight.engine.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightTimings
import dev.hossain.highlight.engine.HljsSelectors
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * Converts Highlight.js HTML output into a Compose [AnnotatedString].
 *
 * Uses a lightweight, custom HTML tokenizer/parser. When called via [convertTimed] or
 * [convertBothThemesTimed], uses a SAX-style single-pass approach that parses the HTML and
 * builds the [AnnotatedString] simultaneously, eliminating the intermediate tree of
 * [CustomNode] objects.
 *
 * The tree-building [parseHtml] function is still available for unit testing the parser
 * in isolation.
 */
internal object HtmlToAnnotatedString {
    /**
     * Converts highlighted HTML to [AnnotatedString] using the provided color map.
     *
     * The `.hljs` base rule's text color (if present in [colorMap]) is applied as an
     * outer span covering the full string. This means callers can render the result with a
     * plain `Text()` composable without needing to pass an explicit `color` - uncolored tokens
     * inherit the theme's default text color rather than `LocalContentColor`.
     *
     * @param html HTML fragment output from highlight.js (not a full document)
     * @param colorMap Map of hljs class names to [SpanStyle], from [ThemeParser]
     */
    fun convert(
        html: String,
        colorMap: Map<String, SpanStyle>,
    ): AnnotatedString = convertTimed(html, colorMap).annotated

    /**
     * Converts highlighted HTML to [AnnotatedString] with per-stage timing data.
     *
     * Uses a SAX-style single-pass approach: the HTML is parsed and the [AnnotatedString]
     * is built simultaneously, eliminating the intermediate [CustomNode] tree. The
     * `htmlParseDuration` field in the returned [TimedConvertResult] represents the combined
     * parse+build time; `treeWalkDuration` is [Duration.ZERO] since there is no separate
     * tree walk.
     *
     * @param html HTML fragment output from highlight.js (not a full document)
     * @param colorMap Map of hljs class names to [SpanStyle], from [ThemeParser]
     * @return [TimedConvertResult] with the [AnnotatedString] and per-stage durations
     */
    internal fun convertTimed(
        html: String,
        colorMap: Map<String, SpanStyle>,
    ): TimedConvertResult {
        if (html.isBlank()) return TimedConvertResult(AnnotatedString(""), Duration.ZERO, Duration.ZERO)

        // Apply the .hljs base text color across the entire string so that plain-text tokens
        // (identifiers, whitespace, etc.) inherit the theme color rather than LocalContentColor.
        val baseTextColor = colorMap[HljsSelectors.BASE]?.color?.takeIf { it != Color.Unspecified }
        val baseStyle = baseTextColor?.let { SpanStyle(color = it) }

        val (result, parseBuildDuration) =
            measureTimedValue {
                buildAnnotatedString {
                    if (baseStyle != null) pushStyle(baseStyle)
                    parseAndBuild(html, colorMap, this)
                    if (baseStyle != null) pop()
                }
            }

        // Report the combined parse+build time as htmlParseDuration; there is no separate tree walk.
        return TimedConvertResult(result, parseBuildDuration, Duration.ZERO)
    }

    /**
     * Converts highlighted HTML to two [AnnotatedString] values - one per theme - in a single
     * parse and traversal pass.
     *
     * Semantically equivalent to calling [convert] twice with different color maps, but more
     * efficient: the HTML is parsed once and both builders receive text nodes and span styles
     * in parallel, each resolved against their own color map.
     *
     * Each builder independently applies the `.hljs` base text color from its own color map,
     * so light and dark outputs have the correct default text colors.
     *
     * @param html HTML fragment output from highlight.js (not a full document)
     * @param lightColorMap Color map for the light theme, from [ThemeParser]
     * @param darkColorMap Color map for the dark theme, from [ThemeParser]
     * @return A [Pair] of (light [AnnotatedString], dark [AnnotatedString])
     */
    internal fun convertBothThemes(
        html: String,
        lightColorMap: Map<String, SpanStyle>,
        darkColorMap: Map<String, SpanStyle>,
    ): Pair<AnnotatedString, AnnotatedString> {
        val timed = convertBothThemesTimed(html, lightColorMap, darkColorMap)
        return Pair(timed.light, timed.dark)
    }

    /**
     * Converts highlighted HTML to two [AnnotatedString] values with per-stage timing data.
     *
     * Uses a SAX-style single-pass approach: the HTML is parsed once and both builders
     * are populated simultaneously. The `htmlParseDuration` field represents the combined
     * parse+build time; `treeWalkDuration` is [Duration.ZERO].
     *
     * @param html HTML fragment output from highlight.js (not a full document)
     * @param lightColorMap Color map for the light theme, from [ThemeParser]
     * @param darkColorMap Color map for the dark theme, from [ThemeParser]
     * @return [TimedConvertBothResult] with both [AnnotatedString] values and per-stage durations
     */
    internal fun convertBothThemesTimed(
        html: String,
        lightColorMap: Map<String, SpanStyle>,
        darkColorMap: Map<String, SpanStyle>,
    ): TimedConvertBothResult {
        if (html.isBlank()) {
            return TimedConvertBothResult(
                light = AnnotatedString(""),
                dark = AnnotatedString(""),
                htmlParseDuration = Duration.ZERO,
                treeWalkDuration = Duration.ZERO,
            )
        }

        // Each builder gets its own independent base text color from its own color map.
        // Do NOT share a single base style - light and dark themes have different default colors.
        val lightBaseStyle = lightColorMap[HljsSelectors.BASE]?.color?.takeIf { it != Color.Unspecified }?.let { SpanStyle(color = it) }
        val darkBaseStyle = darkColorMap[HljsSelectors.BASE]?.color?.takeIf { it != Color.Unspecified }?.let { SpanStyle(color = it) }

        val lightBuilder = AnnotatedString.Builder()
        val darkBuilder = AnnotatedString.Builder()

        val (_, parseBuildDuration) =
            measureTimedValue {
                if (lightBaseStyle != null) lightBuilder.pushStyle(lightBaseStyle)
                if (darkBaseStyle != null) darkBuilder.pushStyle(darkBaseStyle)

                parseAndBuildBoth(html, lightColorMap, darkColorMap, lightBuilder, darkBuilder)

                if (lightBaseStyle != null) lightBuilder.pop()
                if (darkBaseStyle != null) darkBuilder.pop()
            }

        return TimedConvertBothResult(
            light = lightBuilder.toAnnotatedString(),
            dark = darkBuilder.toAnnotatedString(),
            htmlParseDuration = parseBuildDuration,
            treeWalkDuration = Duration.ZERO,
        )
    }
}

/**
 * Internal result type for [HtmlToAnnotatedString.convertTimed].
 */
internal data class TimedConvertResult(
    val annotated: AnnotatedString,
    val htmlParseDuration: Duration,
    val treeWalkDuration: Duration,
)

/**
 * Internal result type for [HtmlToAnnotatedString.convertBothThemesTimed].
 */
internal data class TimedConvertBothResult(
    val light: AnnotatedString,
    val dark: AnnotatedString,
    val htmlParseDuration: Duration,
    val treeWalkDuration: Duration,
)
