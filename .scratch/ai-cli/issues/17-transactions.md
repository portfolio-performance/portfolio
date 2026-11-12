# 17: `transactions` as joined transaction records

Type: task
Status: ready-for-agent
Blocked by: 16

Spec: [spec.md](../spec.md), user stories 34–41, 51 (the `--from`/`--to` part), 58–59 and 62, and "Transaction records", "Currency rules" and "Periods" under "Read surface". [ADR 0005](../../../docs/adr/0005-exchange-rates-from-desktop-ecb-cache.md).

## What to build

`pp-cli transactions` returns each business event as one **transaction record**:

- A buy or sell joins its investment-account side and its cash-account side.
- A transfer carries its `from` and `to` accounts and a `kind`.
- The record `id` is the investment-account side for buys and sells, the outgoing side for transfers, and the transaction itself otherwise.
- Every side's UUID is listed.
- `type` uses core's enum names and is documented as an open enum.

Filters cover account, instrument (through the resolver), date range (`--from`/`--to`, with `--to` defaulting to today from the injected clock) and type. Results are paginated and ordered by date-time, then `id`. Pagination reuses ticket 15's shared mechanism unchanged: `data = {items, total, nextOffset}`, `meta` echoes `limit` and `offset`, and the same validation and usage error apply (see ticket 15's "Decisions").

Amounts are in the record's native currency. `--currency` adds a converted amount that carries the rate, the rate date and the rate's source. Rates come from the transaction itself, exchange-rate instruments or the desktop app's ECB cache, never from core's built-in data (ADR 0005). This ticket builds that rate machinery as a shared helper; tickets 18–21 reuse it. Fees, taxes and forex units use the same shape that later `transaction.*` writes accept.

## Acceptance criteria

- [x] A buy shows both sides in one record; a transfer is one record with `from`, `to` and `kind`, never counted twice; an orphaned side is its own record under its raw core type
- [x] The `id` rule and the `sides` list hold for every record type
- [x] The account, instrument, date-range and type filters work, alone and combined
- [x] Pagination is ordered by date-time, then `id`, with `total` and `nextOffset`
- [x] Converted amounts under `--currency` carry `rate`, `rateDate` and `rateSource`; series-based ones are cross-checked against core's currency converter on the same rates
- [x] A missing rate leaves `converted` out and warns `exchange-rate-missing`; a rate more than 7 days old warns `stale-exchange-rate`; each warning appears once per currency pair with a `count`
- [x] The ECB cache is found through `PP_ECB_RATES` or core's default location built from the injected environment; a missing or unreadable cache warns `exchange-rate-cache-unavailable` and never fails the command; core's built-in 2015 rates and its 1.0 fallback are never used
- [x] `--to` defaults to the injected clock's date, and the resolved dates, filters and currency are echoed in `meta`
- [x] `transactions` is declared, appears in `describe` (including the open `type` enum, the three exchange-rate warning codes and `PP_ECB_RATES`) and passes its consistency test
- [x] Ticket 12's exchange-rate criterion is met and ticked

## Decisions

Settled with the owner in a design session (2026-10-17). Implement them as written; don't re-ask.

### Record types

- `type` is one of: `BUY`, `SELL`, `DEPOSIT`, `REMOVAL`, `INTEREST`, `INTEREST_CHARGE`, `DIVIDENDS`, `FEES`, `FEES_REFUND`, `TAXES`, `TAX_REFUND`, `DELIVERY_INBOUND`, `DELIVERY_OUTBOUND`, `TRANSFER`.
- `TRANSFER` carries `kind`: `cash` (an `AccountTransferEntry`) or `security` (a `PortfolioTransferEntry`). `TRANSFER_IN`/`TRANSFER_OUT` never appear for an intact transfer.
- **Orphaned sides**: a side whose cross entry or partner is missing (only in damaged or hand-edited files) becomes its own one-sided record under its raw core type name, e.g. `TRANSFER_OUT`, or `BUY` with only `cashAccount`. No warning. This is what the open enum is for.
- `type` is an **open enum**: `describe` publishes it as a string with its known values (all of the above plus `TRANSFER_IN` and `TRANSFER_OUT`), not as a closed `enum`, so a new upstream type doesn't fail validation. Extend `Schema` (and the test-side `SchemaValidator`) with that open-enum form.

### Identity

- `id`: the investment-account side for buys and sells, the outgoing side for transfers, and the transaction itself otherwise (including orphans).
- `sides`: an array of every side's UUID, `id` first. Its length is 2 for buys, sells and transfers, and 1 otherwise.

### Date and time

- `date` is always `yyyy-MM-dd`. `time` is `HH:mm` and is omitted when the stored time is midnight.
- Ordering: full stored date-time ascending, then `id` (ordinal).
- `--from`/`--to` compare calendar dates, both inclusive, as in ticket 16. `--to` defaults to today from the injected clock, so future-dated records are hidden unless the agent passes a later `--to`; the `--to` description in `describe` says so. `--from` after `--to` is `invalid-date-range`.

### Record fields

A flat object. Unset fields are omitted, never `null` (ticket 15's field rules: null or blank strings count as unset).

| Field | When | Content |
|---|---|---|
| `id`, `type`, `date`, `sides`, `amount`, `currencyCode`, `units` | always | `units` is `[]` when empty |
| `time` | not midnight | `HH:mm` |
| `kind` | `TRANSFER` | `cash` or `security` |
| `instrument` | the record has a security | UUID |
| `shares` | `BUY`, `SELL`, `DELIVERY_*`, security `TRANSFER`; `DIVIDENDS` when non-zero | decimal, `Values.Share` precision |
| `investmentAccount` | `BUY`, `SELL`, `DELIVERY_*`, orphaned investment-account sides | UUID |
| `cashAccount` | `BUY`, `SELL`, every cash-account type, orphaned cash-account sides | UUID |
| `from`, `to` | `TRANSFER` | account UUIDs (cash accounts for `cash`, investment accounts for `security`) |
| `targetAmount`, `targetCurrencyCode` | cross-currency cash transfer | the incoming side's amount and currency |
| `exchangeRate` | cross-currency cash transfer with a gross-value unit | see below |
| `exDate` | `DIVIDENDS` with an ex-date | `yyyy-MM-dd` |
| `note`, `source` | when set | `source` is core's import origin, unrelated to transfers |
| `converted` | `--currency` given and a rate found | see "Conversion" |

- `amount` is the net value, as stored, in `currencyCode`. There is no `grossAmount` on read. Amounts are decimals with `Values.Amount` precision via `Decimals`.
- References are bare UUIDs, like `referenceCashAccount` in ticket 15.
- For buys and sells, `amount`, `currencyCode`, `units` and `note` come from the investment-account side; for transfers, from the outgoing side.
- **Cross-currency cash transfers**: core keeps a `GROSS_VALUE` unit on the outgoing side whose amount is the outgoing amount and whose forex is the incoming amount. It becomes the top-level `exchangeRate`, with the unit's meaning: `targetAmount × exchangeRate ≈ amount`. It is emitted unrounded as stored and is **not** repeated in `units`, so a record read and written back doesn't state the rate twice. A same-currency transfer omits `targetAmount`, `targetCurrencyCode` and `exchangeRate`.

### Units

- Each unit is `{type, amount, currencyCode, forexAmount?, forexCurrencyCode?, exchangeRate?}`. `type` is `FEE`, `TAX` or `GROSS_VALUE`.
- `exchangeRate` is core's stored value, unrounded, meaning `forexAmount × exchangeRate ≈ amount`. The three forex fields appear together or not at all.
- Units come from the side named above (the investment-account side for buys and sells, the outgoing side for transfers). The same shape is the write input of tickets 26–28.

### Filters

- `--investment-account <ref>`, `--cash-account <ref>` and `--instrument <ref>` resolve through ticket 16's resolver, each against its own kind, with its errors.
- An account filter matches a record if **any** side is in that account: `investmentAccount`/`cashAccount`, or `from`/`to` of a transfer of the matching kind.
- `--instrument` matches every record whose `instrument` is that instrument, including dividends, fees and taxes.
- `--type <list>`: comma-separated, case-insensitive, any value `type` can emit (including `TRANSFER_IN`/`TRANSFER_OUT`). An unknown or empty value is `invalid-option-value` with `option` and `value`.
- Filters combine with AND. `total` counts the filtered records.

### Response shape

```json
{
  "data": {
    "items": [
      {
        "id": "…", "type": "BUY", "date": "2026-03-02", "time": "09:30",
        "sides": ["…portfolio side…", "…account side…"],
        "amount": 1005.00, "currencyCode": "EUR",
        "instrument": "…", "shares": 10.00000000,
        "investmentAccount": "…", "cashAccount": "…",
        "units": [
          { "type": "FEE", "amount": 5.00, "currencyCode": "EUR" },
          { "type": "GROSS_VALUE", "amount": 1000.00, "currencyCode": "EUR",
            "forexAmount": 1085.00, "forexCurrencyCode": "USD", "exchangeRate": 0.9216589862 }
        ],
        "converted": { "currencyCode": "USD", "amount": 1090.42, "rate": 1.0850000000,
                       "rateDate": "2026-03-02", "rateSource": "transaction" }
      }
    ],
    "total": 1
  },
  "meta": {
    "fileVersion": 66, "asOf": "2026-10-17", "limit": 200, "offset": 0,
    "from": "2026-01-01", "to": "2026-10-17", "currency": "USD",
    "instrument": "…", "types": ["BUY", "SELL"],
    "exchangeRates": { "path": "C:\\Users\\…\\ecb_exchange_rates.pb", "latestRateDate": "2026-10-16" }
  },
  "warnings": []
}
```

- `meta` is the paged meta plus `to` (always), and `from`, `currency`, `investmentAccount`, `cashAccount`, `instrument` (resolved UUIDs) and `types` (normalised to upper case, deduplicated, in the published order) when given.
- `meta.exchangeRates` is present only when the ECB cache was loaded.

### Conversion (`--currency`)

- `--currency <code>` must be a currency core knows (`CurrencyUnit.getInstance`); otherwise `invalid-option-value`.
- Each record gets `converted: {currencyCode, amount, rate, rateDate, rateSource}`. Only `amount` is converted; units, `targetAmount` and `shares` stay native.
- The day needed is the record's date. Rates are tried in this order, and `rateSource` says which applied:
  1. `identity`: `currencyCode` already equals `--currency`. Rate 1, `rateDate` = the record's date.
  2. `transaction`: a `GROSS_VALUE` unit's `forexCurrencyCode`, or a cross-currency transfer's `targetCurrencyCode`, equals `--currency`. The rate is the inverse of the stored rate, computed as core's `InverseExchangeRateTimeSeries` does (10 decimal places, `HALF_DOWN`); `rateDate` = the record's date.
  3. `instrument` or `ecb`: the rate series from the CLI's exchange-rate setup below. `ecb` if any step of the chain comes from the ECB cache, `instrument` if every step comes from exchange-rate instruments.
- `converted.amount` = `amount × rate`, rounded as core's `CurrencyConverter.convert` does (minor units, `HALF_DOWN`).

### Exchange-rate setup (shared helper, ADR 0005)

- One helper builds the CLI's rate source; tickets 18–21 reuse it. It subclasses `ExchangeRateProviderFactory` and overrides `getAvailableTimeSeries()` to drop core's `ECBExchangeRateProvider` series and add the CLI's own series read from the ECB cache. Exchange-rate instruments still come from core's `SecurityBasedExchangeRateProvider`.
- **Locating the cache**:
  1. `PP_ECB_RATES`, if set (non-blank), is the file path.
  2. Otherwise core's own rule with the product's default instance area: `<workspace>/.metadata/.plugins/name.abuchen.portfolio/ecb_exchange_rates.pb`, where `<workspace>` is `%LOCALAPPDATA%\PortfolioPerformance\workspace` on Windows, `$HOME/Library/Application Support/name.abuchen.portfolio.product/workspace` on macOS, and `$HOME/.PortfolioPerformance/workspace` on Linux.
  - `LOCALAPPDATA` and `HOME` come only from the injected environment, never from `System.getenv` or `user.home`, so tests never touch the real profile. No fallback to older locations and no reading of the app's `.ini`.
  - `describe` lists `PP_ECB_RATES` among its environment variables.
- **Lazy**: the cache is read only on the first conversion that needs a rate series; `identity` and `transaction` conversions never read it.
- **Parsing**: the CLI parses `PECBData` (`lastModified` = 1, `series` = 2), `PExchangeRateTimeSeries` (`baseCurrency` = 1, `termCurrency` = 2, `exchangeRates` = 3), `PExchangeRate` (`date` = 1 as epoch day, `value` = 2) and `PDecimalValue` (`scale` = 1, `precision` = 2, `value` = 3 as the unscaled value's two's-complement big-endian bytes, decoded as core's `ProtobufUtil.fromDecimalValue` does) itself with protobuf-java's wire reader, because core's generated classes aren't exported. `latestRateDate` is the newest rate date across all series.
- **Lookup** walks the chain (`getComposition()`, unwrapping inverses) itself, because core stamps a chained rate with the requested date and answers dates before the first rate with that later rate:
  - Each step's rate must be dated on or before the day needed; otherwise the rate is **missing**.
  - `rateDate` is the oldest step's date; the rate value is core's product of the steps (inverses as core computes them).
  - The rate is **stale** when `rateDate` is more than 7 calendar days before the day needed (`rateDate.isBefore(day.minusDays(7))`).
  - Core's `CurrencyConverterImpl` fallback (1.0 dated `LocalDate.now()`) is never reached or used.

### Warnings

All three are declared on the command and published in `describe`. Each appears at most once per response, per currency pair where it has one, and covers the records on the returned page.

| Code | When | Extensions |
|---|---|---|
| `exchange-rate-missing` | no usable rate; those records have no `converted` | `baseCurrency`, `termCurrency`, `count` |
| `stale-exchange-rate` | a rate more than 7 days old was used | `baseCurrency`, `termCurrency`, `count`, `maxAgeDays` |
| `exchange-rate-cache-unavailable` | the cache is needed but missing, unreadable or unparsable (including a bad `PP_ECB_RATES`) | `path` (when known), `reason` |

- An unavailable cache only removes the ECB source; rates from transactions and exchange-rate instruments still work, and the command still succeeds.

### Tests

- Test seam: the in-process `PpCli.execute`, with fixtures from the shared JUnit builders and an ECB cache file written by the test (protobuf-java's wire writer) in a temp directory.
- Point `PP_ECB_RATES` at that file, or set `LOCALAPPDATA`/`HOME` in the injected environment to a temp tree, to cover both lookup paths; an environment with neither covers the unavailable case.
- Cross-check series-based conversions against core's `CurrencyConverterImpl` over a factory with the same rates, on dates where each step has a rate on or before the day.
- Cover: each record type and kind, orphaned sides, a cross-currency transfer's `exchangeRate`, every filter alone and combined, ordering with equal timestamps, each `rateSource`, chained rates with differing step dates, a date before the first rate (missing), a stale rate, and warning deduplication.
- `transactions` needs a case in `DescribeConsistencyTest.sampleArguments` and a declaration in `Commands.ALL`.
