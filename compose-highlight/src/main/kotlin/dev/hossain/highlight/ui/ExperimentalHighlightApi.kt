package dev.hossain.highlight.ui

/**
 * Marks an API as experimental within the `compose-highlight` library.
 *
 * Experimental APIs may change signature, behavior, or be removed in any future release
 * without a deprecation cycle. They are provided for early feedback and are not yet
 * considered stable for production use.
 *
 * To use an API annotated with `@ExperimentalHighlightApi`, opt in at the call site:
 *
 * ```kotlin
 * // Opt in for a single composable or function:
 * @OptIn(ExperimentalHighlightApi::class)
 * @Composable
 * fun MyScreen() {
 *     SyntaxHighlightedTextEditor(...)
 * }
 * ```
 *
 * Or propagate the requirement to your own API:
 *
 * ```kotlin
 * @ExperimentalHighlightApi
 * @Composable
 * fun MyEditorScreen() {
 *     SyntaxHighlightedTextEditor(...)
 * }
 * ```
 *
 * To opt in for an entire file, add this to the top of the file (before the package statement):
 *
 * ```kotlin
 * @file:OptIn(ExperimentalHighlightApi::class)
 * ```
 */
@RequiresOptIn(
    message =
        "This API is part of the experimental compose-highlight surface and may change " +
            "or be removed in future releases without a deprecation cycle.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
)
annotation class ExperimentalHighlightApi
