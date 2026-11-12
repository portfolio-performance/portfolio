#!/bin/sh
# Removes what install.sh created for the current user.
#
# Deletes the install directory (including the bundled runtime and the AppCDS archive), the
# pp-cli symlink in the link directory and the PATH lines in the zsh profiles. Backups and lock
# files next to client files are never touched, because `pp-cli restore` depends on the backups.
# Running it again with nothing installed exits 0. It also removes the agent skill: the
# ~/.claude/skills/pp-cli symlink (never followed) and the ~/.agents/skills/pp-cli folder.
#
# Run the copy in the install directory: it uninstalls the directory it sits in. Pass
# --install-dir to uninstall another directory.
#
# Usage: uninstall.sh [--install-dir DIR] [--link-dir DIR] [--skip-path] [--skip-skill]
#                     [--agents-skills-dir DIR] [--claude-skills-dir DIR]
#   --install-dir        what to uninstall (default: the directory of this script when it is an
#                        installed copy, i.e. install.sh generated pp-cli.jsa next to it, otherwise ~/.pp-cli)
#   --link-dir           the directory install.sh symlinked pp-cli into (default /opt/homebrew/bin)
#   --skip-path          do not touch the symlink or the zsh profiles
#   --skip-skill         leave the agent skill in place
#   --agents-skills-dir  where the canonical copy of the skill is (default ~/.agents/skills)
#   --claude-skills-dir  where the Claude Code symlink is (default ~/.claude/skills)
set -eu

script_dir=$(cd "$(dirname "$0")" && pwd -P)
install_dir=
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

if [ -z "$install_dir" ]; then
    if [ -f "$script_dir/pp-cli.jsa" ]; then install_dir=$script_dir; else install_dir=$HOME/.pp-cli; fi
fi
if [ -d "$install_dir" ]; then
    install_dir=$(cd "$install_dir" && pwd -P)
    for name in pp-cli lib runtime; do
        [ -e "$install_dir/$name" ] || die "$install_dir does not look like a pp-cli installation ($name is missing). Nothing was removed."
    done
    exists=true
else
    # resolved like install.sh does, so the symlink and profile checks still match after a partial uninstall
    parent=$(dirname "$install_dir")
    if [ -d "$parent" ]; then install_dir=$(cd "$parent" && pwd -P)/$(basename "$install_dir"); fi
    install_dir=${install_dir%/}
    exists=false
fi

removed=false

# remove_profile_line <profile>: deletes the exact line install.sh wrote; a file the line leaves empty is deleted
remove_profile_line() {
    profile=$1
    if [ ! -f "$profile" ] || ! grep -qxF "$line" "$profile"; then return; fi
    remaining=$(mktemp "${TMPDIR:-/tmp}/pp-cli-uninstall.XXXXXX")
    grep -vxF "$line" "$profile" >"$remaining" || true
    # written back in place, so the file keeps its permissions and links
    cat "$remaining" >"$profile"
    rm -f "$remaining"
    if [ -s "$profile" ]; then
        echo "Removed the PATH line from $profile."
    else
        # the installer created this file just for the line
        rm "$profile"
        echo "Removed the PATH line from $profile, and the file, which is now empty."
    fi
    removed=true
}

if [ "$skip_path" = true ]; then
    echo 'Skipped the symlink and the zsh profiles (--skip-path).'
else
    link=$link_dir/pp-cli
    if [ -L "$link" ] && [ "$(readlink "$link")" = "$install_dir/pp-cli" ]; then
        rm "$link"
        echo "Removed the symlink $link."
        removed=true
    elif [ -e "$link" ] || [ -L "$link" ]; then
        echo "Warning: $link is not a symlink to $install_dir/pp-cli; left in place." >&2
    else
        echo "No symlink at $link."
    fi

    # the line install.sh writes, built the same way
    case $install_dir in
        "$HOME"/*) profile_dir="\$HOME/${install_dir#"$HOME"/}" ;;
        *) profile_dir=$install_dir ;;
    esac
    line="case \":\$PATH:\" in *\":$profile_dir:\"*) ;; *) export PATH=\"$profile_dir:\$PATH\" ;; esac # pp-cli"
    remove_profile_line "$HOME/.zprofile"
    remove_profile_line "$HOME/.zshenv"
    if [ -n "${ZDOTDIR:-}" ] && [ "$ZDOTDIR" != "$HOME" ]; then
        remove_profile_line "$ZDOTDIR/.zprofile"
        remove_profile_line "$ZDOTDIR/.zshenv"
    fi
fi

# remove_skill_entry <path>: removes whatever is there; a symlink is removed itself and never followed
remove_skill_entry() {
    if [ -L "$1" ]; then
        rm "$1"
        echo "Removed the symlink $1"
        removed=true
    elif [ -e "$1" ]; then
        rm -rf "${1:?}"
        echo "Removed $1"
        removed=true
    else
        echo "Nothing at $1."
    fi
}

if [ "$skip_skill" = true ]; then
    echo 'Skipped the agent skill (--skip-skill).'
else
    # the symlink first, so it is never left dangling
    remove_skill_entry "${claude_skills_dir%/}/pp-cli"
    remove_skill_entry "${agents_skills_dir%/}/pp-cli"
fi

if [ "$exists" = true ]; then
    rm -rf "${install_dir:?}"
    echo "Removed $install_dir"
    removed=true
else
    echo "$install_dir does not exist; nothing to remove there."
fi

if [ "$removed" = true ]; then
    echo 'Kept the backups folders and .<name>.pp-cli.lock files next to your client files, because restore needs them.'
else
    echo 'Nothing to remove.'
fi
