# Sample App

A standalone Android app that exercises every feature of the `compose-highlight` library.
It is **not** published - it exists solely to demonstrate usage and serve as a manual test bed.

## Structure

```
sample/
├── src/main/
│   ├── assets/
│   │   ├── samples/          # Code snippets shown in the Languages tab (one file per language)
│   │   └── themes/           # 250+ Highlight.js CSS themes (bundled from highlight.js CDN)
│   │                         # Demonstrates HighlightTheme.fromAsset()
│   └── kotlin/…/sample/
│       ├── MainActivity.kt         # Entry point; wraps SampleScreen in HighlightThemeProvider
│       ├── SampleScreen.kt         # Top-level screen: tab bar + per-tab content routing
│       ├── SampleViewModel.kt      # Manages sample app state and configuration
│       ├── DemoTab.kt              # Sealed class for the 9 demo tabs (type-safe routing)
│       ├── SampleData.kt           # loadCodeSamples(), loadThemePairs(), KOTLIN_SNIPPET, PYTHON_SNIPPET
│       ├── info/                   # Info banner components
│       │   └── InfoBanner.kt
│       ├── sections/               # One file per tab - each exports a single @Composable
│       │   ├── SectionComponents.kt    # Shared SectionHeader / SubSectionHeader
│       │   ├── StylingSection.kt       # Styling playground, typography, and macOS/tab chrome
│       │   ├── ThemeCreationSection.kt # Theme creation (fromCss, fromAsset, Map)
│       │   ├── ThemeGallerySection.kt  # Unified browser for 8 built-in + 200+ asset themes
│       │   ├── LiveEditorSection.kt    # Debounced inline code editor
│       │   ├── StreamingSection.kt     # Real-time token streaming with ticker stats
│       │   ├── LanguageCatalogSection.kt # Languages, engine info, and auto-detection playground
│       │   ├── ReadmeQuickStartSection.kt # Validates README.md snippet compiles and runs
│       │   └── AdvancedSection.kt      # Large file benchmark, dual-theme, bare Text(), callbacks
│       └── perf/                   # Separate performance-benchmark screen
│           ├── PerfActivity.kt
│           └── PerfScreen.kt
```

## Demo tabs

| Tab | What it shows |
|-----|---------------|
| **Languages** | Highlights every file from `assets/samples/` - one code block per language |
| **Styling** | `CodeBlockStyle` playground, typography slider controls, and macOS/tabs custom chrome |
| **Themes** | Custom theme creation via `HighlightTheme.fromCss`, `fromAsset`, and `Map` |
| **Theme Gallery** | Unified browser for 8 built-in themes (with ID resolution) and 200+ asset themes |
| **Live Editor** | Interactive code editing using `SyntaxHighlightedTextEditor` |
| **LLM/Streaming** | Real-time progressive rendering using `StreamingSyntaxHighlightedCode` |
| **Languages & Engine** | Static language catalog, file extensions, engine info, and `highlightAuto` playground |
| **Quick Start** | Validates that the exact `README.md` getting-started code snippet compiles and runs |
| **Advanced & Perf** | Large file benchmark, dual-theme caching, bare Compose `Text()`, callbacks, placeholders |

## Adding a language sample

Drop a file into `assets/samples/` with:
- A two-digit numeric prefix for ordering, e.g. `18_example.rb`
- A real file extension so your IDE applies syntax highlighting

`loadCodeSamples()` in `SampleData.kt` picks it up automatically at runtime - no Kotlin changes needed.
The file extension is mapped to a Highlight.js language identifier by `extensionToLanguage()`.

## Adding a custom theme

Put a Highlight.js CSS file in `assets/themes/` and load it with:

```kotlin
HighlightTheme.fromAsset(context, "themes/my-theme.css")
```

See `SampleData.kt` → `loadThemePairs()` for a working example using the bundled GitHub themes.

## Performance screen

`PerfActivity` / `PerfScreen` is an **exploratory, demo-oriented** tool that visually shows how long it takes to highlight all language samples back-to-back. Launch it from the top-right toolbar icon in the main screen.

> [!WARNING]
> The in-app performance screen is **not** a substitute for benchmark-grade measurement. It runs in a debug build without the release optimizations needed for reliable numbers, and results vary with device state and background load.
>
> For authoritative performance measurement and regression detection, use the AndroidX microbenchmarks in `compose-highlight/src/androidTest/` - specifically the `HighlightEngineBenchmark` class:
> ```bash
> ./gradlew :compose-highlight:connectedAndroidTest \
>   -Pandroid.testInstrumentationRunnerArguments.class=dev.hossain.highlight.benchmark.HighlightEngineBenchmark
> ```
> See the benchmarking commands in [AGENTS.md](../AGENTS.md) or [`HighlightEngineBenchmark`](../compose-highlight/src/androidTest/kotlin/dev/hossain/highlight/benchmark/HighlightEngineBenchmark.kt) for full details.
