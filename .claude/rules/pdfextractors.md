---
paths:
 - "name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/*"
 - "name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/**"
---

PDF importers extract transactions from bank and broker PDF statements.

> **Required:** Whenever these rules are active, the test cases of the modified importer must be reviewed after every change — both for completeness and correct assertions.

- Each bank/broker has its own extractor class inside `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/`
- Tests are located at `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/` within a folder for each bank, containing all connected test-exports.

# Implementation

- `AbstractPDFExtractor` is the base class for every pdf-extractor, containing utility methods
- `ExtractorUtils` offers additional utility methods, if the ones from the base-class aren't enough.
- Use `TextUtil` when it's required to manipulate text
- When implementing a new extractor or adding new parsing logic method, check `BaaderBankPDFExtractor` for reference and use the same pattern
  - Split logic into methods like `addBuySellTransaction()`, `addDividendTransaction()` or `addInterestTransaction()`
  - When extracting information from pdf-documents, create multiple `section`-Blocks. 
  - Consistency in coding style between all extractors is more important than nice code.
  - Paste the matching document lines verbatim above each section-block, wrapped in `@formatter:off` / `@formatter:on` — see below.
- Only implement document types, transaction types and patterns that occur in a real sample document. Do not add alternatives for documents you have not seen.

## `@formatter:off` / `@formatter:on`

These directives protect layout the Eclipse formatter would destroy. Ask: *would reflowing or wrapping this comment lose information?* If yes, wrap it. If no, leave it bare.

**Yes — wrap.** The primary case: document text copied verbatim and placed directly above the regex that parses it. The line breaks and column positions are what make it comparable to the pattern.

```java
// @formatter:off
// Nominale Wertpapierbezeichnung ISIN (WKN)
// Stück 250 GLENCORE PLC              JE00B4T3BW64 (A1JAGV)
// @formatter:on
.section("name", "isin", "wkn", "shares") //
```

Also wrap multi-line column maps (`// Formatting:` followed by the `A | B | C` variants) and long one-line formulas that must not be wrapped mid-expression.

**No — leave bare.** Explanatory prose reads identically after reflowing, so the directives are noise:

```java
// correct
// Percentage quotation, workaround for bonds
var shares = asBigDecimal(v.get("shares"));

// unnecessary
// @formatter:off
// Percentage quotation, workaround for bonds
// @formatter:on
var shares = asBigDecimal(v.get("shares"));
```

**In reviews:** flag a *missing* directive only where verbatim document text or a column map would be reflowed. Do not flag a missing directive on prose comments. Do not flag an *existing* directive as over-use — it is harmless and widespread, and churning it costs more than it saves. Class Javadoc is out of scope: do not flag its presence or absence either way.

## Regular Expressions

Beside general good practices for regular expressions, keep in mind:
* match characters exactly as they appear in the sample text: if the sample shows `ü`, write `ü`. This holds for all non-ASCII characters (umlauts, `ß`, accents, `°`, `€`). Do not replace correct characters with `.` (dot) on suspicion.
* use `.` only for a character that is evidently broken: either the sample shows a replacement character (`�`), or two samples of the same bank disagree at that spot (`ü` in one, `�` in another). Whether a character is broken is the human's call: point it out and ask, don't decide on your own. Mojibake such as `Ã¼` for `ü` replaces one character with two, so a dot does not fit; report the sample as suspect instead.
* existing `.` patterns stay as they are; do not rewrite them to umlauts, or the other way around, in code you are not otherwise changing.
* the special characters `$^{[(|)]}*+?\` in the PDF document are to be escaped
* `.match(" ... ")` — always use anchors `^...$`
* `.find(" ... ")` — do NOT add anchors (they are added automatically)

**In reviews:** flag neither a literal umlaut nor an existing `.` in its place. A newly written `.` where the sample clearly shows the correct character gets a light note, not a required change.

**Standard patterns for common values:**

| Value | Example | Pattern |
| :--- | :--- | :--- |
| Date | 01.01.1970 | `[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}` |
| Date | 1.1.1970 | `[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}` |
| Time | 12:01 | `[\\d]{2}:[\\d]{2}` |
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

**Transaction context** — a value that belongs to a single transaction (a flag, a time read in an earlier section) goes into `v.getTransactionContext()`, which exists only while that transaction is parsed. The document context holds values that are valid for the whole document, such as the currency or the year of a statement; a per-transaction value stored there leaks into the following transactions:
```java
.assign((t, v) -> v.getTransactionContext().putBoolean("noTax", true))

.assign((t, v) -> {
    if (!v.getTransactionContext().getBoolean("noTax"))
        processTaxEntries(t, v, type);
})
```

**Runtime transaction type switch** — when a single PDF document format contains both deposits and removals (or buys and sells) distinguished only by a sign character, it is allowed to combine them into one block. Set `subject()` to the default type and switch in `assign()` based on the extracted sign:
```java
var depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Einzahlung|Auszahlung) (\\-)?[\\.,\\d]+ .*$");
depositRemovalBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                .section("date", "amount", "type", "note") //
                .documentContext("currency") //
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (Einzahlung|Auszahlung)(?<type>\\s(\\-)?)(?<amount>[\\.,\\d]+) (?<note>.*)$") //
                .assign((t, v) -> {
                    // Is type --> "-" change from DEPOSIT to REMOVAL
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

A dividend whose net amount drops to `0.00` because of taxes or fees is a regular dividend: import it with gross value, taxes and fees, and do not skip it with the guard. Otherwise the withheld taxes are missing in the tax and dividend reports.

**Cancellations** — importing cancelled orders (Storno) is not supported. Detect the cancellation in a section and mark the transaction as failure; do not return a `SkippedItem` and do not use `MsgErrorTransactionTypeNotSupportedOrRequired`:
```java
.section("type").optional() //
.match("^(?<type>STORNO)$") //
.assign((t, v) -> v.markAsFailure(Messages.MsgErrorTransactionOrderCancellationUnsupported))
```

A reversed withholding tax (e.g. `Cancel Withholding Tax` in a statement) is a tax refund, not a cancelled order: import it as `TAX_REFUND` (or `TAXES` if the amount is negative).

**Zero amounts and bookings from other documents**
- Purchases and inbound deliveries with a zero amount (e.g. bonus or free shares) are never imported, because the performance would become infinite. Mark them with `MsgErrorTransactionTypeNotSupportedOrRequired` (see the `wrap((t, ctx) -> ...)` of the purchases in `BaaderBankPDFExtractor`) and do not estimate a value from historical prices. An outbound delivery with a zero amount (worthless securities written off) is allowed.
- A line that is booked from somewhere else gets a specific message: `MsgErrorTransactionAlternativeDocumentRequired` if the bank provides another document for it, `MsgErrorTransactionIncludedInOtherTransaction` if it is already part of another transaction of the document (e.g. the withholding tax of a dividend). Use `MsgErrorTransactionTypeNotSupportedOrRequired` only for transactions that are not needed at all.
- Taxes or fees that are already deducted from a booked amount (e.g. the taxes listed below an interest settlement) are units of that transaction; do not book them again as separate removals or taxes.


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

**Security in EUR** — the `...WithSecurityInEUR` variants of a test (security currency differs from the transaction currency) keep the currency check in the transaction assertion:
```java
                hasTaxes("CHF", 0.11 / (71.48 / 67.82)), hasFees("CHF", 0.00), //
                check(tx -> {
                    var c = new CheckCurrenciesAction();
                    var s = c.process((PortfolioTransaction) tx, new Portfolio());
                    assertThat(s, is(Status.OK_STATUS));
                }))));
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
                                hasDate("2012-07-02"), hasShares(0.00), //
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
- `hasDate(...)` and `hasExDate(...)` use the shortest form of the date. A date without time means midnight, and missing seconds mean `:00`:
  - Midnight: write the date only, `hasDate("2025-04-30")`, not `"2025-04-30T00:00"`
  - Otherwise include the time the document gives. Leave out the seconds if they are `:00`: `hasDate("2025-06-05T12:56")`, not `"2025-06-05T12:56:00"`. Keep real seconds: `hasDate("2024-12-05T11:21:27")`
  - Do not shorten existing assertions that use `T00:00` or `:00` seconds unless you change that line anyway
- The `ExtractorMatchers`-class contains the assertion helpers (`purchase(...)`, `dividend(...)`, `hasDate(...)`, ...); the count helpers (`countSecurities`, `countBuySell`, ...) are in `ExtractorTestUtilities`
- The fees and taxes could consist of multiple values, do not merge or consolidate them for better understanding
- Always use `0.00` (double literal) for zero amounts, never the integer `0`
- Every `dividend(...)` assertion must include `hasExDate(...)`. The correct value comes from the JUnit test failure message: look for `dividend with exDate = <value>` in the mismatch output. Use that date, written in the shortest form (see `hasDate` above). If the extractor does not extract an ex-date, the actual value will be `null`.
