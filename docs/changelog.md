# Changelog

Full release history is maintained in [`CHANGELOG.md`](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md) in the repository root.

For release artifacts and APK downloads, see the [GitHub Releases page](https://github.com/hossain-khan/android-compose-highlight/releases).

## Recent highlights

### 0.37.0 - Theme discovery, synchronous language catalog, and recomposition testing

- Added `HighlightThemeDescriptor` and `HighlightTheme.bundled` APIs to dynamically discover, filter, and restore bundled themes with metadata
- Added synchronous compile-time language catalog (`HighlightLanguage.all`), `primary` languages, `isSupported()`, and alias normalization (`canonicalName()`)
- Added dedicated "Theme Discovery" tab and language normalization interactive demos in the sample app
- Added horizontal scrolling toggle in the sample app live editor
- Integrated Dejavu for automated recomposition stability testing across all code block composables

### 0.36.0 - Highlight.js 11.12.0 & Sample UX Enhancements

- Upgraded bundled Highlight.js engine from 11.11.1 to 11.12.0 with grammar fixes for Python, Rust, C/C++, Java, and Go
- Added new Kotlin language aliases (`ktm`, `ktx`) and 2 new themes (`equinox` and `vs-dark`) to the sample app
- Fixed soft keyboard obscuring editor and search fields in the sample app by applying `imePadding` and refining insets
- Added manual "Stream" button control to the sample app LLM/Streaming demo tab
- Streamlined Highlight.js upgrade tooling with automated weekly upstream release monitoring

### 0.35.0 - Newline-aware streaming & progressive backfill

- Added newline-aware debouncing and progressive line backfilling for `StreamingSyntaxHighlightedCode` and `rememberStreamingHighlightedCode`
- Completed lines now snap into full syntax highlighting in the background as newlines (`\n`) arrive without waiting for idle pauses
- Added `triggerOnNewline` and `minThrottleMs` (150 ms) to throttle background highlight jobs and protect the JS engine
- Fixed mid-stream highlight failures flashing the code block to plain text by preserving previously highlighted spans
- Updated sample app with an interactive progressive backfill toggle and comprehensive TypeScript streaming demo

### 0.34.0 - Streaming Syntax Highlighting for AI & LLMs

- Added `StreamingSyntaxHighlightedCode` and `rememberStreamingHighlightedCode` (`@ExperimentalHighlightApi`) for real-time and LLM token streaming
- Implemented span-transfer snapshot pipeline (`applySnapshotSpans`) for zero-flicker, 0 ms UI render latency during streaming
- Added streaming-aware scroll handling to preserve user scroll offsets during token appends
- Added "LLM/Streaming" interactive demo tab in the sample app simulating token streams across Kotlin, Python, and TypeScript
- Upgraded documentation site generator Zensical to 0.0.56 with refreshed Dokka chrome assets

### 0.33.0 - Dracula/Alucard theme aliases, docs asset fingerprinting, and dependency updates

- Added Dracula and Alucard light/dark theme convenience aliases (`rememberDraculaLightTheme()`, `rememberAlucardDarkTheme()`, etc.)
- Migrated instrumented Compose UI tests to the v2 `createComposeRule` API
- Fixed trailing newline bug in documentation code blocks
- Added post-build asset fingerprinting for custom docsite assets to ensure reliable browser cache-busting
- Updated Compose BOM (`2026.08.00`), AndroidX WebKit (`1.17.0`), Kotlinter (`5.7.0`), Roborazzi (`1.71.0`), and Gradle wrapper (`9.7.0`)

---

View the full CHANGELOG in the repo root.
