package dev.hossain.highlight.sample

internal sealed class DemoTab(
    val title: String,
) {
    data object Languages : DemoTab("Languages")

    data object Styling : DemoTab("Styling")

    data object Themes : DemoTab("Themes")

    data object ThemeGallery : DemoTab("Theme Gallery")

    data object LiveEditor : DemoTab("Live Editor")

    data object Streaming : DemoTab("LLM/Streaming")

    data object LanguageCatalog : DemoTab("Languages & Engine")

    data object QuickStart : DemoTab("Quick Start")

    data object Advanced : DemoTab("Advanced & Perf")

    companion object {
        val all by lazy {
            listOf(
                Languages,
                Styling,
                Themes,
                ThemeGallery,
                LiveEditor,
                Streaming,
                LanguageCatalog,
                QuickStart,
                Advanced,
            )
        }
    }
}
