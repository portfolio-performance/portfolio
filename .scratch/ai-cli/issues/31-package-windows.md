# 31: Windows x64 bundle, install script and docs

Type: task
Status: ready-for-agent
Blocked by: 13

Spec: [spec.md](../spec.md), user stories 1–10 (Windows parts), "Packaging and distribution" and seam 2 under "Testing Decisions". ADR 0003.

## What to build

The owner can build `pp-cli` locally on Windows x64 and run it from any new agent shell. The build produces a self-contained bundle:

- the fork's core jar plus the target-platform jars, collected by Tycho `list-dependencies` onto a flat classpath, with both pdfbox bundles excluded and the Windows SWT fragment included;
- a bundled jlink runtime;
- a `pp-cli.cmd` launcher, so PowerShell's execution policy never blocks it.

An idempotent install and update script installs the bundle under `%LOCALAPPDATA%\pp-cli\`, adds that directory to the user `PATH` once, and generates a dynamic AppCDS archive that the launcher uses.

Written instructions cover building, installing, confirming `PATH` from VS Code terminals, PowerShell, cmd and Git Bash, and updating by bumping the pinned tag, rebuilding and re-running the installer. They also state that the CLI's core must match the installed desktop release, and that unsaved edits inside the app can't be detected.

A scripted smoke check (seam 2) runs the installed `pp-cli info` and `describe` against a sample file from a fresh shell. It is a release-time check, not part of the Maven tests.

## Acceptance criteria

- [x] A documented local build produces the Windows bundle with no pdfbox jars and the correct SWT fragment on the classpath
- [x] The bundle runs with its own runtime on a machine with no other Java on `PATH`
- [x] Running the installer twice gives the same result, with no duplicate `PATH` entries, and it regenerates the AppCDS archive
- [x] The smoke check passes from fresh PowerShell, cmd and Git Bash sessions
- [x] The per-platform docs cover build, install, `PATH` confirmation, update and the version-matching caveat

_Notes for later tickets: the module's README is `name.abuchen.portfolio.cli/packaging/README.md` (macOS gets its own section there); the Windows scripts are in `packaging/windows/`. The `cli-bundle` Maven profile of the CLI module writes `target/dependencies-list.txt`, which lists every platform's SWT fragment and the source jars, so the platform script does the filtering and asserts no pdfbox jar and one SWT fragment. The launcher reads `PP_CLI_BUILD_ARCHIVE` to record the AppCDS archive. Fresh cmd shells search the working directory before `PATH`, so the smoke check starts them in the temp directory. Verified with a temp `-InstallDir`; the owner's real install is still to be run._
