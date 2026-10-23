# Packaging and installing `pp-cli`

`pp-cli` ships as a self-contained bundle per platform, built locally on that platform
([ADR 0003](../../docs/adr/0003-package-cli-as-self-contained-per-platform-bundle.md)). The bundle holds:

- the fork's core and CLI jars plus the target-platform jars on a flat classpath, collected by Tycho
  (`tycho-dependency-tools:list-dependencies`, wired up by the `cli-bundle` Maven profile of this module);
- the platform's SWT fragment, and **no pdfbox** (so no PDF import);
- a jlink runtime, so no Java has to be installed;
- a launcher, and an install and update script that also generates a dynamic AppCDS archive
  (measured here: about 0.36 s per call instead of 0.43 s).

Currently documented: [Windows x64](#windows-x64). macOS arm64 follows the same layout.

## Matching the desktop release

The CLI's core is built from the pinned upstream tag reported by `pp-cli info` and `pp-cli describe`
(`pinnedUpstreamTag`, [CoreVersion](../src/name/abuchen/portfolio/cli/CoreVersion.java)). **Keep it equal to the installed
desktop release.**

- A CLI core older than the desktop app refuses the files the app saved (`file-version-unsupported`).
- A CLI core newer than the desktop app migrates the file when it saves it, and the older app can then no
  longer open it. The migration is reported as a warning in `meta`.

The CLI cannot see edits that are unsaved inside the running desktop app. Save and close the file in the
app before an agent writes to it, and reopen it afterwards.

## Windows x64

### Prerequisites

- A Java 21 JDK (with `jmods`) and `JAVA_HOME` pointing to it. This is needed to build only; the installed
  CLI uses its own runtime.
- Maven on `PATH` and network access for the first build, as for any build of this repository.
- Windows PowerShell 5.1 or PowerShell 7. Git for Windows is needed only for the Git Bash check.

### Build

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File name.abuchen.portfolio.cli\packaging\windows\build-bundle.ps1
```

This runs the Tycho build, filters the collected jars and writes
`name.abuchen.portfolio.cli\target\pp-cli-windows-x64\`. The build fails if a pdfbox jar or an SWT fragment
other than `org.eclipse.swt.win32.win32.x86_64` is on the classpath. Use `-SkipMaven` to reuse the last
`target\dependencies-list.txt`.

### Install

```powershell
powershell -ExecutionPolicy Bypass -File name.abuchen.portfolio.cli\target\pp-cli-windows-x64\install.ps1
```

The script is idempotent. It replaces the installation in `%LOCALAPPDATA%\pp-cli\`, adds that directory to the
user `PATH` only if it is not there yet, and regenerates the AppCDS archive with one training run.
Use `-InstallDir <path>` to install elsewhere and `-SkipPath` to leave `PATH` alone.

### Confirm `PATH`

`PATH` changes reach only shells started afterwards. Restart VS Code (its terminals, and the Copilot
and Claude Code shells started from them, inherit its environment), then check in each shell:

| Shell | Command | Expected |
| --- | --- | --- |
| PowerShell, VS Code terminal | `Get-Command pp-cli` | `...\pp-cli\pp-cli.cmd` |
| cmd | `where pp-cli` | `...\pp-cli\pp-cli.cmd` |
| Git Bash | `command -v pp-cli` | `.../pp-cli/pp-cli` (a shim that forwards to `pp-cli.cmd`) |

Then `pp-cli describe` should print JSON.

Launching `pp-cli.cmd` rather than a `.ps1` keeps PowerShell's execution policy out of the way. In cmd, the
working directory is searched before `PATH`, so do not keep another `pp-cli.cmd` in it. A literal `%` in an
argument (a password, say) is expanded by cmd; pass such values through `PP_PASSWORD` instead.

### Smoke check

A release-time check, not part of the Maven tests:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:LOCALAPPDATA\pp-cli\smoke-check.ps1"
```

It verifies the installed bundle (bundled runtime, AppCDS archive, no pdfbox, the one SWT fragment, install
directory on the user `PATH`) and runs `pp-cli info` and `pp-cli describe` against the bundled sample file from fresh
PowerShell, cmd and Git Bash processes. They get the `PATH` stored in the registry, not that of the calling
process, and no `JAVA_HOME` or other Java on `PATH`. It exits with 1 if anything fails. Use `-SampleFile`
for another client file and `-SkipGitBash` without Git for Windows.

### Update

1. Merge the upstream release tag of the desktop release you run into the fork, so core and CLI are built
   from it, and set `PINNED_UPSTREAM_TAG` in `CoreVersion` to that tag.
2. Run `build-bundle.ps1` again.
3. Run the new bundle's `install.ps1`, then the smoke check.
