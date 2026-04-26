package dev.composehighlight.engine

import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

private const val TAG = "ComposeHighlight"

/**
 * Converts Highlight.js HTML output into a Compose [AnnotatedString].
 *
 * Uses jsoup to parse the HTML fragment and performs a recursive tree walk,
 * pushing/popping [SpanStyle] for each `<span class="hljs-*">` element.
 * This matches Perplexity's production implementation (obfuscated class `gn/d.b()`).
 */
object HtmlToAnnotatedString {

    /**
     * Converts highlighted HTML to [AnnotatedString] using the provided color map.
     *
     * @param html HTML fragment output from highlight.js (not a full document)
     * @param colorMap Map of hljs class names to [SpanStyle], from [ThemeParser]
     */
    fun convert(html: String, colorMap: Map<String, SpanStyle>): AnnotatedString {
        if (html.isBlank()) return AnnotatedString("")

        Log.d(TAG, "HtmlToAnnotatedString: colorMap has ${colorMap.size} entries")

        val doc = Jsoup.parseBodyFragment(html)
        val body = doc.body()

        var spansApplied = 0
        var spansMissed = 0

        val result = buildAnnotatedString {
            body.childNodes().forEach { node ->
                walkNode(node, colorMap, this, onHit = { spansApplied++ }, onMiss = { cls ->
                    spansMissed++
                    Log.d(TAG, "HtmlToAnnotatedString: no color for class='$cls'")
                })
            }
        }

        Log.d(TAG, "HtmlToAnnotatedString: spansApplied=$spansApplied, spansMissed=$spansMissed, textLength=${result.text.length}, spanCount=${result.spanStyles.size}")
        return result
    }

    private fun walkNode(
        node: org.jsoup.nodes.Node,
        colorMap: Map<String, SpanStyle>,
        builder: AnnotatedString.Builder,
        onHit: () -> Unit = {},
        onMiss: (String) -> Unit = {},
    ) {
        when (node) {
            is Element -> {
                val style = if (node.tagName() == "span") {
                    // hljs outputs single-class spans like class="hljs-keyword"
                    // but may also have compound classes like "hljs-title function_"
                    val cls = node.className()
                    val resolved = resolveStyle(cls, colorMap)
                    if (resolved != null) onHit() else if (cls.isNotBlank()) onMiss(cls)
                    resolved
                } else null

                if (style != null) builder.pushStyle(style)

                node.childNodes().forEach { child ->
                    walkNode(child, colorMap, builder, onHit, onMiss)
                }

                if (style != null) builder.pop()
            }

            is TextNode -> {
                // Preserve whitespace verbatim — do not call text() which normalizes whitespace
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
private fun buildAnnotatedString(
    block: AnnotatedString.Builder.() -> Unit,
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    builder.block()
    return builder.toAnnotatedString()
}
