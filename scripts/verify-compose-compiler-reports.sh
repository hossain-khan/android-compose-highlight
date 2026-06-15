#!/bin/bash
#
# Verifies the Compose compiler stability report for the public UI surface.
# Fails if any public UI composable is non-skippable or has unstable parameters.
#
# Intended to run in CI after:
#   ./gradlew :compose-highlight:assembleRelease -PcomposeReports=true
#

set -euo pipefail

REPORT="compose-highlight/build/compose_compiler/compose-highlight-composables.txt"

if [ ! -f "$REPORT" ]; then
    echo "::error::Compose compiler report not found: $REPORT"
    exit 1
fi

echo "Verifying Compose compiler report: $REPORT"

awk -v RS='' -v ORS='\n\n' '
    / fun dev\.hossain\.highlight\.ui\./ {
        if ($0 ~ /^restartable/ && $0 !~ / skippable /) {
            print "::error::Non-skippable public UI composable"
            print $0
            exit_code = 1
        }
        if ($0 ~ /\n  unstable /) {
            print "::error::Public UI composable has unstable parameter"
            print $0
            exit_code = 1
        }
    }
    END { exit (exit_code ? 1 : 0) }
' "$REPORT"

echo "Compose compiler report verification passed."
