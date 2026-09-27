package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.sample.PYTHON_SNIPPET
import dev.hossain.highlight.sample.R
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults

/**
 * Demonstrates every [SyntaxHighlightedCode] visibility option:
 * - `languageLabel` - default, null (hidden), and rich custom slot
 * - `actions` - default, null (hidden), multiple trailing buttons, and custom vector icon
 * - `header` - custom chrome and null (no header)
 * - `showLineNumbers` × `languageLabel` (2×2)
 *
 * @param onActionMessage Receives a snackbar message when a custom action button (edit, share) is tapped.
 */
@Composable
internal fun TogglesSection(onActionMessage: (String) -> Unit = {}) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubSectionHeader("languageLabel - rich custom label")
        androidx.compose.material3.Text(
            text = "The slot accepts any @Composable - here is an example with a custom label, icon, and metadata badge.",
            style =
                androidx.compose.ui.text
                    .TextStyle(fontSize = 13.sp),
        )
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            languageLabel = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    androidx.compose.material3.Text(
                        text = "Kotlin",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.stars_2_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    androidx.compose.material3.Text(
                        text = "42 likes",
                        fontSize = 12.sp,
                        color = LocalContentColor.current.copy(alpha = 0.6f),
                    )
                }
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("actions - custom vector icon")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            actions = { onCopy ->
                androidx.compose.material3.IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.copy_content_alt_rounded),
                        modifier = Modifier.size(16.dp),
                        contentDescription = "Copy code",
                    )
                }
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("actions - custom icon with large size (56.dp)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            actions = { onCopy ->
                androidx.compose.material3.IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.copy_content_alt_rounded),
                        modifier = Modifier.size(32.dp),
                        contentDescription = "Copy code",
                    )
                }
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("showLineNumbers=false, languageLabel=default")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = false,
        )

        SubSectionHeader("showLineNumbers=true, languageLabel=default")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = true,
        )

        SubSectionHeader("showLineNumbers=false, languageLabel=null (hidden)")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = false,
            languageLabel = null,
        )

        SubSectionHeader("showLineNumbers=true, languageLabel=null (hidden)")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            showLineNumbers = true,
            languageLabel = null,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("actions - multiple trailing buttons (edit, share, copy)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            actions = { onCopy ->
                androidx.compose.material3.IconButton(onClick = { onActionMessage("Edit action clicked") }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.edit_square_24dp),
                        modifier = Modifier.size(16.dp),
                        contentDescription = "Edit",
                    )
                }
                androidx.compose.material3.IconButton(onClick = { onActionMessage("Share action clicked") }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.share_24dp),
                        modifier = Modifier.size(16.dp),
                        contentDescription = "Share",
                    )
                }
                SyntaxHighlightedCodeDefaults.CopyButton(onClick = onCopy)
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("header - custom chrome (file titlebar)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            header = { onCopy ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.stars_2_24dp),
                        modifier = Modifier.size(14.dp),
                        contentDescription = null,
                    )
                    androidx.compose.material3.Text(
                        text = "MainActivity.kt",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    androidx.compose.material3.IconButton(
                        onClick = { onActionMessage("Edit action clicked") },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.edit_square_24dp),
                            modifier = Modifier.size(14.dp),
                            contentDescription = "Edit",
                        )
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { onActionMessage("Share action clicked") },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.share_24dp),
                            modifier = Modifier.size(14.dp),
                            contentDescription = "Share",
                        )
                    }
                    SyntaxHighlightedCodeDefaults.CopyButton(onClick = onCopy)
                }
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("header=null (no header chrome)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            header = null,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("copy button=default")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
        )

        SubSectionHeader("actions=null (copy button hidden)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            actions = null,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("languageLabel=null + actions=null (header row hidden)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            languageLabel = null,
            actions = null,
        )
    }
}
