package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
 * - `copyButton` - default, null (hidden), and custom vector icon
 * - `showLineNumbers` x `languageLabel` (2x2)
 */
@Composable
internal fun TogglesSection() {
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

        SubSectionHeader("copyButton - custom vector icon")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            copyButton = { onClick ->
                androidx.compose.material3.IconButton(onClick = onClick) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.content_copy_24dp),
                        modifier = Modifier.size(16.dp),
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

        SubSectionHeader("copyButton=default")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
        )

        SubSectionHeader("copyButton=null (hidden)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            copyButton = null,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        SubSectionHeader("languageLabel=null + copyButton=null (header row hidden)")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            languageLabel = null,
            copyButton = null,
        )
    }
}
