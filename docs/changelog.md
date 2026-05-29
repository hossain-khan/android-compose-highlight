# Changelog

Full release history is maintained in [`CHANGELOG.md`](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md) in the repository root.

For release artifacts and APK downloads, see the [GitHub Releases page](https://github.com/hossain-khan/android-compose-highlight/releases).

## Recent highlights

### 0.24.1 - Live editor state management fixes

- Fixed span color loss in `SyntaxHighlightedTextEditor` during mid-text edits via prefix/suffix analysis
- Added `onHighlightComplete` callback to `SyntaxHighlightedTextEditor` for deterministic testing
- New public helper `rememberSyntaxHighlightedEditorValue()` for custom editor layouts with syntax highlighting

### 0.24.0 - Live code editor support

- New `SyntaxHighlightedTextEditor` composable with live syntax highlighting as you type
- 150 ms keystroke debounce with cursor and selection always preserved
- New **Live Editor** demo tab in sample app showcasing 6 languages
- Marked `@ExperimentalHighlightApi` for gradual API stabilization

### 0.23.0 - Screenshot regression testing

- 13 Roborazzi screenshot goldens covering themes, layout variants, languages, and error states
- HTML token fixtures from bundled `highlight.min.js` for testing visual stability
- `HighlightEngine.webViewForTest()` for cleaner test integration
- Replaced regex CSS parser with recursive-descent implementation for robustness

### 0.22.1 - Resource leaks and performance

- Fixed InputStream leak in `ThemeParser` CSS loading
- Reset horizontal scroll on code change to prevent stale scroll state
- Stabilized lambda instances for copy button and language label slots

### 0.22.0 - WebView and theme correctness

- Surrogate pair handling for emoji and supplementary Unicode in highlighted output
- Content-aware `HighlightTheme` equality - themes compared by content, not just name
- `WebViewInitFailed` exception when WebView is unavailable (Android Go, MDM-disabled)

### 0.17.x - Theme parser improvements
- Merged rules for same CSS selector (fixes `nord` and other multi-rule themes)
- Named CSS color support (`red`, `green`, `grey`, `gold`, etc.)
- 4-digit hex color support (`#rgba`)
- `@media` at-rule block stripping (fixes `a11y-light` and similar themes)

---

[View full CHANGELOG on GitHub](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md){ .md-button }
