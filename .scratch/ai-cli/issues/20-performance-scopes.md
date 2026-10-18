# 20: `performance` at account and instrument scope, and `--by instrument`

Type: task
Status: ready-for-agent
Blocked by: 19

Spec: [spec.md](../spec.md), user stories 48–50.

## What to build

`performance` also works at a narrower scope:

- one investment account, optionally together with its reference cash account;
- one cash account;
- one instrument.

Each scope is selected through the resolver. `performance --by instrument` returns one record per instrument that was active in the period, with IRR, TTWROR, absolute gain, dividends, fees, taxes, opening value and closing value, so an agent can rank performers. An instrument with no activity in the period produces a `no-activity-in-period` warning instead of failing the query.

## Decisions

- **Scope options**: `--investment-account <ref>`, `--cash-account <ref>` or `--instrument <ref>`, resolved like in `transactions`. At most one is allowed (`conflicting-options` usage error otherwise). With none, the scope is the client.
- **Reference cash account**: the flag `--with-reference-cash-account` defaults to off and is valid only with `--investment-account` (usage error otherwise). If the investment account has no reference cash account, the command fails with `no-reference-cash-account` (422), carrying the account's `uuid`.
- **Figures per scope**: every scope returns ticket 19's figure set and schema, taken from that scope's core performance index and snapshot: `forPortfolio`, `forPortfolioPlusAccount`, `forAccount` or `forInvestment`. The defaults stay the same: base currency or `--currency`, and FIFO or `--cost-method`.
- **`--by <dimension>`**: `instrument` is the only value in v1; any other value is `invalid-option-value`. It works with client scope and investment-account scope (with or without the reference cash account). For the per-instrument records, the client is narrowed to the scope first and core's per-security performance snapshot then runs on it. With `--cash-account` or `--instrument` it is a usage error.
- **Active in the period** (see `GLOSSARY.md`): an instrument is active exactly when core's per-security snapshot has a record for it.
- **`--by instrument` data**: `{currencyCode, items}`, unpaged, with no scope total, sorted by name (case-insensitive, ordinal) then UUID. Each item has:
  - identity: `uuid`, `name`, `retired`, plus `isin` and `tickerSymbol` when set;
  - `irr` (the record's IRR) and `ttwror` (not annualized);
  - `initialValue` (start valuation) and `finalValue` (market value at the end);
  - `dividends`, `fees`, `taxes`;
  - `absoluteGain`: core's delta, which ignores the cost method;
  - `realizedCapitalGains` and `unrealizedCapitalGains` by the cost method.

  The field names follow ticket 19 (`initialValue`/`finalValue`, not opening/closing). The schema notes that a record's `irr` can differ slightly from `irr` at `--instrument` scope, because the two come from different core calculations.
- **Quiet instruments**: `no-activity-in-period` is raised only at `--instrument` scope. `data` then keeps `currencyCode` but leaves out every figure, and the warning carries the instrument's `uuid` and `name`. With `--by instrument`, quiet instruments are simply absent and no warning is raised.
- **Missing exchange rates with `--by`**: each record gets its own strict converter. A record that needs a missing rate keeps its identity fields and loses its figures, while the other records are still reported. `exchange-rate-missing` is raised as today.
- **`meta.scope`** is always present: `{kind: client|investment-account|cash-account|instrument, uuid?, name?, referenceCashAccount?: {uuid, name}}`.

## Acceptance criteria

- [ ] Each scope's figures match core's performance APIs for the same fixture and period
- [ ] The investment-account scope works with and without its reference cash account, and asking for a missing reference cash account fails with `no-reference-cash-account`
- [ ] Combining scope options, `--with-reference-cash-account` without `--investment-account`, and `--by` with `--cash-account` or `--instrument` are usage errors
- [ ] `--by instrument` records match core's per-security performance snapshot, both for the client and for an investment account
- [ ] A quiet instrument at `--instrument` scope produces `no-activity-in-period` in `warnings`, not an error, and its figures are left out; quiet instruments are absent from `--by instrument`
- [ ] A record that needs a missing exchange rate loses only its own figures
- [ ] `meta.scope` echoes the resolved scope
- [ ] The new options are declared, appear in `describe` and pass its consistency test
