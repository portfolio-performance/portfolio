# 26: `transaction.add` for cash-account transaction types

Type: task
Status: ready-for-agent
Blocked by: 17, 22

Spec: [spec.md](../spec.md), user stories 66, 82–85, 88 and 94, and "Validation layers" under "Write surface".

## What to build

An agent can record single-sided cash events with `transaction.add`: deposits, removals, interest, fees, fee refunds, taxes, tax refunds and dividends. The input uses the same shape `transactions` returns: date, amount, currency, optional instrument, cash account, units (fees, taxes and forex), note, source and `exDate`. The response returns the new record's `id` and side UUIDs.

Validation, all reported together:

1. Core's non-mutating import checks: transaction date, security-related values, currencies, forex gross value and valid types.
2. CLI rules:
   - Amounts and exchange rates are positive.
   - The instrument is required, refused or optional according to the type, and `shares` are accepted only on share-carrying types.
   - The cash side uses the cash account's currency; an instrument in another currency requires a forex gross-value unit with a rate.
   - `exDate` is allowed only on dividends with an instrument, and never after the payment date.
   - `grossAmount` is a cross-check only and is refused when it doesn't reconcile.

## Acceptance criteria

- [ ] Each supported cash-account type can be added, and reading it back through `transactions` returns the same record
- [ ] Each rule above has a refusal test with the expected `field` path and `code`
- [ ] A forex dividend with a correct gross-value unit is accepted; one without the unit is refused
- [ ] `transaction.add` is declared with its input schema, appears in `describe` and passes its consistency test
