package dev.hossain.highlight.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.hossain.highlight.engine.HighlightEngine
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.ui.internal.LocalHighlightEngine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Robolectric tests verifying that highlighting effects and state in [rememberHighlightedCode],
 * [rememberHighlightedCodeBothThemes], and [rememberSyntaxHighlightedEditorValue] properly
 * restart when the underlying [HighlightEngine] instance changes (e.g. when provider ownership
 * is swapped or recreated), and do not trigger redundant work on normal parent recompositions (#490).
 */
@OptIn(ExperimentalHighlightApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class RememberHighlightedCodeEngineKeyTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val theme = HighlightTheme.fromCss("", "test-theme")

    @Test
    fun `rememberHighlightedCode restarts on new engine when LocalHighlightEngine is swapped`() {
        var engine1: HighlightEngine? = null
        var engine2: HighlightEngine? = null
        var activeEngine by mutableStateOf<HighlightEngine?>(null)

        composeTestRule.setContent {
            val context = LocalContext.current
            val e1 = remember { HighlightEngine(context.applicationContext).also { engine1 = it } }
            val e2 = remember { HighlightEngine(context.applicationContext).also { engine2 = it } }

            val currentEngine = activeEngine ?: e1
            CompositionLocalProvider(LocalHighlightEngine provides currentEngine) {
                rememberHighlightedCode(
                    code = "val a = 1",
                    language = "kotlin",
                    theme = theme,
                )
            }
        }
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val firstEngine = requireNotNull(engine1)
        val secondEngine = requireNotNull(engine2)

        // Initially, the first engine is active and has initialized its WebView.
        assertThat(firstEngine.webViewForTest()).isNotNull()
        assertThat(secondEngine.webViewForTest()).isNull()

        // Swap to the second engine.
        activeEngine = secondEngine
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // The second engine must now be initialized because the effect restarted against it.
        assertThat(secondEngine.webViewForTest()).isNotNull()

        firstEngine.destroy()
        secondEngine.destroy()
    }

    @Test
    fun `rememberHighlightedCodeBothThemes restarts on new engine when LocalHighlightEngine is swapped`() {
        var engine1: HighlightEngine? = null
        var engine2: HighlightEngine? = null
        var activeEngine by mutableStateOf<HighlightEngine?>(null)

        composeTestRule.setContent {
            val context = LocalContext.current
            val e1 = remember { HighlightEngine(context.applicationContext).also { engine1 = it } }
            val e2 = remember { HighlightEngine(context.applicationContext).also { engine2 = it } }

            val currentEngine = activeEngine ?: e1
            CompositionLocalProvider(
                LocalHighlightEngine provides currentEngine,
                LocalLightHighlightTheme provides theme,
                LocalDarkHighlightTheme provides theme,
            ) {
                rememberHighlightedCodeBothThemes(
                    code = "val b = 2",
                    language = "kotlin",
                    lightTheme = theme,
                    darkTheme = theme,
                )
            }
        }
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val firstEngine = requireNotNull(engine1)
        val secondEngine = requireNotNull(engine2)

        assertThat(firstEngine.webViewForTest()).isNotNull()
        assertThat(secondEngine.webViewForTest()).isNull()

        // Swap to the second engine.
        activeEngine = secondEngine
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // The second engine must now have its WebView initialized.
        assertThat(secondEngine.webViewForTest()).isNotNull()

        firstEngine.destroy()
        secondEngine.destroy()
    }

    @Test
    fun `rememberSyntaxHighlightedEditorValue restarts on new engine when LocalHighlightEngine is swapped`() {
        var engine1: HighlightEngine? = null
        var engine2: HighlightEngine? = null
        var activeEngine by mutableStateOf<HighlightEngine?>(null)

        composeTestRule.setContent {
            val context = LocalContext.current
            val e1 = remember { HighlightEngine(context.applicationContext).also { engine1 = it } }
            val e2 = remember { HighlightEngine(context.applicationContext).also { engine2 = it } }

            val currentEngine = activeEngine ?: e1
            CompositionLocalProvider(LocalHighlightEngine provides currentEngine) {
                rememberSyntaxHighlightedEditorValue(
                    value = TextFieldValue("val c = 3"),
                    language = "kotlin",
                    theme = theme,
                    debounceMs = 0L,
                )
            }
        }
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val firstEngine = requireNotNull(engine1)
        val secondEngine = requireNotNull(engine2)

        assertThat(firstEngine.webViewForTest()).isNotNull()
        assertThat(secondEngine.webViewForTest()).isNull()

        // Swap to the second engine.
        activeEngine = secondEngine
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // The second engine must now have its WebView initialized.
        assertThat(secondEngine.webViewForTest()).isNotNull()

        firstEngine.destroy()
        secondEngine.destroy()
    }

    @Test
    fun `normal parent recomposition does not restart highlighting when engine is unchanged`() {
        var engine: HighlightEngine? = null
        var recomposeTrigger by mutableStateOf(0)

        composeTestRule.setContent {
            val context = LocalContext.current
            val e = remember { HighlightEngine(context.applicationContext).also { engine = it } }

            @Suppress("UNUSED_VARIABLE")
            val readTrigger = recomposeTrigger

            CompositionLocalProvider(LocalHighlightEngine provides e) {
                rememberHighlightedCode(
                    code = "val unchanged = 42",
                    language = "kotlin",
                    theme = theme,
                )
            }
        }
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val activeEngine = requireNotNull(engine)
        val webView = requireNotNull(activeEngine.webViewForTest())

        // Complete page load and capture evaluateJavascript callback count.
        val webViewClient = Shadows.shadowOf(webView).getWebViewClient()
        val lastUrl = Shadows.shadowOf(webView).getLastLoadedUrl() ?: ""
        webViewClient?.onPageFinished(webView, lastUrl)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val initialCallback = Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
        assertThat(initialCallback).isNotNull()

        // Trigger unrelated parent recomposition without changing engine, code, or theme.
        recomposeTrigger++
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // The callback should still be the same instance because LaunchedEffect did NOT restart.
        val postRecomposeCallback = Shadows.shadowOf(webView).getLastEvaluatedJavascriptCallback()
        assertThat(postRecomposeCallback).isSameInstanceAs(initialCallback)

        activeEngine.destroy()
    }

    @Test
    fun `adding and removing HighlightThemeProvider switches between standalone and shared engines`() {
        var sharedEngine: HighlightEngine? = null
        var hasProvider by mutableStateOf(false)

        composeTestRule.setContent {
            val context = LocalContext.current
            val shared = remember { HighlightEngine(context.applicationContext).also { sharedEngine = it } }

            if (hasProvider) {
                CompositionLocalProvider(LocalHighlightEngine provides shared) {
                    rememberHighlightedCode(
                        code = "val transition = 100",
                        language = "kotlin",
                        theme = theme,
                    )
                }
            } else {
                rememberHighlightedCode(
                    code = "val transition = 100",
                    language = "kotlin",
                    theme = theme,
                )
            }
        }
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val shared = requireNotNull(sharedEngine)
        // In the initial pass (without provider), standalone engine is created; shared is untouched.
        assertThat(shared.webViewForTest()).isNull()

        // Transition: add provider (shared engine should now be invoked).
        hasProvider = true
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        val sharedWebView = requireNotNull(shared.webViewForTest())
        val webViewClient = Shadows.shadowOf(sharedWebView).getWebViewClient()
        val lastUrl = Shadows.shadowOf(sharedWebView).getLastLoadedUrl() ?: ""
        webViewClient?.onPageFinished(sharedWebView, lastUrl)
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // Transition: remove provider (shared engine is no longer in scope; switches to standalone engine).
        hasProvider = false
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        shared.destroy()
    }
}
