# 05: What is the valuation module behind the Reporting currency?

Type: grilling
Status: open
Blocked by: 02

Map: [map.md](../map.md). Review card 2, "One valuation module behind the Reporting currency", in [architecture-review.html](../architecture-review.html#c2). Must keep ADR 0005 (rate source order, never guess, per-command total behaviour) and ADR 0002 (core's 1.0 fallback can't be fixed, so StrictConverter stays as the adapter).

## Question

What module do Holdings, Allocation, Transactions and Performance declare and ask for the **Reporting currency**, converted figures, **Stale price** and exchange-rate warnings, and `meta.exchangeRates`? How does it make "never report a figure computed at core's 1.0 rate" impossible to get wrong, and what sits behind its seam for tests?

Evidence from the review:

- Each converting command has about 7 touch points: the `--currency` option, `requested`/`orBase`, the meta currency plus `ratesMetaSchema`, `putRatesInto`, the WARNINGS declaration, warning forwarding, and warning order (Holdings 61/122/125/159/164/199–205, Allocation 60/93/96/114/118/157–166, Transactions 66/90/97/124/127/143–152, Performance 43/91/97/121/128/158–167).
- Never-guess is enforced three ways: Holdings uses its own `Figure`/`total`; Allocation pre-checks `allRated` (~135) and then trusts core, mirroring core internals (~130); Performance uses StrictConverter.
- StrictConverter's `isComplete()` is only valid once every lazy core figure has been read (PerformanceCommand ~148–150), and PerformanceByInstrument must `reset()` for each record (~100–106).
- `WARNINGS` and `WARNINGS_COUNTING_DAYS` describe the same codes differently depending on the caller (ReportingCurrency ~34–44).
- ReportingCurrency needs the whole CommandContext only for `env`, `osName` and `clock` (ExchangeRates ~206). ReportingCurrencyTest builds a 7-argument context with a null WriteEnvironment.
- StrictConverter and ExchangeRates have no direct tests.
- Holdings and Allocation both build `ClientSnapshot.create(client, coreConverter(), date)` plus StalePrices.

Sub-questions to settle: how the module fits ticket 02's facet interface; the shape of the trust call (e.g. `trusted(Supplier)`); the cache-locator seam and its two adapters (desktop ECB file, in-memory rates); the module's rate inversions call core's `ExchangeRate.inverse` instead of copying it, and no other rounding change applies (ticket 04, ADR 0007).
