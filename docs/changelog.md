# Changelog

Full release history is maintained in [`CHANGELOG.md`](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md) in the repository root.

For release artifacts and APK downloads, see the [GitHub Releases page](https://github.com/hossain-khan/android-compose-highlight/releases).

## Recent highlights

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

- Added scroll-state hoisting to `SyntaxHighlightedCode` and `SyntaxHighlightedTextEditor`
  for programmatic scroll control
- Fixed Compose Preview crashes by blocking WebView initialization in `@Preview`
  composables across the editor, read-only blocks, and theme provider
- Fixed `HighlightResult.spanCount` semantics and CSS `#RRGGBBAA` color parsing order
- Stabilized `SyntaxHighlightedTextEditor` callbacks, remembered focus/scroll modifiers,
  and added `modifier` parameters to the default copy button and language badge slot helpers
- Hardened CI with release builds, Maven publication smoke test, and Compose compiler
  report verification

### 0.30.2 - Remove deprecated treeWalk timing

- Removed the deprecated `treeWalk` timing property entirely from `HighlightTimings`
- Cleaned up the timings usage in `HighlightEngine` and internally in `HtmlToAnnotatedString`
- Updated the sample app performance breakdown screen and timing unit tests to
  remove the property
- Synced documentation across the guides to reflect the updated timing model

### 0.30.1 - Parser performance optimizations

- Optimized HTML-to-AnnotatedString pipeline with SAX-style single-pass parsing,
  eliminating intermediate tree allocations
- Added substring avoidance, in-place attribute extraction, lazy entity decoding, and
  allocation-free numeric parsing
- Benchmarks show 29-57% faster single-theme and 5-39% faster dual-theme conversions
  across all fixtures
- Saved Jsoup baseline and SAX optimized JSON reports under
  `resources/html-parser-benchmarks/` for regression tracking

---

View the full CHANGELOG in the repo root.
