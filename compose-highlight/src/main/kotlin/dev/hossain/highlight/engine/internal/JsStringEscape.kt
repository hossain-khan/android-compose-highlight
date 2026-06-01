package dev.hossain.highlight.engine.internal

/**
 * Unescapes a JSON-encoded string returned by [android.webkit.WebView.evaluateJavascript].
 *
 * Uses a single character-by-character pass to correctly handle all escape sequences,
 * including cases like `\\n` (JSON for a literal backslash followed by 'n') that sequential
 * [String.replace] calls cannot handle correctly (the `\\` and `\n` replacements interfere).
 *
 * Supported escape sequences: `\"`, `\\`, `\/`, `\n`, `\r`, `\t`, `\uXXXX`.
 *
 * UTF-16 surrogate pairs (two consecutive `\uXXXX` sequences where the first is a high surrogate
 * U+D800-U+DBFF and the second is a low surrogate U+DC00-U+DFFF) are combined into a single
 * supplementary code point. This is required to correctly decode emoji and other characters above
 * U+FFFF that `evaluateJavascript` encodes as surrogate pairs.
 */
internal fun unescapeJsString(jsonString: String): String {
    // Strip surrounding double quotes if present
    val inner =
        if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
            jsonString.substring(1, jsonString.length - 1)
        } else {
            jsonString
        }
    val sb = StringBuilder(inner.length)
    var i = 0
    while (i < inner.length) {
        val c = inner[i]
        if (c == '\\' && i + 1 < inner.length) {
            when (inner[i + 1]) {
                '"' -> {
                    sb.append('"')
                    i += 2
                }

                '\\' -> {
                    sb.append('\\')
                    i += 2
                }

                '/' -> {
                    sb.append('/')
                    i += 2
                }

                'n' -> {
                    sb.append('\n')
                    i += 2
                }

                'r' -> {
                    sb.append('\r')
                    i += 2
                }

                't' -> {
                    sb.append('\t')
                    i += 2
                }

                'u' -> {
                    // \uXXXX - exactly 4 hex digits required
                    if (i + 5 < inner.length) {
                        val hex = inner.substring(i + 2, i + 6)
                        val codePoint = hex.toIntOrNull(16)
                        if (codePoint != null) {
                            // Check for a UTF-16 surrogate pair: high surrogate followed by \uXXXX low surrogate.
                            // Characters above U+FFFF (e.g. emoji) are encoded by evaluateJavascript as two
                            // consecutive \uXXXX sequences representing the UTF-16 surrogate pair.
                            if (codePoint in 0xD800..0xDBFF &&
                                i + 11 < inner.length &&
                                inner[i + 6] == '\\' &&
                                inner[i + 7] == 'u'
                            ) {
                                val lowHex = inner.substring(i + 8, i + 12)
                                val lowSurrogate = lowHex.toIntOrNull(16)
                                if (lowSurrogate != null && lowSurrogate in 0xDC00..0xDFFF) {
                                    // Combine the surrogate pair into a single supplementary code point.
                                    val supplementary =
                                        Character.toCodePoint(codePoint.toChar(), lowSurrogate.toChar())
                                    sb.appendCodePoint(supplementary)
                                    i += 12 // skip both \uXXXX sequences
                                } else {
                                    // Not a valid low surrogate - emit the high surrogate as-is (best effort).
                                    sb.append(codePoint.toChar())
                                    i += 6
                                }
                            } else {
                                sb.append(codePoint.toChar())
                                i += 6
                            }
                        } else {
                            sb.append(c)
                            i++
                        }
                    } else {
                        sb.append(c)
                        i++
                    }
                }

                else -> {
                    sb.append(c)
                    i++
                }
            }
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString()
}

/**
 * Escapes a string for safe interpolation into a single-quoted JavaScript string literal.
 *
 * Escape order:
 * 1. `\` -> `\\` (must be first to avoid double-escaping subsequent replacements)
 * 2. `'` -> `\'`
 * 3. `\n` (LF, U+000A) -> `\n`
 * 4. `\r` (CR, U+000D) -> `\r`
 * 5. `\t` (HT, U+0009) -> `\t`
 * 6. U+2028 (Line Separator) -> `\u2028` (pre-ES2019 JS treats this as a line terminator)
 * 7. U+2029 (Paragraph Separator) -> `\u2029` (pre-ES2019 JS treats this as a line terminator)
 * 8. Remaining control characters U+0000-U+001F (null byte, ANSI escapes, etc.) -> `\uXXXX`
 *
 * Steps 6-7 are required for compatibility with WebView on pre-Android 10 devices (pre-ES2019
 * V8). Without these escapes, a string containing U+2028 or U+2029 would produce an unterminated
 * string literal in the JS engine, resulting in a `HighlightException.JsExecutionFailed`.
 *
 * Step 8 prevents silent string corruption: the null byte (U+0000) can cause V8 to truncate
 * the string at that position, and ANSI escape codes (U+001B) common in terminal output are
 * explicitly escaped to avoid interaction with the highlight.js parser.
 */
internal fun escapeForJs(str: String): String {
    val sb = StringBuilder(str.length + 8)
    for (c in str) {
        when (c) {
            '\\' -> {
                sb.append("\\\\")
            }

            '\'' -> {
                sb.append("\\'")
            }

            '\n' -> {
                sb.append("\\n")
            }

            '\r' -> {
                sb.append("\\r")
            }

            '\t' -> {
                sb.append("\\t")
            }

            '\u2028' -> {
                sb.append("\\u2028")
            }

            '\u2029' -> {
                sb.append("\\u2029")
            }

            else -> {
                val code = c.code
                if (code in 0x00..0x1F) {
                    sb.append("\\u").append(code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(c)
                }
            }
        }
    }
    return sb.toString()
}
