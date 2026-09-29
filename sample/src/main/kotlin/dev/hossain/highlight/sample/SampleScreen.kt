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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hossain.highlight.sample.info.InfoBanner
import dev.hossain.highlight.sample.perf.PerfActivity
import dev.hossain.highlight.sample.sections.AdvancedSection
import dev.hossain.highlight.sample.sections.LanguageCatalogSection
import dev.hossain.highlight.sample.sections.LargeFileSection
import dev.hossain.highlight.sample.sections.LiveEditorSection
import dev.hossain.highlight.sample.sections.ReadmeQuickStartSection
import dev.hossain.highlight.sample.sections.SectionHeader
import dev.hossain.highlight.sample.sections.StreamingSection
import dev.hossain.highlight.sample.sections.StylingSection
import dev.hossain.highlight.sample.sections.ThemeCreationSection
import dev.hossain.highlight.sample.sections.ThemeGallerySection
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults
import kotlinx.coroutines.launch

/**
 * Main demo screen that renders a scrollable list of syntax-highlighted code snippets.
 *
 * Uses [HighlightThemeProvider] to supply the active theme to all [SyntaxHighlightedCode]
 * composables in the tree. The top bar provides two controls:
 * - **Theme picker** (palette icon): cycles between Tokyo Night, GitHub, Dracula,
 *   Tomorrow, and Atom One theme families, demonstrating both built-in and user-provided themes.
 * - **Light/Dark toggle**: switches between the light and dark variant of the selected theme.
 *
 * Sections are organized into 10 focused hubs:
 * - **Languages**: highlights samples across different languages.
 * - **Styling**: interactive styling playground with typography controls and custom chrome showcase.
 * - **Themes**: exercises every [HighlightTheme] factory method (`fromCss`, `fromAsset`, `Map`).
 * - **Theme Gallery**: unified theme browser for 8 built-in themes plus 200+ asset themes.
 * - **Live Editor**: interactive debounced inline editor with live syntax highlighting.
 * - **LLM/Streaming**: real-time token streaming with ticker stats.
 * - **Languages & Engine**: static language catalog, file extensions, engine info, and auto-detection playground.
 * - **Large File**: production-scale file benchmark (~71 KB JS) with latency stage breakdown.
 * - **Quick Start**: executes the exact README.md getting-started snippet to ensure it compiles and runs.
 * - **Advanced**: dual-theme caching, bare Compose Text usage, callbacks, and low-level engine APIs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SampleScreen(viewModel: SampleViewModel = viewModel()) {
    val context = LocalContext.current
    val codeSamples = viewModel.codeSamples
    val themePairs = viewModel.themePairs
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isDark by rememberSaveable { mutableStateOf(true) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var activeTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showStylingSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Shared copy handler: copies to clipboard and shows a snackbar confirmation.
    val onCopyClick: (String) -> Unit =
        remember(scope, clipboard, snackbarHostState) {
            { code ->
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("code", code)))
                    snackbarHostState.showSnackbar("Successfully copied source code to clipboard")
                }
            }
        }

    // Shared feedback handler for custom header action demos (edit, share, ...).
    val onActionMessage: (String) -> Unit =
        remember(scope, snackbarHostState) {
            { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
        }

    var selectedThemeIndex by rememberSaveable { mutableIntStateOf(4) } // Atom One
    val activePair = themePairs[selectedThemeIndex.coerceIn(themePairs.indices)]
    val tabs = DemoTab.all

    HighlightThemeProvider(
        lightHighlightTheme = activePair.light,
        darkHighlightTheme = activePair.dark,
        darkTheme = isDark,
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (tabs[activeTabIndex.coerceIn(tabs.indices)] == DemoTab.Styling) {
                    ExtendedFloatingActionButton(
                        onClick = { showStylingSheet = true },
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.tune_24dp),
                                contentDescription = null,
                            )
                        },
                        text = { Text("Customize Style") },
                    )
                }
            },
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
                        .consumeWindowInsets(PaddingValues(top = innerPadding.calculateTopPadding()))
                        .imePadding(),
            ) {
                val selectedTabIndex = activeTabIndex.coerceIn(tabs.indices)
                PrimaryScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { activeTabIndex = index },
                            text = { Text(tab.title) },
                        )
                    }
                }
                LazyColumn(
                    state = listState,
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
                    when (tabs[selectedTabIndex]) {
                        DemoTab.Languages -> {
                            item { InfoBanner() }
                            codeSamples.forEach { sample ->
                                item(key = sample.displayLabel) {
                                    SectionHeader(sample.displayLabel)
                                    SyntaxHighlightedCode(
                                        code = sample.code,
                                        language = sample.language,
                                        modifier = Modifier.fillMaxWidth(),
                                        showLineNumbers = sample.language == "python",
                                        onCopyClick = onCopyClick,
                                        actions = { onCopy ->
                                            SyntaxHighlightedCodeDefaults.CopyButton(
                                                onClick = onCopy,
                                                contentDescription = "Copy code",
                                            )
                                        },
                                    )
                                }
                            }
                        }

                        DemoTab.Styling -> {
                            item {
                                StylingSection(
                                    showSheet = showStylingSheet,
                                    onDismissSheet = { showStylingSheet = false },
                                    onActionMessage = onActionMessage,
                                )
                            }
                        }

                        DemoTab.Themes -> {
                            item { ThemeCreationSection() }
                        }

                        DemoTab.ThemeGallery -> {
                            item { ThemeGallerySection() }
                        }

                        DemoTab.LiveEditor -> {
                            item { LiveEditorSection() }
                        }

                        DemoTab.Streaming -> {
                            item { StreamingSection() }
                        }

                        DemoTab.LanguageCatalog -> {
                            item { LanguageCatalogSection() }
                        }

                        DemoTab.LargeFile -> {
                            item { LargeFileSection(onCopyClick = onCopyClick) }
                        }

                        DemoTab.QuickStart -> {
                            item { ReadmeQuickStartSection() }
                        }

                        DemoTab.Advanced -> {
                            item {
                                AdvancedSection(
                                    lightTheme = activePair.light,
                                    darkTheme = activePair.dark,
                                    isDark = isDark,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
