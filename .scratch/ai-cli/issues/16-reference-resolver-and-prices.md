# 16: Reference resolver and `prices --instrument`

Type: task
Status: ready-for-agent
Blocked by: 15

Spec: [spec.md](../spec.md), user stories 32–33, 47 and 58, and "Reference resolution" under "Read surface".

## What to build

An agent can refer to an entity on reads by UUID (canonical) or, as a convenience, by ISIN, ticker or exact name. The resolver never guesses. An ambiguous match fails with an error that lists the candidates (UUID and name), and an unknown reference fails with a not-found error. Name, ISIN and ticker lookups are for reads only; writes accept UUIDs alone.

The first consumer is `pp-cli prices --instrument <ref>`. It returns the instrument's price history with optional `--from`/`--to`, plus the latest price and its date. A stale latest price produces a warning.

## Acceptance criteria

- [ ] `prices --instrument` resolves by UUID, ISIN, ticker and exact name
- [ ] An ambiguous reference gives a structured error listing every candidate; an unknown reference gives a structured not-found error
- [ ] The date range filters the history; the latest price and its date are always included
- [ ] A stale latest price produces a warning in `warnings`
- [ ] Prices are decimals and dates are ISO 8601
- [ ] `prices` is declared, appears in `describe` and passes its consistency test
