package dev.hossain.highlight.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.engine.HighlightTheme
import kotlinx.coroutines.delay

/**
 * Displays syntax-highlighted code in a styled block.
 *
 * Shows unstyled monospace code immediately while async highlighting runs,
 * then fades in the highlighted version when ready (no visible flicker).
 *
 * This composable reads the active theme from [LocalHighlightTheme], so a
 * [HighlightThemeProvider] ancestor **must** exist in the composition tree, or you
 * must pass an explicit [theme] parameter.
 *
 * ## Usage — with HighlightThemeProvider (recommended)
 *
 * ```kotlin
 * HighlightThemeProvider(
 *     lightHighlightTheme = HighlightTheme.tomorrow(LocalContext.current),
 *     darkHighlightTheme  = HighlightTheme.atomOneDark(LocalContext.current),
 * ) {
 *     SyntaxHighlightedCode(
 *         code            = """fun greet(name: String) = "Hello, ${'$'}name!"""",
 *         language        = "kotlin",
 *         showLineNumbers = true,
 *     )
 * }
 * ```
 *
 * ## Usage — with an explicit theme
 *
 * ```kotlin
 * SyntaxHighlightedCode(
 *     code     = "SELECT * FROM users WHERE active = 1",
 *     language = "sql",
 *     theme    = HighlightTheme.tomorrow(LocalContext.current),
 * )
 * ```
 *
 * ## Custom styling
 *
 * ```kotlin
 * SyntaxHighlightedCode(
 *     code     = jsonSnippet,
 *     language = "json",
 *     style    = CodeBlockStyle(
 *         shape   = RoundedCornerShape(4.dp),
 *         padding = PaddingValues(8.dp),
 *     ),
 *     showCopyButton = false,
 * )
 * ```
 *
 * @param code The source code to display.
 * @param language Highlight.js language identifier (e.g. `"python"`, `"kotlin"`).
 * @param modifier Modifier for the outer container.
 * @param theme The theme to use. Defaults to [LocalHighlightTheme]. Throws if no
 *   [HighlightThemeProvider] is present and no explicit theme is passed.
 * @param style Visual style configuration — shape, padding, line-number column, etc.
 * @param showLineNumbers Whether to show a line-number gutter on the left.
 * @param showLanguageLabel Whether to show the language badge in the header.
 * @param showCopyButton Whether to show the copy-to-clipboard button.
 * @param onCopyClick Optional custom copy handler. If `null`, copies to the system clipboard.
 * @param onHighlightComplete Optional callback invoked with the highlight duration in milliseconds
 *   when highlighting succeeds. Useful for performance metrics.
 * @param fontFamily Font family for the code text. Defaults to monospace.
 * @param fontSize Font size for the code text.
 * @param lineHeight Line height for the code text.
 */
@Composable
fun SyntaxHighlightedCode(
    code: String,
    language: String,
    modifier: Modifier = Modifier,
    theme: HighlightTheme = LocalHighlightTheme.current,
    style: CodeBlockStyle = CodeBlockStyle.Default,
    showLineNumbers: Boolean = false,
    showLanguageLabel: Boolean = true,
    showCopyButton: Boolean = true,
    onCopyClick: ((String) -> Unit)? = null,
    onHighlightComplete: ((durationMs: Long) -> Unit)? = null,
    fontFamily: FontFamily = FontFamily.Monospace,
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 20.sp,
) {
    val highlightedState = rememberHighlightedCode(code, language, theme, onHighlightComplete)
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var copyConfirmed by remember { mutableStateOf(false) }

    val backgroundColor =
        theme.backgroundColor.takeIf { it != Color.Unspecified }
            ?: Color(0xFF1E1E1E)
    val textColor =
        theme.defaultTextColor.takeIf { it != Color.Unspecified }
            ?: Color(0xFFCCCCCC)
    val lineNumberColor =
        style.lineNumberColor.takeIf { it != Color.Unspecified }
            ?: textColor.copy(alpha = 0.4f)

    Surface(
        modifier = modifier,
        shape = style.shape,
        color = backgroundColor,
    ) {
        Column {
            // Header: language badge + copy button
            if (showLanguageLabel || showCopyButton) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(style.headerPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showLanguageLabel && language.isNotBlank()) {
                        Text(
                            text = language,
                            style =
                                TextStyle(
                                    color = textColor.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontFamily = fontFamily,
                                ),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (showCopyButton) {
                        CopyButton(
                            copyConfirmed = copyConfirmed,
                            size = style.copyButtonSize,
                            tint = textColor.copy(alpha = 0.7f),
                            onClick = {
                                val handler = onCopyClick
                                if (handler != null) {
                                    handler(code)
                                } else {
                                    clipboardManager.setText(AnnotatedString(code))
                                }
                                copyConfirmed = true
                            },
                        )
                        // Reset "Copied!" after 2 seconds
                        if (copyConfirmed) {
                            LaunchedEffect(copyConfirmed) {
                                delay(2_000)
                                copyConfirmed = false
                            }
                        }
                    }
                }
            }

            // Code content with horizontal scroll
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                SelectionContainer {
                    AnimatedContent(
                        targetState = highlightedState.value,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "syntax-highlight-fade",
                    ) { highlighted ->
                        if (showLineNumbers) {
                            LineNumberedCode(
                                code = code,
                                highlighted = highlighted,
                                textColor = textColor,
                                lineNumberColor = lineNumberColor,
                                style = style,
                                fontFamily = fontFamily,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                            )
                        } else {
                            Text(
                                text = highlighted ?: AnnotatedString(code),
                                modifier = Modifier.padding(style.padding),
                                style =
                                    TextStyle(
                                        color = textColor,
                                        fontFamily = fontFamily,
                                        fontSize = fontSize,
                                        lineHeight = lineHeight,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LineNumberedCode(
    code: String,
    highlighted: AnnotatedString?,
    textColor: Color,
    lineNumberColor: Color,
    style: CodeBlockStyle,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    lineHeight: TextUnit,
) {
    val lines = code.lines()
    val codeStyle =
        TextStyle(
            color = textColor,
            fontFamily = fontFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
        )
    val lineNumStyle =
        TextStyle(
            color = lineNumberColor,
            fontFamily = fontFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
        )

    Row(modifier = Modifier.padding(style.padding)) {
        // Line number gutter
        Column(modifier = Modifier.width(style.lineNumberWidth)) {
            lines.forEachIndexed { index, _ ->
                Text(
                    text = "${index + 1}",
                    style = lineNumStyle,
                )
            }
        }
        // Code text
        if (highlighted != null) {
            Text(text = highlighted, style = codeStyle)
        } else {
            Text(text = code, style = codeStyle)
        }
    }
}

@Composable
private fun CopyButton(
    copyConfirmed: Boolean,
    size: androidx.compose.ui.unit.Dp,
    tint: Color,
    onClick: () -> Unit,
) {
    if (copyConfirmed) {
        Text(
            text = "Copied!",
            style = TextStyle(color = tint, fontSize = 12.sp),
        )
    } else {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(size),
        ) {
            // Use a simple text icon since we don't bundle icons
            Text(
                text = "⧉",
                style = TextStyle(color = tint, fontSize = 16.sp),
            )
        }
    }
}
