# API Reference

This section documents the public API of `compose-highlight`.

| Class / Function | Description |
|---|---|
| [`SyntaxHighlightedCode`](syntax-highlighted-code.md) | Primary composable - renders a styled, highlighted code block |
| [`HighlightThemeProvider`](syntax-highlighted-code.md#highlightthemeprovider) | Provides a shared theme and engine to a composable subtree |
| [`CodeBlockStyle`](code-block-style.md) | Visual style configuration (shape, padding, font, copy button size) |
| [`SyntaxHighlightedCodeDefaults`](code-block-style.md#syntaxhighlightedcodedefaults) | Default constants and helper composables (`CopyButton`, `LanguageLabel`) |
| [`HighlightTheme`](highlight-theme.md) | Theme model backed by a Highlight.js CSS file |
| [`HighlightEngine`](highlight-engine.md) | Lower-level engine for custom highlighting pipelines |
| [`HighlightLanguage`](highlight-language.md) | Maps file extensions to Highlight.js language identifiers |
| [`HighlightResult`](highlight-engine.md#highlightresult) | Result of `HighlightEngine.highlight()` - annotated string, span count, timing |
| [`ThemedHighlightResult`](highlight-engine.md#themedhighlightresult) | Result of `HighlightEngine.highlightBothThemes()` - light and dark variants |
| [`AutoHighlightResult`](highlight-engine.md#autohighlightresult) | Result type returned by `HighlightEngine.highlightAuto()` |
| [`HighlightTimings`](../guides/performance.md#timing-callbacks) | Per-stage timing breakdown (`jsBridge`, `jsonUnescape`, `htmlParse`, `treeWalk`, `themeParse`, `total`) |
| [`HighlightLanguageInfo`](highlight-engine.md#highlightlanguageinfo) | Metadata returned by `HighlightEngine.getLanguage()` |
| [`HighlightException`](highlight-engine.md) | Sealed exception hierarchy for all engine errors |
| [`rememberHighlightedCode`](syntax-highlighted-code.md#rememberhighlightedcode) | Composable helper - highlights code and returns an `AnnotatedString` state |
| [`rememberHighlightedCodeBothThemes`](syntax-highlighted-code.md#rememberhighlightedcodeboththemes) | Highlights once, produces light + dark variants |
| [`rememberHighlightEngine`](highlight-engine.md#rememberhighlightengine) | Returns the shared or standalone `HighlightEngine` for the current composition |
| `rememberTomorrowTheme` | Composable factory for the built-in Tomorrow (light) theme |
| `rememberTomorrowNightTheme` | Composable factory for the built-in Tomorrow Night (dark) theme |
| `rememberAtomOneDarkTheme` | Composable factory for the built-in Atom One Dark theme |
| `rememberAtomOneLightTheme` | Composable factory for the built-in Atom One Light theme |

---

For the full generated KDoc reference, see the [Dokka API Docs](https://hossain-khan.github.io/android-compose-highlight/api/).
