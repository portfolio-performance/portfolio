---
paths:
 - "name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/*"
 - "name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/**"
---

PDF importers extract transactions from bank and broker PDF statements.

> **Required:** Whenever these rules are active, the test cases of the modified importer must be reviewed after every change — both for completeness and correct assertions.

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

## Regular Expressions

Beside general good practices for regular expressions, keep in mind:
* all special characters in the PDF document (`äöüÄÖÜß` as well as e.g. circumflex or similar) should be matched by a `.` (dot) because the PDF to text conversion can create different results
* the special characters `$^{[(|)]}*+?\` in the PDF document are to be escaped
* `.match(" ... ")` — always use anchors `^...$`
* `.find(" ... ")` — do NOT add anchors (they are added automatically)

**Standard patterns for common values:**

| Value | Example | Pattern |
| :--- | :--- | :--- |
| Date | 01.01.1970 | `[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}` |
| Date | 1.1.1970 | `[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}` |
| Time | 12:01 | `[\\d]{2}\\:[\\d]{2}` |
| ISIN | IE00BKM4GZ66 | `[A-Z]{2}[A-Z0-9]{9}[0-9]` |
| WKN | A111X9 | `[A-Z0-9]{6}` |
| Valoren | 1098758 | `[A-Z0-9]{5,9}` |
| SEDOL | B5B74S0 | `[A-Z0-9]{7}` |
| CUSIP | 11135F101 | `[A-Z0-9]{9}` |
| TickerSymbol | AAPL, BRK.B | `[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?` |
| Crypto Ticker | BTC, ETH-BTC | `[A-Z0-9]{1,5}(?:[\\-\\/][A-Z0-9]{1,5})?` |
| Amount | 751,68 | `[\\.,\\d]+` or `[\\.\\d]+,[\\d]{2}` |
| Amount | 74'120.00 | `[\\.'\\d]+` |
| Amount | 20 120.00 | `[\\.\\d\\s]+` |
| Currency | EUR | `[A-Z]{3}` |
| Currency Symbol | € or $ | `\\p{Sc}` |

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

**Runtime transaction type switch** — when a single PDF document format contains both deposits and removals (or buys and sells) distinguished only by a sign character, it is allowed to combine them into one block. Set `subject()` to the default type and switch in `assign()` based on the extracted sign. Use `@formatter:off/on` inside the assign lambda to mark the switch comment:
```java
var depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Einzahlung|Auszahlung) (\\-)?[\\.,\\d]+ .*$");
depositRemovalBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                .section("date", "amount", "type", "note") //
                .documentContext("currency") //
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (Einzahlung|Auszahlung)(?<type>\\s(\\-)?)(?<amount>[\\.,\\d]+) (?<note>.*)$") //
                .assign((t, v) -> {
                    // @formatter:off
                    // Is type --> "-" change from DEPOSIT to REMOVAL
                    // @formatter:on
                    if ("-".equals(trim(v.get("type"))))
                        t.setType(AccountTransaction.Type.REMOVAL);

                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(v.get("currency"));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(trim(v.get("note")));
                })

                .wrap(TransactionItem::new));
```

Block variable names may use an underscore suffix to distinguish format variants: `depositRemovalBlock_Format01`, `depositRemovalBlock_Format02`.

**wrap() block** — the SkippedItem guard is optional; only add it when the extractor can produce zero-amount transactions that should be skipped. If used, apply the canonical `if/return SkippedItem` form; no ternary, no intermediate `item` variable, no unused `ctx` parameter. Always guard the amount check with a currency code null-check:
```java
// AccountTransaction — simple form (no guard needed)
.wrap(TransactionItem::new)

// AccountTransaction — with guard
.wrap(t -> {
    if (t.getCurrencyCode() != null && t.getAmount() == 0)
        return new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

    return new TransactionItem(t);
})

// BuySellEntry — simple form (no guard needed)
.wrap(BuySellEntryItem::new)

// BuySellEntry — with guard
.wrap(t -> {
    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() == 0)
        return new SkippedItem(new BuySellEntryItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

    return new BuySellEntryItem(t);
})
```


# Testing
Test methods should be in following structure

```java
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

**Crypto extractors** — when a test class uses the `lookupCryptoProvider` override (class-level extractor field with anonymous subclass), `hasFeed()` and `hasFeedProperty()` are mandatory in every `security(...)` assertion:
```java
// class-level extractor with crypto provider override
BSDEXPDFExtractor extractor = new BSDEXPDFExtractor(new Client())
{
    @Override
    protected List<SecuritySearchProvider> lookupCryptoProvider()
    {
        return TestCoinSearchProvider.cryptoProvider();
    }
};

// check security — hasFeed and hasFeedProperty are required
assertThat(results, hasItem(security( //
                hasIsin(null), hasWkn(null), hasTicker("BTC"), //
                hasName("Bitcoin"), //
                hasCurrencyCode("EUR"), //
                hasFeed(CoinGeckoQuoteFeed.ID), //
                hasFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID, "bitcoin"))));
```
Tests that do not involve crypto securities (e.g. deposits/removals only) may create a plain `new BSDEXPDFExtractor(new Client())` locally within the test method instead.

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

`deposit(...)` and `removal(...)` assertions do NOT include `hasGrossValue`, `hasTaxes`, or `hasFees` — use this compact format:

```java
assertThat(results, hasItem(deposit(hasDate("2011-10-19"), hasAmount("EUR", 100.00), //
                hasSource("Kontoauszug01.txt"), hasNote(null))));
```

`interest(...)`, `interestCharge(...)`, `fee(...)` and `feeRefund(...)` assertions always include `hasGrossValue`, `hasTaxes` and `hasFees` — use this multi-line format:

```java
assertThat(results, hasItem(interest( //
                hasDate("2021-12-25"), //
                hasSource("Kontoauszug01.txt"), //
                hasNote(null), //
                hasAmount("EUR", 0.61), hasGrossValue("EUR", 0.83), //
                hasTaxes("EUR", (0.20 + 0.01 + 0.01)), hasFees("EUR", 0.00))));
```

- Each method has a starting `assertThat`-Block checking the counts. All 8 assertions need to be present.
- Use `//` to enforce line-breaks when checking securities and transactions
- Include time (hours, minutes, seconds) in `hasDate` when the source document provides it.
- The `ExtractorMatchers`-class contains test assertion helpers
- The fees and taxes could consist of multiple values, do not merge or consolidate them for better understanding
- Always use `0.00` (double literal) for zero amounts, never the integer `0`
- Every `dividend(...)` assertion must include `hasExDate(...)`. The correct value comes from the JUnit test failure message: look for `dividend with exDate = <value>` in the mismatch output. Use that exact value. If the extractor does not extract an ex-date, the actual value will be `null`.
