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

    /** All available theme pairs - GitHub uses fromAsset() to demonstrate custom themes. */
    val themePairs: List<ThemePair> by lazy {
        listOf(
            ThemePair(
                name = "GitHub",
                light = HighlightTheme.fromAsset(application, "themes/github.css", "github"),
                dark = HighlightTheme.fromAsset(application, "themes/github-dark.css", "github-dark"),
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
