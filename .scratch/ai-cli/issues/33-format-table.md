# 33: `--format table` for human reading

Type: task
Status: ready-for-agent
Blocked by: 15

Spec: [spec.md](../spec.md), user story 64 (nice-to-have).

## What to build

The owner can read list output in a terminal. `--format table` renders a command's `data` rows as an aligned text table on stdout, with warnings still on stderr. JSON stays the default and the only format for agents. This ticket covers the master-data lists; each later list command can opt in through its declaration.

## Decisions

- `--format` is declared on every command, defaulting to `json`. `table` is accepted only by a command that declares table columns; elsewhere it is an `invalid-option-value` usage error. `describe` publishes each command's allowed values.
- Errors stay problem+json on stdout in either format.
- Columns: `instruments` UUID, Name, Currency, ISIN, WKN, Ticker, Retired; `investment-accounts` UUID, Name, Reference cash account, Retired; `cash-accounts` UUID, Name, Currency, Retired. Notes and attributes are left to JSON.
- Stdout holds only the table. Warnings go to stderr as `warning: <code>: <detail>`, and a paging footer goes to stderr when the page isn't the whole list. The rest of `meta` is dropped.
- The table is built from the rendered JSON items; a command opts in by declaring columns as a JSON key plus a header.
- Rendering uses `com.github.freva:ascii-table` (MIT), added to the target definitions like Gson. The style is a header, one `-` separator and no outer border. UUIDs are never truncated; Name is capped at 40 characters with `…`. There is no terminal-width detection, and wide characters (CJK, emoji) may misalign, because the library counts UTF-16 characters.
- An absent field is an empty cell, `retired` shows as `yes` or empty, and newlines or tabs in a value become a space. An empty list prints the header and separator only.
- No ADR: the dependency is small, CLI-only and easy to swap.

## Acceptance criteria

- [ ] `instruments`, `investment-accounts` and `cash-accounts` render as readable tables with `--format table`
- [ ] Without `--format`, or with `--format json`, output is unchanged
- [ ] A command that doesn't support tables gives a usage error for `--format table`
- [ ] The `--format` option is declared, appears in `describe` and passes its consistency test
