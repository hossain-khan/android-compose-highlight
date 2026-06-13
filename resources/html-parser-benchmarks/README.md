# HTML Parser Benchmark Data

This directory contains benchmark JSON reports from `HtmlParserBenchmark`, used to
track performance across parser implementation changes.

## How benchmarks are run

```bash
./gradlew :compose-highlight:testDebugUnitTest \
  --tests "dev.hossain.highlight.benchmark.HtmlParserBenchmark" \
  -PrunBenchmark=true --rerun-tasks
```

Each run produces a JSON report at
`compose-highlight/build/reports/benchmarks/html-parser-baseline-<epoch-ms>.json`
with mean/stddev/min/max in microseconds for all 12 benchmark methods
(6 single-theme `convert` + 6 dual-theme `convertBothThemes`).

See the `AGENTS.md` section on benchmarking for full details.

## Configuration

All reports in this directory use:

- **Warmup iterations:** 100 (lets JIT compile the hot path)
- **Measurement iterations:** 50

## Fixtures

Six real-world highlight.js HTML outputs under
`compose-highlight/src/test/resources/highlight-fixtures/`:

| Fixture | Size |
| --- | --- |
| real-c.html | 76 KB |
| real-rust.html | 63 KB |
| real-kotlin.html | 57 KB |
| real-go.html | 31 KB |
| real-sql.html | 24 KB |
| real-csharp.html | 8 KB |

## Reports

### jsoup-baseline-run{1,2,3}.json

**Date:** 2026-06-13
**Parser:** Jsoup-based HTML parser (pre-custom parser).
This was the original implementation that parsed the HTML using Jsoup's complete DOM tree
and walked all nodes.

### baseline-pre-sax-optimization.json

**Date:** 2026-06-13
**Parser:** Two-phase custom parser pipeline.
`parseHtml()` builds an intermediate `CustomNode` tree, then `walkNode()` traverses the tree
to build `AnnotatedString`.

### sax-optimized-run{1,2,3}.json

**Date:** 2026-06-13
**Parser:** SAX-style single-pass custom parser.
`parseAndBuild()`/`parseAndBuildBoth()` parse HTML and build `AnnotatedString` simultaneously,
eliminating the intermediate tree. Also includes substring avoidance, in-place attribute
extraction, lazy entity decoding, and allocation-free numeric parsing.

Three consecutive runs are saved for both Jsoup and SAX-optimized versions to verify stability.

## Three-Way Comparison (best of 3 runs vs baseline)

All values are in microseconds (us). Lower is better.

| Benchmark | Jsoup (us) | Custom (us) | SAX (us) | Jsoup->SAX | Custom->SAX |
| --- | --- | --- | --- | --- | --- |
| convertBothC | 897.86 | 504.26 | 324.91 | **-63.8%** | **-35.6%** |
| convertBothCsharp | 168.59 | 60.98 | 42.00 | **-75.1%** | **-31.1%** |
| convertBothGo | 410.82 | 223.12 | 147.63 | **-64.1%** | **-33.8%** |
| convertBothKotlin | 600.56 | 294.72 | 278.72 | **-53.6%** | **-5.4%** |
| convertBothRust | 235.50 | 211.98 | 159.74 | **-32.2%** | **-24.6%** |
| convertBothSql | 996.30 | 417.96 | 254.90 | **-74.4%** | **-39.0%** |
| convertC | 1381.68 | 496.30 | 354.64 | **-74.3%** | **-28.5%** |
| convertCsharp | 80.98 | 42.92 | 20.92 | **-74.2%** | **-51.3%** |
| convertGo | 581.30 | 205.22 | 94.21 | **-83.8%** | **-54.1%** |
| convertKotlin | 420.75 | 419.64 | 178.91 | **-57.5%** | **-57.4%** |
| convertRust | 346.67 | 353.91 | 192.67 | **-44.4%** | **-45.6%** |
| convertSql | 257.85 | 160.04 | 92.62 | **-64.1%** | **-42.1%** |

## Diffing reports

Use `jq` to compare reports side-by-side:

```bash
# Extract mean times as a sorted table
jq -r '.benchmarks | sort_by(.name) | .[] | "\(.name)\t\(.meanUs)"' \
  resources/html-parser-benchmarks/jsoup-baseline-run1.json

jq -r '.benchmarks | sort_by(.name) | .[] | "\(.name)\t\(.meanUs)"' \
  resources/html-parser-benchmarks/baseline-pre-sax-optimization.json

jq -r '.benchmarks | sort_by(.name) | .[] | "\(.name)\t\(.meanUs)"' \
  resources/html-parser-benchmarks/sax-optimized-run3.json
```
