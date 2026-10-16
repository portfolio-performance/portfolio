# 27: `transaction.add` for buys, sells and transfers

Type: task
Status: ready-for-agent
Blocked by: 26

Spec: [spec.md](../spec.md), user stories 66, 84–87 and 89.

## What to build

An agent can record two-sided business events as one transaction record. Construction goes through core's entry classes, so both sides are created together.

- **Buys and sells**: an investment-account side and its cash-account side. A forex buy or sell, where the instrument currency differs from the cash account currency, needs a forex gross-value unit with a rate within core's rounding tolerance.
- **Security transfers** between investment accounts, including inbound and outbound deliveries.
- **Cash transfers** between cash accounts. For a cross-currency transfer, `amount` is in the source currency and `targetAmount` in the target currency. The rate is derived and echoed, and any supplied rate is checked within core's rounding tolerance.
- **Overselling**: a write is refused if it would make any investment account's holding of an instrument negative at the end of any date.

## Acceptance criteria

- [ ] A buy, a sell, a security transfer and a same-currency and cross-currency cash transfer can each be added, and read back through `transactions` as one record with both sides
- [ ] A forex buy with a consistent rate is accepted; an inconsistent one is refused with a field-level error
- [ ] A cross-currency transfer echoes the derived rate; a supplied rate outside tolerance is refused
- [ ] A sell, or an outbound transfer, that would make any end-of-date holding negative is refused, including when a later buy would restore it
- [ ] The new types appear in the `transaction.add` input schema in `describe` and pass its consistency test
