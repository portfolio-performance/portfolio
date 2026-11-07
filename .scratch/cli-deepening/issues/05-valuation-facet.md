# 05: What is the valuation module behind the Reporting currency?

Type: grilling
Status: resolved
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

## Answer

Resolved by grilling. All six proposals were accepted as put.

**Module.** It keeps the glossary name: `ReportingCurrency` in `cli.currency`, with no new glossary term. `ExchangeRates`, `StrictConverter` and `EcbCache` become its private implementation. `Prices` and `StalePrices` stay in the package as neighbours.

**Facet.** In the ticket 02 facet interface:

```java
static Facet<ReportingCurrency> facet(String description, Counting counting);       // default: the file's base currency (holdings, allocation, performance)
static Facet<Optional<ReportingCurrency>> ifGiven(String description);              // no default (transactions)
enum Counting { ITEMS, DAYS }                                                         // picks the warning wording; WARNINGS / WARNINGS_COUNTING_DAYS go
```

- **What it declares:** `--currency`, the meta members `currency` and `exchangeRates`, and the warnings `exchange-rate-missing`, `stale-exchange-rate` and `exchange-rate-cache-unavailable`.
- **`report()`:** puts both meta members and forwards the rate warnings. Handlers no longer touch them.
- **`needsClient`:** yes, because the default is the base currency. Validation of the currency code moves into a typed `OptionSpec.currency(...)`, checked at parse time like `OptionSpec.choices`. That type brings `invalid-option-value`, so usage errors still come before file errors.
- **Removed:** `requested`/`orBase`, `ratesMetaSchema` as a per-command call, `putRatesInto` and the warning forwarding all go from the commands. Performance keeps its own `data.currencyCode`.

**Stale price.** `StalePrices.FACET` is a separate facet.

- It declares `stale-price`. Its value is the collector, now `note(security, latest, asOf)`. Its `report()` emits the warnings sorted by name, then UUID.
- Holdings and Allocation list it before `ReportingCurrency`, so stale-price warnings still precede the rate warnings.
- Allocation's `over-assigned` comes from the handler and therefore moves ahead of `stale-price`. This is accepted: warning order across codes is not contract, and no test may depend on it.

**Trust call for figures core calculates.** The raw `coreConverter()` and `strictConverter()` leave the interface. In their place:

```java
CoreFigures core = reporting.coreFigures();
CurrencyConverter converter();                                   // strict: notes every rate core asks for
<T> Optional<T> trusted(Supplier<T> figures);                    // fresh check, run, empty if any rate was missing (DAYS counting)
<T> Optional<T> trusted(List<Money> items, Supplier<T> figures); // ITEMS counting: see Allocation
CurrencyConverter unreported();                                  // core runs whose figures are never reported; silent, tolerates core's 1.0
```

- **Finished data only.** The supplier must return finished data, the rendered map, so every lazy core figure is read inside the call. `isComplete()` and `reset()` are no longer visible to callers.
- **Performance:** `core.trusted(() -> figures(index, snapshot)).ifPresent(data::putAll)`. `PerformanceByInstrument` makes the same call per record. `quietInstrument` uses `unreported()`, and Holdings builds its `ClientSnapshot` on `unreported()` too: both report no figure core converted.
- **Holdings and Transactions:** they keep the CLI's own `figure`/`total`/`putConvertedInto` path. It is safe by construction, because a figure without a rate has no `converted` and spoils the `total`.
- **Grep-ability:** `unreported()` is the single, greppable place where core's fallback is tolerated.

**Allocation.** It uses `trusted(items, supplier)`.

- Each holding's value is looked up for the warnings, as today, so the counts still count holdings.
- The supplier runs over a strict converter that stays silent.
- The verdict is "every item rated **and** core found every rate it asked for". This closes the gap where `allRated` duplicated core's internals.
- If core misses a rate that the item pre-check didn't need, that pair gets an `exchange-rate-missing` with count 1. `allRated` goes.

**Seam and adapters.**

- `ReportingCurrency` is built from `(code, client, RateCache, today)` and no longer takes a `CommandContext`.
- The seam:

  ```java
  interface RateCache { EcbCache.Loaded load() throws EcbCache.Unavailable; }
  ```

- Two adapters:
  - `RateCache.desktop(env, osName)` is the production adapter: `PP_ECB_RATES`, then the OS default (ADR 0005).
  - `RateCache.of(series...)` holds rates in memory for tests.
- The facet's `resolve` builds the desktop adapter from `run`. The cache is still read lazily, on the first lookup that needs a series.

**Rounding.** `ExchangeRates.find` inverts a step with core's `ExchangeRate.inverse(BigDecimal)` instead of a copy of it. The two compute the same value (scale 10, HALF_DOWN), so no output changes. No other rounding changes (ticket 04, ADR 0007).

**Tests (replace, don't layer).**

- `ReportingCurrencyTest` becomes an interface test over `RateCache.of` and drops its 7-argument context with the null `WriteEnvironment`. It covers:
  - identity, own rate, instrument, ECB and fixed sources;
  - a chained rate dated by its oldest step;
  - a rate before the first one counted as missing;
  - stale rates;
  - each cache-unavailable reason;
  - `trusted` with a lazy supplier;
  - the item and day counts;
  - the core-only missing rate.
- `StrictConverter` and `ExchangeRates` get no direct tests: this suite covers them through the interface.
- `FacetHarness` covers both facets' declarations.
- The per-command CliFixture tests that re-check missing, stale and cache cases are cut down to one smoke run per converting command.

**Contract.** Two possible changes, both for the contract-change ripple:

- the `--currency` help line, if the typed option renders differently (the golden help test will show it);
- the new count-1 `exchange-rate-missing` in Allocation.

No ADR: this deepening keeps ADR 0005 and ADR 0002 and makes no new hard-to-reverse trade-off.
