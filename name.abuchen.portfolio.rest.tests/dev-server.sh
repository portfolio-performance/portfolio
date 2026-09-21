#!/usr/bin/env bash
#
# Runs DevServer on a plain classpath, without OSGi or UI.
#
#   ./dev-server.sh                     # bundled sample, default REST port
#   ./dev-server.sh --file my.portfolio --port 8080 --token secret
#
# Defaults to RestApiConstants.DEFAULT_PORT.
#
set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLUGINS="$REPO/portfolio-product/target/repository/plugins"
MIRROR="$REPO/name.abuchen.portfolio.rest.tests/target/dev-classpath"

# prefer freshly compiled classes
BUNDLES=(
    name.abuchen.portfolio
    name.abuchen.portfolio.rest
    name.abuchen.portfolio.rest.tests
    name.abuchen.portfolio.ui
)

CP=""
for bundle in "${BUNDLES[@]}"; do
    classes="$REPO/$bundle/target/classes"
    if [[ ! -d "$classes" ]]; then
        echo "missing $classes - compile first:" >&2
        echo "  mvn -f portfolio-app/pom.xml compile -Plocal-dev -pl :portfolio-target-definition,\\" >&2
        echo "    :name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,\\" >&2
        echo "    :name.abuchen.portfolio.bootstrap,:name.abuchen.portfolio.ui,:name.abuchen.portfolio.rest,\\" >&2
        echo "    :name.abuchen.portfolio.rest.tests -am -amd" >&2
        exit 1
    fi
    CP="$CP:$classes"
done

if [[ ! -d "$PLUGINS" ]]; then
    echo "missing $PLUGINS - build the product once to resolve the target platform:" >&2
    echo "  mvn -f portfolio-app/pom.xml verify -Plocal-dev" >&2
    exit 1
fi

# Mirror dependencies and strip signatures so split Eclipse packages co-load.
if [[ ! -d "$MIRROR" || -n "$(find "$PLUGINS" -name '*.jar' -newer "$MIRROR" -print -quit)" ]]; then
    echo "preparing dependencies in $MIRROR ..." >&2
    rm -rf "$MIRROR"
    mkdir -p "$MIRROR"
    find "$PLUGINS" -name '*.jar' ! -name 'name.abuchen.zulu*' ! -name '*.source_*' -exec cp {} "$MIRROR/" \;
    (
        cd "$MIRROR"
        for jar in *.jar; do
            zip -q -d "$jar" 'META-INF/*.SF' 'META-INF/*.RSA' 'META-INF/*.DSA' 'META-INF/*.EC' >/dev/null 2>&1 || true
        done
    )
    touch "$MIRROR"
fi

for jar in "$MIRROR"/*.jar; do
    CP="$CP:$jar"
done

exec java -cp "${CP#:}" name.abuchen.portfolio.rest.testsupport.DevServer "$@"
