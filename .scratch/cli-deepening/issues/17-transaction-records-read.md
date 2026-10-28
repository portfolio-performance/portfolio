# 17: TransactionRecords read side

Type: task
Status: ready-for-agent
Blocked by: 13, 16

Spec: [spec.md](../spec.md), "Transaction records". Decision: [ticket 01](01-transaction-record-module.md). Ticket 13 is a blocker only to avoid conflicting rewrites of `TransactionsCommand`.

## What to build

Create the `name.abuchen.portfolio.cli.transaction` package with the read half of the module.

- **Interface**: `TransactionRecords.open(Client, Predicate<Object> existedBeforeBatch)` with `all()`, `find(sideUuid)`, `render(record)` and the static output `Schema`. The read side passes `any -> true`.
- **Absorbed**:
  - `query/TransactionRecord`, the side join and orphans. It stays a public read value with its `touches…` predicates, and `ownRate` moves next to it.
  - `query/TransactionShape`'s output schema.
  - The render half of `query/TransactionsCommand` (~207–257).
  - `query/BuiltRecord`.
- **Behaviour**: the side join runs once per `open`/call, with nothing cached between calls.
- **`TransactionsCommand`** stays in `query`. It keeps its filters, sorting, paging and conversion, and calls `all()` and `render()`.
- **Write side, transitional**: the existing `apply` transaction operations call `TransactionRecords` instead of `TransactionRecord.allOf`, so per-lookup rescans go. The rest of the write side moves in tickets 18 and 19.

## Acceptance criteria

- [ ] `query` no longer holds `TransactionRecord`, `BuiltRecord` or the render code; `apply` imports nothing from `query` for transactions
- [ ] New `cli.transaction` tests on an in-memory `Client` (junit `AccountBuilder`, `PortfolioBuilder`, `SecurityBuilder`) cover render per type, an orphan side, a damaged cross entry, and `find` by either side's UUID
- [ ] Golden help unchanged
- [ ] CLI suite green

## Tests deleted

- The render cases of `TransactionsCommandTest`: field-by-field member checks per type. Keep its filter, paging and sort cases.
