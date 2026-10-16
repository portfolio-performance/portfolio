# How is the CLI packaged, installed and launched?

Type: grilling
Status: resolved
Blocked by: 09

## Question

Given the options from ticket 09, which packaging does v1 use, how does a user install and update it, and how is `pp-cli` put on the PATH for Copilot and Claude Code shells? Does the build produce it in CI or only locally, and which platforms are supported (personal use, so macOS first)?

## Answer

v1 packages the CLI as a **self-contained, per-platform bundle**: the fork's core jar plus the target-platform jars collected by Tycho `list-dependencies` on a flat classpath, run by a thin launcher against a **bundled jlink runtime**. pdfbox is left out, so there is no PDF import. Each platform is built locally on its own machine, never in CI; **macOS arm64 and Windows x64** are the v1 targets.

### Packaging

- Option (b) from ticket 09: collected jars + bundled jlink runtime + thin launcher. No user-installed JDK, version-locked to the fork's pinned upstream tag (ticket 08), and an all-jar classpath so AppCDS works.
- pdfbox 1.8/3.x clash on any flat classpath and the ticket-03 architecture runs no OSGi launch, so the CLI ships without pdfbox and has **no PDF statement import**.
- Rejected: (a) bare fat jar (needs a system JDK, plus service-file/signature merging), (c) jpackage app image (heavier than a CLI needs), (d) reuse the installed app's JRE/jars (couples to the `.app` layout and would run a different core than the fork was built from; version skew already exists, installed 0.87.0 vs fork 0.88.1).

### Platforms and build

- Cross-platform for personal use: **macOS arm64 and Windows x64**. SWT's classes live in a per-platform fragment that must be on the classpath, so each platform is a distinct artifact.
- **Built locally, natively on each machine.** Tycho resolves the correct SWT fragment and jlink emits the host runtime, so there is no cross-jmod plumbing. No CI; locally built artifacts need no Developer ID or notarization because nothing is downloaded.

### Launcher, install, update, PATH

- Launcher: `pp-cli` POSIX `sh` script on macOS; `pp-cli.cmd` batch wrapper on Windows (runs from cmd, PowerShell, the VS Code terminal and Git Bash without execution-policy friction). Each just execs the bundled runtime with the fixed classpath and the AppCDS archive.
- One idempotent **install/update script per platform** owns layout and PATH, and **updating is re-running it** after bumping the pinned tag:
  - macOS → install to `~/.pp-cli/`; symlink `pp-cli` into `/opt/homebrew/bin` if present, else append the dir to `~/.zprofile` and `~/.zshenv` (agent shells may not source `~/.zshrc`).
  - Windows → install to `%LOCALAPPDATA%\pp-cli\` and add that dir to the user `PATH` (via `setx`/registry), inherited by new VS Code terminals and shells.
- Both the Copilot (VS Code terminal) and Claude Code shells resolve `pp-cli` through `PATH`.

### Startup

- Generate a dynamic **AppCDS** archive per platform at install time; the launcher adds `-XX:SharedArchiveFile`. ~0.5 s → ~0.37 s per command, which matters for one-JVM-per-call agent usage.

### Documentation deliverable

- Ship **per-platform build, install and launch instructions** (macOS arm64 and Windows x64) alongside the module: how to build from the fork, run the install/update script, confirm `pp-cli` is on `PATH` in both agent shells, and how to update by bumping the pinned tag. This is a required v1 deliverable, not optional.

### How it was resolved

Grilled 2026-10-16; the user accepted each recommendation, added the documentation deliverable, and widened the scope from the "macOS first" default to cross-platform.

| Decision | Reasoning |
| --- | --- |
| Option (b): collected jars + bundled jlink runtime + thin launcher | Self-contained, no user JDK, version-locked to the fork's pinned tag, all-jar classpath for AppCDS. |
| No PDF import (drop pdfbox) | pdfbox 1.8/3.x clash on any flat classpath and ticket 03 runs no OSGi; the read/write surfaces need no import. |
| Cross-platform: macOS arm64 + Windows x64 | Personal use across both machines; SWT's per-platform fragment makes each a separate artifact. |
| Built locally, natively on each machine | Tycho resolves the host SWT fragment and jlink emits the host runtime; no cross-jmod plumbing, no signing/notarization for un-downloaded artifacts. |
| `pp-cli` sh launcher + `pp-cli.cmd` wrapper | One thin exec per platform; `.cmd` avoids PowerShell execution-policy friction across agent shells. |
| One idempotent install/update script per platform; install dir + PATH as above; update = re-run | Agent shells may skip `~/.zshrc`; a fixed dir on PATH plus Homebrew symlink / user PATH makes `pp-cli` resolvable, and re-running after a tag bump is the update path. |
| AppCDS generated per platform at install time | ~25% faster per call for one-JVM-per-command usage, with no runtime complexity. |
| Per-platform build/install/launch docs are a required deliverable | The agents cannot use the tool if they cannot build, find or launch it; captured so v1 does not miss it. |

Rejected options:

- **(a) fat jar / (c) jpackage / (d) reuse installed app:** see Packaging above.
- **Cross-building both platforms from one machine:** needs foreign jmods and cross-classpath plumbing; native per-machine builds are simpler with both machines on hand.
- **`.ps1` Windows launcher:** execution-policy friction in agent shells; a `.cmd` wrapper runs everywhere.
- **CI packaging / signing / notarization:** no downloaded artifact to notarize for personal use.
