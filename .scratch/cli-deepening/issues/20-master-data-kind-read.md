# 20: MasterDataKind read side

Type: task
Status: ready-for-agent
Blocked by: 10, 16

Spec: [spec.md](../spec.md), "Master data kinds". Decision: [ticket 03](03-master-data-kind.md).

## What to build

Create `name.abuchen.portfolio.cli.masterdata` with `MasterDataKind<T>` and one declaration per kind, published as `INSTRUMENTS`, `CASH_ACCOUNTS` and `INVESTMENT_ACCOUNTS`.

- **Declaration**: it holds the entity id and noun, the entities, UUID and name, and an ordered **member** list. Each member has a name, a read schema, a getter and a render mode (*always* or *if set*). The declaration also holds the lookup keys and candidate details as member names (an undeclared name fails at class load), and the list command's summary and table columns.
  - Write fields come in ticket 21.
  - `attributes` (getter prepared once per client) and `updatedAt` (it also feeds the sort) are read-only members.
- **Derived from the declaration**: the item `Schema` and `render`, the reference lookup (UUID first, then pooled lookup keys, never guess), the paged list `CommandSpec` (built on ticket 10's facets), and `EntityKind.of(reference)`.
- **`Reference` interface** in `cli.command`: noun, entities, UUID, name, lookup keys, optional retired flag, candidate details. `Filters.reference(...)` uses it. Taxonomy's reference declaration sits next to `TaxonomiesCommand`. `MasterDataKind` exposes `reference()`.
- **Removed**:
  - `References.Kind`;
  - `MasterDataList.Source`;
  - the master data `EntityKind` constants;
  - `query/InstrumentsCommand`, `CashAccountsCommand` and `InvestmentAccountsCommand`;
  - the `command → query` import of `InstrumentsCommand.SOURCE`.

  `AttributeCodec`, the resolver and the list builder move to `cli.masterdata`.

## Acceptance criteria

- [ ] `cli.command` imports nothing from `query`
- [ ] The three list commands are derived, and their output and help are byte-identical (golden help unchanged)
- [ ] New `cli.masterdata` tests on an in-memory `Client` cover render per kind and the reference lookup (UUID, each lookup key, ambiguity candidates, retired)
- [ ] CLI suite green

## Tests deleted

- `ReferencesTest`, taken over by the per-kind lookup tests (taxonomy lookup cases move to a taxonomy reference test)
- The render cases of `InstrumentsCommandTest`, `CashAccountsCommandTest` and `InvestmentAccountsCommandTest`. Keep one or two smoke runs per list command.
- Keep `ListPagingTest`, `SortAndOrderTest` and `InstrumentAttributesTest`.
