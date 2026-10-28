# 08: Golden help test and ADR 0007 comments

Type: task
Status: resolved
Blocked by:

Spec: [spec.md](../spec.md), "Migration order" and "Ruled out: an amounts & rates module". Decisions: [ticket 02](02-command-facets.md) (help contract guard), [ticket 04](04-amounts-and-rates.md).

## What to build

A safety net that goes in before any refactor:

- **Golden `--help` test**: a test in `cli.tests` that renders `pp-cli --help`, `pp-cli <command> --help` for every command in `Commands.ALL`, `pp-cli apply --help`, and `pp-cli apply --help <op>` for every operation. It compares each one byte for byte with a checked-in golden file under the test bundle. On a mismatch it prints a unified diff. It reads expected output only, and has no "regenerate" switch.
- **ADR 0007 comments**: one line each.
  - At the scale-10 HALF_UP transfer rate in `apply/TransferOperations`: it mirrors the desktop `AccountTransferModel.setAmount`.
  - At the double `share` division in `query/AllocationCommand`: it mirrors core's `getShare()`, which `AllocationCommandTest` asserts it equals.
  - Each comment links ADR 0007 by file name.

No production behaviour changes.

## Acceptance criteria

- [ ] The golden files hold today's help output for the global help, every command, `apply` and every operation
- [ ] The golden test fails with a readable diff when any help line changes (checked by changing one description locally, then reverting)
- [ ] Both rounding sites carry a one-line comment naming the core or UI site they mirror and ADR 0007
- [ ] CLI suite green

## Tests deleted

None. This ticket only adds a guard.

## Answer

Implemented on integration branch `feat/cli-deepening`, merge commit c07c6e228. Golden help test (GoldenHelpTest, golden/help/) and the two ADR 0007 comments.
