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

internal sealed interface CustomNode

internal class CustomElement(
    val tagName: String,
    val className: String,
    val childNodes: List<CustomNode>,
) : CustomNode

internal class CustomTextNode(
    val wholeText: String,
) : CustomNode

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
                        val classes = cls.trim().split(Regex("\\s+"))
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

        // Try exact match first (e.g. "hljs-keyword")
        colorMap[classAttr]?.let { return it }

        // Try dot-joined compound key (e.g. "hljs-title.function_" for class="hljs-title function_")
        val classes = classAttr.trim().split(Regex("\\s+"))
        if (classes.size > 1) {
            val compoundKey = classes.joinToString(".")
            colorMap[compoundKey]?.let { return it }
        }

        // Fall back to the first recognized class
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
        // Fast-path: exact match (e.g. "hljs-keyword" - the large majority of tokens).
        colorMap[classAttr]?.let { return it }
        // Compound key (e.g. "hljs-title.function_" for class="hljs-title function_").
        if (classes.size > 1) {
            val compoundKey = classes.joinToString(".")
            colorMap[compoundKey]?.let { return it }
        }
        // Fall back to the first recognized class.
        return classes.firstNotNullOfOrNull { colorMap[it] }
    }
}

/**
 * Parses a simple HTML fragment into a lightweight CustomNode tree.
 * Supports basic elements (like span), attributes (like class), HTML comments,
 * and standard HTML entity decoding.
 */
internal fun parseHtml(html: String): List<CustomNode> {
    var index = 0
    val length = html.length

    fun peek(offset: Int = 0): Char? {
        val i = index + offset
        return if (i < length) html[i] else null
    }

    fun decodeEntities(text: String): String {
        if (!text.contains('&')) return text
        val sb = StringBuilder(text.length)
        var i = 0
        val len = text.length
        while (i < len) {
            val c = text[i]
            if (c == '&') {
                val semi = text.indexOf(';', i)
                if (semi != -1 && semi - i < 10) {
                    val entity = text.substring(i + 1, semi)
                    when (entity) {
                        "amp" -> {
                            sb.append('&')
                        }

                        "lt" -> {
                            sb.append('<')
                        }

                        "gt" -> {
                            sb.append('>')
                        }

                        "quot" -> {
                            sb.append('"')
                        }

                        "apos" -> {
                            sb.append('\'')
                        }

                        "nbsp" -> {
                            sb.append('\u00A0')
                        }

                        else -> {
                            if (entity.startsWith("#")) {
                                val code =
                                    if (entity.startsWith("#x")) {
                                        entity.substring(2).toIntOrNull(16)
                                    } else {
                                        entity.substring(1).toIntOrNull(10)
                                    }
                                if (code != null) {
                                    sb.append(code.toChar())
                                } else {
                                    sb.append('&').append(entity).append(';')
                                }
                            } else {
                                sb.append('&').append(entity).append(';')
                            }
                        }
                    }
                    i = semi + 1
                } else {
                    sb.append('&')
                    i++
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    fun extractClassAttr(attrsString: String): String {
        var i = 0
        val len = attrsString.length
        while (i < len) {
            while (i < len && attrsString[i].isWhitespace()) {
                i++
            }
            if (i >= len) break

            val eq = attrsString.indexOf('=', i)
            if (eq == -1) break
            val attrName = attrsString.substring(i, eq).trim().lowercase()
            i = eq + 1

            while (i < len && attrsString[i].isWhitespace()) {
                i++
            }
            if (i >= len) break

            val quote = attrsString[i]
            if (quote == '"' || quote == '\'') {
                i++
                val endQuote = attrsString.indexOf(quote, i)
                if (endQuote != -1) {
                    val attrValue = attrsString.substring(i, endQuote)
                    if (attrName == "class") {
                        return attrValue
                    }
                    i = endQuote + 1
                } else {
                    val attrValue = attrsString.substring(i)
                    if (attrName == "class") {
                        return attrValue
                    }
                    i = len
                }
            } else {
                var endValue = i
                while (endValue < len && !attrsString[endValue].isWhitespace()) {
                    endValue++
                }
                val attrValue = attrsString.substring(i, endValue)
                if (attrName == "class") {
                    return attrValue
                }
                i = endValue
            }
        }
        return ""
    }

    fun parseNodes(parentTag: String?): List<CustomNode> {
        val nodes = mutableListOf<CustomNode>()
        while (index < length) {
            val c = peek() ?: break
            if (c == '<') {
                if (html.startsWith("<!--", index)) {
                    val endComment = html.indexOf("-->", index + 4)
                    if (endComment != -1) {
                        index = endComment + 3
                    } else {
                        index = length
                    }
                    continue
                }

                if (peek(1) == '/') {
                    val tagEnd = html.indexOf('>', index + 2)
                    val tagName =
                        if (tagEnd != -1) {
                            html.substring(index + 2, tagEnd).trim().lowercase()
                        } else {
                            ""
                        }
                    if (tagEnd != -1 && tagName == parentTag) {
                        index = tagEnd + 1
                        break
                    } else if (tagEnd != -1) {
                        index = tagEnd + 1
                        continue
                    } else {
                        index = length
                        break
                    }
                }

                val tagEnd = html.indexOf('>', index + 1)
                if (tagEnd == -1) {
                    val text = html.substring(index)
                    nodes.add(CustomTextNode(decodeEntities(text)))
                    index = length
                    break
                }

                val tagContent = html.substring(index + 1, tagEnd).trim()
                val isSelfClosing = tagContent.endsWith('/')
                val cleanContent = if (isSelfClosing) tagContent.dropLast(1).trim() else tagContent

                val firstSpace = cleanContent.indexOfAny(charArrayOf(' ', '\t', '\r', '\n'))
                val tagName =
                    if (firstSpace != -1) {
                        cleanContent.substring(0, firstSpace).lowercase()
                    } else {
                        cleanContent.lowercase()
                    }

                var className = ""
                if (firstSpace != -1) {
                    val attrsString = cleanContent.substring(firstSpace + 1)
                    className = extractClassAttr(attrsString)
                }

                index = tagEnd + 1

                if (isSelfClosing) {
                    nodes.add(CustomElement(tagName, className, emptyList()))
                } else {
                    val children = parseNodes(tagName)
                    nodes.add(CustomElement(tagName, className, children))
                }
            } else {
                val nextTag = html.indexOf('<', index)
                val text =
                    if (nextTag != -1) {
                        val t = html.substring(index, nextTag)
                        index = nextTag
                        t
                    } else {
                        val t = html.substring(index)
                        index = length
                        t
                    }
                if (text.isNotEmpty()) {
                    nodes.add(CustomTextNode(decodeEntities(text)))
                }
            }
        }
        return nodes
    }

    return parseNodes(null)
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
