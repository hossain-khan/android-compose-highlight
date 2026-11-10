# Guide: Tracking Library Consumers Across GitHub

This guide documents how to identify external projects and applications consuming
`dev.hossain:compose-highlight`, why GitHub's built-in dependency graph is insufficient,
and how to run and refresh the automated usage report.

---

## 1. The Challenge: Why GitHub Dependents Shows "0 Repositories"

If you navigate to GitHub's native Dependents tab:

```text
https://github.com/hossain-khan/android-compose-highlight/network/dependents
```

GitHub displays:

> *"We haven't found any dependents for this repository yet."*

### Why this happens

GitHub's automated dependency graph only indexes Maven/Gradle dependencies if repositories
submit explicit **Dependency Submission snapshots (SBOMs)** via GitHub Actions. Standard
Android Gradle builds downloading artifacts from Maven Central (`dev.hossain:compose-highlight:<version>`)
are **not** parsed into the network graph automatically.

As a result, major production apps (including [`T8RIN/ImageToolbox`](https://github.com/T8RIN/ImageToolbox)
with **15,500+ stars**) do not appear in the dependents tab even though they actively depend on
the library in production.

---

## 2. Comprehensive Identification Strategy (Multi-Vector Code Search)

To reliably discover real-world consumers, we use a 3-vector search strategy across GitHub:

### Vector 1: Maven Coordinate Declarations

Searches for direct dependency declarations across `build.gradle`, `build.gradle.kts`, `pom.xml`, and TOML catalogs:

```bash
gh search code "dev.hossain:compose-highlight"
```

To exclude the author's own repositories and see only third-party adoption:

```bash
gh search code "dev.hossain:compose-highlight NOT owner:hossain-khan"
```

### Vector 2: Package Imports

Searches for actual usage in Kotlin source files (`import dev.hossain.highlight...`):

```bash
gh search code "import dev.hossain.highlight"
```

### Vector 3: Version Catalog Module Declarations

Searches for Gradle version catalog references in `libs.versions.toml`:

```bash
gh search code "dev.hossain extension:toml"
```

---

## 3. Automated Usage Report Tool

The repository provides an automated, reproducible Python script to scan GitHub, enrich metadata,
detect library versions, and generate both an interactive single-page HTML report and Markdown documentation:

```text
scripts/generate-consumer-report.py
```

### Generated Artifacts

- **Interactive Single-Page HTML Report:** [`resources/consumer-report/index.html`](./consumer-report/index.html)
- **Markdown Summary:** [`resources/consumer-report/README.md`](./consumer-report/README.md)
- **Raw JSON Dataset:** [`resources/consumer-report/data.json`](./consumer-report/data.json)

### Usage

```bash
# 1. Full live refresh from GitHub (uses gh CLI)
python3 scripts/generate-consumer-report.py --refresh

# 2. Regenerate HTML/MD from cached dataset without hitting GitHub API
python3 scripts/generate-consumer-report.py --offline

# 3. View the single-page HTML site
open resources/consumer-report/index.html
```

### How the Script Works

1. **Rate Limit Safety:** Checks GitHub Code Search quota (`code_search` resource)
   and automatically pauses if rate limits are reached.
2. **Multi-Vector Discovery:** Executes targeted search queries to capture build declarations,
   package imports, and version catalogs.
3. **Metadata Enrichment:** Queries the GitHub REST API (`/repos/{owner}/{repo}`) using standard
   core quota (5,000 req/hr) to retrieve stars, forks, license, and repository description.
4. **Version Detection:** Inspects remote build files (`gradle/libs.versions.toml`, `build.gradle.kts`)
   to extract the exact `compose-highlight` release version deployed by each consumer.
5. **Report Generation:** Generates a standalone, responsive HTML page with dark/light mode,
   live search, category filtering, sortable columns, and metric cards.

---

## 4. Current Adoption Snapshot

| Consumer | Stars | Version | Role / Application |
| --- | --- | --- | --- |
| [`T8RIN/ImageToolbox`](https://github.com/T8RIN/ImageToolbox) | 15,500+ | `0.41.1` | Flagship image manipulation app. Uses `rememberHighlightedCode` in Shader Studio and Code Preview. |
| [`mouse0329/nezumi-ai`](https://github.com/mouse0329/nezumi-ai) | 3 | `0.40.0` | Privacy-focused offline AI chat app. Uses `HighlightThemeProvider` and `SyntaxHighlightedCode`. |
| [`jasongzy/s3pocket`](https://github.com/jasongzy/s3pocket) | 0 | `0.32.0` | Android S3 client. Uses `rememberHighlightedCode` in `ObjectDetailsScreen` for cloud preview. |
| [`musabadru/HiJean`](https://github.com/musabadru/HiJean) | 0 | `0.32.0` | Islamic Android app with Jetpack Compose. |
| [`musabadru/android-compose-template`](https://github.com/musabadru/android-compose-template) | 0 | `0.32.0` | Modern Jetpack Compose starter template recommending `compose-highlight`. |
| [`hossain-khan/android-hi-light`](https://github.com/hossain-khan/android-hi-light) | Google Play | `0.41.1` | Snippet - official showcase and personal code notebook app. |
| [`hossain-khan/android-code-with-ai`](https://github.com/hossain-khan/android-code-with-ai) | - | `0.39.0` | CodeMateX on-device AI coding tutor. |
| [`hossain-khan/android-remote-notify`](https://github.com/hossain-khan/android-remote-notify) | 8 | `0.32.0` | Remote notification utility with JSON syntax highlighting. |
