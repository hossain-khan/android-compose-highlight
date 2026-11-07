# Changelog

Full release history is maintained in [`CHANGELOG.md`](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md) in the repository root.

For release artifacts and APK downloads, see the [GitHub Releases page](https://github.com/hossain-khan/android-compose-highlight/releases).

## Recent highlights

### 0.41.1 - Editor horizontal scrolling arrow key navigation fix

- Restored vertical line navigation with Up and Down arrow keys when horizontal scrolling is enabled in `SyntaxHighlightedTextEditor`
- Trapped directional arrow keys at document boundaries to prevent accidental focus escape to outside UI controls
- Added support for Shift-based selection expansion during manual vertical line traversal

### 0.41.0 - Packaging fixes, dynamic engine recreation, and custom fonts

- Scoped ABI-exposed dependencies as `api` in published POM and Gradle module metadata
- Restarted highlighting effects dynamically when the active `HighlightEngine` changes or is recreated
- Added Fira Code custom font and programming ligature showcase to the sample app
- Upgraded WebKit to 1.18.0 and fixed documentation code snippets

### 0.40.0 - Reliability, lifecycle hardening, and theme safety

- Formalized explicit WebView lifecycle state machine with generation tracking to prevent destruction races
- Handled WebView renderer process crashes (`onRenderProcessGone`) with auto-recreation and typed exceptions
- Offloaded custom theme resolution from Main thread during initial composition with safe fallbacks and typed errors
- Unified build-time and runtime CSS theme parser behavior for hex colors, font styles, and `!important`
- Consolidated sample app demo tabs into 10 focused hubs with dedicated README Quick Start validation tab

### 0.39.0 - Actions and header slots

- Added `actions` trailing slot with `RowScope` receiver and pre-wired `onCopy` action to `SyntaxHighlightedCode` and `StreamingSyntaxHighlightedCode`
- Added coarse-grained `header` chrome slot for full top bar replacement (tabs, titlebars) and clean single-parameter headerless mode (`header = null`)
- Added `SyntaxHighlightedCodeDefaults.Header` exposing the standard header layout as a reusable composable
- BREAKING: Removed legacy `copyButton` parameter in favor of the more flexible `actions` and `header` slots
- Enabled Kotlin `explicitApi()` mode across `:compose-highlight` to enforce public API boundaries and explicit visibility

### 0.38.0 - Keyboard focus trap fix, WebView hardening, and unknown language optimization

- Added keyboard escape (`Key.Escape`) and focus traversal (`Shift+Tab`, `Ctrl+Tab`) to prevent keyboard traps (WCAG 2.1.2) in `SyntaxHighlightedTextEditor`
- Optimized unknown or blank language highlighting by validating against `hljs.getLanguage()` to bypass full auto-detection, eliminating a 56x latency penalty
- Hardened internal WebView security with disabled file/content access, blocked outbound network requests, and Content Security Policy enforcement
- Preserved horizontal scroll state across screen rotation and LazyColumn recycling in code viewers
- Removed broad consumer R8 keep rules to allow downstream apps to dead-code eliminate unused library components

---

View the full CHANGELOG in the repo root.
