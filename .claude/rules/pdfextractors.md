---
paths:
 - "name.abuchen.portfolio/src/name/abuchen/datatransfer/pdf/*"
 - "name.abuchen.portfolio.tests/src/name/abuchen/datatransfer/pdf/*"
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

    // check dividends transaction
    assertThat(results, hasItem(purchase( //
                    hasDate("2024-12-12T13:12:51"), hasShares(3.00), //
                    hasSource("Kauf01.txt"), //
                    hasNote("Ord.-Nr.: SCALsin78vS5CYz"), //
                    hasAmount("EUR", 19.49), hasGrossValue("EUR", 18.50), //
                    hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));

}
```

- Each method has a starting `assertThat`-Block checking the counts. All 8 assertions need to be present.
- Use `//` to enforce line-breaks when checking securities and transactions
- All dates should be checked with hours, minutes and seconds.
- The `ExtractorMatchers`-class contains test assertion helpers