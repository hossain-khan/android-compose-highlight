package dev.hossain.highlight.ui.internal

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

/**
 * Sentinel used to detect when the caller did not supply a custom `actions` slot.
 * `null` means "render nothing"; the sentinel means "use the default resolution".
 */
internal val DefaultActionsSentinel: (@Composable RowScope.(onCopy: () -> Unit) -> Unit) = { }

/**
 * Sentinel used to detect when the caller did not supply a custom `languageLabel` slot.
 * `null` means "hide the badge"; the sentinel means "use the default language label".
 */
internal val DefaultLanguageLabelSentinel: (@Composable () -> Unit) = { }

/**
 * Sentinel used to detect when the caller did not supply a custom `header` slot.
 * `null` means "no header chrome"; the sentinel means "use the default header layout".
 */
internal val DefaultHeaderSentinel: (@Composable (onCopy: () -> Unit) -> Unit) = { }

/**
 * Resolves the effective header slot content according to the documented precedence rules,
 * shared by [dev.hossain.highlight.ui.SyntaxHighlightedCode] and
 * [dev.hossain.highlight.ui.StreamingSyntaxHighlightedCode]:
 *
 * 1. `header` explicitly provided - render it alone (custom chrome owns the row).
 * 2. `header` explicitly `null` - render no header at all.
 * 3. `header` unset - render the default header layout, resolving `languageLabel` and `actions`:
 *    - `actions` explicitly provided - render it (including `actions = null` to hide all).
 *    - `actions` unset - render the default copy button.
 */
@Composable
internal fun CodeBlockHeader(
    language: String,
    style: CodeBlockStyle,
    onCopy: () -> Unit,
    languageLabel: (@Composable () -> Unit)?,
    actions: (@Composable RowScope.(onCopy: () -> Unit) -> Unit)?,
    header: (@Composable (onCopy: () -> Unit) -> Unit)?,
) {
    when {
        header === DefaultHeaderSentinel -> {
            val effectiveLanguageLabel: (@Composable () -> Unit)? =
                remember(languageLabel, language) {
                    when {
                        languageLabel === DefaultLanguageLabelSentinel -> {
                            { SyntaxHighlightedCodeDefaults.LanguageLabel(language = language) }
                        }

                        else -> {
                            languageLabel
                        }
                    }
                }

            val effectiveActions: (@Composable RowScope.(onCopy: () -> Unit) -> Unit)? =
                remember(actions, style.copyButtonSize) {
                    when {
                        // actions explicitly provided (including null = hide all trailing actions).
                        actions != null && actions !== DefaultActionsSentinel -> {
                            actions
                        }

                        // actions explicitly null: hide all trailing actions.
                        actions == null -> {
                            null
                        }

                        // actions unset: default copy button.
                        else -> {
                            { onCopy: () -> Unit ->
                                SyntaxHighlightedCodeDefaults.CopyButton(onClick = onCopy, size = style.copyButtonSize)
                            }
                        }
                    }
                }

            SyntaxHighlightedCodeDefaults.Header(
                languageLabel = effectiveLanguageLabel,
                actions = effectiveActions,
                onCopy = onCopy,
                padding = style.headerPadding,
            )
        }

        header != null -> {
            header(onCopy)
        }
    }
}
