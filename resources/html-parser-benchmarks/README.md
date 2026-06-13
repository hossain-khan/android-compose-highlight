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
|---------|------|
| real-c.html | 76 KB |
| real-rust.html | 63 KB |
| real-kotlin.html | 57 KB |
| real-go.html | 31 KB |
| real-sql.html | 24 KB |
| real-csharp.html | 8 KB |

## Reports

### baseline-pre-sax-optimization.json

**Date:** 2026-06-13
**Parser:** Two-phase pipeline - `parseHtml()` builds intermediate `CustomNode` tree,
then `walkNode()` traverses the tree to build `AnnotatedString`.

This is the baseline before the SAX-style optimization.

### sax-optimized-run{1,2,3}.json

**Date:** 2026-06-13
**Parser:** SAX-style single-pass - `parseAndBuild()`/`parseAndBuildBoth()` parse HTML
and build `AnnotatedString` simultaneously, eliminating the intermediate tree. Also
includes substring avoidance, in-place attribute extraction, lazy entity decoding,
and allocation-free numeric parsing.

Three consecutive runs to verify stability.

### Summary comparison (best of 3 optimized runs vs baseline)

| Benchmark | Baseline (us) | Optimized (us) | Improvement |
|-----------|--------------|----------------|-------------|
| convertKotlin | 419.64 | 178.91 | **-57%** |
| convertGo | 205.22 | 94.21 | **-54%** |
| convertRust | 353.91 | 192.67 | **-46%** |
| convertCsharp | 42.92 | 23.02 | **-46%** |
| convertSql | 160.04 | 104.68 | **-35%** |
| convertC | 496.30 | 354.64 | **-29%** |
| convertBothSql | 417.96 | 254.90 | **-39%** |
| convertBothC | 504.26 | 331.89 | **-34%** |
| convertBothGo | 223.12 | 151.85 | **-32%** |
| convertBothCsharp | 60.98 | 42.00 | **-31%** |
| convertBothRust | 211.98 | 159.74 | **-25%** |
| convertBothKotlin | 294.72 | 278.72 | **-5%** |

## Diffing reports

Use `jq` to compare two reports side-by-side:

```bash
# Extract mean times as a sorted table
jq -r '.benchmarks | sort_by(.name) | .[] | "\(.name)\t\(.meanUs)"' \
  resources/html-parser-benchmarks/baseline-pre-sax-optimization.json

jq -r '.benchmarks | sort_by(.name) | .[] | "\(.name)\t\(.meanUs)"' \
  resources/html-parser-benchmarks/sax-optimized-run3.json
```
