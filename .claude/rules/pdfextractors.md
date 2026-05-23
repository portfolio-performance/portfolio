---
paths:
 - "name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/*"
 - "name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/**"
---

PDF importers extract transactions from bank and broker PDF statements. 

- Each bank/broker has its own extractor class inside `name.abuchen.portfolio/src/name/abuchen/datatransfer/pdf/`
- Tests are located at `name.abuchen.portfolio.tests/src/name/abuchen/datatransfer/pdf/` within a folder for each bank, containing all connected test-exports.

# Implementation

- `AbstractPDFExtractor` is the base class for every pdf-extractor, containing utility methods
- `ExtractorUtils` offers additional utility methods, if the ones from the base-class aren't enough.
- Use `TextUtil` when it's required to manipulate text
- When implementing a new extractor or adding new parsing logic method, check `BaaderBankPDFExtractor` for reference and use the same pattern
  - Split logic into methods like `addBuySellTransaction()`, `addDividendTransaction()` or `addInterestTransaction()`
  - When extracting information from pdf-documents, create multiple `section`-Blocks. 
  - Consistency in coding style between all extractors is more important than nice code.
  - Add comment-blocks with `@formatter:off` and `@formatter:on` before each section-block showing the format that is handled there.

## Conventions

**DocumentType declaration** — always `final`:
```java
final var type = new DocumentType("...");
```

**subject() lambda** — always use the single-argument convenience constructor or method reference, never the verbose 3-line form:
```java
// correct
.subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))
.subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))
.subject(() -> new PortfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND))
.subject(AccountTransferEntry::new)   // no Type parameter — use method reference

// wrong
.subject(() -> {
    var t = new AccountTransaction();
    t.setType(AccountTransaction.Type.DIVIDENDS);
    return t;
})
```

**setCurrencyCode** — always use `asCurrencyCode()`, never `CurrencyUnit` constants:
```java
t.setCurrencyCode(asCurrencyCode("EUR"));   // correct
t.setCurrencyCode(CurrencyUnit.EUR);         // wrong
```

**Fixed-currency extractors** — when an extractor always deals with one currency (e.g. a USD-only broker), declare a private String constant and use it with `asCurrencyCode()`. Keep `asCurrencyCode()` — it has normalization side-effects even for known codes:
```java
private static final String USD = "USD";

v.put("currency", asCurrencyCode(USD));          // correct
v.put("currency", CurrencyUnit.USD);              // wrong — CurrencyUnit object, not String
v.put("currency", asCurrencyCode("USD"));         // works but prefer named constant
```

**DocumentContext currency** — when storing currency into the document context, always normalize with `asCurrencyCode()`. When the value is later read back via `.documentContext("currency")`, it is already normalized — do NOT wrap it with `asCurrencyCode()` again:
```java
// storing — always normalize
.assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))  // correct
.assign((ctx, v) -> ctx.put("currency", v.get("currency")))                   // wrong

// reading via documentContext — already normalized, no wrapping needed
.section("tax").optional()
.documentContext("currency")
.assign((t, v) -> {
    var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));             // correct
    var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(...));      // wrong — redundant
})

// reading via local .section — currency is raw, asCurrencyCode() is required
.section("tax", "currency").optional()
.assign((t, v) -> {
    var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));  // correct
})
```

**wrap() block** — use the canonical `if/return SkippedItem` form; no ternary, no intermediate `item` variable, no unused `ctx` parameter. Always guard the amount check with a currency code null-check:
```java
// AccountTransaction
.wrap(t -> {
    if (t.getCurrencyCode() != null && t.getAmount() == 0)
        return new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

    return new TransactionItem(t);
})

// BuySellEntry
.wrap(t -> {
    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() == 0)
        return new SkippedItem(new BuySellEntryItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

    return new BuySellEntryItem(t);
})
```


# Testing
Test methods should be in following structure

```
@Test
public void testWertpapierKauf01()
{
    var extractor = new ScalableCapitalPDFExtractor(new Client());

    List<Exception> errors = new ArrayList<>();

    var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

    assertThat(errors, empty());
    assertThat(countSecurities(results), is(1L));
    assertThat(countBuySell(results), is(1L));
    assertThat(countAccountTransactions(results), is(0L));
    assertThat(countAccountTransfers(results), is(0L));
    assertThat(countItemsWithFailureMessage(results), is(0L));
    assertThat(countSkippedItems(results), is(0L));
    assertThat(results.size(), is(2));
    new AssertImportActions().check(results, "EUR");

    // check security
    assertThat(results, hasItem(security( //
                    hasIsin("IE0008T6IUX0"), hasWkn(null), hasTicker(null), //
                    hasName("Vngrd Fds-ESG Dv.As-Pc Al ETF"), //
                    hasCurrencyCode("EUR"))));

    // check purchase transaction
    assertThat(results, hasItem(purchase( //
                    hasDate("2024-12-12T13:12:51"), hasShares(3.00), //
                    hasSource("Kauf01.txt"), //
                    hasNote("Ord.-Nr.: SCALsin78vS5CYz"), //
                    hasAmount("EUR", 19.49), hasGrossValue("EUR", 18.50), //
                    hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));

}
```

`withFailureMessage(...)` assertions follow this structure — the `//` after the opening parenthesis is required:

```java
// check failure message
assertThat(results, hasItem(withFailureMessage( //
                Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                dividend( //
                                hasDate("2022-06-13"), hasExDate(null), //
                                hasShares(10.672), //
                                hasSource("DividendeStorno01.txt"), //
                                hasNote("Vorgangs-Nr.: XXXXX"), //
                                hasAmount("EUR", 7.04), hasGrossValue("EUR", 7.04), //
                                hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
```
`skippedItem(...)` assertions follow the same structure — the `//` after the opening parenthesis is required:

```java
// check skipped item
assertThat(results, hasItem(skippedItem( //
                Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                purchase( //
                                hasDate("2012-07-02T00:00"), hasShares(0.00), //
                                hasSource("Quartalsbericht06.txt"), //
                                hasNote(null), //
                                hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
```

- Each method has a starting `assertThat`-Block checking the counts. All 8 assertions need to be present.
- Use `//` to enforce line-breaks when checking securities and transactions
- Include time (hours, minutes, seconds) in `hasDate` when the source document provides it.
- The `ExtractorMatchers`-class contains test assertion helpers
- The fees and taxes could consist of multiple values, do not merge or consolidate them for better understanding
- Every `dividend(...)` assertion must include `hasExDate(...)`. The correct value comes from the JUnit test failure message: look for `dividend with exDate = <value>` in the mismatch output. Use that exact value. If the extractor does not extract an ex-date, the actual value will be `null`.
