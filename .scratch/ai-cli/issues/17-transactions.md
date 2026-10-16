# 17: `transactions` as joined transaction records

Type: task
Status: ready-for-agent
Blocked by: 16

Spec: [spec.md](../spec.md), user stories 34–41, 51 (the `--from`/`--to` part), 58–59 and 62, and "Transaction records", "Currency rules" and "Periods" under "Read surface".

## What to build

`pp-cli transactions` returns each business event as one **transaction record**:

- A buy or sell joins its investment-account side and its cash-account side.
- A transfer carries its source, target and a `kind`.
- The record `id` is the investment-account side for buys and sells, the outgoing side for transfers, and the transaction itself otherwise.
- Every side's UUID is listed.
- `type` uses core's enum names and is documented as an open enum.

Filters cover account, instrument (through the resolver), date range (`--from`/`--to`, with `--to` defaulting to today from the injected clock) and type. Results are paginated and ordered by date, then UUID. Pagination reuses ticket 15's shared mechanism unchanged: `data = {items, total, nextOffset}`, `meta` echoes `limit` and `offset`, and the same validation and usage error apply (see ticket 15's "Decisions").

Amounts are in the record's native currency. `--currency` adds converted fields that carry the rate and the rate date, using the transaction's own forex rate or its date's ECB rate. A fallback to the built-in ECB data produces a warning. Fees, taxes and forex units use the same shape that later `transaction.*` writes accept.

## Acceptance criteria

- [ ] A buy shows both sides in one record; a transfer is one record with source, target and `kind`, never counted twice
- [ ] The `id` rule and the list of side UUIDs hold for every record type
- [ ] The account, instrument, date-range and type filters work, alone and combined
- [ ] Pagination is ordered by date, then UUID, with `total` and `nextOffset`
- [ ] Converted fields under `--currency` carry the rate and rate date and are cross-checked against core's currency converter
- [ ] A fallback to the built-in ECB data shows up in `warnings`
- [ ] `--to` defaults to the injected clock's date, and the resolved dates are echoed in `meta`
- [ ] `transactions` is declared, appears in `describe` and passes its consistency test
