# Insights

The recipe for an overview of the client file, or for "give me insights". Pass `--file "<path>"` on every call and read each response as the Answers rules in the skill say. Project each response to the fields named below (see Shell in the skill).

## Calls

Run them in this order. Done when every call has returned and each `warnings` entry is accounted for.

1. `pp-cli info`: file, base currency, `pinnedUpstreamTag`.
2. `pp-cli holdings`: what is held today. Keep per instrument `name`, `shares`, `price`, `currencyCode`, `converted.amount`; per cash account `name` and `converted.amount`.
3. `pp-cli taxonomies`, then `pp-cli allocation --taxonomy "<uuid>"` once per taxonomy listed. `taxonomies` returns every classification with its assignments, which is large: keep only `uuid` and `name` of each item. From `allocation`, keep per item `name`, `path`, `value`, `share`.
4. `pp-cli performance` three times: `--period L1Y0` (1Y), `--period YTD`, and all-time with `--from` set to the date of the oldest record from `pp-cli transactions --sort date --order asc --limit 1`.
5. `pp-cli performance --by instrument` for the same periods you quote, to see what drove the figures. Keep per item `name`, `irr`, `ttwror`, `finalValue`, `absoluteGain`.
6. `pp-cli transactions --order desc --limit 10` for the latest records. Each record carries the names next to the UUIDs; `--limit` takes more.
7. Derive what no command returns, from the `holdings` response: the split by `currencyCode`, and the share of the total held by the largest three positions. Total is the sum of `converted.amount` over instruments and cash. A zero total (an empty client, or positions netting to zero) leaves the split and shares undefined: report them as unavailable.

## Traps

- **Cumulative against annualised**: all-time TTWROR is cumulative; IRR is annualised. State which is which and compare neither against the other.
- **Allocation nesting**: a row's `value` is cumulative over its children. The total is the sum of the top-level rows plus the unassigned row; adding children double-counts.
- **Share**: `share` is a share of the client total, not of the parent classification. A share of the parent is the row's `value` divided by its parent's `value`.
- **Closed or briefly held positions**: an IRR that is extreme (millions of percent) or a TTWROR of -100% is mathematically valid and still misleading. Flag the position and leave the figure unquoted.
- **Minor currency units**: for an instrument whose `currencyCode` is `ILA` or `GBX`, `amount` is in the minor unit, 1/100 of the main currency. Quote `converted`, which is in the reporting currency: the base currency, or `--currency` if given.
- **Stale file**: compare the date of the newest record from step 6 with `meta.asOf`. A gap of weeks or more means activity since then is unrecorded, so report the gap next to every figure it could distort.
