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

// Hoisted to module scope so the dual-theme tree walk doesn't recompile the pattern per span.
private val WHITESPACE_REGEX = Regex("\\s+")

/**
 * Converts Highlight.js HTML output into a Compose [AnnotatedString].
 *
 * Uses a lightweight, custom HTML tokenizer/parser that runs in a single pass,
 * pushing/popping [SpanStyle] for each `<span class="hljs-*">` element.
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
     * Measures time separately for the custom parse pass and the tree walk,
     * so [HighlightEngine] can populate [HighlightTimings] fields.
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

        val (bodyNodes, htmlParseDuration) = measureTimedValue { parseHtml(html) }

        // Apply the .hljs base text color across the entire string so that plain-text tokens
        // (identifiers, whitespace, etc.) inherit the theme color rather than LocalContentColor.
        val baseTextColor = colorMap[HljsSelectors.BASE]?.color?.takeIf { it != Color.Unspecified }
        val baseStyle = baseTextColor?.let { SpanStyle(color = it) }

        val (result, treeWalkDuration) =
            measureTimedValue {
                buildAnnotatedString {
                    if (baseStyle != null) pushStyle(baseStyle)
                    bodyNodes.forEach { node ->
                        walkNode(node, colorMap, this)
                    }
                    if (baseStyle != null) pop()
                }
            }

        return TimedConvertResult(result, htmlParseDuration, treeWalkDuration)
    }

    /**
     * Converts highlighted HTML to two [AnnotatedString] values - one per theme - in a single
     * parse and traversal pass.
     *
     * Semantically equivalent to calling [convert] twice with different color maps, but more
     * efficient: the HTML is parsed once and the custom tree is walked once. Both builders receive
     * text nodes and span styles in parallel, each resolved against their own color map.
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
     * Semantically equivalent to [convertBothThemes] but also returns timing for the shared
     * HTML parse and the combined dual-theme tree walk.
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

        val (bodyNodes, htmlParseDuration) = measureTimedValue { parseHtml(html) }

        // Each builder gets its own independent base text color from its own color map.
        // Do NOT share a single base style - light and dark themes have different default colors.
        val lightBaseStyle = lightColorMap[HljsSelectors.BASE]?.color?.takeIf { it != Color.Unspecified }?.let { SpanStyle(color = it) }
        val darkBaseStyle = darkColorMap[HljsSelectors.BASE]?.color?.takeIf { it != Color.Unspecified }?.let { SpanStyle(color = it) }

        val lightBuilder = AnnotatedString.Builder()
        val darkBuilder = AnnotatedString.Builder()

        val (_, treeWalkDuration) =
            measureTimedValue {
                if (lightBaseStyle != null) lightBuilder.pushStyle(lightBaseStyle)
                if (darkBaseStyle != null) darkBuilder.pushStyle(darkBaseStyle)

                bodyNodes.forEach { node ->
                    walkNodeBothThemes(node, lightColorMap, darkColorMap, lightBuilder, darkBuilder)
                }

                if (lightBaseStyle != null) lightBuilder.pop()
                if (darkBaseStyle != null) darkBuilder.pop()
            }

        return TimedConvertBothResult(
            light = lightBuilder.toAnnotatedString(),
            dark = darkBuilder.toAnnotatedString(),
            htmlParseDuration = htmlParseDuration,
            treeWalkDuration = treeWalkDuration,
        )
    }

    private fun walkNode(
        node: CustomNode,
        colorMap: Map<String, SpanStyle>,
        builder: AnnotatedString.Builder,
    ) {
        when (node) {
            is CustomElement -> {
                val style =
                    if (node.tagName == "span") {
                        val cls = node.className
                        resolveStyle(cls, colorMap)
                    } else {
                        null
                    }

                if (style != null) builder.pushStyle(style)

                node.childNodes.forEach { child ->
                    walkNode(child, colorMap, builder)
                }

                if (style != null) builder.pop()
            }

            is CustomTextNode -> {
                builder.append(node.wholeText)
            }
        }
    }

    private fun walkNodeBothThemes(
        node: CustomNode,
        lightColorMap: Map<String, SpanStyle>,
        darkColorMap: Map<String, SpanStyle>,
        lightBuilder: AnnotatedString.Builder,
        darkBuilder: AnnotatedString.Builder,
    ) {
        when (node) {
            is CustomElement -> {
                // Evaluate tagName once; resolve styles for both color maps in the same pass.
                val lightStyle: SpanStyle?
                val darkStyle: SpanStyle?
                if (node.tagName == "span") {
                    val cls = node.className
                    if (cls.isBlank()) {
                        lightStyle = null
                        darkStyle = null
                    } else {
                        // Parse the class list once; reuse for both color-map lookups to avoid
                        // redundant trim/split/Regex work on the hot dual-theme path.
                        val classes = cls.trim().split(WHITESPACE_REGEX)
                        lightStyle = resolveStyleFromClasses(cls, classes, lightColorMap)
                        darkStyle = resolveStyleFromClasses(cls, classes, darkColorMap)
                    }
                } else {
                    lightStyle = null
                    darkStyle = null
                }

                if (lightStyle != null) lightBuilder.pushStyle(lightStyle)
                if (darkStyle != null) darkBuilder.pushStyle(darkStyle)

                node.childNodes.forEach { child ->
                    walkNodeBothThemes(child, lightColorMap, darkColorMap, lightBuilder, darkBuilder)
                }

                if (lightStyle != null) lightBuilder.pop()
                if (darkStyle != null) darkBuilder.pop()
            }

            is CustomTextNode -> {
                lightBuilder.append(node.wholeText)
                darkBuilder.append(node.wholeText)
            }
        }
    }

    /**
     * Resolves the best [SpanStyle] for a given element class attribute.
     *
     * hljs class attributes can be:
     * - Single: `"hljs-keyword"`
     * - Compound space-separated: `"hljs-title function_"` (two classes)
     *
     * Tries the full joined key first, then falls back to each individual class.
     */
    private fun resolveStyle(
        classAttr: String,
        colorMap: Map<String, SpanStyle>,
    ): SpanStyle? {
        if (classAttr.isBlank()) return null
        colorMap[classAttr]?.let { return it }
        val classes = classAttr.trim().split(WHITESPACE_REGEX)
        if (classes.size > 1) {
            val compoundKey = classes.joinToString(".")
            colorMap[compoundKey]?.let { return it }
        }
        return classes.firstNotNullOfOrNull { colorMap[it] }
    }

    /**
     * Like [resolveStyle] but accepts a pre-parsed class list so the [walkNodeBothThemes]
     * hot path can parse the class attribute once and reuse it for both color-map lookups.
     */
    private fun resolveStyleFromClasses(
        classAttr: String,
        classes: List<String>,
        colorMap: Map<String, SpanStyle>,
    ): SpanStyle? {
        colorMap[classAttr]?.let { return it }
        if (classes.size > 1) {
            val compoundKey = classes.joinToString(".")
            colorMap[compoundKey]?.let { return it }
        }
        return classes.firstNotNullOfOrNull { colorMap[it] }
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
