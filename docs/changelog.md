# Changelog

Full release history is maintained in [`CHANGELOG.md`](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md) in the repository root.

For release artifacts and APK downloads, see the [GitHub Releases page](https://github.com/hossain-khan/android-compose-highlight/releases).

## Recent highlights

### 0.21.0 - Slot parameter rename and fallback colors

- `languageLabelContent` renamed to `languageLabel`; `copyButtonContent` renamed to `copyButton` (matches Material 3 slot naming conventions)
- `CodeBlockStyle` gains `fallbackBackgroundColor` and `fallbackTextColor` - configurable fallbacks used when the active theme has no `.hljs` base rule
- `SyntaxHighlightedCodeDefaults.fallbackBackgroundColor` and `fallbackTextColor` constants exposed
- `placeholder` parameter added to `SyntaxHighlightedCode` - composable rendered while async highlighting is in progress
- `onError` callback added to `SyntaxHighlightedCode`, `rememberHighlightedCode`, and `rememberHighlightedCodeBothThemes`
- `HighlightEngine` now implements `java.io.Closeable` - `close()` delegates to `destroy()`

### 0.20.0 - Language discovery APIs

- `HighlightLanguage.fromExtension(ext)` - maps file extensions to Highlight.js language identifiers without a WebView round-trip
- `HighlightEngine.highlightAuto(code, theme)` - auto-detect language and highlight; returns `AutoHighlightResult` with `detectedLanguage`
- `HighlightEngine.getLanguage(nameOrAlias)` - validate a language name and retrieve its metadata; returns `HighlightLanguageInfo` (name + aliases list)
- `AutoHighlightResult` and `HighlightLanguageInfo` data classes added

### 0.19.1 - Bug fixes

- `CodeBlockStyle.copyButtonSize` now correctly scales the default copy button touch target and glyph
- `SyntaxHighlightedCodeDefaults.CopyButton` `size` parameter now scales the `⧉` icon glyph proportionally

### 0.18.0 - Timing diagnostics

- `HighlightTimings` data class with per-stage duration breakdown
- `HighlightResult.timings` and `ThemedHighlightResult.timings` always populated

### 0.17.x - Theme parser improvements
- Merged rules for same CSS selector (fixes `nord` and other multi-rule themes)
- Named CSS color support (`red`, `green`, `grey`, `gold`, etc.)
- 4-digit hex color support (`#rgba`)
- `@media` at-rule block stripping (fixes `a11y-light` and similar themes)

---

[View full CHANGELOG on GitHub](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md){ .md-button }
