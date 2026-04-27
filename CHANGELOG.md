# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- `HighlightTheme.fromColorMap()` — supply a theme from any `Map<String, SpanStyle>` (e.g. Material 3 dynamic color)
- Sample app uses GitHub and GitHub Dark themes via `HighlightTheme.fromAsset()`, demonstrating user-provided custom themes

### Fixed
- `HighlightTheme.fromAsset()` now correctly throws `HighlightException.ThemeNotFound` when the asset file is missing (previously the error was silently swallowed)

## [0.2.0] - 2026-04-26

### Added
- JitPack publishing support — library available via `com.github.hossain-khan.android-compose-highlight:compose-highlight:0.2.0`
- Comprehensive KDoc with usage examples on all public API classes
- `MODULE.md` for Dokka module-level documentation page
- Dokka API docs published to GitHub Pages via CI

## [0.1.0] - 2026-04-26

### Added
- Initial release
- `SyntaxHighlightedCode` composable with line numbers, copy button, and language badge
- `HighlightThemeProvider` for automatic light/dark theme switching via `CompositionLocal`
- `HighlightEngine` for headless/programmatic `AnnotatedString` generation
- `rememberHighlightEngine` and `rememberHighlightedCode` Compose helpers
- `CodeBlockStyle` with `Default` and `Compact` presets
- Built-in themes: Tomorrow (light), Tomorrow Night (dark), Atom One Dark, Atom One Light
- Custom theme support via `HighlightTheme.fromAsset()` and `HighlightTheme.fromCss()`
- 190+ languages via bundled Highlight.js
- GitHub Actions CI workflow (lint + unit tests + assemble)
- AndroidX Microbenchmarks for WebView JS pipeline, ThemeParser, and HtmlToAnnotatedString
