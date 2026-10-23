# 34: Uninstall scripts for Windows and macOS

Type: task
Status: ready-for-agent
Blocked by: 31, 32

Spec: [spec.md](../spec.md), "Packaging and distribution" (owner user stories for install and update). ADR 0003.

## What to build

The installers from tickets 31 and 32 have no counterpart, so the owner can't cleanly remove `pp-cli`. Each platform gets an idempotent `uninstall.ps1` / `uninstall.sh` that reverses exactly what its installer created.

The script ships in the bundle, is copied into the install directory next to `install.*` (added to the installer's `managed` list) and runs from the installed copy, so uninstalling needs no build tree. On Windows it has to cope with deleting the directory it was started from.

## Decisions

- **Removes only installer-created artifacts**: the install directory (including the AppCDS archive and bundled runtime), the Windows user-`PATH` entry, the macOS symlink and the macOS profile lines.
- **Keeps user data**: `backups/` folders and `.<name>.pp-cli.lock` files next to client files are never touched, because `restore` depends on the backups. The output says they are kept.
- **Default target**: the script's own directory when `pp-cli.jsa` is beside it (only an installed copy has one, because the installer generates it); otherwise the standard directory. `-InstallDir` / `--install-dir` always wins.
- **Interface mirrors the installers**: Windows `-InstallDir` and `-SkipPath`; macOS `--install-dir`, `--link-dir` and `--skip-path`. No confirmation prompt. Output is one line per item removed or skipped.
- **Idempotent**: with nothing installed it exits 0 and says there is nothing to remove.
- **Ownership checks**:
  - The install directory is refused unless it looks like a pp-cli install (the `pp-cli` launcher plus `lib` and `runtime`), so a mistyped path can't wipe an arbitrary folder.
  - The macOS symlink `<link-dir>/pp-cli` is removed only if it is a symlink to this install's `pp-cli`; a real file or another target is left alone with a warning.
  - macOS profile lines are removed only if they equal the exact line the installer writes for this install directory, built with the same `$HOME`-relative logic. Both `$HOME` and `$ZDOTDIR` (when set and different) are checked, and each file touched is named.
  - The Windows `PATH` entry is removed only if its expanded, trimmed, trailing-`\`-stripped value equals the install directory (case-insensitive), the comparison `Add-UserPathEntry` uses. Other entries are untouched, and running programs are notified of the change as on install.
- **Docs and tracking**: an `Uninstall` section per platform in `packaging/README.md`, a one-line update to the Consequences of ADR 0003 ("install/update" becomes "install/update/uninstall"), and an owner user story in `spec.md` with its "install and update script" wording extended. No glossary change and no new ADR: the behavior is easy to reverse and not a trade-off.

## Acceptance criteria

- [ ] After install and uninstall into a temp directory, the directory is gone, a second uninstall exits 0, and the output reports that backups and lock files are kept
- [ ] Windows: the user `PATH` is byte-identical before install and after uninstall, and entries the installer didn't add are untouched
- [ ] macOS: the symlink is gone and the profile files are byte-identical to before install, with a temp `--link-dir` and `ZDOTDIR` used so the real profiles are never edited (the tests need `env -u ZDOTDIR` otherwise)
- [ ] macOS: a foreign `<link-dir>/pp-cli` file or a symlink to another target is left in place with a warning
- [ ] Pointing the script at a directory that isn't a pp-cli install is refused with no side effects
- [ ] The uninstaller runs from the installed copy, including on Windows where it deletes its own directory, and from a custom install directory without needing `-InstallDir`
- [ ] The per-platform docs, ADR 0003 and the spec are updated as described

_Notes: the scripts live in `name.abuchen.portfolio.cli/packaging/windows/` and `packaging/macos/`. The Windows half can be verified on the Windows machine; the macOS script needs a Mac or `sh` for its round-trip run._
