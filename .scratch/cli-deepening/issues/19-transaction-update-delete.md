# 19: TransactionRecords update and delete

Type: task
Status: ready-for-agent
Blocked by: 18

Spec: [spec.md](../spec.md), "Transaction records". Decision: [ticket 01](01-transaction-record-module.md), "Update path".

## What to build

`TransactionRecords.update(JsonObject, Claims)` and `delete(JsonObject, Claims)`, both returning `Outcome`.

- **Update takes a direct path**:
  1. Build a typed Draft from the existing sides.
  2. Lay the patch members on it (`null` clears an optional member).
  3. Run the same table check, build, core checks and oversell check as add.
  4. Copy the result onto the existing sides, and move owners and cross-entry pointers if they changed.

  `type-immutable` and `kind-immutable` are Draft checks. The diff compares rendered members with typed equality and drops `updatedAt`.
- **Claims** on every side are taken before validation, so `duplicate-target` alone stops the operation.
- **Removed**:
  - `apply/TransactionUpdates`, with its render → Gson → `TransactionShape.inputOf` path, the `units:[]` and mirrored exDate special cases, and `toString` diffing;
  - `OperationContext.replacing()`, its second constructor and the separately counted failures;
  - `EntityKind.TRANSACTION_RECORD` from `cli.command`. It now belongs to `cli.transaction`.
- **Handlers**: the `transaction.update` and `transaction.delete` `OperationSpec` handlers move to `cli.transaction` and publish the patch schema.

## Acceptance criteria

- [ ] `update(render(r))` returns `Unchanged`
- [ ] Moves between owners, `record-incomplete`, claims and the c84342165 regression (a refused type change stops the update) are tested at the interface
- [ ] Oversell through `update` (a buy moved between investment accounts) and `delete` (deleting a buy)
- [ ] `OperationContext` has one constructor and no `replacing()`
- [ ] The contract sentence "overlaid on the current record and then validated exactly like an add" still holds; golden help unchanged
- [ ] CLI suite green

## Tests deleted

- The cases in `TransactionUpdateDeleteTest` that the interface tests duplicate. Keep one CliFixture smoke run each for update and delete.
- The remaining update round-trip cases of `TransactionRoundTripTest`. Delete the class if nothing is left.
