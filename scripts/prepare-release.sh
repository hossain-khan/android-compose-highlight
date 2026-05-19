#!/usr/bin/env bash
#
# prepare-release.sh - bumps all version references for a new release.
#
# Usage: ./scripts/prepare-release.sh <new-version>
# Example: ./scripts/prepare-release.sh 0.17.2
#
# Files updated:
#   - gradle.properties        (VERSION_NAME)
#   - README.md                (dependency snippet)
#   - sample/build.gradle.kts  (versionName, versionCode auto-incremented)
#   - CHANGELOG.md             ([Unreleased] renamed to [<version>] - <date>)
#
# After running, verify the diff with `git diff`, then:
#   ./gradlew formatKotlin :compose-highlight:assembleDebug :sample:assembleDebug :compose-highlight:test
#   git checkout -b release/<version>
#   git add -A && git commit -m "chore: prepare release <version>"
#   git push -u origin release/<version>
#   gh pr create --title "chore: prepare release <version>" --base main
#
# After the PR is merged:
#   git checkout main && git pull
#   git tag <version> && git push origin <version>
#
# Then trigger the publish workflow manually (dry run first, then real).

set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Usage: $0 <new-version>"
  echo "Example: $0 0.18.0"
  exit 1
fi

NEW_VERSION="$1"
DATE=$(date +%Y-%m-%d)
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Derive current version from gradle.properties
CURRENT_VERSION=$(grep "^VERSION_NAME=" "$REPO_ROOT/gradle.properties" | cut -d'=' -f2)

if [ -z "$CURRENT_VERSION" ]; then
  echo "ERROR: Could not read current VERSION_NAME from gradle.properties"
  exit 1
fi

echo "Bumping $CURRENT_VERSION -> $NEW_VERSION (date: $DATE)"
echo ""

# 1. gradle.properties
sed -i '' "s/VERSION_NAME=$CURRENT_VERSION/VERSION_NAME=$NEW_VERSION/" "$REPO_ROOT/gradle.properties"
echo "- gradle.properties updated"

# 2. README.md
sed -i '' "s/compose-highlight:$CURRENT_VERSION/compose-highlight:$NEW_VERSION/" "$REPO_ROOT/README.md"
echo "- README.md updated"

# 3. sample/build.gradle.kts - versionName
sed -i '' "s/versionName = \"$CURRENT_VERSION\"/versionName = \"$NEW_VERSION\"/" "$REPO_ROOT/sample/build.gradle.kts"
echo "- sample/build.gradle.kts versionName updated"

# 4. sample/build.gradle.kts - versionCode (auto-increment)
CURRENT_CODE=$(grep "versionCode = " "$REPO_ROOT/sample/build.gradle.kts" | grep -o '[0-9]*')
NEW_CODE=$((CURRENT_CODE + 1))
sed -i '' "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" "$REPO_ROOT/sample/build.gradle.kts"
echo "- sample/build.gradle.kts versionCode: $CURRENT_CODE -> $NEW_CODE"

# 5. CHANGELOG.md - rename [Unreleased] to versioned entry
sed -i '' "s/## \[Unreleased\]/## [Unreleased]\n\n## [$NEW_VERSION] - $DATE/" "$REPO_ROOT/CHANGELOG.md"
echo "- CHANGELOG.md [Unreleased] -> [$NEW_VERSION] - $DATE"

echo ""
echo "Done. Verify with: git diff"
echo ""
echo "Next steps:"
echo "  1. Run checks:"
echo "     ./gradlew formatKotlin :compose-highlight:assembleDebug :sample:assembleDebug :compose-highlight:test"
echo ""
echo "  2. Create a release branch, commit, push, and open a PR:"
echo "     git checkout -b release/$NEW_VERSION"
echo "     git add -A && git commit -m \"chore: prepare release $NEW_VERSION\""
echo "     git push -u origin release/$NEW_VERSION"
echo "     gh pr create --title \"chore: prepare release $NEW_VERSION\" --base main"
echo ""
echo "  3. After the PR is merged, pull main and create the git tag:"
echo "     git checkout main && git pull"
echo "     git tag $NEW_VERSION && git push origin $NEW_VERSION"
echo ""
echo "  4. Trigger the publish workflow manually (dry run first, then real)."
