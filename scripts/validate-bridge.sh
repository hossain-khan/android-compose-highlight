#!/usr/bin/env bash
#
# validate-bridge.sh - Validates the JS/Kotlin bridge contract and security hardening for HighlightEngine.
#
# Usage: ./scripts/validate-bridge.sh
#

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BRIDGE_FILE="$REPO_ROOT/compose-highlight/src/main/assets/compose-highlight/bridge.html"
HLJS_FILE="$REPO_ROOT/compose-highlight/src/main/assets/compose-highlight/highlight.min.js"
WEBVIEW_FILE="$REPO_ROOT/compose-highlight/src/main/kotlin/dev/hossain/highlight/engine/internal/WebViewManager.kt"

echo "Validating bridge files..."

if [ ! -f "$BRIDGE_FILE" ]; then
  echo "ERROR: Bridge file not found: $BRIDGE_FILE"
  exit 1
fi

if [ ! -f "$HLJS_FILE" ]; then
  echo "ERROR: Highlight.js file not found: $HLJS_FILE"
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

# 2. Contract and Security checks
echo "Checking bridge API and security contracts..."

# Content-Security-Policy meta tag must be present and restrictive
if ! grep -qi '<meta[[:space:]]\+http-equiv=["'\'']Content-Security-Policy["'\'']' "$BRIDGE_FILE"; then
  echo "ERROR: bridge.html missing Content-Security-Policy meta tag"
  exit 1
fi

if ! grep -qi "default-src[[:space:]]\+['\"]none['\"]" "$BRIDGE_FILE"; then
  echo "ERROR: bridge.html CSP missing default-src 'none'"
  exit 1
fi

if ! grep -qi "script-src[[:space:]]\+['\"]self['\"][[:space:]]\+['\"]unsafe-inline['\"]" "$BRIDGE_FILE"; then
  echo "ERROR: bridge.html CSP missing script-src 'self' 'unsafe-inline'"
  exit 1
fi

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

# highlightCode() delegates directly to hljs.highlight with language guard
if ! grep -Eq "hljs\.highlight\(" "$BRIDGE_FILE"; then
  echo "ERROR: bridge.html missing hljs.highlight() invocation"
  exit 1
fi
if ! grep -Eq "hljs\.getLanguage\(" "$BRIDGE_FILE"; then
  echo "ERROR: bridge.html missing hljs.getLanguage() guard"
  exit 1
fi

# WebViewManager must load bridge.html from the correct appassets URL
if ! grep -q 'https://appassets.androidplatform.net/assets/compose-highlight/bridge.html' "$WEBVIEW_FILE"; then
  echo "ERROR: WebViewManager.kt does not load expected bridge URL"
  exit 1
fi

# WebViewManager security hardening invariants
for setting in "allowFileAccess = false" "allowContentAccess = false" "blockNetworkLoads = true"; do
  if ! grep -Fq "$setting" "$WEBVIEW_FILE"; then
    echo "ERROR: WebViewManager.kt missing hardened setting: $setting"
    exit 1
  fi
done

if ! grep -Eq "override fun shouldOverrideUrlLoading\(" "$WEBVIEW_FILE"; then
  echo "ERROR: WebViewManager.kt missing shouldOverrideUrlLoading navigation guard"
  exit 1
fi

if ! grep -Eq "override fun shouldInterceptRequest\(" "$WEBVIEW_FILE"; then
  echo "ERROR: WebViewManager.kt missing shouldInterceptRequest origin guard"
  exit 1
fi

# 3. Semantic runtime check via Node.js if available
if command -v node >/dev/null 2>&1; then
  echo "Running bridge runtime contract check with Node.js..."
  node -e "
    const fs = require('fs');
    const vm = require('vm');
    const bridgeHtml = fs.readFileSync('$BRIDGE_FILE', 'utf8');
    const hljsCode = fs.readFileSync('$HLJS_FILE', 'utf8');
    const scriptRegex = /<script>([\s\S]*?)<\/script>/gi;
    let match;
    const scripts = [];
    while ((match = scriptRegex.exec(bridgeHtml)) !== null) {
      scripts.push(match[1]);
    }
    const ctx = {};
    vm.runInNewContext(hljsCode, ctx);
    for (const s of scripts) {
      vm.runInNewContext(s, ctx);
    }

    // 1. Valid language highlighting
    const kotlinRes = JSON.parse(ctx.highlightCode('fun main() {}', 'kotlin'));
    if (kotlinRes.error || !kotlinRes.html.includes('hljs-keyword')) {
      throw new Error('highlightCode failed for valid language: ' + JSON.stringify(kotlinRes));
    }

    // 2. Unknown language guard (must NOT trigger highlightAuto)
    const unknownRes = JSON.parse(ctx.highlightCode('fun main() { val x = 1 < 2 }', 'unknown_lang'));
    if (unknownRes.error || unknownRes.html.includes('hljs-') || !unknownRes.unsupported) {
      throw new Error('highlightCode failed to guard unknown language: ' + JSON.stringify(unknownRes));
    }

    // 3. Comprehensive HTML entity escaping on unhighlighted code (&, <, >, \", ')
    const escapeTestRes = JSON.parse(ctx.highlightCode('<a & \"b\" > \'c\'', 'unknown_lang'));
    const expectedEscaped = '&lt;a &amp; &quot;b&quot; &gt; &#39;c&#39;';
    if (escapeTestRes.html !== expectedEscaped) {
      throw new Error('highlightCode failed complete entity escaping: expected ' + expectedEscaped + ' but got ' + escapeTestRes.html);
    }

    // 4. Blank language guard
    const blankRes = JSON.parse(ctx.highlightCode('some text', ''));
    if (blankRes.error || blankRes.html.includes('hljs-') || !blankRes.unsupported) {
      throw new Error('highlightCode failed to guard blank language: ' + JSON.stringify(blankRes));
    }

    // 5. highlightAuto produces spans
    const autoRes = JSON.parse(ctx.highlightAuto('fun main() {}'));
    if (autoRes.error || !autoRes.html.includes('hljs-')) {
      throw new Error('highlightAuto failed: ' + JSON.stringify(autoRes));
    }

    // 6. listLanguages returns non-empty array of strings
    const langs = ctx.listLanguages();
    if (!Array.isArray(langs) || langs.length < 100) {
      throw new Error('listLanguages did not return an array with expected language count: ' + JSON.stringify(langs));
    }
    const requiredLangs = ['kotlin', 'python', 'javascript', 'java', 'swift'];
    for (const lang of requiredLangs) {
      if (!langs.includes(lang)) {
        throw new Error('listLanguages missing core language: ' + lang);
      }
    }

    // 7. getLanguage handles valid languages, aliases, and unknown languages
    const kotlinMeta = JSON.parse(ctx.getLanguage('kotlin'));
    if (!kotlinMeta || kotlinMeta.name !== 'Kotlin' || !Array.isArray(kotlinMeta.aliases) || !kotlinMeta.aliases.includes('kt')) {
      throw new Error('getLanguage failed for canonical name: ' + JSON.stringify(kotlinMeta));
    }

    const aliasMeta = JSON.parse(ctx.getLanguage('kt'));
    if (!aliasMeta || aliasMeta.name !== 'Kotlin') {
      throw new Error('getLanguage failed for alias: ' + JSON.stringify(aliasMeta));
    }

    const unknownMeta = ctx.getLanguage('unknown_lang_identifier_123');
    if (unknownMeta !== null) {
      throw new Error('getLanguage expected null for unknown language, got: ' + JSON.stringify(unknownMeta));
    }

    // 8. hljsVersion returns valid semver string
    const version = ctx.hljsVersion();
    if (typeof version !== 'string' || !/^\d+\.\d+\.\d+/.test(version)) {
      throw new Error('hljsVersion did not return a valid semver string: ' + version);
    }
  "
fi

echo "Bridge validation passed! All contracts and syntax checks are valid."
