#!/usr/bin/env bash
#
# upgrade-hljs.sh - Orchestrates upgrading the bundled Highlight.js library.
#
# Usage: ./scripts/upgrade-hljs.sh <new-version>
# Example: ./scripts/upgrade-hljs.sh 11.12.0
#

set -euo pipefail

# Helper function for portable in-place replacement using sed
sedi() {
  local pattern="$1"
  local filepath="$2"
  if [ "$(uname)" = "Darwin" ]; then
    sed -i '' "$pattern" "$filepath"
  else
    sed -i "$pattern" "$filepath"
  fi
}

if [ $# -ne 1 ]; then
  echo "Usage: $0 <new-version>"
  echo "Example: $0 11.12.0"
  exit 1
fi

NEW_VERSION="$1"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUNDLE_PATH="$REPO_ROOT/compose-highlight/src/main/assets/compose-highlight/highlight.min.js"

# Extract currently bundled version
CURRENT_VERSION=$(node -e "
  const vm = require('vm');
  const fs = require('fs');
  const sandbox = {};
  try {
    vm.runInNewContext(fs.readFileSync('$BUNDLE_PATH', 'utf8'), sandbox);
    console.log(sandbox.hljs.versionString);
  } catch (e) {
    console.error('Error reading current hljs version:', e.message);
    process.exit(1);
  }
")

echo "================================================================="
echo "Upgrading bundled Highlight.js: $CURRENT_VERSION -> $NEW_VERSION"
echo "================================================================="
echo ""

# 1. Re-bundle highlight.min.js
echo "Step 1: Bundling highlight.js@$NEW_VERSION..."
node "$REPO_ROOT/scripts/bundle-hljs.mjs" "$NEW_VERSION"
echo ""

# 2. Refresh sample app themes
echo "Step 2: Downloading sample app CSS themes from cdnjs..."
"$REPO_ROOT/scripts/refresh-sample-themes.sh" "$NEW_VERSION"
echo ""

# 3. Refresh test fixtures
echo "Step 3: Refreshing HTML test fixtures..."
"$REPO_ROOT/gradlew" -p "$REPO_ROOT" :compose-highlight:refreshHljsFixtures
echo ""

# 4. Update version references in code and docs
echo "Step 4: Updating version references..."

# Count updated themes
THEME_COUNT=$(find "$REPO_ROOT/sample/src/main/assets/themes" -name '*.min.css' | wc -l | tr -d ' ')
ROOT_THEME_COUNT=$(find "$REPO_ROOT/sample/src/main/assets/themes" -maxdepth 1 -name '*.min.css' | wc -l | tr -d ' ')

# Update HighlightEngine.kt KDoc example
sedi "s/(e\.g\. \"$CURRENT_VERSION\")/(e.g. \"$NEW_VERSION\")/" "$REPO_ROOT/compose-highlight/src/main/kotlin/dev/hossain/highlight/engine/HighlightEngine.kt"

# Update HighlightEngineRobolectricTest.kt and HighlightEngineTest.kt
sedi "s/\"$CURRENT_VERSION\"/\"$NEW_VERSION\"/g" "$REPO_ROOT/compose-highlight/src/test/kotlin/dev/hossain/highlight/engine/HighlightEngineRobolectricTest.kt"
sedi "s/e\.g\. \"$CURRENT_VERSION\"/e.g. \"$NEW_VERSION\"/g" "$REPO_ROOT/compose-highlight/src/androidTest/kotlin/dev/hossain/highlight/engine/HighlightEngineTest.kt"

# Update sample themes README.md
sedi "s/version \*\*$CURRENT_VERSION\*\*/version \*\*$NEW_VERSION\*\*/g" "$REPO_ROOT/sample/src/main/assets/themes/README.md"
sedi "s/highlight\.js\/$CURRENT_VERSION/highlight.js\/$NEW_VERSION/g" "$REPO_ROOT/sample/src/main/assets/themes/README.md"
sedi "s/[0-9]* minified CSS theme files/$THEME_COUNT minified CSS theme files/g" "$REPO_ROOT/sample/src/main/assets/themes/README.md"
sedi "s/# [0-9]* root-level themes/# $ROOT_THEME_COUNT root-level themes/g" "$REPO_ROOT/sample/src/main/assets/themes/README.md"
sedi "s/\/\/ [0-9]* themes/\/\/ $THEME_COUNT themes/g" "$REPO_ROOT/sample/src/main/assets/themes/README.md"

# Update updating-hljs-themes.md
sedi "s/VERSION=\"$CURRENT_VERSION\"/VERSION=\"$NEW_VERSION\"/g" "$REPO_ROOT/resources/updating-hljs-themes.md"
sedi "s/highlight\.js $CURRENT_VERSION/highlight.js $NEW_VERSION/g" "$REPO_ROOT/resources/updating-hljs-themes.md"
sedi "s/\*\*[0-9]* minified CSS theme files\*\*/\*\*$THEME_COUNT minified CSS theme files\*\*/g" "$REPO_ROOT/resources/updating-hljs-themes.md"

echo "Updated version references and counts across repository files."
echo ""

# 5. Validate bridge contract
echo "Step 5: Validating bridge contract..."
"$REPO_ROOT/scripts/validate-bridge.sh"
echo ""

# 6. Run Kotlin code formatter
echo "Step 6: Formatting Kotlin code..."
"$REPO_ROOT/gradlew" -p "$REPO_ROOT" formatKotlin
echo ""

echo "================================================================="
echo "Highlight.js upgrade completed successfully!"
echo "================================================================="
echo "Next steps:"
echo "  1. Review changes: git diff"
echo "  2. Run verification: ./gradlew :compose-highlight:assembleDebug :sample:assembleDebug :compose-highlight:test"
echo "  3. If screenshots shifted, re-record: ./gradlew :compose-highlight:recordRoborazziDebug"
echo "  4. Add an entry to CHANGELOG.md under [Unreleased]"
echo "================================================================="
