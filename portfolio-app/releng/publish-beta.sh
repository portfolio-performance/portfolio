#!/bin/bash
#
# Publish a freshly-built release into updatesite/releases/<version>/ and
# point the beta composite channel at it. The stable channel is not touched.
#
# Usage: ./publish-beta.sh <version>

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/composite.sh
. "$SCRIPT_DIR/lib/composite.sh"

VERSION=${1:-}
if [ -z "$VERSION" ]; then
    echo "Usage: $0 <version> (e.g. 0.84.0)" >&2
    exit 1
fi

BUILD_OUTPUT="$SCRIPT_DIR/../../portfolio-product/target/repository"
if [ ! -d "$BUILD_OUTPUT" ]; then
    echo "Build output not found: $BUILD_OUTPUT" >&2
    echo "Run the Maven/Tycho build first." >&2
    exit 1
fi

UPDATESITE="$SCRIPT_DIR/updatesite"
TARGET="$UPDATESITE/releases/$VERSION"

if [ -e "$TARGET" ]; then
    echo "Release folder already exists: $TARGET" >&2
    echo "Refusing to overwrite. Delete it manually if the re-publish is intentional." >&2
    exit 1
fi

mkdir -p "$TARGET"
cp -R "$BUILD_OUTPUT"/* "$TARGET"/

TS=$(perl -e 'print int(time*1000)')
write_composite "$UPDATESITE/portfolio-beta" "$VERSION" "$TS"

cd "$UPDATESITE"
git add -A
git commit -m "Beta $VERSION"

echo "Published $VERSION to beta channel. Push updatesite manually after review."
