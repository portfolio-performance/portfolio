# 03: Should one module per Master data kind replace the four registries?

Type: grilling
Status: open
Blocked by:

Map: [map.md](../map.md). Review card 4, "One Master data kind", in [architecture-review.html](../architecture-review.html#c4).

## Question

Should each **Master data** kind (Instrument, Investment account, Cash account) be one module that owns its UUID, name, **Reference** lookup keys, read render, write fields and **Retired** flag, with today's registries derived from it? If so, what is its interface, and does **Taxonomy** fit in it too?

Evidence from the review:

- One kind is described in `References.Kind` (command), `MasterDataList.Source` (command), `EntityKind` (apply; UUID only, **Existing entity**), `EntityOperations.Definition`/`Field` (apply) and the read renders (`InstrumentsCommand.render` ~65–79, `CashAccountsCommand.render`).
- "Write fields are named as in the output" is a convention only (InstrumentOperations ~7–27, AccountOperations ~23–56).
- `Change.entity` is a free string and has drifted: the schema at ApplyCommand.java:38 lists four kinds, but TaxonomyOperations.java:83 emits `taxonomy-assignment`.
- There are dependency cycles: `command → query` (References imports `InstrumentsCommand.SOURCE`), and `apply → cli → apply` (ApplyCommand reads `Commands.OPERATIONS`).
- `EntityOperations.find` builds an ad-hoc EntityKind on each call (~272–273).

Sub-questions to settle: whether `EntityKind.TRANSACTION_RECORD` belongs here or to ticket 01's module; the `Change.entity` value set, an allowed contract change; whether an in-memory test that "every write field is a read item member" replaces the convention.
