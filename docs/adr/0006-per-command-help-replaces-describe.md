# Per-command `--help`, generated from our own specs, replaces `describe`

`pp-cli describe` dumped every command, option, response schema, operation, error code and limit as one JSON document of about 20 KB. Agent harnesses truncated it, so agents never saw the response shapes and guessed wrong keys. They even misread `share`, which `describe` documented correctly. We remove `describe` and replace it with standard, plain-text help. `pp-cli --help` lists the commands, the global options, the error codes and the limits. `pp-cli <command> --help` gives one command's usage, options, response fields and the warning and error codes it can return. `pp-cli apply --help <operation>` gives one write operation's input schema. The help text is generated from the existing `CommandSpec`, `OptionSpec` and `Schema` declarations, which stay the single declaration of each command. We don't adopt a third-party parser.

**Considered Options**

- *Keep `describe` and add a `--command` filter*: rejected. The default output still overflows, and agents start from the default.
- *Keep `describe` as a machine-readable dump next to `--help`*: rejected. It has no consumer besides our own agents, and two discovery surfaces would drift apart.
- *A cheat sheet of response shapes in the agent skill*: rejected. It caches what the tool can answer, and it goes stale.
- *picocli*: rejected for now. Its generated help can't show response fields or error codes without a custom renderer. Its command model would duplicate our specs, which already drive parsing, validation and table columns. It would also add a target-platform entry, an OSGi import and packaging work. We'd revisit it if shell completion or "did you mean" suggestions become worth that cost.

**Consequences**

- This removes a command, so it breaks the contract. Agents and the skill must switch to `--help`.
- Tests that validated responses against the schema `describe` published now validate against the in-code `Schema` objects. The guarantee that real responses conform to the documented schema stays.
- Issue 13 of `.scratch/ai-cli/` is superseded where it assumes `describe`.
- The error code `command-required` is removed. A bare `pp-cli` now prints the global help and exits 0 instead of failing with a usage error.
