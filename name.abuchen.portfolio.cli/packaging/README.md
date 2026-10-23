# Packaging and installing `pp-cli`

`pp-cli` ships as a self-contained bundle per platform, built locally on that platform
([ADR 0003](../../docs/adr/0003-package-cli-as-self-contained-per-platform-bundle.md)). The bundle holds:

- the fork's core and CLI jars plus the target-platform jars on a flat classpath, collected by Tycho
  (`tycho-dependency-tools:list-dependencies`, wired up by the `cli-bundle` Maven profile of this module);
- the platform's SWT fragment, and **no pdfbox** (so no PDF import);
- a jlink runtime, so no Java has to be installed;
- a launcher, and an install and update script that also generates a dynamic AppCDS archive
  (measured here: about 0.36 s per call instead of 0.43 s), plus an uninstall script.

Documented platforms: [Windows x64](#windows-x64) and [macOS arm64](#macos-arm64).

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

### Uninstall

```powershell
powershell -ExecutionPolicy Bypass -File "$env:LOCALAPPDATA\pp-cli\uninstall.ps1"
```

Run the copy in the install directory: it uninstalls the directory it sits in (also a custom one, without
`-InstallDir`) and can delete its own directory. Pass `-InstallDir <path>` to uninstall another directory
and `-SkipPath` to leave `PATH` alone. The script is idempotent: with nothing installed it exits 0 and says there is
nothing to remove. It removes only what `install.ps1` created:

- the install directory, including the bundled runtime and the AppCDS archive. It is refused, with nothing
  changed, unless it has `pp-cli.cmd`, `lib` and `runtime`;
- the user `PATH` entry, only if it equals the install directory (compared like the installer does); other
  entries stay, and running programs are notified of the change.

`backups` folders and `.<name>.pp-cli.lock` files next to your client files are kept, because `restore` depends
on them. Close any running `pp-cli` first, and open a new terminal to see the `PATH` change.

`packaging\test\test-uninstall.ps1` tests the script in temp directories. It backs up and restores the real user `PATH`.

## macOS arm64

### Prerequisites

- An Apple silicon Mac and an arm64 shell. A Rosetta shell reports `x86_64` and the build refuses it.
- A Java 21 JDK (with `jmods`) and `JAVA_HOME` pointing to it, for example
  `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`. This is needed to build only; the installed CLI uses
  its own runtime.
- Maven on `PATH` and network access for the first build, as for any build of this repository.

### Build

From the repository root:

```sh
name.abuchen.portfolio.cli/packaging/macos/build-bundle.sh
```

This runs the Tycho build, filters the collected jars and writes
`name.abuchen.portfolio.cli/target/pp-cli-macos-arm64/`. The build fails if a pdfbox jar or an SWT fragment
other than `org.eclipse.swt.cocoa.macosx.aarch64` is on the classpath. Use `--skip-maven` to reuse the last
`target/dependencies-list.txt`. Nothing is downloaded at run time, so no signing or notarization is needed.

### Install

```sh
name.abuchen.portfolio.cli/target/pp-cli-macos-arm64/install.sh
```

The script is idempotent. It replaces the installation in `~/.pp-cli/`, regenerates the AppCDS archive with one
training run and wires `pp-cli` onto `PATH` once:

- If `/opt/homebrew/bin` exists and is writable, it symlinks `pp-cli` there (replacing an earlier symlink; a
  regular file of that name is refused).
- Otherwise it appends one `PATH` line, tagged `# pp-cli`, to `~/.zprofile` and `~/.zshenv` (under `$ZDOTDIR`
  if that is set), unless the line is already there.

Use `--install-dir <path>` to install elsewhere, `--link-dir <path>` to pick another link directory and
`--skip-path` to leave `PATH` alone.

### Confirm `PATH`

Agent shells run `zsh -c` (non-interactive: reads `~/.zshenv`) or `zsh -lc` (login: also reads
`~/.zprofile`). Both are covered by the install. The Homebrew link relies on `/opt/homebrew/bin` being on the
`PATH` the shell starts with, as it is wherever Homebrew's `brew shellenv` runs in a profile. Restart VS Code
(the Copilot and Claude Code shells inherit its environment), then in each shell:

| Shell | Command | Expected |
| --- | --- | --- |
| VS Code terminal, Copilot agent shell, Claude Code | `command -v pp-cli` | `/opt/homebrew/bin/pp-cli`, or `~/.pp-cli/pp-cli` without Homebrew |

Then `pp-cli describe` should print JSON. The smoke check below does this from fresh shells for you.

### Smoke check

A release-time check, not part of the Maven tests:

```sh
~/.pp-cli/smoke-check.sh
```

It verifies the installed bundle (bundled arm64 runtime, AppCDS archive, no pdfbox, the one SWT fragment) and
runs `pp-cli info` and `pp-cli describe` against the bundled sample file from fresh `zsh -c` and `zsh -lc`
processes, as an agent shell starts them. They get an empty environment with only a minimal `PATH` (plus
`/opt/homebrew/bin` when `pp-cli` is linked there) and no `JAVA_HOME`, and `pp-cli` must resolve to the
expected path. It exits with 1 if anything fails. Use `--sample-file` for another client file, and
`--install-dir` and `--link-dir` if you installed elsewhere.

### Update

Keep the CLI core equal to the installed desktop release ([Matching the desktop release](#matching-the-desktop-release)).

1. Merge the upstream release tag of the desktop release you run into the fork, so core and CLI are built
   from it, and set `PINNED_UPSTREAM_TAG` in `CoreVersion` to that tag.
2. Run `build-bundle.sh` again.
3. Run the new bundle's `install.sh`, then the smoke check.

### Uninstall

```sh
~/.pp-cli/uninstall.sh
```

Run the copy in the install directory: it uninstalls the directory it sits in (also a custom one, without
`--install-dir`). Pass `--install-dir <path>` to uninstall another directory, `--link-dir <path>` if you installed
with another link directory and `--skip-path` to leave the symlink and the profiles alone. The script is
idempotent: with nothing installed it exits 0 and says there is nothing to remove. It removes only what
`install.sh` created:

- the install directory, including the bundled runtime and the AppCDS archive. It is refused, with nothing
  changed, unless it has `pp-cli`, `lib` and `runtime`;
- the symlink `<link-dir>/pp-cli`, only if it points to this installation's `pp-cli`. A regular file or a link to
  anything else stays, with a warning;
- the `# pp-cli` `PATH` line in `~/.zprofile` and `~/.zshenv`, and under `$ZDOTDIR` when that is set, only if it is
  exactly the line the installer writes for this directory. Each file it changes is named, and a file the line
  leaves empty (one the installer created) is deleted.

`backups` folders and `.<name>.pp-cli.lock` files next to your client files are kept, because `restore` depends
on them. Open a new terminal to see the `PATH` change.

`packaging/test/test-uninstall.sh` tests the script with `sh`, round-tripping the real `install.sh` against a fake
bundle with temp `HOME`, `ZDOTDIR` and link directories, so your profiles are never edited.
