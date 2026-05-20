package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import dev.hossain.highlight.ui.LocalHighlightTheme
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberHighlightEngine
import dev.hossain.highlight.ui.rememberHighlightedCodeBothThemes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val DarkCodeBackground = Color(0xFF1E1E1E)
private val LightCodeBackground = Color(0xFFFAFAFA)
private val DarkCodeText = Color(0xFFCCCCCC)
private val LightCodeText = Color(0xFF333333)

/**
 * Demonstrates [rememberHighlightedCodeBothThemes]: highlights code **once** for both light and
 * dark themes in a single JavaScript call, enabling instant theme switching without re-highlighting.
 *
 * A toggle lets you flip between light and dark to verify the switch is instant once the
 * initial load completes. The duration of the single JS call is shown below the code block.
 *
 * Also demonstrates:
 * - Silent failure detection via [HighlightResult.spanCount] - an unsupported language
 *   produces 0 spans instead of throwing, displayed with an error-coloured card.
 * - Raw [dev.hossain.highlight.engine.HighlightEngine.highlightToHtml] pipeline - shows the
 *   `<span class="hljs-*">` HTML string before any theme colour is applied.
 * - Pre-warming via [dev.hossain.highlight.engine.HighlightEngine.initialize] - measures
 *   WebView warm-up time with a button-triggered call.
 * - Direct [dev.hossain.highlight.engine.HighlightEngine.highlight] call - full pipeline in a
 *   single suspend call, showing [HighlightResult] metrics.
 * - [dev.hossain.highlight.engine.HighlightException] error handling - catches and displays
 *   sealed class subtypes in an error-coloured card.
 *
 * @param lightTheme The light theme from the active theme pair selected via the theme chooser.
 * @param darkTheme The dark theme from the active theme pair selected via the theme chooser.
 * @param isDark Whether the global light/dark toggle (from the top-bar button) is currently dark.
 */
@Composable
internal fun AdvancedEngineSection(
    lightTheme: HighlightTheme,
    darkTheme: HighlightTheme,
    isDark: Boolean,
) {
    var useDark by remember(isDark) { mutableStateOf(isDark) }

    val result by
        rememberHighlightedCodeBothThemes(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            lightTheme = lightTheme,
            darkTheme = darkTheme,
        )

    val displayText = if (useDark) result?.dark else result?.light

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SubSectionHeader("rememberHighlightedCodeBothThemes")
        Text(
            text =
                "Highlights once for both light and dark in a single JS call. " +
                    "Flip the toggle below - switching is instant after the initial load.",
            style = TextStyle(fontSize = 13.sp),
        )

        // Toggle row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = if (useDark) "🌙 Dark" else "☀ Light", style = TextStyle(fontSize = 13.sp))
            Switch(
                checked = useDark,
                onCheckedChange = { useDark = it },
            )
        }

        // Render the highlighted text manually, using a Surface to show the theme's background.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color =
                if (useDark) {
                    darkTheme.backgroundColor.takeIf { it != Color.Unspecified } ?: DarkCodeBackground
                } else {
                    lightTheme.backgroundColor.takeIf { it != Color.Unspecified } ?: LightCodeBackground
                },
        ) {
            val textColor =
                if (useDark) {
                    darkTheme.defaultTextColor.takeIf { it != Color.Unspecified } ?: DarkCodeText
                } else {
                    lightTheme.defaultTextColor.takeIf { it != Color.Unspecified } ?: LightCodeText
                }
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

        Spacer(modifier = Modifier.height(8.dp))

        // Duration metric - read directly from the result state, no separate variable needed
        result?.let { r ->
            Text(
                text = "⏱ Both themes highlighted in ${r.durationMs}ms (single JS call)",
                style =
                    TextStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Silent failure demo
        SubSectionHeader("Silent failure detection via spanCount")
        Text(
            text =
                "Passing an unsupported language produces no tokens (spanCount = 0) instead of " +
                    "throwing an exception. Use spanCount to detect this and warn the caller.",
            style = TextStyle(fontSize = 13.sp),
        )
        var silentFailureResult by remember { mutableStateOf<HighlightResult?>(null) }
        SyntaxHighlightedCode(
            code = "let x = doSomethingCool(42)",
            language = "fakescript",
            modifier = Modifier.fillMaxWidth(),
            onHighlightComplete = { result -> silentFailureResult = result },
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
                        text = "spanCount = ${result.spanCount}" + if (result.spanCount == 0) "  ← no tokens, silent failure" else "",
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
                        text = "durationMs = ${result.durationMs} ms",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Raw highlightToHtml pipeline demo
        SubSectionHeader("Raw pipeline: highlightToHtml()")
        Text(
            text =
                "The lower-level highlightToHtml() returns the raw HTML string with " +
                    "<span class=\"hljs-*\"> tokens before any theme is applied. " +
                    "Useful when you need to process tokens yourself.",
            style = TextStyle(fontSize = 13.sp),
        )
        val rawEngine = rememberHighlightEngine()
        var rawHtml by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            rawEngine.highlightToHtml("val x = 42", "kotlin").onSuccess { rawHtml = it.html }
        }
        rawHtml?.let { html ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = html,
                    modifier = Modifier.padding(12.dp),
                    style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Pre-warming demo
        SubSectionHeader("Pre-warming - engine.initialize()")
        Text(
            text =
                "Calling initialize() warms up the hidden WebView before the first highlight. " +
                    "Useful to reduce latency on the first real highlight request.",
            style = TextStyle(fontSize = 13.sp),
        )
        // Use a dedicated standalone engine (not the shared provider engine) so it starts
        // uninitialized regardless of other highlights already having warmed up the provider's
        // shared WebView. This lets the demo always show real warm-up timing on first tap.
        val initContext = LocalContext.current
        val initEngine = remember { HighlightEngine(initContext.applicationContext) }
        DisposableEffect(Unit) { onDispose { initEngine.destroy() } }
        val scope = rememberCoroutineScope()
        var initStatus by remember { mutableStateOf<String?>(null) }
        Button(onClick = {
            scope.launch {
                val alreadyReady = initEngine.isInitialized.value
                if (alreadyReady) {
                    initStatus = "Already initialized"
                } else {
                    val start = System.nanoTime()
                    val initResult = initEngine.initialize()
                    if (initResult.isSuccess) {
                        // Await bridge.html fully loaded (onPageFinished) for accurate timing
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

        // Direct engine.highlight() call demo
        SubSectionHeader("Direct call - engine.highlight()")
        Text(
            text =
                "Use engine.highlight() directly when you need highlighting outside a composable - " +
                    "for example, in a ViewModel or background coroutine. " +
                    "Returns a HighlightResult with the AnnotatedString, span count, and timing.",
            style = TextStyle(fontSize = 13.sp),
        )
        val directTheme = LocalHighlightTheme.current
        val directEngine = rememberHighlightEngine()
        var directResult by remember { mutableStateOf<HighlightResult?>(null) }
        LaunchedEffect(directTheme) {
            directEngine
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
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "language   = \"${r.language}\"",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                    Text(
                        text = "spanCount  = ${r.spanCount}",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                    Text(
                        text = "durationMs = ${r.durationMs} ms",
                        style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // HighlightException error handling demo
        SubSectionHeader("Error handling - HighlightException")
        Text(
            text =
                "Engine methods return Result<T> and wrap failures in HighlightException subtypes. " +
                    "Tap the button to trigger an error (invalid theme asset) and see the sealed class in action.",
            style = TextStyle(fontSize = 13.sp),
        )
        val context = LocalContext.current
        val errorScope = rememberCoroutineScope()
        var caughtException by remember { mutableStateOf<HighlightException?>(null) }
        Button(onClick = {
            errorScope.launch {
                // Use a theme pointing to a non-existent asset - colorMap access throws IOException,
                // which is wrapped in HighlightException.HtmlParseFailed by the engine.
                val brokenTheme =
                    HighlightTheme.fromAsset(
                        context = context.applicationContext,
                        assetPath = "nonexistent-theme.css",
                        name = "broken",
                    )
                runCatching {
                    // runCatching is needed because theme.colorMap is a lazy property that can
                    // throw an IOException (wrapped in HighlightException) when first accessed
                    // during highlight(). The Result returned by highlight() only covers
                    // failures that occur after colorMap succeeds.
                    directEngine.highlight("val x = 42", "kotlin", brokenTheme)
                }.onFailure { e ->
                    caughtException =
                        when (e) {
                            is HighlightException -> e
                            else -> HighlightException.HtmlParseFailed(e)
                        }
                }
            }
        }) {
            Text("Trigger Engine Error")
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Comparison note
        SubSectionHeader("For comparison - standard SyntaxHighlightedCode (re-highlights on toggle)")
        Text(
            text =
                "The block below uses the global theme from HighlightThemeProvider. " +
                    "Switch the top-bar 🌙/☀ button to see re-highlighting happen.",
            style = TextStyle(fontSize = 13.sp),
        )
        SyntaxHighlightedCode(
            code = KOTLIN_SNIPPET,
            language = "kotlin",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
