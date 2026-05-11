package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberHighlightEngine
import dev.hossain.highlight.ui.rememberHighlightedCodeBothThemes

private val DarkCodeBackground = Color(0xFF1E1E1E)
private val LightCodeBackground = Color(0xFFFAFAFA)
private val DarkCodeText = Color(0xFFCCCCCC)
private val LightCodeText = Color(0xFF333333)

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
                    darkTheme.backgroundColor.takeIf { it != Color.Unspecified } ?: DarkCodeBackground
                } else {
                    lightTheme.backgroundColor.takeIf { it != Color.Unspecified } ?: LightCodeBackground
                },
        ) {
            val textColor =
                if (useDark) {
                    darkTheme.defaultTextColor.takeIf { it != Color.Unspecified } ?: DarkCodeText
                } else {
                    lightTheme.defaultTextColor.takeIf { it != Color.Unspecified } ?: LightCodeText
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
