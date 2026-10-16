# 21: `taxonomies` and `allocation`

Type: task
Status: ready-for-agent
Blocked by: 18

Spec: [spec.md](../spec.md), user stories 54–55.

## What to build

`pp-cli taxonomies` lists taxonomies and their classifications (id, name, parent, weight), so an agent can find the taxonomy the user means. `pp-cli allocation --taxonomy <ref> --date <date>` returns flat rows: classification id, name, parent path, weight, value and share. The rows add up to 100%, including an "unassigned" row. Values are in the base currency or `--currency`, at the as-of date.

## Acceptance criteria

- [ ] `taxonomies` lists every taxonomy with its classification tree flattened, with parent references
- [ ] `allocation` values match core's grouping of the client snapshot by taxonomy
- [ ] Shares sum to 100%, including the "unassigned" row
- [ ] Both commands are declared, appear in `describe` and pass its consistency test
