package name.abuchen.portfolio.datatransfer.pdf.alpian;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
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
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.AlpianPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class AlpianPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new AlpianPDFExtractor(new Client());

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
                        hasIsin("IE00B4K48X80"), hasWkn(null), hasTicker(null), //
                        hasName("iShares Core MSCI Europe E"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-12T10:19:19"), hasShares(1.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Ref.-Nr.: SCTRSC23346K8NG6"), //
                        hasAmount("CHF", 67.82), hasGrossValue("CHF", 67.72), //
                        hasForexGrossValue("EUR", 71.37), //
                        hasTaxes("CHF", 0.11 / (71.48 / 67.82)), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf01WithSecurityInCHF()
    {
        var security = new Security("iShares Core MSCI Europe E", "CHF");
        security.setIsin("IE00B4K48X80");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new AlpianPDFExtractor(client);

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
                        hasDate("2023-12-12T10:19:19"), hasShares(1.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Ref.-Nr.: SCTRSC23346K8NG6"), //
                        hasAmount("CHF", 67.82), hasGrossValue("CHF", 67.72), //
                        hasTaxes("CHF", 0.11 / (71.48 / 67.82)), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new AlpianPDFExtractor(new Client());

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
                        hasIsin("CH0445689208"), hasWkn(null), hasTicker(null), //
                        hasName("21Shares Crypto Basket Ind"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-08-04T12:36:59"), hasShares(31.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Ref.-Nr.: SCTRSC26216Q9PFQ"), //
                        hasAmount("CHF", 293.16), hasGrossValue("CHF", 292.94), //
                        hasTaxes("CHF", 0.22), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new AlpianPDFExtractor(new Client());

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
                        hasIsin("IE00B4K48X80"), hasWkn(null), hasTicker(null), //
                        hasName("iShares Core MSCI Europe E"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-10-19T08:12:34"), hasShares(20.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ref.-Nr.: SCTRSC2329278Z05"), //
                        hasAmount("CHF", 1253.18), hasGrossValue("CHF", 1255.06), //
                        hasForexGrossValue("EUR", 1326.70), //
                        hasTaxes("CHF", 1.99 / (1324.71 / 1253.18)), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01WithSecurityInCHF()
    {
        var security = new Security("iShares Core MSCI Europe E", "CHF");
        security.setIsin("IE00B4K48X80");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new AlpianPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

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
        assertThat(results, hasItem(sale( //
                        hasDate("2023-10-19T08:12:34"), hasShares(20.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ref.-Nr.: SCTRSC2329278Z05"), //
                        hasAmount("CHF", 1253.18), hasGrossValue("CHF", 1255.06), //
                        hasTaxes("CHF", 1.99 / (1324.71 / 1253.18)), hasFees("CHF", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new AlpianPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK7Y2Q41"), hasWkn(null), hasTicker(null), //
                        hasName("iShares USD Corp Bond"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-08-04T10:48:55"), hasShares(88.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Ref.-Nr.: SCTRSC26216LBS6Y"), //
                        hasAmount("CHF", 381.74), hasGrossValue("CHF", 382.31), //
                        hasTaxes("CHF", 0.57), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new AlpianPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0016999846"), hasWkn(null), hasTicker(null), //
                        hasName("iShares Swiss Dom Govt Bd"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-01-23T00:00"), hasExDate("2025-01-21T00:00"), //
                        hasShares(3.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1.13), hasGrossValue("CHF", 1.74), //
                        hasTaxes("CHF", 0.61), hasFees("CHF", 0.00))));
    }

    @Test
    public void testGebuehrenBelastung01()
    {
        var extractor = new AlpianPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GebuehrenBelastung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2025-04-01T00:00"), //
                        hasSource("GebuehrenBelastung01.txt"), //
                        hasNote("Period: JAN 2025 - MAR 2025"), //
                        hasAmount("CHF", 76.37), hasGrossValue("CHF", 76.37), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new AlpianPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-15"), hasAmount("CHF", 1.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Domestic Clearing (DD)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-05-23"), hasAmount("CHF", 29999.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Domestic Clearing (DD)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-05-23"), hasAmount("CHF", 30000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Debit Internal Transfer"))));
    }

    @Test
    public void testDepotauszug01()
    {
        var extractor = new AlpianPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(7L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "CHF");

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        dividend( //
                                        hasDate("2026-07-23T00:00"), hasExDate(null), //
                                        hasShares(4.00), //
                                        hasSource("Depotauszug01.txt"), //
                                        hasNote("iShares Swiss Dom Govt Bd 3-7"), //
                                        hasAmount("CHF", 2.56), hasGrossValue("CHF", 2.56), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        dividend( //
                                        hasDate("2026-07-23T00:00"), hasExDate(null), //
                                        hasShares(3.00), //
                                        hasSource("Depotauszug01.txt"), //
                                        hasNote("iShares Core CHF Corporate Bond"), //
                                        hasAmount("CHF", 1.74), hasGrossValue("CHF", 1.74), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        dividend( //
                                        hasDate("2026-07-23T00:00"), hasExDate(null), //
                                        hasShares(5.00), //
                                        hasSource("Depotauszug01.txt"), //
                                        hasNote("iShares Swiss Dom Govt Bd 0-3"), //
                                        hasAmount("CHF", 4.30), hasGrossValue("CHF", 4.30), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        taxes( //
                                        hasDate("2026-07-23T00:00"), //
                                        hasSource("Depotauszug01.txt"), //
                                        hasNote("iShares Swiss Dom Govt Bd 3-7"), //
                                        hasAmount("CHF", 0.90), hasGrossValue("CHF", 0.90), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        taxes( //
                                        hasDate("2026-07-23T00:00"), //
                                        hasSource("Depotauszug01.txt"), //
                                        hasNote("iShares Core CHF Corporate Bond"), //
                                        hasAmount("CHF", 0.61), hasGrossValue("CHF", 0.61), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        taxes( //
                                        hasDate("2026-07-23T00:00"), //
                                        hasSource("Depotauszug01.txt"), //
                                        hasNote("iShares Swiss Dom Govt Bd 0-3"), //
                                        hasAmount("CHF", 1.51), hasGrossValue("CHF", 1.51), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        fee( //
                                        hasDate("2026-07-01T00:00"), //
                                        hasSource("Depotauszug01.txt"), //
                                        hasNote("Fees posted from 202604 to 202606"), //
                                        hasAmount("CHF", 91.72), hasGrossValue("CHF", 91.72), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));
    }

    @Test
    public void testDepotauszug02()
    {
        var extractor = new AlpianPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(6L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(6L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "CHF");

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        purchase( //
                                        hasDate("2026-08-04T00:00"), hasShares(31.00), //
                                        hasSource("Depotauszug02.txt"), //
                                        hasNote("21Shares Crypto Basket Index-ETP"), //
                                        hasAmount("CHF", 293.16), hasGrossValue("CHF", 293.16), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        sale( //
                                        hasDate("2026-08-04T00:00"), hasShares(5.00), //
                                        hasSource("Depotauszug02.txt"), //
                                        hasNote("Xtrackers Glb Infl-Lnkd Bd"), //
                                        hasAmount("CHF", 433.95), hasGrossValue("CHF", 433.95), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        purchase( //
                                        hasDate("2026-08-04T00:00"), hasShares(5.00), //
                                        hasSource("Depotauszug02.txt"), //
                                        hasNote("iShares Core CHF Corporate-Bond"), //
                                        hasAmount("CHF", 483.68), hasGrossValue("CHF", 483.68), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        sale( //
                                        hasDate("2026-08-04T00:00"), hasShares(88.00), //
                                        hasSource("Depotauszug02.txt"), //
                                        hasNote("iShares USD Corp Bond"), //
                                        hasAmount("CHF", 381.74), hasGrossValue("CHF", 381.74), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        sale( //
                                        hasDate("2026-08-27T00:00"), hasShares(12.00), //
                                        hasSource("Depotauszug02.txt"), //
                                        hasNote("Xtrackers MSCI Emerging-Markets"), //
                                        hasAmount("CHF", 909.43), hasGrossValue("CHF", 909.43), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        purchase( //
                                        hasDate("2026-08-27T00:00"), hasShares(18.00), //
                                        hasSource("Depotauszug02.txt"), //
                                        hasNote("Vanguard FTSE Dev-AsiaPacexJpn"), //
                                        hasAmount("CHF", 853.88), hasGrossValue("CHF", 853.88), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));
    }
}
