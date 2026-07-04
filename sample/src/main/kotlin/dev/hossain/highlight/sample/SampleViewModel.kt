package dev.hossain.highlight.sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dev.hossain.highlight.engine.HighlightTheme

/**
 * ViewModel that retains demo data across configuration changes.
 *
 * Loading code samples from assets and parsing custom CSS themes are relatively
 * expensive, one-time operations. Keeping them in a ViewModel avoids re-reading
 * assets and re-parsing themes every time the Activity is recreated (e.g. on
 * screen rotation).
 *
 * Uses [AndroidViewModel] to access the [Application] context so that no
 * Activity [android.content.Context] is held beyond its lifecycle.
 */
internal class SampleViewModel(
    application: Application,
) : AndroidViewModel(application) {
    /** All demo code samples loaded from `assets/samples/`. */
    val codeSamples: List<CodeSample> by lazy {
        loadCodeSamples(application)
    }

    /** All available theme pairs - Tokyo Night uses fromAsset() to demonstrate custom themes. */
    val themePairs: List<ThemePair> by lazy {
        listOf(
            ThemePair(
                name = "Tokyo Night",
                light = HighlightTheme.fromAsset(application, "themes/tokyo-night-light.min.css", "tokyo-night-light"),
                dark = HighlightTheme.fromAsset(application, "themes/tokyo-night-dark.min.css", "tokyo-night-dark"),
            ),
            ThemePair(
                name = "GitHub",
                light = HighlightTheme.githubLight(),
                dark = HighlightTheme.githubDark(),
            ),
            ThemePair(
                name = "Dracula",
                light = HighlightTheme.alucardLight(),
                dark = HighlightTheme.draculaDark(),
            ),
            ThemePair(
                name = "Tomorrow",
                light = HighlightTheme.tomorrow(),
                dark = HighlightTheme.tomorrowNight(),
            ),
            ThemePair(
                name = "Atom One",
                light = HighlightTheme.atomOneLight(),
                dark = HighlightTheme.atomOneDark(),
            ),
        )
    }
}
