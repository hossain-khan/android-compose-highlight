package dev.hossain.highlight.engine

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Converts Highlight.js HTML output into a Compose [AnnotatedString].
 *
 * Uses jsoup to parse the HTML fragment and performs a recursive tree walk,
 * pushing/popping [SpanStyle] for each `<span class="hljs-*">` element.
 */
object HtmlToAnnotatedString {
    /**
     * Converts highlighted HTML to [AnnotatedString] using the provided color map.
     *
     * @param html HTML fragment output from highlight.js (not a full document)
     * @param colorMap Map of hljs class names to [SpanStyle], from [ThemeParser]
     */
    fun convert(
        html: String,
        colorMap: Map<String, SpanStyle>,
    ): AnnotatedString {
        if (html.isBlank()) return AnnotatedString("")

        val doc = Jsoup.parseBodyFragment(html)
        val body = doc.body()

        val result =
            buildAnnotatedString {
                body.childNodes().forEach { node ->
                    walkNode(node, colorMap, this)
                }
            }

        return result
    }

    private fun walkNode(
        node: org.jsoup.nodes.Node,
        colorMap: Map<String, SpanStyle>,
        builder: AnnotatedString.Builder,
    ) {
        when (node) {
            is Element -> {
                val style =
                    if (node.tagName() == "span") {
                        val cls = node.className()
                        resolveStyle(cls, colorMap)
                    } else {
                        null
                    }

                if (style != null) builder.pushStyle(style)

                node.childNodes().forEach { child ->
                    walkNode(child, colorMap, builder)
                }

                if (style != null) builder.pop()
            }

            is TextNode -> {
                builder.append(node.wholeText)
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
}

// Extension to use buildAnnotatedString pattern without Compose runtime
private fun buildAnnotatedString(block: AnnotatedString.Builder.() -> Unit): AnnotatedString {
    val builder = AnnotatedString.Builder()
    builder.block()
    return builder.toAnnotatedString()
}
