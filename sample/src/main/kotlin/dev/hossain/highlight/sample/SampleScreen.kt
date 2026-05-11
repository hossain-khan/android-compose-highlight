package dev.hossain.highlight.sample

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.engine.HighlightTheme
import dev.hossain.highlight.sample.perf.PerfActivity
import dev.hossain.highlight.sample.sections.AdvancedEngineSection
import dev.hossain.highlight.sample.sections.CallbacksSection
import dev.hossain.highlight.sample.sections.EngineInfoSection
import dev.hossain.highlight.sample.sections.SectionHeader
import dev.hossain.highlight.sample.sections.StylingSection
import dev.hossain.highlight.sample.sections.ThemeCreationSection
import dev.hossain.highlight.sample.sections.TogglesSection
import dev.hossain.highlight.sample.sections.TypographySection
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main demo screen that renders a scrollable list of syntax-highlighted code snippets.
 *
 * Uses [HighlightThemeProvider] to supply the active theme to all [SyntaxHighlightedCode]
 * composables in the tree. The top bar provides two controls:
 * - **Theme picker** (🎨): cycles between GitHub (custom asset-based), Tomorrow, and Atom One
 *   theme families, demonstrating both built-in and user-provided themes.
 * - **Light/Dark toggle**: switches between the light and dark variant of the selected theme.
 *
 * The GitHub themes are loaded from the sample app's own assets via [HighlightTheme.fromAsset],
 * showcasing that library users can bundle any Highlight.js CSS and use it as a theme — they are
 * not limited to the built-in options.
 *
 * Sections are organized into tabs:
 * - **Languages**: highlights SAMPLES across different languages (original demo).
 * - **Styling**: demonstrates [CodeBlockStyle] variants and custom style parameters.
 * - **Typography**: shows [SyntaxHighlightedCode] typography customization via [CodeBlockStyle.textStyle].
 * - **Toggles**: shows all boolean flag combinations (line numbers, language label, copy button).
 * - **Callbacks**: demonstrates `onHighlightComplete` and `onCopyClick` callbacks of [SyntaxHighlightedCode].
 * - **Themes**: exercises every [HighlightTheme] factory method.
 * - **Advanced**: shows [rememberHighlightedCodeBothThemes] for instant theme switching.
 * - **Engine**: shows [HighlightEngine.highlightJsVersion] and [HighlightEngine.supportedLanguages].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleScreen() {
    val context = LocalContext.current
    val codeSamples by produceState(initialValue = emptyList<CodeSample>(), context) {
        value = withContext(Dispatchers.IO) { loadCodeSamples(context) }
    }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isDark by remember { mutableStateOf(true) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf<DemoTab>(DemoTab.Languages) }

    // Shared copy handler: copies to clipboard and shows a snackbar confirmation.
    val onCopyClick: (String) -> Unit = { code ->
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("code", code)))
            snackbarHostState.showSnackbar("Successfully copied source code to clipboard")
        }
    }

    // All available theme pairs — GitHub uses fromAsset() to demonstrate custom themes.
    val themePairs =
        remember(context) {
            listOf(
                ThemePair(
                    name = "GitHub",
                    light = HighlightTheme.fromAsset(context, "themes/github.css", "github"),
                    dark = HighlightTheme.fromAsset(context, "themes/github-dark.css", "github-dark"),
                ),
                ThemePair(
                    name = "Tomorrow",
                    light = HighlightTheme.tomorrow(context),
                    dark = HighlightTheme.tomorrowNight(context),
                ),
                ThemePair(
                    name = "Atom One",
                    light = HighlightTheme.atomOneLight(context),
                    dark = HighlightTheme.atomOneDark(context),
                ),
            )
        }

    var selectedThemeIndex by remember { mutableIntStateOf(2) } // Atom One
    val activePair = themePairs[selectedThemeIndex]

    HighlightThemeProvider(
        lightHighlightTheme = activePair.light,
        darkHighlightTheme = activePair.dark,
        darkTheme = isDark,
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Highlight Demo") },
                    actions = {
                        // Performance benchmark screen
                        IconButton(onClick = {
                            context.startActivity(Intent(context, PerfActivity::class.java))
                        }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.speed_24dp),
                                contentDescription = "Performance benchmark",
                            )
                        }
                        // Theme family picker
                        Box {
                            IconButton(onClick = { showThemeMenu = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.palette_24dp),
                                    contentDescription = "Select theme: ${activePair.name}",
                                )
                            }
                            DropdownMenu(
                                expanded = showThemeMenu,
                                onDismissRequest = { showThemeMenu = false },
                            ) {
                                themePairs.forEachIndexed { index, pair ->
                                    DropdownMenuItem(
                                        text = { Text(pair.name) },
                                        onClick = {
                                            selectedThemeIndex = index
                                            showThemeMenu = false
                                        },
                                    )
                                }
                            }
                        }
                        // Light/dark variant toggle
                        IconButton(
                            onClick = { isDark = !isDark },
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Icon(
                                imageVector =
                                    ImageVector.vectorResource(
                                        if (isDark) R.drawable.light_mode_24dp else R.drawable.mode_night_24dp,
                                    ),
                                contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                        .consumeWindowInsets(innerPadding),
            ) {
                val tabs = DemoTab.all
                val selectedTabIndex = tabs.indexOf(activeTab)
                PrimaryScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            text = { Text(tab.title) },
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (activeTab) {
                        DemoTab.Languages -> {
                            codeSamples.forEach { sample ->
                                item(key = sample.displayLabel) {
                                    SectionHeader(sample.displayLabel)
                                    SyntaxHighlightedCode(
                                        code = sample.code,
                                        language = sample.language,
                                        modifier = Modifier.fillMaxWidth(),
                                        showLineNumbers = sample.language == "python",
                                        onCopyClick = onCopyClick,
                                        copyButtonIcon = { tint ->
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.content_copy_24dp),
                                                modifier = Modifier.size(16.dp),
                                                contentDescription = "Copy code",
                                                tint = tint,
                                            )
                                        },
                                    )
                                }
                            }
                        }

                        DemoTab.Styling -> {
                            item { StylingSection() }
                        }

                        DemoTab.Typography -> {
                            item { TypographySection() }
                        }

                        DemoTab.Toggles -> {
                            item { TogglesSection() }
                        }

                        DemoTab.Callbacks -> {
                            item { CallbacksSection() }
                        }

                        DemoTab.Themes -> {
                            item { ThemeCreationSection() }
                        }

                        DemoTab.Advanced -> {
                            item { AdvancedEngineSection(isDark = isDark) }
                        }

                        DemoTab.Engine -> {
                            item { EngineInfoSection() }
                        }
                    }
                }
            }
        }
    }
}
