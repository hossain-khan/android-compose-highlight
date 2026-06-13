# Changelog

Full release history is maintained in [`CHANGELOG.md`](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md) in the repository root.

For release artifacts and APK downloads, see the [GitHub Releases page](https://github.com/hossain-khan/android-compose-highlight/releases).

## Recent highlights

### 0.29.0 - Tab and auto-indent support in text editor

- Added `indentation`, `autoIndentEnabled`, and `tabKeyInterceptionEnabled` parameters to `SyntaxHighlightedTextEditor`
- Tab key hardware interception to insert custom indentation instead of shifting focus
- Auto-indentation to automatically copy the previous line's leading whitespace on hardware/virtual keyboards
- Intercepted arrow keys to prevent focus from escaping the editor boundaries

### 0.28.0 - Vector copy icon and sample app polish

- Default copy icon replaced with vector drawable (`copy_code_block.xml`) with proportional scaling
- Info banner added to sample app Languages tab with library version and "Open Docs" button
- `LIB_VERSION_NAME` build config field keeps displayed version in sync with published artifact
- `EngineInfoSection` search box outline now matches other info cards, language chips use vector icons
- KDoc updated for `CopyButton` to describe vector icon behavior

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

---

[View full CHANGELOG on GitHub](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md){ .md-button }
