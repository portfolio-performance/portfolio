# 15: Finish facets: derived scope, immutable Response, derived samples

Type: task
Status: resolved
Blocked by: 11, 13, 14

Spec: [spec.md](../spec.md), "Command facets" and "Testing Decisions". Decision: [ticket 02](02-command-facets.md).

## What to build

Every command is on `CommandSpec.of`. Remove the transitional parts and close the guards.

- **Construction**: `CommandSpec`'s old constructors and `usesClientFile` go, and `CommandSpec.of` is the only construction path.
- **Error-code scope**: `ErrorCode` scope becomes `GLOBAL | EVERY_COMMAND | BY_USE`. A command's codes are the union of three sets:
  - `EVERY_COMMAND`;
  - the codes implied by option types (a Reference brings `reference-not-found`/`reference-ambiguous`; a required option brings `option-required`);
  - the codes its facets declare.

  The `CLIENT_FILE`, `REFERENCE_OPTION`, `REQUIRED_OPTION` and `COMMANDS` scopes go, along with the command-name lists and `alsoCommands`. Help filters `ErrorCode.values()` by the derived set.
- **`Response`**: immutable (`List.copyOf`, unmodifiable meta) and built only by the framework. `Response.warn()` goes.
- **`CliRun`**: on every failing run, it asserts that the problem document's code is in that command's `errorCodes()`.
- **Static test**: every `BY_USE` code is declared by some facet of some command in `Commands.ALL`. [Ticket 22](22-declared-failure-codes.md) widens it to operations and `BATCH`.
- **HelpConsistencyTest**: the regex scan goes, and `sampleArguments` is derived from the facets' samples (a reference draws the first entity of its kind from the fixture). Two overrides stay, `apply --input` and `restore --backup`. The switch and its "add sample arguments" failure go.

## Acceptance criteria

- [ ] No command name appears in `ErrorCode`
- [ ] Each command's help lists the same codes as before, or a superset, with every difference called out in the commit and the golden file updated
- [ ] Mutating a `Response`'s warnings or meta throws
- [ ] Adding a command means only adding it to `Commands.ALL` (no ErrorCode or sample-argument edits): shown by the derived test passing for all current commands
- [ ] CLI suite green

## Tests deleted

- `HelpConsistencyTest.theErrorCodesInHelpAreExactlyTheOnesTheToolFailsWith` (the regex scan), replaced by the CliRun check and the static test
- The hand-written `sampleArguments` switch
- Per-command help-scope cases in `HelpTest.commandHelpListsOnlyTheErrorCodesTheCommandCanReturn` that only restate command-name lists. Keep one case per scope rule.

## Answer

Implemented on integration branch `feat/cli-deepening`, merge commit 2594c550f. CommandSpec.of only; ErrorCode scope GLOBAL/EVERY_COMMAND/BY_USE; immutable Response; CliRun code-in-scope check; facet samples.
