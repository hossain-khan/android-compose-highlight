# Changelog

Full release history is maintained in [`CHANGELOG.md`](https://github.com/hossain-khan/android-compose-highlight/blob/main/CHANGELOG.md) in the repository root.

For release artifacts and APK downloads, see the [GitHub Releases page](https://github.com/hossain-khan/android-compose-highlight/releases).

## Recent highlights

### 0.19.1 - Bug fixes
- `CodeBlockStyle.copyButtonSize` now correctly scales the default copy button touch target and glyph
- `SyntaxHighlightedCodeDefaults.CopyButton` `size` parameter now scales the `⧉` icon glyph proportionally

### 0.19.0 - Slot API (Breaking Change)
- `languageLabelContent` replaces `showLanguageLabel: Boolean`
- `copyButtonContent` replaces `showCopyButton: Boolean` + `copyButtonIcon` + `copyButtonContentDescription`
- New helpers: `SyntaxHighlightedCodeDefaults.CopyButton` and `SyntaxHighlightedCodeDefaults.LanguageLabel`

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
