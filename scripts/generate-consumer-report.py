#!/usr/bin/env python3
"""
Generate Consumer Usage Report for android-compose-highlight

This script searches GitHub for real-world repositories and applications that consume
`dev.hossain:compose-highlight` (via Maven coordinate declarations, version catalogs,
or package imports) and generates:
  1. A structured JSON dataset (resources/consumer-report/data.json)
  2. A single-page interactive HTML report (resources/consumer-report/index.html)
  3. A companion Markdown summary (resources/consumer-report/README.md)

Why this script is necessary:
  GitHub's automated dependency graph (/network/dependents) does not automatically
  index Maven Central dependencies for Gradle unless repositories submit explicit
  dependency submission snapshots (SBOMs). Consequently, GitHub Dependents shows 0
  repositories even though prominent apps (such as ImageToolbox with 15,500+ stars)
  depend on compose-highlight. GitHub Code Search is the true authoritative source.

Usage:
  # Refresh data from GitHub and regenerate reports
  python3 scripts/generate-consumer-report.py --refresh

  # Regenerate reports using cached data (no GitHub API calls)
  python3 scripts/generate-consumer-report.py --offline

  # Custom output paths
  python3 scripts/generate-consumer-report.py --output-html custom.html
"""

import argparse
import base64
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

THIS_REPO = "hossain-khan/android-compose-highlight"
DEFAULT_JSON = "resources/consumer-report/data.json"
DEFAULT_HTML = "resources/consumer-report/index.html"
DEFAULT_MD = "resources/consumer-report/README.md"

SEARCH_QUERIES = [
    "dev.hossain:compose-highlight",
    "import dev.hossain.highlight",
    "dev.hossain extension:toml",
]

# Curated metadata notes for prominent consumers
CURATED_NOTES = {
    "T8RIN/ImageToolbox": {
        "title": "Image Toolbox",
        "category": "Third-Party App",
        "highlight": "Flagship 15,500+ star image manipulation tool for Android. Uses compose-highlight for live GLSL shader editing in Shader Studio and code preview.",
        "verified_usage": "Uses rememberHighlightedCode and custom VisualTransformation in Shader Studio and Code Preview components.",
    },
    "mouse0329/nezumi-ai": {
        "title": "Nezumi AI",
        "category": "Third-Party App",
        "highlight": "Privacy-focused offline Android AI chat application. Uses HighlightThemeProvider and SyntaxHighlightedCode to render markdown code responses.",
        "verified_usage": "Renders code blocks inside LatexText composable with HighlightThemeProvider and SyntaxHighlightedCode.",
    },
    "jasongzy/s3pocket": {
        "title": "S3 Pocket",
        "category": "Third-Party App",
        "highlight": "Native Android client for S3-compatible cloud storage. Uses compose-highlight to display syntax-highlighted previews of stored code files.",
        "verified_usage": "Uses rememberHighlightedCode in ObjectDetailsScreen to preview text/code objects stored in S3.",
    },
    "musabadru/HiJean": {
        "title": "HiJean",
        "category": "Third-Party App",
        "highlight": "Islamic Android application built with modern Jetpack Compose architecture.",
        "verified_usage": "Declares compose-highlight in gradle/libs.versions.toml version catalog.",
    },
    "musabadru/android-compose-template": {
        "title": "Android Compose Template",
        "category": "Third-Party Template",
        "highlight": "Modern Jetpack Compose application starter template with dynamic features and modular architecture.",
        "verified_usage": "Recommends and includes compose-highlight in gradle/libs.versions.toml version catalog.",
    },
    "hossain-khan/android-hi-light": {
        "title": "Snippet (HiLight)",
        "category": "First-Party Showcase",
        "highlight": "Official flagship demo app and personal code notebook published on Google Play. Features live interactive showcase gallery for all library composables.",
        "verified_usage": "Deep integration across 33 files; exercises SyntaxHighlightedCode, StreamingSyntaxHighlightedCode, and SyntaxHighlightedTextEditor.",
    },
    "hossain-khan/android-code-with-ai": {
        "title": "CodeMateX",
        "category": "First-Party App",
        "highlight": "On-device AI coding tutor app for Android leveraging Gemini Nano. Uses compose-highlight to render code blocks in AI chat bubbles and lessons.",
        "verified_usage": "Uses compose-highlight with multiplatform-markdown-renderer in ChatMessageBubble and LessonScreenUi.",
    },
    "hossain-khan/android-remote-notify": {
        "title": "Remote Notify",
        "category": "First-Party App",
        "highlight": "Remote notification utility app published on Google Play with JSON syntax highlighting for notification payloads.",
        "verified_usage": "Uses compose-highlight in ConfigureNotificationMediumScreen to highlight alert payload configurations.",
    },
    "hossain-khan/kids-math-tutor": {
        "title": "Math Pup",
        "category": "First-Party App",
        "highlight": "Educational Android math tutor app for children with code import challenge UI.",
        "verified_usage": "Uses compose-highlight in ImportChallengeUi.",
    },
    "hossain-khan/android-syntax-highlighter-compose": {
        "title": "Syntax Highlighter Benchmark",
        "category": "First-Party Benchmark",
        "highlight": "Interactive benchmarking and comparison suite evaluating compose-highlight against other mobile syntax highlighters.",
        "verified_usage": "Runs side-by-side performance and APK size comparisons between syntax highlighting approaches.",
    },
    "hossain-khan/android-compose-highlight-test-harness": {
        "title": "Integration Test Harness",
        "category": "First-Party Test Harness",
        "highlight": "Dedicated Android integration test harness validating library behavior across different themes, languages, and screen orientations.",
        "verified_usage": "Houses 14 test screens verifying Compose lifecycle, WebView prewarming, and editor debounce.",
    },
    "hossain-khan/hossains-dev-bytes": {
        "title": "Dev Bytes Tech Blog",
        "category": "First-Party Documentation",
        "highlight": "Technical blog article exploring streaming syntax highlighting architecture for LLMs in Jetpack Compose.",
        "verified_usage": "Full technical deep dive article on compose-highlight streaming architecture.",
    },
    "hossain-khan/neon": {
        "title": "Neon (KMP)",
        "category": "First-Party Research",
        "highlight": "Next-generation Kotlin Multiplatform syntax highlighting project planning backward-compatible migration.",
        "verified_usage": "PRD documents migration and architectural lineage from android-compose-highlight.",
    },
}


def run_gh(command: str) -> str:
    """Run a gh command in a clean subshell."""
    full_cmd = ["zsh", "-f", "-c", f"gh {command}"]
    res = subprocess.run(full_cmd, capture_output=True, text=True)
    if res.returncode != 0:
        raise RuntimeError(f"Command failed (exit {res.returncode}): gh {command}\nError: {res.stderr.strip()}")
    return res.stdout.strip()


def check_rate_limit() -> None:
    """Ensure sufficient code_search and core rate limit quota exists."""
    try:
        raw = run_gh("api rate_limit")
        data = json.loads(raw)
        cs = data.get("resources", {}).get("code_search", {})
        rem = cs.get("remaining", 10)
        reset_epoch = cs.get("reset", 0)
        if rem == 0:
            wait_sec = max(1, int(reset_epoch - time.time()) + 2)
            print(f"[RateLimit] code_search remaining is 0. Waiting {wait_sec}s for reset window...", file=sys.stderr)
            time.sleep(wait_sec)
    except Exception as e:
        print(f"[RateLimit] Warning: could not check rate limit: {e}", file=sys.stderr)


def search_github() -> dict:
    """Execute search queries and aggregate file occurrences by repository."""
    repo_files: dict[str, set[str]] = {}
    print("[Discovery] Running GitHub code search queries...")

    for query in SEARCH_QUERIES:
        check_rate_limit()
        print(f"  * Searching: {query}")
        try:
            raw = run_gh(f'search code "{query}" --json path,repository,url --limit 100')
            items = json.loads(raw)
            for item in items:
                repo_name = item.get("repository", {}).get("nameWithOwner")
                path = item.get("path")
                if repo_name and path and repo_name != THIS_REPO:
                    repo_files.setdefault(repo_name, set()).add(path)
        except Exception as e:
            print(f"    Warning: query '{query}' encountered error: {e}", file=sys.stderr)

    return repo_files


def fetch_repo_metadata(repo_name: str) -> dict:
    """Fetch repository metadata via GitHub REST API."""
    try:
        raw = run_gh(f"api /repos/{repo_name}")
        return json.loads(raw)
    except Exception as e:
        print(f"  Warning: could not fetch repo metadata for {repo_name}: {e}", file=sys.stderr)
        return {}


def fetch_file_content(repo_name: str, path: str) -> str | None:
    """Fetch raw file content from a repository."""
    try:
        raw = run_gh(f"api /repos/{repo_name}/contents/{path}")
        data = json.loads(raw)
        content_b64 = data.get("content")
        if content_b64:
            return base64.b64decode(content_b64).decode("utf-8", errors="ignore")
    except Exception:
        pass
    return None


def extract_version_from_content(content: str) -> str | None:
    """Extract library version from Gradle or TOML files."""
    patterns = [
        r'compose[Hh]ighlight(?:Version)?\s*=\s*["\']([^"\']+)["\']',
        r'compose-highlight\s*=\s*\{[^}]*version\.ref\s*=\s*["\']([^"\']+)["\']',
        r'dev\.hossain:compose-highlight:([^"\']+)["\']',
        r'dev\.hossain["\'],\s*name\s*=\s*["\']compose-highlight["\'],\s*version\.ref\s*=\s*["\']([^"\']+)["\']',
        r'version\.ref\s*=\s*["\']([^"\']+)["\']',
    ]
    for pat in patterns:
        m = re.search(pat, content)
        if m:
            val = m.group(1)
            # If val looks like a version reference, look up its definition in content
            if not re.match(r"^\d+\.\d+", val):
                ref_m = re.search(rf'{re.escape(val)}\s*=\s*["\']([^"\']+)["\']', content)
                if ref_m:
                    return ref_m.group(1)
            else:
                return val
    return None


def detect_version(repo_name: str, file_paths: list[str]) -> tuple[str, str | None]:
    """Inspect build files to detect the compose-highlight version in use."""
    candidate_paths = [
        "gradle/libs.versions.toml",
        "app/build.gradle.kts",
        "build.gradle.kts",
        "app/build.gradle",
        "build.gradle",
    ]
    # Check known candidate paths first, then any other toml/gradle files
    ordered = [p for p in candidate_paths if p in file_paths]
    for p in file_paths:
        if (p.endswith(".toml") or p.endswith(".gradle") or p.endswith(".gradle.kts")) and p not in ordered:
            ordered.append(p)

    for p in ordered:
        content = fetch_file_content(repo_name, p)
        if content:
            ver = extract_version_from_content(content)
            if ver:
                return ver, p

    return "Unknown", None


def build_dataset(repo_files: dict[str, set[str]]) -> dict:
    """Enrich discovered repositories into a complete report dataset."""
    consumers = []
    print(f"\n[Enrichment] Processing {len(repo_files)} consumer repositories...")

    for repo_name, paths in sorted(repo_files.items()):
        print(f"  -> Enriching: {repo_name} ({len(paths)} matched files)")
        meta = fetch_repo_metadata(repo_name)
        paths_list = sorted(paths)
        version, ver_file = detect_version(repo_name, paths_list)

        curated = CURATED_NOTES.get(repo_name, {})
        is_third_party = not repo_name.startswith("hossain-khan/")

        # Categorize
        if is_third_party:
            category = curated.get("category", "Third-Party App")
            category_group = "3rd-party"
        else:
            category = curated.get("category", "First-Party Companion")
            category_group = "first-party"

        stars = meta.get("stargazers_count", 0)
        forks = meta.get("forks_count", 0)
        open_issues = meta.get("open_issues_count", 0)
        desc = meta.get("description") or curated.get("highlight", "No description provided.")
        license_info = meta.get("license") or {}
        license_spdx = license_info.get("spdx_id") or license_info.get("name") or "None"
        pushed_at = meta.get("pushed_at", "")
        updated_at = meta.get("updated_at", "")
        owner_avatar = meta.get("owner", {}).get("avatar_url", "")
        html_url = meta.get("html_url") or f"https://github.com/{repo_name}"

        consumers.append({
            "repo": repo_name,
            "title": curated.get("title", repo_name.split("/")[-1]),
            "url": html_url,
            "description": desc,
            "category": category,
            "category_group": category_group,
            "stars": stars,
            "forks": forks,
            "open_issues": open_issues,
            "license": license_spdx,
            "language": meta.get("language") or "Kotlin",
            "detected_version": version,
            "version_file": ver_file,
            "files_count": len(paths_list),
            "files": paths_list,
            "highlight": curated.get("highlight", desc),
            "verified_usage": curated.get("verified_usage", ""),
            "updated_at": updated_at,
            "pushed_at": pushed_at,
            "avatar_url": owner_avatar,
        })

    # Sort consumers: 3rd party first by stars descending, then first party by stars descending
    consumers.sort(key=lambda c: (0 if c["category_group"] == "3rd-party" else 1, -c["stars"], c["repo"]))

    total_stars = sum(c["stars"] for c in consumers)
    third_party_count = sum(1 for c in consumers if c["category_group"] == "3rd-party")
    first_party_count = sum(1 for c in consumers if c["category_group"] == "first-party")

    # Distinct versions
    versions = {}
    for c in consumers:
        v = c["detected_version"]
        if v and v != "Unknown":
            versions[v] = versions.get(v, 0) + 1

    latest_version = max(versions.keys(), key=lambda v: [int(x) if x.isdigit() else 0 for x in v.split(".")]) if versions else "N/A"

    data = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "repository": THIS_REPO,
        "summary": {
            "total_consumers": len(consumers),
            "third_party_consumers": third_party_count,
            "first_party_consumers": first_party_count,
            "total_consumer_stars": total_stars,
            "latest_detected_version": latest_version,
            "version_distribution": versions,
        },
        "consumers": consumers,
    }
    return data


def render_html(data: dict) -> str:
    """Generate the complete single-page interactive HTML report."""
    summary = data["summary"]
    consumers = data["consumers"]
    gen_time = data["generated_at"][:10]

    # JSON for client-side search/filter
    consumers_json = json.dumps(consumers)

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Consumer Usage Report - compose-highlight</title>
<style>
  :root {{
    --bg: #ffffff;
    --bg-card: #f8f9fa;
    --bg-card-hover: #f1f3f5;
    --border: #e5e7eb;
    --text-primary: #111827;
    --text-secondary: #4b5563;
    --text-muted: #6b7280;
    --accent: #2563eb;
    --accent-hover: #1d4ed8;
    --accent-light: #eff6ff;
    --badge-bg: #e0f2fe;
    --badge-text: #0369a1;
    --success-bg: #dcfce7;
    --success-text: #15803d;
    --pill-bg: #f3f4f6;
    --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    --font-mono: "SF Mono", "Fira Code", Menlo, Consolas, monospace;
    --shadow: 0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04);
    --shadow-md: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -1px rgba(0,0,0,0.06);
  }}

  @media (prefers-color-scheme: dark) {{
    :root {{
      --bg: #0d1117;
      --bg-card: #161b22;
      --bg-card-hover: #1f242c;
      --border: #30363d;
      --text-primary: #f0f6fc;
      --text-secondary: #8b949e;
      --text-muted: #6e7681;
      --accent: #58a6ff;
      --accent-hover: #79c0ff;
      --accent-light: rgba(56, 139, 253, 0.15);
      --badge-bg: rgba(56, 139, 253, 0.2);
      --badge-text: #79c0ff;
      --success-bg: rgba(46, 160, 67, 0.2);
      --success-text: #3fb950;
      --pill-bg: #21262d;
      --shadow: 0 1px 3px rgba(0,0,0,0.4);
      --shadow-md: 0 4px 8px rgba(0,0,0,0.5);
    }}
  }}

  *, *::before, *::after {{ box-sizing: border-box; margin: 0; padding: 0; }}

  body {{
    font-family: var(--font-sans);
    background: var(--bg);
    color: var(--text-primary);
    line-height: 1.6;
    padding: 32px 20px 80px;
    max-width: 1200px;
    margin: 0 auto;
    -webkit-font-smoothing: antialiased;
  }}

  header {{
    border-bottom: 1px solid var(--border);
    padding-bottom: 24px;
    margin-bottom: 32px;
  }}

  .badge-library {{
    display: inline-block;
    font-size: 0.78rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    background: var(--accent-light);
    color: var(--accent);
    padding: 3px 10px;
    border-radius: 9999px;
    margin-bottom: 12px;
  }}

  h1 {{
    font-size: 2.2rem;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: var(--text-primary);
    margin-bottom: 8px;
  }}

  .subtitle {{
    font-size: 1.05rem;
    color: var(--text-secondary);
    max-width: 800px;
  }}

  .meta-bar {{
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 16px;
    margin-top: 14px;
    font-size: 0.88rem;
    color: var(--text-muted);
  }}

  .meta-bar a {{
    color: var(--accent);
    text-decoration: none;
  }}
  .meta-bar a:hover {{ text-decoration: underline; }}

  /* Metric cards */
  .metrics-grid {{
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
    margin-bottom: 36px;
  }}

  .metric-card {{
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: 10px;
    padding: 20px;
    box-shadow: var(--shadow);
    transition: transform 0.15s ease, box-shadow 0.15s ease;
  }}
  .metric-card:hover {{
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }}

  .metric-label {{
    font-size: 0.82rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--text-muted);
    margin-bottom: 6px;
  }}

  .metric-value {{
    font-size: 2.1rem;
    font-weight: 700;
    color: var(--text-primary);
    line-height: 1.2;
  }}

  .metric-sub {{
    font-size: 0.82rem;
    color: var(--text-secondary);
    margin-top: 4px;
  }}

  /* Featured Callout Banner */
  .featured-banner {{
    background: linear-gradient(135deg, var(--accent-light) 0%, var(--bg-card) 100%);
    border: 1px solid var(--accent);
    border-radius: 12px;
    padding: 24px 28px;
    margin-bottom: 36px;
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    align-items: center;
    gap: 20px;
  }}

  .featured-content {{
    max-width: 800px;
  }}

  .featured-badge {{
    display: inline-block;
    font-size: 0.72rem;
    font-weight: 700;
    background: var(--accent);
    color: #fff;
    padding: 2px 8px;
    border-radius: 4px;
    margin-bottom: 8px;
  }}

  .featured-title {{
    font-size: 1.35rem;
    font-weight: 700;
    margin-bottom: 6px;
  }}

  .featured-desc {{
    font-size: 0.95rem;
    color: var(--text-secondary);
  }}

  .featured-stats {{
    display: flex;
    gap: 20px;
    text-align: right;
  }}

  .featured-stat-val {{
    font-size: 1.8rem;
    font-weight: 700;
    color: var(--accent);
  }}
  .featured-stat-label {{
    font-size: 0.75rem;
    text-transform: uppercase;
    color: var(--text-muted);
  }}

  /* Filter and Controls Toolbar */
  .toolbar {{
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    margin-bottom: 24px;
    padding: 14px 18px;
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: 8px;
  }}

  .filter-tabs {{
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }}

  .tab-btn {{
    background: transparent;
    border: 1px solid var(--border);
    color: var(--text-secondary);
    padding: 6px 14px;
    border-radius: 6px;
    font-size: 0.88rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
  }}

  .tab-btn:hover {{
    background: var(--bg-card-hover);
    color: var(--text-primary);
  }}

  .tab-btn.active {{
    background: var(--accent);
    color: #fff;
    border-color: var(--accent);
  }}

  .search-box {{
    display: flex;
    align-items: center;
    gap: 10px;
  }}

  .search-input {{
    background: var(--bg);
    border: 1px solid var(--border);
    color: var(--text-primary);
    padding: 6px 12px;
    border-radius: 6px;
    font-size: 0.88rem;
    width: 240px;
    outline: none;
  }}
  .search-input:focus {{
    border-color: var(--accent);
  }}

  .sort-select {{
    background: var(--bg);
    border: 1px solid var(--border);
    color: var(--text-primary);
    padding: 6px 10px;
    border-radius: 6px;
    font-size: 0.88rem;
    outline: none;
    cursor: pointer;
  }}

  /* Consumer Table */
  .table-container {{
    border: 1px solid var(--border);
    border-radius: 8px;
    overflow-x: auto;
    background: var(--bg-card);
    box-shadow: var(--shadow);
    margin-bottom: 40px;
  }}

  table {{
    width: 100%;
    border-collapse: collapse;
    font-size: 0.92rem;
    text-align: left;
  }}

  th {{
    background: var(--pill-bg);
    color: var(--text-muted);
    font-weight: 600;
    font-size: 0.78rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    padding: 12px 16px;
    border-bottom: 1px solid var(--border);
    white-space: nowrap;
  }}

  td {{
    padding: 14px 16px;
    border-bottom: 1px solid var(--border);
    vertical-align: top;
  }}

  tr:last-child td {{
    border-bottom: none;
  }}

  tr:hover td {{
    background: var(--bg-card-hover);
  }}

  .repo-title-row {{
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 4px;
  }}

  .repo-name {{
    font-weight: 600;
    color: var(--text-primary);
    font-size: 0.98rem;
  }}

  .repo-link {{
    color: var(--accent);
    text-decoration: none;
  }}
  .repo-link:hover {{ text-decoration: underline; }}

  .repo-desc {{
    font-size: 0.84rem;
    color: var(--text-secondary);
    line-height: 1.5;
    max-width: 480px;
  }}

  .badge {{
    display: inline-block;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 0.75rem;
    font-weight: 600;
    white-space: nowrap;
  }}

  .badge-3rd {{
    background: var(--success-bg);
    color: var(--success-text);
  }}

  .badge-first {{
    background: var(--badge-bg);
    color: var(--badge-text);
  }}

  .badge-version {{
    background: var(--pill-bg);
    color: var(--text-primary);
    font-family: var(--font-mono);
    font-size: 0.82rem;
    border: 1px solid var(--border);
  }}

  .badge-stars {{
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-weight: 600;
    color: var(--text-primary);
  }}

  .files-list {{
    font-size: 0.78rem;
    color: var(--text-muted);
    font-family: var(--font-mono);
    max-width: 320px;
    line-height: 1.4;
  }}
  .files-list summary {{
    cursor: pointer;
    color: var(--accent);
    margin-bottom: 4px;
    font-family: var(--font-sans);
  }}

  /* Methodology section */
  .methodology {{
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: 10px;
    padding: 28px;
    margin-top: 48px;
  }}

  .methodology h2 {{
    font-size: 1.35rem;
    margin-bottom: 12px;
  }}

  .methodology p {{
    font-size: 0.92rem;
    color: var(--text-secondary);
    margin-bottom: 14px;
  }}

  pre {{
    background: var(--bg);
    border: 1px solid var(--border);
    border-radius: 6px;
    padding: 12px 16px;
    font-family: var(--font-mono);
    font-size: 0.85rem;
    overflow-x: auto;
    margin-bottom: 14px;
  }}

  code {{
    font-family: var(--font-mono);
    background: var(--pill-bg);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 0.86em;
  }}
  pre code {{
    background: transparent;
    padding: 0;
  }}
</style>
</head>
<body>

<header>
  <span class="badge-library">Ecosystem Report</span>
  <h1>compose-highlight Adoption & Consumer Report</h1>
  <p class="subtitle">
    Comprehensive census of public GitHub repositories and open-source Android applications
    consuming <code>dev.hossain:compose-highlight</code>.
  </p>
  <div class="meta-bar">
    <span>Generated: <strong>{gen_time}</strong></span>
    <span>•</span>
    <span>Target Coordinate: <code>dev.hossain:compose-highlight</code></span>
    <span>•</span>
    <a href="https://github.com/hossain-khan/android-compose-highlight" target="_blank">Repository</a>
    <span>•</span>
    <a href="https://central.sonatype.com/artifact/dev.hossain/compose-highlight" target="_blank">Maven Central</a>
  </div>
</header>

<section class="metrics-grid">
  <div class="metric-card">
    <div class="metric-label">Identified Consumers</div>
    <div class="metric-value">{summary["total_consumers"]}</div>
    <div class="metric-sub">{summary["third_party_consumers"]} external • {summary["first_party_consumers"]} first-party</div>
  </div>
  <div class="metric-card">
    <div class="metric-label">Third-Party Adoption</div>
    <div class="metric-value">{summary["third_party_consumers"]}</div>
    <div class="metric-sub">Open-source apps & templates</div>
  </div>
  <div class="metric-card">
    <div class="metric-label">Total Consumer Stars</div>
    <div class="metric-value">{summary["total_consumer_stars"]:,}</div>
    <div class="metric-sub">Across all consumer repos</div>
  </div>
  <div class="metric-card">
    <div class="metric-label">Latest Version in Wild</div>
    <div class="metric-value">{summary["latest_detected_version"]}</div>
    <div class="metric-sub">Active adoption of recent release</div>
  </div>
</section>

<!-- Featured Flagship Consumer Callout -->
<section class="featured-banner">
  <div class="featured-content">
    <span class="featured-badge">Flagship 3rd-Party Consumer</span>
    <div class="featured-title">T8RIN/ImageToolbox</div>
    <div class="featured-desc">
      A massive Android image editor app with <strong>15,500+ stars</strong> on GitHub.
      ImageToolbox relies on <code>compose-highlight</code> (v0.41.1) to power its interactive
      <strong>GLSL Shader Studio</strong> and code inspection views via <code>rememberHighlightedCode</code>
      and custom VisualTransformation integration.
    </div>
  </div>
  <div class="featured-stats">
    <div>
      <div class="featured-stat-val">15.5k+</div>
      <div class="featured-stat-label">Stars</div>
    </div>
    <div>
      <div class="featured-stat-val">v0.41.1</div>
      <div class="featured-stat-label">Version</div>
    </div>
  </div>
</section>

<!-- Controls Toolbar -->
<div class="toolbar">
  <div class="filter-tabs">
    <button class="tab-btn active" data-filter="all">All ({len(consumers)})</button>
    <button class="tab-btn" data-filter="3rd-party">3rd-Party ({summary["third_party_consumers"]})</button>
    <button class="tab-btn" data-filter="first-party">First-Party ({summary["first_party_consumers"]})</button>
  </div>
  <div class="search-box">
    <input type="text" id="searchInput" class="search-input" placeholder="Search repository or keyword...">
    <select id="sortSelect" class="sort-select">
      <option value="stars-desc">Sort by Stars</option>
      <option value="name-asc">Sort by Name</option>
      <option value="version-desc">Sort by Version</option>
      <option value="updated-desc">Sort by Last Updated</option>
    </select>
  </div>
</div>

<!-- Main Consumer Table -->
<div class="table-container">
  <table>
    <thead>
      <tr>
        <th>Repository</th>
        <th>Type</th>
        <th>Stars</th>
        <th>Version</th>
        <th>Matched Files & Usage</th>
      </tr>
    </thead>
    <tbody id="tableBody">
      <!-- Populated dynamically via JS -->
    </tbody>
  </table>
</div>

<!-- Methodology & Reproduction Guide -->
<section class="methodology">
  <h2>Discovery Methodology & Reproduction Guide</h2>
  <p>
    <strong>Why doesn't GitHub Dependents show these repositories?</strong><br>
    GitHub's native <code>/network/dependents</code> page only displays dependents when repositories
    push explicit Dependency Submission snapshots (SBOMs) to GitHub for Maven/Gradle artifacts.
    Standard Gradle builds resolving from Maven Central are not indexed automatically by GitHub's
    dependency graph.
  </p>
  <p>
    To solve this, our automated discovery system uses a <strong>multi-vector code search pipeline</strong>:
  </p>
  <ul>
    <li><strong>Vector 1: Maven Coordinates</strong> - searches for <code>dev.hossain:compose-highlight</code> across all build files and catalogs.</li>
    <li><strong>Vector 2: Package Imports</strong> - searches for <code>import dev.hossain.highlight</code> across Kotlin/Java source files.</li>
    <li><strong>Vector 3: Version Catalogs</strong> - searches for <code>compose-highlight</code> in TOML version catalogs.</li>
  </ul>
  <p style="margin-top: 14px;"><strong>How to refresh this report:</strong></p>
  <pre><code># Re-run live discovery against GitHub using gh CLI
python3 scripts/generate-consumer-report.py --refresh

# View report in your browser
open resources/consumer-report/index.html</code></pre>
</section>

<script>
  const consumers = {consumers_json};
  let activeFilter = 'all';
  let searchTerm = '';
  let sortBy = 'stars-desc';

  function renderTable() {{
    const tbody = document.getElementById('tableBody');
    let list = consumers.filter(c => {{
      if (activeFilter !== 'all' && c.category_group !== activeFilter) return false;
      if (searchTerm) {{
        const term = searchTerm.toLowerCase();
        const matchesName = c.repo.toLowerCase().includes(term);
        const matchesTitle = (c.title || '').toLowerCase().includes(term);
        const matchesDesc = (c.description || '').toLowerCase().includes(term);
        const matchesVer = (c.detected_version || '').toLowerCase().includes(term);
        if (!matchesName && !matchesTitle && !matchesDesc && !matchesVer) return false;
      }}
      return true;
    }});

    list.sort((a, b) => {{
      if (sortBy === 'stars-desc') return b.stars - a.stars;
      if (sortBy === 'name-asc') return a.repo.localeCompare(b.repo);
      if (sortBy === 'version-desc') return (b.detected_version || '').localeCompare(a.detected_version || '');
      if (sortBy === 'updated-desc') return (b.pushed_at || '').localeCompare(a.pushed_at || '');
      return 0;
    }});

    if (list.length === 0) {{
      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; padding:32px; color:var(--text-muted);">No matching repositories found.</td></tr>';
      return;
    }}

    tbody.innerHTML = list.map(c => {{
      const typeBadge = c.category_group === '3rd-party'
        ? '<span class="badge badge-3rd">' + c.category + '</span>'
        : '<span class="badge badge-first">' + c.category + '</span>';

      const filesPreview = c.files.slice(0, 3).map(f => `<div>• ${{f}}</div>`).join('');
      const moreFiles = c.files.length > 3
        ? `<details class="files-list"><summary>+${{c.files.length - 3}} more files</summary>` +
          c.files.slice(3).map(f => `<div>• ${{f}}</div>`).join('') + `</details>`
        : '';

      const verText = c.detected_version !== 'Unknown'
        ? `<span class="badge badge-version">v${{c.detected_version}}</span>`
        : '<span class="badge badge-version" style="opacity:0.6;">Unknown</span>';

      return `
        <tr>
          <td>
            <div class="repo-title-row">
              <a class="repo-name repo-link" href="${{c.url}}" target="_blank">${{c.repo}}</a>
            </div>
            <div class="repo-desc">${{c.description || ''}}</div>
          </td>
          <td>${{typeBadge}}</td>
          <td>
            <span class="badge-stars">⭐ ${{c.stars.toLocaleString()}}</span>
          </td>
          <td>${{verText}}</td>
          <td>
            <div class="files-list">
              ${{filesPreview}}
              ${{moreFiles}}
            </div>
          </td>
        </tr>
      `;
    }}).join('');
  }}

  // Filter tab events
  document.querySelectorAll('.tab-btn').forEach(btn => {{
    btn.addEventListener('click', () => {{
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      activeFilter = btn.dataset.filter;
      renderTable();
    }});
  }});

  // Search input events
  document.getElementById('searchInput').addEventListener('input', (e) => {{
    searchTerm = e.target.value;
    renderTable();
  }});

  // Sort events
  document.getElementById('sortSelect').addEventListener('change', (e) => {{
    sortBy = e.target.value;
    renderTable();
  }});

  // Initial render
  renderTable();
</script>

</body>
</html>
"""
    return html


def render_markdown(data: dict) -> str:
    """Generate companion Markdown report."""
    summary = data["summary"]
    consumers = data["consumers"]
    gen_time = data["generated_at"][:10]

    lines = [
        "# compose-highlight - Consumer Adoption Report",
        "",
        f"> **Generated:** {gen_time} | **Target Coordinate:** `dev.hossain:compose-highlight`",
        "",
        "## Summary Metrics",
        "",
        f"- **Total Identified Consumers:** {summary['total_consumers']} repositories",
        f"- **Third-Party Open Source Apps & Templates:** {summary['third_party_consumers']} repositories",
        f"- **First-Party Showcases & Companion Tools:** {summary['first_party_consumers']} repositories",
        f"- **Combined Consumer Stars:** {summary['total_consumer_stars']:,} stars",
        f"- **Latest Detected Version:** `v{summary['latest_detected_version']}`",
        "",
        "---",
        "",
        "## Prominent Third-Party Consumers",
        "",
        "| Repository | Stars | Version | License | Highlight |",
        "| --- | --- | --- | --- | --- |",
    ]

    for c in consumers:
        if c["category_group"] == "3rd-party":
            repo_link = f"[{c['repo']}]({c['url']})"
            stars = f"{c['stars']:,}"
            ver = f"`{c['detected_version']}`" if c["detected_version"] != "Unknown" else "N/A"
            highlight = c["highlight"].replace("|", "\\|")
            lines.append(f"| {repo_link} | {stars} | {ver} | {c['license']} | {highlight} |")

    lines.extend([
        "",
        "---",
        "",
        "## First-Party Companion Apps & Showcases",
        "",
        "| Repository | Stars | Version | Role & Description |",
        "| --- | --- | --- | --- |",
    ])

    for c in consumers:
        if c["category_group"] == "first-party":
            repo_link = f"[{c['repo']}]({c['url']})"
            stars = f"{c['stars']:,}"
            ver = f"`{c['detected_version']}`" if c["detected_version"] != "Unknown" else "N/A"
            highlight = c["highlight"].replace("|", "\\|")
            lines.append(f"| {repo_link} | {stars} | {ver} | {highlight} |")

    lines.extend([
        "",
        "---",
        "",
        "## How to Reproduce and Refresh",
        "",
        "Run the automated generator script to refresh live data from GitHub:",
        "",
        "```bash",
        "# Refresh live data from GitHub and update HTML/MD/JSON reports",
        "python3 scripts/generate-consumer-report.py --refresh",
        "",
        "# Rebuild reports from cached dataset without hitting GitHub API",
        "python3 scripts/generate-consumer-report.py --offline",
        "```",
        "",
        "Open `resources/consumer-report/index.html` to view the interactive single-page HTML report.",
        "",
    ])

    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Generate consumer usage report for compose-highlight.")
    parser.add_argument("--refresh", action="store_true", help="Force fetching fresh data from GitHub via gh CLI.")
    parser.add_argument("--offline", action="store_true", help="Use cached data.json without querying GitHub.")
    parser.add_argument("--output-json", default=DEFAULT_JSON, help="Path for JSON dataset output.")
    parser.add_argument("--output-html", default=DEFAULT_HTML, help="Path for HTML report output.")
    parser.add_argument("--output-md", default=DEFAULT_MD, help="Path for Markdown report output.")
    args = parser.parse_args()

    # Determine whether to refresh or use cache
    json_path = Path(args.output_json)
    need_refresh = args.refresh or (not json_path.exists() and not args.offline)

    if need_refresh:
        print("[Workflow] Initiating live discovery on GitHub...")
        repo_files = search_github()
        dataset = build_dataset(repo_files)
        # Ensure output directory exists
        json_path.parent.mkdir(parents=True, exist_ok=True)
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(dataset, f, indent=2)
        print(f"[Output] Saved dataset to {json_path}")
    else:
        print(f"[Workflow] Loading cached dataset from {json_path}...")
        if not json_path.exists():
            print(f"Error: {json_path} does not exist. Run with --refresh to create it.", file=sys.stderr)
            sys.exit(1)
        with open(json_path, "r", encoding="utf-8") as f:
            dataset = json.load(f)

    # Render HTML
    html_content = render_html(dataset)
    html_path = Path(args.output_html)
    html_path.parent.mkdir(parents=True, exist_ok=True)
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(html_content)
    print(f"[Output] Generated HTML report at {html_path}")

    # Render Markdown
    md_content = render_markdown(dataset)
    md_path = Path(args.output_md)
    md_path.parent.mkdir(parents=True, exist_ok=True)
    with open(md_path, "w", encoding="utf-8") as f:
        f.write(md_content)
    print(f"[Output] Generated Markdown report at {md_path}")

    print("\n[Done] Consumer report generation complete!")


if __name__ == "__main__":
    main()
