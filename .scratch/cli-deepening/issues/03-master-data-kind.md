# 03: Should one module per Master data kind replace the four registries?

Type: grilling
Status: resolved
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

## Answer

Resolved by grilling. Yes: one module per **Master data** kind, with the registries derived from it. Taxonomy stays out.

**Module and package.** A new package `name.abuchen.portfolio.cli.masterdata` holds a generic `MasterDataKind<T>` and one declaration file per kind (Instrument, Investment account, Cash account). Each publishes `INSTRUMENTS`, `CASH_ACCOUNTS` and `INVESTMENT_ACCOUNTS`: the kind, its list `CommandSpec` and its operations. A declaration holds:
- the entity id (`cash-account`; it is also the operation-name prefix, and its plural is the list command name);
- the noun (`cash account`);
- the entities, UUID and name;
- an ordered **member** list;
- the **Reference** lookup keys and the ambiguity-candidate details;
- create, add and initialize;
- a named validator;
- the list command's summary and table columns.

These are derived from the declaration:
- the reference lookup;
- the paged list command;
- the existing-entity `EntityKind`;
- the item `Schema` and `render`;
- the add and update `OperationSpec`s.

These are removed:
- `References.Kind`;
- `MasterDataList.Source`;
- `EntityOperations.Definition` and `Field`;
- the master data `EntityKind` constants;
- the ad-hoc `EntityKind.of(...)` in `EntityOperations.find`;
- `query/InstrumentsCommand`, `CashAccountsCommand` and `InvestmentAccountsCommand`.

`AttributeCodec` moves to `cli.masterdata`. The resolver logic (UUID first, then pooled lookup keys, never guess) and the list builder (paging and sorting) live in `cli.masterdata` as the implementation behind the kind.

**Members are the read item.**
- A member has a name and a read schema (the read description). A writable member also has a writer: the write description, required, optional or flag, the setter, the "needs" phrase, and its single-member checks.
- The item `Schema`, `render`, and the add and update schemas are all derived from the member list, so every write field is a read member by construction. No convention or extra test is needed.
- Render modes: *always* (`uuid`, `name` via orEmpty, `retired`, a cash account's `currencyCode`) and *if set* (blank counts as unset).
- Read and write descriptions stay separate strings, because help text is fixed contract. Ticket 02's golden `--help` test guards it.
- `attributes` and `updatedAt` are read-only members. `attributes` uses a getter prepared once per client, the only use of that hook. `updatedAt` also feeds the `updatedAt` sort.
- Lookup keys (`isin`, `tickerSymbol`, `name`; `name` for the accounts) and candidate details (`isin`, `tickerSymbol`, `currencyCode`; `currencyCode`; none) are lists of member names. They read through the members' getters, so `matchedBy` keeps printing member names. A name that is not declared fails at class load.

**Checks.**
- Single-member checks sit on the member's writer: `invalid-currency`, `invalid-isin`, and the type checks.
- Checks that look beyond the member stay as the kind's named validator code: `duplicate-isin`, `currency-immutable`, and the rule that the reference cash account must be an **Existing entity**.

**Write interface.** This is ticket 01's shape:
- `kind.add(JsonObject)` and `kind.update(JsonObject, Claims)` return `Applied | Unchanged | Refused(List<FieldFailure>)`.
- The `OperationSpec` handlers are thin adapters.
- Master data code no longer calls `OperationContext.fail`, `created` or `updated`.
- Reading the `uuid` member, finding the existing entity and claiming it stays private to the master data update. There is no `OperationContext.claimExisting` helper, because transactions claim per side through `Claims` and `taxonomy.assign` claims a composite key.

**Taxonomy.** Taxonomy is not a Master data kind: it has no list command, no write fields and no retired flag, and `taxonomy.assign` changes an **Assignment**. It shares only a narrow **Reference** interface in `cli.command` (noun, entities, UUID, name, lookup keys, optional retired, candidate details), which the reference facet uses. Taxonomy's reference declaration sits next to `TaxonomiesCommand`, `MasterDataKind` exposes `reference()`, and `EntityKind.of(reference)` gives the existing-entity check for all four kinds. `EntityKind.TRANSACTION_RECORD` belongs to `cli.transaction`, as ticket 01 decided.

**`Change.entity`.** It becomes a Java enum, and its schema is `enumOf` its values: `instrument`, `cash-account`, `investment-account`, `transaction`, `taxonomy-assignment`. `taxonomy-assignment` is kept as emitted and is now documented. An operation can only report a `Change` with an enum value, so drift no longer compiles. This is an allowed contract change; its help and skill ripple stays in the map's fog.

**Dependency direction.** `OperationSpec` and `ApplyBatch.OPERATION_MEMBER` move to `cli.command`, joining `OperationInput` and `EntityKind` (ticket 01). The operation registry moves from `Commands.OPERATIONS` into `apply`, and root `Commands` and `HelpText` read it from there. Dependencies then run root → `apply` → {`masterdata`, `transaction`} → `command`. This breaks `command → query` (References imported `InstrumentsCommand.SOURCE`) and `apply → cli → apply`.

**Tests (replace, don't layer).**
- New tests in `cli.tests` under `cli.masterdata`, on an in-memory `Client` built with the junit builders. Per kind they cover:
  - render → add → render round trips, and `update(render)` → `Unchanged`;
  - required, type, currency, ISIN and reference-cash-account failures;
  - claims;
  - the reference lookup, taking over `ReferencesTest`.
- Deleted: the cases these duplicate in `AccountOperationsTest`, the instrument cases of `ApplyCommandTest`, and the render cases of `InstrumentsCommandTest`, `CashAccountsCommandTest` and `InvestmentAccountsCommandTest`.
- Kept end to end:
  - `ListPagingTest`;
  - `SortAndOrderTest`;
  - `InstrumentAttributesTest`;
  - one or two smoke runs per list command and per operation;
  - CliRun's contract check.

No ADR (no candidate rejected) and no glossary change (implementation terms only).
