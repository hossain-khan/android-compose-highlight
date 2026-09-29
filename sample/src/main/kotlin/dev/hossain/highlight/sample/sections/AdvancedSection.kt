package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightException
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.sample.KOTLIN_SNIPPET
import dev.hossain.highlight.sample.PYTHON_SNIPPET
import dev.hossain.highlight.ui.LocalHighlightTheme
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberHighlightEngine
import dev.hossain.highlight.ui.rememberHighlightedCode
import dev.hossain.highlight.ui.rememberHighlightedCodeBothThemes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val DarkCodeBackground = Color(0xFF1E1E1E)
private val LightCodeBackground = Color(0xFFFAFAFA)
private val DarkCodeText = Color(0xFFCCCCCC)
private val LightCodeText = Color(0xFF333333)

/**
 * Consolidated advanced engine features and integration patterns:
 * 1. Dual-theme caching via [rememberHighlightedCodeBothThemes].
 * 2. Custom Compose integrations with bare [Text] and [rememberHighlightedCode].
 * 3. Engine pre-warming and lifecycle management.
 * 4. Direct suspend engine calls outside composables.
 * 5. Event callbacks and silent failure detection.
 * 6. Placeholder slots during asynchronous load.
 * 7. Raw HTML token pipeline and error handling with [HighlightException].
 */
@Composable
internal fun AdvancedSection(
    lightTheme: HighlightTheme,
    darkTheme: HighlightTheme,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text =
                "Deep dive into advanced engine capabilities, custom Compose integrations, " +
                    "lifecycle hooks, and low-level engine APIs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── 1. Dual-Theme Caching ───────────────────────────────────────────
        SubSectionHeader("1. Instant Theme Switching (rememberHighlightedCodeBothThemes)")
        Text(
            text =
                "Highlights once for both light and dark in a single JavaScript pass. " +
                    "Theme switching is instant without re-querying the WebView bridge.",
            style = TextStyle(fontSize = 13.sp),
        )

        var useDarkByToggle by remember(isDark) { mutableStateOf(isDark) }
        val dualResult by
            rememberHighlightedCodeBothThemes(
                code = KOTLIN_SNIPPET,
                language = "kotlin",
                lightTheme = lightTheme,
                darkTheme = darkTheme,
            )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = if (useDarkByToggle) "🌙 Dark" else "☀ Light", style = TextStyle(fontSize = 13.sp))
            Switch(
                checked = useDarkByToggle,
                onCheckedChange = { useDarkByToggle = it },
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color =
                if (useDarkByToggle) {
                    darkTheme.backgroundColor.takeIf { it != Color.Unspecified } ?: DarkCodeBackground
                } else {
                    lightTheme.backgroundColor.takeIf { it != Color.Unspecified } ?: LightCodeBackground
                },
        ) {
            val textColor =
                if (useDarkByToggle) {
                    darkTheme.defaultTextColor.takeIf { it != Color.Unspecified } ?: DarkCodeText
                } else {
                    lightTheme.defaultTextColor.takeIf { it != Color.Unspecified } ?: LightCodeText
                }
            val displayText = if (useDarkByToggle) dualResult?.dark else dualResult?.light
            Text(
                text = displayText ?: AnnotatedString(KOTLIN_SNIPPET),
                modifier = Modifier.padding(16.dp),
                style =
                    TextStyle(
                        color = textColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                    ),
            )
        }

        dualResult?.let { r ->
            Text(
                text = "⏱ Both themes generated in ${r.durationMs}ms (single JS bridge call)",
                style =
                    TextStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── 2. Bare Compose Text() with rememberHighlightedCode ──────────────
        SubSectionHeader("2. Custom UI via Bare Text() & rememberHighlightedCode")
        Text(
            text =
                "SyntaxHighlightedCode is optional. For custom cards, speech bubbles, or custom layout, " +
                    "use rememberHighlightedCode() directly and pass the AnnotatedString to standard Compose Text().",
            style = TextStyle(fontSize = 13.sp),
        )

        val bareSnippet =
            """
            // Minimal highlighted Compose Text demo
            val highlightedCode by rememberHighlightedCode(code, "kotlin")
            Text(text = highlightedCode ?: AnnotatedString(code))
            """.trimIndent()

        val bareHighlightResult by rememberHighlightedCode(bareSnippet, "kotlin")

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Rendered inside standard OutlinedCard using bare Text():",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = bareHighlightResult ?: AnnotatedString(bareSnippet),
                    style =
                        TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        ),
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── 3. Engine Pre-Warming & Warm-up State ─────────────────────────────
        SubSectionHeader("3. Engine Pre-Warming & Lifecycle")
        Text(
            text =
                "Calling initialize() warms up the hidden WebView before user interaction. " +
                    "Observe isInitialized StateFlow as the engine warms up.",
            style = TextStyle(fontSize = 13.sp),
        )

        val initContext = LocalContext.current
        val initEngine = remember { HighlightEngine(initContext.applicationContext) }
        DisposableEffect(Unit) { onDispose { initEngine.destroy() } }
        val isInitializedState by initEngine.isInitialized.collectAsState()
        var initStatus by remember { mutableStateOf<String?>(null) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = {
                scope.launch {
                    val alreadyReady = initEngine.isInitialized.value
                    if (alreadyReady) {
                        initStatus = "Already initialized"
                    } else {
                        val start = System.nanoTime()
                        val initResult = initEngine.initialize()
                        if (initResult.isSuccess) {
                            initEngine.isInitialized.first { it }
                        }
                        val elapsedMs = (System.nanoTime() - start) / 1_000_000L
                        initStatus =
                            if (initResult.isSuccess) {
                                "WebView ready in ${elapsedMs}ms"
                            } else {
                                "Init failed: ${initResult.exceptionOrNull()?.message}"
                            }
                    }
                }
            }) {
                Text("Initialize Engine")
            }

            Text(
                text = "isInitialized = $isInitializedState",
                style =
                    TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (isInitializedState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    ),
            )
        }

        initStatus?.let { status ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(12.dp),
                    style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── 4. Direct Suspend Engine Calls ──────────────────────────────────
        SubSectionHeader("4. Direct Coroutine Pipeline (engine.highlight)")
        Text(
            text =
                "engine.highlight() can be called from ViewModels, background jobs, or repositories " +
                    "without requiring any Compose lifecycle context.",
            style = TextStyle(fontSize = 13.sp),
        )

        val directTheme = LocalHighlightTheme.current
        val sharedEngine = rememberHighlightEngine()
        var directResult by remember { mutableStateOf<HighlightResult?>(null) }
        LaunchedEffect(directTheme) {
            sharedEngine
                .highlight(KOTLIN_SNIPPET, "kotlin", directTheme)
                .onSuccess { directResult = it }
        }

        directResult?.let { r ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = directTheme.backgroundColor.takeIf { it != Color.Unspecified } ?: DarkCodeBackground,
            ) {
                Text(
                    text = r.annotated,
                    modifier = Modifier.padding(16.dp),
                    style =
                        TextStyle(
                            color = directTheme.defaultTextColor.takeIf { it != Color.Unspecified } ?: DarkCodeText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        ),
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "language = \"${r.language}\"  |  tokens = ${r.spanCount}  |  latency = ${r.durationMs}ms",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── 5. Event Callbacks & Silent Failure ──────────────────────────────
        SubSectionHeader("5. Callbacks & Silent Failure Detection")
        Text(
            text =
                "onHighlightComplete reports duration and tokens. " +
                    "Unsupported languages produce spanCount = 0 instead of throwing; detect this to warn users.",
            style = TextStyle(fontSize = 13.sp),
        )

        var silentFailureResult by remember { mutableStateOf<HighlightResult?>(null) }
        var customCopyMessage by remember { mutableStateOf("") }

        SyntaxHighlightedCode(
            code = "let x = doSomethingCool(42)",
            language = "fakescript",
            modifier = Modifier.fillMaxWidth(),
            onHighlightComplete = { result -> silentFailureResult = result },
            onCopyClick = { code ->
                customCopyMessage = "Custom handler intercepted copy of ${code.length} characters"
            },
        )

        silentFailureResult?.let { result ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color =
                    if (result.spanCount == 0) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                shape = RoundedCornerShape(6.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "language  = \"${result.language}\"",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                    Text(
                        text =
                            "spanCount = ${result.spanCount}" +
                                if (result.spanCount == 0) " (silent failure - language not recognized)" else "",
                        style =
                            TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color =
                                    if (result.spanCount == 0) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            ),
                    )
                    Text(
                        text = "duration  = ${result.durationMs}ms",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }

        if (customCopyMessage.isNotEmpty()) {
            Text(
                text = customCopyMessage,
                style = TextStyle(color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── 6. Placeholders ──────────────────────────────────────────────────
        SubSectionHeader("6. Placeholder Slots During Async Load")
        Text(
            text = "Render custom views (dimmed raw text or loading labels) while highlight computation runs.",
            style = TextStyle(fontSize = 13.sp),
        )

        SyntaxHighlightedCode(
            code = PYTHON_SNIPPET,
            language = "python",
            modifier = Modifier.fillMaxWidth(),
            placeholder = { rawCode ->
                Text(
                    text = rawCode,
                    style =
                        TextStyle(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        ),
                )
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── 7. Raw Tokens Pipeline & Error Handling ──────────────────────────
        SubSectionHeader("7. Raw highlightToHtml() & Error Handling")
        Text(
            text =
                "highlightToHtml() exposes the raw HTML token string. " +
                    "All engine errors return Result.failure wrapping HighlightException subtypes.",
            style = TextStyle(fontSize = 13.sp),
        )

        var rawHtml by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            sharedEngine.highlightToHtml("val x = 42", "kotlin").onSuccess { rawHtml = it.html }
        }

        rawHtml?.let { html ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = "Raw HTML: $html",
                    modifier = Modifier.padding(12.dp),
                    style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                )
            }
        }

        var caughtException by remember { mutableStateOf<HighlightException?>(null) }
        Button(onClick = {
            scope.launch {
                val brokenTheme =
                    HighlightTheme.fromAsset(
                        context = context.applicationContext,
                        assetPath = "nonexistent-theme.css",
                        name = "broken",
                    )
                sharedEngine
                    .highlight("val x = 42", "kotlin", brokenTheme)
                    .onFailure { e ->
                        caughtException =
                            when (e) {
                                is HighlightException -> e
                                else -> HighlightException.HtmlParseFailed(e)
                            }
                    }
            }
        }) {
            Text("Trigger Engine Error (Invalid Asset)")
        }

        caughtException?.let { ex ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(6.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "type    = ${ex::class.simpleName}",
                        style =
                            TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                    )
                    Text(
                        text = "message = ${ex.message}",
                        style =
                            TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                    )
                }
            }
        }
    }
}
