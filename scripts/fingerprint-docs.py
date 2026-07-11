#!/usr/bin/env python3
"""
Post-build script for Zensical documentation site.
Fingerprints shiki-kotlin.js and shiki-kotlin.css in the site/ directory with the current
git commit hash and updates all HTML file references to enforce browser cache busting.
"""

import subprocess
import sys
from pathlib import Path

SITE_DIR = Path("site")
ASSETS_TO_FINGERPRINT = [
    ("javascripts/shiki-kotlin.js", "javascripts/shiki-kotlin.{hash_str}.js"),
    ("stylesheets/shiki-kotlin.css", "stylesheets/shiki-kotlin.{hash_str}.css"),
]


def get_git_hash() -> str:
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "--short", "HEAD"], text=True
        ).strip()
    except Exception as e:
        print(f"Warning: Could not get git hash ({e}), falling back to 'latest'")
        return "latest"


def main():
    if not SITE_DIR.exists() or not SITE_DIR.is_dir():
        print("Error: site/ directory not found. Run 'zensical build' first.")
        sys.exit(1)

    hash_str = get_git_hash()
    print(f"Fingerprinting docs assets with hash: {hash_str}")

    replacements = {}
    for old_rel, new_rel_template in ASSETS_TO_FINGERPRINT:
        old_path = SITE_DIR / old_rel
        if not old_path.exists():
            print(f"Warning: Asset not found, skipping: {old_path}")
            continue

        new_rel = new_rel_template.format(hash_str=hash_str)
        new_path = SITE_DIR / new_rel

        # Rename file on disk
        old_path.rename(new_path)
        replacements[old_rel] = new_rel
        print(f"  Renamed: {old_rel} -> {new_rel}")

    if not replacements:
        print("No assets were fingerprinted.")
        return

    # Rewrite references across all HTML files in site/
    html_files = list(SITE_DIR.rglob("*.html"))
    updated_count = 0
    for html_file in html_files:
        content = html_file.read_text(encoding="utf-8")
        original_content = content

        for old_rel, new_rel in replacements.items():
            content = content.replace(old_rel, new_rel)

        if content != original_content:
            html_file.write_text(content, encoding="utf-8")
            updated_count += 1

    print(f"Successfully updated asset references in {updated_count} HTML files.")


if __name__ == "__main__":
    main()
