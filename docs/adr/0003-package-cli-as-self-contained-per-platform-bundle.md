# Package the AI-agent CLI as a self-contained, per-platform bundle

The `pp-cli` tool (ADR 0002) must run from the Copilot and Claude Code shells on both of the owner's machines without a separate JDK install. We package it as the fork's core jar plus the target-platform jars collected by Tycho `list-dependencies` on a flat classpath, launched by a thin script against a **bundled jlink runtime**, built locally and natively on each platform. v1 targets **macOS arm64 and Windows x64**; pdfbox is excluded, so there is no PDF statement import.

**Considered Options**

- *Bare fat/shaded jar needing a system Java 21*: rejected. It pushes a JDK install onto the user and still requires merging `META-INF/services` files and stripping signatures.
- *jpackage app image*: rejected. Heavier than a headless CLI needs.
- *Reuse the installed desktop app's JRE and plugin jars*: rejected. It couples to the signed `.app` layout and would run the CLI against a different core than the fork was built from; version skew already exists (installed 0.87.0 vs fork 0.88.1), and the CLI is pinned to its own upstream tag (ticket 08).
- *Cross-building both platforms from one machine*: rejected. Needs foreign jmods and cross-classpath plumbing; native per-machine builds are simpler because both machines are on hand.

**Consequences**

- SWT's classes live in a per-platform fragment that must be on the classpath, so each OS/arch is a distinct artifact and build.
- Dropping pdfbox (it clashes 1.8 vs 3.x on any flat classpath, and ticket 03 runs no OSGi) means no PDF import; the read/write surfaces do not need one.
- A dynamic AppCDS archive is generated per platform at install time to cut per-call startup (~0.5 s → ~0.37 s) for one-JVM-per-command usage.
- One idempotent install/update script per platform owns the install dir and `PATH` wiring (macOS: `~/.pp-cli/` symlinked into `/opt/homebrew/bin` or appended to `~/.zprofile`/`~/.zshenv`; Windows: `%LOCALAPPDATA%\pp-cli\` on the user `PATH`); updating is re-running it after bumping the pinned tag.
- Local-only builds need no Developer ID or notarization because nothing is downloaded.
- Per-platform build, install and launch instructions ship with the module as a required deliverable.
