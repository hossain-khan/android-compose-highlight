# Sample App

A standalone Android app that exercises every feature of the `compose-highlight` library.
It is **not** published — it exists solely to demonstrate usage and serve as a manual test bed.

## Structure

```
sample/
├── src/main/
│   ├── assets/
│   │   ├── samples/          # Code snippets shown in the Languages tab (one file per language)
│   │   └── themes/           # 80+ Highlight.js CSS themes (bundled from highlight.js CDN)
│   │                         # Demonstrates HighlightTheme.fromAsset()
│   └── kotlin/…/sample/
│       ├── MainActivity.kt         # Entry point; wraps SampleScreen in HighlightThemeProvider
│       ├── SampleScreen.kt         # Top-level screen: tab bar + per-tab content routing
│       ├── DemoTab.kt              # Sealed class for the 11 demo tabs (type-safe routing)
│       ├── SampleData.kt           # loadCodeSamples(), loadThemePairs(), KOTLIN_SNIPPET, PYTHON_SNIPPET
│       ├── sections/               # One file per tab — each exports a single @Composable
│       │   ├── SectionComponents.kt    # Shared SectionHeader / SubSectionHeader
│       │   ├── StylingSection.kt
│       │   ├── TypographySection.kt
│       │   ├── TogglesSection.kt
│       │   ├── CallbacksSection.kt
│       │   ├── PlaceholderSection.kt
│       │   ├── ThemeCreationSection.kt
│       │   ├── AllThemesSection.kt
│       │   ├── LanguageDiscoverabilitySection.kt
│       │   ├── AdvancedEngineSection.kt
│       │   └── EngineInfoSection.kt
│       └── perf/                   # Separate performance-benchmark screen
│           ├── PerfActivity.kt
│           └── PerfScreen.kt
```

## Demo tabs

| Tab | What it shows |
|-----|---------------|
| **Languages** | Highlights every file from `assets/samples/` — one code block per language |
| **Styling** | `CodeBlockStyle` variants and custom background/border parameters |
| **Typography** | `CodeBlockStyle.textStyle` — font size, weight, line height |
| **Toggles** | All boolean flags: line numbers, language label, copy button |
| **Callbacks** | `onHighlightComplete` and `onCopyClick` in action |
| **Placeholder** | `placeholder` slot of `SyntaxHighlightedCode` — default, dimmed text, and loading label |
| **Themes** | `HighlightTheme` factory methods demonstrated side-by-side |
| **All Themes** | Scrollable showcase of all 256 bundled highlight.js themes with live preview |
| **Lang Discover** | Language detection and manual language selection with filter chips |
| **Advanced** | `rememberHighlightedCodeBothThemes` for instant light/dark switching |
| **Engine** | `HighlightEngine.highlightJsVersion` and `supportedLanguages` |

## Adding a language sample

Drop a file into `assets/samples/` with:
- A two-digit numeric prefix for ordering, e.g. `18_example.rb`
- A real file extension so your IDE applies syntax highlighting

`loadCodeSamples()` in `SampleData.kt` picks it up automatically at runtime — no Kotlin changes needed.
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
> For authoritative performance measurement and regression detection, use the AndroidX microbenchmarks in `compose-highlight/src/androidTest/` — specifically the `HighlightEngineBenchmark` class:
> ```bash
> ./gradlew :compose-highlight:connectedAndroidTest \
>   -Pandroid.testInstrumentationRunnerArguments.class=dev.hossain.highlight.benchmark.HighlightEngineBenchmark
> ```
> See the [Benchmarks section in the root README](../README.md#benchmarks) for full details.
