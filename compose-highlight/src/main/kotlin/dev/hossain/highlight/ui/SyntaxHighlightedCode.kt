package dev.hossain.highlight.ui

import android.content.ClipData
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import kotlinx.coroutines.launch

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
 *     lightHighlightTheme = HighlightTheme.tomorrow(LocalContext.current.applicationContext),
 *     darkHighlightTheme  = HighlightTheme.atomOneDark(LocalContext.current.applicationContext),
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
 *     theme    = HighlightTheme.tomorrow(LocalContext.current.applicationContext),
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
 *         shape     = RoundedCornerShape(4.dp),
 *         padding   = PaddingValues(8.dp),
 *         textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontSize = 15.sp),
 *     ),
 *     showCopyButton = false,
 * )
 * ```
 *
 * @param code The source code to display.
 * @param language Highlight.js language identifier (e.g. `"python"`, `"kotlin"`).
 * @param modifier Modifier for the outer container. The composable also applies a
 *   `testTag("syntax-highlighted-code")` on the outer surface to support UI testing.
 * @param theme The theme to use. Defaults to [LocalHighlightTheme]. Throws if no
 *   [HighlightThemeProvider] is present and no explicit theme is passed.
 * @param style Visual style configuration — shape, padding, line-number column, font, etc.
 *   Use [CodeBlockStyle.textStyle] to override typography (font family, size, line height).
 *   See [SyntaxHighlightedCodeDefaults] for the default values.
 * @param showLineNumbers Whether to show a line-number gutter on the left.
 * @param showLanguageLabel Whether to show the language badge in the header.
 * @param showCopyButton Whether to show the copy-to-clipboard button.
 * @param onCopyClick Optional custom copy handler. If `null`, copies to the system clipboard.
 *   This callback is your signal that a copy occurred — use it to show your own feedback
 *   (e.g. a `Snackbar`, `Toast`, or animated indicator). The library does not show any
 *   built-in "Copied!" confirmation.
 * @param copyButtonIcon Optional composable slot that replaces the default `⧉` copy icon.
 *   Receives the recommended `tint` [Color] derived from the active theme so the icon blends
 *   naturally with the code block background. Only used when [showCopyButton] is `true`.
 *   Example:
 *   ```kotlin
 *   SyntaxHighlightedCode(
 *       code = snippet,
 *       language = "kotlin",
 *       copyButtonIcon = { tint ->
 *           Icon(
 *               imageVector = ImageVector.vectorResource(R.drawable.content_copy_24dp),
 *               contentDescription = "Copy",
 *               tint = tint,
 *           )
 *       },
 *   )
 *   ```
 * @param copyButtonContentDescription The content description for the copy button, used by
 *   accessibility services like TalkBack. Defaults to `"Copy code"`. Provide a localized string
 *   for non-English users.
 * @param onHighlightComplete Optional callback invoked with a [HighlightResult] when highlighting
 *   succeeds. Use [HighlightResult.durationMs] for timing, [HighlightResult.spanCount] to detect
 *   silent failures (0 = no tokens produced), and [HighlightResult.language] to confirm the
 *   language that was highlighted. Useful for performance metrics and test harnesses.
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
    copyButtonIcon: (@Composable (tint: Color) -> Unit)? = null,
    copyButtonContentDescription: String = "Copy code",
    onHighlightComplete: ((HighlightResult) -> Unit)? = null,
) {
    // Remember derived colors and text styles keyed on theme and style so they are only
    // recomputed when the theme or style actually changes, not on every recomposition.
    val backgroundColor =
        remember(theme, style) {
            theme.backgroundColor.takeIf { it != Color.Unspecified }
                ?: Color(0xFF1E1E1E)
        }
    val textColor =
        remember(theme, style) {
            theme.defaultTextColor.takeIf { it != Color.Unspecified }
                ?: Color(0xFFCCCCCC)
        }
    val lineNumberColor =
        remember(theme, style) {
            style.lineNumberColor.takeIf { it != Color.Unspecified }
                ?: (theme.defaultTextColor.takeIf { it != Color.Unspecified } ?: Color(0xFFCCCCCC)).copy(alpha = 0.4f)
        }

    // Apply the theme's foreground color on top of the caller-supplied text style.
    val themedCodeStyle = remember(theme, style) { style.textStyle.copy(color = textColor) }
    val themedLineNumStyle = remember(theme, style) { style.textStyle.copy(color = lineNumberColor) }
    val languageLabelStyle =
        remember(theme, style) {
            style.textStyle.copy(
                color = textColor.copy(alpha = 0.6f),
                fontSize = 12.sp,
                lineHeight = TextUnit.Unspecified,
            )
        }

    // In Android Studio Preview, WebView cannot be created. Render a themed fallback
    // (using the active theme's background and text colors) so that @Preview composables
    // work without crashing.
    if (LocalInspectionMode.current) {
        Surface(
            modifier = modifier.testTag("syntax-highlighted-code"),
            shape = style.shape,
            color = backgroundColor,
        ) {
            Text(
                text = code,
                modifier = Modifier.padding(style.padding),
                style = themedCodeStyle,
            )
        }
        return
    }

    val highlightedState = rememberHighlightedCode(code, language, theme, onHighlightComplete)
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier.testTag("syntax-highlighted-code"),
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
                            style = languageLabelStyle,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (showCopyButton) {
                        CopyButton(
                            size = style.copyButtonSize,
                            tint = textColor.copy(alpha = 0.7f),
                            customIcon = copyButtonIcon,
                            contentDescription = copyButtonContentDescription,
                            onClick = {
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
                            },
                        )
                    }
                }
            }

            // Code content with horizontal scroll
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AnimatedContent(
                    targetState = highlightedState.value,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "syntax-highlight-fade",
                ) { highlighted ->
                    SelectionContainer {
                        if (showLineNumbers) {
                            LineNumberedCode(
                                code = code,
                                highlighted = highlighted,
                                codeTextStyle = themedCodeStyle,
                                lineNumTextStyle = themedLineNumStyle,
                                style = style,
                            )
                        } else {
                            Text(
                                text = highlighted ?: AnnotatedString(code),
                                modifier = Modifier.padding(style.padding),
                                style = themedCodeStyle,
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
    codeTextStyle: TextStyle,
    lineNumTextStyle: TextStyle,
    style: CodeBlockStyle,
) {
    // Count lines from the text that will actually be rendered so line numbers always align.
    // Memoized to avoid recomputing on every recomposition when the rendered text is unchanged.
    val lineCount = remember(highlighted?.text, code) { (highlighted?.text ?: code).lines().size }
    val lineNumbers = remember(lineCount) { (1..lineCount).joinToString("\n") }

    Row(modifier = Modifier.padding(style.padding)) {
        // Line number gutter — rendered as a single Text to share the same line-height
        // behaviour as the code Text, keeping numbers and code visually aligned.
        Text(
            text = lineNumbers,
            style = lineNumTextStyle,
            modifier = Modifier.width(style.lineNumberWidth),
            textAlign = TextAlign.End,
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Code text
        if (highlighted != null) {
            Text(text = highlighted, style = codeTextStyle)
        } else {
            Text(text = code, style = codeTextStyle)
        }
    }
}

@Composable
private fun CopyButton(
    size: androidx.compose.ui.unit.Dp,
    tint: Color,
    customIcon: (@Composable (tint: Color) -> Unit)?,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(size).semantics { this.contentDescription = contentDescription },
    ) {
        if (customIcon != null) {
            customIcon(tint)
        } else {
            // Default: simple text icon — no bundled icon dependency
            Text(
                text = "⧉",
                style = TextStyle(color = tint, fontSize = 16.sp),
            )
        }
    }
}
