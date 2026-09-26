package dev.hossain.highlight.engine

import androidx.compose.runtime.Immutable

/**
 * Metadata for a bundled syntax highlighting theme.
 *
 * Lightweight: no theme initialization occurs until [theme] or [create] is first accessed.
 * Once accessed, the [HighlightTheme] instance is cached for referential stability.
 *
 * @property id Stable string identifier for persistence (e.g. `"tomorrow-night"`, `"dracula"`).
 * @property displayName Human-readable name for UI display (e.g. `"Tomorrow Night"`, `"Dracula"`).
 *   These are proper nouns / brand names and are not localizable.
 * @property isDark Whether this theme is designed for dark backgrounds.
 */
@Immutable
public class HighlightThemeDescriptor internal constructor(
    public val id: String,
    public val displayName: String,
    public val isDark: Boolean,
    factory: () -> HighlightTheme,
) {
    /** Whether this theme is designed for light backgrounds. */
    public val isLight: Boolean get() = !isDark

    /** The [HighlightTheme] instance. Lazily created on first access, then cached. */
    public val theme: HighlightTheme by lazy(factory)

    /** Creates (or returns the cached) [HighlightTheme] instance. */
    public fun create(): HighlightTheme = theme

    /**
     * Compares this descriptor to another for equality based solely on [id].
     */
    public override fun equals(other: Any?): Boolean = other is HighlightThemeDescriptor && id == other.id

    /**
     * Returns a hash code consistent with [equals], based solely on [id].
     */
    public override fun hashCode(): Int = id.hashCode()

    /**
     * Returns a string representation of this descriptor.
     */
    public override fun toString(): String = "HighlightThemeDescriptor(id=$id, displayName=$displayName, isDark=$isDark)"
}
