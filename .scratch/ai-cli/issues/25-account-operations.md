# 25: Cash account and investment account operations

Type: task
Status: ready-for-agent
Blocked by: 22

Spec: [spec.md](../spec.md), user stories 72–73, 91–92.

## What to build

An agent can model new bank accounts and depots through `apply`:

- `cash-account.add` and `cash-account.update`: name, currency, note, retired.
- `investment-account.add` and `investment-account.update`: name, reference cash account, note, retired.
- Input field names match the read output of ticket 15: `name`, `currencyCode`, `note`, `retired` for cash accounts; `name`, `referenceCashAccount`, `note`, `retired` for investment accounts.

Rules:

- Accounts require a name and a currency.
- An investment account requires a reference cash account that existed before the batch, given by UUID.
- A cash account's currency is immutable once it has transactions.

## Acceptance criteria

- [ ] Each of the four operations works, and the reloaded client file matches the reported change records
- [ ] A missing name or currency, and an unknown or in-batch reference cash account, are refused with field-level errors
- [ ] A currency change on a cash account with transactions is refused
- [ ] The operations are declared with input schemas, appear in `describe` and pass its consistency test
