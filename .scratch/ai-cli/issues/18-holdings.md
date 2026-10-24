# 18: `holdings` at a date

Type: task
Status: ready-for-agent
Blocked by: 17

Spec: [spec.md](../spec.md), user stories 42–46, 57 and 58.

## What to build

`pp-cli holdings --date <date>` answers "what do I own and what is it worth". Holdings are aggregated across investment accounts by default. `--investment-account <ref>` filters to one account and `--by-account` splits the output into one block per account. Zero-share holdings are hidden unless `--include-zero` is given.

A separate `cash` section lists cash account balances. Totals are in the base currency, or in `--currency`, converted at the as-of date with the rate and rate date shown. Each row also gives its native amount. A stale price produces a warning. `meta` carries the as-of date and the reporting currency.

## Acceptance criteria

- [x] Share counts and values match core's client snapshot for the same fixture and date
- [x] `--investment-account`, `--by-account` and `--include-zero` behave as described
- [x] The `cash` section lists cash account balances at the date
- [x] Totals are in the base currency or `--currency`, rows carry native amounts, and converted amounts carry the rate and rate date
- [x] Stale prices and stale, missing or unavailable exchange rates produce warnings, using ticket 17's rate rules (ADR 0005); this ticket decides how a total behaves when one of its parts has no rate
- [x] `holdings` is declared, appears in `describe` and passes its consistency test

## Decisions

Made while implementing; the owner can overrule any of them.

- **A total with a part that has no rate is left out**, in line with "a missing rate is never guessed (the figure is left out)". The block or section that contains the unconverted row has no `total`; the row has no `converted`; an `exchange-rate-missing` warning names the pair and counts the rows. Other blocks and the other section keep their totals. Zero amounts need no rate (core converts them to zero without one), so a sold-out holding or an empty cash account never spoils a total.
- **Shape**: `data = {holdings: [block…], cash: {items, total?}}`. A block is `{investmentAccount?: {uuid, name}, items, total?}`. The aggregate is one block without `investmentAccount`; `--investment-account` gives one block for that account; `--by-account` gives one block per investment account, ordered by name then UUID (all of them, empty ones included, or just the filtered one). Rows are ordered by name (case-insensitive, ordinal) then UUID. There is no paging.
- **Rows**: a holding has `instrument`, `name`, `shares`, `price {date, value}` (the price valuation used, including core's fallback to the last transaction's price; omitted for sold-out holdings), the native `amount` and `currencyCode`, and `converted` (the same shape as in `transactions`, with `rateSource` `identity` when the currency already matches). A cash row has `cashAccount`, `name`, native `amount` and `currencyCode`, and `converted`. A zero amount in a foreign currency has no `converted`: converting nothing looks up no rate and warns about none.
- **The `cash` section ignores `--investment-account`.** It always lists every cash account (retired ones included), at the date, because a cash account can serve several investment accounts. `--by-account` doesn't split it.
- **`--date` defaults to today** from the injected clock. The rate day is the date. `meta` carries `date`, `currency` (the reporting currency), `investmentAccount` when filtered, and `exchangeRates` when the cache was read. `meta.asOf` stays the day the command ran.
- **`--include-zero`** adds instruments that have transactions on or before the date in the scope and a net of zero shares. Instruments first bought after the date never appear.
- **`stale-price`** is judged against the `--date`, using the price the holding was valued with, and appears once per instrument even when it is held in several blocks.
- **Switches**: `--by-account` and `--include-zero` take no value (`--by-account=x` is `invalid-option-value`). `describe` publishes them with `type: "flag"` and `default: false`, and without a `valueName`.
