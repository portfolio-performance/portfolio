# Exchange rates come from the desktop app's ECB cache, read-only, never from core's built-in data

`pp-cli` runs core without OSGi (ADR 0002). In that setting, core's ECB provider finds no storage location and keeps its built-in rates, a single snapshot from 2015-12-18 that it returns for any date. ECB rates are never stored in the client file: the desktop app keeps them in one app-wide cache, `ecb_exchange_rates.pb` in the platform state location of `name.abuchen.portfolio`, and refreshes it at startup. So the original plan (offline ECB data, no persisted caches, and a warning when rates fell back to the built-in data) would have made every ECB conversion a 2015 conversion. We decided instead that every command takes rates from, in order:

1. the transaction's own rate (its forex gross-value unit, or a cross-currency transfer);
2. **Exchange-rate instruments** in the client file, which core already prefers;
3. the desktop app's ECB cache, read-only.

The built-in data and core's silent fallback rate of 1.0 are never used, with one exception: `performance` has core itself calculate its figures through core's converter, so a pair without a rate is calculated with 1.0 there. It looks up the rate of every foreign currency the client uses for the last day of the period and issues the same warnings, so the figures are never silently wrong.

**Locating the cache.** The `PP_ECB_RATES` environment variable names the file and wins outright. Otherwise the CLI mirrors core's own rule, `<instance area>/.metadata/.plugins/name.abuchen.portfolio/ecb_exchange_rates.pb`, with the product's default instance area for each OS:

- Windows: `%LOCALAPPDATA%\PortfolioPerformance\workspace`
- macOS: `~/Library/Application Support/name.abuchen.portfolio.product/workspace`
- Linux: `~/.PortfolioPerformance/workspace`

Home directories come only from the CLI's injected environment, never from the real process. The CLI doesn't read the app's launcher `.ini`, so a custom `-data` workspace needs `PP_ECB_RATES`. There is no fallback to the bundle data area that older releases used, just as core has none. The cache is read only when a command needs a conversion. The CLI parses the three small protobuf messages itself (`PECBData`, `PExchangeRateTimeSeries`, `PExchangeRate`), because core's generated classes and its provider's injectable constructor aren't exported. It supplies the rates by overriding `ExchangeRateProviderFactory.getAvailableTimeSeries()`, which drops core's ECB series, so core's snapshot and performance engines use the same rates.

**Honest rates.** Core labels a chained rate (e.g. USD → EUR → CHF) with the requested date, and it answers a date before the first rate with that first, later rate. The CLI therefore looks up each step of the chain itself:

- A step counts only if its rate is dated on or before the day needed; otherwise the rate is **missing**.
- The reported rate date is the oldest step's date.
- A rate whose date is more than 7 days before the day needed is **stale**, the same threshold as stale prices.
- A missing rate is never guessed: the figure is left out and a warning issued. A stale rate is used, with a warning.
- A missing or unreadable cache, including a `PP_ECB_RATES` that points nowhere, removes the ECB source with one warning and never fails the command.
- `meta.exchangeRates` names the cache path and its latest rate date whenever the cache was used.

**Considered Options**

- *Client-file rates only*: rejected. Few users keep exchange-rate instruments, so multi-currency totals would usually be impossible and wouldn't match the app.
- *Download from the ECB*: rejected. It breaks the offline guarantee; agent sandboxes often lack network access, and tests would depend on the network.
- *Bundle an ECB snapshot with each CLI build*: rejected. It's stale from the day it ships, the same flaw with a later date.
- *Built-in data with a fallback warning* (the original plan): rejected. It warns on every conversion, while the figures are still wrong.
- *Start Equinox to reuse core's lookup*: rejected. It would resolve the CLI's own instance area, not the desktop app's.

**Consequences**

- CLI figures match the desktop app's, but are only as fresh as the app's last start. `meta.exchangeRates.latestRateDate` makes that visible, so an agent can suggest opening PP to refresh.
- The cache format is internal to core (it moved from XML to protobuf in 2026). If it changes, the cache reads as unavailable rather than producing wrong figures; the parser then needs updating alongside the pinned core.
- Desktop releases older than upstream `86b6eb860` keep the cache elsewhere; users of those need `PP_ECB_RATES` or an upgrade.
- Commands that aggregate (`holdings`, `performance`, `allocation`) inherit the never-guess rule; each of those tickets decides how a total behaves when one of its parts has no rate. `holdings` leaves the total out. `performance` reports core's figures and warns.
- `describe` lists `PP_ECB_RATES` among the environment variables, and each converting command lists `exchange-rate-cache-unavailable`, `exchange-rate-missing` and `stale-exchange-rate` among its warning codes.
