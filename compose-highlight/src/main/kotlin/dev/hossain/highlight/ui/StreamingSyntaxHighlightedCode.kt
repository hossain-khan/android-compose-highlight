package dev.hossain.highlight.ui

import android.content.ClipData
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.engine.HighlightException
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.internal.CodeBlockHeader
import dev.hossain.highlight.ui.internal.DefaultActionsSentinel
import dev.hossain.highlight.ui.internal.DefaultHeaderSentinel
import dev.hossain.highlight.ui.internal.DefaultLanguageLabelSentinel
import kotlinx.coroutines.launch

private val LineNumberGutterSpacing = 8.dp

/**
 * Displays streaming, syntax-highlighted code in a styled block tailored for real-time token generation
 * (e.g. LLM responses, terminal logs, and live code generators).
 *
 * This composable is marked **experimental** ([ExperimentalHighlightApi]). Call sites must
 * opt in with `@OptIn(ExperimentalHighlightApi::class)` or propagate the annotation.
 *
 * ## Differences from [SyntaxHighlightedCode]
 * - **0 ms Streaming Latency:** As incoming text grows, new characters render immediately while
 *   spans from previous lines are preserved via span-transfer ([rememberStreamingHighlightedCode]).
 * - **No Animation Thrash:** Updates spans in-place without `AnimatedContent` cross-fading, avoiding
 *   visual ghosting during active token generation.
 * - **Streaming-Aware Scroll:** Horizontal scroll position is preserved while new tokens are appended
 *   to the stream, rather than resetting to `0` on every token.
 *
 * ## Usage
 *
 * ```kotlin
 * HighlightThemeProvider(
 *     lightHighlightTheme = rememberTomorrowLightTheme(),
 *     darkHighlightTheme  = rememberAtomOneDarkTheme(),
 * ) {
 *     StreamingSyntaxHighlightedCode(
 *         code            = streamingLlmResponse,
 *         language        = "kotlin",
 *         showLineNumbers = true,
 *     )
 * }
 * ```
 *
 * @param code The source code to display (actively growing or static).
 * @param language Highlight.js language identifier (e.g. `"python"`, `"kotlin"`, `"json"`).
 * @param modifier Modifier for the outer container. Applies `testTag("streaming-syntax-highlighted-code")`
 *   to the outer surface.
 * @param theme The theme to use. Defaults to [LocalHighlightTheme].
 * @param style Visual style configuration - shape, padding, line numbers, typography, etc.
 * @param showLineNumbers Whether to show a line-number gutter on the left.
 * @param debounceMs Delay in milliseconds to debounce highlight engine calls after token arrival.
 *   Defaults to [StreamingSyntaxHighlightedCodeDefaults.DEBOUNCE_MS] (200 ms).
 * @param triggerOnNewline Whether to trigger a background highlight run when a new newline (`\n`)
 *   is detected in the stream, progressively styling completed lines. Defaults to `true`.
 * @param minThrottleMs Minimum interval in milliseconds between consecutive newline-triggered
 *   highlight runs to avoid engine overload. Defaults to
 *   [StreamingSyntaxHighlightedCodeDefaults.MIN_THROTTLE_MS] (150 ms).
 * @param scrollState Hoisted scroll state for horizontal scrolling.
 * @param languageLabel Optional composable for the language badge in the header. `null` hides it.
 *   Ignored when [header] is customized.
 * @param actions Optional composable slot for trailing header actions (copy, edit, share, etc.).
 *   `null` hides all trailing actions. Runs with a [RowScope] receiver and receives an `onCopy`
 *   action pre-wired to copy [code] to the system clipboard (or call [onCopyClick] if provided).
 *   The default renders [SyntaxHighlightedCodeDefaults.CopyButton].
 *   Ignored when [header] is customized.
 * @param header Optional coarse-grained slot that replaces the entire header row. `null` renders
 *   no header chrome at all. Receives an `onCopy` action pre-wired to copy [code] to the system
 *   clipboard (or call [onCopyClick] if provided). When customized, [languageLabel] and
 *   [actions] are ignored. The default delegates to [SyntaxHighlightedCodeDefaults.Header].
 * @param onCopyClick Optional custom copy callback. When `null`, copies to the system clipboard.
 * @param onHighlightComplete Optional callback invoked with [HighlightResult] when highlighting succeeds.
 * @param onError Optional callback invoked with [HighlightException] when highlighting fails.
 */
@ExperimentalHighlightApi
@Composable
public fun StreamingSyntaxHighlightedCode(
    code: String,
    language: String,
    modifier: Modifier = Modifier,
    theme: HighlightTheme = LocalHighlightTheme.current,
    style: CodeBlockStyle = CodeBlockStyle.Default,
    showLineNumbers: Boolean = false,
    debounceMs: Long = StreamingSyntaxHighlightedCodeDefaults.DEBOUNCE_MS,
    triggerOnNewline: Boolean = true,
    minThrottleMs: Long = StreamingSyntaxHighlightedCodeDefaults.MIN_THROTTLE_MS,
    scrollState: ScrollState = rememberScrollState(),
    languageLabel: (@Composable () -> Unit)? =
        if (language.isNotBlank()) DefaultLanguageLabelSentinel else null,
    actions: (@Composable RowScope.(onCopy: () -> Unit) -> Unit)? = DefaultActionsSentinel,
    header: (@Composable (onCopy: () -> Unit) -> Unit)? = DefaultHeaderSentinel,
    onCopyClick: ((String) -> Unit)? = null,
    onHighlightComplete: ((HighlightResult) -> Unit)? = null,
    onError: ((HighlightException) -> Unit)? = null,
) {
    val backgroundColor =
        remember(theme, style) {
            theme.backgroundColor.takeIf { it != Color.Unspecified }
                ?: style.fallbackBackgroundColor
        }
    val textColor =
        remember(theme, style) {
            theme.defaultTextColor.takeIf { it != Color.Unspecified }
                ?: style.fallbackTextColor
        }
    val lineNumberColor =
        remember(theme, style) {
            style.lineNumberColor.takeIf { it != Color.Unspecified }
                ?: (theme.defaultTextColor.takeIf { it != Color.Unspecified } ?: style.fallbackTextColor).copy(alpha = 0.4f)
        }

    val themedCodeStyle = remember(theme, style) { style.textStyle.copy(color = textColor) }
    val themedLineNumStyle = remember(theme, style) { style.textStyle.copy(color = lineNumberColor) }

    if (LocalInspectionMode.current) {
        Surface(
            modifier = modifier.testTag("streaming-syntax-highlighted-code"),
            shape = style.shape,
            color = backgroundColor,
            contentColor = textColor,
        ) {
            Text(
                text = code,
                modifier = Modifier.padding(style.padding),
                style = themedCodeStyle,
            )
        }
        return
    }

    val highlightedCode =
        rememberStreamingHighlightedCode(
            code = code,
            language = language,
            theme = theme,
            debounceMs = debounceMs,
            triggerOnNewline = triggerOnNewline,
            minThrottleMs = minThrottleMs,
            onHighlightComplete = onHighlightComplete,
            onError = onError,
        )

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val onCopyAction: () -> Unit =
        remember(code, onCopyClick, clipboard, scope) {
            {
                val handler = onCopyClick
                if (handler != null) {
                    handler(code)
                } else {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("code", code)),
                        )
                    }
                }
            }
        }

    var previousCode by rememberSaveable { mutableStateOf(code) }
    var previousLanguage by rememberSaveable { mutableStateOf(language) }

    LaunchedEffect(code, language) {
        val prev = previousCode
        val isAppend = code.startsWith(prev) && language == previousLanguage
        if (!isAppend) {
            scrollState.scrollTo(0)
        }
        previousCode = code
        previousLanguage = language
    }

    Surface(
        modifier = modifier.testTag("streaming-syntax-highlighted-code"),
        shape = style.shape,
        color = backgroundColor,
        contentColor = textColor,
    ) {
        Column {
            // Header: language badge + actions (or a fully custom header slot)
            CodeBlockHeader(
                language = language,
                style = style,
                onCopy = onCopyAction,
                languageLabel = languageLabel,
                actions = actions,
                header = header,
            )

            Box(modifier = Modifier.horizontalScroll(scrollState)) {
                SelectionContainer {
                    if (showLineNumbers) {
                        LineNumberedStreamingCode(
                            code = code,
                            highlighted = highlightedCode,
                            codeTextStyle = themedCodeStyle,
                            lineNumTextStyle = themedLineNumStyle,
                            style = style,
                        )
                    } else {
                        Text(
                            text = highlightedCode,
                            modifier = Modifier.padding(style.padding),
                            style = themedCodeStyle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LineNumberedStreamingCode(
    code: String,
    highlighted: AnnotatedString,
    codeTextStyle: TextStyle,
    lineNumTextStyle: TextStyle,
    style: CodeBlockStyle,
    modifier: Modifier = Modifier,
) {
    val lineCount = remember(highlighted.text, code) { highlighted.text.lines().size }
    val lineNumbers = remember(lineCount) { (1..lineCount).joinToString("\n") }

    Row(modifier = modifier.padding(style.padding)) {
        Text(
            text = lineNumbers,
            style = lineNumTextStyle,
            modifier = Modifier.widthIn(min = style.lineNumberWidth),
            textAlign = TextAlign.End,
            softWrap = false,
        )
        Spacer(modifier = Modifier.width(LineNumberGutterSpacing))
        Text(
            text = highlighted,
            style = codeTextStyle,
        )
    }
}

@OptIn(ExperimentalHighlightApi::class)
@Preview(showBackground = true)
@Composable
private fun StreamingSyntaxHighlightedCodePreview() {
    StreamingSyntaxHighlightedCode(
        code = "fun stream() = println(\"Streaming...\")",
        language = "kotlin",
        theme = rememberTomorrowLightTheme(),
    )
}
