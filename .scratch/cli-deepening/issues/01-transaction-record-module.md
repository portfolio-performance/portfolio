# 01: What is the interface of a deepened Transaction record module?

Type: grilling
Status: open
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
