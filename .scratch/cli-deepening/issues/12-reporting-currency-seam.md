# 12: ReportingCurrency seam and trust call

Type: task
Status: resolved
Blocked by: 08

Spec: [spec.md](../spec.md), "Valuation". Decision: [ticket 05](05-valuation-facet.md).

## What to build

Deepen `ReportingCurrency` behind its new interface, without the facet yet ([tickets 13](13-converting-commands-on-facets.md) and [14](14-performance-on-facets.md) add that).

- **Construction and seam**: `ReportingCurrency` is built from `(code, client, RateCache, today)` and no longer takes a `CommandContext`. The seam is `interface RateCache { EcbCache.Loaded load() throws EcbCache.Unavailable; }`, with two adapters:
  - `RateCache.desktop(env, osName)`: `PP_ECB_RATES`, then the OS default (ADR 0005);
  - `RateCache.of(series…)`: in memory.

  The cache is still read lazily.
- **Trust call**: `coreFigures()` returns `CoreFigures` with `converter()`, `trusted(Supplier)`, `trusted(List<Money> items, Supplier)` and `unreported()`. The raw `coreConverter()` and `strictConverter()` leave the interface. `StrictConverter`'s `isComplete()` and `reset()` become private.
- **Callers switched**:
  - Performance and `PerformanceByInstrument`: `trusted(...)`.
  - `quietInstrument` and Holdings' `ClientSnapshot`: `unreported()`.
  - Allocation: `trusted(items, …)`. `allRated` goes. If core misses a rate that the item pre-check didn't need, that pair gets an `exchange-rate-missing` with count 1.
- **Rounding**: `ExchangeRates.find` and the inversion in `TransactionsCommand` (~302) call core's `ExchangeRate.inverse` (ticket 04). The output stays the same.
- `ExchangeRates`, `StrictConverter` and `EcbCache` become package-private implementation where nothing outside `cli.currency` needs them.

The commands keep their current option, meta and warning wiring in this ticket; only how they get converters changes.

## Acceptance criteria

- [ ] `ReportingCurrencyTest` is an interface test over `RateCache.of`, with no `CommandContext` or null `WriteEnvironment`. It covers identity, own rate, instrument, ECB and fixed sources; a chained rate dated by its oldest step; a rate before the first counted as missing; stale rates; each cache-unavailable reason; `trusted` with a lazy supplier; item and day counts; and the core-only missing rate
- [ ] `unreported()` is the only call site in `cli` that hands core a converter which tolerates the 1.0 fallback (grep)
- [ ] No hand-copied rate inversion remains
- [ ] Allocation's core-only missing rate is reported with count 1 (one end-to-end case in `AllocationCommandTest`)
- [ ] CLI suite green

## Tests deleted

- The old construction-level cases of `ReportingCurrencyTest`, rewritten as above
- The per-command missing, stale and cache-unavailable cases in `HoldingsCommandTest`, `AllocationCommandTest`, `PerformanceCommandTest` and `TransactionsConversionTest` that the new interface test covers. Keep one smoke run per converting command for a missing rate and one for a stale rate.

## Answer

Implemented on integration branch `feat/cli-deepening`, merge commit 3d18650fa. RateCache seam (desktop, in-memory) and coreFigures().trusted/unreported. trusted(items, ...) also takes the valuation day.
