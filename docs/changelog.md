# Changelog

Full release history is maintained in [`CHANGELOG.md`](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md) in the repository root.

For release artifacts and APK downloads, see the [GitHub Releases page](https://github.com/hossain-khan/android-compose-highlight/releases).

## Recent highlights

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

### 0.30.0 - Custom HTML parser replaces Jsoup

- Replaced the JVM-only Jsoup dependency with a single-pass pure-Kotlin HTML
  tokenizer scoped to the hljs HTML subset
- Sample APK measured 128.7 KB smaller post-R8 (-5.49%); dex drops 271 classes and
  2,298 methods. Four R8/ProGuard `-keep` rules were removed from downstream consumers
- Dual-theme highlight path is 1.27×-1.97× faster on real-world
  Kotlin/C/Rust/Go/C#/SQL fixtures
- Added real-world language test coverage with extensive token-count assertions and an
  opt-in JVM microbenchmark (`HtmlParserBenchmark`)
- Prepared the codebase for Kotlin Multiplatform (KMP)

### 0.29.0 - Tab and auto-indent support in text editor

- Added `indentation`, `autoIndentEnabled`, and `tabKeyInterceptionEnabled`
  parameters to `SyntaxHighlightedTextEditor`
- Added Tab key hardware interception to insert custom indentation instead of
  shifting focus
- Added auto-indentation to copy the previous line's leading whitespace on
  hardware and virtual keyboards
- Intercepted arrow keys to prevent focus from escaping the editor boundaries

---

View the full CHANGELOG in the repo root.
