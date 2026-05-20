package dev.hossain.highlight.engine

/**
 * Metadata returned by [HighlightEngine.getLanguage].
 *
 * Contains the human-readable display [name] plus any registered [aliases] for the
 * requested language.
 *
 * @property name Human-readable display name from Highlight.js (e.g. `"Kotlin"`, `"Python"`).
 *   This is NOT the language identifier - use [aliases] or the name you passed to
 *   [HighlightEngine.getLanguage] as the identifier for highlighting calls.
 * @property aliases Registered aliases for the language.
 */
data class HighlightLanguageInfo(
    val name: String,
    val aliases: List<String>,
)
