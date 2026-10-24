# What does the read command surface look like?

Type: grilling
Status: resolved

## Question

Which read commands and calculated figures does the CLI expose (e.g. list securities, accounts, portfolios, transactions, holdings snapshot at a date, performance over a period, taxonomy breakdown), how are entities addressed (UUID, name, ISIN), and what output format do agents get (JSON by default, human table optional)?

## Answer

All commands are read-only, one JVM per call: `pp-cli <resource> <verb> --file <path>` (`PP_FILE` env fallback; encrypted-file password via `PP_PASSWORD` or stdin prompt, never a flag).

### Commands

- `info`: file version, base currency, counts.
- `instruments`, `cash-accounts`, `investment-accounts`: listings with a `retired` flag; instruments include user-defined attributes (type, name, value).
- `transactions`: logical records, one per business event (a buy or sell carries both its investment-account and cash-account legs; `TRANSFER` carries source and target with a `kind` field). Filters: account, instrument, date range, type. `type` mirrors the core enum names (`BUY`, `SELL`, `DEPOSIT`, `REMOVAL`, `DIVIDENDS`, `INTEREST`, `INTEREST_CHARGE`, `FEES`, `FEES_REFUND`, `TAXES`, `TAX_REFUND`, `TRANSFER`, `DELIVERY_INBOUND`, `DELIVERY_OUTBOUND`, ...) and is documented as an open enum.
- `holdings --date`: aggregated across investment accounts; `--investment-account <ref>` filters, `--by-account` returns one block per account. Zero-share rows are hidden unless `--include-zero`. Cash balances are a separate `cash` section.
- `prices --instrument <ref> [--from --to]`: dated price history, including the latest price and its date.
- `performance`: scopes are client, one investment account (optionally with its reference cash account), one cash account, one instrument. `--by instrument` returns one record per instrument active in the period (IRR, TTWROR, absolute gain, dividends, fees, taxes, opening and closing value). An instrument with no activity yields a `no-activity-in-period` warning, not an error. Classification performance is out of v1.
- `taxonomies`, `allocation --taxonomy <ref> --date`: flat rows (classification id, name, parent path, weight, value, share) that reconcile to 100% with an "unassigned" row.
- `describe`: machine-readable commands, options and response schemas, generated from the same definitions that implement the commands.

Deferred: `income` (dividends, interest, fees, taxes over a period) and matched trades.

### Cross-cutting rules

- **Addressing**: the UUID is canonical in all output and input. ISIN, ticker or exact name are accepted as a convenience and never guess: an ambiguous match fails and lists the candidates. Core has no lookup helper, so the CLI module adds one.
- **Output**: JSON on stdout by default, optional `--format table` (nice-to-have). Logs and warnings go to stderr. Errors are structured (problem+json style) with a non-zero exit code.
- **Envelope**: `{ "data": ..., "meta": { fileVersion, asOf, currency, resolved period, cost method, ... }, "warnings": [...] }`. Warnings cover stale prices, a missing exchange rate that fell back to built-in 2015 ECB data, and no activity in the period.
- **Numbers**: decimals with an ISO `currency` field, never internal scaled longs. Dates are ISO 8601. Percentages and rates are unrounded fractions (`0.0732` is 7.32%).
- **Periods**: `--from <date> --to <date>` (`--to` defaults to today) or `--period <ReportingPeriod code>` (for example `L1Y0`, `YTD`, `Y2025`); passing both is a usage error. The resolved dates are always echoed.
- **Currency**: records (`instruments`, `cash-accounts`, `transactions`) are native with an ISO code per amount, and `--currency` adds converted fields alongside. Aggregates (`holdings` totals, `performance`, `allocation`) convert to the client's base currency, and `--currency` picks another; `holdings` rows also carry the native amount. The reporting currency is always in `meta`.
- **Conversion rate date**: transaction date (or the transaction's own forex rate) for transactions, as-of date for `holdings` and `allocation`, PP's engine behaviour for `performance`. Each converted amount carries the rate and rate date used.
- **Cost method**: `--cost-method fifo|moving-average`, default `fifo`, always echoed.
- **Pagination**: list commands take `--limit` (default 200) and `--offset`; the response carries `total` and `nextOffset`; ordering is date then UUID.
- **Field naming**: camelCase, following #5870's `openapi.yaml` where an equivalent exists, the same style elsewhere. Awkward #5870 shapes are not copied.

### Core building blocks

`ClientSnapshot` (holdings, `groupByTaxonomy`), `PerformanceIndex` (TTWROR, IRR), `LazySecurityPerformanceSnapshot` (per-instrument records), `ReportingPeriod.from(String)`. Core has no general JSON serializer for the model, so the CLI module owns the JSON mapping.
