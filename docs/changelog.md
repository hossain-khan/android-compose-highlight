# Changelog

Full release history is maintained in [`CHANGELOG.md`](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md) in the repository root.

For release artifacts and APK downloads, see the [GitHub Releases page](https://github.com/hossain-khan/android-compose-highlight/releases).

## Recent highlights

### 0.35.0 - Newline-aware streaming & progressive backfill

- Added newline-aware debouncing and progressive line backfilling for `StreamingSyntaxHighlightedCode` and `rememberStreamingHighlightedCode`
- Completed lines now snap into full syntax highlighting in the background as newlines (`\n`) arrive without waiting for idle pauses
- Added `triggerOnNewline` and `minThrottleMs` (150 ms) to throttle background highlight jobs and protect the JS engine
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

### 0.32.0 - New precompiled themes, spec compliance, and API cleanups

- Added four new built-in themes (GitHub, GitHub Dark, Dracula, and Alucard) with precompiled color maps for fast loading without Context
- Aligned Dracula and Alucard built-in themes to be fully spec-compliant with correct color and selector mappings
- Renamed `rememberTomorrowTheme()` to `rememberTomorrowLightTheme()` for naming consistency across light/dark suffix theme helpers
- Removed dead legacy parser code from `HtmlParser.kt` to shrink AAR size and improve code coverage metrics

### 0.31.0 - Scroll hoisting, preview fixes, and CI hardening

- Added scroll-state hoisting to `SyntaxHighlightedCode` and `SyntaxHighlightedTextEditor` for programmatic scroll control
- Fixed Compose Preview crashes by blocking WebView initialization in `@Preview` composables across the editor, read-only blocks, and theme provider
- Fixed `HighlightResult.spanCount` semantics and CSS `#RRGGBBAA` color parsing order
- Stabilized `SyntaxHighlightedTextEditor` callbacks, remembered focus/scroll modifiers, and added `modifier` parameters to the default copy button and language badge slot helpers
- Hardened CI with release builds, Maven publication smoke test, and Compose compiler report verification

---

View the full CHANGELOG in the repo root.
