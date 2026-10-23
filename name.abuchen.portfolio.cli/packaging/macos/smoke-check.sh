#!/bin/sh
# Release-time smoke check of the installed pp-cli on macOS arm64.
#
# Checks the installed bundle (own runtime, AppCDS archive, no pdfbox, the cocoa macosx aarch64
# SWT fragment) and runs `pp-cli info` and `pp-cli describe` against a sample file from fresh
# non-interactive zsh processes, as an agent shell starts them: `zsh -c` (reads ~/.zshenv) and
# `zsh -lc` (also reads ~/.zprofile). A fresh shell gets an empty environment with a minimal PATH
# (plus the link directory when pp-cli is symlinked into it) and no JAVA_HOME, so the check
# proves the PATH wiring and the bundled runtime. Exits with 1 if anything fails.
#
# Usage: smoke-check.sh [--install-dir DIR] [--link-dir DIR] [--sample-file FILE]
#   --install-dir  the installation to check (default ~/.pp-cli)
#   --link-dir     where install.sh may have symlinked pp-cli (default /opt/homebrew/bin)
#   --sample-file  the client file to read (default: the sample shipped in the bundle)
set -u

install_dir=$HOME/.pp-cli
link_dir=/opt/homebrew/bin
sample_file=
while [ $# -gt 0 ]; do
    case $1 in
        --install-dir) install_dir=${2:?--install-dir needs a value}; shift 2 ;;
        --link-dir) link_dir=${2:?--link-dir needs a value}; shift 2 ;;
        --sample-file) sample_file=${2:?--sample-file needs a value}; shift 2 ;;
        *) echo "Unknown option: $1" >&2; exit 2 ;;
    esac
done

install_dir=$(cd "$install_dir" 2>/dev/null && pwd -P) || { echo "Not installed: $install_dir" >&2; exit 1; }
[ -n "$sample_file" ] || sample_file=$install_dir/sample/sample-client.xml
swt_fragment=org.eclipse.swt.cocoa.macosx.aarch64
failures=0

check() { # description, then a command whose exit status is the result
    description=$1
    shift
    if "$@" >/dev/null 2>&1; then
        echo "  ok    $description"
    else
        echo "  FAIL  $description"
        failures=$((failures + 1))
    fi
}

no_pdfbox() {
    ! ls "$install_dir/lib" | grep -Eq '^(name\.abuchen\.portfolio\.pdfbox[13]|pdfbox|pdfbox-io|fontbox|jempbox)-[0-9]'
}

one_swt_fragment() {
    fragments=$(ls "$install_dir/lib" | grep -E '^org\.eclipse\.swt\.[a-z0-9]+\.[a-z0-9]+\.[a-z0-9_]+-[0-9]')
    [ "$(printf '%s\n' "$fragments" | wc -l)" -eq 1 ] && case $fragments in "$swt_fragment"-*) true ;; *) false ;; esac
}

runtime_is_arm64() {
    file "$install_dir/runtime/bin/java" | grep -q 'Mach-O 64-bit executable arm64'
}

# -Xshare:on fails instead of silently ignoring an archive the JVM rejects (the launcher falls back quietly)
archive_is_used() {
    "$install_dir/runtime/bin/java" "-XX:SharedArchiveFile=$install_dir/pp-cli.jsa" -Xshare:on -Xlog:disable \
        -cp "$install_dir/lib/*" name.abuchen.portfolio.cli.PpCli describe
}

# The directory the shell is expected to resolve pp-cli to: the Homebrew link when install.sh made one
expected=$install_dir/pp-cli
fresh_path=/usr/bin:/bin:/usr/sbin:/sbin
if [ -L "$link_dir/pp-cli" ] && [ "$(readlink "$link_dir/pp-cli")" = "$install_dir/pp-cli" ]; then
    expected=$link_dir/pp-cli
    fresh_path=$link_dir:$fresh_path
fi

echo "Installed bundle: $install_dir"
check 'the host is macOS arm64' sh -c '[ "$(uname -s)" = Darwin ] && [ "$(uname -m)" = arm64 ]'
check 'bundled runtime is present' test -x "$install_dir/runtime/bin/java"
check 'bundled runtime is arm64' runtime_is_arm64
check 'AppCDS archive is present' test -f "$install_dir/pp-cli.jsa"
check 'the JVM accepts and uses the AppCDS archive' archive_is_used
check 'no pdfbox jars on the classpath' no_pdfbox
check 'exactly one SWT fragment, cocoa macosx aarch64' one_swt_fragment
check "sample file exists ($sample_file)" test -f "$sample_file"
if [ "$failures" -gt 0 ]; then
    printf '\n%s check(s) failed.\n' "$failures"
    exit 1
fi

out=$(mktemp -d "${TMPDIR:-/tmp}/pp-cli-smoke.XXXXXX") || exit 1
trap 'rm -rf "$out"' EXIT

# fresh <zsh flag> <command> [args]: runs the command in a new zsh with an empty environment
fresh() {
    flag=$1
    shift
    (cd "$out" && env -i HOME="$HOME" USER="${USER:-}" LOGNAME="${LOGNAME:-}" TMPDIR="$out" TERM=dumb PATH="$fresh_path" \
        ${ZDOTDIR:+ZDOTDIR="$ZDOTDIR"} /bin/zsh "$flag" "$@") >"$out/stdout" 2>"$out/stderr"
    exit_code=$?
}

# json_check <description> <keypath>... : every key path must exist in the JSON on stdout
json_check() {
    description=$1
    shift
    valid=true
    [ "$exit_code" -eq 0 ] || valid=false
    for keypath in "$@"; do
        plutil -extract "$keypath" raw -o - - <"$out/stdout" >/dev/null 2>&1 || valid=false
    done
    if [ "$valid" = true ]; then
        echo "  ok    $description"
    else
        echo "  FAIL  $description"
        echo "        exit code $exit_code; stdout: $(head -c 600 "$out/stdout"); stderr: $(head -c 600 "$out/stderr")"
        failures=$((failures + 1))
    fi
}

for flag in -c -lc; do
    shell="zsh $flag"
    echo "Fresh $shell"

    fresh "$flag" 'command -v pp-cli'
    if [ "$exit_code" -eq 0 ] && [ "$(cat "$out/stdout")" = "$expected" ]; then
        echo "  ok    $shell: pp-cli resolves to $expected"
    else
        echo "  FAIL  $shell: pp-cli resolves to $expected"
        echo "        exit code $exit_code; stdout: $(head -c 600 "$out/stdout"); stderr: $(head -c 600 "$out/stderr")"
        failures=$((failures + 1))
    fi

    fresh "$flag" 'pp-cli info --file "$1"' pp-cli-smoke "$sample_file"
    json_check "$shell: pp-cli info" data.counts data.pinnedUpstreamTag data.format warnings

    fresh "$flag" 'pp-cli describe'
    json_check "$shell: pp-cli describe" data.commands data.pinnedUpstreamTag data.limits warnings
done

if [ "$failures" -gt 0 ]; then
    printf '\n%s check(s) failed.\n' "$failures"
    exit 1
fi
printf '\nSmoke check passed.\n'
