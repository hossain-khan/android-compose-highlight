package dev.hossain.highlight.sample

import dev.hossain.highlight.sample.sections.AdvancedEngineSection
import dev.hossain.highlight.sample.sections.AllThemesSection
import dev.hossain.highlight.sample.sections.CallbacksSection
import dev.hossain.highlight.sample.sections.EngineInfoSection
import dev.hossain.highlight.sample.sections.StylingSection
import dev.hossain.highlight.sample.sections.ThemeCreationSection
import dev.hossain.highlight.sample.sections.TogglesSection
import dev.hossain.highlight.sample.sections.TypographySection

internal sealed class DemoTab(
    val title: String,
) {
    data object LanguageDiscoverability : DemoTab("Lang Discover")

    data object Languages : DemoTab("Languages")

    data object Styling : DemoTab("Styling")

    data object Typography : DemoTab("Typography")

    data object Toggles : DemoTab("Toggles")

    data object Callbacks : DemoTab("Callbacks")

    data object Placeholder : DemoTab("Placeholder")

    data object Themes : DemoTab("Themes")

    data object AllThemes : DemoTab("All Themes")

    data object Advanced : DemoTab("Advanced")

    data object Engine : DemoTab("Engine")

    data object Chat : DemoTab("Chat")

    companion object {
        val all by lazy {
            listOf(
                Languages,
                Styling,
                Typography,
                Toggles,
                Callbacks,
                Placeholder,
                Themes,
                AllThemes,
                LanguageDiscoverability,
                Advanced,
                Engine,
                Chat,
            )
        }
    }
}
