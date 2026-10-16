# 15: Master-data lists with pagination

Type: task
Status: ready-for-agent
Blocked by: 13

Spec: [spec.md](../spec.md), user stories 28–31 and 62–63.

## What to build

An agent can list what's in a client file:

- `pp-cli instruments`: UUID, name, currency, ISIN, ticker, WKN, retired flag and user-defined attributes.
- `pp-cli investment-accounts`: includes the reference cash account.
- `pp-cli cash-accounts`: currency and retired flag.

UUIDs are canonical in all output. List commands share pagination: `--limit` (default 200) and `--offset`, returning `total` and `nextOffset`, with stable ordering. Field names are camelCase and match the upstream #5870 names where an equivalent exists.

## Acceptance criteria

- [ ] All three commands return the documented fields for a fixture built with the shared JUnit builders
- [ ] `--limit` and `--offset` page through results, and `total` and `nextOffset` are correct, with `nextOffset` absent or null on the last page
- [ ] Ordering is stable across calls
- [ ] All three commands are declared, appear in `describe` and pass its consistency test
