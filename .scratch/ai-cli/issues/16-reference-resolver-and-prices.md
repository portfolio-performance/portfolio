# 16: Reference resolver and `prices --instrument`

Type: task
Status: ready-for-agent
Blocked by: 15

Spec: [spec.md](../spec.md), user stories 32–33, 47 and 58, and "Reference resolution" under "Read surface".

## What to build

An agent can refer to an entity on reads by UUID (canonical) or, as a convenience, by ISIN, ticker or exact name. The resolver never guesses. An ambiguous match fails with an error that lists the candidates (UUID and name), and an unknown reference fails with a not-found error. Name, ISIN and ticker lookups are for reads only; writes accept UUIDs alone.

The first consumer is `pp-cli prices --instrument <ref>`. It returns the instrument's price history with optional `--from`/`--to`, plus the latest price and its date. A stale latest price produces a warning.

## Acceptance criteria

- [ ] The resolver resolves instruments by UUID, ISIN, ticker and exact name, and investment and cash accounts by UUID and name, each kind searching only its own entities
- [ ] `prices --instrument` resolves through it
- [ ] An ambiguous reference gives `reference-ambiguous` listing every candidate; an unknown reference gives `reference-not-found`
- [ ] A missing `--instrument` gives `option-required`; malformed dates give `invalid-option-value`; `--from` after `--to` gives `invalid-date-range`
- [ ] The date range filters the history, which is paged with ticket 15's mechanism; `latest` is included whenever a price exists on or before the as-of date
- [ ] A stale latest price produces a `stale-price` warning in `warnings`; retired and manual-feed instruments don't
- [ ] Prices are decimals and dates are ISO 8601
- [ ] `prices` is declared, appears in `describe` (including the required flag, the date option type and the warning codes) and passes its consistency test

## Decisions

Settled with the owner in a design session (2026-10-16). Implement them as written; don't re-ask.

### Resolver scope

- One resolver for all three **master data** kinds. Instruments match by UUID, ISIN, ticker or name. Investment and cash accounts match by UUID or name (they have no ISIN or ticker).
- Instruments cover every security in the client, including indices and exchange-rate securities, the same set `instruments` lists.
- Each option searches only its own kind: `--instrument <cash-account-uuid>` is not found.
- `prices` is the only consumer in this ticket. Ticket 17 reuses the resolver for its account and instrument filters. Taxonomy references (ticket 21) are out of scope here.
- There's no UUID-only write mode. Writes (ticket 22) look up UUIDs directly. `describe` states that name, ISIN and ticker references are accepted on reads only.

### Matching

1. An exact UUID match wins immediately.
2. Otherwise ISIN, ticker and name matches are pooled, with no precedence among the fields. An entity that matches on several fields counts once. One entity resolves; more than one is ambiguous; none is not found. Example: instrument A has ticker `SAP`, instrument B is named `SAP`, so `SAP` is ambiguous.
- Every comparison (UUID, ISIN, ticker, name) is case-insensitive and ordinal, the same comparison as the list ordering (`String.CASE_INSENSITIVE_ORDER`/`equalsIgnoreCase`, not a locale `Collator`). Two names that differ only by case become an ambiguity error, never a guess.
- Stored values are compared as stored, with no trimming. A null or blank stored ISIN, ticker or name never matches.
- A blank reference value is the existing `invalid-option-value` usage error.
- Retired entities take part in matching. They get no preference either way: an active and a retired instrument with the same ISIN are ambiguous.

### Errors

| Case | `code` | status | exit |
|---|---|---|---|
| No match | `reference-not-found` | 404 | 1 |
| Several matches | `reference-ambiguous` | 409 | 1 |
| `--instrument` missing | `option-required` | 400 | 2 (usage) |
| Malformed date | `invalid-option-value` | 400 | 2 (usage) |
| `--from` after `--to` | `invalid-date-range` | 400 | 2 (usage) |

- Neither reference error is a usage error: the command line is well formed, and the failure depends on the file's contents.
- `reference-not-found` and `reference-ambiguous` carry `option` (e.g. `instrument`) and `value` extensions, like `invalid-option-value`.
- `reference-ambiguous` also carries `candidates`: every match, uncapped, ordered by name (case-insensitive, ordinal) then UUID. Each candidate has:
  - all kinds: `uuid`, `name`, `retired`, and `matchedBy`, an array of the fields that matched, named as in the list output: `isin`, `tickerSymbol`, `name`;
  - instruments: `isin`, `tickerSymbol` and `currencyCode`, when set;
  - cash accounts: `currencyCode`.
  - Null and blank fields follow ticket 15's field rules (omitted; `name` always present).
- `option-required` carries an `option` extension. `invalid-date-range` carries `from` and `to`.

### Options

- `OptionSpec` gains a `required` flag. `describe` publishes it as `required: true`, and the parser (or the command) raises `option-required` when the option is missing. `--instrument` is required.
- `OptionSpec` gains a date type, which ticket 17 reuses:
  - Values are ISO `yyyy-MM-dd`, and `describe` publishes `type: "date"`.
  - Both bounds are inclusive.
  - An absent `--from` means no lower bound. `--to` defaults to today from the injected clock.
- `prices` takes `--instrument`, `--from`, `--to`, `--limit` and `--offset`. There's no `--period`.

### Latest price

- `latest` is core's `Security.getSecurityPrice(asOf)`, with `asOf` being today from the injected clock. That is the price valuation uses: the feed's latest price when it is current, otherwise the last stored price.
- It doesn't depend on `--from`/`--to`.
- `latest` is omitted when no price is dated on or before `asOf`. Core then returns a zero placeholder or a future-dated price, and neither is a real latest price.

### History

- Source: `Security.getPricesIncludingLatest()`, so the history ends with the same price `latest` reports.
- It is filtered by the inclusive date range and ordered by date ascending (dates are unique per instrument).
- Paging reuses ticket 15's mechanism unchanged: `--limit` 1–1000 (default 200), `--offset` ≥ 0, `total`, and `nextOffset` omitted on the last page.

### Response shape

```json
{
  "data": {
    "instrument": { "uuid": "…", "name": "…", "currencyCode": "EUR" },
    "latest": { "date": "2026-10-15", "value": 123.45000000 },
    "items": [ { "date": "2026-01-02", "value": 120.10000000 } ],
    "total": 1234,
    "nextOffset": 200
  },
  "meta": { "fileVersion": 66, "asOf": "2026-10-16", "limit": 200, "offset": 0, "from": "2026-01-01", "to": "2026-10-16" },
  "warnings": []
}
```

- `instrument` echoes what the reference resolved to. `currencyCode` is omitted when blank (indices).
- `latest` is omitted as described above.
- `meta` is the paged meta plus `to` (always) and `from` (only when given).
- `value` is a decimal: the stored long ÷ 10^`Values.Quote` precision. Extract `AttributeCodec`'s private plain-decimal encoding into a shared helper and use it for both, so trailing zeros are kept as in attribute output today (`0.00000001`, never `1E-8`).

### Stale price

- The latest price is stale when its date is more than 7 calendar days before `asOf` (`date.isBefore(asOf.minusDays(7))`), unless:
  - the instrument is retired, or
  - its feed is manual (`QuoteFeed.MANUAL`), where the feed is `getLatestFeed()`, falling back to `getFeed()` when that is null.
- This mirrors the app's warning colour in `SecuritiesTable`. The 7 days are fixed, with no option.
- An instrument with no latest price gets no stale warning.
- Put the rule in one shared helper; ticket 18 reuses it.

### Warnings

- This ticket introduces the warning shape every later warning uses: `{code, detail, …extensions}`, with the same `code`/`detail` keys as errors.
- Stale: `{"code": "stale-price", "detail": "…", "instrument": "<uuid>", "date": "2026-09-30", "ageDays": 16}`. `ageDays` is the number of calendar days from the price date to `asOf`.
- Warnings go in the envelope only, not on stderr. User story 60's "warnings on stderr" means core log output.
- Each `CommandSpec` declares the warning codes it can emit, and `describe` publishes them per command as `warnings: [{code, description}]` (empty for commands that emit none). The consistency test covers them. Tickets 17–20 add their codes the same way.

### Tests

- Test seam: the in-process `PpCli.execute`, with fixtures from the shared JUnit builders.
- Cover each matching rule above, including UUID-beats-name, cross-field ambiguity, case-insensitivity, blank stored fields, retired candidates, and account resolution by name.
- `prices` needs a case in `DescribeConsistencyTest.sampleArguments` and a declaration in `Commands.ALL`.
