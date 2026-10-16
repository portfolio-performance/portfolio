# 33: `--format table` for human reading

Type: task
Status: ready-for-agent
Blocked by: 15

Spec: [spec.md](../spec.md), user story 64 (nice-to-have).

## What to build

The owner can read list output in a terminal. `--format table` renders a command's `data` rows as an aligned text table on stdout, with warnings still on stderr. JSON stays the default and the only format for agents. This ticket covers the master-data lists; each later list command can opt in through its declaration.

## Acceptance criteria

- [ ] `instruments`, `investment-accounts` and `cash-accounts` render as readable tables with `--format table`
- [ ] Without `--format`, or with `--format json`, output is unchanged
- [ ] A command that doesn't support tables gives a usage error for `--format table`
- [ ] The `--format` option is declared, appears in `describe` and passes its consistency test
