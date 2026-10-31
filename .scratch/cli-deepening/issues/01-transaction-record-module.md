# 01: What is the interface of a deepened Transaction record module?

Type: grilling
Status: resolved
Blocked by:

Map: [map.md](../map.md). Review card 1, "Deepen the Transaction record module", in [architecture-review.html](../architecture-review.html#c1). Use design-it-twice (see map Notes).

## Question

What single module should own the **Transaction record**: its **Sides** join, its read and write shape, the per-type member rules, parsing, building, update-as-replace and delete? What is its interface, which modules disappear into it, and which tests move to that interface?

Evidence from the review (line numbers as of commit c84342165):

- Members are keyed three times: the schema in `query/TransactionShape`, the render in `TransactionsCommand.render` (~207–257), and the parse by string literals in `apply/TransactionInput` (~66–203), plus its `NOT_ALLOWED` map (~31–39).
- Per-type rules live as prose in TransactionShape member descriptions and separately as code: refusals in TransactionOperations (~102), TradeOperations (~27–29) and TransferOperations (~33–45), and the EnumSets in TransactionOperations (~47–54).
- Update renders the record, converts it with Gson, applies `TransactionShape.inputOf`, overlays the patch and re-runs add (TransactionUpdates ~116–138). It needs round-trip special cases (`units:[]` dropped, the exDate rule mirrored) and detects changes with `toString()`.
- `OperationContext` carries `Optional<TransactionUpdates.Existing> replacing` and a separately counted failure set. That design caused the bug fixed in c84342165.
- `TransactionRecord.allOf` rescans the client per lookup, per created report and per replace.
- `apply` imports `query` 15 times; `BuiltRecord` lives in `query` but only `apply` uses it.
- There are no direct tests of TransactionShape, TransactionInput, the side join (orphan or damaged), OversellGuard, BuiltRecord or `replace`/`move`. About 1,500 lines of tests go through CliFixture.

Sub-questions to settle: where the per-type rule table lives and whether help prose is generated from it; whether update keeps its "overlay, then add" design or gets a direct path; whether OversellGuard sits inside the module; what the batch-scoped index looks like; the package name.

## Answer

Resolved by grilling, using design-it-twice. Four parallel designs were compared: minimal interface, flexible (Ledger + Plan + plug-in checks), easy common caller (two-phase Plan), and rule table at the centre. The chosen design is a hybrid led by the rule-table design, taking the minimal design's result type.

**Module and package.** One deep module in `name.abuchen.portfolio.cli.transaction`. It absorbs:
- `query/TransactionRecord` (the side join and orphans)
- `query/BuiltRecord`
- `query/TransactionShape`
- the render half of `query/TransactionsCommand`
- `apply/TransactionInput`
- the cash branch, EnumSets, `commit` and `runCoreChecks` of `apply/TransactionOperations`
- `apply/TradeOperations` and `apply/TransferOperations`
- `apply/TransactionUpdates`
- `apply/OversellGuard`
- `EntityKind.TRANSACTION_RECORD`

`apply` keeps three short `OperationSpec` handlers; whether they move is left to ticket 02. `TransactionsCommand` stays in `query` and keeps its filters, sorting, paging and `--currency` conversion; the conversion belongs to ticket 05.

**Interface.**

```java
TransactionRecords.open(Client client, Predicate<Object> existedBeforeBatch)  // read side passes any -> true
  List<TransactionRecord> all();
  Optional<TransactionRecord> find(String sideUuid);
  Map<String,Object> render(TransactionRecord record);
  Outcome add(JsonObject input);
  Outcome update(JsonObject input, Claims claims);   // uuid + patch members
  Outcome delete(JsonObject input, Claims claims);
  static Schema output / add / patch declarations    // ADR 0006: help still generated from Schema
sealed interface Outcome = Applied(kind, recordId, before, after) | Unchanged(recordId) | Refused(List<FieldFailure>)
interface Claims { OptionalInt claim(String uuid); }   // batch::claim in production, a HashMap in tests
```

**Invariants and error modes.**

- `Refused` means the model wasn't touched. `Applied` means the change is already made.
- No caller needs two phases. `--dry-run` is unaffected, because it works at the `SafeWrite` file level and a batch already mutates the in-memory model operation by operation.
- Every rule outcome is a returned `FieldFailure`. Only programming errors throw. There is no context threaded through and no second failure counter, which removes the class of bug fixed in c84342165.
- Claims on every side are taken before validation, so `duplicate-target` alone stops the operation, as today.
- The side join runs once at the start of each call, with nothing cached between calls. The record reported after a write is derived from the sides just built, not from another scan. Nothing can go stale, even if a future operation changes transactions outside the module.
- `TransactionRecord` stays a public read value with its `touches…` predicates, and `ownRate` moves next to it.

**Per-type rules.**

- An internal table `(type, transfer kind) × member → REQUIRED | OPTIONAL | REFUSED`.
- Each refusal code is declared once on its member (it replaces `NOT_ALLOWED`, the `refuse(...)` lists and the EnumSets).
- One message template per refused cell: `"<TYPE> must not have <member>."`, with the transfer kind where it applies. Codes and fields are unchanged; messages aren't contract.
- Cross-field invariants stay as named code beside the table: cross-currency cash transfers (target amount and derived rate), the forex `GROSS_VALUE` requirement and unit validation, gross-amount reconciliation, the exDate rules, same-account transfers, and the five core builders (cash, delivery, buy/sell, cash transfer, security transfer).
- The order of failures inside `Refused` follows the table's member order. It is deterministic but not contract; tests assert on the set of `(field, code)` pairs.

**Help prose.** Stays hand-written, because help text is fixed contract. A drift test checks that every REQUIRED or REFUSED cell is named in the right clause of its member's description. Generating the prose from the table would be a later contract change, outside this map.

**Update path.** A direct path:

1. Build a typed Draft from the existing sides.
2. Lay the patch members on it (`null` clears an optional member).
3. Run the same table check, build, core checks and oversell check as add.
4. Copy the result onto the existing sides, and move owners and cross-entry pointers if they changed.

Type and kind changes are refused as Draft checks (`type-immutable`, `kind-immutable`). This removes render → Gson → `TransactionShape.inputOf`, the `units:[]` and mirrored exDate special cases, and the diffing by `toString`. The diff compares rendered members with typed equality and still drops `updatedAt`. The contract sentence "overlaid on the current record and then validated exactly like an add" still holds.

**OversellGuard.** Private to the module and tested only through `add/update/delete`: an oversell already in the file, deleting a buy, and an update that moves a buy between investment accounts.

**Shared framework change.** `apply/OperationInput`'s primitives and `apply/EntityKind` move to `cli.command` and report into a sink that collects `FieldFailure(field, code, message, check)`. `OperationContext` adapts that sink by adding the operation index, and loses `replacing()`, its second constructor and the separately counted failures. Master data operations use the same reader, and ticket 03 is still free to reshape `EntityKind`. This breaks the `apply` → `transaction` → `apply` cycle; `apply` no longer imports `query` for transactions.

**Tests (replace, don't layer).**

- New tests in `cli.tests` under `cli.transaction`, on an in-memory `Client` built with the junit `AccountBuilder`, `PortfolioBuilder` and `SecurityBuilder`. They cover:
  - the per-cell table matrix;
  - render → add → render round trips per type, and `update(render)` → `Unchanged`;
  - update and delete: moves between owners, `record-incomplete`, claims, and the c84342165 regression;
  - the side join (orphan and damaged cross entries);
  - oversell.
- Deleted: the cases these duplicate in `TransactionAddTest`, `TransactionAddTradesTest`, `TransactionRoundTripTest`, `TransactionUpdateDeleteTest`, and the render cases of `TransactionsCommandTest`.
- Kept end to end:
  - one or two CliFixture smoke runs per operation;
  - one batch-atomicity run (a later failing operation leaves nothing written);
  - the filter, paging and sort cases of `TransactionsCommandTest`;
  - `TransactionsConversionTest` (ticket 05);
  - CliRun's contract check on every run.

**Not changed.** No glossary terms (Draft and Outcome are implementation names). No ADR: none of the decisions is both hard to reverse and surprising.
