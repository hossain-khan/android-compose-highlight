# Changelog

Full release history is maintained in [`CHANGELOG.md`](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md) in the repository root.

For release artifacts and APK downloads, see the [GitHub Releases page](https://github.com/hossain-khan/android-compose-highlight/releases).

## Recent highlights

### 0.27.0 - Editor keyboard options and cursor brush

- `SyntaxHighlightedTextEditor` now defaults to code-friendly `keyboardOptions` (autocorrect off, Ascii keyboard)
- Editor `onHighlightComplete` callback now receives `HighlightResult` instead of `AnnotatedString`
- New `keyboardOptions` and `cursorBrush` parameters for `SyntaxHighlightedTextEditor`
- `SyntaxHighlightedTextEditorDefaults.CodeKeyboardOptions` constant added
- `ThemeParser` no longer silently swallows non-IO exceptions

### 0.26.0 - Selector coverage and parser efficiency

- Expanded `HljsSelectors` with all official Highlight.js scopes, adding 22 missing selector constants
- Fixed `META_PROMPT` selector mapping to `hljs-meta.prompt_` for sub-scope compatibility
- Rewrote `escapeForJs` to a single-pass `StringBuilder` walk to reduce per-highlight allocations
- Reorganized implementation-only code into `.internal` packages and hid internals from Dokka API docs
- Added broad parser and escaping boundary test coverage for the new selector and escape paths

### 0.25.0 - Editor callbacks and Dokka retheme

- Added `onError` callback to `SyntaxHighlightedTextEditor` and `rememberSyntaxHighlightedEditorValue` for failure observability
- Fixed span color loss on multi-line strings and block comments during mid-text edits
- Added `SyntaxHighlightedTextEditorDefaults` object with pre-allocated singletons to reduce GC pressure while typing
- Dokka API site rethemed to match the main docs site with shared light/dark palette state
- Upgraded Zensical to 0.0.43 with improved link validation

### 0.24.1 - Live editor state management fixes

- Fixed span color loss in `SyntaxHighlightedTextEditor` during mid-text edits via prefix/suffix analysis
- Added `onHighlightComplete` callback to `SyntaxHighlightedTextEditor` for deterministic testing
- New public helper `rememberSyntaxHighlightedEditorValue()` for custom editor layouts with syntax highlighting
- Eliminated stale `LaunchedEffect` for span clearing; stale detection now happens in-composition

### 0.24.0 - Live code editor support

- New `SyntaxHighlightedTextEditor` composable with live syntax highlighting as you type
- 150 ms keystroke debounce with cursor and selection always preserved
- New **Live Editor** demo tab in sample app showcasing 6 languages
- Marked `@ExperimentalHighlightApi` for gradual API stabilization

---

[View full CHANGELOG on GitHub](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md){ .md-button }
