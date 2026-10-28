# 10: Facet framework and the plain read commands

Type: task
Status: ready-for-agent
Blocked by: 09

Spec: [spec.md](../spec.md), "Command facets". Decision: [ticket 02](02-command-facets.md).

## What to build

The facet framework in `cli.command`, plus the read commands that need no Reporting currency.

- **Framework**:
  - `Facet<T>`, `Declaration`, `Report`, `Facet.of(...)` and `Facet.option(...)`.
  - `CommandSpec.of(name, summary, dataSchema, handler, facets…)`, with options, meta schema, warnings, error codes and table columns derived.
  - `CommandContext` gains `get(facet)`, a lazy `client()` and `warn(...)`.
  - The ordering and class-init rejections from ticket 02.
  - The programming-error `IllegalStateException`s.
- **Coexistence**: the old `CommandSpec` constructors and `usesClientFile` stay for unconverted commands until [ticket 15](15-finish-facets.md). `HelpText` handles both forms.
- **Facets built here**:
  - `ClientFile.READ` (`--file`, `fileVersion`/`asOf` meta, client-file codes);
  - `DateRange.facet(fromDesc, toDesc)` (`invalid-date-range`);
  - `Paging.FACET`;
  - `Sorting.Spec` as `Facet<Sorting<T>>`;
  - `Filters.reference(option, metaKey, metaDesc, predicate)`.

  Each takes its per-command descriptions as parameters.
- **Commands converted**: `info` (no `ClientFile`), `prices`, `taxonomies`, and the three master data lists through `MasterDataList`. Their handlers return data only.
- **`FacetHarness.run(facet, args, client)`** in `cli.tests`. It checks the meta fragment against the declared schema, the warnings against the declared ones, and that what the facet throws is within its declared codes. It has a test per facet built here.

If deriving the meta order changes a `Response meta:` block, update the golden file and call it out in the commit (spec, "Contract-change ripple").

## Acceptance criteria

- [ ] The six converted commands are built only with `CommandSpec.of`, and their handlers no longer touch `meta` or `Response`
- [ ] `CommandSpec.of` rejects a duplicated option or meta member at class-init (unit test)
- [ ] Usage errors still come before file errors (e.g. a bad `--from` together with a missing file)
- [ ] Every facet built here has a `FacetHarness` test
- [ ] Golden help unchanged, or each difference is a meta-order line called out in the commit
- [ ] CLI suite green

## Tests deleted

- The per-command `invalid-date-range` cases in `PricesCommandTest` (the other copies go in tickets 13 and 14)
- Paging and sorting meta cases in `PricesCommandTest`, now covered by `FacetHarness`. `ListPagingTest` and `SortAndOrderTest` stay as the end-to-end paging and sorting runs.
