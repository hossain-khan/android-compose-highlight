#!/usr/bin/env bash
#
# refresh-sample-themes.sh - Re-downloads highlight.js minified CSS themes for the sample app.
#
# Usage: ./scripts/refresh-sample-themes.sh <version>
# Example: ./scripts/refresh-sample-themes.sh 11.12.0
#

set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 11.12.0"
  exit 1
fi

VERSION="$1"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$REPO_ROOT/sample/src/main/assets/themes"

echo "Fetching theme file list for highlight.js $VERSION from cdnjs..."
mkdir -p "$DEST/base16"

API_URL="https://api.cdnjs.com/libraries/highlight.js/$VERSION?fields=files"

THEMES=$(curl -s "$API_URL" | python3 -c "
import json, sys
data = json.load(sys.stdin)
files = data.get('files', [])
themes = sorted(set(
    f.split('styles/')[1].replace('.min.css','')
    for f in files
    if 'styles/' in f and f.endswith('.min.css')
))
if not themes:
    sys.exit('ERROR: No .min.css themes found in cdnjs API response for version $VERSION')
print('\n'.join(themes))
")

THEME_COUNT=$(echo "$THEMES" | grep -v '^$' | wc -l | tr -d ' ')
echo "Found $THEME_COUNT minified themes for highlight.js $VERSION"

echo "Downloading themes to $DEST..."
echo "$THEMES" | while IFS= read -r theme; do
  [ -z "$theme" ] && continue
  url="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/$VERSION/styles/${theme}.min.css"
  outfile="$DEST/${theme}.min.css"
  mkdir -p "$(dirname "$outfile")"
  curl -s -o "$outfile" "$url" &
done

wait
TOTAL_DOWNLOADED=$(find "$DEST" -name '*.min.css' | wc -l | tr -d ' ')
echo "Successfully downloaded $TOTAL_DOWNLOADED themes into $DEST"
