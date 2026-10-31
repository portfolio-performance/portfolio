# Which domain write operations does v1 support?

Type: grilling
Status: resolved

## Question

Which write operations are in v1 (e.g. add/edit/delete account and portfolio transactions, add security, update prices, assign taxonomy classifications), what inputs each takes, and what validation rules apply (cross-entry consistency of buy/sell pairs, currency, dates)?

## Answer

There is one write command: `pp-cli apply --input <file|->`, JSON only. It takes `{"operations": [...]}`; a single change is a batch of one. There are no per-resource write commands. Every `apply` goes through the safe-write sequence of ticket 06, and `--dry-run` applies to it unchanged. `describe` publishes the op schemas.

### Operations and fields

- `transaction.add | update | delete`: `type`, `date`, `amount`, `currency`, `shares`, `instrument`, accounts (cash account, investment account, or from/to for transfers), `units` (FEE, TAX, GROSS_VALUE), `note`, `source`, `exDate` (dividends only, requires an instrument, not after `date`), `targetAmount` (cross-currency cash transfers). `type` cannot change on update.
- `instrument.add | update`: `name`, `currencyCode`, `isin`, `tickerSymbol`, `wkn`, `note`, `retired`.
- `cash-account.add | update`: `name`, `currencyCode`, `note`, `retired`.
- `investment-account.add | update`: `name`, `referenceCashAccount`, `note`, `retired`.
- Field names match the read output (ticket 15), so an agent can send back what it read. (Renamed from `currency`, `ticker` and `referenceAccount` in the ticket 15 design session.)
- `taxonomy.assign`: replaces the complete set of assignments of one instrument in one existing taxonomy.

Out of v1: any write to prices (historical or latest; only the app edits them), deleting instruments or accounts (the core removal cascades into transactions), creating taxonomies or classifications, watchlists, investment plans, attributes, events, online feed settings, assigning cash accounts to taxonomies.

### Batch rules

- Atomic: all operations apply or none do.
- Operations are independent. There are no caller-supplied IDs and no references to the result of an earlier operation. Every reference in a write is a UUID that exists in the file before the batch; ISIN, ticker and name lookups from ticket 04 are read-only conveniences.
- Operations run in order against the evolving model, so duplicate checks see earlier adds. Two operations on the same existing entity in one batch are refused.

### Transaction records

- A buy, sell or transfer is created, updated and deleted as a whole record (see `GLOSSARY.md`). Delete removes both sides; there is no single-side delete.
- Record `id` on read: the investment-account side for buy and sell, the outgoing side for transfers, the transaction itself otherwise. Writes also accept the other side's UUID and resolve it to the same record. Responses list `id` plus every side's UUID.
- `update` is read, merge, validate, write: the patch is overlaid on the full record, and the result must pass the same validation as `add`. Nothing is recomputed silently. A patch that leaves fees, taxes or forex units inconsistent with a changed `shares` or `amount` is refused with a field-level error.
- `amount` is the net value and is authoritative; `units` use the shape that `transactions` returns on read, so a record can be read, edited and written back. An optional `grossAmount` is a cross-check only and is refused if it does not reconcile with `amount` plus or minus the units.

### Validation

- The CLI calls core's non-mutating `ImportAction` checks directly (`CheckTransactionDateAction`, `CheckSecurityRelatedValuesAction`, `CheckCurrenciesAction`, `CheckForexGrossValueAction`, `CheckValidTypesAction`) and builds records through the core entry classes. The CLI adds only what core lacks: `amount` > 0, `shares` > 0, exchange rate > 0, and structured, field-named errors.
- Instrument per type: required for `BUY`, `SELL`, `DELIVERY_*`, `DIVIDENDS` and portfolio transfers; refused for `DEPOSIT`, `REMOVAL`, `INTEREST`, `INTEREST_CHARGE` and cash transfers; optional for fees and taxes. `shares` is accepted only for share-carrying types.
- Currency: the cash side uses the cash account's currency. An instrument in another currency needs a forex `GROSS_VALUE` unit with a rate. Portfolio transfers have no currency check.
- Cross-currency cash transfers are supported: `amount` in the source currency, `targetAmount` in the target currency. The rate is derived and echoed; a supplied `exchangeRate` is checked with core's rounding tolerance. Forex `BUY` and `SELL` follow the same pattern.
- Overselling: refuse any write that makes an investment account's holding of an instrument negative at the end of any date. This covers edits and deletes that remove an earlier buy. Core's `SharesHeldConsistencyCheck` only catches a negative total.
- Instruments: ISIN format check (core `Isin`) and refusal of a duplicate ISIN. Instrument currency is immutable once the instrument has transactions.
- Accounts: name and currency are required. An investment account needs an existing reference cash account (never null), which may be changed freely. A cash account's currency is immutable once it has transactions.
- Taxonomy: each weight is greater than 0, classifications are not repeated, the total is at most 100% (weights are fractions; the remainder is reported as unassigned), only leaf classifications are assignable, and an empty list clears the instrument from the taxonomy.

### Results and errors

- Success: the ticket 04 envelope with `data.results[]`, one entry per operation by index, each holding `created`, `updated` and `deleted` change records (entity type, UUID(s), before/after for updated fields). `meta` carries the backup path and file versions (ticket 06).
- Failure: nothing is written, the exit code is non-zero, and the structured error lists every validation failure in the batch. Each carries `operationIndex`, a `field` path, a machine-readable `code`, a message and, where applicable, the core check that raised it.

### How it was resolved

Grilled 2026-10-10 and 2026-10-15. The user accepted each recommendation except where noted.

| Decision | Reasoning |
| --- | --- |
| No price writes (user's answer) | Prices are maintained by the app and its feeds; the CLI stays offline and never edits history. |
| Patch edits, on the user's condition that integrity is not weakened | Read-merge-validate-write means a patch can never bypass the `add` rules. |
| JSON input only (user's answer) | One consistent, robust input shape that matches the read output. |
| Atomic batch, but no dependencies between operations (user's answer) | Dropped caller-supplied IDs and in-batch references; a new entity's UUID comes back in the response for the next run. |
| Cross-currency cash transfers included (user's answer) | The app already models them as a forex unit on the source side plus a target amount. |
| Reuse core validators (user's answer) | Core has no single per-transaction validator but has five reusable import checks; the CLI adds only the three missing positivity rules. |
| Reference account freely changeable | The app only blocks changing an account's currency; the earlier stricter rule would block legitimate edits. |
| Taxonomy total at most 100%, not exactly 100% | The app and importer accept partial assignment, and ticket 04's `allocation` already reports an "unassigned" row. |
| Leaf-only taxonomy assignment | Core and the app allow non-leaf assignments, but an agent writing one is almost certainly a mistake. |
| Refuse overselling by end-of-date holdings | `Checker` only catches a negative total, so a sell dated before its buy passes it. |
| One `apply` command, no per-resource writes | A single path through one validator and one safe-write sequence. |

Rejected options:

- **Deleting instruments and accounts:** core cascades into transactions, so a delete would silently remove records.
- **Per-resource write commands as wrappers over `apply`:** redundant once input is JSON only.
- **Caller-supplied IDs and in-batch references:** not needed; they add states to validate.
- **Exactly-100% taxonomy totals:** refuses states the app itself produces.
- **Flag-based input for scalar fields:** one input shape is more robust.
