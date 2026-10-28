# 09: `ErrorCode` enum

Type: task
Status: ready-for-agent
Blocked by: 08

Spec: [spec.md](../spec.md), "Command facets". Decision: [ticket 02](02-command-facets.md), "Error codes".

## What to build

Replace the `ErrorSpec` record and its `ALL` list with an `ErrorCode` enum in `cli.command`. It keeps the same codes, published code strings and global descriptions.

- `CliException.usage(...)` and `failure(...)` take an `ErrorCode` instead of a string. Every call site changes to match.
- **Transitional**: the enum keeps today's scope rules (`GLOBAL`, `EVERY_COMMAND`, `CLIENT_FILE`, `REFERENCE_OPTION`, `REQUIRED_OPTION`, and the named-command lists) as fields, so help output does not change. [Ticket 15](15-finish-facets.md) replaces them with `GLOBAL | EVERY_COMMAND | BY_USE`, derived from facets.
- `HelpText`, `ErrorSpecTest`, `SkillDriftTest` and `HelpConsistencyTest` read `ErrorCode.values()`.
- HelpConsistencyTest's regex scan stays until ticket 15. It is adapted to find `ErrorCode.<CONSTANT>` arguments to `usage`/`failure`.
- Write-operation field codes stay strings for now ([ticket 22](22-declared-failure-codes.md)).

## Acceptance criteria

- [ ] `ErrorSpec` is gone; every problem-document code comes from an `ErrorCode` constant
- [ ] A free-string code passed to `CliException` no longer compiles
- [ ] Golden help test unchanged
- [ ] CLI suite green

## Tests deleted

None. `ErrorSpecTest` is renamed to `ErrorCodeTest` and kept.
