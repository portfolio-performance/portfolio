# 13: Converting commands on facets: holdings, allocation, transactions

Type: task
Status: ready-for-agent
Blocked by: 10, 12

Spec: [spec.md](../spec.md), "Command facets" and "Valuation". Decisions: [ticket 02](02-command-facets.md), [ticket 05](05-valuation-facet.md).

## What to build

- **`ReportingCurrency.facet(desc, Counting)`** and **`ifGiven(desc)`**, with `enum Counting { ITEMS, DAYS }`. `WARNINGS` and `WARNINGS_COUNTING_DAYS` go. The facet declares `--currency`, the `currency` and `exchangeRates` meta, and the three rate warnings. Its `report()` puts the meta and forwards the warnings.
- **`OptionSpec.currency(...)`**: a typed option, validated at parse time. It brings `invalid-option-value`. `ReportingCurrency.requested` and `orBase` go.
- **`StalePrices.FACET`** declares `stale-price`. Its value is the collector `note(security, latest, asOf)`, and `report()` sorts by name, then UUID.
- **`OptionSpec.choices(...)`**: multi-choice (comma-separated, case-insensitive, deduplicated in published order), with a "one or more of …" help line. It replaces `TransactionsCommand.parseTypes`.
- **Commands converted to `CommandSpec.of`**:
  - `holdings`: `StalePrices.FACET` before `ReportingCurrency.facet(…, ITEMS)`, plus its date and reference filters;
  - `allocation`: `StalePrices.FACET` before `ReportingCurrency.facet(…, ITEMS)`. `over-assigned` now comes from the handler, ahead of `stale-price`; that is accepted;
  - `transactions`: `ReportingCurrency.ifGiven(…)`, `DateRange`, the reference filters, `OptionSpec.choices` for `--type`, paging and sorting.
- `FacetHarness` tests for both currency facets and for `StalePrices.FACET`.

## Acceptance criteria

- [ ] No converting handler touches `currency`/`exchangeRates` meta or forwards rate or stale-price warnings
- [ ] An invalid `--currency` or `--type` value is `invalid-option-value` before any file error
- [ ] Golden help updated for the `--type` line, and for `--currency` if it renders differently; each called out in the commit
- [ ] Tests on warning order across codes assert sets, not order
- [ ] CLI suite green

## Tests deleted

- The `invalid-date-range` copy in `TransactionsCommandTest`
- `--type` parsing cases in `TransactionsCommandTest`, covered by an `OptionSpec.choices` unit test
- The `--currency` validation cases repeated across `HoldingsCommandTest`, `AllocationCommandTest` and `TransactionsConversionTest`, covered by the facet test (keep one smoke run per command)
- `StalePricesTest` cases duplicated by the `StalePrices.FACET` harness test
