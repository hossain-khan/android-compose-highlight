[![CI](https://github.com/hossain-khan/android-compose-highlight/actions/workflows/ci.yml/badge.svg)](https://github.com/hossain-khan/android-compose-highlight/actions/workflows/ci.yml) [![codecov](https://codecov.io/github/hossain-khan/android-compose-highlight/graph/badge.svg?token=MHCCHQVSLX)](https://codecov.io/github/hossain-khan/android-compose-highlight) [![release](https://badgen.net/github/release/hossain-khan/android-compose-highlight?icon=github&color=purple)](https://github.com/hossain-khan/android-compose-highlight/releases/latest) [![Maven Centra](https://img.shields.io/maven-central/v/dev.hossain/compose-highlight?color=brown)](https://central.sonatype.com/artifact/dev.hossain/compose-highlight) [![Android Weekly](https://androidweekly.net/issues/issue-727/badge)](https://androidweekly.net/issues/issue-727)


# <img src="docs/assets/images/logo-plain.png" height="26" atl="Compose Code Highlight Logo"> Compose Highlight for Android

A Jetpack Compose library for beautiful syntax highlighting - powered by [Highlight.js](https://highlightjs.org/) running in a hidden WebView, converting tokenised HTML to native Compose `AnnotatedString`. Supports 190+ languages with no custom lexers to maintain.

**[📚 Full documentation →](https://hossain-khan.github.io/android-compose-highlight/)**

## Install

```kotlin
dependencies {
    implementation("dev.hossain:compose-highlight:0.28.0")
}
```

## Quick Start

```kotlin
HighlightThemeProvider(
    lightHighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme = rememberTomorrowNightTheme(),
) {
    SyntaxHighlightedCode(
        code            = "data class Config(val enableHighlight: Boolean = true)",
        language        = "kotlin",
        showLineNumbers = true,
    )
}
```

## Demo

| Sample App | All Themes |
| ---- | ----- |
| <video src="https://github.com/user-attachments/assets/5dc18969-396a-45de-849b-0f8e3e9ffd26"> | <video src="https://github.com/user-attachments/assets/ed155002-be9b-4ba4-898b-35597cced0da"> |


## Links

- 📚 **Docs:** [github.io/android-compose-highlight/](https://hossain-khan.github.io/android-compose-highlight/)
- 🔖 **API Reference:** [github.io/android-compose-highlight/api/](https://hossain-khan.github.io/android-compose-highlight/api/)
- 📝 **Blog:** [hossain.dev/syntax-highlighting-on-android-highlight-js-native-compose-engine/](https://hossain.dev/posts/syntax-highlighting-on-android-highlight-js-native-compose-engine/)
- 📱 **Sample App:** [src/sample](https://github.com/hossain-khan/android-compose-highlight/tree/main/sample) [[⬇️ APK]](https://github.com/hossain-khan/android-compose-highlight/releases/latest)
- ✨ **Compare Other Libs:** [github.com/android-syntax-highlighter-compose](https://github.com/hossain-khan/android-syntax-highlighter-compose)

## License

MIT
