# What are the options for packaging a headless Java 21 CLI built from this Tycho repo?

Type: research
Status: resolved

## Question

How can the CLI module from ticket 03 (core bundle jar plus target-platform jars on a plain classpath) be built and shipped from this Maven/Tycho build: fat/shaded jar with a launcher script, jlink runtime image, jpackage app image, or a launcher placed inside an existing PP install that reuses its bundled JRE and plugin jars? For each, what does it cost to build here (Tycho plugin support, resolving target-platform jars outside OSGi), what does the user need installed, artifact size, startup time per command (one JVM per call), and macOS signing or quarantine issues? Also: how would an agent's shell find the `pp-cli` command (PATH, Homebrew, symlink)?

## Comments

Research findings: branch `research/packaging-options`, file `.scratch/ai-cli/research/packaging-options.md`.

## Answer

- Startup measured with the installed app's JRE and jars: about 0.5 s per load-plus-snapshot run; `-XX:TieredStopAtLevel=1` gives about 0.42 s, plus dynamic AppCDS about 0.37 s. Bare JVM start is 0.03 s, so cost is PP class loading. AppCDS needs an all-jar classpath.
- pdfbox 1.8 and 3.x share package names and only coexist under OSGi; every flat-classpath option must leave pdfbox out (no PDF import) or relocate 1.8 in a fat jar.
- (a) Fat jar: must also merge five `META-INF/services` files, strip signatures (bcprov), keep `Multi-Release`; SWT carries natives for one architecture.
- Collecting jars: Tycho `list-dependencies` writes resolved jar paths to a file and tracks upstream changes on rebuild; hand-declared Maven artifacts drift.
- (b) jlink and (c) jpackage: jlink can only build the runtime (app jars are not modules) and the jars stay on a classpath; runtime image 64 MB (vs 151 MB PP JRE, 335 MB JDK); use `--generate-cds-archive`.
- (d) Reuse installed app: signed Zulu 21.0.5 JRE and `bundles.info` listing active jars; launcher must live outside the `.app` (signature). Local plain product builds include no JRE; only the distro build does. Version skew already exists (installed 0.87.0 vs fork 0.88.1), which ties into ticket 08.
- Signing: locally built artifacts run without Developer ID; notarization only matters for downloads.
- PATH: `~/.local/bin` is not on the macOS default PATH and agent shells may not read `~/.zshrc`; use `~/.zshenv`/`~/.zprofile`, a Homebrew tap (`/opt/homebrew/bin`), or absolute paths.
