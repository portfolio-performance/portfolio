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

- [ ] A documented local build produces the Windows bundle with no pdfbox jars and the correct SWT fragment on the classpath
- [ ] The bundle runs with its own runtime on a machine with no other Java on `PATH`
- [ ] Running the installer twice gives the same result, with no duplicate `PATH` entries, and it regenerates the AppCDS archive
- [ ] The smoke check passes from fresh PowerShell, cmd and Git Bash sessions
- [ ] The per-platform docs cover build, install, `PATH` confirmation, update and the version-matching caveat
