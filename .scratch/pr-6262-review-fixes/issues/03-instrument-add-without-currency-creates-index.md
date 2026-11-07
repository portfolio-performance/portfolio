# 03: `instrument.add` without a currency creates an index

**What to build:** Adding an instrument without `currencyCode` produces an instrument with no currency (an index), as the `instrument.add` help text promises ("ISO currency code; omit for an index."). Today the core `Security` defaults its currency to EUR, so an add that omits the currency reports `currencyCode: "EUR"`. A supplied currency must still win, and `instrument.update` behavior (including the immutable-currency check) must not change.

Suggested approach: clear the currency in the instrument kind's create initializer, so the fields present in the input are applied afterwards. First verify that the initializer runs before the input fields are applied. If it does not, or if clearing breaks another path, fall back to documenting the EUR default in the help text and regenerate the golden help files, and say so in the commit message.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] `apply` with `instrument.add` and no `currencyCode` yields an instrument with no currency in the `after` result
- [ ] `instrument.add` with a `currencyCode` still creates an instrument with that currency
- [ ] Update validation (currency kept) and the file's other behavior are unchanged; existing CLI tests pass
- [ ] A test covers both the omitted and the supplied case
- [ ] Origin: CodeRabbit review thread on PR #6262 (outdated; the logic is now in the instrument master-data declaration)
