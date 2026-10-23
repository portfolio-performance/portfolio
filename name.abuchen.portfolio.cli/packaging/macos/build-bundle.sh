#!/bin/sh
# Builds the self-contained pp-cli bundle for macOS arm64.
#
# Runs the Tycho build of the core and CLI bundles, collects the resolved jars with
# tycho-dependency-tools:list-dependencies, keeps only what a flat classpath on macOS arm64 needs
# (no pdfbox, no source jars, only the cocoa macosx aarch64 SWT fragment), builds a jlink runtime
# and assembles target/pp-cli-macos-arm64/ next to the launcher and installer.
#
# Requires JAVA_HOME to point to a Java 21 JDK (with jmods) and an arm64 shell (not Rosetta).
#
# Usage: build-bundle.sh [--skip-maven]
#   --skip-maven  reuse the last name.abuchen.portfolio.cli/target/dependencies-list.txt
set -eu

here=$(cd "$(dirname "$0")" && pwd -P)
repo_root=$(cd "$here/../../.." && pwd -P)
cli_dir=$repo_root/name.abuchen.portfolio.cli
bundle_dir=$cli_dir/target/pp-cli-macos-arm64
swt_fragment=org.eclipse.swt.cocoa.macosx.aarch64

skip_maven=false
case ${1:-} in
    '') ;;
    --skip-maven) skip_maven=true ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
esac

die() { echo "$*" >&2; exit 1; }

[ "$(uname -s)" = Darwin ] && [ "$(uname -m)" = arm64 ] || die 'This bundle must be built on macOS arm64 (a Rosetta shell reports x86_64).'
[ -n "${JAVA_HOME:-}" ] || die 'JAVA_HOME must point to a Java 21 JDK.'
for tool in java jdeps jlink; do
    [ -x "$JAVA_HOME/bin/$tool" ] || die "$tool not found in $JAVA_HOME/bin. JAVA_HOME must be a JDK, not a JRE."
done
[ -d "$JAVA_HOME/jmods" ] || die "No jmods directory in $JAVA_HOME; jlink needs a full JDK."
grep -Eq '^JAVA_VERSION="21(\.|")' "$JAVA_HOME/release" || die "Java 21 is required in JAVA_HOME ($JAVA_HOME)."

if [ "$skip_maven" = false ]; then
    reactor=:portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.cli
    MAVEN_OPTS=${MAVEN_OPTS:--Xmx4g} mvn -f "$repo_root/portfolio-app/pom.xml" package -Plocal-dev,cli-bundle -DskipTests -pl "$reactor"
fi

list_file=$cli_dir/target/dependencies-list.txt
[ -f "$list_file" ] || die "$list_file not found. Run without --skip-maven."

pdfbox='^(name\.abuchen\.portfolio\.pdfbox[13]|pdfbox|pdfbox-io|fontbox|jempbox)-[0-9]'
sources='\.source[-_]'
swt_fragments='^org\.eclipse\.swt\.[a-z0-9]+\.[a-z0-9]+\.[a-z0-9_]+-[0-9]'

# keep one jar per name, without sources, pdfbox or an SWT fragment other than ours
tab=$(printf '\t')
jars=$(
    while IFS= read -r path; do
        [ -n "$path" ] || continue
        name=$(basename "$path")
        if printf '%s\n' "$name" | grep -Eq "$sources|$pdfbox"; then continue; fi
        if printf '%s\n' "$name" | grep -Eq "$swt_fragments" && ! printf '%s\n' "$name" | grep -q "^$swt_fragment-"; then continue; fi
        printf '%s\t%s\n' "$name" "$path"
    done <"$list_file" | sort -t "$tab" -u -k1,1
)

count_jars() { printf '%s\n' "$jars" | cut -f1 | grep -Ec "$1" || true; }
[ "$(count_jars "^$swt_fragment-")" -eq 1 ] || die "Expected exactly one $swt_fragment jar."
[ "$(count_jars "$swt_fragments")" -eq 1 ] || die 'A foreign SWT fragment is on the classpath.'
[ "$(count_jars "$pdfbox")" -eq 0 ] || die 'A pdfbox jar is on the classpath.'
[ "$(count_jars '^name\.abuchen\.portfolio-[0-9]')" -eq 1 ] || die 'The core jar is missing from the dependency list.'
[ "$(count_jars '^name\.abuchen\.portfolio\.cli-[0-9]')" -eq 1 ] || die 'The CLI jar is missing from the dependency list.'

rm -rf "$bundle_dir"
lib_dir=$bundle_dir/lib
mkdir -p "$lib_dir"
printf '%s\n' "$jars" | cut -f2 | while IFS= read -r path; do cp "$path" "$lib_dir/"; done

# Modules the classpath needs, plus the ones only reached reflectively or by service lookup
deps=$("$JAVA_HOME/bin/jdeps" --multi-release 21 --ignore-missing-deps --print-module-deps -R \
    --class-path "$lib_dir/*" "$lib_dir"/name.abuchen.portfolio*.jar)
modules=$(printf '%s,jdk.crypto.ec,jdk.localedata,jdk.unsupported\n' "$deps" | tr ',' '\n' | sort -u | paste -sd, -)
"$JAVA_HOME/bin/jlink" --add-modules "$modules" --strip-debug --no-header-files --no-man-pages \
    --compress zip-6 --generate-cds-archive --output "$bundle_dir/runtime"

cp "$here/pp-cli" "$here/install.sh" "$here/smoke-check.sh" "$bundle_dir/"
chmod 755 "$bundle_dir/pp-cli" "$bundle_dir/install.sh" "$bundle_dir/smoke-check.sh"
mkdir "$bundle_dir/sample"
cp "$repo_root/name.abuchen.portfolio.cli.tests/src/name/abuchen/portfolio/cli/historic/client69.xml" "$bundle_dir/sample/sample-client.xml"

echo "Bundle ready: $bundle_dir ($(printf '%s\n' "$jars" | wc -l | tr -d ' ') jars, modules: $modules)"
echo "Install it with: $bundle_dir/install.sh"
