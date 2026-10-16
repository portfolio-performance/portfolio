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

## Acceptance criteria

- [ ] Each scope's figures match core's performance APIs for the same fixture and period
- [ ] The investment-account scope works with and without its reference cash account
- [ ] `--by instrument` records match core's per-security performance snapshot
- [ ] A quiet instrument in scope produces `no-activity-in-period` in `warnings`, not an error
- [ ] The new options are declared, appear in `describe` and pass its consistency test
