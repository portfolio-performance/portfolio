# 05: Explain minor currency units in field descriptions

Type: task
Status: done

Spec: [spec.md](../spec.md), "Solution". Glossary: **Minor currency unit**.

## What to build

An agent quoted holdings in ILA (agorot) as if they were in ILS. Core models ILA as its own currency with a fixed 0.01 rate to ILS, so the figures are correct. Only the documentation is missing.

Update the `Schema` descriptions of every `currencyCode` and `amount` that can be in an instrument's currency (`holdings` rows, `prices`, `transactions` units, performance by instrument, …). They should say:

- the currency may be a minor currency unit, such as ILA (1/100 ILS) or GBX (1/100 GBP);
- the figure in the reporting currency is in `converted` (or wherever that command puts it).

The wording is generated into help (issue 01) and is the single source of truth. The skill doesn't repeat it.

## Acceptance criteria

- [x] Every affected `currencyCode`/`amount` description mentions minor units and names the converted field
- [x] No behaviour change; existing tests pass

## Comments
