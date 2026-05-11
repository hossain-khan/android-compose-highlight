package dev.hossain.highlight.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.sample.R
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults
import dev.hossain.highlight.ui.rememberHighlightEngine
import dev.hossain.highlight.ui.rememberHighlightedCodeBothThemes

// ── Short snippets reused across all demo sections ──────────────────────────

private val KOTLIN_SNIPPET =
    """
data class User(val name: String, val age: Int)

fun List<User>.adults(): List<User> =
    filter { it.age >= 18 }
    """.trimIndent()

private val PYTHON_SNIPPET =
    """
def fibonacci(n: int) -> int:
    if n <= 1:
        return n
    a, b = 0, 1
    for _ in range(n - 1):
        a, b = b, a + b
    return b
    """.trimIndent()

/**
 * Material 3–inspired light CSS theme used to demonstrate [HighlightTheme.fromCss].
 *
 * Colors are derived from the Material 3 color system (purple primary palette).
 */
private val MATERIAL3_LIGHT_CSS =
    """
.hljs {
  background: #FEF7FF;
  color: #1C1B1F;
}
.hljs-keyword,
.hljs-built_in {
  color: #6750A4;
  font-weight: bold;
}
.hljs-string,
.hljs-attr {
  color: #386A20;
}
.hljs-comment,
.hljs-quote {
  color: #938F99;
  font-style: italic;
}
.hljs-number,
.hljs-literal {
  color: #1565C0;
}
.hljs-name,
.hljs-selector-tag {
  color: #B3261E;
}
.hljs-title,
.hljs-section {
  color: #984061;
  font-weight: bold;
}
.hljs-type {
  color: #006874;
}
    """.trimIndent()

// ── Section composables ─────────────────────────────────────────────────────

/**
 * Demonstrates all [CodeBlockStyle] variants:
 * - [CodeBlockStyle.Default] preset
 * - [CodeBlockStyle.Compact] preset
 * - A custom style with explicit shape, padding, and copy-button size
 */
@Composable
internal fun StylingSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Default preset
        SubSectionHeader("CodeBlockStyle.Default")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style = CodeBlockStyle.Default,
            showLineNumbers = true,
        )

        // Compact preset
        SubSectionHeader("CodeBlockStyle.Compact")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style = CodeBlockStyle.Compact,
        )

        // Fully custom style
        SubSectionHeader("Custom style — sharp corners, wide gutter, small copy button")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style =
                CodeBlockStyle(
                    shape = RoundedCornerShape(4.dp),
                    padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    headerPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    lineNumberColor = Color(0xFF888888),
                    lineNumberWidth = 48.dp,
                    copyButtonSize = 24.dp,
                ),
            showLineNumbers = true,
        )
    }
}

/**
 * Demonstrates [SyntaxHighlightedCode] typography customization via [CodeBlockStyle.textStyle]:
 * - `textStyle.copy(fontSize = ...)` variants
 * - `textStyle.copy(lineHeight = ...)` variants
 * - `textStyle.copy(fontFamily = ...)` variants
 */
@Composable
internal fun TypographySection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Font size variants
        SubSectionHeader("textStyle — fontSize = 13.sp (default)")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
        )

        SubSectionHeader("textStyle — fontSize = 15.sp")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            style =
                CodeBlockStyle(
                    textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontSize = 15.sp),
                ),
        )

        SubSectionHeader("textStyle — fontSize = 18.sp")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            style =
                CodeBlockStyle(
                    textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontSize = 18.sp),
                ),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Line height variants
        SubSectionHeader("textStyle — lineHeight = 20.sp (default)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
        )

        SubSectionHeader("textStyle — lineHeight = 28.sp (spacious)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style =
                CodeBlockStyle(
                    textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(lineHeight = 28.sp),
                ),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Font family variants
        SubSectionHeader("textStyle — fontFamily = FontFamily.Monospace (default)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
        )

        SubSectionHeader("textStyle — fontFamily = FontFamily.Serif")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style =
                CodeBlockStyle(
                    textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontFamily = FontFamily.Serif),
                ),
        )

        SubSectionHeader("textStyle — fontFamily = FontFamily.SansSerif")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            style =
                CodeBlockStyle(
                    textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontFamily = FontFamily.SansSerif),
                ),
        )
    }
}

/**
 * Demonstrates every [SyntaxHighlightedCode] boolean flag combination:
 * - `showLineNumbers` × `showLanguageLabel` (2×2)
 * - `showCopyButton` on/off
 */
@Composable
internal fun TogglesSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubSectionHeader("showLineNumbers=false, showLanguageLabel=true (defaults)")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = false,
            showLanguageLabel = true,
        )

        SubSectionHeader("showLineNumbers=true, showLanguageLabel=true")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = true,
            showLanguageLabel = true,
        )

        SubSectionHeader("showLineNumbers=false, showLanguageLabel=false")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = false,
            showLanguageLabel = false,
        )

        SubSectionHeader("showLineNumbers=true, showLanguageLabel=false")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = true,
            showLanguageLabel = false,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("showCopyButton=true (default)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            showCopyButton = true,
        )

        SubSectionHeader("showCopyButton=false")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            showCopyButton = false,
        )
    }
}

/**
 * Demonstrates [SyntaxHighlightedCode] event callbacks:
 * - `onHighlightComplete` — shows full [HighlightResult] (duration, span count, language).
 * - `onCopyClick` — custom copy handler with inline feedback.
 * - `rememberHighlightEngine().isInitialized` — shows engine warm-up state.
 */
@Composable
internal fun CallbacksSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // onHighlightComplete: show full HighlightResult fields
        var highlightResult by remember { mutableStateOf<HighlightResult?>(null) }

        SubSectionHeader("onHighlightComplete — full HighlightResult")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            onHighlightComplete = { result -> highlightResult = result },
        )
        highlightResult?.let { result ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "language  = \"${result.language}\"",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                    Text(
                        text = "durationMs = ${result.durationMs} ms",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                    val spanLabel = if (result.spanCount == 0) "${result.spanCount}  ⚠ silent failure" else "${result.spanCount}"
                    Text(
                        text = "spanCount  = $spanLabel",
                        style =
                            TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color =
                                    if (result.spanCount ==
                                        0
                                    ) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            ),
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // isInitialized demo
        val engine = rememberHighlightEngine()
        var engineInitialized by remember { mutableStateOf(engine.isInitialized) }

        SubSectionHeader("HighlightEngine.isInitialized")
        Text(
            text = "Highlight a block to warm up the engine, then observe the flag flip.",
            style = TextStyle(fontSize = 13.sp),
        )
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            onHighlightComplete = { engineInitialized = engine.isInitialized },
        )
        Text(
            text = "engine.isInitialized = $engineInitialized",
            style =
                TextStyle(
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (engineInitialized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
            modifier = Modifier.padding(top = 4.dp),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // onCopyClick custom handler demo
        var customCopyMessage by remember { mutableStateOf("") }

        SubSectionHeader("onCopyClick — custom copy handler")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            onCopyClick = { code ->
                customCopyMessage = "📋 Custom handler received ${code.length} chars — not sent to clipboard!"
            },
        )
        if (customCopyMessage.isNotEmpty()) {
            Text(
                text = customCopyMessage,
                style =
                    TextStyle(
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                    ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // copyButtonIcon custom slot demo
        SubSectionHeader("copyButtonIcon — custom vector icon")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            copyButtonIcon = { tint ->
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.content_copy_24dp),
                    modifier = Modifier.size(16.dp),
                    contentDescription = "Copy code",
                    tint = tint,
                )
            },
        )
    }
}

/**
 * Exercises every [HighlightTheme] factory method side-by-side:
 * - Built-in: [HighlightTheme.tomorrow], [HighlightTheme.tomorrowNight],
 *   [HighlightTheme.atomOneLight], [HighlightTheme.atomOneDark]
 * - [HighlightTheme.fromAsset] — GitHub CSS bundled in sample assets
 * - [HighlightTheme.fromCss] — Material 3–inspired inline CSS
 * - [HighlightTheme.fromColorMap] — Material 3–inspired precomputed color map (dark variant)
 */
@Composable
internal fun ThemeCreationSection() {
    val context = LocalContext.current.applicationContext

    // Built-in themes
    val tomorrowTheme = remember(context) { HighlightTheme.tomorrow(context) }
    val tomorrowNightTheme = remember(context) { HighlightTheme.tomorrowNight(context) }
    val atomOneLightTheme = remember(context) { HighlightTheme.atomOneLight(context) }
    val atomOneDarkTheme = remember(context) { HighlightTheme.atomOneDark(context) }

    // fromAsset() — GitHub themes bundled in the sample app's assets/
    val githubTheme = remember(context) { HighlightTheme.fromAsset(context, "themes/github.css", "github") }
    val githubDarkTheme = remember(context) { HighlightTheme.fromAsset(context, "themes/github-dark.css", "github-dark") }

    // fromCss() — Material 3–inspired inline CSS string
    val material3LightTheme =
        remember {
            HighlightTheme.fromCss(
                cssText = MATERIAL3_LIGHT_CSS,
                name = "material3-light",
            )
        }

    // fromColorMap() — Material 3–inspired precomputed dark color map
    val material3DarkTheme =
        remember {
            HighlightTheme.fromColorMap(
                name = "material3-dark",
                colorMap =
                    mapOf(
                        "hljs" to SpanStyle(color = Color(0xFFE6E1E5), background = Color(0xFF1C1B1F)),
                        "hljs-keyword" to SpanStyle(color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold),
                        "hljs-built_in" to SpanStyle(color = Color(0xFFCBA6F7)),
                        "hljs-string" to SpanStyle(color = Color(0xFF6DD58C)),
                        "hljs-attr" to SpanStyle(color = Color(0xFF6DD58C)),
                        "hljs-comment" to SpanStyle(color = Color(0xFF938F99), fontStyle = FontStyle.Italic),
                        "hljs-quote" to SpanStyle(color = Color(0xFF938F99), fontStyle = FontStyle.Italic),
                        "hljs-number" to SpanStyle(color = Color(0xFF7FCFFF)),
                        "hljs-literal" to SpanStyle(color = Color(0xFF7FCFFF)),
                        "hljs-type" to SpanStyle(color = Color(0xFF80CBC4)),
                        "hljs-title" to SpanStyle(color = Color(0xFFFFB4AB), fontWeight = FontWeight.Bold),
                        "hljs-name" to SpanStyle(color = Color(0xFFFFB4AB)),
                        "hljs-selector-tag" to SpanStyle(color = Color(0xFFFFB4AB)),
                    ),
                backgroundColor = Color(0xFF1C1B1F),
                defaultTextColor = Color(0xFFE6E1E5),
            )
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Built-in themes ────────────────────────────────────────────────
        SubSectionHeader("Built-in: HighlightTheme.tomorrow() — light")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = tomorrowTheme,
        )

        SubSectionHeader("Built-in: HighlightTheme.tomorrowNight() — dark")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = tomorrowNightTheme,
        )

        SubSectionHeader("Built-in: HighlightTheme.atomOneLight() — light")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = atomOneLightTheme,
        )

        SubSectionHeader("Built-in: HighlightTheme.atomOneDark() — dark")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = atomOneDarkTheme,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── fromAsset() ────────────────────────────────────────────────────
        SubSectionHeader("fromAsset(): GitHub light (themes/github.css)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = githubTheme,
        )

        SubSectionHeader("fromAsset(): GitHub dark (themes/github-dark.css)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = githubDarkTheme,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── fromCss() ──────────────────────────────────────────────────────
        SubSectionHeader("fromCss(): Material 3 light (inline CSS)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = material3LightTheme,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── fromColorMap() ─────────────────────────────────────────────────
        SubSectionHeader("fromColorMap(): Material 3 dark (precomputed map)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            theme = material3DarkTheme,
        )
    }
}

/**
 * Demonstrates [rememberHighlightedCodeBothThemes]: highlights code **once** for both light and
 * dark themes in a single JavaScript call, enabling instant theme switching without re-highlighting.
 *
 * A toggle lets you flip between light and dark to verify the switch is instant once the
 * initial load completes. The duration of the single JS call is shown below the code block.
 *
 * Also demonstrates:
 * - Silent failure detection via [HighlightResult.spanCount] — an unsupported language
 *   produces 0 spans instead of throwing, displayed with an error-coloured card.
 * - Raw [dev.hossain.highlight.engine.HighlightEngine.highlightToHtml] pipeline — shows the
 *   `<span class="hljs-*">` HTML string before any theme colour is applied.
 *
 * @param isDark Whether the global light/dark toggle (from the top-bar button) is currently dark.
 */
@Composable
internal fun AdvancedEngineSection(isDark: Boolean) {
    val context = LocalContext.current.applicationContext
    val lightTheme = remember(context) { HighlightTheme.tomorrow(context) }
    val darkTheme = remember(context) { HighlightTheme.tomorrowNight(context) }

    var useDark by remember(isDark) { mutableStateOf(isDark) }

    val result by
        rememberHighlightedCodeBothThemes(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            lightTheme = lightTheme,
            darkTheme = darkTheme,
        )

    val displayText = if (useDark) result?.dark else result?.light

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SubSectionHeader("rememberHighlightedCodeBothThemes")
        Text(
            text =
                "Highlights once for both light and dark in a single JS call. " +
                    "Flip the toggle below — switching is instant after the initial load.",
            style = TextStyle(fontSize = 13.sp),
        )

        // Toggle row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = if (useDark) "🌙 Dark" else "☀ Light", style = TextStyle(fontSize = 13.sp))
            Switch(
                checked = useDark,
                onCheckedChange = { useDark = it },
            )
        }

        // Render the highlighted text manually, using a Surface to show the theme's background.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color =
                if (useDark) {
                    darkTheme.backgroundColor.takeIf { it != Color.Unspecified } ?: Color(0xFF1E1E1E)
                } else {
                    lightTheme.backgroundColor.takeIf { it != Color.Unspecified } ?: Color(0xFFFAFAFA)
                },
        ) {
            val textColor =
                if (useDark) {
                    darkTheme.defaultTextColor.takeIf { it != Color.Unspecified } ?: Color(0xFFCCCCCC)
                } else {
                    lightTheme.defaultTextColor.takeIf { it != Color.Unspecified } ?: Color(0xFF333333)
                }
            Text(
                text = displayText ?: AnnotatedString(KOTLIN_SNIPPET),
                modifier = Modifier.padding(16.dp),
                style =
                    TextStyle(
                        color = textColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                    ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Duration metric — read directly from the result state, no separate variable needed
        result?.let { r ->
            Text(
                text = "⏱ Both themes highlighted in ${r.durationMs}ms (single JS call)",
                style =
                    TextStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Silent failure demo
        SubSectionHeader("Silent failure detection via spanCount")
        Text(
            text =
                "Passing an unsupported language produces no tokens (spanCount = 0) instead of " +
                    "throwing an exception. Use spanCount to detect this and warn the caller.",
            style = TextStyle(fontSize = 13.sp),
        )
        var silentFailureResult by remember { mutableStateOf<HighlightResult?>(null) }
        SyntaxHighlightedCode(
            code = "let x = doSomethingCool(42)",
            language = "fakescript",
            modifier = Modifier.fillMaxWidth(),
            onHighlightComplete = { result -> silentFailureResult = result },
        )
        silentFailureResult?.let { result ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color =
                    if (result.spanCount == 0) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                shape = RoundedCornerShape(6.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "language  = \"${result.language}\"",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                    Text(
                        text = "spanCount = ${result.spanCount}" + if (result.spanCount == 0) "  ← no tokens, silent failure" else "",
                        style =
                            TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color =
                                    if (result.spanCount == 0) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            ),
                    )
                    Text(
                        text = "durationMs = ${result.durationMs} ms",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Raw highlightToHtml pipeline demo
        SubSectionHeader("Raw pipeline: highlightToHtml()")
        Text(
            text =
                "The lower-level highlightToHtml() returns the raw HTML string with " +
                    "<span class=\"hljs-*\"> tokens before any theme is applied. " +
                    "Useful when you need to process tokens yourself.",
            style = TextStyle(fontSize = 13.sp),
        )
        val rawEngine = rememberHighlightEngine()
        var rawHtml by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            rawEngine.highlightToHtml("val x = 42", "kotlin").onSuccess { rawHtml = it }
        }
        rawHtml?.let { html ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = html,
                    modifier = Modifier.padding(12.dp),
                    style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Comparison note
        SubSectionHeader("For comparison — standard SyntaxHighlightedCode (re-highlights on toggle)")
        Text(
            text =
                "The block below uses the global theme from HighlightThemeProvider. " +
                    "Switch the top-bar 🌙/☀ button to see re-highlighting happen.",
            style = TextStyle(fontSize = 13.sp),
        )
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Shared UI helpers ───────────────────────────────────────────────────────

/**
 * A smaller sub-section label for grouping related items within a demo section.
 */
@Composable
internal fun SubSectionHeader(title: String) {
    Text(
        text = title,
        style =
            TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.SansSerif,
            ),
        modifier = Modifier.padding(bottom = 4.dp),
    )
}
