# 29: `taxonomy.assign`

Type: task
Status: ready-for-agent
Blocked by: 21, 22

Spec: [spec.md](../spec.md), user stories 74–75 and 93.

## What to build

An agent can classify an instrument. `taxonomy.assign` replaces the instrument's complete assignment within one existing taxonomy with a list of classification UUIDs and weights. An empty list removes the instrument from that taxonomy. Weights must be positive and at most 100% in total, classifications must not repeat, and only leaf classifications may be used. Creating taxonomies or classifications, and assigning cash accounts, are out of scope.

An agent reads the instrument's current assignment from `taxonomies` (ticket 21), where each classification lists its direct `assignments` with `weight` as a fraction. Input uses the same field names and units, so an agent can send back what it read. A leaf classification is one that no other classification names as its `parent`.

## Acceptance criteria

- [x] Assigning, reassigning and removing (with an empty list) works, and `allocation` reflects the change
- [x] Non-positive weights, repeated classifications, totals over 100% and non-leaf classifications are each refused with field-level errors
- [x] An unknown taxonomy, classification or instrument UUID is refused
- [x] `taxonomy.assign` is declared with its input schema, appears in `describe` and passes its consistency test
