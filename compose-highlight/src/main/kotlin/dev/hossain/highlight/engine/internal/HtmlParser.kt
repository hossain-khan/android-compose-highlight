package dev.hossain.highlight.engine.internal

/**
 * A lightweight, custom HTML tokenizer and parser for highlight.js HTML output.
 *
 * ## Rationale: Why we do not use Jsoup
 *
 * 1. **Kotlin Multiplatform (KMP) Readiness**: Jsoup is a JVM-only library. Moving to a pure
 *    Kotlin custom parser enables future support for Kotlin Multiplatform.
 * 2. **Binary Footprint**: Jsoup is a heavy dependency. By implementing a lightweight parser,
 *    we reduce the library's binary footprint.
 * 3. **R8/Proguard Optimization**: Removing Jsoup eliminates complex consumer-side Proguard
 *    rules and potential shrinking bugs.
 * 4. **Single-Purpose Efficiency**: Jsoup is designed for full DOM parsing, sanitization, and
 *    querying. highlight.js outputs a very narrow, safe subset of HTML (nested `span` elements
 *    with `class` attributes, comments, and text). A single-pass tokenizer scoped to that subset
 *    avoids the allocation and CPU cost of a full DOM parser.
 */
internal sealed interface CustomNode

/**
 * Represents an HTML element node (e.g. `<span>`).
 *
 * @property tagName The lowercase name of the HTML tag.
 * @property className The value of the class attribute.
 * @property childNodes The child nodes nested within this element.
 */
internal class CustomElement(
    val tagName: String,
    val className: String,
    val childNodes: List<CustomNode>,
) : CustomNode

/**
 * Represents a text node containing raw or entity-decoded text.
 *
 * @property wholeText The text content of the node.
 */
internal class CustomTextNode(
    val wholeText: String,
) : CustomNode

/**
 * Parses a simple HTML fragment into a lightweight tree of [CustomNode]s.
 *
 * Supports nested elements with class attributes, HTML comments, and the six most common named
 * entities (`&amp;`, `&lt;`, `&gt;`, `&quot;`, `&apos;`, `&nbsp;`) plus numeric character
 * references (`&#NN;` and `&#xNN;`). Unknown named entities pass through as literal text.
 *
 * Recovery from malformed input favors leniency: unmatched closing tags bubble up to enclosing
 * scopes (HTML5-style implicit close), stray closers at the document root are silently skipped,
 * and a `<` with no matching `>` is treated as literal text.
 *
 * @param html HTML fragment (not a full document with `<html>`/`<body>` wrappers).
 * @return A list of parsed [CustomNode]s representing the root-level elements and text.
 */
internal fun parseHtml(html: String): List<CustomNode> {
    var index = 0
    val length = html.length

    fun peek(offset: Int = 0): Char? {
        val i = index + offset
        return if (i < length) html[i] else null
    }

    // Quote-aware scan for the closing `>` of a tag. A bare `indexOf('>')` would stop at a `>`
    // that appears inside a quoted attribute value, e.g. `<span class="a>b">`.
    fun findTagEnd(start: Int): Int {
        var i = start
        while (i < length) {
            val c = html[i]
            if (c == '>') return i
            if (c == '"' || c == '\'') {
                val close = html.indexOf(c, i + 1)
                if (close == -1) return -1
                i = close + 1
                continue
            }
            i++
        }
        return -1
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
                                // Reject surrogate halves (D800-DFFF) and code points outside Unicode
                                // (>U+10FFFF). Use appendCodePoint so non-BMP characters (e.g. emoji at
                                // U+1F600) emit a surrogate pair instead of being truncated by toChar().
                                if (code != null && code in 0..0x10FFFF && code !in 0xD800..0xDFFF) {
                                    sb.appendCodePoint(code)
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
            // The substring between `i` and the next `=` may contain valueless boolean attrs
            // (e.g. `disabled class="x"` has substring `"disabled class"` before the `=`). Treat
            // only the last whitespace-delimited token as the actual attribute name; the leading
            // tokens are valueless attrs we don't care about.
            val attrName =
                attrsString
                    .substring(i, eq)
                    .trim()
                    .substringAfterLast(' ')
                    .lowercase()
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
                    if (tagEnd == -1) {
                        index = length
                        break
                    }
                    val tagName = html.substring(index + 2, tagEnd).trim().lowercase()
                    if (tagName == parentTag) {
                        // Matching closer for our parent: consume it and return.
                        index = tagEnd + 1
                        break
                    }
                    if (parentTag == null) {
                        // Stray closer at the document root - silently skip it.
                        index = tagEnd + 1
                        continue
                    }
                    // Mismatched closer for a nested element. Bubble up: leave the closer in place
                    // (do NOT advance index) and break out so the enclosing parseNodes() call gets
                    // a chance to match it. This mirrors the HTML5 implicit-close behavior Jsoup
                    // produced for inputs like `<span><b>x</span>`: <b> closes when </span> appears.
                    break
                }

                val tagEnd = findTagEnd(index + 1)
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
