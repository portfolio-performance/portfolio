#!/bin/sh
# Tests macos/uninstall.sh in temp directories, round-tripping the real install.sh.
#
# The bundle is a fake: install.sh only needs lib, runtime and a pp-cli launcher that writes the
# AppCDS archive when asked. HOME, ZDOTDIR and the link directory are temp directories, so the real
# profiles and /opt/homebrew/bin are never touched. Runs on macOS or Linux. Exits with 1 if
# anything fails.
#
# Usage: sh test-uninstall.sh
set -u

here=$(cd "$(dirname "$0")" && pwd -P)
macos=$here/../macos
root=$(mktemp -d "${TMPDIR:-/tmp}/pp-cli-uninstall-test.XXXXXX") || exit 1
root=$(cd "$root" && pwd -P)
trap 'rm -rf "$root"' EXIT
failures=0

check() { # description, then a command whose exit status is the result
    description=$1
    shift
    if "$@"; then
        echo "  ok    $description"
    else
        echo "  FAIL  $description"
        failures=$((failures + 1))
    fi
}

contains() { case $1 in *"$2"*) true ;; *) false ;; esac; }
lacks() { ! contains "$1" "$2"; }

# a fresh sandbox: $home, $zdot, $link_dir and a fake bundle
new_sandbox() {
    sandbox=$root/$1
    home=$sandbox/home
    zdot=$sandbox/zdot
    link_dir=$sandbox/bin
    bundle=$sandbox/bundle
    mkdir -p "$home" "$zdot" "$link_dir" "$bundle/lib" "$bundle/runtime" "$bundle/sample"
    : >"$bundle/lib/core.jar"
    : >"$bundle/sample/sample-client.xml"
    cat >"$bundle/pp-cli" <<'LAUNCHER'
#!/bin/sh
if [ -n "${PP_CLI_BUILD_ARCHIVE:-}" ]; then : >"$(dirname "$0")/pp-cli.jsa"; fi
LAUNCHER
    chmod 755 "$bundle/pp-cli"
    cp "$macos/install.sh" "$macos/uninstall.sh" "$bundle/"
    printf '# my profile\nexport FOO=1\n' >"$zdot/.zprofile"
    printf 'export BAR=2' >"$zdot/.zshenv"
    printf 'export HOME_PROFILE=1\n' >"$home/.zprofile"
    snapshot "$sandbox/before"
}

# snapshot <dir>: copies the profile files, so they can be compared byte for byte later
snapshot() {
    mkdir -p "$1/home" "$1/zdot"
    for file in home/.zprofile home/.zshenv zdot/.zprofile zdot/.zshenv; do
        if [ -f "$sandbox/$file" ]; then cp "$sandbox/$file" "$1/$file"; fi
    done
}

same_profiles() {
    snapshot "$sandbox/after"
    diff -r "$sandbox/before" "$sandbox/after" >/dev/null
}

# run_as <zdotdir or empty> <command> [args]: runs in a clean environment with the sandbox HOME
run_as() {
    zdotdir=$1
    shift
    if [ -n "$zdotdir" ]; then
        env -i PATH="$PATH" HOME="$home" ZDOTDIR="$zdotdir" TMPDIR="$root" "$@" 2>&1
    else
        env -i PATH="$PATH" HOME="$home" TMPDIR="$root" "$@" 2>&1
    fi
}

echo 'Uninstalling the Homebrew-style symlink install from the installed copy'
new_sandbox link
install_dir=$home/.pp-cli
run_as "$zdot" sh "$bundle/install.sh" --install-dir "$install_dir" --link-dir "$link_dir" >/dev/null
check 'the install created the symlink' test -L "$link_dir/pp-cli"
check 'the installed copy of uninstall.sh exists' test -f "$install_dir/uninstall.sh"
mkdir -p "$sandbox/client/backups"
: >"$sandbox/client/backups/client.xml"
: >"$sandbox/client/.client.xml.pp-cli.lock"
output=$(run_as "$zdot" sh "$install_dir/uninstall.sh" --link-dir "$link_dir"); status=$?
check "exit code 0 ($status: $output)" test "$status" -eq 0
check 'the install directory is gone, with the script that ran from it' test ! -e "$install_dir"
check 'the symlink is gone' test ! -L "$link_dir/pp-cli"
check 'the profile files are byte-identical' same_profiles
check 'the output says backups and lock files are kept' contains "$output" 'Kept the backups folders and .<name>.pp-cli.lock files'
check 'backups and lock files are untouched' test -f "$sandbox/client/backups/client.xml" -a -f "$sandbox/client/.client.xml.pp-cli.lock"
output=$(run_as "$zdot" sh "$macos/uninstall.sh" --install-dir "$install_dir" --link-dir "$link_dir"); status=$?
check "a second run exits 0 ($status: $output)" test "$status" -eq 0
check 'a second run says there is nothing to remove' contains "$output" 'Nothing to remove.'

echo 'Uninstalling the profile-line install into a custom directory outside HOME'
new_sandbox profiles
install_dir=$sandbox/custom\ install
run_as "$zdot" sh "$bundle/install.sh" --install-dir "$install_dir" --link-dir "$link_dir/missing" >/dev/null
check 'the install added the PATH lines' grep -q '# pp-cli$' "$zdot/.zprofile"
output=$(run_as "$zdot" sh "$install_dir/uninstall.sh" --link-dir "$link_dir/missing"); status=$?
check "exit code 0 ($status: $output)" test "$status" -eq 0
check 'the custom install directory is gone without --install-dir' test ! -e "$install_dir"
check 'the profile files are byte-identical, except for the newline the installer added' test "$(cat "$zdot/.zshenv")" = 'export BAR=2'
check 'the output names each profile it touched' contains "$output" "$zdot/.zprofile"
rm -rf "$sandbox/before/zdot/.zshenv"
printf 'export BAR=2\n' >"$sandbox/before/zdot/.zshenv"
check 'the profile files match the originals' same_profiles

echo 'Removing lines in HOME when ZDOTDIR is set at uninstall time but was not at install time'
new_sandbox home-profiles
install_dir=$home/.pp-cli
run_as '' sh "$bundle/install.sh" --install-dir "$install_dir" --link-dir "$link_dir/missing" >/dev/null
check 'the install added the PATH line under HOME' grep -q '# pp-cli$' "$home/.zprofile"
output=$(run_as "$zdot" sh "$install_dir/uninstall.sh" --link-dir "$link_dir/missing"); status=$?
check "exit code 0 ($status: $output)" test "$status" -eq 0
check 'the HOME profile is byte-identical' same_profiles

echo 'Handling HOME and ZDOTDIR paths with spaces'
new_sandbox spaces
mv "$home" "$sandbox/my home"
home=$sandbox/my\ home
mv "$zdot" "$sandbox/my zdot"
zdot=$sandbox/my\ zdot
cp "$zdot/.zprofile" "$sandbox/zprofile.orig"
install_dir=$home/.pp-cli
run_as "$zdot" sh "$bundle/install.sh" --install-dir "$install_dir" --link-dir "$link_dir/missing" >/dev/null
check 'the install added the PATH line' grep -q '# pp-cli$' "$zdot/.zprofile"
output=$(run_as "$zdot" sh "$install_dir/uninstall.sh" --link-dir "$link_dir/missing"); status=$?
check "exit code 0 ($status: $output)" test "$status" -eq 0
check 'the profile is byte-identical' cmp -s "$zdot/.zprofile" "$sandbox/zprofile.orig"

echo 'Cleaning up the symlink after the install directory is already gone'
new_sandbox partial
install_dir=$home/.pp-cli
run_as "$zdot" sh "$bundle/install.sh" --install-dir "$install_dir" --link-dir "$link_dir" >/dev/null
rm -rf "$install_dir"
output=$(run_as "$zdot" sh "$macos/uninstall.sh" --install-dir "$install_dir" --link-dir "$link_dir"); status=$?
check "exit code 0 ($status: $output)" test "$status" -eq 0
check 'the dangling symlink is removed' test ! -L "$link_dir/pp-cli"

echo 'Leaving a foreign pp-cli in the link directory alone'
new_sandbox foreign
install_dir=$home/.pp-cli
run_as "$zdot" sh "$bundle/install.sh" --install-dir "$install_dir" --link-dir "$link_dir" --skip-path >/dev/null
printf 'real file' >"$link_dir/pp-cli"
output=$(run_as "$zdot" sh "$install_dir/uninstall.sh" --link-dir "$link_dir"); status=$?
check "exit code 0 ($status: $output)" test "$status" -eq 0
check 'a real file is left in place' test -f "$link_dir/pp-cli" -a ! -L "$link_dir/pp-cli"
check 'a warning names it' contains "$output" "Warning: $link_dir/pp-cli"
rm "$link_dir/pp-cli"
ln -s /somewhere/else/pp-cli "$link_dir/pp-cli"
run_as "$zdot" sh "$macos/uninstall.sh" --install-dir "$install_dir" --link-dir "$link_dir" >/dev/null
output=$(run_as "$zdot" sh "$macos/uninstall.sh" --install-dir "$install_dir" --link-dir "$link_dir")
check 'a symlink to another target is left in place' test "$(readlink "$link_dir/pp-cli")" = /somewhere/else/pp-cli
check 'a warning names it' contains "$output" "Warning: $link_dir/pp-cli"

echo 'Refusing a directory that is not a pp-cli installation'
new_sandbox refuse
other=$sandbox/other
mkdir -p "$other/lib"
: >"$other/precious.txt"
output=$(run_as "$zdot" sh "$macos/uninstall.sh" --install-dir "$other" --link-dir "$link_dir"); status=$?
check "exit code is not 0 ($status)" test "$status" -ne 0
check 'the output explains the refusal' contains "$output" 'does not look like a pp-cli installation'
check 'the directory is untouched' test -f "$other/precious.txt"
check 'the profile files are untouched' same_profiles

echo '--skip-path leaves the symlink and the profiles alone'
new_sandbox skip
install_dir=$home/.pp-cli
run_as "$zdot" sh "$bundle/install.sh" --install-dir "$install_dir" --link-dir "$link_dir" >/dev/null
output=$(run_as "$zdot" sh "$install_dir/uninstall.sh" --link-dir "$link_dir" --skip-path); status=$?
check "exit code 0 ($status: $output)" test "$status" -eq 0
check 'the install directory is gone' test ! -e "$install_dir"
check 'the symlink is still there' test -L "$link_dir/pp-cli"

echo 'The default target is the standard directory for a script outside an installation'
new_sandbox default
mkdir -p "$home/.pp-cli/lib" "$home/.pp-cli/runtime"
: >"$home/.pp-cli/pp-cli"
output=$(run_as "$zdot" sh "$bundle/uninstall.sh" --link-dir "$link_dir"); status=$?
check "exit code 0 ($status: $output)" test "$status" -eq 0
check 'the bundle copy removed ~/.pp-cli' test ! -e "$home/.pp-cli"
check 'the bundle itself is untouched' test -f "$bundle/uninstall.sh"

if [ "$failures" -gt 0 ]; then
    printf '\n%s check(s) failed.\n' "$failures"
    exit 1
fi
printf '\nAll checks passed.\n'
