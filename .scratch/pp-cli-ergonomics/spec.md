# pp-cli ergonomics: lessons from a real agent session

Status: ready-for-agent

Source: a retro on an agent using the `pp-cli` skill against a real client file (format version 70, base currency ILS, 29 instruments, 331 transaction records, 3 taxonomies) to answer "give me insights". Decisions were made in a grilling session. Discovery: [ADR 0006](../../docs/adr/0006-per-command-help-replaces-describe.md). Vocabulary: [GLOSSARY.md](../../GLOSSARY.md). Builds on [.scratch/ai-cli/spec.md](../ai-cli/spec.md).

## Problem Statement

The agent got the right figures, but with avoidable friction and one misstatement:

- `pp-cli describe` was about 20 KB. The harness truncated it, so the agent never saw response shapes, guessed JSON keys and failed. It also read an allocation `share` as a share of the parent. `describe` documented it correctly as a share of the total, but the agent never got that far.
- `transactions` can't be read newest-first and carries only UUIDs. To show the latest records, the agent fetched all 331, sorted them itself and made a second call to map UUIDs to names.
- Core's "Loaded … with [XML]" log line goes to stderr. Harnesses that merge stderr into stdout then break JSON parsing.
- Holdings of Israeli instruments carry `currencyCode: "ILA"` with amounts in agorot. Quoting `amount` instead of `converted` is off by 100×.
- The skill has no recipe for the common "insights" request, and none of its traps are written down: annualised IRR compared with cumulative TTWROR, allocation rows that are cumulative across nesting, and an IRR of about 8 million on a briefly held, sold position.
- The agent left CLI output with financial data in `/tmp`.

## Solution

- **Help replaces `describe`** ([ADR 0006](../../docs/adr/0006-per-command-help-replaces-describe.md)). `pp-cli --help` and bare `pp-cli` list the commands, the global options, the error codes and the limits. `pp-cli <command> --help` shows usage, options with their defaults and allowed values, response `data` fields with their descriptions, and the warning and error codes the command can return. `pp-cli apply --help <operation>` shows one write operation's input schema. Help is plain text, generated from the existing specs, with no third-party parser.
- **Quiet by default**: core INFO/status logging is off unless `--verbose` is given. Real errors still reach stderr.
- **Sort and order on paged lists**: `--sort <field>` and `--order asc|desc`, with ties always broken by id.
- **Last modified**: `updatedAt` is added to master data and transaction records, and is sortable.
- **Names in transaction records**: flat `…Name` fields sit next to each UUID field.
- **Minor currency units** are explained in the response field descriptions.
- **Skill**: discovery moves to `--help`, an `insights.md` recipe is added, and a scratch-file hygiene rule is added.

## Contract policy

Additive changes are made freely: new optional fields, and new options whose defaults keep current behaviour. Renaming or removing a field or a command needs an ADR.

## Decisions

### Help

- Text, not JSON. One command's help must fit comfortably in an agent's context.
- Global help covers the errors catalogue and `limits`. Each command's help lists only the codes that command can produce.
- `apply --help` lists the operations with one-line summaries. `apply --help <operation>` gives the full input schema.
- `describe` is removed.

### Sorting

| Command | `--sort` fields | Default |
|---|---|---|
| `instruments`, `investment-accounts`, `cash-accounts` | `name`, `updatedAt` | `name` |
| `transactions` | `date`, `updatedAt` | `date` |
| `prices` | `date` | `date` |
| `taxonomies` | `position`, `name` | `position` |

- `--order` defaults to `asc`, so default output doesn't change.
- `taxonomies` defaults to `position`, the order of the file and the app, because a `name` default would change today's output.
- `--sort date` on `transactions` orders by date-time, as today.
- Unpaged commands (`holdings`, `performance --by instrument`) get no sort options. The agent sees the whole list anyway.
- Records with no `updatedAt` sort before all others in `asc`. This default was chosen while writing the spec; confirm it during implementation.

### Last modified

- `updatedAt` is an ISO-8601 UTC instant, such as `2026-10-24T15:04:57Z`. It is left out when core has none.
- On a transaction record, it is the latest `updatedAt` across the record's sides.

### Transaction record names

Each record adds `instrumentName`, `investmentAccountName`, `cashAccountName`, `fromName` and `toName` wherever the matching UUID field is present. The UUID fields stay as they are.

### Figures left unchanged

- IRR on closed or briefly held positions is mathematically valid, so the CLI emits it unchanged. The insights recipe tells agents to flag it rather than quote it.
- `allocation` stays one taxonomy per call.

## Out of scope

- picocli, shell completion and "did you mean" suggestions.
- Sorting unpaged lists, and `--sort` by value, IRR or TTWROR.
- An `allocation` call that covers all taxonomies, and a CLI `report` command.

## Issues

1. [01-per-command-help-replaces-describe](issues/01-per-command-help-replaces-describe.md)
2. [02-quiet-core-logging](issues/02-quiet-core-logging.md)
3. [03-sort-and-order-on-lists](issues/03-sort-and-order-on-lists.md)
4. [04-names-in-transaction-records](issues/04-names-in-transaction-records.md)
5. [05-minor-currency-unit-descriptions](issues/05-minor-currency-unit-descriptions.md)
6. [06-skill-update](issues/06-skill-update.md)
