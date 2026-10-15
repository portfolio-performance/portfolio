# What are the options for packaging a headless Java 21 CLI built from this Tycho repo?

Type: research

## Question

How can the CLI module from ticket 03 (core bundle jar plus target-platform jars on a plain classpath) be built and shipped from this Maven/Tycho build: fat/shaded jar with a launcher script, jlink runtime image, jpackage app image, or a launcher placed inside an existing PP install that reuses its bundled JRE and plugin jars? For each, what does it cost to build here (Tycho plugin support, resolving target-platform jars outside OSGi), what does the user need installed, artifact size, startup time per command (one JVM per call), and macOS signing or quarantine issues? Also: how would an agent's shell find the `pp-cli` command (PATH, Homebrew, symlink)?
