# 06: How do write operations declare their failure codes?

Type: grilling
Status: resolved
Blocked by: 01, 02

Map: [map.md](../map.md). Review card 5, "Declared failure codes for write operations", in [architecture-review.html](../architecture-review.html#c5). Contract change allowed (map Notes): the codes become listed in `apply --help <op>`.

Note from [ticket 01](01-transaction-record-module.md): the transaction refusal codes (`*-not-allowed`) now sit on the members of the Transaction record module's internal rule table, and all of its failures come back as `FieldFailure` values in `Outcome.Refused`. Declaring them per operation can read from there.

## Question

Where are the field-level failure codes of `apply` operations declared, so that `apply --help <op>` lists them, `context.fail` can only raise a declared code, and SkillDriftTest knows them? Is it per OperationSpec, one shared catalog, or the deepened modules from tickets 01 and 03, following ticket 02's "declared by whatever raises it" rule?

Evidence from the review:

- About 54 free-string codes across `apply` (`negative-holding`, `currency-mismatch`, `type-immutable`, `duplicate-target`, ...).
- SkillDriftTest takes known codes only from ErrorSpec and WarningSpec (~148–155). The skill can only say "explain each entry of failures" (SKILL.md ~61).
- Some codes are shared across operations (`required`, `invalid-type`, `reference-not-found`, `duplicate-target`) and come from OperationInput or OperationContext, not from an operation.

Sub-questions to settle: how shared codes are declared once; whether each code's description doubles as its help text; whether a test probes that every declared code is reachable.

## Answer

Resolved by grilling.

**One code enum.** The field-level codes join ticket 02's `ErrorCode` enum in `cli.command`. There is no separate `FailureCode` enum, because the two sets overlap: `reference-not-found` and `reference-ambiguous` come from the same Reference lookup (ticket 03), whether for an option or for an operation member. `FieldFailure(field, ErrorCode code, message, check)` takes the enum, so a free-string code no longer compiles.
- Where a code can appear follows from who declares it:
  - a facet's codes appear as the problem document's `code`;
  - an operation's or `BATCH`'s codes appear in `failures[].code`;
  - a code can be in both sets.
- `pp-cli --help` keeps its "a failure is a problem+json whose code is one of" list, filtered to the codes some facet declares. The top-level contract doesn't change.
- Each code has one generic description. It is the code's only help text; the per-failure `message` stays as it is and is not contract.

**An operation's code set** is derived, never hand-listed: `OperationSpec.failureCodes()` is the union of the codes below. It may be a superset of what the operation can raise, never a subset.
- **Implied by the input `Schema`** (the `OperationInput` primitives in `cli.command`):
  - a required member brings `required`;
  - a typed member brings `invalid-type`;
  - a positive number brings `not-positive`;
  - an amount brings `invalid-amount`;
  - a Reference brings `reference-not-found`;
  - every operation brings `unknown-field`;
  - an operation that claims brings `duplicate-target`.
- **Declared by the owning module** for that operation:
  - **`TransactionRecords`**, per add, update and delete: the rule table's refusal codes, the named cross-field invariants, `type-immutable`/`kind-immutable`, `record-incomplete`, `import-check-failed` and `negative-holding`.
  - **`MasterDataKind`**, per kind: the member writers' checks and the validator's codes.
  - **`taxonomy.assign`**: `not-a-leaf`, `duplicate-classification` and `weight-total-exceeded`.

**Batch-level codes.** `invalid-operation` and `unknown-operation` are declared by apply's `BATCH` facet, because no operation exists yet when they are raised. `apply --help` lists them.

**Help (allowed contract addition).**
- `apply --help <op>` gains a "Failure codes:" section, one `code  description` line per code in the operation's set.
- `apply --help` lists the two batch codes and adds one line pointing to `apply --help <op>`.
- The global `pp-cli --help` lists no field-only codes.

**`check` member.** It stays an undeclared free string, because the core check names belong to core (ADR 0002). Only `import-check-failed` is declared.

**Tests.**
- **Compile time:** the enum closes the code set.
- **CliRun:** on every `validation-failed` run, it asserts that each `failures[].code` is in the declared set of the operation at that `operationIndex`, or in the `BATCH` set.
- **Static check:** ticket 02's check widens from "every `BY_USE` code is declared by some facet" to "by some facet, operation or `BATCH`", so no code is dead.
- **SkillDriftTest:** it needs no change, because `ErrorCode.values()` already covers the field codes.
- **No reachability probe** per (operation, code). Tickets 01 and 03's interface tests (the rule-table matrix and the per-kind failures) already exercise the codes.

**Glossary.** The term **Failure** is added to the AI/CLI vocabulary, separating a failure code from the problem document's error code. No ADR: a single code enum is not surprising after ticket 02.
