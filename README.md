[![CI](https://github.com/hossain-khan/android-compose-highlight/actions/workflows/ci.yml/badge.svg)](https://github.com/hossain-khan/android-compose-highlight/actions/workflows/ci.yml) [![codecov](https://codecov.io/github/hossain-khan/android-compose-highlight/graph/badge.svg?token=MHCCHQVSLX)](https://codecov.io/github/hossain-khan/android-compose-highlight) [![release](https://badgen.net/github/release/hossain-khan/android-compose-highlight)](https://github.com/hossain-khan/android-compose-highlight/releases/latest) [![Maven Centra](https://img.shields.io/maven-central/v/dev.hossain/compose-highlight?color=brown)](https://central.sonatype.com/artifact/dev.hossain/compose-highlight) [![Android Weekly](https://androidweekly.net/issues/issue-727/badge)](https://androidweekly.net/issues/issue-727)


# <img src="docs/assets/images/favicon.png" height="24" atl="Compose Code Highlight Logo"> Compose Highlight for Android

A Jetpack Compose library for beautiful syntax highlighting - powered by [Highlight.js](https://highlightjs.org/) running in a hidden WebView, converting tokenised HTML to native Compose `AnnotatedString`. Supports 190+ languages with no custom lexers to maintain.

**[📚 Full documentation →](https://hossain-khan.github.io/android-compose-highlight/)**

## Install

```kotlin
dependencies {
    implementation("dev.hossain:compose-highlight:0.24.1")
}
```

## Quick Start

```kotlin
HighlightThemeProvider(
    lightHighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme  = rememberAtomOneDarkTheme(),
) {
    SyntaxHighlightedCode(
        code            = "data class Config(val enableHighlight: Boolean = true)",
        language        = "kotlin",
        showLineNumbers = true,
    )
}
```

## Demo

| Sample App | Customizations |
| ---- | ----- |
| <video src="https://github.com/user-attachments/assets/9521cb13-ff32-4a5b-956e-4e620bbee4d1"> | <video src="https://github.com/user-attachments/assets/02a71e4d-6ada-4008-99c0-f642c618d778"> |

## Links

- 📚 **Docs:** https://hossain-khan.github.io/android-compose-highlight/
- 🔖 **API Reference:** https://hossain-khan.github.io/android-compose-highlight/api/
- 📝 **Blog:** https://hossain.dev/posts/syntax-highlighting-on-android-highlight-js-native-compose-engine/
- 📱 **Sample App:** https://github.com/hossain-khan/android-compose-highlight/tree/main/sample
- ✨ **Compare Other Libs:** https://github.com/hossain-khan/android-syntax-highlighter-compose

## License

MIT
