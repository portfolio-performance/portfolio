# 04: Does an amounts & rates module pass the deletion test?

Type: grilling
Status: resolved
Blocked by:

Map: [map.md](../map.md). Review card 6, "An amounts & rates module", in [architecture-review.html](../architecture-review.html#c6). Speculative: this ticket may end with "rule it out".

## Question

Should stored↔decimal conversion, rate inversion, conversion rounding, shares as a fraction, and ADR 0003's signed-vs-magnitude rule live in one module, or is the scatter harmless? Before deciding that, which of today's inconsistent rounding modes are intended?

Evidence from the review:

- `Rate.convert` uses HALF_DOWN (currency/ExchangeRates ~99), mirroring core.
- Rate inversion at scale 10 HALF_DOWN appears twice (ExchangeRates ~245, TransactionsCommand ~302).
- The transfer rate uses scale 10 **HALF_UP** (apply/TransferOperations ~23, ~178–179).
- Allocation computes the share by **double** division (query/AllocationCommand ~193); `TaxonomyTree.fraction` (~80) and `OversellGuard.plain` (~112–115) do their own arithmetic.
- The gross-amount sign logic sits in TransactionInput (~171–188). ADR 0003's sign rule is enforced nowhere central.
- `command/Decimals` already owns formatting and `toStored`.

If the module stays: its interface and whether ticket 05 (Valuation) uses it. If it's ruled out: whether the HALF_UP and double cases are bugs that need their own fix ticket.

## Answer

**Ruled out.** The amounts & rates module fails the deletion test. Recorded in [ADR 0007](../../../docs/adr/0007-cli-arithmetic-mirrors-core-rounding-per-site.md).

- Every rounding mode in the evidence is intended, because each copies the core or UI site for the same figure:
  - `Rate.convert` uses HALF_DOWN, like core `CurrencyConverter`.
  - The two inversions use scale 10 HALF_DOWN, like core `ExchangeRate.inverse`.
  - The transfer rate uses scale 10 HALF_UP, like the desktop `AccountTransferModel.setAmount`.
  - The allocation `share` is a double division, like core's `getShare()`, which `AllocationCommandTest` asserts it equals.
- None of these needs a fix ticket.
- `TaxonomyTree.fraction` is exact, and `OversellGuard.plain` only formats a message. Neither has any rounding to unify.
- The gross-amount check in TransactionInput reconciles input. It is not ADR 0003's sign rule. ADR 0003 governs output fields per field, so there is nothing to centralise.
- `Decimals` stays the one shared module for stored ↔ decimal conversion and JSON encoding.
- Follow-ups:
  - Ticket 05: the two hand-copied inversions (ExchangeRates ~245, TransactionsCommand ~302) call core's `ExchangeRate.inverse`. 05 picks up no other rounding from this ticket.
  - Ticket 07's spec: the HALF_UP transfer-rate site and the double share site each get a one-line comment naming the core or UI site they mirror.
