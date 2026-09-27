package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.R

/**
 * Default values and helper composables used by [SyntaxHighlightedCode] and [CodeBlockStyle].
 *
 * Expose constants so callers can build on them without hard-coding magic numbers, and helper
 * composables so callers can compose on top of the built-in defaults:
 *
 * ```kotlin
 * // Use the library default text style with only fontSize overridden
 * val myStyle = CodeBlockStyle(
 *     textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontSize = 15.sp),
 * )
 *
 * // Start from Compact but widen the line-number gutter
 * val myCompact = CodeBlockStyle.Compact.copy(
 *     lineNumberWidth = SyntaxHighlightedCodeDefaults.lineNumberWidth + 16.dp,
 * )
 *
 * // Customise the actions slot while keeping the default copy button
 * SyntaxHighlightedCode(
 *     code = snippet,
 *     language = "kotlin",
 *     actions = { onCopy ->
 *         SyntaxHighlightedCodeDefaults.CopyButton(
 *             onClick = onCopy,
 *             contentDescription = stringResource(R.string.copy_code),
 *         )
 *     },
 * )
 * ```
 */
public object SyntaxHighlightedCodeDefaults {
    /**
     * Default [TextStyle] applied to the code text: monospace font, 13 sp size, 20 sp line height.
     *
     * Pass a copy to [CodeBlockStyle.textStyle] to override just the properties you care about:
     *
     * ```kotlin
     * SyntaxHighlightedCode(
     *     code     = snippet,
     *     language = "kotlin",
     *     style    = CodeBlockStyle(
     *         textStyle = SyntaxHighlightedCodeDefaults.codeTextStyle.copy(fontSize = 15.sp),
     *     ),
     * )
     * ```
     */
    public val codeTextStyle: TextStyle =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )

    /** Default corner radius for the code block container. */
    public val shape: Shape = RoundedCornerShape(8.dp)

    /** Default inner padding for the code content area. */
    public val padding: PaddingValues = PaddingValues(16.dp)

    /** Default padding for the header row (language label + copy button). */
    public val headerPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

    /**
     * Default minimum width reserved for the line-number gutter.
     *
     * The gutter automatically expands beyond this value for code blocks with 1,000+ lines
     * or larger typography to fit multi-digit line numbers without wrapping.
     */
    public val lineNumberWidth: Dp = 32.dp

    /** Default size (width and height) of the copy button. */
    public val copyButtonSize: Dp = 32.dp

    /**
     * Default background color used when the active theme CSS has no `.hljs { background: ... }`
     * rule. Matches the dark background from the built-in tomorrow-night and atom-one-dark themes.
     */
    public val fallbackBackgroundColor: Color = Color(0xFF1E1E1E)

    /**
     * Default text color used when the active theme CSS has no `.hljs { color: ... }` rule.
     * Provides readable light-gray text on the dark [fallbackBackgroundColor].
     */
    public val fallbackTextColor: Color = Color(0xFFCCCCCC)

    /**
     * Default copy-to-clipboard button used by [SyntaxHighlightedCode]'s `actions` slot.
     *
     * Renders a vector copy icon inside an [IconButton]. The icon size scales proportionally
     * to the button [size] (60 % of the touch target) so that callers who customize the button
     * size get a matching icon automatically. Tint and size default to values that blend naturally
     * with the code block background when placed inside a [SyntaxHighlightedCode] block.
     *
     * Pass to `actions` to retain the default look while customising other parameters:
     *
     * ```kotlin
     * SyntaxHighlightedCode(
     *     code = snippet,
     *     language = "kotlin",
     *     actions = { onCopy ->
     *         SyntaxHighlightedCodeDefaults.CopyButton(
     *             onClick = onCopy,
     *             contentDescription = stringResource(R.string.copy_code_label),
     *         )
     *     },
     * )
     * ```
     *
     * @param onClick Action invoked when the button is clicked. Wire this to the `onCopy`
     *   parameter received from the `actions` slot.
     * @param modifier Modifier applied to the root [IconButton]. Use this for padding, test tags,
     *   or other positioning/customisation.
     * @param tint Icon color. Defaults to [LocalContentColor] at 70 % opacity, which resolves
     *   correctly when inside a [SyntaxHighlightedCode] block.
     * @param contentDescription Accessibility label for TalkBack and other assistive services.
     *   Provide a localized string for non-English users.
     * @param size Width and height of the button touch target. Defaults to [copyButtonSize].
     */
    @Composable
    public fun CopyButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        tint: Color = LocalContentColor.current.copy(alpha = 0.7f),
        contentDescription: String = "Copy code",
        size: Dp = copyButtonSize,
    ) {
        IconButton(
            onClick = onClick,
            modifier =
                modifier
                    .size(size)
                    .semantics { this.contentDescription = contentDescription },
        ) {
            // Scale the icon proportionally to the button size so that callers who customize
            // `size` get a matching icon without needing to adjust it separately.
            Icon(
                painter = painterResource(R.drawable.copy_code_block),
                contentDescription = null,
                modifier = Modifier.size(size * 0.6f),
                tint = tint,
            )
        }
    }

    /**
     * Default header layout used by [SyntaxHighlightedCode]'s `header` slot: the language label
     * on the left, the trailing actions on the right.
     *
     * Pass to `header` to retain the default layout while customising individual pieces via
     * `languageLabel` and `actions`, or call it directly from a custom `header` slot:
     *
     * ```kotlin
     * SyntaxHighlightedCode(
     *     code = snippet,
     *     language = "kotlin",
     *     header = { onCopy ->
     *         SyntaxHighlightedCodeDefaults.Header(
     *             languageLabel = { SyntaxHighlightedCodeDefaults.LanguageLabel("kotlin") },
     *             actions = { copy -> SyntaxHighlightedCodeDefaults.CopyButton(onClick = copy) },
     *             onCopy = onCopy,
     *             padding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
     *         )
     *     },
     * )
     * ```
     *
     * Nothing is rendered when both [languageLabel] and [actions] are `null`.
     *
     * @param languageLabel Optional composable rendered on the left side of the row.
     * @param actions Optional trailing actions rendered on the right side of the row, laid out
     *   with [RowScope]. Receives the pre-wired copy action to forward to copy buttons.
     * @param onCopy Copy action forwarded to [actions]. Wire this to any copy button's `onClick`.
     * @param modifier Modifier applied to the root [Row]. Use this for background, test tags,
     *   or other positioning/customisation.
     * @param padding Padding applied around the header row. Defaults to [headerPadding].
     */
    @Composable
    public fun Header(
        languageLabel: (@Composable () -> Unit)?,
        actions: (@Composable RowScope.(onCopy: () -> Unit) -> Unit)?,
        onCopy: () -> Unit,
        modifier: Modifier = Modifier,
        padding: PaddingValues = headerPadding,
    ) {
        if (languageLabel != null || actions != null) {
            Row(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .padding(padding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                languageLabel?.invoke()
                Spacer(modifier = Modifier.weight(1f))
                actions?.invoke(this, onCopy)
            }
        }
    }

    /**
     * Default language badge used by [SyntaxHighlightedCode]'s `languageLabel` slot.
     *
     * Renders the language identifier as a dimmed [Text]. Color and size default to values that
     * blend naturally with the code block header when placed inside a [SyntaxHighlightedCode] block.
     *
     * Use this helper when toggling label visibility at runtime so you don't need to reconstruct
     * the full default style:
     *
     * ```kotlin
     * var showLabel by remember { mutableStateOf(true) }
     *
     * SyntaxHighlightedCode(
     *     code = snippet,
     *     language = "kotlin",
     *     languageLabel = if (showLabel) {
     *         { SyntaxHighlightedCodeDefaults.LanguageLabel("kotlin") }
     *     } else null,
     * )
     * ```
     *
     * @param language Text to display (typically the Highlight.js language identifier).
     * @param modifier Modifier applied to the root [Text]. Use this for padding, test tags,
     *   or other positioning/customisation.
     * @param color Label color. Defaults to [LocalContentColor] at 60 % opacity, which resolves
     *   correctly when inside a [SyntaxHighlightedCode] block.
     * @param fontSize Label font size. Defaults to 12 sp.
     */
    @Composable
    public fun LanguageLabel(
        language: String,
        modifier: Modifier = Modifier,
        color: Color = LocalContentColor.current.copy(alpha = 0.6f),
        fontSize: TextUnit = 12.sp,
    ) {
        if (language.isNotBlank()) {
            Text(
                text = language,
                modifier = modifier,
                style =
                    TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = color,
                        fontSize = fontSize,
                    ),
            )
        }
    }
}
