#!/bin/bash
#
# Delete release folders not referenced by either channel. Dry-run by default.
#
# Usage:
#   ./prune-releases.sh            dry-run, prints plan
#   ./prune-releases.sh --confirm  apply and commit

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/composite.sh
. "$SCRIPT_DIR/lib/composite.sh"

CONFIRM=0
if [ "${1:-}" = "--confirm" ]; then
    CONFIRM=1
elif [ -n "${1:-}" ]; then
    echo "Usage: $0 [--confirm]" >&2
    exit 1
fi

UPDATESITE="$SCRIPT_DIR/updatesite"

STABLE=$(current_version "$UPDATESITE/portfolio/compositeContent.xml")
BETA=$(current_version "$UPDATESITE/portfolio-beta/compositeContent.xml")

if [ -z "$STABLE" ] || [ -z "$BETA" ]; then
    echo "Could not determine current version for stable ('$STABLE') or beta ('$BETA')." >&2
    echo "Malformed composite — refusing to prune." >&2
    exit 1
fi

echo "Stable channel: $STABLE"
echo "Beta channel:   $BETA"

if [ "$STABLE" = "$BETA" ]; then
    echo "Keep: $STABLE"
else
    echo "Keep: $STABLE, $BETA"
fi

TO_DELETE=()
for dir in "$UPDATESITE"/releases/*/; do
    [ -d "$dir" ] || continue
    name=$(basename "$dir")
    if [ "$name" != "$STABLE" ] && [ "$name" != "$BETA" ]; then
        TO_DELETE+=("$name")
    fi
done

if [ "${#TO_DELETE[@]}" -eq 0 ]; then
    echo "Nothing to prune."
    exit 0
fi

echo "Would delete:"
for v in "${TO_DELETE[@]}"; do
    echo "  releases/$v"
done

if [ "$CONFIRM" -ne 1 ]; then
    echo "Run with --confirm to apply."
    exit 0
fi

for v in "${TO_DELETE[@]}"; do
    rm -rf "$UPDATESITE/releases/$v"
done

cd "$UPDATESITE"
git add -A
git commit -m "Prune old releases"

echo "Pruned ${#TO_DELETE[@]} release(s). Push updatesite manually after review."
