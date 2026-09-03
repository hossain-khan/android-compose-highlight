#!/usr/bin/env bash
#
# validate-bridge.sh - Validates the JS/Kotlin bridge contract for HighlightEngine.
#
# Usage: ./scripts/validate-bridge.sh
#

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BRIDGE_FILE="$REPO_ROOT/compose-highlight/src/main/assets/compose-highlight/bridge.html"
WEBVIEW_FILE="$REPO_ROOT/compose-highlight/src/main/kotlin/dev/hossain/highlight/engine/internal/WebViewManager.kt"

echo "Validating bridge files..."

if [ ! -f "$BRIDGE_FILE" ]; then
  echo "ERROR: Bridge file not found: $BRIDGE_FILE"
  exit 1
fi

if [ ! -f "$WEBVIEW_FILE" ]; then
  echo "ERROR: WebViewManager file not found: $WEBVIEW_FILE"
  exit 1
fi

# 1. HTML validation via htmlhint if npx/htmlhint is available
if command -v npx >/dev/null 2>&1; then
  echo "Checking bridge.html syntax with htmlhint..."
  npx --yes htmlhint@1.9.2 \
    --rules 'doctype-first:true,tag-pair:true,tagname-lowercase:true,attr-lowercase:true,attr-value-double-quotes:true,id-unique:true,src-not-empty:true,title-require:false' \
    "$BRIDGE_FILE"
else
  echo "Notice: npx not available, skipping htmlhint syntax check"
fi

# 2. Contract checks
echo "Checking bridge API contract..."

# highlight.min.js must be loaded
if ! grep -Eiq "<script[^>]*src=[\"']highlight\\.min\\.js[\"'][^>]*>" "$BRIDGE_FILE"; then
  echo "ERROR: bridge.html does not load highlight.min.js"
  exit 1
fi

# All 5 JS functions called by HighlightEngine via evaluateJavascript() must exist
for fn in highlightCode highlightAuto listLanguages getLanguage hljsVersion; do
  if ! grep -Eq "function[[:space:]]+${fn}[[:space:]]*\(" "$BRIDGE_FILE"; then
    echo "ERROR: bridge.html missing required function: ${fn}()"
    exit 1
  fi
done

# highlightCode() uses getElementById("code") to set content
if ! grep -Eq "getElementById[[:space:]]*\([[:space:]]*['\"]code['\"][[:space:]]*\)|querySelector[[:space:]]*\([[:space:]]*['\"]#code['\"][[:space:]]*\)" "$BRIDGE_FILE"; then
  echo "ERROR: bridge.html missing element with id='code' lookup"
  exit 1
fi

# WebViewManager must load bridge.html from the correct appassets URL
if ! grep -q 'https://appassets.androidplatform.net/assets/compose-highlight/bridge.html' "$WEBVIEW_FILE"; then
  echo "ERROR: WebViewManager.kt does not load expected bridge URL"
  exit 1
fi

echo "Bridge validation passed! All contracts and syntax checks are valid."
