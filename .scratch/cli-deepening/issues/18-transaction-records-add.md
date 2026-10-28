# 18: TransactionRecords add and the rule table

Type: task
Status: ready-for-agent
Blocked by: 17

Spec: [spec.md](../spec.md), "Transaction records". Decision: [ticket 01](01-transaction-record-module.md), "Per-type rules", "Help prose", "OversellGuard".

## What to build

`TransactionRecords.add(JsonObject) → Outcome`, driven by the internal rule table.

- **Rule table**: `(type, transfer kind) × member → REQUIRED | OPTIONAL | REFUSED`. Each refusal code is declared once, on its member. The table replaces:
  - `TransactionInput.NOT_ALLOWED`;
  - the `refuse(...)` lists in `TransactionOperations`, `TradeOperations` and `TransferOperations`;
  - the EnumSets in `TransactionOperations` (~47–54).

  One message template applies per refused cell: `"<TYPE> must not have <member>."`, with the transfer kind where it applies.
- **Cross-field invariants** stay as named code beside the table:
  - cross-currency cash transfers (target amount, derived rate);
  - the forex `GROSS_VALUE` requirement and unit validation;
  - gross-amount reconciliation;
  - the exDate rules;
  - same-account transfers;
  - the five core builders.

  The ADR 0007 comment on the transfer rate moves with its code.
- **Absorbed**: `apply/TransactionInput`, the add path of `TransactionOperations` (cash branch, `commit`, `runCoreChecks`), `TradeOperations`, `TransferOperations` and `OversellGuard` (now private).
- **Static schema**: the add `Schema` is published by the module.
- **Handler**: `transaction.add`'s `OperationSpec` handler moves to `cli.transaction` and is a thin adapter over `add`.
- **Help-prose drift test**: every REQUIRED or REFUSED cell is named in the right clause of its member's hand-written description.

## Acceptance criteria

- [ ] No `*-not-allowed` code string appears outside the rule table
- [ ] `Refused` leaves the model untouched (asserted in the matrix test)
- [ ] The per-cell matrix test covers every `(type, kind, member)` cell, asserting sets of `(field, code)` pairs
- [ ] The render → add → render round trip holds per type
- [ ] Oversell tests through `add`, including an oversell already in the file
- [ ] The help-prose drift test passes, and fails when a REFUSED cell is removed from a description (checked locally)
- [ ] Golden help unchanged: codes and fields are unchanged, and messages aren't contract
- [ ] CLI suite green

## Tests deleted

- The cases in `TransactionAddTest` and `TransactionAddTradesTest` that the matrix, round-trip and oversell tests duplicate. Keep one or two CliFixture smoke runs per add flavour (cash, trade, transfer) and one batch-atomicity run (a later failing operation leaves nothing written).
- The add round-trip cases of `TransactionRoundTripTest`
