# 32: macOS arm64 bundle, install script and docs

Type: task
Status: done
Blocked by: 31

Spec: [spec.md](../spec.md), user stories 1–10 (macOS parts), "Packaging and distribution" and seam 2. ADR 0003.

Note: this ticket must be built and verified natively on the owner's macOS arm64 machine.

## What to build

The same self-contained bundle as ticket 31, for macOS arm64:

- the macOS arm64 SWT fragment on the flat classpath, with pdfbox excluded;
- a bundled jlink runtime;
- a POSIX `sh` launcher named `pp-cli` that uses the AppCDS archive.

An idempotent install and update script installs the bundle to `~/.pp-cli/`. If `/opt/homebrew/bin` exists it symlinks `pp-cli` there; otherwise it appends the install directory to `~/.zprofile` and `~/.zshenv`, once. It generates the dynamic AppCDS archive. The per-platform docs gain a macOS section with the same coverage as Windows, including confirming that non-interactive Claude Code and Copilot shells resolve `pp-cli`. The seam-2 smoke check runs on macOS.

## Acceptance criteria

- [x] A documented local build on macOS arm64 produces the bundle with the correct SWT fragment and no pdfbox jars
- [x] Running the installer twice gives the same result, with no duplicate profile lines or broken symlinks
- [x] The smoke check passes from a fresh non-interactive zsh, as an agent shell would start it
- [x] The macOS docs cover build, install, `PATH` confirmation for both agent shells, update and the version-matching caveat

_Notes: the scripts are in `name.abuchen.portfolio.cli/packaging/macos/`, the docs in a macOS section of `packaging/README.md`. Verified with a temp `HOME` and `--install-dir`, with and without a link directory, running the installer twice and the smoke check from `zsh -c` and `zsh -lc`. The owner's real install is still to be run. The owner's shell sets `ZDOTDIR`, so the installer writes the profile lines there; test runs need `env -u ZDOTDIR` or they edit the real profiles._
