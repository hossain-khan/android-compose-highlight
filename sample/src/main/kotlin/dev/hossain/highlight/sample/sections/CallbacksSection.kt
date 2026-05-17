package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.sample.PYTHON_SNIPPET
import dev.hossain.highlight.sample.R
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberHighlightEngine

/**
 * Demonstrates [SyntaxHighlightedCode] event callbacks:
 * - `onHighlightComplete` — shows full [HighlightResult] (duration, span count, language).
 * - `onCopyClick` — custom copy handler with inline feedback.
 * - `rememberHighlightEngine().isInitialized` — shows engine warm-up state.
 */
@Composable
internal fun CallbacksSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // onHighlightComplete: show full HighlightResult fields
        var highlightResult by remember { mutableStateOf<HighlightResult?>(null) }

        SubSectionHeader("onHighlightComplete — full HighlightResult")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            onHighlightComplete = { result -> highlightResult = result },
        )
        highlightResult?.let { result ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "language  = \"${result.language}\"",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                    Text(
                        text = "durationMs = ${result.durationMs} ms",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                    val spanLabel = if (result.spanCount == 0) "${result.spanCount}  ⚠ silent failure" else "${result.spanCount}"
                    Text(
                        text = "spanCount  = $spanLabel",
                        style =
                            TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color =
                                    if (result.spanCount == 0) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            ),
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // isInitialized demo
        val engine = rememberHighlightEngine()
        val engineInitialized by engine.isInitialized.collectAsState()

        SubSectionHeader("HighlightEngine.isInitialized")
        Text(
            text = "Highlight a block to warm up the engine, then observe the flag flip.",
            style = TextStyle(fontSize = 13.sp),
        )
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "engine.isInitialized = $engineInitialized",
            style =
                TextStyle(
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (engineInitialized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
            modifier = Modifier.padding(top = 4.dp),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // onCopyClick custom handler demo
        var customCopyMessage by remember { mutableStateOf("") }

        SubSectionHeader("onCopyClick — custom copy handler")
        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            onCopyClick = { code ->
                customCopyMessage = "📋 Custom handler received ${code.length} chars — not sent to clipboard!"
            },
        )
        if (customCopyMessage.isNotEmpty()) {
            Text(
                text = customCopyMessage,
                style =
                    TextStyle(
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                    ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // languageLabelContent rich custom slot demo
        SubSectionHeader("languageLabelContent — rich custom label")
        Text(
            text = "The slot accepts any @Composable — icon, bold text, metadata badges, like counts, etc.",
            style = TextStyle(fontSize = 13.sp),
        )
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            languageLabelContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Kotlin",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.star_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "42 likes",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // copyButtonContent custom slot demo
        SubSectionHeader("copyButtonContent — custom vector icon")
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
            copyButtonContent = { onClick ->
                androidx.compose.material3.IconButton(onClick = onClick) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.content_copy_24dp),
                        modifier = Modifier.size(16.dp),
                        contentDescription = "Copy code",
                    )
                }
            },
        )
    }
}
