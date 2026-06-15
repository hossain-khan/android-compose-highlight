package dev.hossain.highlight.engine.internal

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle

// A lightweight, custom HTML tokenizer and parser for highlight.js HTML output.
//
// The library's production hot path uses the SAX-style parseAndBuild and parseAndBuildBoth
// functions, which parse HTML and emit spans directly into one or two AnnotatedString.Builder
// instances in a single pass - no intermediate tree allocation.
//
// ## Rationale: Why we do not use Jsoup
//
// 1. Kotlin Multiplatform (KMP) Readiness: Jsoup is a JVM-only library. Moving to a pure Kotlin
//    custom parser enables future support for Kotlin Multiplatform.
// 2. Binary Footprint: Jsoup is a heavy dependency. By implementing a lightweight parser, we
//    reduce the library's binary footprint.
// 3. R8/Proguard Optimization: Removing Jsoup eliminates complex consumer-side Proguard rules
//    and potential shrinking bugs.
// 4. Single-Purpose Efficiency: Jsoup is designed for full DOM parsing, sanitization, and
//    querying. highlight.js outputs a very narrow, safe subset of HTML (nested span elements
//    with class attributes, comments, and text). A single-pass tokenizer scoped to that subset
//    avoids the allocation and CPU cost of a full DOM parser.

/**
 * SAX-style single-pass parse and build: parses the HTML and directly emits spans into a
 * single [AnnotatedString.Builder] without allocating an intermediate tree.
 *
 * The caller is responsible for pushing/popping the base `.hljs` style outside this function.
 *
 * @param html HTML fragment output from highlight.js
 * @param colorMap Map of hljs class names to [SpanStyle], from ThemeParser
 * @param builder The builder to append text and push/pop styles into
 */
internal fun parseAndBuild(
    html: String,
    colorMap: Map<String, SpanStyle>,
    builder: AnnotatedString.Builder,
) {
    var index = 0
    val length = html.length

    fun walkNodes(parentTag: String?) {
        while (index < length) {
            val c = html[index]
            if (c == '<') {
                if (html.startsWith("<!--", index)) {
                    val endComment = html.indexOf("-->", index + 4)
                    index = if (endComment != -1) endComment + 3 else length
                    continue
                }

                if (index + 1 < length && html[index + 1] == '/') {
                    val tagEnd = html.indexOf('>', index + 2)
                    if (tagEnd == -1) {
                        index = length
                        break
                    }
                    if (parentTag != null && regionMatchesTrimmedLowercase(html, index + 2, tagEnd, parentTag)) {
                        index = tagEnd + 1
                        break
                    }
                    if (parentTag == null) {
                        index = tagEnd + 1
                        continue
                    }
                    break
                }

                val tagEnd = findTagEnd(html, index + 1, length)
                if (tagEnd == -1) {
                    appendDecodedText(builder, html, index, length)
                    index = length
                    break
                }

                val contentStart = skipWhitespace(html, index + 1, tagEnd)
                var contentEnd = tagEnd
                val isSelfClosing = contentEnd > contentStart && html[contentEnd - 1] == '/'
                if (isSelfClosing) {
                    contentEnd = skipWhitespaceReverse(html, contentStart, contentEnd - 1)
                }

                val firstSpace = indexOfWhitespace(html, contentStart, contentEnd)
                val tagNameEnd = if (firstSpace != -1) firstSpace else contentEnd

                val tagName = lowercaseSubstring(html, contentStart, tagNameEnd)

                var style: SpanStyle? = null
                if (tagName == "span" && firstSpace != -1) {
                    val className = extractClassAttrInPlace(html, firstSpace + 1, contentEnd)
                    if (className.isNotEmpty()) {
                        style = resolveStyle(className, colorMap)
                    }
                }

                index = tagEnd + 1

                if (isSelfClosing) {
                    // Self-closing elements produce no text or children.
                } else {
                    if (style != null) builder.pushStyle(style)
                    walkNodes(tagName)
                    if (style != null) builder.pop()
                }
            } else {
                val nextTag = html.indexOf('<', index)
                val textEnd = if (nextTag != -1) nextTag else length
                if (textEnd > index) {
                    appendDecodedText(builder, html, index, textEnd)
                }
                index = textEnd
            }
        }
    }

    walkNodes(null)
}

/**
 * SAX-style single-pass parse and build for dual-theme output: parses the HTML once and
 * emits spans into two [AnnotatedString.Builder]s simultaneously.
 *
 * Semantically equivalent to calling [parseAndBuild] twice with different color maps, but
 * the HTML is parsed only once.
 *
 * @param html HTML fragment output from highlight.js
 * @param lightColorMap Color map for the light theme
 * @param darkColorMap Color map for the dark theme
 * @param lightBuilder Builder for the light-theme AnnotatedString
 * @param darkBuilder Builder for the dark-theme AnnotatedString
 */
internal fun parseAndBuildBoth(
    html: String,
    lightColorMap: Map<String, SpanStyle>,
    darkColorMap: Map<String, SpanStyle>,
    lightBuilder: AnnotatedString.Builder,
    darkBuilder: AnnotatedString.Builder,
) {
    var index = 0
    val length = html.length

    // Hoisted to avoid per-call allocation in the hot loop.
    val whitespaceRegex = HtmlParserConstants.WHITESPACE_REGEX

    fun walkNodes(parentTag: String?) {
        while (index < length) {
            val c = html[index]
            if (c == '<') {
                if (html.startsWith("<!--", index)) {
                    val endComment = html.indexOf("-->", index + 4)
                    index = if (endComment != -1) endComment + 3 else length
                    continue
                }

                if (index + 1 < length && html[index + 1] == '/') {
                    val tagEnd = html.indexOf('>', index + 2)
                    if (tagEnd == -1) {
                        index = length
                        break
                    }
                    if (parentTag != null && regionMatchesTrimmedLowercase(html, index + 2, tagEnd, parentTag)) {
                        index = tagEnd + 1
                        break
                    }
                    if (parentTag == null) {
                        index = tagEnd + 1
                        continue
                    }
                    break
                }

                val tagEnd = findTagEnd(html, index + 1, length)
                if (tagEnd == -1) {
                    appendDecodedTextBoth(lightBuilder, darkBuilder, html, index, length)
                    index = length
                    break
                }

                val contentStart = skipWhitespace(html, index + 1, tagEnd)
                var contentEnd = tagEnd
                val isSelfClosing = contentEnd > contentStart && html[contentEnd - 1] == '/'
                if (isSelfClosing) {
                    contentEnd = skipWhitespaceReverse(html, contentStart, contentEnd - 1)
                }

                val firstSpace = indexOfWhitespace(html, contentStart, contentEnd)
                val tagNameEnd = if (firstSpace != -1) firstSpace else contentEnd

                val tagName = lowercaseSubstring(html, contentStart, tagNameEnd)

                var lightStyle: SpanStyle? = null
                var darkStyle: SpanStyle? = null
                if (tagName == "span" && firstSpace != -1) {
                    val className = extractClassAttrInPlace(html, firstSpace + 1, contentEnd)
                    if (className.isNotBlank()) {
                        // Parse the class list once; reuse for both color-map lookups.
                        val classes = className.trim().split(whitespaceRegex)
                        lightStyle = resolveStyleFromClasses(className, classes, lightColorMap)
                        darkStyle = resolveStyleFromClasses(className, classes, darkColorMap)
                    }
                }

                index = tagEnd + 1

                if (!isSelfClosing) {
                    if (lightStyle != null) lightBuilder.pushStyle(lightStyle)
                    if (darkStyle != null) darkBuilder.pushStyle(darkStyle)
                    walkNodes(tagName)
                    if (lightStyle != null) lightBuilder.pop()
                    if (darkStyle != null) darkBuilder.pop()
                }
            } else {
                val nextTag = html.indexOf('<', index)
                val textEnd = if (nextTag != -1) nextTag else length
                if (textEnd > index) {
                    appendDecodedTextBoth(lightBuilder, darkBuilder, html, index, textEnd)
                }
                index = textEnd
            }
        }
    }

    walkNodes(null)
}

// ─────────────────────────────────────────────────────────────────────────────
// Internal helper functions used by the SAX-style builders
// ─────────────────────────────────────────────────────────────────────────────

/** Module-level constants shared by the SAX builders and HtmlToAnnotatedString. */
internal object HtmlParserConstants {
    val WHITESPACE_REGEX = Regex("\\s+")
}

/**
 * Quote-aware scan for the closing `>` of a tag. A bare `indexOf('>')` would stop at a `>`
 * that appears inside a quoted attribute value, e.g. `<span class="a>b">`.
 */
private fun findTagEnd(
    html: String,
    start: Int,
    length: Int,
): Int {
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

/**
 * Decodes HTML entities in the given text. Supports the six standard named entities
 * and numeric character references (decimal and hex).
 */
internal fun decodeEntities(text: String): String {
    if (!text.contains('&')) return text
    val sb = StringBuilder(text.length)
    var i = 0
    val len = text.length
    while (i < len) {
        val c = text[i]
        if (c == '&') {
            val semi = text.indexOf(';', i)
            if (semi != -1 && semi - i < 10) {
                val decoded = decodeEntityInline(text, i + 1, semi)
                if (decoded != null) {
                    sb.append(decoded)
                    i = semi + 1
                    continue
                }
                // Decode numeric references
                if (text[i + 1] == '#') {
                    val code = parseNumericEntity(text, i + 2, semi)
                    if (code != null && code in 0..0x10FFFF && code !in 0xD800..0xDFFF) {
                        sb.appendCodePoint(code)
                        i = semi + 1
                        continue
                    }
                }
                // Unknown entity - pass through literally
                sb.append(text, i, semi + 1)
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

/**
 * Tries to decode a named entity between [start] (exclusive of `&`) and [end] (exclusive of `;`).
 * Returns the decoded character as a String, or null if unknown.
 */
private fun decodeEntityInline(
    text: String,
    start: Int,
    end: Int,
): String? {
    val len = end - start
    // Fast-path the six standard entities by length, then first-char discrimination.
    return when (len) {
        2 -> {
            when {
                text[start] == 'l' && text[start + 1] == 't' -> "<"
                text[start] == 'g' && text[start + 1] == 't' -> ">"
                else -> null
            }
        }

        3 -> {
            when {
                text[start] == 'a' && text[start + 1] == 'm' && text[start + 2] == 'p' -> "&"
                else -> null
            }
        }

        4 -> {
            when {
                text[start] == 'q' && regionEquals(text, start, "quot") -> "\""
                text[start] == 'a' && regionEquals(text, start, "apos") -> "'"
                text[start] == 'n' && regionEquals(text, start, "nbsp") -> "\u00A0"
                else -> null
            }
        }

        else -> {
            null
        }
    }
}

/** Helper to compare a region of text against a known string. */
private fun regionEquals(
    text: String,
    start: Int,
    expected: String,
): Boolean {
    for (j in expected.indices) {
        if (text[start + j] != expected[j]) return false
    }
    return true
}

/** Parses a numeric character reference (decimal or hex) between positions. */
private fun parseNumericEntity(
    text: String,
    start: Int,
    semi: Int,
): Int? =
    if (start < semi && text[start] == 'x') {
        // Hex: &#xNN;
        parseHexInt(text, start + 1, semi)
    } else {
        // Decimal: &#NN;
        parseDecInt(text, start, semi)
    }

/** Parses a hex integer from the character range [start, end) without allocating a substring. */
private fun parseHexInt(
    text: String,
    start: Int,
    end: Int,
): Int? {
    if (start >= end) return null
    var result = 0
    for (i in start until end) {
        val c = text[i]
        val digit =
            when (c) {
                in '0'..'9' -> c - '0'
                in 'a'..'f' -> c - 'a' + 10
                in 'A'..'F' -> c - 'A' + 10
                else -> return null
            }
        result = result * 16 + digit
        if (result > 0x10FFFF) return null // Overflow guard
    }
    return result
}

/** Parses a decimal integer from the character range [start, end) without allocating a substring. */
private fun parseDecInt(
    text: String,
    start: Int,
    end: Int,
): Int? {
    if (start >= end) return null
    var result = 0
    for (i in start until end) {
        val c = text[i]
        if (c !in '0'..'9') return null
        result = result * 10 + (c - '0')
        if (result > 0x10FFFF) return null // Overflow guard
    }
    return result
}

/**
 * Extracts the `class` attribute value from the attribute region of a tag, operating
 * directly on the parent HTML string without allocating intermediate substrings for
 * attribute names/values that are not `class`.
 *
 * @param html The full HTML string
 * @param start Start index of the attributes region (after tag name)
 * @param end End index of the attributes region (before `>` or `/`)
 * @return The class attribute value, or empty string if not found
 */
private fun extractClassAttrInPlace(
    html: String,
    start: Int,
    end: Int,
): String {
    var i = start
    while (i < end) {
        // Skip whitespace
        while (i < end && html[i].isWhitespace()) i++
        if (i >= end) break

        val eq = html.indexOf('=', i)
        if (eq == -1 || eq >= end) break

        // Check if the attribute name (last whitespace-delimited token before '=') is "class".
        // The substring between `i` and the `=` may contain valueless boolean attrs
        // (e.g. `disabled class="x"` has "disabled class" before the =). We only care about
        // the last token.
        val isClassAttr = isLastTokenClass(html, i, eq)

        i = eq + 1

        // Skip whitespace after =
        while (i < end && html[i].isWhitespace()) i++
        if (i >= end) break

        val quote = html[i]
        if (quote == '"' || quote == '\'') {
            i++
            val endQuote = html.indexOf(quote, i)
            if (endQuote != -1 && endQuote <= end) {
                if (isClassAttr) {
                    return html.substring(i, endQuote)
                }
                i = endQuote + 1
            } else {
                if (isClassAttr) {
                    return html.substring(i, end)
                }
                i = end
            }
        } else {
            val valueStart = i
            while (i < end && !html[i].isWhitespace()) i++
            if (isClassAttr) {
                return html.substring(valueStart, i)
            }
        }
    }
    return ""
}

/**
 * Checks if the last whitespace-delimited token in html[start, end) is "class" (case-insensitive).
 * Avoids allocating any substrings.
 */
private fun isLastTokenClass(
    html: String,
    start: Int,
    end: Int,
): Boolean {
    // Find the start of the last token by scanning backwards from end.
    var tokenStart = end - 1
    // Trim trailing whitespace
    while (tokenStart >= start && html[tokenStart].isWhitespace()) tokenStart--
    if (tokenStart < start) return false
    val tokenEnd = tokenStart + 1

    // Find the start of this token
    while (tokenStart > start && !html[tokenStart - 1].isWhitespace()) tokenStart--

    // Check if this token is "class" (5 chars)
    val tokenLen = tokenEnd - tokenStart
    if (tokenLen != 5) return false
    return (html[tokenStart] == 'c' || html[tokenStart] == 'C') &&
        (html[tokenStart + 1] == 'l' || html[tokenStart + 1] == 'L') &&
        (html[tokenStart + 2] == 'a' || html[tokenStart + 2] == 'A') &&
        (html[tokenStart + 3] == 's' || html[tokenStart + 3] == 'S') &&
        (html[tokenStart + 4] == 's' || html[tokenStart + 4] == 'S')
}

/**
 * Compares the trimmed, lowercased content of html[start, end) against [expected].
 * Used for closing-tag matching without allocating a substring.
 */
private fun regionMatchesTrimmedLowercase(
    html: String,
    start: Int,
    end: Int,
    expected: String,
): Boolean {
    var s = start
    var e = end
    // Trim leading whitespace
    while (s < e && html[s].isWhitespace()) s++
    // Trim trailing whitespace
    while (e > s && html[e - 1].isWhitespace()) e--

    val len = e - s
    if (len != expected.length) return false
    for (j in 0 until len) {
        if (html[s + j].lowercaseChar() != expected[j]) return false
    }
    return true
}

/** Skips leading whitespace in html[start, end) and returns the first non-whitespace index. */
private fun skipWhitespace(
    html: String,
    start: Int,
    end: Int,
): Int {
    var i = start
    while (i < end && html[i].isWhitespace()) i++
    return i
}

/** Skips trailing whitespace in html[start, end) and returns the end index after trimming. */
private fun skipWhitespaceReverse(
    html: String,
    start: Int,
    end: Int,
): Int {
    var i = end
    while (i > start && html[i - 1].isWhitespace()) i--
    return i
}

/** Finds the first whitespace character in html[start, end), or -1 if none. */
private fun indexOfWhitespace(
    html: String,
    start: Int,
    end: Int,
): Int {
    for (i in start until end) {
        val c = html[i]
        if (c == ' ' || c == '\t' || c == '\r' || c == '\n') return i
    }
    return -1
}

/** Checks if [ch] exists anywhere in html[start, end). */
private fun containsChar(
    html: String,
    start: Int,
    end: Int,
    ch: Char,
): Boolean {
    for (i in start until end) {
        if (html[i] == ch) return true
    }
    return false
}

/**
 * Returns the substring html[start, end), skipping [String.lowercase] allocation if
 * the region is already all-lowercase. highlight.js always emits lowercase tags, so this
 * avoids an allocation on the hot path.
 */
private fun lowercaseSubstring(
    html: String,
    start: Int,
    end: Int,
): String {
    var needsLowercase = false
    for (i in start until end) {
        if (html[i] in 'A'..'Z') {
            needsLowercase = true
            break
        }
    }
    val sub = html.substring(start, end)
    return if (needsLowercase) sub.lowercase() else sub
}

/**
 * Appends decoded text from html[start, end) into the builder, skipping entity decoding
 * when no `&` is present (the common case for highlight.js text nodes).
 */
private fun appendDecodedText(
    builder: AnnotatedString.Builder,
    html: String,
    start: Int,
    end: Int,
) {
    val hasAmpersand = containsChar(html, start, end, '&')
    val text = html.substring(start, end)
    builder.append(if (hasAmpersand) decodeEntities(text) else text)
}

/**
 * Appends decoded text from html[start, end) into both builders simultaneously.
 */
private fun appendDecodedTextBoth(
    lightBuilder: AnnotatedString.Builder,
    darkBuilder: AnnotatedString.Builder,
    html: String,
    start: Int,
    end: Int,
) {
    val hasAmpersand = containsChar(html, start, end, '&')
    val text = html.substring(start, end)
    val decoded = if (hasAmpersand) decodeEntities(text) else text
    lightBuilder.append(decoded)
    darkBuilder.append(decoded)
}

// ─────────────────────────────────────────────────────────────────────────────
// Style resolution - shared by SAX builders (duplicated from HtmlToAnnotatedString
// to keep the SAX path self-contained without pulling in the object's private methods)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolves the best [SpanStyle] for a given class attribute value.
 *
 * hljs class attributes can be:
 * - Single: `"hljs-keyword"`
 * - Compound space-separated: `"hljs-title function_"` (two classes)
 *
 * Tries the full joined key first, then falls back to each individual class.
 */
internal fun resolveStyle(
    classAttr: String,
    colorMap: Map<String, SpanStyle>,
): SpanStyle? {
    if (classAttr.isBlank()) return null
    colorMap[classAttr]?.let { return it }
    val classes = classAttr.trim().split(HtmlParserConstants.WHITESPACE_REGEX)
    if (classes.size > 1) {
        val compoundKey = classes.joinToString(".")
        colorMap[compoundKey]?.let { return it }
    }
    return classes.firstNotNullOfOrNull { colorMap[it] }
}

/**
 * Like [resolveStyle] but accepts a pre-parsed class list so the dual-theme hot path
 * can parse the class attribute once and reuse it for both color-map lookups.
 */
internal fun resolveStyleFromClasses(
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
