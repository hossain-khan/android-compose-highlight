# Changelog

Full release history is maintained in [`CHANGELOG.md`](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md) in the repository root.

For release artifacts and APK downloads, see the [GitHub Releases page](https://github.com/hossain-khan/android-compose-highlight/releases).

## Recent highlights

### 0.38.0 - Keyboard focus trap fix, WebView hardening, and unknown language optimization

- Added keyboard escape (`Key.Escape`) and focus traversal (`Shift+Tab`, `Ctrl+Tab`) to prevent keyboard traps (WCAG 2.1.2) in `SyntaxHighlightedTextEditor`
- Optimized unknown or blank language highlighting by validating against `hljs.getLanguage()` to bypass full auto-detection, eliminating a 56x latency penalty
- Hardened internal WebView security with disabled file/content access, blocked outbound network requests, and Content Security Policy enforcement
- Preserved horizontal scroll state across screen rotation and LazyColumn recycling in code viewers
- Removed broad consumer R8 keep rules to allow downstream apps to dead-code eliminate unused library components

### 0.37.1 - Auto-expanding line numbers and large file benchmark

- Fixed line-number gutter wrapping multi-digit numbers onto multiple lines for files with 1,000+ lines
- Added automatic gutter expansion via minimum width constraint (`widthIn`) and disabled text wrapping
- Added dedicated "Large File" demo tab in the sample app benchmarking 2,170+ lines of JavaScript with live pipeline timings
- Added Binary Compatibility Validator (`apiCheck` / `apiDump`) to track and enforce public ABI baseline

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

---

View the full CHANGELOG in the repo root.
