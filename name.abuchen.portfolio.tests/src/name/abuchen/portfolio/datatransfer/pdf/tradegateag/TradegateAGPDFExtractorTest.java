package name.abuchen.portfolio.datatransfer.pdf.tradegateag;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
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

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TradegateAGPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class TradegateAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

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
                        hasIsin("IE00BM67HN09"), hasWkn("A113FG"), hasTicker(null), //
                        hasName("Xtr.(IE)-MSCI Wrld Con.Staples Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-04T11:04:53"), hasShares(3.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Order-/Ref.nr. 9876543 | Limit 43,2500 EUR"), //
                        hasAmount("EUR", 129.75), hasGrossValue("EUR", 129.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BM67HN09"), hasWkn("A113FG"), hasTicker(null), //
                        hasName("Xtr.(IE)-MSCI Wrld Con.Staples Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-06-05T09:22:27"), hasShares(3.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Order-/Ref.nr. 9876543 | Limit 43,6600 EUR"), //
                        hasAmount("EUR", 130.75), hasGrossValue("EUR", 130.98), //
                        hasTaxes("EUR", 0.22 + 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

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
                        hasIsin("IE00BVZ6SP04"), hasWkn("A14PHG"), hasTicker(null), //
                        hasName("PFI ETFs-EO Sh.Mat.UC.ETF Registered Shares EUR Acc.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-11-13T20:33:19"), hasShares(10.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Order-/Ref.nr. 8321468 | Limit 102,8600 EUR"), //
                        hasAmount("EUR", 1028.40), hasGrossValue("EUR", 1028.67), //
                        hasTaxes("EUR", 0.24 + 0.01 + 0.02), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

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
                        hasIsin("IE00BVZ6SP04"), hasWkn("A14PHG"), hasTicker(null), //
                        hasName("PFI ETFs-EO Sh.Mat.UC.ETF Registered Shares EUR Acc.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-07-18T16:22:36"), hasShares(10.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Order-/Ref.nr. 87947243 | Limit 105,0840 EUR"), //
                        hasAmount("EUR", 1050.90), hasGrossValue("EUR", 1050.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BZ163G84"), hasWkn("A143JK"), hasTicker(null), //
                        hasName("Vanguard EUR Corp.Bond U.ETF Registered Shares EUR Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-26T00:00"), hasShares(76.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Order-/Ref.nr. 9876543"), //
                        hasAmount("EUR", 7.16), hasGrossValue("EUR", 9.72), //
                        hasTaxes("EUR", 2.43 + 0.13), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BZ163L38"), hasWkn("A143JQ"), hasTicker(null), //
                        hasName("Vang.USD Em.Mkts Gov.Bd U.ETF Registered Shares USD Dis.oN"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-09-25T00:00"), hasShares(59.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Order-/Ref.nr. 9876543"), //
                        hasAmount("EUR", 7.21), hasGrossValue("EUR", 9.79), //
                        hasForexGrossValue("USD", 10.96), //
                        hasTaxes("EUR", (2.74 + 0.15) / 1.11856), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        var security = new Security("Vang.USD Em.Mkts Gov.Bd U.ETF Registered Shares USD Dis.oN", "EUR");
        security.setIsin("IE00BZ163L38");
        security.setWkn("A143JQ");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradegateAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-09-25T00:00"), hasShares(59.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Order-/Ref.nr. 9876543"), //
                        hasAmount("EUR", 7.21), hasGrossValue("EUR", 9.79), //
                        hasTaxes("EUR", (2.74 + 0.15) / 1.11856), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000S9YS762"), hasWkn("A3D7VW"), hasTicker(null), //
                        hasName("Linde plc Registered Shares EO -,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-09-18T00:00"), hasShares(3.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Order-/Ref.nr. 35894402"), //
                        hasAmount("EUR", 2.81), hasGrossValue("EUR", 3.81), //
                        hasForexGrossValue("USD", 4.50), //
                        hasTaxes("EUR", (0.95 + 0.05)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("Linde plc Registered Shares EO -,001", "EUR");
        security.setIsin("IE000S9YS762");
        security.setWkn("A3D7VW");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradegateAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-09-18T00:00"), hasShares(3.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Order-/Ref.nr. 35894402"), //
                        hasAmount("EUR", 2.81), hasGrossValue("EUR", 3.81), //
                        hasTaxes("EUR", (0.95 + 0.05)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

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
                        hasIsin("DE0006070006"), hasWkn("607000"), hasTicker(null), //
                        hasName("HOCHTIEF AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-07-07T00:00"), hasShares(12.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Order-/Ref.nr. 35894402"), //
                        hasAmount("EUR", 46.21), hasGrossValue("EUR", 62.76), ////
                        hasTaxes("EUR", (15.69 + 0.86)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

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
                        hasIsin("US0382221051"), hasWkn("865177"), hasTicker(null), //
                        hasName("Applied Materials Inc. Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(6.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Order-/Ref.nr. 35894402"), //
                        hasAmount("EUR", 1.87), hasGrossValue("EUR", 2.20), //
                        hasForexGrossValue("USD", 2.40), //
                        hasTaxes("EUR", 0.33), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        var security = new Security("Applied Materials Inc. Registered Shares o.N.", "EUR");
        security.setIsin("US0382221051");
        security.setWkn("865177");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new TradegateAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-03-13T00:00"), hasShares(6.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Order-/Ref.nr. 35894402"), //
                        hasAmount("EUR", 1.87), hasGrossValue("EUR", 2.20), //
                        hasTaxes("EUR", 0.33), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BVZ6SP04"), hasWkn("A14PHG"), hasTicker(null), //
                        hasName("PFI ETFs-EO Sh.Mat.UC.ETF Registered Shares EUR Acc.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2025-01-02T00:00"), hasShares(220.00), //
                                        hasSource("Vorabpauschale01.txt"), //
                                        hasNote("Order-/Ref.nr. 11522032"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testSteuerausgleichsrechnung01()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerausgleichsrechnung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check taxes transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-01-14T00:00"), hasShares(0), //
                        hasSource("Steuerausgleichsrechnung01.txt"), //
                        hasNote("Order-/Ref.nr. 73385152 | 13.01.2025 - 14.01.2025"), //
                        hasAmount("EUR", 0.31), hasGrossValue("EUR", 0.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerausgleichsrechnung02()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerausgleichsrechnung02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-12-03T00:00"), hasShares(0), //
                        hasSource("Steuerausgleichsrechnung02.txt"), //
                        hasNote("Order-/Ref.nr. 9876543 | 07.11.2024 - 03.12.2024"), //
                        hasAmount("EUR", 0.44), hasGrossValue("EUR", 0.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerausgleichsrechnung03()
    {
        var extractor = new TradegateAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerausgleichsrechnung03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check tax Refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-11-03T00:00"), hasShares(0), //
                        hasSource("Steuerausgleichsrechnung03.txt"), //
                        hasNote("Order-/Ref.nr. 9876543 | 31.10.2025 - 03.11.2025"), //
                        hasAmount("EUR", 0.18), hasGrossValue("EUR", 0.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
