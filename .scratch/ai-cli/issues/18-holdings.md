# 18: `holdings` at a date

Type: task
Status: ready-for-agent
Blocked by: 17

Spec: [spec.md](../spec.md), user stories 42–46, 57 and 58.

## What to build

`pp-cli holdings --date <date>` answers "what do I own and what is it worth". Holdings are aggregated across investment accounts by default. `--investment-account <ref>` filters to one account and `--by-account` splits the output into one block per account. Zero-share holdings are hidden unless `--include-zero` is given.

A separate `cash` section lists cash account balances. Totals are in the base currency, or in `--currency`, converted at the as-of date with the rate and rate date shown. Each row also gives its native amount. A stale price produces a warning. `meta` carries the as-of date and the reporting currency.

## Acceptance criteria

- [ ] Share counts and values match core's client snapshot for the same fixture and date
- [ ] `--investment-account`, `--by-account` and `--include-zero` behave as described
- [ ] The `cash` section lists cash account balances at the date
- [ ] Totals are in the base currency or `--currency`, rows carry native amounts, and converted amounts carry the rate and rate date
- [ ] Stale prices and stale, missing or unavailable exchange rates produce warnings, using ticket 17's rate rules (ADR 0005); this ticket decides how a total behaves when one of its parts has no rate
- [ ] `holdings` is declared, appears in `describe` and passes its consistency test
