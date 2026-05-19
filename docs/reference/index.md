# API Reference

This section documents the public API of `compose-highlight`.

| Class / Function | Description |
|---|---|
| [`SyntaxHighlightedCode`](syntax-highlighted-code.md) | Primary composable — renders a styled, highlighted code block |
| [`HighlightThemeProvider`](syntax-highlighted-code.md#highlightthemeprovider) | Provides a shared theme and engine to a composable subtree |
| [`CodeBlockStyle`](code-block-style.md) | Visual style configuration (shape, padding, font, copy button size) |
| [`SyntaxHighlightedCodeDefaults`](code-block-style.md#syntaxhighlightedcodedefaults) | Default constants and helper composables (`CopyButton`, `LanguageLabel`) |
| [`HighlightTheme`](highlight-theme.md) | Theme model backed by a Highlight.js CSS file |
| [`HighlightEngine`](highlight-engine.md) | Lower-level engine for custom highlighting pipelines |
| `rememberHighlightedCode` | Composable helper — highlights code and returns an `AnnotatedString` state |
| `rememberHighlightedCodeBothThemes` | Highlights once, produces light + dark variants |
| `rememberHighlightEngine` | Returns the shared or standalone `HighlightEngine` for the current composition |
| `rememberTomorrowTheme` | Composable factory for the built-in Tomorrow (light) theme |
| `rememberTomorrowNightTheme` | Composable factory for the built-in Tomorrow Night (dark) theme |
| `rememberAtomOneDarkTheme` | Composable factory for the built-in Atom One Dark theme |
| `rememberAtomOneLightTheme` | Composable factory for the built-in Atom One Light theme |

---

For the full generated KDoc reference, see the [Dokka API Docs](https://hossain-khan.github.io/android-compose-highlight/api/).
