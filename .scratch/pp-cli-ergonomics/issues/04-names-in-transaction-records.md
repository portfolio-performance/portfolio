# 04: Names in transaction records

Type: task
Status: done

Spec: [spec.md](../spec.md), "Transaction record names".

## What to build

Each transaction record from `pp-cli transactions` adds a flat name field next to each UUID field it carries:

| UUID field | Name field |
|---|---|
| `instrument` | `instrumentName` |
| `investmentAccount` | `investmentAccountName` |
| `cashAccount` | `cashAccountName` |
| `from` | `fromName` |
| `to` | `toName` |

- A name field is present exactly when its UUID field is present. An unnamed entity gives an empty string, matching `MasterData.orEmpty`.
- The UUID fields are left unchanged. This is additive under the contract policy.
- Any write operation that echoes records in its result (such as `apply --dry-run` diffs) uses the same names, if it renders records the same way.

## Acceptance criteria

- [x] A buy shows `instrumentName`, `investmentAccountName` and `cashAccountName`
- [x] A cash transfer and a security transfer show `fromName` and `toName`
- [x] A dividend shows `instrumentName` and `cashAccountName`
- [x] The response schema and help describe the new fields; schema conformance tests pass

## Comments
