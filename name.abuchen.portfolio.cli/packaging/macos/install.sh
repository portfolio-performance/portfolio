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
# Usage: install.sh [--install-dir DIR] [--link-dir DIR] [--skip-path]
#   --install-dir  where to install (default ~/.pp-cli)
#   --link-dir     the directory to symlink pp-cli into (default /opt/homebrew/bin)
#   --skip-path    do not touch PATH
set -eu

bundle_dir=$(cd "$(dirname "$0")" && pwd -P)
install_dir=$HOME/.pp-cli
link_dir=/opt/homebrew/bin
skip_path=false
while [ $# -gt 0 ]; do
    case $1 in
        --install-dir) install_dir=${2:?--install-dir needs a value}; shift 2 ;;
        --link-dir) link_dir=${2:?--link-dir needs a value}; shift 2 ;;
        --skip-path) skip_path=true; shift ;;
        *) echo "Unknown option: $1" >&2; exit 2 ;;
    esac
done

die() { echo "$*" >&2; exit 1; }

for name in lib runtime pp-cli; do
    [ -e "$bundle_dir/$name" ] || die "$bundle_dir is not a pp-cli bundle ($name is missing). Run build-bundle.sh first."
done
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
