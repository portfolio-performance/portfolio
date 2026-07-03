# Spin-off Support — Design Specification

**Status:** Draft (design agreed; ready for implementation planning)
**Scope:** MVP — standard *retained* spin-off corporate action
**Related:** economic domain model in the originating handoff (retained vs surrendered forms,
legs, allocation methods, conservation invariants, edge cases).

---

## 1. Summary

A **spin-off** is a corporate action in which a company you hold (the **source / parent**)
distributes shares of a **new / spun-off entity** (the **target / spinco**) to its shareholders,
pro rata. In the standard *retained* case you keep all parent shares and receive new shares; no
parent shares are sold. The only change on the parent side is **cost basis**: part of the parent's
basis is reallocated to the new holding, conserving total basis. It is a **basis reallocation +
share receipt**, not a trade.

This feature adds first-class modelling of the retained spin-off: manual entry/edit through a
dialog, correct cost-basis and performance treatment across all snapshot calculations, multi-currency
support, and native persistence.

## 2. Scope

**In scope (MVP)**
- Standard retained spin-off (parent shares kept; basis-only reduction on parent).
- Cash-in-lieu for fractional entitlements (via a real disposal of the fraction).
- Multi-currency (source security, target security, and cash account may all differ).
- Manual entry, edit, and delete through a dedicated dialog.
- Native protobuf + XStream persistence.

**Explicitly deferred** (model is shaped to admit them without a persistence change)
- Split-off / share-surrender (exchange) form.
- Elections, lifecycle/announced states.
- Per-lot basis carryover to spinco (holding-period inheritance) — MVP uses one aggregate lot.
- PDF / CSV / JSON import of spin-offs (manual entry only).
- The wider family (stock dividend/bonus, rights distribution, bond conversion) — though the new
  transaction primitives are named generically so they can be reused.

## 3. Economic decomposition — the four legs

A spin-off is one event (`CorporateActionEntry`) composed of up to four transaction legs, all in the
**same portfolio**:

| # | Leg | Transaction | Shares | Purpose |
|---|-----|-------------|--------|---------|
| 1 | Source basis reduction | `PortfolioTransaction` type `DISTRIBUTION_OUTBOUND` | **0** | reduce parent cost basis; no disposal |
| 2 | Target receipt | `PortfolioTransaction` type `DISTRIBUTION_INBOUND` | full entitlement | receive spinco shares with a derived basis |
| 3 | Fractional sale | `PortfolioTransaction` type `SELL` (existing) | floored fraction | realise cash-in-lieu with a real gain/loss |
| 4 | Cash-in-lieu | `AccountTransaction` type `SELL` (existing) | — | cash side of leg 3 |

Legs 3–4 form an ordinary `BuySell` (SELL) pair nested inside the event. The entitlement is delivered
in full on leg 2 (e.g. 33.333 shares), then the floored fraction (0.333) is sold on legs 3–4 — so the
realised gain on the fraction falls out of the existing capital-gains machinery.

### 3.1 New transaction types

Two new `PortfolioTransaction.Type` values are introduced. They are named for their **mechanical role**,
not "spin-off" — the corporate-action identity lives on `CorporateActionEntry` — so they can be reused by
future distribution-family actions.

- **`DISTRIBUTION_OUTBOUND`** — `isPurchase = false`, always `shares = 0`. Reduces cost basis only.
  (Future: `shares > 0` could express a share-surrender for split-off / bond conversion without a new type.)
- **`DISTRIBUTION_INBOUND`** — `isPurchase = true`, `shares = full entitlement`. Receives a security whose
  basis is *assigned/derived* rather than paid in cash.

## 4. Cost basis — derived, never persisted as an amount

Cost basis in Portfolio Performance is **not stored**; it is recomputed from the transaction stream by
`snapshot/security/CostCalculation.java` (FIFO cost = Σ `grossAmount` of the FIFO `LineItem`s; a separate
moving-average total). A spin-off reallocates a *fraction* of the parent's basis.

**Decision:** persist a **basis ratio** (`BigDecimal`, the fraction of parent basis moving to spinco), not
a resolved amount. Basis is then derived from history, so it stays correct if earlier parent buys are later
edited.

### 4.1 Source leg (`DISTRIBUTION_OUTBOUND`) in `CostCalculation`
- Reduce **each currently-held FIFO lot's** `grossAmount` and `netAmount` by the basis ratio
  (proportional across *all* lots — a spin-off reallocates uniformly, preserving per-lot cost and
  holding periods for later parent sells). **Do not** touch shares.
- Reduce `movingRelativeCost` / `movingRelativeNetCost` by the same ratio.

### 4.2 Target leg (`DISTRIBUTION_INBOUND`) in `CostCalculation`
- Establish the spinco lot with basis = **ratio × parent basis at ex-date**, obtained via a
  **record-to-record dependency** (the spinco cost record queries the parent's cost record at ex-date;
  see §7). MVP uses a single aggregate lot dated ex-date.
- **`amount` is NOT the basis** for this type. Unlike `BUY` / `DELIVERY_INBOUND` (where `amount` *is* the
  basis and equals invested capital), the spin-off deliberately breaks that identity — which is exactly
  why `DELIVERY_INBOUND` cannot be reused.

### 4.3 Conservation (structural)
`parent basis removed == spinco basis + cashed-fraction basis`, and `parent quantity unchanged`, hold
**by construction**: parent-out and spinco-in derive from the same ratio; the source leg has `shares = 0`;
the cashed fraction is a real disposal of part of the spinco lot.

## 5. `amount` semantics and performance

`amount` on both new types = **market value at ex-date** = `entitlement shares × spinco ex-date reference
price`. It is decoupled from basis (§4.2) and exists to drive external-flow / performance calculations and
display. The **spinco ex-date reference price** is persisted (also seeds the spinco quote); it is the only
market-value input required.

Performance treatment (each new type must be added to every switch; some `default` to `throw`):

| Calculation | `DISTRIBUTION_OUTBOUND` (source) | `DISTRIBUTION_INBOUND` (target) |
|---|---|---|
| `SharesHeldCalculation` | ± 0 | + entitlement shares |
| `CostCalculation` | proportional basis reduction (§4.1) | derived basis (§4.2) |
| `CapitalGainsCalculation` | not a disposal → no gain | establishes basis; no gain on receipt |
| TTWROR transferals (`ClientIndex`, `PerformanceIndex`) | **outbound** transferal = `amount` (MV) | **inbound** transferal = `amount` (MV) |
| `IRRCalculation` | **+amount** (value returned; same arm as `DELIVERY_OUTBOUND`) | **−amount** (value invested; same arm as `DELIVERY_INBOUND`) |
| `DeltaCalculation` | MV as value returned | MV as value invested |

**Why the performance flow matters:** at ex-date the parent *quote* genuinely drops (we cannot rescale it
as we do for a split). Booking the source MV as an outbound flow prevents that drop from registering as a
loss in the parent's per-security TTWROR/IRR; booking the target MV as an inbound flow prevents a spurious
gain on spinco. The two flows are equal (both `= amount` in the source currency) and **cancel at client
level**, so client-wide performance is undisturbed.

## 6. Multi-currency

Source security, target security and cash account may all differ (e.g. account EUR, source CHF, target USD).

**Rule:** the **event currency = source security currency**. Each leg is denominated per Portfolio
Performance's existing convention, carrying a `GROSS_VALUE` `Unit` (forex + exchange rate) to its native
currency where they differ. The stored rate is the **transaction's own rate** (never the ECB reference from
rate history), consistent with all other PP transactions.

Worked example — account EUR, source CHF, target USD:
- Source leg — **CHF**, no forex (= event currency).
- Target leg — **CHF** with `GROSS_VALUE` forex to **USD**.
- Fractional sale (legs 3–4): portfolio SELL leg **EUR** with `GROSS_VALUE` forex to **USD**; account cash
  leg **EUR**, no units (`TRANSACTIONS_WO_UNITS` forbids units on SELL account legs — the forex rides on the
  portfolio leg).

Because both mandatory security legs (1 and 2) share the source currency, their MV transferals cancel
**exactly** at client level — no rate-history fallback.

**Implementation rule:** the single stored event rate (source→spinco, on the target leg's `GROSS_VALUE`)
must drive **both** the target MV forex **and** the derived-basis conversion in §4.2 — otherwise the ECB
reference rate would silently re-enter.

Currency invariants are enforced by extending `datatransfer/actions/CheckCurrenciesAction.java` to the new
types (transaction currency == security currency ⇒ no forex; otherwise `GROSS_VALUE` in the security
currency).

## 7. Cross-security basis derivation (the record-to-record dependency)

`Calculation.perform(CostCalculation.class, converter, security, lineItems)` runs **per security**
(`LazySecurityPerformanceRecord`), so the target leg's cost pass cannot see the parent's history directly.

**MVP approach (Option A):** the spinco cost record obtains its incoming basis by querying the **parent's**
cost record at ex-date and scaling by the ratio (a record-to-record dependency in the lazy record graph;
needs ordering/cycle-guarding). A later **Option B** (a client-wide coordinated pre-pass) can replace it —
this is a **pure compute-layer change with no persistence impact**, because both read the same stored data
(the ratio, the legs, the link). Per-lot carryover is likewise derivable from the same ratio and stays
persistence-compatible.

### 7.1 Phase 0 spike findings (2026-07-01)

A spike (`SpinOffBasisDerivationTest`, on branch `spike/spinoff-basis-derivation`) **validated the
core bet**: the spinco cost is derivable from the parent's history through the real
`LazySecurityPerformanceSnapshot` path, and conservation holds (100 sh @ 5,000 basis, ratio 0.25 →
parent 3,750 + spinco 1,250 == 5,000). Details:

- **Derivation works, and the per-security isolation is surmountable from inside the visitor.** The spike
  wired it as an **in-visitor sub-computation**: `CostCalculation`, on `DISTRIBUTION_INBOUND`, runs a fresh
  `CostCalculation` over the *source* security's non-distribution transactions dated ≤ ex-date (reached via
  the leg's owning portfolio) and scales by the ratio. This proves the *number* is derivable but is **not**
  yet the cached record-to-record lookup of Option A nor the pre-pass of Option B — both remain open as the
  production wiring and are a compute-layer choice (persistence-neutral, as stated above). Recommendation:
  proceed to Option A (cache via the parent record) in Phase 2 to avoid the sub-computation's re-work.
- **Blast radius confirmed.** Adding the two enum values caused **no core compile errors** (ECJ warns, not
  errors, on statement switches with a `default`). But every switch with `default: throw` needs explicit
  handling at runtime: the spike had to add `DISTRIBUTION_INBOUND`/`DISTRIBUTION_OUTBOUND` arms to
  `CostCalculation` **and** `SecurityPosition` (`getShares`). Phase 1 must likewise cover
  `CapitalGainsCalculation`, `IRRCalculation` (`default: throw`), `SharesHeldCalculation`, the transferal
  collectors, and the filters.
- **Spike limitations to resolve later:** source-basis sub-computation excludes prior distribution legs
  (no chained spin-offs); single-currency; single portfolio; single aggregate lot; ignores the cash/fraction
  legs. `basisRatio`/`sourceSecurity` are temporary carriers on `PortfolioTransaction` — Phase 1 moves them
  onto `CorporateActionEntry`.
- **Phase 2 known limitation (deferred to Phase 3):** for a parent bought in multiple lots strictly before
  the reporting interval, the real `CostCalculation` pass collapses the opening position into one aggregated
  `ValuationAtStart` lot and removes `round(aggregate · ratio)`, while the `DistributionBasis.derivedInboundBasis`
  fallback sub-pass reads the un-aggregated transaction history and removes `Σ round(lotᵢ · ratio)`. The two
  disagree by a rounding cent, and because the snapshot-scoped cache memoizes whichever pass runs first, the
  spinco's derived basis is order-dependent. See `SpinOffBasisDerivationTest` (`@Ignore`d) for a reproduction.

## 8. The event container — `CorporateActionEntry`

A spin-off has **more than two legs and event-level properties**, which the binary, property-less
`CrossEntry` cannot hold. `getCrossTransaction(t)` / `getCrossOwner(t)` return *the* single counterpart
(~69 consumers rely on this), and `PTransaction` encodes a cross-entry as a single pairwise `otherUuid` link.

**Decision:** introduce **`CorporateActionEntry implements CrossEntry`**:
- Holds `List<(owner, transaction)>` legs + a typed **property bag** (event type, distribution ratio, basis
  ratio, spinco ex-date reference price, ex-date). Adds `getLegs()` / `getProperties()`.
- Satisfies the legacy `CrossEntry` interface via a designated **primary counterpart**
  (source leg ↔ target leg) so existing consumers keep working through a binary view.
- Verified safe: no consumer relies on `getCrossTransaction` being a strict two-way inverse
  (`getCrossTransaction(getCrossTransaction(t)) == t`).
- The primary counterpart must be chosen so no reachable `(AccountTransaction)` cast of the counterpart is
  violated (e.g. `ClientClassificationFilter` lines ~428/446, in type-guarded branches).

**Group-aware delete/edit** (a required consequence, not a differentiator): removing any leg removes all N;
edits cascade to siblings. The binary delete-cascade at `model/TransactionOwner.java:34`
(`getCrossTransaction(transaction)` → delete that one) must be reworked N-ary, else orphan legs remain.

## 9. Persistence

### 9.1 Wire format (protobuf, `client.proto` / `ProtobufWriter`)
- New top-level message **`PCorporateAction`** in `PClient` (`repeated PCorporateAction`): event type,
  typed property bag (distribution ratio as an exact **numerator:denominator** integer pair; basis ratio as
  `BigDecimal`; spinco ex-date reference price; ex-date), and `repeated string legs` (leg transaction uuids).
- Legs serialise normally in the flat `PClient.transactions` list with their owner + new type; **drop
  `otherUuid`** for them — the group's leg list is the source of truth.
- Reuse the existing `PTransaction.exDate` field for the ex-date.
- New additive `PTransaction.Type` enum values for `DISTRIBUTION_INBOUND` / `DISTRIBUTION_OUTBOUND`.

### 9.2 In-memory
- A **Client-level registry** `List<CorporateActionEntry>` mirroring `InvestmentPlan`
  (`Client.getPlans()`/`addPlan()`/`removePlan()` pattern) — owns the entry + properties; legs back-point to
  it via `Transaction.getCrossEntry()`.

### 9.3 XStream
- Register aliases/converters for `CorporateActionEntry` + the registry field so the XML format round-trips
  (whole-`Client` XStream serialisation).

### 9.4 Versioning / forward-compatibility
- Bump `Client.CURRENT_VERSION`. An older app opening a newer file **cleanly refuses** ("file from a newer
  version") via the existing `ClientFactory` version check, rather than mis-reading the new transaction
  types. Newer apps read old files unchanged.

## 10. User interface

Modelled on **`SecurityTransactionDialog`** (edit-capable; forex binding), **not** the stock-split wizard
(create-only / applied destructively).

- **`CorporateActionDialog`** extends `AbstractTransactionDialog`, backed by a new **`SpinOffModel`** extends
  `AbstractModel`. Reuse the exchange-rate / `GROSS_VALUE` binding from `BuySellModel` / `SecurityDeliveryModel`.
- **Create** from the security context menu; **edit** by opening any leg (via `OpenDialogAction`, which
  detects the `CorporateActionEntry` and opens the whole group); **delete** removes all legs.
- **Form inputs:** source security (pre-filled) + portfolio + ex-date; target security (pick existing or
  create new: name + currency, ISIN/WKN optional); distribution ratio → editable entitlement (shares are a
  persisted **fact**, per §11); basis ratio with an inline **FMV helper** (enter parent + spinco FMV/share →
  compute ratio; only the ratio persists; spinco FMV/share is the reference price); cash-in-lieu (floored
  fraction → cash, amount overridable to the broker figure, target account defaulting to the portfolio's
  `referenceAccount`); FX rates where currencies differ; a live **conservation summary**.
- **Per portfolio:** a parent held in several portfolios requires running the dialog once per portfolio
  (distinct accounts, rounding, and FX make them genuinely separate events).

## 11. Shares vs basis — deliberate asymmetry

- **Shares received are a persisted fact.** The broker delivered exactly N spinco shares on ex-date; editing
  an old parent buy's *cost* must not retroactively change that. Target-leg shares and the sold-fraction
  shares are authoritative and persisted. The distribution ratio is stored for **provenance / seeding** only.
- **Basis is derived** (§4), so it *does* track later corrections to parent cost.

## 12. Fractional handling

- Boundary is **floor**: the whole part is delivered as shares; the remainder is always cashed. Configurable
  rounding modes are deferred.
- The cashed fraction is a **real disposal** (legs 3–4), so `parent basis removed == spinco basis +
  cashed-fraction basis` holds and the fraction's (immaterial, sub-one-share) gain/loss is realised through
  existing machinery.

## 13. Invariant enforcement (layered)

1. **Construction guarantee** — derived basis, shared-derived MV, `shares = 0` source: the core invariants
   (basis conservation, unchanged parent quantity, transferal cancellation) are *unrepresentable if violated*.
2. **Entry-time validation** — soft-warn if entitlement ≠ `ratio × holdings at ex-date` (brokers round);
   block on currency mismatch.
3. **A `checks/impl` consistency check** (à la `SharesHeldConsistencyCheck`, with offer-to-fix) — catches
   externally-edited/broken groups on load.
4. **No assertions inside the calc engines** (they would fire on legitimately messy data and cost performance).

## 14. Integration points (verified)

| Area | File(s) |
|---|---|
| New transaction types | `model/PortfolioTransaction.java` |
| Cost engine | `snapshot/security/CostCalculation.java` |
| Per-security isolation | `snapshot/security/LazySecurityPerformanceRecord.java`, `Calculation.java` |
| Capital gains | `snapshot/security/CapitalGainsCalculation.java`, `CapitalGainsCalculationMovingAverage.java` |
| Shares held | `snapshot/security/SharesHeldCalculation.java` |
| IRR | `snapshot/security/IRRCalculation.java` (`default: throw` — mandatory) |
| Delta | `snapshot/security/DeltaCalculation.java` |
| TTWROR transferals | `snapshot/ClientIndex.java`, `snapshot/PerformanceIndex.java` |
| Filters | `snapshot/filter/*` (PortfolioClientFilter, WithoutTaxesFilter, ClientClassificationFilter) |
| Cross-entry seam | `model/CrossEntry.java`, `model/TransactionOwner.java`, `model/Transaction.java` |
| Persistence | `model/client.proto`, `model/ProtobufWriter.java`, `model/ClientFactory.java`, `model/Client.java` |
| Currency validation | `datatransfer/actions/CheckCurrenciesAction.java`, `CheckValidTypesAction.java` |
| datatransfer (no-op defaults) | `datatransfer/actions/InsertAction.java`, `DetectDuplicatesAction.java`, `csv/CSVPortfolioTransactionExtractor.java`, `json/JTransaction.java` |
| UI | `ui/dialogs/transactions/` (`AbstractTransactionDialog`, `AbstractModel`, `BuySellModel`, `SecurityDeliveryModel`, `OpenDialogAction`) |
| Checks | `checks/impl/` |

## 15. Open items for implementation planning

- Exact record-to-record dependency wiring + cycle-guarding for §7 (Option A).
- Concrete `CorporateActionEntry` API and how `updateFrom` / owner reassignment behave N-ary.
- `SpinOffModel` field/validation binding and the create-new-security inline flow.
- Test coverage: cost/gains/performance across single- and multi-currency; conservation; edit/delete
  round-trips; save/load (protobuf + XStream); version-refusal by older app.
