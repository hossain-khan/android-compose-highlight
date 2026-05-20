package dev.hossain.highlight.engine

/**
 * Metadata returned by [HighlightEngine.getLanguage].
 *
 * Contains the canonical Highlight.js display [name] plus any registered [aliases] for the
 * requested language.
 *
 * @property name Canonical Highlight.js language name.
 * @property aliases Registered aliases for the language.
 */
data class HighlightLanguageInfo(
    val name: String,
    val aliases: List<String>,
)
