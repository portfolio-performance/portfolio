# 19: `performance` for the client, with periods and cost method

Type: task
Status: ready-for-agent
Blocked by: 17

Spec: [spec.md](../spec.md), user stories 48 (client scope), 51–53 and 57, and "Periods" and "Cost method".

## What to build

`pp-cli performance` reports the whole client's IRR, TTWROR and gains for a period. The period is either `--from`/`--to` (with `--to` defaulting to today from the injected clock) or a `--period` reporting-period code such as `L1Y0`, `YTD` or `Y2025`, parsed by core. Passing both is a usage error. The resolved dates are always echoed in `meta`.

`--cost-method fifo|moving-average` defaults to FIFO and is always echoed in `meta`. Figures are in the base currency or `--currency`, as the engine computes them.

## Acceptance criteria

- [ ] IRR, TTWROR and absolute gain match core's performance index for the same fixture and period
- [ ] `--period` codes resolve through core, and the resolved dates are echoed
- [ ] `--period` together with `--from` or `--to` is a usage error
- [ ] `--cost-method` changes the gain figures as core does, and the method is always echoed in `meta`
- [ ] `performance` is declared, appears in `describe` and passes its consistency test
