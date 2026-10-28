# 16: Shared write framework in `cli.command`

Type: task
Status: ready-for-agent
Blocked by: 11

Spec: [spec.md](../spec.md), "Transaction records" and "Master data kinds". Decisions: [ticket 01](01-transaction-record-module.md), "Shared framework change"; [ticket 03](03-master-data-kind.md), "Dependency direction" and "`Change.entity`". Ticket 11 is a blocker only to avoid conflicting rewrites of `ApplyCommand`.

## What to build

Move the parts both write modules build on, so they can depend on `cli.command` alone.

- **Moved to `cli.command`**:
  - `apply/OperationInput`'s primitives and `apply/EntityKind`. They report into a sink that collects `FieldFailure(field, code, message, check)`; `code` stays a string until [ticket 22](22-declared-failure-codes.md). `OperationContext` adapts the sink by adding the operation index.
  - `OperationSpec` and `ApplyBatch.OPERATION_MEMBER`.
  - New: `sealed interface Outcome = Applied(kind, recordId, before, after) | Unchanged(recordId) | Refused(List<FieldFailure>)` and `interface Claims { OptionalInt claim(String uuid); }`. `OperationContext` turns an `Outcome` into its changes and failures.
- **Registry**: the operation registry moves from `Commands.OPERATIONS` into `apply`, and root `Commands` and `HelpText` read it from there. The `apply → cli → apply` cycle is gone.
- **`Change.entity`** becomes an enum with schema `enumOf(...)`: `instrument`, `cash-account`, `investment-account`, `transaction`, `taxonomy-assignment`. `TaxonomyOperations` emits the enum value.
- **Contract ripple**:
  - The `apply --help` results schema lists the five values; update the golden file.
  - `skills/pp-cli/write-protocol.md` step 4 names taxonomy assignments among the entities in `results`.
- `OperationContext.replacing()` stays until [ticket 19](19-transaction-update-delete.md).

## Acceptance criteria

- [ ] `OperationInput`, `EntityKind`, `OperationSpec`, `Outcome`, `Claims` and `FieldFailure` live in `cli.command`; `cli.command` imports nothing from `apply` or `query`
- [ ] `Commands.OPERATIONS` is gone; `apply` imports nothing from the root package
- [ ] `Change.entity` is an enum, and the schema's value set equals the emitted set (a `taxonomy.assign` dry run conforms)
- [ ] Golden help and the skill updated for the five entity values, and SkillDriftTest green
- [ ] CLI suite green

## Tests deleted

- `OperationInputTest` moves to `cli.command` and asserts through the sink. Delete its cases that only checked `OperationContext` bookkeeping.
