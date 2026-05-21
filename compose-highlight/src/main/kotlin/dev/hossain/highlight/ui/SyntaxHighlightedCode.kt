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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.engine.HighlightException
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import kotlinx.coroutines.launch

// Sentinel used to detect when the caller did not supply a custom copyButtonContent.
// This allows the composable body to resolve the default CopyButton with the correct
// size from CodeBlockStyle.copyButtonSize, which cannot be referenced in a parameter
// default value (Kotlin does not allow forward references to other parameters).
private val DefaultCopyButtonSentinel: (@Composable (onClick: () -> Unit) -> Unit) = { }

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
 *     lightHighlightTheme = rememberTomorrowTheme(),
 *     darkHighlightTheme  = rememberAtomOneDarkTheme(),
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
 *     theme    = rememberTomorrowTheme(),
 * )
 * ```
 *
 * ## Custom copy button slot
 *
 * ```kotlin
 * SyntaxHighlightedCode(
 *     code = snippet,
 *     language = "kotlin",
 *     copyButtonContent = { onClick ->
 *         IconButton(onClick = onClick) {
 *             Icon(
 *                 imageVector = ImageVector.vectorResource(R.drawable.content_copy_24dp),
 *                 contentDescription = "Copy",
 *             )
 *         }
 *     },
 * )
 * ```
 *
 * ## Hide header elements
 *
 * ```kotlin
 * SyntaxHighlightedCode(
 *     code = jsonSnippet,
 *     language = "json",
 *     languageLabelContent = null,   // hide the language badge
 *     copyButtonContent    = null,   // hide the copy button
 * )
 * ```
 *
 * ## Custom placeholder while loading
 *
 * ```kotlin
 * SyntaxHighlightedCode(
 *     code = myCode,
 *     language = "kotlin",
 *     placeholder = { rawCode ->
 *         Text(
 *             text = rawCode,
 *             color = Color.Gray.copy(alpha = 0.5f),
 *             fontFamily = FontFamily.Monospace,
 *         )
 *     },
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
 * @param languageLabelContent Optional composable content for the language badge in the header.
 *   `null` hides the badge entirely. The default shows [language] in a dimmed style derived from
 *   the active theme. Renders inside a [Surface] whose [LocalContentColor] is the theme foreground
 *   — use `LocalContentColor.current` inside your slot to inherit it automatically.
 *   Use [SyntaxHighlightedCodeDefaults.LanguageLabel] as a starting point for customisation.
 *   Wrap your lambda in `remember` if it captures an unstable value.
 * @param copyButtonContent Optional composable slot for the copy button in the header. `null`
 *   hides the button entirely. The slot receives an `onClick` action pre-wired to copy [code] to
 *   the system clipboard (or call [onCopyClick] if provided) — pass it to your button's `onClick`.
 *   The default uses [SyntaxHighlightedCodeDefaults.CopyButton].
 *   ```kotlin
 *   copyButtonContent = { onClick ->
 *       TextButton(onClick = onClick) { Text("Copy") }
 *   }
 *   ```
 *   Wrap your lambda in `remember` if it captures unstable values.
 * @param onCopyClick Optional custom copy handler. If `null`, copies to the system clipboard.
 *   This callback is your signal that a copy occurred — use it to show your own feedback
 *   (e.g. a `Snackbar`, `Toast`, or animated indicator). The library does not show any
 *   built-in "Copied!" confirmation.
 * @param onHighlightComplete Optional callback invoked with a [HighlightResult] when highlighting
 *   succeeds. Use [HighlightResult.durationMs] for timing, [HighlightResult.spanCount] to detect
 *   silent failures (0 = no tokens produced), and [HighlightResult.language] to confirm the
 *   language that was highlighted. Useful for performance metrics and test harnesses.
 * @param onError Optional callback invoked with the [HighlightException] when highlighting fails.
 *   The composable always falls back to plain unstyled text on failure - this callback is purely
 *   observational and does not affect the rendered output.
 *   ```kotlin
 *   SyntaxHighlightedCode(
 *       code = myCode,
 *       language = userInput,
 *       onError = { error ->
 *           Log.w("Highlight", "Failed: ${error.message}")
 *       },
 *   )
 *   ```
 * @param placeholder Optional composable rendered while highlighting is in progress (before the
 *   first highlight result is available). When `null` (default), the raw unstyled code is shown
 *   until highlighting completes - preserving the existing behavior. The [code] string is passed
 *   so the placeholder can optionally render it styled differently (e.g., dimmed or with a shimmer
 *   overlay).
 *   ```kotlin
 *   SyntaxHighlightedCode(
 *       code = myCode,
 *       language = "kotlin",
 *       placeholder = { rawCode ->
 *           Text(
 *               text = rawCode,
 *               color = Color.Gray.copy(alpha = 0.5f),
 *               fontFamily = FontFamily.Monospace,
 *           )
 *       },
 *   )
 *   ```
 */
@Composable
fun SyntaxHighlightedCode(
    code: String,
    language: String,
    modifier: Modifier = Modifier,
    theme: HighlightTheme = LocalHighlightTheme.current,
    style: CodeBlockStyle = CodeBlockStyle.Default,
    showLineNumbers: Boolean = false,
    languageLabelContent: (@Composable () -> Unit)? =
        if (language.isNotBlank()) {
            {
                SyntaxHighlightedCodeDefaults.LanguageLabel(
                    language = language,
                    color =
                        LocalContentColor.current.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
            }
        } else {
            null
        },
    copyButtonContent: (@Composable (onClick: () -> Unit) -> Unit)? = DefaultCopyButtonSentinel,
    onCopyClick: ((String) -> Unit)? = null,
    onHighlightComplete: ((HighlightResult) -> Unit)? = null,
    onError: ((HighlightException) -> Unit)? = null,
    placeholder: (@Composable (code: String) -> Unit)? = null,
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

    // Resolve the effective copy button: when the caller used the default (sentinel),
    // substitute a real lambda that forwards style.copyButtonSize so the CodeBlockStyle
    // property actually takes effect.
    val effectiveCopyButton: (@Composable (onClick: () -> Unit) -> Unit)? =
        when {
            copyButtonContent === DefaultCopyButtonSentinel -> {
                { onClick -> SyntaxHighlightedCodeDefaults.CopyButton(onClick = onClick, size = style.copyButtonSize) }
            }

            else -> {
                copyButtonContent
            }
        }
    // In Android Studio Preview, WebView cannot be created. Render a themed fallback
    // (using the active theme's background and text colors) so that @Preview composables
    // work without crashing.
    if (LocalInspectionMode.current) {
        Surface(
            modifier = modifier.testTag("syntax-highlighted-code"),
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

    val latestOnError = rememberUpdatedState(onError)
    var highlightFailed by remember(code, language, theme) { mutableStateOf(false) }
    LaunchedEffect(code, language, theme) {
        highlightFailed = false
    }
    val highlightedState =
        rememberHighlightedCode(
            code = code,
            language = language,
            theme = theme,
            onHighlightComplete = onHighlightComplete,
            onError = { error ->
                highlightFailed = true
                latestOnError.value?.invoke(error)
            },
        )
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier.testTag("syntax-highlighted-code"),
        shape = style.shape,
        color = backgroundColor,
        contentColor = textColor,
    ) {
        Column {
            // Header: language badge + copy button
            if (languageLabelContent != null || effectiveCopyButton != null) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(style.headerPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    languageLabelContent?.invoke()
                    Spacer(modifier = Modifier.weight(1f))
                    if (effectiveCopyButton != null) {
                        effectiveCopyButton {
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
                        if (highlighted == null && placeholder != null && !highlightFailed) {
                            Box(modifier = Modifier.padding(style.padding)) {
                                placeholder(code)
                            }
                        } else if (showLineNumbers) {
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
    modifier: Modifier = Modifier,
) {
    // Count lines from the text that will actually be rendered so line numbers always align.
    // Memoized to avoid recomputing on every recomposition when the rendered text is unchanged.
    val lineCount = remember(highlighted?.text, code) { (highlighted?.text ?: code).lines().size }
    val lineNumbers = remember(lineCount) { (1..lineCount).joinToString("\n") }

    Row(modifier = modifier.padding(style.padding)) {
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
