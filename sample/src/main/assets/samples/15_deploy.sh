#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="${SCRIPT_DIR}/build"

log()  { printf '[%s] %s\n' "$(date +%T)" "$*"; }
fail() { log "ERROR: $*" >&2; exit 1; }

setup() {
    mkdir -p "$BUILD_DIR"
    [[ -f "gradle.properties" ]] || fail "Not a Gradle project"
    log "Setup done → $BUILD_DIR"
}

run_tests() {
    local filter="${1:-}"
    if [[ -n "$filter" ]]; then
        ./gradlew test --tests "$filter"
    else
        ./gradlew test
    fi
    log "Tests complete ✓"
}

publish() {
    local version="${1:?version required (e.g. 1.2.3)}"
    [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] \
        || fail "Invalid semver: $version"
    ./gradlew publishToMavenLocal -Pversion="$version"
    git tag "$version" && git push origin "$version"
    log "Published $version"
}

case "${1:-help}" in
    setup)   setup ;;
    test)    run_tests "${2:-}" ;;
    publish) publish "${2:-}" ;;
    clean)   rm -rf "$BUILD_DIR" && log "Cleaned." ;;
    *)       echo "Usage: $0 {setup|test|publish <ver>|clean}" ;;
esac