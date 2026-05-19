# compose-highlight

**Jetpack Compose syntax highlighting powered by [Highlight.js](https://highlightjs.org/).**

Renders beautifully colored, selectable, copy-able code blocks in any Compose UI — with light/dark theme support, line numbers, and full customization of the header slots.

[![Maven Central](https://img.shields.io/maven-central/v/dev.hossain/compose-highlight.svg)](https://search.maven.org/artifact/dev.hossain/compose-highlight)
[![License](https://img.shields.io/github/license/hossain-khan/android-compose-highlight)](https://github.com/hossain-khan/android-compose-highlight/blob/main/LICENSE)

---

## Quick install

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("dev.hossain:compose-highlight:<version>")
}
```

Replace `<version>` with the [latest release](https://github.com/hossain-khan/android-compose-highlight/releases).

---

## Minimal example

```kotlin
HighlightThemeProvider {
    SyntaxHighlightedCode(
        code     = "val greeting = \"Hello, World!\"",
        language = "kotlin",
    )
}
```

That's it. `HighlightThemeProvider` automatically picks Tomorrow (light) or Tomorrow Night (dark) based on the system setting.

---

## Key features

- **180+ languages** — every language Highlight.js supports
- **Light + dark themes** — automatic system-mode switching, or manual override
- **Built-in themes** — Tomorrow, Tomorrow Night, Atom One Dark, Atom One Light
- **Custom themes** — load any Highlight.js CSS from `assets/`, raw CSS string, or a `Map<String, SpanStyle>`
- **Slots** — replace the language badge and copy button with any composable
- **Line numbers** — optional gutter with configurable width and color
- **Copy to clipboard** — built-in, with an `onCopyClick` callback for custom feedback
- **Performance** — one hidden WebView shared across all code blocks via `HighlightThemeProvider`

---

## Next steps

- [Getting Started](getting-started.md) — full setup walkthrough
- [API Reference](reference/index.md) — all public classes and functions
- [Guides](guides/theming.md) — theming, customization, line numbers, performance
- [Changelog](changelog.md) — release history
