# List output deliberately deviates from the upstream REST API in two places

`pp-cli` aligns its field names with the upstream REST API proposal (buchen/portfolio#5870), so that what an agent knows from upstream carries over. List output deviates in two places on purpose. **Instrument attributes** are an always-present array of `{id, name, type, value}`, not upstream's map keyed by attribute type id, which upstream omits when it's empty. User-created attribute type ids are random UUIDs and the CLI has no `attribute-types` command, so an agent can only read the id-keyed map if the name and type are given inline. **Paging fields** `total` and `nextOffset` live inside `data` next to `items` (`data = {items, total, nextOffset}`). Upstream's `{items}` wrapper has no paging, because upstream returns whole lists. The CLI pages every list so that large client files don't flood an agent's context.

**Considered Options**

- *Copy upstream's id-keyed attribute map*: rejected. An agent can't interpret a key like `5c1f…` without a separate lookup that doesn't exist.
- *Id-keyed map plus a separate `attribute-types` command*: rejected. An extra round trip on every instrument read, for no gain over inlining.
- *Paging fields in `meta`*: rejected. `total` and `nextOffset` describe the result set, not the assumptions behind it. `meta` only echoes the resolved `limit`, `offset`, `sort` and `order`.

**Consequences**

- Agents and code that consume upstream's JSON need an adapter for attributes and for the list wrapper.
- If upstream later adds paging or inline attribute metadata, revisit this decision and align with upstream where it's compatible.
