package name.abuchen.portfolio.datatransfer.pdf.saxobank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SaxoBankPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class SaxoBankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker("SWDA"), //
                        hasName("iShares Core MSCI World UCITS ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-05T11:21:27"), hasShares(49.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Order-ID 5236807355 | Trade-ID 6093088529"), //
                        hasAmount("CHF", 4869.43), hasGrossValue("CHF", 4850.02), //
                        hasForexGrossValue("USD", 5466.77), //
                        hasTaxes("CHF", 7.29 - 0.02), hasFees("CHF", 12.12 + 0.02))));
    }

    @Test
    public void testWertpapierKauf01WithSecurityInCHF()
    {
        var security = new Security("iShares Core MSCI World UCITS ETF", "CHF");
        security.setIsin("IE00B4L5Y983");
        security.setTickerSymbol("SWDA");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SaxoBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-05T11:21:27"), hasShares(49.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Order-ID 5236807355 | Trade-ID 6093088529"), //
                        hasAmount("CHF", 4869.43), hasGrossValue("CHF", 4850.02), //
                        hasTaxes("CHF", 7.29 - 0.02), hasFees("CHF", 12.12 + 0.02))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker("SWDA"), //
                        hasName("iShares Core MSCI World UCITS ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-05T11:21:27"), hasShares(49.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Order-ID 5236807355 | Trade-ID 6093088529"), //
                        hasAmount("CHF", 4869.43), hasGrossValue("CHF", 4850.02), //
                        hasForexGrossValue("USD", 5466.77), //
                        hasTaxes("CHF", 7.29 - 0.02), hasFees("CHF", 12.12 + 0.02))));
    }

    @Test
    public void testWertpapierKauf02WithSecurityInCHF()
    {
        var security = new Security("iShares Core MSCI World UCITS ETF", "CHF");
        security.setIsin("IE00B4L5Y983");
        security.setTickerSymbol("SWDA");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SaxoBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-05T11:21:27"), hasShares(49.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Order-ID 5236807355 | Trade-ID 6093088529"), //
                        hasAmount("CHF", 4869.43), hasGrossValue("CHF", 4850.02), //
                        hasTaxes("CHF", 7.29 - 0.02), hasFees("CHF", 12.12 + 0.02))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker("SWDA"), //
                        hasName("iShares Core MSCI World UCITS ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-05T10:35:00"), hasShares(214.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Order-ID 5253341230 | Trade-ID 6154473459"), //
                        hasAmount("CHF", 21480.86), hasGrossValue("CHF", 21395.21), //
                        hasForexGrossValue("USD", 23655.07), //
                        hasTaxes("CHF", 32.17 - 0.08), hasFees("CHF", 53.48 + 0.08))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInCHF()
    {
        var security = new Security("iShares Core MSCI World UCITS ETF", "CHF");
        security.setIsin("IE00B4L5Y983");
        security.setTickerSymbol("SWDA");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SaxoBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-05T10:35:00"), hasShares(214.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Order-ID 5253341230 | Trade-ID 6154473459"), //
                        hasAmount("CHF", 21480.86), hasGrossValue("CHF", 21395.21), //
                        hasTaxes("CHF", 32.17 - 0.08), hasFees("CHF", 53.48 + 0.08))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US02079K3059"), hasWkn(null), hasTicker("GOOGL"), //
                        hasName("Alphabet Inc. - A Share"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-07T19:00:21"), hasShares(15.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Order-ID 5555555555 | Trade-ID 6666666666"), //
                        hasAmount("CHF", 1903.60), hasGrossValue("CHF", 1894.49), //
                        hasForexGrossValue("USD", 2200.69), //
                        hasTaxes("CHF", 2.85 - 0.01), hasFees("CHF", 1.52 + 0.01 + 4.73 + 0.01))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInCHF()
    {
        var security = new Security("Alphabet Inc. - A Share", "CHF");
        security.setIsin("US02079K3059");
        security.setTickerSymbol("GOOGL");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SaxoBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-07T19:00:21"), hasShares(15.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Order-ID 5555555555 | Trade-ID 6666666666"), //
                        hasAmount("CHF", 1903.60), hasGrossValue("CHF", 1894.49), //
                        hasTaxes("CHF", 2.85 - 0.01), hasFees("CHF", 1.52 + 0.01 + 4.73 + 0.01))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn(null), hasTicker("VWRL"), //
                        hasName("Vanguard FTSE All-World UCITS ETF"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-20T09:27:39"), hasShares(25.00), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Order-ID 5240614298 | Trade-ID 6107719451"), //
                        hasAmount("CHF", 3057.58), hasGrossValue("CHF", 3050.00), //
                        hasTaxes("CHF", 4.58), hasFees("CHF", 3.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005933956"), hasWkn(null), hasTicker("DJSXE"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-05T12:56"), hasShares(306.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Order-ID 8482757546 | Trade-ID 3805096641"), //
                        hasAmount("CHF", 15972.49), hasGrossValue("CHF", 15908.79), //
                        hasForexGrossValue("EUR", 16955.45), //
                        hasTaxes("CHF", 23.93 - 0.06), hasFees("CHF", 39.83))));
    }

    @Test
    public void testWertpapierKauf06WithSecurityInCHF()
    {
        var security = new Security("DE0005933956", "CHF");
        security.setTickerSymbol("DJSXE");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SaxoBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-05T12:56"), hasShares(306.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Order-ID 8482757546 | Trade-ID 3805096641"), //
                        hasAmount("CHF", 15972.49), hasGrossValue("CHF", 15908.79), //
                        hasTaxes("CHF", 23.93 - 0.06), hasFees("CHF", 39.83))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker("SWDA"), //
                        hasName("iShares Core MSCI World UCITS ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-05T12:47:54"), hasShares(20.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Order-ID 5311230162 | Trade-ID 6358706941"), //
                        hasAmount("CHF", 1947.88), hasGrossValue("CHF", 1940.11), //
                        hasForexGrossValue("USD", 2388.22), //
                        hasTaxes("CHF", 2.92 - 0.01), hasFees("CHF", 4.86))));
    }

    @Test
    public void testWertpapierKauf07WithSecurityInCHF()
    {
        var security = new Security("iShares Core MSCI World UCITS ETF", "CHF");
        security.setIsin("IE00B4L5Y983");
        security.setTickerSymbol("SWDA");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new SaxoBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-05T12:47:54"), hasShares(20.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Order-ID 5311230162 | Trade-ID 6358706941"), //
                        hasAmount("CHF", 1947.88), hasGrossValue("CHF", 1940.11), //
                        hasTaxes("CHF", 2.92 - 0.01), hasFees("CHF", 4.86))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0016999846"), hasWkn(null), hasTicker("CSBGC7"), //
                        hasName(null), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-10-06T13:31:10"), hasShares(5.00), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Order-ID 5330170993 | Trade-ID 6428724964"), //
                        hasAmount("CHF", 376.70), hasGrossValue("CHF", 376.42), //
                        hasTaxes("CHF", 0.28), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0016999846"), hasWkn(null), hasTicker("CSBGC7"), //
                        hasName(null), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-11-03T09:28:44"), hasShares(15.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Order-ID 5339573521 | Trade-ID 6463593307"), //
                        hasAmount("CHF", 1130.17), hasGrossValue("CHF", 1134.02), //
                        hasTaxes("CHF", 0.85), hasFees("CHF", 3.00))));
    }

    @Test
    public void testSecurityBuy01()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US26923G8226"), hasWkn(null), hasTicker("PFFA"), //
                        hasName("Virtus Infracap US Preferred Stock ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-09T19:47:57"), hasShares(49.00), //
                        hasSource("Buy01.txt"), //
                        hasNote("Order ID 5276831204 | Trade ID 6236413100"), //
                        hasAmount("USD", 981.98), hasGrossValue("USD", 980.98), //
                        hasTaxes("USD", 0.00), hasFees("USD", 1.00))));
    }

    @Test
    public void testSecurityBuy02()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US26923G8226"), hasWkn(null), hasTicker("PFFA"), //
                        hasName("Virtus Infracap US Preferred Stock ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-09T19:47:57"), hasShares(49.00), //
                        hasSource("Buy02.txt"), //
                        hasNote("Order ID 5276831204 | Trade ID 6236413100"), //
                        hasAmount("USD", 981.98), hasGrossValue("USD", 980.98), //
                        hasTaxes("USD", 0.00), hasFees("USD", 1.00))));
    }

    @Test
    public void testSecurityBuy03()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy03.txt"), errors);

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
                        hasIsin("FR001400XJJ3"), hasWkn(null), hasTicker(null), //
                        hasName("Republic of France 3.75% 25 May 2056"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-20T08:45:38"), hasShares(100.00), //
                        hasSource("Buy03.txt"), //
                        hasNote("Order ID 5298178479 | Trade ID 6310729230 | Bond Accrued 30,82 EUR"), //
                        hasAmount("EUR", 9584.92), hasGrossValue("EUR", 9534.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 20.00 + 30.82))));
    }

    @Test
    public void testSecurityBuy04()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B6R52259"), hasWkn(null), hasTicker("SSAC_CHF"), //
                        hasName("iShares MSCI ACWI USD Acc UCITS ETF"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-05T12:46:23"), hasShares(14.00), //
                        hasSource("Buy04.txt"), //
                        hasNote("Order ID 5311227095 | Trade ID 6358696418"), //
                        hasAmount("CHF", 1128.22), hasGrossValue("CHF", 1126.53), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 1.69))));
    }

    @Test
    public void testSecurityBuy05()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0016999846"), hasWkn(null), hasTicker("CSBGC7"), //
                        hasName(null), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-05T13:02:36"), hasShares(5.00), //
                        hasSource("Buy05.txt"), //
                        hasNote("Order ID 5311259140 | Trade ID 6358773884"), //
                        hasAmount("CHF", 376.27), hasGrossValue("CHF", 375.99), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.28))));
    }

    @Test
    public void testSecurityBuy06()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4K48X80"), hasWkn(null), hasTicker("IMAE"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-05T13:16:09"), hasShares(1.00), //
                        hasSource("Buy06.txt"), //
                        hasNote("Order ID 6510309204 | Trade ID 4021824604"), //
                        hasAmount("EUR", 87.40), hasGrossValue("EUR", 87.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.10))));

    }

    @Test
    public void testSecurityBuy07()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0016999846"), hasWkn(null), hasTicker("CSBGC7"), //
                        hasName(null), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-05T12:33:36"), hasShares(13.00), //
                        hasSource("Buy07.txt"), //
                        hasNote("Order-ID 5349985997 | Trade-ID 6504011106"), //
                        hasAmount("CHF", 978.77), hasGrossValue("CHF", 978.04), //
                        hasTaxes("CHF", 0.73), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSecurityBuy08()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1781541252"), hasWkn(null), hasTicker("LCUJ"), //
                        hasName("Amundi Core MSCI Japan (Acc) UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-05T12:52:33"), hasShares(2.00), //
                        hasSource("Buy08.txt"), //
                        hasNote("Order ID 5073052313 | Trade ID 9047570613"), //
                        hasAmount("EUR", 38.17), hasGrossValue("EUR", 38.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));
    }

    @Test
    public void testCashTransfer01()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CashTransfer01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-28"), hasAmount("CHF", 4600.00), //
                        hasSource("CashTransfer01.txt"), hasNote("39482097030"))));
    }

    @Test
    public void testCashTransfer02()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CashTransfer02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-06-20"), hasAmount("EUR", 10.00), //
                        hasSource("CashTransfer02.txt"), hasNote("45108148786"))));
    }

    @Test
    public void testCashTransfer03()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CashTransfer03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "PLN");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-06-16"), hasAmount("PLN", 85207.65), //
                        hasSource("CashTransfer03.txt"), hasNote("44951434298"))));
    }

    @Test
    public void testCashTransfer04()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "CashTransfer04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-08-18"), hasAmount("EUR", 3000.00), //
                        hasSource("CashTransfer04.txt"), hasNote("46769031349"))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US09248X1000"), hasWkn(null), hasTicker("BBN"), //
                        hasName("BlackRock Taxable Municipal Bond Trust"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-30T00:00"), hasShares(604.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Event Id 9369584"), //
                        hasAmount("USD", 47.69), hasGrossValue("USD", 56.11), //
                        hasTaxes("USD", 8.42), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US72201B1017"), hasWkn(null), hasTicker("PTY"), //
                        hasName("PIMCO Corporate & Income Opportunity Fund"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-01T00:00"), hasShares(69.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Event Id 9369517"), //
                        hasAmount("USD", 6.97), hasGrossValue("USD", 8.20), //
                        hasTaxes("USD", 1.23), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0237935637"), hasWkn(null), hasTicker("CHDVD"), //
                        hasName("iShares (CH) Swiss Dividend ETF"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-07-17T00:00"), hasShares(75.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Event Id 9413396"), //
                        hasAmount("CHF", 12.67), hasGrossValue("CHF", 19.50), //
                        hasTaxes("CHF", 6.83), hasFees("CHF", 0.00))));
    }

    @Test
    public void testInterest01()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Interest01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-05-01T00:00"), //
                        hasSource("Interest01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 3.32), hasGrossValue("USD", 3.32), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new SaxoBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-26"), hasAmount("CHF", 700.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));
    }
}
