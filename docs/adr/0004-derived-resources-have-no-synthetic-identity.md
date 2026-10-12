# Derived resources have no synthetic identity

`GET /v1/files/{file}/trades` is the first resource the REST API computes rather than stores. A trade
does not exist in the file: `TradeCollector` recomputes it on every request, and its very shape
depends on a query parameter — under `grouping=per-lot` the same transactions produce a different set
of trades than under `combined`. Giving each item a `uuid`, so that the payload looks like every
other collection, was the tempting move.

**Decision**

> A resource the server computes rather than stores gets no synthetic identity. It is addressed by
> the filter that produced it. Where a caller needs a durable reference, expose a foreign key to a
> stored entity instead of minting an id for the derived one.

**Considered Options**

- *A random UUID per item.* It would not survive the next request — an identifier that identifies
  nothing. A client caching it, logging it or sending it back would be wrong every time, and nothing
  in the payload would say so.

- *A deterministic, content-derived id* — hash the inputs that determine the trade (security uuid,
  portfolio uuid, entry date, shares, grouping) the way Git addresses content. Genuinely stateless
  and self-invalidating: edit a transaction and the id changes, which is honest. Rejected because it
  buys a stable *reference* without buying a *lookup* — resolving `GET /trades/{hash}` still means
  recollecting every trade in the file and scanning for a match, so the endpoint would cost the same
  as the collection while looking cheaper. An opaque token that unlocks nothing is worse than no
  token.

- *Materialise the computation as a resource* — `POST` a job, receive an id, `GET` the result by it,
  expire it with a `410 Gone`, as Stripe's Report Runs and AWS Athena query executions do. That
  produces real identity, but it introduces server-side state with a lifecycle into an API whose
  entire premise is a thin, stateless read over an already-open file.

**The escape hatch**

If a use case for referencing a single trade does appear, the answer is a foreign key, not an id: a
closed trade holds its `realClosingTransaction`, and `Transaction` carries a persisted `uuid`. Three
caveats belong to that option and are recorded here so they are not rediscovered.
`TradeCollector.split(...)` fabricates transaction copies that receive *fresh* random uuids and exist
nowhere in the `Client`, so only `realClosingTransaction` is safe to expose; under `per-lot` a single
sale can close several lots, so the reference is not unique; and an open trade has no closing
transaction at all. It is a reference, never a key.
