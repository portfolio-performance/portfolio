# Sign monetary fields only inside reconciling breakdowns

Fees and taxes appear in the REST API in three different roles, and each pulled toward a different
sign. `GET /v1/files/{file}/performance` emits them **negated** so that its breakdown reconciles by
plain addition. The per-instrument performance resource aggregates the same charges but satisfies no
such identity. A future transactions resource will expose them per record, where the model stores
them as positive magnitudes — `Unit.Type.FEE` amounts and `AccountTransaction` FEES/TAXES amounts are
positive and will stay that way, because the file format depends on it. Deciding this per endpoint
would leave `fees` meaning opposite things one path segment apart, and the wire format cannot be
renamed once clients exist.

**Decision**

> A money field that is a term in a reconciling sum is signed. Every other money field is a
> magnitude.

| field | endpoint | value | why |
|---|---|---|---|
| `breakdown.fees` | `/performance` | `-18.90` | term of `openingValue + … + netDeposits = closingValue` |
| `expenses.fees` | `/performance/instruments` | `18.90` | an aggregate of charges; no identity |
| `fees` | future `/transactions` | `18.90` | a measured quantity of one event |

The negation in `EntityJson.performance` (`negate(...)` applied to `CategoryType.FEES` and
`TAXES`) is therefore not a house style to copy — it is a local consequence of that payload being
a breakdown, and it stays.

The rule generalises past fees. `Risk.getMaxDrawdown()` returns a positive fraction and is emitted
positive, even though charts conventionally draw drawdown below zero: it is a magnitude, and it is
a term in nothing.

Any endpoint that emits signed money must say in `openapi.yaml` which identity its terms add up to.
If there is no such identity to name, the fields are magnitudes.

**Considered Options**

- *Signed everywhere — `fees` always negative.* One sign for one word, nothing to learn. Rejected
  because the transactions resource would have to negate what the model stores, what the PP UI
  shows, and what the established interchange formats carry: OFX has held `COMMISSION`, `FEES` and
  `TAXES` as positive amounts inside investment transactions for decades, and Plaid exposes `fees`
  positive with direction on `amount`. Interactive Brokers is the visible counterexample, reporting
  commission negative in Flex output. Bending the largest and least changeable surface of the API to
  suit one breakdown is the wrong way round.
- *Magnitudes everywhere — flip `/performance` too.* Total uniformity, and achievable: the REST API
  branch is unreleased, so there is no compatibility obligation. Rejected because the breakdown then
  stops reconciling by addition — the documented identity acquires minus signs on two of its nine
  terms, and every client charting contributions has to special-case which ones to subtract. The
  reconcile-by-addition property was a deliberate design goal and is worth one stated rule.
- *Decide per endpoint as each is designed.* What was happening implicitly. Rejected: it produced a
  question that had to be re-argued from scratch for the second endpoint, and would have produced an
  inconsistency nobody noticed until a client summed across two of them.

**Consequences**

`fees` is a positive magnitude nearly everywhere in the API and negative only inside a breakdown, so
the rule must be stated in the specification rather than inferred from any single payload. A client
aggregating one field across endpoints has to know which side of the line it is on — the cost of
this decision, accepted in exchange for both the model-faithful transaction surface and the
self-adding breakdown.

There is a usable test when adding a field: if you can write down the equation it participates in,
sign it; otherwise emit the magnitude. This also acts as a design check — a payload with signed
money and no documentable identity is a payload that has not decided what it is.

Attribution-style resources added later (per investment account, per taxonomy, per period) will each
need this call. Those that reconcile inherit the negation; those that merely aggregate do not.
