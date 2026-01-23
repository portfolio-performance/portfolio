package name.abuchen.portfolio.datatransfer.pdf.vdkbanknv;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
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

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.VDKBankNVPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class VDKBankNVPDFExtractorTest
{
    @Test
    public void testAankoop01()
    {
        var extractor = new VDKBankNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Aankoop01.txt"), errors);

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
                        hasIsin("US0138721065"), hasWkn(null), hasTicker(null), //
                        hasName("Alcoa Corp"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-21T19:44"), hasShares(100.00), //
                        hasSource("Aankoop01.txt"), //
                        hasNote("Borderel-Ref.: 2025.000123456789 | Ord.-Ref.: 395693"), //
                        hasAmount("EUR", 2047.36), hasGrossValue("EUR", 2004.63), //
                        hasForexGrossValue("USD", 2298.56), //
                        hasTaxes("EUR", (8.04 / 1.146626)), hasFees("EUR", 35.72))));
    }

    @Test
    public void testAankoop01WithSecurityInEUR()
    {
        var security = new Security("Alcoa Corp", "EUR");
        security.setIsin("US0138721065");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new VDKBankNVPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Aankoop01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-21T19:44"), hasShares(100.00), //
                        hasSource("Aankoop01.txt"), //
                        hasNote("Borderel-Ref.: 2025.000123456789 | Ord.-Ref.: 395693"), //
                        hasAmount("EUR", 2047.36), hasGrossValue("EUR", 2004.63), //
                        hasTaxes("EUR", (8.04 / 1.146626)), hasFees("EUR", 35.72), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testVerkoop01()
    {
        var extractor = new VDKBankNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkoop01.txt"), errors);

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
                        hasIsin("BE0974320526"), hasWkn(null), hasTicker(null), //
                        hasName("Umicore NV"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-03T12:45:20"), hasShares(100.00), //
                        hasSource("Verkoop01.txt"), //
                        hasNote("Borderel-Ref.: 2025.000123456789 | Ord.-Ref.: 297671"), //
                        hasAmount("EUR", 852.07), hasGrossValue("EUR", 870.00), //
                        hasTaxes("EUR", 3.05), hasFees("EUR", 9.92 + 4.96))));
    }

    @Test
    public void testVerkoop02()
    {
        var extractor = new VDKBankNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkoop02.txt"), errors);

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
                        hasIsin("NO0010786288"), hasWkn(null), hasTicker(null), //
                        hasName("Norwegian Govt 1,750% 17/02/2027"), //
                        hasCurrencyCode("NOK"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-19T12:44:20"), hasShares(800.00), //
                        hasSource("Verkoop02.txt"), //
                        hasNote("Borderel-Ref.: 2024.000123456789 | Ord.-Ref.: 088618"), //
                        hasAmount("EUR", 6449.78), hasGrossValue("EUR", 6493.67), //
                        hasForexGrossValue("NOK", 76443.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 43.89))));
    }

    @Test
    public void testVerkoop02WithSecurityInEUR()
    {
        var security = new Security("Norwegian Govt 1,750% 17/02/2027", "EUR");
        security.setIsin("NO0010786288");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new VDKBankNVPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkoop02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-19T12:44:20"), hasShares(800.00), //
                        hasSource("Verkoop02.txt"), //
                        hasNote("Borderel-Ref.: 2024.000123456789 | Ord.-Ref.: 088618"), //
                        hasAmount("EUR", 6449.78), hasGrossValue("EUR", 6493.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 43.89), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new VDKBankNVPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("BE0003470755"), hasWkn(null), hasTicker(null), //
                        hasName("Solvay NV"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-17T00:00"), hasShares(100.00), //
                        hasSource("Dividend01.txt"), //
                        hasNote("Borderel-Ref.: 2024.000123456789 | Ord.-Ref.: D046336-758"), //
                        hasAmount("EUR", 113.40), hasGrossValue("EUR", 162.00), //
                        hasTaxes("EUR", 48.60), hasFees("EUR", 0.00))));
    }
}
