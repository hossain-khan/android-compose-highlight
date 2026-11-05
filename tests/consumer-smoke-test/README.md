# Consumer Smoke Test Template

Static fixture project used by the `publishedArtifactConsumerCheck` Gradle task.

This project represents a clean, standalone Android application that depends only on the published
`dev.hossain:compose-highlight` artifact (without declaring transitive dependencies like Compose UI,
Compose Foundation, or Kotlinx Coroutines).

The `PublishedArtifactConsumerCheckTask` task in `buildSrc`:

1. Copies this template into `build/consumer-smoke-test`.
2. Substitutes placeholders for the local Maven repository URL and dependency versions.
3. Compiles both `assembleDebug` and minified `assembleRelease` to verify runtime type visibility and R8 shrinking rules.
