# 02: Quiet core logging by default

Type: task
Status: done

Spec: [spec.md](../spec.md), "Solution".

## What to build

Core prints status lines such as `Status INFO: name.abuchen.portfolio.model.ClientFactory code=0 Loaded … with [XML]` to the console. [PpCli.java](../../../name.abuchen.portfolio.cli/src/name/abuchen/portfolio/cli/PpCli.java) already sends console output to stderr. Harnesses that merge stderr into stdout still break JSON parsing.

- By default, drop core's INFO and OK status output.
- Core WARNING/ERROR output and the CLI's own internal-error stack traces still go to stderr.
- A new global `--verbose` switch restores today's behaviour. It is declared in the specs, so it shows up in help.
- Silence logging inside the CLI module only, without patching core ([ADR 0002](../../../docs/adr/0002-headless-java-cli-module-in-fork.md)).

## Acceptance criteria

- [x] `pp-cli info --file <sample>` writes nothing to stderr
- [x] With `--verbose`, the load status line appears on stderr again
- [x] An internal error still writes its stack trace to stderr
- [x] stdout holds only the JSON document in every case

## Comments
