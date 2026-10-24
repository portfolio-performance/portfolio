# 01: Per-command `--help` replaces `describe`

Type: task
Status: ready-for-agent

Spec: [spec.md](../spec.md), "Help". [ADR 0006](../../../docs/adr/0006-per-command-help-replaces-describe.md).

## What to build

Plain-text help generated from the existing `CommandSpec`, `OptionSpec` and `Schema` declarations, with no third-party parser:

- `pp-cli --help` and bare `pp-cli`: usage, the commands with one-line summaries, the global options (`--file`, `--format`, `--verbose` once issue 02 lands, …), the error codes catalogue and the `limits`.
- `pp-cli <command> --help`: usage line, options (value placeholder, type, allowed values, default, required), response `data` fields with their descriptions (nested fields indented), and the warning and error codes this command can return.
- `pp-cli apply --help`: the operations with one-line summaries. `pp-cli apply --help <operation>`: that operation's input schema.
- `--help` wins over every other argument. It needs no `--file` and never loads a client file.
- An unknown operation after `apply --help` gives the usual usage error.

Remove `DescribeCommand` and its registration. Keep the in-code schemas, since they now feed help and tests.

## Tests

- Rework `DescribeConsistencyTest` so the guarantee stays: every declared command and operation is reachable through help, and every real response conforms to its in-code `Schema` (validate directly, not through a `describe` round trip).
- Rework `SkillDriftTest` so names in the skill are checked against the declared specs instead of `describe` output.
- Replace `DescribeCommandTest` with help tests: each command's help mentions all of its options and top-level `data` fields; global help lists every command and error code.
- Size guard: no single help output (global, per command or per operation) exceeds about 6 KB.

## Acceptance criteria

- [ ] `pp-cli --help`, bare `pp-cli`, `pp-cli <command> --help` and `pp-cli apply --help [<operation>]` work as described, with exit code 0
- [ ] `describe` is gone and gives `unknown-command`
- [ ] Schema conformance of real responses is still tested for every command and operation
- [ ] The skill drift test checks against the declared specs
- [ ] The help size guard passes
- [ ] Packaging README and any docs that mention `describe` are updated

## Comments
