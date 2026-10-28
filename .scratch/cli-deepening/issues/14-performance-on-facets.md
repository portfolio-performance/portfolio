# 14: Performance on facets

Type: task
Status: ready-for-agent
Blocked by: 10, 12

Spec: [spec.md](../spec.md), "Command facets". Decisions: [ticket 02](02-command-facets.md), [ticket 05](05-valuation-facet.md).

## What to build

Move `performance` onto `CommandSpec.of`.

- **`PerformancePeriod`** becomes a facet. It owns `period-required` and its own cross-option rules.
- **`PerformanceScope`** becomes a facet and absorbs `--by`, because `unsupportedScope` couples the two. It owns `conflicting-options` and the `option-required` rule for `--with-reference-cash-account`.
- **`ReportingCurrency.facet(…, DAYS)`**, built in ticket 13. If ticket 13 hasn't landed when this starts, add only what this ticket needs and let ticket 13 reuse it.
- Performance keeps its own `data.currencyCode`.
- `FacetHarness` tests for `PerformancePeriod` and `PerformanceScope`.

## Acceptance criteria

- [ ] `PerformanceCommand` declares its facets and returns data only
- [ ] The "required together" `option-required` and `conflicting-options` cases raise the same codes as before
- [ ] Golden help unchanged, or each difference is a meta-order or `--currency` line called out in the commit
- [ ] CLI suite green

## Tests deleted

- The `invalid-date-range` copy in `PerformanceCommandTest`
- Period and scope validation cases in `PerformanceScopesTest` and `PerformanceCommandTest` that the two facet tests cover (keep one smoke run per scope)
