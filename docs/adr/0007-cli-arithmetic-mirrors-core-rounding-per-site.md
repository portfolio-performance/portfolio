# CLI arithmetic mirrors core's rounding per site, not one house rule

The CLI rounds in several different ways. Converting an amount rounds HALF_DOWN, and so does inverting a rate at scale 10. A transfer's derived rate rounds HALF_UP at scale 10. Allocation's `share` is a double division. Read side by side this looks inconsistent, and an architecture review proposed one amounts & rates module to settle it. It is not inconsistent: each site copies the core or desktop UI computation for the same figure. Conversion and inversion copy core's `CurrencyConverter` and `ExchangeRate.inverse`. The transfer rate copies the desktop transfer dialog, which derives it the same way. The share copies core's `getShare()`, and a test asserts that the two are equal. A single house rule would make the CLI disagree with what the user sees in Portfolio Performance by a last digit.

**Decision**

> Each piece of CLI arithmetic copies the core or UI site it corresponds to, rounding mode and scale included. Where core offers the computation, the CLI calls it rather than copying it. Where the CLI must copy it, a one-line comment names the site it mirrors.

`Decimals` stays the one shared module for stored ↔ decimal conversion and JSON encoding. There is no amounts & rates module.

**Considered Options**

- *One amounts & rates module with a single rounding policy*: rejected. Every policy breaks parity with core or the desktop app at some site.
- *One module that wraps each core convention under its own name*: rejected because it fails the deletion test. Each wrapper would pass straight through to one call site, and it would hide which core site the CLI mirrors, so locality gets worse.

**Consequences**

- HALF_UP in the transfer rate and the double `share` are intended. They are not bugs.
- The two hand-copied rate inversions should call core's `ExchangeRate.inverse`.
- ADR 0003's sign rule stays a rule about output fields, decided per field when the field is designed. No module enforces it.
