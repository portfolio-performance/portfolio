# 27: `transaction.add` for buys, sells, transfers and deliveries

Type: task
Status: ready-for-agent
Blocked by: 26

Spec: [spec.md](../spec.md), user stories 66, 84–87 and 89.

## What to build

An agent can record buys, sells and transfers as one two-sided transaction record, and deliveries as one-sided records on an investment account. Two-sided records are built through core's entry classes, so both sides are created together.

- **Buys and sells**: an investment-account side and its cash-account side. A forex buy or sell, where the instrument currency differs from the cash account currency, needs a forex gross-value unit with a rate within core's rounding tolerance.
- **Security transfers** between investment accounts.
- **Inbound and outbound deliveries**: shares that enter or leave an investment account with no cash side.
- **Cash transfers** between cash accounts. For a cross-currency transfer, `amount` is in the source currency and `targetAmount` in the target currency. The rate is derived and echoed, and any supplied rate is checked within core's rounding tolerance.
- **Overselling**: a write is refused if it would make any investment account's holding of an instrument negative at the end of any date.

## Decisions

Settled in a grilling session. Field names mirror the `transactions` output.

- **Buy / sell** (`BUY`, `SELL`):
  - `investmentAccount` and `cashAccount` are both required, with no default. Any cash account already in the file is allowed, not only the reference cash account.
  - `instrument` and a positive `shares` are required.
  - `currencyCode` must be the cash account's currency, and both sides use it.
  - An instrument in another currency requires a `GROSS_VALUE` unit with its forex in the instrument's currency and a rate within core's tolerance; otherwise the input is refused (`forex-mismatch` / `forex-gross-value-required`).
  - `FEE` and `TAX` units may carry forex, but only in the instrument's currency.
  - `grossAmount` is a cross-check: for a buy it is `amount − fees − taxes`, for a sell `amount + fees + taxes`.
- **Deliveries** (`DELIVERY_INBOUND`, `DELIVERY_OUTBOUND`):
  - `investmentAccount`, `instrument` and a positive `shares` are required, and `cashAccount` is refused.
  - `currencyCode` can be any ISO currency; if it isn't the instrument's, a `GROSS_VALUE` unit is required as for buys.
  - `FEE` and `TAX` units are allowed.
  - `grossAmount` is a cross-check: inbound works like a buy, outbound like a sell.
- **Transfers**:
  - The input is `"type":"TRANSFER"`, a `kind` of `cash` or `security`, and `from`/`to` as account UUIDs.
  - `from` and `to` must differ, and `investmentAccount` and `cashAccount` are refused.
  - `units` are refused on both kinds (`units-not-allowed`), and so is `grossAmount`.
- **Security transfers**: `instrument`, a positive `amount` and a positive `shares` are required, and `currencyCode` must be the instrument's currency.
- **Cash transfers**:
  - `currencyCode` must be the `from` account's currency.
  - For a same-currency transfer, `targetAmount` and `exchangeRate` are refused (`target-amount-not-allowed`).
  - For a cross-currency transfer, a positive `targetAmount` is required and is in the `to` account's currency.
  - The stored rate is always derived as `amount ÷ targetAmount`, unrounded at 10 decimal places, and echoed as `exchangeRate`. A supplied `exchangeRate` is only checked against core's rounding tolerance.
- **Overselling**:
  - The check covers each pair of investment account and instrument that the operation touches, against the evolving batch. It uses dates only and ignores the time of day.
  - A write is refused if it leaves a pair negative at the end of a date where the holding wasn't already at least that negative before the write. Buys and inbound deliveries are therefore never refused, and an oversell already in the file doesn't block unrelated writes.
  - The failure has field `shares` and code `negative-holding`, and its message names the investment account, the first date that goes negative and the shortfall.
- **Response**:
  - There is one `created` change with entity `transaction`.
  - Its `uuid` is the record's `id`: the investment-account side of a buy or sell, the outgoing side of a transfer, and the only side of a delivery.
  - Its `after` is the rendered record, including `sides`, the same as a `transactions` read-back.

## Acceptance criteria

- [ ] A buy, a sell, a security transfer, and a same-currency and a cross-currency cash transfer can each be added, and read back through `transactions` as one record with both sides
- [ ] An inbound and an outbound delivery can each be added and read back
- [ ] A forex buy with a consistent rate is accepted; an inconsistent one is refused with a field-level error
- [ ] A cross-currency transfer echoes the derived rate; a supplied rate outside tolerance is refused
- [ ] A sell, an outbound delivery, or an outbound transfer that would make any end-of-date holding negative is refused, including when a later buy would restore it
- [ ] An oversell already in the file doesn't block an unrelated write
- [ ] The new types appear in the `transaction.add` input schema in `describe` and pass its consistency test
