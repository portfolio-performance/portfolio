# 29: `taxonomy.assign`

Type: task
Status: ready-for-agent
Blocked by: 21, 22

Spec: [spec.md](../spec.md), user stories 74–75 and 93.

## What to build

An agent can classify an instrument. `taxonomy.assign` replaces the instrument's complete assignment within one existing taxonomy with a list of classification UUIDs and weights. An empty list removes the instrument from that taxonomy. Weights must be positive and at most 100% in total, classifications must not repeat, and only leaf classifications may be used. Creating taxonomies or classifications, and assigning cash accounts, are out of scope.

## Acceptance criteria

- [ ] Assigning, reassigning and removing (with an empty list) works, and `allocation` reflects the change
- [ ] Non-positive weights, repeated classifications, totals over 100% and non-leaf classifications are each refused with field-level errors
- [ ] An unknown taxonomy, classification or instrument UUID is refused
- [ ] `taxonomy.assign` is declared with its input schema, appears in `describe` and passes its consistency test
