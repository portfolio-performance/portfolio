#!/bin/sh
# Installs or updates pp-cli for the current user.
#
# Copies this bundle to ~/.pp-cli, wires it onto PATH once and regenerates the AppCDS archive the
# launcher uses. Running it again gives the same result, so updating means rebuilding the bundle
# and running this script again.
#
# PATH wiring: if the link directory exists and is writable (Homebrew's /opt/homebrew/bin),
# pp-cli is symlinked into it. Otherwise the install directory is added to PATH in ~/.zprofile
# and ~/.zshenv (or under $ZDOTDIR), once each.
#
# Usage: install.sh [--install-dir DIR] [--link-dir DIR] [--skip-path] [--skip-skill]
#                   [--agents-skills-dir DIR] [--claude-skills-dir DIR]
#   --install-dir        where to install (default ~/.pp-cli)
#   --link-dir           the directory to symlink pp-cli into (default /opt/homebrew/bin)
#   --skip-path          do not touch PATH
#   --skip-skill         do not install the agent skill
#   --agents-skills-dir  where the canonical copy of the skill goes (default ~/.agents/skills, read by
#                        Copilot, Codex, Cursor and others)
#   --claude-skills-dir  where the symlink for Claude Code goes (default ~/.claude/skills; Claude Code
#                        does not read .agents)
#
# The agent skill is copied to <agents-skills-dir>/pp-cli, replacing whatever is there, and
# <claude-skills-dir>/pp-cli becomes a symlink to it.
set -eu

bundle_dir=$(cd "$(dirname "$0")" && pwd -P)
install_dir=$HOME/.pp-cli
link_dir=/opt/homebrew/bin
skip_path=false
skip_skill=false
agents_skills_dir=$HOME/.agents/skills
claude_skills_dir=$HOME/.claude/skills
while [ $# -gt 0 ]; do
    case $1 in
        --install-dir) install_dir=${2:?--install-dir needs a value}; shift 2 ;;
        --link-dir) link_dir=${2:?--link-dir needs a value}; shift 2 ;;
        --skip-path) skip_path=true; shift ;;
        --skip-skill) skip_skill=true; shift ;;
        --agents-skills-dir) agents_skills_dir=${2:?--agents-skills-dir needs a value}; shift 2 ;;
        --claude-skills-dir) claude_skills_dir=${2:?--claude-skills-dir needs a value}; shift 2 ;;
        *) echo "Unknown option: $1" >&2; exit 2 ;;
    esac
done

die() { echo "$*" >&2; exit 1; }

for name in lib runtime pp-cli; do
    [ -e "$bundle_dir/$name" ] || die "$bundle_dir is not a pp-cli bundle ($name is missing). Run build-bundle.sh first."
done
[ "$skip_skill" = true ] || [ -f "$bundle_dir/skills/pp-cli/SKILL.md" ] ||
    die "$bundle_dir has no agent skill (skills/pp-cli/SKILL.md is missing). Run build-bundle.sh first."
mkdir -p "$install_dir"
install_dir=$(cd "$install_dir" && pwd -P)
[ "$bundle_dir" != "$install_dir" ] || die 'Run install.sh from the built bundle, not from the install directory.'

managed='lib runtime sample pp-cli pp-cli.jsa install.sh uninstall.sh smoke-check.sh'
for name in $managed; do
    rm -rf "${install_dir:?}/$name"
done
for name in $managed; do
    if [ -e "$bundle_dir/$name" ]; then cp -R "$bundle_dir/$name" "$install_dir/$name"; fi
done
echo "Installed pp-cli to $install_dir"

# one representative run: loads a file and computes holdings, so those classes land in the archive
if ! PP_CLI_BUILD_ARCHIVE=1 "$install_dir/pp-cli" holdings --file "$install_dir/sample/sample-client.xml" >/dev/null 2>&1 ||
    [ ! -f "$install_dir/pp-cli.jsa" ]; then
    die 'Generating the AppCDS archive failed.'
fi
echo 'Generated the AppCDS archive.'

if [ "$skip_skill" = true ]; then
    echo 'Skipped the agent skill (--skip-skill).'
else
    mkdir -p "$agents_skills_dir" "$claude_skills_dir"
    canonical=$(cd "$agents_skills_dir" && pwd -P)/pp-cli
    claude_link=$(cd "$claude_skills_dir" && pwd -P)/pp-cli
    # both entries belong to the installer, whatever they are; rm removes a symlink itself, not its target
    rm -rf "${claude_link:?}" "${canonical:?}"
    cp -R "$bundle_dir/skills/pp-cli" "$canonical"
    echo "Installed the agent skill to $canonical"
    ln -sfn "$canonical" "$claude_link"
    echo "Linked $claude_link to $canonical"
fi

[ "$skip_path" = false ] || exit 0

if [ -d "$link_dir" ] && [ -w "$link_dir" ]; then
    if [ -e "$link_dir/pp-cli" ] && [ ! -L "$link_dir/pp-cli" ]; then
        die "$link_dir/pp-cli exists and is not a symlink; remove it and run install.sh again."
    fi
    ln -sfn "$install_dir/pp-cli" "$link_dir/pp-cli"
    echo "Linked $link_dir/pp-cli to $install_dir/pp-cli."
    exit 0
fi
[ ! -d "$link_dir" ] || echo "$link_dir is not writable; adding $install_dir to the zsh profiles instead."

# the line is written with $HOME unexpanded where possible, so the profile survives a moved home
case $install_dir in
    "$HOME"/*) profile_dir="\$HOME/${install_dir#"$HOME"/}" ;;
    *) profile_dir=$install_dir ;;
esac
line="case \":\$PATH:\" in *\":$profile_dir:\"*) ;; *) export PATH=\"$profile_dir:\$PATH\" ;; esac # pp-cli"
for profile in "${ZDOTDIR:-$HOME}/.zprofile" "${ZDOTDIR:-$HOME}/.zshenv"; do
    if [ -f "$profile" ] && grep -qxF "$line" "$profile"; then
        echo "$profile already has the PATH line."
        continue
    fi
    # a file that does not end in a newline would swallow the line
    if [ -s "$profile" ] && [ -n "$(tail -c 1 "$profile")" ]; then printf '\n' >>"$profile"; fi
    printf '%s\n' "$line" >>"$profile"
    echo "Added the PATH line to $profile."
done
echo 'Open a new terminal (restart VS Code) to pick it up.'
