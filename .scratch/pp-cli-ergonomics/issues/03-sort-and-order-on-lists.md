# 03: `--sort` and `--order` on paged lists, and `updatedAt`

Type: task
Status: done

Spec: [spec.md](../spec.md), "Sorting" and "Last modified". Glossary: **Last modified**, **Transaction date**.

## What to build

Add the **Last modified** time to the output:

- `updatedAt` on `instruments`, `investment-accounts`, `cash-accounts` and `transactions` items, as an ISO-8601 UTC instant. It is left out when core has none.
- On a transaction record, use the latest `updatedAt` across its sides.

Add `--sort <field>` and `--order asc|desc` to the paged list commands. Each command declares its own sort fields, so help lists them:

| Command | `--sort` fields | Default |
|---|---|---|
| `instruments`, `investment-accounts`, `cash-accounts` | `name`, `updatedAt` | `name` |
| `transactions` | `date`, `updatedAt` | `date` |
| `prices` | `date` | `date` |
| `taxonomies` | `position`, `name` | `position` |

- `taxonomies` defaults to `position` (file order, as the app shows them) instead of `name`: a `name` default would change today's output.
- Ties are broken by id, so paging stays stable. `--order desc` reverses the whole comparator, tie-breaker included.
- Items with no `updatedAt` sort first in `asc`.
- An unknown sort field or order value gives `invalid-option-value`, with the allowed values listed.
- `meta` echoes the resolved `sort` and `order`.
- The defaults reproduce today's output exactly.
- Build this once as a shared mechanism next to `Paging`/`MasterDataList`, not separately in each command.

## Acceptance criteria

- [x] `transactions --order desc --limit 10` returns the 10 latest records by transaction date
- [x] `transactions --sort updatedAt --order desc` returns the most recently modified records first; a buy's `updatedAt` is the later of its two sides
- [x] Master data lists sort by `name` and `updatedAt` in both orders
- [x] Default calls produce byte-identical `data` to before (existing tests unchanged)
- [x] Paging across pages is stable under every sort and order
- [x] Response schemas and help document `updatedAt`, `--sort`, `--order` and each command's natural order

## Comments
