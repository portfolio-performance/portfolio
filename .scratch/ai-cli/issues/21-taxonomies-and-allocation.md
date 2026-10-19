# 21: `taxonomies` and `allocation`

Type: task
Status: ready-for-agent
Blocked by: 18

Spec: [spec.md](../spec.md), user stories 54–55.

## What to build

`pp-cli taxonomies` lists taxonomies and their classifications (uuid, name, parent, target weight, direct assignments), so an agent can find the taxonomy the user means and read an instrument's current assignment. `pp-cli allocation --taxonomy <ref> --date <date>` returns flat rows, one per classification at every depth: uuid, name, parent, path, target weight, value and share. The top-level rows and an "unassigned" row add up to 100%. Values are in the base currency or `--currency`, at the as-of date.

## Decisions

Settled in a grilling session. See `GLOSSARY.md` for **Taxonomy**, **Classification**, **Assignment**, **Target weight**, **Allocation** and **Unassigned**.

- **Identifiers**: taxonomies and classifications are named `uuid` in output, like master data, so an agent can pass back what it read to `taxonomy.assign`.
- **`taxonomies` shape**: the shared paging contract (`--limit`, `--offset`, `data = {items, total, nextOffset}`) over taxonomies, in file order (the order the app shows them). A taxonomy item is `{uuid, name, classifications}`. `classifications` is unpaged and flattened, in tree pre-order: siblings by the app's rank, then name (case-insensitive, ordinal), then UUID.
- **Classification fields**: `uuid`, `name`, `parent` (the parent's UUID, omitted at the top level), `path` (ancestor names, top level first, excluding the classification itself), `targetWeight`, `key` and `note` when set, and `assignments`. `color` and `rank` are not emitted.
- **`targetWeight`**: the classification's rebalancing target as an unrounded fraction of its parent, always present (0 when unset). The name avoids confusion with assignment weights.
- **`assignments`**: always present, possibly empty. Only the classification's direct assignments, not its subtree's: `{instrument | cashAccount, name, weight}`, with `weight` the assignment weight as a fraction. Sorted by name, then UUID; retired instruments and accounts are included. Ticket 29 needs this to replace an assignment safely.
- **`--taxonomy <ref>`**: resolved by UUID or exact name (case-insensitive) through the resolver, with `reference-not-found` and `reference-ambiguous` as for the other kinds.
- **Scope**: the client only. There is no `--investment-account` in v1.
- **Rows**: one row per classification at every depth, plus the unassigned row. A row's `value` is its whole subtree's value. Each node's value comes from core's roll-up: `Classification.flattenAssignments()` gives each node's subtree as core's top-level grouping sees it, so the top level equals `ClientSnapshot.groupByTaxonomy` exactly. Classifications with nothing held are emitted with `value` 0 and `share` 0. Rows carry no instruments or positions; an agent combines `taxonomies` assignments with `holdings` for that.
- **Row fields**: `uuid`, `name`, `parent?`, `path`, `targetWeight`, `value` (in the reporting currency at the date) and `share` (an unrounded fraction of `total`). Rows carry no native amounts, since a row mixes currencies.
- **Unassigned row**: always present, always last, with `unassigned: true`, `name: "Unassigned"` (fixed English, not core's localized label), and no `uuid`, `parent` or `targetWeight`. It is top level, so the top-level rows plus it reconcile to `total`.
- **Order**: the same tree pre-order as `taxonomies`, with the unassigned row last.
- **`allocation` shape**: `data = {items, total}`, unpaged; `total` is the client's total assets (cash plus securities) in the reporting currency. `meta` carries `date` (default today from the injected clock), `currency` (base or `--currency`), `taxonomy {uuid, name}`, and `exchangeRates` when the cache was read.
- **Cash**: follows core. Cash accounts with a non-zero balance take part (unassigned unless the user classified them), and the denominator is total assets. Shares are not clamped, so a negative cash balance can make a share negative or above 1.
- **Missing exchange rate**: no total and no share can be trusted, so every row keeps its identity but leaves out `value` and `share`, `total` is left out, and `exchange-rate-missing` is raised. Rates are taken at the as-of date (ADR 0005).
- **Zero total**: when `total` is zero, `share` is left out on every row and `value` stays.
- **Over-assignment**: an instrument or cash account whose assignment weights in the taxonomy add up to more than 100% is counted as core counts it, and an `over-assigned` warning names it and its total assignment weight.
- **Warnings**: `stale-price` is raised as in `holdings` (judged against `--date`, once per instrument), alongside the exchange-rate warnings and `over-assigned`. `describe` lists them.

## Acceptance criteria

- [ ] `taxonomies` lists every taxonomy with its classification tree flattened in pre-order, with `parent`, `path`, `targetWeight` and direct `assignments`
- [ ] `--taxonomy` resolves by UUID or exact name, with `reference-not-found` and `reference-ambiguous`
- [ ] `allocation` has a row for every classification at every depth, empty ones included, plus an always-present, last unassigned row
- [ ] Top-level and unassigned row values equal core's grouping of the client snapshot by taxonomy; deeper rows equal their subtree's value
- [ ] Top-level and unassigned shares sum to 1 up to floating-point error, except with a negative cash balance or an over-assignment
- [ ] A missing exchange rate leaves out every `value`, `share` and the `total`; a zero total leaves out every `share`
- [ ] `over-assigned` and `stale-price` warnings are raised as described
- [ ] Both commands are declared, appear in `describe` and pass its consistency test
