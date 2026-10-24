#!/bin/sh
# Tests macos/uninstall.sh, the agent skill step of install.sh and the skill check of smoke-check.sh
# in temp directories, round-tripping the real install.sh.
#
# The bundle is a fake: install.sh only needs lib, runtime, a skill and a pp-cli launcher that writes the
# AppCDS archive when asked. HOME, ZDOTDIR, the link directory and the skills directories are temp
# directories, so the real profiles, /opt/homebrew/bin, ~/.agents and ~/.claude are never touched.
# Runs on macOS or Linux. Exits with 1 if anything fails.
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
    mkdir -p "$home" "$zdot" "$link_dir" "$bundle/lib" "$bundle/runtime" "$bundle/sample" "$bundle/skills/pp-cli"
    : >"$bundle/lib/core.jar"
    : >"$bundle/sample/sample-client.xml"
    printf -- '---\nname: pp-cli\ndescription: test\n---\n# pp-cli\n' >"$bundle/skills/pp-cli/SKILL.md"
    printf 'protocol\n' >"$bundle/skills/pp-cli/write-protocol.md"
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

echo 'Installing the skill'
new_sandbox skill
install_dir=$home/.pp-cli
agents=$sandbox/agents
claude=$sandbox/claude
skill_flags="--agents-skills-dir $agents --claude-skills-dir $claude"
# shellcheck disable=SC2086
output=$(run_as "$zdot" sh "$bundle/install.sh" --install-dir "$install_dir" --link-dir "$link_dir/missing" --skip-path $skill_flags); status=$?
check "exit code 0 ($status: $output)" test "$status" -eq 0
check 'the bundle skill is at the canonical path' cmp -s "$bundle/skills/pp-cli/SKILL.md" "$agents/pp-cli/SKILL.md"
check 'the write protocol came along' test -f "$agents/pp-cli/write-protocol.md"
check 'the Claude path is a symlink to the canonical copy' test "$(readlink "$claude/pp-cli")" = "$agents/pp-cli"
check 'the default locations under HOME were not used' test ! -e "$home/.agents" -a ! -e "$home/.claude"
: >"$agents/pp-cli/stale.txt"
rm "$claude/pp-cli"
mkdir "$claude/pp-cli"
: >"$claude/pp-cli/mine.txt"
# shellcheck disable=SC2086
output=$(run_as "$zdot" sh "$bundle/install.sh" --install-dir "$install_dir" --link-dir "$link_dir/missing" --skip-path $skill_flags); status=$?
check "a second install exits 0 ($status: $output)" test "$status" -eq 0
check 'a second install replaced the canonical folder, not merged' test ! -e "$agents/pp-cli/stale.txt" -a -f "$agents/pp-cli/SKILL.md"
check 'a second install replaced a real folder at the Claude path with the symlink' test "$(readlink "$claude/pp-cli")" = "$agents/pp-cli" -a ! -e "$agents/pp-cli/mine.txt"
check 'the symlink resolves to the skill' test -f "$claude/pp-cli/SKILL.md"

echo 'Uninstalling the skill'
# shellcheck disable=SC2086
output=$(run_as "$zdot" sh "$install_dir/uninstall.sh" --link-dir "$link_dir/missing" $skill_flags); status=$?
check "exit code 0 ($status: $output)" test "$status" -eq 0
check 'the symlink is gone, not dangling' test ! -e "$claude/pp-cli" -a ! -L "$claude/pp-cli"
check 'the canonical folder is gone' test ! -e "$agents/pp-cli"
check 'one output line per item' test "$(printf '%s\n' "$output" | grep -cF "$claude/pp-cli")" -eq 1 -a "$(printf '%s\n' "$output" | grep -cF "$agents/pp-cli")" -eq 1
# shellcheck disable=SC2086
output=$(run_as "$zdot" sh "$macos/uninstall.sh" --install-dir "$install_dir" --link-dir "$link_dir/missing" $skill_flags); status=$?
check "a second uninstall exits 0 ($status: $output)" test "$status" -eq 0
check 'a second uninstall says there is nothing to remove' contains "$output" 'Nothing to remove.'

echo 'Removing the Claude link without following it'
mkdir -p "$sandbox/precious" "$agents/pp-cli"
: >"$sandbox/precious/keep.txt"
ln -s "$sandbox/precious" "$claude/pp-cli"
# shellcheck disable=SC2086
output=$(run_as "$zdot" sh "$macos/uninstall.sh" --install-dir "$install_dir" --link-dir "$link_dir/missing" $skill_flags); status=$?
check "exit code 0 ($status: $output)" test "$status" -eq 0
check 'the link is removed' test ! -L "$claude/pp-cli"
check 'what the link pointed to is untouched' test -f "$sandbox/precious/keep.txt"

echo 'Removing a dangling Claude link'
ln -s "$sandbox/gone" "$claude/pp-cli"
check 'the link is dangling before the uninstall' test -L "$claude/pp-cli" -a ! -e "$claude/pp-cli"
# shellcheck disable=SC2086
output=$(run_as "$zdot" sh "$macos/uninstall.sh" --install-dir "$install_dir" --link-dir "$link_dir/missing" $skill_flags); status=$?
check "exit code 0 ($status: $output)" test "$status" -eq 0
check 'the dangling link is removed' test ! -L "$claude/pp-cli"
ln -s "$sandbox/gone" "$claude/pp-cli"
# shellcheck disable=SC2086
output=$(run_as "$zdot" sh "$bundle/install.sh" --install-dir "$install_dir" --link-dir "$link_dir/missing" --skip-path $skill_flags); status=$?
check "an install replaces a dangling link ($status: $output)" test "$status" -eq 0 -a "$(readlink "$claude/pp-cli")" = "$agents/pp-cli"

echo '--skip-skill touches neither skills directory'
new_sandbox skip-skill
install_dir=$home/.pp-cli
agents=$sandbox/agents
claude=$sandbox/claude
# shellcheck disable=SC2086
output=$(run_as "$zdot" sh "$bundle/install.sh" --install-dir "$install_dir" --skip-path --skip-skill --agents-skills-dir "$agents" --claude-skills-dir "$claude"); status=$?
check "install exits 0 ($status: $output)" test "$status" -eq 0
check 'install created no skills directory' test ! -e "$agents" -a ! -e "$claude" -a ! -e "$home/.agents" -a ! -e "$home/.claude"
mkdir -p "$agents/pp-cli" "$claude"
: >"$agents/pp-cli/SKILL.md"
printf 'mine' >"$claude/pp-cli"
output=$(run_as "$zdot" sh "$install_dir/uninstall.sh" --link-dir "$link_dir/missing" --skip-skill --agents-skills-dir "$agents" --claude-skills-dir "$claude"); status=$?
check "uninstall exits 0 ($status: $output)" test "$status" -eq 0
check 'uninstall left both skill entries in place' test -f "$agents/pp-cli/SKILL.md" -a -f "$claude/pp-cli"

echo 'The skills directories default to HOME'
new_sandbox skill-default
install_dir=$home/.pp-cli
output=$(run_as "$zdot" sh "$bundle/install.sh" --install-dir "$install_dir" --link-dir "$link_dir/missing" --skip-path); status=$?
check "install exits 0 ($status: $output)" test "$status" -eq 0
check 'the skill is under ~/.agents/skills and linked from ~/.claude/skills' test -f "$home/.agents/skills/pp-cli/SKILL.md" -a "$(readlink "$home/.claude/skills/pp-cli")" = "$home/.agents/skills/pp-cli"
output=$(run_as "$zdot" sh "$install_dir/uninstall.sh" --link-dir "$link_dir/missing"); status=$?
check "uninstall exits 0 ($status: $output)" test "$status" -eq 0
check 'both default locations are cleaned' test ! -e "$home/.agents/skills/pp-cli" -a ! -L "$home/.claude/skills/pp-cli"

echo 'Smoke check of the skill'
smoke=$macos/smoke-check.sh
smoke_skill() { run_as '' sh "$smoke" --skill-only --agents-skills-dir "$agents" --claude-skills-dir "$claude"; }
new_skill_home() { # name [skill name]
    agents=$root/$1/agents
    claude=$root/$1/claude
    mkdir -p "$agents/pp-cli" "$claude"
    printf -- '---\nname: %s\n---\n' "${2:-pp-cli}" >"$agents/pp-cli/SKILL.md"
}
home=$root/smoke-home
mkdir -p "$home"
new_skill_home smoke-good
ln -s "$agents/pp-cli" "$claude/pp-cli"
output=$(smoke_skill); status=$?
check "passes with both locations and a link to the canonical copy ($status: $output)" test "$status" -eq 0
new_skill_home smoke-no-link
output=$(smoke_skill); status=$?
check 'fails when the Claude link is missing' test "$status" -eq 1
new_skill_home smoke-elsewhere
mkdir "$root/smoke-elsewhere/other"
cp "$agents/pp-cli/SKILL.md" "$root/smoke-elsewhere/other/"
ln -s "$root/smoke-elsewhere/other" "$claude/pp-cli"
output=$(smoke_skill); status=$?
check 'fails when the Claude link points elsewhere' test "$status" -eq 1
new_skill_home smoke-copy
cp -R "$agents/pp-cli" "$claude/pp-cli"
output=$(smoke_skill); status=$?
check 'fails when the Claude path is a copy, not a link' test "$status" -eq 1
new_skill_home smoke-name other-skill
ln -s "$agents/pp-cli" "$claude/pp-cli"
output=$(smoke_skill); status=$?
check 'fails when SKILL.md does not name pp-cli' test "$status" -eq 1
new_skill_home smoke-no-canonical
ln -s "$agents/pp-cli" "$claude/pp-cli"
rm -rf "$agents/pp-cli"
output=$(smoke_skill); status=$?
check 'fails when the canonical folder is missing' test "$status" -eq 1

if [ "$failures" -gt 0 ]; then
    printf '\n%s check(s) failed.\n' "$failures"
    exit 1
fi
printf '\nAll checks passed.\n'
