# Packaging and installing `pp-cli`

`pp-cli` ships as a self-contained bundle per platform, built locally on that platform
([ADR 0003](../../docs/adr/0003-package-cli-as-self-contained-per-platform-bundle.md)). The bundle holds:

- the fork's core and CLI jars plus the target-platform jars on a flat classpath, collected by Tycho
  (`tycho-dependency-tools:list-dependencies`, wired up by the `cli-bundle` Maven profile of this module);
- the platform's SWT fragment, and **no pdfbox** (so no PDF import);
- a jlink runtime, so no Java has to be installed;
- a launcher, and an install and update script that also generates a dynamic AppCDS archive
  (measured here: about 0.36 s per call instead of 0.43 s), plus an uninstall script;
- the [agent skill](#agent-skill) for `pp-cli`, which the install script puts where Copilot and Claude Code find it.

Documented platforms: [Windows x64](#windows-x64) and [macOS arm64](#macos-arm64).

## Agent skill

The bundle carries the `pp-cli` agent skill (source: [skills/pp-cli/](../skills/pp-cli)). The install script
installs it with `pp-cli`, so the skill always matches the installed CLI, and the uninstall script removes it:

| Location | What | Read by |
| --- | --- | --- |
| `~/.agents/skills/pp-cli/` | the canonical copy | Copilot (VS Code), Codex, Cursor and others |
| `~/.claude/skills/pp-cli` | a symlink to the canonical copy | Claude Code |

There are two locations because Claude Code reads personal skills only from `~/.claude/skills/` (it follows a
symlinked skill folder), while the other tools read `~/.agents/skills/`. Both entries belong to the installer:
install replaces them whatever they are, and uninstall removes them (the symlink itself, never what it points to).

- **Skip it**: `install.ps1 -SkipSkill` or `install.sh --skip-skill` touches neither directory, and neither does the
  same flag on uninstall. Tell the smoke check too (`-SkipSkill` / `--skip-skill`).
- **Other directories**: `-AgentsSkillsDir` and `-ClaudeSkillsDir` (macOS: `--agents-skills-dir` and
  `--claude-skills-dir`) replace the two skills directories, on install, uninstall and smoke check. The tests use them
  so they never touch your home folder.
- **Windows privilege**: the installer stays non-admin, but creating a symlink needs an elevated shell or Developer
  Mode. Without it the installer asks Windows for elevation (a UAC prompt) for the link step alone. If you decline,
  or `-NoElevate` is given, it still does everything else (the `%LOCALAPPDATA%\pp-cli` install, the AppCDS archive,
  the `PATH` entry and the `~\.agents` copy), prints the elevated `New-Item -ItemType SymbolicLink ...` command that
  completes the link and exits with 1. Run that command in an elevated PowerShell, or turn on Developer Mode and run
  `install.ps1` again. macOS needs no privilege.
- **Check by hand once per machine**: start a session in Copilot (VS Code) and in Claude Code and confirm that each
  lists the `pp-cli` skill, for example by asking about the holdings of a client file.

## Matching the desktop release

The CLI's core is built from the pinned upstream tag reported by `pp-cli info` and the global
`pp-cli --help` (`pinnedUpstreamTag`, [CoreVersion](../src/name/abuchen/portfolio/cli/CoreVersion.java)). **Keep it equal to the installed
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
user `PATH` only if it is not there yet, and regenerates the AppCDS archive with one training run. It also installs
the [agent skill](#agent-skill) and links it for Claude Code.
Use `-InstallDir <path>` to install elsewhere, `-SkipPath` to leave `PATH` alone, `-SkipSkill` to leave the skill out and
`-NoElevate` to never show the UAC prompt for the Claude link.
Without symlink privilege the script asks for elevation for that one step; if that is declined or unavailable it finishes
everything else and exits with 1 after printing the elevated command that creates the Claude link.

### Confirm `PATH`

`PATH` changes reach only shells started afterwards. Restart VS Code (its terminals, and the Copilot
and Claude Code shells started from them, inherit its environment), then check in each shell:

| Shell | Command | Expected |
| --- | --- | --- |
| PowerShell, VS Code terminal | `Get-Command pp-cli` | `...\pp-cli\pp-cli.cmd` |
| cmd | `where pp-cli` | `...\pp-cli\pp-cli.cmd` |
| Git Bash | `command -v pp-cli` | `.../pp-cli/pp-cli` (a shim that forwards to `pp-cli.cmd`) |

Then `pp-cli --help` should print the usage text.

Launching `pp-cli.cmd` rather than a `.ps1` keeps PowerShell's execution policy out of the way. In cmd, the
working directory is searched before `PATH`, so do not keep another `pp-cli.cmd` in it. A literal `%` in an
argument (a password, say) is expanded by cmd; pass such values through `PP_PASSWORD` instead.

### Smoke check

A release-time check, not part of the Maven tests:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:LOCALAPPDATA\pp-cli\smoke-check.ps1"
```

It verifies the installed bundle (bundled runtime, AppCDS archive, no pdfbox, the one SWT fragment, install
directory on the user `PATH`) and runs `pp-cli info` against the bundled sample file and `pp-cli --help` from fresh
PowerShell, cmd and Git Bash processes. They get the `PATH` stored in the registry, not that of the calling
process, and no `JAVA_HOME` or other Java on `PATH`. It also checks the agent skill: the canonical copy exists with
`SKILL.md` named `pp-cli`, and `~\.claude\skills\pp-cli` is a link that resolves to it. It exits with 1 if anything
fails. Use `-SampleFile` for another client file, `-SkipGitBash` without Git for Windows, `-SkipSkill` after an
install with `-SkipSkill` and `-SkillOnly` to check just the skill.

### Update

1. Merge the upstream release tag of the desktop release you run into the fork, so core and CLI are built
   from it, and set `PINNED_UPSTREAM_TAG` in `CoreVersion` to that tag.
2. Run `build-bundle.ps1` again.
3. Run the new bundle's `install.ps1` (it also replaces the agent skill with the one of the new bundle), then the
   smoke check.

### Uninstall

```powershell
powershell -ExecutionPolicy Bypass -File "$env:LOCALAPPDATA\pp-cli\uninstall.ps1"
```

Run the copy in the install directory: it uninstalls the directory it sits in (also a custom one, without
`-InstallDir`) and can delete its own directory. Pass `-InstallDir <path>` to uninstall another directory
and `-SkipPath` to leave `PATH` alone and `-SkipSkill` to leave the agent skill in place. The script is idempotent: with nothing installed it exits 0 and says there is
nothing to remove. It removes only what `install.ps1` created:

- the install directory, including the bundled runtime and the AppCDS archive. It is refused, with nothing
  changed, unless it has `pp-cli.cmd`, `lib` and `runtime`;
- the user `PATH` entry, only if it equals the install directory (compared like the installer does); other
  entries stay, and running programs are notified of the change;
- the agent skill: the link `~\.claude\skills\pp-cli` (removed itself, never followed, so no dangling link is left) and
  the folder `~\.agents\skills\pp-cli`, one output line per item (`-AgentsSkillsDir` and `-ClaudeSkillsDir` name other directories).

`backups` folders and `.<name>.pp-cli.lock` files next to your client files are kept, because `restore` depends
on them. Close any running `pp-cli` first, and open a new terminal to see the `PATH` change.

`packaging\test\test-uninstall.ps1` tests the install, uninstall and skill steps in temp directories (the skills directories are temp ones too). It backs up and restores the real user `PATH`. The symlink itself is only created, and checked, when the shell may create symlinks; otherwise a junction stands in for it and the unprivileged install path is checked instead.

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

Use `--install-dir <path>` to install elsewhere, `--link-dir <path>` to pick another link directory,
`--skip-path` to leave `PATH` alone and `--skip-skill` to leave out the [agent skill](#agent-skill). Unless skipped, the
skill is copied to `~/.agents/skills/pp-cli/` and `~/.claude/skills/pp-cli` becomes a symlink to it (no privilege needed).

### Confirm `PATH`

Agent shells run `zsh -c` (non-interactive: reads `~/.zshenv`) or `zsh -lc` (login: also reads
`~/.zprofile`). Both are covered by the install. The Homebrew link relies on `/opt/homebrew/bin` being on the
`PATH` the shell starts with, as it is wherever Homebrew's `brew shellenv` runs in a profile. Restart VS Code
(the Copilot and Claude Code shells inherit its environment), then in each shell:

| Shell | Command | Expected |
| --- | --- | --- |
| VS Code terminal, Copilot agent shell, Claude Code | `command -v pp-cli` | `/opt/homebrew/bin/pp-cli`, or `~/.pp-cli/pp-cli` without Homebrew |

Then `pp-cli --help` should print the usage text. The smoke check below does this from fresh shells for you.

### Smoke check

A release-time check, not part of the Maven tests:

```sh
~/.pp-cli/smoke-check.sh
```

It verifies the installed bundle (bundled arm64 runtime, AppCDS archive, no pdfbox, the one SWT fragment) and
runs `pp-cli info` against the bundled sample file and `pp-cli --help` from fresh `zsh -c` and `zsh -lc`
processes, as an agent shell starts them. They get an empty environment with only a minimal `PATH` (plus
`/opt/homebrew/bin` when `pp-cli` is linked there) and no `JAVA_HOME`, and `pp-cli` must resolve to the
expected path. It also checks the agent skill: the canonical copy exists with `SKILL.md` named `pp-cli`, and
`~/.claude/skills/pp-cli` is a symlink that resolves to it. It exits with 1 if anything fails. Use `--sample-file`
for another client file, `--install-dir` and `--link-dir` if you installed elsewhere, `--skip-skill` after an install with
`--skip-skill` and `--skill-only` to check just the skill.

### Update

Keep the CLI core equal to the installed desktop release ([Matching the desktop release](#matching-the-desktop-release)).

1. Merge the upstream release tag of the desktop release you run into the fork, so core and CLI are built
   from it, and set `PINNED_UPSTREAM_TAG` in `CoreVersion` to that tag.
2. Run `build-bundle.sh` again.
3. Run the new bundle's `install.sh` (it also replaces the agent skill with the one of the new bundle), then the
   smoke check.

### Uninstall

```sh
~/.pp-cli/uninstall.sh
```

Run the copy in the install directory: it uninstalls the directory it sits in (also a custom one, without
`--install-dir`). Pass `--install-dir <path>` to uninstall another directory, `--link-dir <path>` if you installed
with another link directory and `--skip-path` to leave the symlink and the profiles alone, `--skip-skill` to leave the agent
skill in place. The script is
idempotent: with nothing installed it exits 0 and says there is nothing to remove. It removes only what
`install.sh` created:

- the install directory, including the bundled runtime and the AppCDS archive. It is refused, with nothing
  changed, unless it has `pp-cli`, `lib` and `runtime`;
- the symlink `<link-dir>/pp-cli`, only if it points to this installation's `pp-cli`. A regular file or a link to
  anything else stays, with a warning;
- the `# pp-cli` `PATH` line in `~/.zprofile` and `~/.zshenv`, and under `$ZDOTDIR` when that is set, only if it is
  exactly the line the installer writes for this directory. Each file it changes is named, and a file the line
  leaves empty (one the installer created) is deleted;
- the agent skill: the symlink `~/.claude/skills/pp-cli` (removed itself, never followed, so no dangling link is left) and
  the folder `~/.agents/skills/pp-cli`, one output line per item (`--agents-skills-dir` and `--claude-skills-dir` name other directories).

`backups` folders and `.<name>.pp-cli.lock` files next to your client files are kept, because `restore` depends
on them. Open a new terminal to see the `PATH` change.

`packaging/test/test-uninstall.sh` tests the scripts with `sh`, round-tripping the real `install.sh` against a fake
bundle with temp `HOME`, `ZDOTDIR`, link and skills directories, so your profiles and skills are never touched.
