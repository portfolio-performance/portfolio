#!/bin/bash
#
# Promote a version currently live on the beta channel to the stable channel.
# Same bytes, same version — only the stable composite is rewritten.
#
# Usage: ./promote-to-production.sh <version>

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/composite.sh
. "$SCRIPT_DIR/lib/composite.sh"

VERSION=${1:-}
if [ -z "$VERSION" ]; then
    echo "Usage: $0 <version> (e.g. 0.84.0)" >&2
    exit 1
fi

UPDATESITE="$SCRIPT_DIR/updatesite"
RELEASE_DIR="$UPDATESITE/releases/$VERSION"

if [ ! -d "$RELEASE_DIR" ]; then
    echo "Release folder not found: $RELEASE_DIR" >&2
    echo "Run publish-beta.sh for this version first." >&2
    exit 1
fi

BETA_VERSION=$(current_version "$UPDATESITE/portfolio-beta/compositeContent.xml")
if [ "$BETA_VERSION" != "$VERSION" ]; then
    echo "Beta channel currently points at '$BETA_VERSION', not '$VERSION'." >&2
    echo "Refusing to promote a version that is not live on beta." >&2
    exit 1
fi

STABLE_VERSION=$(current_version "$UPDATESITE/portfolio/compositeContent.xml")
if [ "$STABLE_VERSION" = "$VERSION" ]; then
    echo "Stable channel already points at $VERSION — nothing to do."
    exit 0
fi

TS=$(perl -e 'print int(time*1000)')
write_composite "$UPDATESITE/portfolio" "$VERSION" "$TS"

cd "$UPDATESITE"
git add -A
git commit -m "Release $VERSION"

echo "Promoted $VERSION to stable channel. Push updatesite manually after review."
