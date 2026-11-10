package name.abuchen.portfolio.datatransfer.pdf.natixisinterepargne;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSkippedItems;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.NatixisInterepargnePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class NatixisInterepargnePDFExtractorTest
{
    @Test
    public void testInteressement01()
    {
        var extractor = new NatixisInterepargnePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Interessement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("SELECTION DNCA MIXTE ISR (I)"), //
                        hasCurrencyCode("EUR"))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("NATIXIS ES MONETAIRE (PART I2)"), //
                        hasCurrencyCode("EUR"))));

        // check 3rd security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("AVENIR RENDEMENT (PART I)"), //
                        hasCurrencyCode("EUR"))));

        // check 4th security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("IMPACT ISR RENDEMENT SOLID I"), //
                        hasCurrencyCode("EUR"))));

        // check 1st purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-20T00:00"), hasShares(15.2883), //
                        hasSource("Interessement01.txt"), //
                        hasNote("Intéressement 2024 | PEE | Disponibilité 01/06/2030"), //
                        hasAmount("EUR", 340.43), hasGrossValue("EUR", 330.34), //
                        hasTaxes("EUR", 10.09), hasFees("EUR", 0.00))));

        // check 2nd purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-20T00:00"), hasShares(6.7327), //
                        hasSource("Interessement01.txt"), //
                        hasNote("Intéressement 2024 | PEE | Disponibilité 01/06/2030"), //
                        hasAmount("EUR", 170.22), hasGrossValue("EUR", 165.18), //
                        hasTaxes("EUR", 5.04), hasFees("EUR", 0.00))));

        // check 3rd purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-20T00:00"), hasShares(15.4030), //
                        hasSource("Interessement01.txt"), //
                        hasNote("Intéressement 2024 | PEE | Disponibilité 01/06/2030"), //
                        hasAmount("EUR", 680.87), hasGrossValue("EUR", 660.68), //
                        hasTaxes("EUR", 20.19), hasFees("EUR", 0.00))));

        // check 4th purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-20T00:00"), hasShares(18.7178), //
                        hasSource("Interessement01.txt"), //
                        hasNote("Intéressement 2024 | PEE | Disponibilité 01/06/2030"), //
                        hasAmount("EUR", 510.68), hasGrossValue("EUR", 495.54), //
                        hasTaxes("EUR", 15.14), hasFees("EUR", 0.00))));
    }

    @Test
    public void testInteressement01WithFundNamesEndingWithNumber()
    {
        // Regression test: a fund name ending with a number must not be merged
        // with the employee payment. The test is based on Interessement01.txt
        // with two fund names replaced, therefore no additional document is
        // required.
        var text = PDFInputFile.loadSingleTestCase(getClass(), "Interessement01.txt").getText() //
                        .replace("SELECTION DNCA MIXTE ISR (I) 236,35", "AVENIR RETRAITE 2030 236,35") //
                        .replace("AVENIR RENDEMENT (PART I) 472,70", "FONDS S&P 500 472,70");

        var extractor = new NatixisInterepargnePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.createTestCase("Interessement01.txt", text), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // check security with year at the end of the name
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("AVENIR RETRAITE 2030"), //
                        hasCurrencyCode("EUR"))));

        // check security with three-digit number at the end of the name
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("FONDS S&P 500"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction of fund name with year
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-20T00:00"), hasShares(15.2883), //
                        hasSource("Interessement01.txt"), //
                        hasNote("Intéressement 2024 | PEE | Disponibilité 01/06/2030"), //
                        hasAmount("EUR", 340.43), hasGrossValue("EUR", 330.34), //
                        hasTaxes("EUR", 10.09), hasFees("EUR", 0.00))));

        // check purchase transaction of fund name with three-digit number
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-20T00:00"), hasShares(15.4030), //
                        hasSource("Interessement01.txt"), //
                        hasNote("Intéressement 2024 | PEE | Disponibilité 01/06/2030"), //
                        hasAmount("EUR", 680.87), hasGrossValue("EUR", 660.68), //
                        hasTaxes("EUR", 20.19), hasFees("EUR", 0.00))));
    }

    @Test
    public void testArbitrage01()
    {
        var extractor = new NatixisInterepargnePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Arbitrage01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("IMPACT ISR RENDEMENT SOLID I"), //
                        hasCurrencyCode("EUR"))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("AVENIR RETRAITE 2060-2064 I"), //
                        hasCurrencyCode("EUR"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-11-25T00:00"), hasShares(9.2267), //
                        hasSource("Arbitrage01.txt"), //
                        hasNote("Arbitrage | PERCOL"), //
                        hasAmount("EUR", 242.37), hasGrossValue("EUR", 242.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-25T00:00"), hasShares(7.2140), //
                        hasSource("Arbitrage01.txt"), //
                        hasNote("Arbitrage | PERCOL"), //
                        hasAmount("EUR", 242.37), hasGrossValue("EUR", 242.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
