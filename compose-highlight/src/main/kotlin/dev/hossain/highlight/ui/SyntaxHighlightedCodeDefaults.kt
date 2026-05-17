package dev.hossain.highlight.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
 * // Customise the copy button slot while keeping the default icon
 * SyntaxHighlightedCode(
 *     code = snippet,
 *     language = "kotlin",
 *     copyButtonContent = { onClick ->
 *         SyntaxHighlightedCodeDefaults.CopyButton(
 *             onClick = onClick,
 *             contentDescription = stringResource(R.string.copy_code),
 *         )
 *     },
 * )
 * ```
 */
object SyntaxHighlightedCodeDefaults {
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
    val codeTextStyle: TextStyle =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )

    /** Default corner radius for the code block container. */
    val shape: Shape = RoundedCornerShape(8.dp)

    /** Default inner padding for the code content area. */
    val padding: PaddingValues = PaddingValues(16.dp)

    /** Default padding for the header row (language label + copy button). */
    val headerPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

    /** Default width reserved for the line-number gutter. */
    val lineNumberWidth: Dp = 32.dp

    /** Default size (width and height) of the copy button. */
    val copyButtonSize: Dp = 32.dp

    /**
     * Default copy-to-clipboard button used by [SyntaxHighlightedCode]'s `copyButtonContent` slot.
     *
     * Renders a simple `⧉` text icon inside an [IconButton]. Tint and size default to values
     * that blend naturally with the code block background when placed inside a
     * [SyntaxHighlightedCode] block.
     *
     * Pass to `copyButtonContent` to retain the default look while customising other parameters:
     *
     * ```kotlin
     * SyntaxHighlightedCode(
     *     code = snippet,
     *     language = "kotlin",
     *     copyButtonContent = { onClick ->
     *         SyntaxHighlightedCodeDefaults.CopyButton(
     *             onClick = onClick,
     *             contentDescription = stringResource(R.string.copy_code_label),
     *         )
     *     },
     * )
     * ```
     *
     * @param onClick Action invoked when the button is clicked. Wire this to the `onClick`
     *   parameter received from the `copyButtonContent` slot.
     * @param tint Icon color. Defaults to [LocalContentColor] at 70 % opacity, which resolves
     *   correctly when inside a [SyntaxHighlightedCode] block.
     * @param contentDescription Accessibility label for TalkBack and other assistive services.
     *   Provide a localized string for non-English users.
     * @param size Width and height of the button touch target. Defaults to [copyButtonSize].
     */
    @Composable
    fun CopyButton(
        onClick: () -> Unit,
        tint: Color = LocalContentColor.current.copy(alpha = 0.7f),
        contentDescription: String = "Copy code",
        size: Dp = copyButtonSize,
    ) {
        IconButton(
            onClick = onClick,
            modifier =
                Modifier
                    .size(size)
                    .semantics { this.contentDescription = contentDescription },
        ) {
            Text(
                text = "⧉",
                style = TextStyle(color = tint, fontSize = 16.sp),
            )
        }
    }

    /**
     * Default language badge used by [SyntaxHighlightedCode]'s `languageLabelContent` slot.
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
     *     languageLabelContent = if (showLabel) {
     *         { SyntaxHighlightedCodeDefaults.LanguageLabel("kotlin") }
     *     } else null,
     * )
     * ```
     *
     * @param language Text to display (typically the Highlight.js language identifier).
     * @param color Label color. Defaults to [LocalContentColor] at 60 % opacity, which resolves
     *   correctly when inside a [SyntaxHighlightedCode] block.
     * @param fontSize Label font size. Defaults to 12 sp.
     */
    @Composable
    fun LanguageLabel(
        language: String,
        color: Color = LocalContentColor.current.copy(alpha = 0.6f),
        fontSize: TextUnit = 12.sp,
    ) {
        if (language.isNotBlank()) {
            Text(
                text = language,
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
