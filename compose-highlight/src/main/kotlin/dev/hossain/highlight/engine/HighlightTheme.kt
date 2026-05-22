package dev.hossain.highlight.engine

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * Represents a syntax highlighting theme backed by a Highlight.js CSS file.
 *
 * The color map is lazily initialized and cached — CSS parsing happens at most once per theme.
 * Background and text colors are derived from the already-parsed [colorMap] (the `.hljs` rule),
 * avoiding double-parsing of the CSS file.
 *
 * ## Built-in themes
 *
 * ```kotlin
 * // Light themes
 * HighlightTheme.tomorrow(context)
 * HighlightTheme.atomOneLight(context)
 *
 * // Dark themes
 * HighlightTheme.tomorrowNight(context)
 * HighlightTheme.atomOneDark(context)
 * ```
 *
 * ## Custom theme from an asset file
 *
 * Any Highlight.js CSS theme can be bundled in your app's `assets/` folder and loaded at runtime.
 * This is the recommended way to ship additional themes with your app.
 *
 * ```kotlin
 * // Place your .css file in src/main/assets/themes/github.css
 * val theme = HighlightTheme.fromAsset(
 *     context   = context,
 *     assetPath = "themes/github.css",
 *     name      = "github",
 * )
 * ```
 *
 * Note: `fromAsset()` is lazy — CSS parsing (and any `ThemeNotFound` error) occurs when the
 * theme is first applied, not at factory-call time.
 *
 * ## Custom theme from raw CSS
 *
 * ```kotlin
 * val theme = HighlightTheme.fromCss(
 *     cssText = rawCssString,
 *     name    = "my-inline-theme",
 * )
 * ```
 *
 * ## Custom theme from a precomputed color map
 *
 * For maximum control — e.g. deriving colors from Material 3 dynamic color or any other
 * source — you can supply the color map directly:
 *
 * ```kotlin
 * val colorMap: Map<String, SpanStyle> = mapOf(
 *     "hljs"          to SpanStyle(color = Color(0xFF24292E), background = Color(0xFFFFFFFF)),
 *     "hljs-keyword"  to SpanStyle(color = Color(0xFFD73A49), fontWeight = FontWeight.Bold),
 *     "hljs-string"   to SpanStyle(color = Color(0xFF032F62)),
 *     // ... add more token types as needed
 * )
 * val theme = HighlightTheme.fromColorMap(
 *     name            = "my-dynamic-theme",
 *     colorMap        = colorMap,
 *     backgroundColor = Color(0xFFFFFFFF),
 *     defaultTextColor = Color(0xFF24292E),
 * )
 * ```
 *
 * Any valid Highlight.js CSS theme works with `fromAsset` / `fromCss`. Community themes are at
 * [highlightjs/highlight.js/src/styles](https://github.com/highlightjs/highlight.js/tree/main/src/styles).
 *
 * ## Theme identity
 *
 * Two `HighlightTheme` instances are equal when they share the same [name] **and** the same
 * content identity (derived from the CSS text, asset path, or color map used to create
 * the theme). This means Compose APIs (`remember`, `LaunchedEffect`, `key`) correctly detect
 * theme changes even when two themes share the same name but carry different color content.
 *
 * ```kotlin
 * val light = HighlightTheme.fromCss(lightCss, "custom")
 * val dark  = HighlightTheme.fromCss(darkCss,  "custom")
 * light == dark  // false - same name but different CSS content
 *
 * val a = HighlightTheme.fromCss(css, "custom")
 * val b = HighlightTheme.fromCss(css, "custom")
 * a == b         // true  - same name and same CSS content
 * ```
 *
 * @property name Display name for this theme. Used together with [contentIdentity] by [equals]
 *   and [hashCode].
 */
@Stable
class HighlightTheme private constructor(
    val name: String,
    private val colorMapProvider: () -> Map<String, SpanStyle>,
    /**
     * Precomputed identity hash for the theme content. Computed once at construction time
     * from the CSS text, asset path, or color-map inputs, and stored as a [Long] so that
     * [equals] and [hashCode] are O(1). Used by Compose keys (`remember`, `LaunchedEffect`)
     * to detect theme changes even when two themes share the same [name].
     */
    private val contentIdentity: Long,
) {
    /** Lazily-parsed map of hljs class names → [SpanStyle]. Cached forever. */
    private val colorMapLazy = lazy { colorMapProvider() }
    val colorMap: Map<String, SpanStyle>
        get() = colorMapLazy.value

    /**
     * Tracks whether [colorMap] has been initialized (lazy block has run).
     * Used by [timedColorMap] to report [Duration.ZERO] on repeated calls.
     *
     * Uses [AtomicBoolean] with compare-and-set so that under concurrent access exactly one
     * caller reports the real parse duration; all others report [Duration.ZERO].
     */
    private val colorMapInitialized = AtomicBoolean(false)

    /**
     * Returns [colorMap] together with the time taken to initialize it.
     *
     * Called exclusively by [HighlightEngine] so that theme-parse timing is
     * only attributed to an actual highlight call, not to incidental accesses of
     * [colorMap], [backgroundColor], or [defaultTextColor] from other callers.
     *
     * On the first call the initial parse duration is returned. On all subsequent calls
     * the cached map is returned with [Duration.ZERO].
     *
     * Under concurrent access [AtomicBoolean.compareAndSet] ensures exactly one caller
     * reports a non-zero duration; all racing callers report [Duration.ZERO].
     */
    internal fun timedColorMap(): Pair<Map<String, SpanStyle>, Duration> =
        if (colorMapInitialized.get()) {
            colorMap to Duration.ZERO
        } else {
            // Preserve attribution semantics: if any non-engine path initialized colorMap first,
            // do not attribute parse time to HighlightEngine.
            if (colorMapLazy.isInitialized()) {
                colorMapInitialized.compareAndSet(false, true)
                colorMap to Duration.ZERO
            } else {
                val (map, duration) = measureTimedValue { colorMapLazy.value }
                if (colorMapInitialized.compareAndSet(false, true)) {
                    map to duration
                } else {
                    map to Duration.ZERO
                }
            }
        }

    /** Background color from the `.hljs` CSS rule. Unspecified if not present in theme. */
    val backgroundColor: Color by lazy {
        colorMap["hljs"]?.background?.takeIf { it != Color.Unspecified } ?: Color.Unspecified
    }

    /** Default text color from the `.hljs` CSS rule. Unspecified if not present in theme. */
    val defaultTextColor: Color by lazy {
        colorMap["hljs"]?.color?.takeIf { it != Color.Unspecified } ?: Color.Unspecified
    }

    /**
     * Two themes are equal when they have the same [name] **and** the same content identity.
     * This ensures that themes sharing a name but carrying different CSS or color-map content
     * are treated as distinct, so Compose recomposition keys (`remember`, `LaunchedEffect`)
     * correctly trigger a re-highlight when the theme content changes.
     */
    override fun equals(other: Any?): Boolean =
        other is HighlightTheme &&
            name == other.name &&
            contentIdentity == other.contentIdentity

    override fun hashCode(): Int = 31 * name.hashCode() + contentIdentity.hashCode()

    override fun toString(): String = "HighlightTheme(name=$name)"

    companion object {
        /**
         * Built-in Base16 Tomorrow light theme.
         *
         * The provided [context] is defensively normalized to `applicationContext` before being
         * retained by the lazy theme provider.
         *
         * @param context Any [Context]; normalized to `applicationContext` internally.
         * @return A [HighlightTheme] backed by the bundled `tomorrow.css`.
         */
        fun tomorrow(context: Context): HighlightTheme {
            val appContext = context.applicationContext
            val assetPath = "compose-highlight/themes/tomorrow.css"
            return HighlightTheme(
                name = "tomorrow",
                colorMapProvider = { ThemeParser.parseAsset(appContext, assetPath) },
                contentIdentity = contentDigest64("asset", assetPath),
            )
        }

        /**
         * Built-in Base16 Tomorrow Night dark theme.
         *
         * The provided [context] is defensively normalized to `applicationContext` before being
         * retained by the lazy theme provider.
         *
         * @param context Any [Context]; normalized to `applicationContext` internally.
         * @return A [HighlightTheme] backed by the bundled `tomorrow-night.css`.
         */
        fun tomorrowNight(context: Context): HighlightTheme {
            val appContext = context.applicationContext
            val assetPath = "compose-highlight/themes/tomorrow-night.css"
            return HighlightTheme(
                name = "tomorrow-night",
                colorMapProvider = { ThemeParser.parseAsset(appContext, assetPath) },
                contentIdentity = contentDigest64("asset", assetPath),
            )
        }

        /**
         * Built-in Atom One Dark theme.
         *
         * The provided [context] is defensively normalized to `applicationContext` before being
         * retained by the lazy theme provider.
         *
         * @param context Any [Context]; normalized to `applicationContext` internally.
         * @return A [HighlightTheme] backed by the bundled `atom-one-dark.css`.
         */
        fun atomOneDark(context: Context): HighlightTheme {
            val appContext = context.applicationContext
            val assetPath = "compose-highlight/themes/atom-one-dark.css"
            return HighlightTheme(
                name = "atom-one-dark",
                colorMapProvider = { ThemeParser.parseAsset(appContext, assetPath) },
                contentIdentity = contentDigest64("asset", assetPath),
            )
        }

        /**
         * Built-in Atom One Light theme.
         *
         * The provided [context] is defensively normalized to `applicationContext` before being
         * retained by the lazy theme provider.
         *
         * @param context Any [Context]; normalized to `applicationContext` internally.
         * @return A [HighlightTheme] backed by the bundled `atom-one-light.css`.
         */
        fun atomOneLight(context: Context): HighlightTheme {
            val appContext = context.applicationContext
            val assetPath = "compose-highlight/themes/atom-one-light.css"
            return HighlightTheme(
                name = "atom-one-light",
                colorMapProvider = { ThemeParser.parseAsset(appContext, assetPath) },
                contentIdentity = contentDigest64("asset", assetPath),
            )
        }

        /**
         * Custom theme loaded from a Highlight.js CSS file in the app's `assets/` folder.
         *
         * This is the recommended way for app developers to ship additional themes. Download any
         * `.css` file from the [Highlight.js styles](https://github.com/highlightjs/highlight.js/tree/main/src/styles)
         * directory, place it in `src/main/assets/`, and reference it here.
         *
         * ```kotlin
         * // src/main/assets/themes/github.css  ← place the CSS here
         * val theme = HighlightTheme.fromAsset(
         *     context   = context,
         *     assetPath = "themes/github.css",
         *     name      = "github",
         * )
         * HighlightThemeProvider(lightHighlightTheme = theme, ...) { ... }
         * ```
         *
         * Note: loading is lazy — the CSS is parsed on first use, not at factory-call time.
         * The provided [context] is defensively normalized to `applicationContext` before being
         * retained by the lazy theme provider.
         *
         * @throws [HighlightException.ThemeNotFound] if the asset file is found but contains no
         *   parseable color rules (e.g. it is empty or uses only unsupported CSS properties).
         * @throws java.io.IOException if the asset file cannot be opened (missing or unreadable).
         *   This exception is also thrown lazily, on first use of the theme.
         * @param context Any [Context]; normalized to `applicationContext` internally.
         * @param assetPath Path within `assets/` to the Highlight.js CSS file (e.g. `"themes/github.css"`).
         * @param name Display name for the theme.
         * @return A [HighlightTheme] whose color map is lazily parsed from [assetPath].
         */
        fun fromAsset(
            context: Context,
            assetPath: String,
            name: String,
        ): HighlightTheme {
            val appContext = context.applicationContext
            return HighlightTheme(
                name = name,
                colorMapProvider = {
                    val map = ThemeParser.parseAsset(appContext, assetPath)
                    if (map.isEmpty()) throw HighlightException.ThemeNotFound(assetPath)
                    map
                },
                contentIdentity = contentDigest64("asset", assetPath),
            )
        }

        /**
         * Custom theme from raw Highlight.js CSS text.
         *
         * Use this when you fetch or generate CSS at runtime rather than bundling it as an asset.
         *
         * ```kotlin
         * val css = // ... fetch from network or build programmatically
         * val theme = HighlightTheme.fromCss(
         *     cssText = css,
         *     name    = "my-runtime-theme",
         * )
         * ```
         *
         * @param cssText Raw Highlight.js-compatible CSS text.
         * @param name Display name for the theme.
         * @return A [HighlightTheme] whose color map is lazily parsed from [cssText].
         */
        fun fromCss(
            cssText: String,
            name: String,
        ): HighlightTheme =
            HighlightTheme(
                name = name,
                colorMapProvider = { ThemeParser.parse(cssText) },
                contentIdentity = contentDigest64("css", cssText),
            )

        /**
         * Custom theme from a precomputed color map.
         *
         * Use this when deriving colors from Material 3 dynamic color, app branding, or any
         * non-CSS source. The [colorMap] keys are Highlight.js class names without the leading
         * dot (e.g. `"hljs-keyword"`, `"hljs-string"`, `"hljs"`). The `"hljs"` entry is used
         * to derive [HighlightTheme.backgroundColor] and [HighlightTheme.defaultTextColor]; you
         * can also override those explicitly via [backgroundColor] and [defaultTextColor].
         *
         * ```kotlin
         * val colorMap = mapOf(
         *     "hljs"         to SpanStyle(color = Color(0xFF24292E), background = Color(0xFFFFFFFF)),
         *     "hljs-keyword" to SpanStyle(color = Color(0xFFD73A49), fontWeight = FontWeight.Bold),
         *     "hljs-string"  to SpanStyle(color = Color(0xFF032F62)),
         *     "hljs-comment" to SpanStyle(color = Color(0xFF6A737D), fontStyle = FontStyle.Italic),
         *     // ... add more token types as needed
         * )
         * val theme = HighlightTheme.fromColorMap(
         *     name             = "my-dynamic-theme",
         *     colorMap         = colorMap,
         *     backgroundColor  = Color(0xFFFFFFFF),
         *     defaultTextColor = Color(0xFF24292E),
         * )
         * ```
         *
         * @param name Display name for the theme.
         * @param colorMap Map of hljs class name → [SpanStyle].
         * @param backgroundColor Optional explicit background color. If null, derived from `colorMap["hljs"]`.
         * @param defaultTextColor Optional explicit default text color. If null, derived from `colorMap["hljs"]`.
         * @return A [HighlightTheme] backed by the provided [colorMap].
         */
        fun fromColorMap(
            name: String,
            colorMap: Map<String, SpanStyle>,
            backgroundColor: Color? = null,
            defaultTextColor: Color? = null,
        ): HighlightTheme {
            // Defensively copy so later mutations to the caller's map don't affect the theme.
            val immutableMap = colorMap.toMap()
            val effectiveColorMap =
                if (backgroundColor != null || defaultTextColor != null) {
                    val base = immutableMap.toMutableMap()
                    val existing = base["hljs"] ?: SpanStyle()
                    base["hljs"] =
                        existing.copy(
                            background = backgroundColor ?: existing.background,
                            color = defaultTextColor ?: existing.color,
                        )
                    base.toMap()
                } else {
                    immutableMap
                }
            val contentIdentity = contentDigest64(effectiveColorMap)
            return HighlightTheme(
                name = name,
                colorMapProvider = { effectiveColorMap },
                contentIdentity = contentIdentity,
            )
        }

        private fun contentDigest64(
            prefix: String,
            value: String,
        ): Long =
            digestToLong {
                update(prefix.toByteArray(Charsets.UTF_8))
                update(byteArrayOf(0))
                update(value.toByteArray(Charsets.UTF_8))
            }

        private fun contentDigest64(colorMap: Map<String, SpanStyle>): Long =
            digestToLong {
                update("colormap".toByteArray(Charsets.UTF_8))
                update(byteArrayOf(0))
                colorMap.toSortedMap().forEach { (selector, style) ->
                    update(selector.toByteArray(Charsets.UTF_8))
                    update(byteArrayOf(0))
                    update(style.toString().toByteArray(Charsets.UTF_8))
                    update(byteArrayOf(0))
                }
            }

        private inline fun digestToLong(updateDigest: MessageDigest.() -> Unit): Long {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.updateDigest()
            val bytes = digest.digest()
            return ByteBuffer.wrap(bytes, 0, Long.SIZE_BYTES).long
        }
    }
}
