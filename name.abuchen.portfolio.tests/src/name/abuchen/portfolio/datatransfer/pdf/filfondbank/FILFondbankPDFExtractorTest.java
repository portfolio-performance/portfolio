package name.abuchen.portfolio.datatransfer.pdf.filfondbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
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
import name.abuchen.portfolio.datatransfer.pdf.FILFondbankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

public class FILFondbankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

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
                        hasIsin("IE00BJ0KDQ92"), hasWkn("A1XB5U"), hasTicker(null), //
                        hasName("Xtrackers MSCI World UCITS ETF 1C"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-30T00:00"), hasShares(0.263), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftrags-Nr. 2616145223"), //
                        hasAmount("EUR", 26.00), hasGrossValue("EUR", 26.00), //
                        hasForexGrossValue("USD", 27.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf01WithSecurityInEUR()
    {
        var security = new Security("Xtrackers MSCI World UCITS ETF 1C", "EUR");
        security.setIsin("IE00BJ0KDQ92");
        security.setWkn("A1XB5U");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FILFondbankPDFExtractor(client);

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
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-30T00:00"), hasShares(0.263), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftrags-Nr. 2616145223"), //
                        hasAmount("EUR", 26.00), hasGrossValue("EUR", 26.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0731782404"), hasWkn("A1JSY0"), hasTicker(null), //
                        hasName("Fidelity Funds - Global Dividend Fund A QINCOME (EUR)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-24T00:00"), hasShares(2.156), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftrags-Nr. 0263897940"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4X9L533"), hasWkn("A1C9KK"), hasTicker(null), //
                        hasName("HSBC MSCI WORLD ETF"), //
                        hasCurrencyCode("USD"))));
        assertThat(results, hasItem(security( //
                        hasIsin("IE000OJ5TQP4"), hasWkn("A3EB9T"), hasTicker(null), //
                        hasName("Future of Defense UCITS ETF Ac"), //
                        hasCurrencyCode("USD"))));
        assertThat(results, hasItem(security( //
                        hasIsin("EE3600102901"), hasWkn("A0PEF0"), hasTicker(null), //
                        hasName("Avaron Emerging Europe Fund C"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-06-15T11:15:38"), hasShares(9.451), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftrags-Nr. 2667709223"), //
                        hasAmount("EUR", 400.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasForexGrossValue("USD", 461.23))));
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-06-15T11:14:49"), hasShares(22.223), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftrags-Nr. 2667756179"), //
                        hasAmount("EUR", 400.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasForexGrossValue("USD", 461.23))));
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-06-15T00:00"), hasShares(7.711), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftrags-Nr. 2667761620"), //
                        hasAmount("EUR", 400.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00) //
        )));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInEUR()
    {
        var client = new Client();
        addSecurity(client, "IE00B4X9L533", "A1C9KK", "HSBC MSCI WORLD ETF", "EUR");
        addSecurity(client, "IE000OJ5TQP4", "A3EB9T", "Future of Defense UCITS ETF Ac", "EUR");
        addSecurity(client, "EE3600102901", "A0PEF0", "Avaron Emerging Europe Fund C", "EUR");

        var extractor = new FILFondbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);
        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-06-15T11:15:38"), hasShares(9.451), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftrags-Nr. 2667709223"), //
                        hasAmount("EUR", 400.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-06-15T11:14:49"), hasShares(22.223), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftrags-Nr. 2667756179"), //
                        hasAmount("EUR", 400.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-06-15T00:00"), hasShares(7.711), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftrags-Nr. 2667761620"), //
                        hasAmount("EUR", 400.00), hasGrossValue("EUR", 400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    private void addSecurity(Client client, String isin, String wkn, String name, String currency)
    {
        var security = new Security(name, currency);
        security.setIsin(isin);
        security.setWkn(wkn);
        client.addSecurity(security);
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

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
                        hasIsin("LU0731782404"), hasWkn("A1JSY0"), hasTicker(null), //
                        hasName("Fidelity Funds - Global Dividend Fund A QINCOME (EUR)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-17T00:00"), hasShares(2.156), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftrags-Nr. 8364413825"), //
                        hasAmount("EUR", 50.30), hasGrossValue("EUR", 50.36), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung01()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0446734526"), hasWkn("A0X97T"), hasTicker(null), //
                        hasName("UBS Msci Pacific exJap.U ETF A"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B9M6SJ31"), hasWkn("A1W02Q"), hasTicker(null), //
                        hasName("ISHS Gbl.Corp.Bd.EU Hgd.U.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-16"), hasShares(0.045), //
                        hasSource("Fondabrechnung01.txt"), hasNote("Auftrags-Nr. 2536717769 | Splitkauf"), //
                        hasAmount("EUR", 1.77), hasGrossValue("EUR", 1.77), //
                        hasForexGrossValue("USD", 1.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-05-01"), hasShares(0.009), //
                        hasSource("Fondabrechnung01.txt"), hasNote("Auftrags-Nr. 2536921666 | Wiederanlage"), //
                        hasAmount("EUR", 0.96), hasGrossValue("EUR", 0.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung01WithSecurityInEUR()
    {
        var security1 = new Security("UBS Msci Pacific exJap.U ETF A", "EUR");
        security1.setIsin("LU0446734526");
        security1.setWkn("A0X97T");

        var client = new Client();
        client.addSecurity(security1);

        var extractor = new FILFondbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B9M6SJ31"), hasWkn("A1W02Q"), hasTicker(null), //
                        hasName("ISHS Gbl.Corp.Bd.EU Hgd.U.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-04-16"), hasShares(0.045), //
                        hasSource("Fondabrechnung01.txt"), hasNote("Auftrags-Nr. 2536717769 | Splitkauf"), //
                        hasAmount("EUR", 1.77), hasGrossValue("EUR", 1.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-05-01"), hasShares(0.009), //
                        hasSource("Fondabrechnung01.txt"), hasNote("Auftrags-Nr. 2536921666 | Wiederanlage"), //
                        hasAmount("EUR", 0.96), hasGrossValue("EUR", 0.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung02()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0629459743"), hasWkn("A1JA1R"), hasTicker(null), //
                        hasName("UBS-MSCI WLD.SOC.RESP.A.D.E."), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1048313891"), hasWkn("A110QD"), hasTicker(null), //
                        hasName("UBS Emer.Mkt.Soc.Resp.UETFADIS"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-09-03"), hasShares(2.417), //
                        hasSource("Fondabrechnung02.txt"), hasNote("Auftrags-Nr. 2539808855"), //
                        hasAmount("EUR", 210.00), hasGrossValue("EUR", 209.48), //
                        hasForexGrossValue("USD", 229.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.52))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-09-03"), hasShares(7.877), //
                        hasSource("Fondabrechnung02.txt"), hasNote("Auftrags-Nr. 2539808856"), //
                        hasAmount("EUR", 90.00), hasGrossValue("EUR", 89.56), //
                        hasForexGrossValue("USD", 98.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.44))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-10-02"), hasShares(2.409), //
                        hasSource("Fondabrechnung02.txt"), hasNote("Auftrags-Nr. 2540401210"), //
                        hasAmount("EUR", 210.00), hasGrossValue("EUR", 209.48), //
                        hasForexGrossValue("USD", 228.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.52))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-10-02"), hasShares(7.728), //
                        hasSource("Fondabrechnung02.txt"), hasNote("Auftrags-Nr. 2540401213"), //
                        hasAmount("EUR", 90.00), hasGrossValue("EUR", 89.56), //
                        hasForexGrossValue("USD", 97.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.44))));

        // check 5th buy sell transaction... its a sell with account fee
        assertThat(results, hasItem(sale( //
                        hasDate("2019-10-15"), hasShares(0.011), //
                        hasSource("Fondabrechnung02.txt"), hasNote("Auftrags-Nr. 2540818151 | Entgeltbelastung"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.01), //
                        hasForexGrossValue("USD", 1.12), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-10-15"), //
                        hasShares(0.011), //
                        hasSource("Fondabrechnung02.txt"), //
                        hasNote("Auftrags-Nr. 2540818151 | Verwahrentgelt Fonds 2019"), //
                        hasAmount("EUR", 1.01), hasGrossValue("EUR", 1.01), //
                        hasForexGrossValue("USD", 1.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung02WithSecurityInEUR()
    {
        var security1 = new Security("UBS-MSCI WLD.SOC.RESP.A.D.E.", "EUR");
        security1.setIsin("LU0629459743");
        security1.setWkn("A1JA1R");

        var security2 = new Security("UBS Emer.Mkt.Soc.Resp.UETFADIS", "EUR");
        security2.setIsin("LU1048313891");
        security2.setWkn("A110QD");

        var client = new Client();
        client.addSecurity(security1);
        client.addSecurity(security2);

        var extractor = new FILFondbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-09-03"), hasShares(2.417), //
                        hasSource("Fondabrechnung02.txt"), hasNote("Auftrags-Nr. 2539808855"), //
                        hasAmount("EUR", 210.00), hasGrossValue("EUR", 209.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.52))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-09-03"), hasShares(7.877), //
                        hasSource("Fondabrechnung02.txt"), hasNote("Auftrags-Nr. 2539808856"), //
                        hasAmount("EUR", 90.00), hasGrossValue("EUR", 89.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.44))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-10-02"), hasShares(2.409), //
                        hasSource("Fondabrechnung02.txt"), hasNote("Auftrags-Nr. 2540401210"), //
                        hasAmount("EUR", 210.00), hasGrossValue("EUR", 209.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.52))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-10-02"), hasShares(7.728), //
                        hasSource("Fondabrechnung02.txt"), hasNote("Auftrags-Nr. 2540401213"), //
                        hasAmount("EUR", 90.00), hasGrossValue("EUR", 89.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.44))));

        // check 5th buy sell transaction... its a sell with account fee
        assertThat(results, hasItem(sale( //
                        hasDate("2019-10-15"), hasShares(0.011), //
                        hasSource("Fondabrechnung02.txt"), hasNote("Auftrags-Nr. 2540818151 | Entgeltbelastung"), //
                        hasAmount("EUR", 1.00), hasGrossValue("EUR", 1.01), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-10-15"), //
                        hasShares(0.011), //
                        hasSource("Fondabrechnung02.txt"), //
                        hasNote("Auftrags-Nr. 2540818151 | Verwahrentgelt Fonds 2019"), //
                        hasAmount("EUR", 1.01), hasGrossValue("EUR", 1.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung03()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung03.txt"), errors);

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
                        hasIsin("DE0009779884"), hasWkn("977988"), hasTicker(null), //
                        hasName("Amundi Aktien Rohstoffe"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-09"), hasShares(4), //
                        hasSource("Fondabrechnung03.txt"), hasNote("Auftrags-Nr. 2568216341"), //
                        hasAmount("EUR", 464.00), hasGrossValue("EUR", 464.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung04()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung04.txt"), errors);

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
                        hasIsin("LU0048578792"), hasWkn("973270"), hasTicker(null), //
                        hasName("Fidelity F.- Europ.Growth F. A"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-09"), hasShares(1.049), //
                        hasSource("Fondabrechnung04.txt"), hasNote("Auftrags-Nr. 2539291225 | Wiederanlage"), //
                        hasAmount("EUR", 15.67), hasGrossValue("EUR", 15.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung05()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung05.txt"), errors);

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
                        hasIsin("IE00B3RBWM25"), hasWkn("A1JX52"), hasTicker(null), //
                        hasName("Vanguard FTSE AllWorld UETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-01-15"), hasShares(0.266), //
                        hasSource("Fondabrechnung05.txt"), hasNote("Auftrags-Nr. 2534379721 | Wiederanlage"), //
                        hasAmount("USD", 20.83), hasGrossValue("USD", 20.83), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testFondabrechnung06()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung06.txt"), errors);

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
                        hasIsin("IE00B3RBWM25"), hasWkn("A1JX52"), hasTicker(null), //
                        hasName("Vanguard FTSE AllWorld UETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-01-14"), hasShares(87.557), //
                        hasSource("Fondabrechnung06.txt"), hasNote("Auftrags-Nr. 2534364118"), //
                        hasAmount("EUR", 6000.00), hasGrossValue("EUR", 5980.86), //
                        hasForexGrossValue("USD", 6800.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 8.97 + 10.17))));
    }

    @Test
    public void testFondabrechnung06WithSecurityInEUR()
    {
        var security = new Security("Vanguard FTSE AllWorld UETF", "EUR");
        security.setIsin("IE00B3RBWM25");
        security.setWkn("A1JX52");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FILFondbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung06.txt"), errors);

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
                        hasDate("2019-01-14"), hasShares(87.557), //
                        hasSource("Fondabrechnung06.txt"), hasNote("Auftrags-Nr. 2534364118"), //
                        hasAmount("EUR", 6000.00), hasGrossValue("EUR", 5980.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 8.97 + 10.17), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testFondabrechnung07()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung07.txt"), errors);

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
                        hasIsin("AT0000A1QA38"), hasWkn("A2DHT6"), hasTicker(null), //
                        hasName("Own 360 Standortfds Österr. A"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-11-03T16:51:24"), hasShares(0.316), //
                        hasSource("Fondabrechnung07.txt"), hasNote("Auftrags-Nr. 2581216"), //
                        hasAmount("EUR", 30.00), hasGrossValue("EUR", 30.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung08()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(17));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0075056555"), hasWkn("986932"), hasTicker(null), //
                        hasName("BGF World Mining Fund A2 USD"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009807016"), hasWkn("980701"), hasTicker(null), //
                        hasName("hausInvest"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0048578792"), hasWkn("973270"), hasTicker(null), //
                        hasName("Fidelity F.- Europ.Growth F. A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0114760746"), hasWkn("941034"), hasTicker(null), //
                        hasName("Templeton Growth (Euro) Fund"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0088814487"), hasWkn("988525"), hasTicker(null), //
                        hasName("Fid Fd - Euro Blue Chip Fd A"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-01"), hasShares(0.363), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2567655380 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.50), //
                        hasForexGrossValue("USD", 19.50 * 1.175902), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-01"), hasShares(0.459), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2567655380 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-01"), hasShares(1.145), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2567655380 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-01"), hasShares(1.022), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2567655380 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.54))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-01"), hasShares(0.739), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2567655380 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(0.349), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2568789861 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.52), //
                        hasForexGrossValue("USD", 19.52 * 1.180651), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.48))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(0.459), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2568789861 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.47))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(1.135), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2568789861 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(1.031), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2568789861 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.53))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(0.733), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2568789861 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.52))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-10"), hasShares(0.307), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2569249320 | Wiederanlage"), //
                        hasAmount("EUR", 5.37), hasGrossValue("EUR", 5.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-10"), hasShares(0.09), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2569273184 | Wiederanlage"), //
                        hasAmount("EUR", 2.43), hasGrossValue("EUR", 2.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung08WithSecurityInEUR()
    {
        var security1 = new Security("BGF World Mining Fund A2 USD", "EUR");
        security1.setIsin("LU0075056555");
        security1.setWkn("986932");

        var client = new Client();
        client.addSecurity(security1);

        var extractor = new FILFondbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(16));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009807016"), hasWkn("980701"), hasTicker(null), //
                        hasName("hausInvest"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0048578792"), hasWkn("973270"), hasTicker(null), //
                        hasName("Fidelity F.- Europ.Growth F. A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0114760746"), hasWkn("941034"), hasTicker(null), //
                        hasName("Templeton Growth (Euro) Fund"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0088814487"), hasWkn("988525"), hasTicker(null), //
                        hasName("Fid Fd - Euro Blue Chip Fd A"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-01"), hasShares(0.363), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2567655380 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-01"), hasShares(0.459), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2567655380 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-01"), hasShares(1.145), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2567655380 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-01"), hasShares(1.022), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2567655380 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.54))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-01"), hasShares(0.739), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2567655380 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check 6th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(0.349), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2568789861 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.48))));

        // check 7th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(0.459), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2568789861 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.47))));

        // check 8th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(1.135), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2568789861 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check 9th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(1.031), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2568789861 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.53))));

        // check 10th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-02"), hasShares(0.733), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2568789861 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.52))));

        // check 11th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-10"), hasShares(0.307), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2569249320 | Wiederanlage"), //
                        hasAmount("EUR", 5.37), hasGrossValue("EUR", 5.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 12th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-10"), hasShares(0.09), //
                        hasSource("Fondabrechnung08.txt"), hasNote("Auftrags-Nr. 2569273184 | Wiederanlage"), //
                        hasAmount("EUR", 2.43), hasGrossValue("EUR", 2.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung09()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008488214"), hasWkn("848821"), hasTicker(null), //
                        hasName("UBS(D) Equity Fd.-Global Opp."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0049112450"), hasWkn("973285"), hasTicker(null), //
                        hasName("Fidelity Fd-Pacific Fund A USD"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0323578657"), hasWkn("A0M430"), hasTicker(null), //
                        hasName("FvS SICAV-Multiple Opportun. R"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0M8HD2"), hasWkn("A0M8HD"), hasTicker(null), //
                        hasName("Frankfurter Aktienfd.für Stift"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B39R2S49"), hasWkn("A0Q349"), hasTicker(null), //
                        hasName("M&G Global Dividend Fund A EUR"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-10"), hasShares(4.633), //
                        hasSource("Fondabrechnung09.txt"), hasNote("Auftrags-Nr. 2526343587"), //
                        hasAmount("EUR", 897.60), hasGrossValue("EUR", 921.60), //
                        hasTaxes("EUR", 21.15 + 1.16 + 1.69), hasFees("EUR", 0.00))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-10"), hasShares(22.211), //
                        hasSource("Fondabrechnung09.txt"), hasNote("Auftrags-Nr. 2526343590"), //
                        hasAmount("EUR", 637.32), hasGrossValue("EUR", 650.26), //
                        hasForexGrossValue("USD", 755.62), //
                        hasTaxes("EUR", 11.40 + 0.63 + 0.91), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-11"), hasShares(3.772), //
                        hasSource("Fondabrechnung09.txt"), hasNote("Auftrags-Nr. 2526343592"), //
                        hasAmount("EUR", 880.19), hasGrossValue("EUR", 888.53), //
                        hasTaxes("EUR", 7.35 + 0.40 + 0.59), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-10"), hasShares(6.367), //
                        hasSource("Fondabrechnung09.txt"), hasNote("Auftrags-Nr. 2526343593"), //
                        hasAmount("EUR", 807.73), hasGrossValue("EUR", 816.89), //
                        hasTaxes("EUR", 8.07 + 0.44 + 0.65), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-10"), hasShares(29.419), //
                        hasSource("Fondabrechnung09.txt"), hasNote("Auftrags-Nr. 2526343595"), //
                        hasAmount("EUR", 749.75), hasGrossValue("EUR", 783.40), //
                        hasTaxes("EUR", 29.65 + 1.63 + 2.37), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung09WithSecurityInEUR()
    {
        var security2 = new Security("Fidelity Fd-Pacific Fund A USD", "EUR");
        security2.setIsin("LU0049112450");
        security2.setWkn("973285");

        var client = new Client();
        client.addSecurity(security2);

        var extractor = new FILFondbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008488214"), hasWkn("848821"), hasTicker(null), //
                        hasName("UBS(D) Equity Fd.-Global Opp."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0323578657"), hasWkn("A0M430"), hasTicker(null), //
                        hasName("FvS SICAV-Multiple Opportun. R"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0M8HD2"), hasWkn("A0M8HD"), hasTicker(null), //
                        hasName("Frankfurter Aktienfd.für Stift"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B39R2S49"), hasWkn("A0Q349"), hasTicker(null), //
                        hasName("M&G Global Dividend Fund A EUR"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-10"), hasShares(4.633), //
                        hasSource("Fondabrechnung09.txt"), hasNote("Auftrags-Nr. 2526343587"), //
                        hasAmount("EUR", 897.60), hasGrossValue("EUR", 921.60), //
                        hasTaxes("EUR", 21.15 + 1.16 + 1.69), hasFees("EUR", 0.00))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-10"), hasShares(22.211), //
                        hasSource("Fondabrechnung09.txt"), hasNote("Auftrags-Nr. 2526343590"), //
                        hasAmount("EUR", 637.32), hasGrossValue("EUR", 650.26), //
                        hasTaxes("EUR", 11.40 + 0.63 + 0.91), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-11"), hasShares(3.772), //
                        hasSource("Fondabrechnung09.txt"), hasNote("Auftrags-Nr. 2526343592"), //
                        hasAmount("EUR", 880.19), hasGrossValue("EUR", 888.53), //
                        hasTaxes("EUR", 7.35 + 0.40 + 0.59), hasFees("EUR", 0.00))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-10"), hasShares(6.367), //
                        hasSource("Fondabrechnung09.txt"), hasNote("Auftrags-Nr. 2526343593"), //
                        hasAmount("EUR", 807.73), hasGrossValue("EUR", 816.89), //
                        hasTaxes("EUR", 8.07 + 0.44 + 0.65), hasFees("EUR", 0.00))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-10-10"), hasShares(29.419), //
                        hasSource("Fondabrechnung09.txt"), hasNote("Auftrags-Nr. 2526343595"), //
                        hasAmount("EUR", 749.75), hasGrossValue("EUR", 783.40), //
                        hasTaxes("EUR", 29.65 + 1.63 + 2.37), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung10()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0227384020"), hasWkn("A0HF3W"), hasTicker(null), //
                        hasName("Nordea Stable Return BP EUR"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0325074333"), hasWkn("A0M5KA"), hasTicker(null), //
                        hasName("JPM-HIGHB.US ST. A DIST EO"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0289228842"), hasWkn("A0MNZ3"), hasTicker(null), //
                        hasName("JPM-Europe Eq Plus A dist-EUR"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-09-07"), hasShares(53.781), //
                        hasSource("Fondabrechnung10.txt"), hasNote("Auftrags-Nr. 2525653390"), //
                        hasAmount("EUR", 883.62), hasGrossValue("EUR", 883.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-09-07"), hasShares(43.326), //
                        hasSource("Fondabrechnung10.txt"), hasNote("Auftrags-Nr. 2525653392"), //
                        hasAmount("EUR", 776.84), hasGrossValue("EUR", 776.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-09-07"), hasShares(49.436), //
                        hasSource("Fondabrechnung10.txt"), hasNote("Auftrags-Nr. 2525653393"), //
                        hasAmount("EUR", 694.58), hasGrossValue("EUR", 694.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung11()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(10L));
        assertThat(countBuySell(results), is(10L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(30));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0002635265"), hasWkn("263526"), hasTicker(null), //
                        hasName("iShares Pfandbriefe (DE)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B02KXM00"), hasWkn("A0DK61"), hasTicker(null), //
                        hasName("iShares EURO STOXX Small U ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B0M63284"), hasWkn("A0HGV5"), hasTicker(null), //
                        hasName("ISHS-EUR.PROP.YLD.UC.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B2QWCY14"), hasWkn("A0Q1YY"), hasTicker(null), //
                        hasName("iShs S&P SmallCap 600 UCITS ET"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3QL0Y14"), hasWkn("A1JJAD"), hasTicker(null), //
                        hasName("Global Short Fixed I.F.EUR Dis"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B42THM37"), hasWkn("A1JH9Z"), hasTicker(null), //
                        hasName("Emerging Mark.Value F.EUR Dis"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B5KQNG97"), hasWkn("A1C19C"), hasTicker(null), //
                        hasName("HSBC S&P 500 ETF"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B945VV12"), hasWkn("A1T8FS"), hasTicker(null), //
                        hasName("Vanguard FTSE Devel.Euro.UETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B95PGT31"), hasWkn("A1T8FU"), hasTicker(null), //
                        hasName("Vanguard FTSE Japan UCITS ETF"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B9M6SJ31"), hasWkn("A1W02Q"), hasTicker(null), //
                        hasName("ISHS Gbl.Corp.Bd.EU Hgd.U.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.004), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.43), hasGrossValue("EUR", 0.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 1st security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.004), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.43), hasGrossValue("EUR", 0.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2st security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.003), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 2st security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.003), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.002), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 3rd security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.002), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.001), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.08), //
                        hasForexGrossValue("USD", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 4th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.001), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.08), //
                        hasForexGrossValue("USD", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.043), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.43), hasGrossValue("EUR", 0.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 5th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.043), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.43), hasGrossValue("EUR", 0.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.025), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.26), hasGrossValue("EUR", 0.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 6th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.025), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.26), hasGrossValue("EUR", 0.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.006), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.15), hasGrossValue("EUR", 0.15), //
                        hasForexGrossValue("USD", 0.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 7th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.006), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.15), hasGrossValue("EUR", 0.15), //
                        hasForexGrossValue("USD", 0.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.006), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.17), hasGrossValue("EUR", 0.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 8th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.006), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.17), hasGrossValue("EUR", 0.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-02"), hasShares(0.002), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasForexGrossValue("USD", 0.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 9th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-02"), //
                        hasShares(0.002), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasForexGrossValue("USD", 0.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-02"), hasShares(0.001), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.11), hasGrossValue("EUR", 0.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 10th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-02"), //
                        hasShares(0.001), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.11), hasGrossValue("EUR", 0.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung11WithSecurityInEUR()
    {
        var security4 = new Security("iShs S&P SmallCap 600 UCITS ET", "EUR");
        security4.setIsin("IE00B2QWCY14");
        security4.setWkn("A0Q1YY");

        var security7 = new Security("HSBC S&P 500 ETF", "EUR");
        security7.setIsin("IE00B5KQNG97");
        security7.setWkn("A1C19C");

        var security9 = new Security("Vanguard FTSE Japan UCITS ETF", "EUR");
        security9.setIsin("IE00B95PGT31");
        security9.setWkn("A1T8FU");

        var client = new Client();
        client.addSecurity(security4);
        client.addSecurity(security7);
        client.addSecurity(security9);

        var extractor = new FILFondbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(10L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(27));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0002635265"), hasWkn("263526"), hasTicker(null), //
                        hasName("iShares Pfandbriefe (DE)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B02KXM00"), hasWkn("A0DK61"), hasTicker(null), //
                        hasName("iShares EURO STOXX Small U ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B0M63284"), hasWkn("A0HGV5"), hasTicker(null), //
                        hasName("ISHS-EUR.PROP.YLD.UC.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3QL0Y14"), hasWkn("A1JJAD"), hasTicker(null), //
                        hasName("Global Short Fixed I.F.EUR Dis"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B42THM37"), hasWkn("A1JH9Z"), hasTicker(null), //
                        hasName("Emerging Mark.Value F.EUR Dis"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B945VV12"), hasWkn("A1T8FS"), hasTicker(null), //
                        hasName("Vanguard FTSE Devel.Euro.UETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B9M6SJ31"), hasWkn("A1W02Q"), hasTicker(null), //
                        hasName("ISHS Gbl.Corp.Bd.EU Hgd.U.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check 1st security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.004), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.43), hasGrossValue("EUR", 0.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 1st security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.004), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.43), hasGrossValue("EUR", 0.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2st security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.003), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 2st security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.003), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.002), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 3rd security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.002), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 4th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.001), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 4th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.001), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 5th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.043), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.43), hasGrossValue("EUR", 0.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 5th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.043), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.43), hasGrossValue("EUR", 0.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 6th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.025), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.26), hasGrossValue("EUR", 0.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 6th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.025), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.26), hasGrossValue("EUR", 0.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 7th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.006), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.15), hasGrossValue("EUR", 0.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 7th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.006), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.15), hasGrossValue("EUR", 0.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 8th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-01"), hasShares(0.006), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.17), hasGrossValue("EUR", 0.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 8th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-01"), //
                        hasShares(0.006), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.17), hasGrossValue("EUR", 0.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 9th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-02"), hasShares(0.002), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 9th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-02"), //
                        hasShares(0.002), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 10th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-07-02"), hasShares(0.001), //
                        hasSource("Fondabrechnung11.txt"), hasNote("Auftrags-Nr. 2538422391 | Entgeltbelastung"), //
                        hasAmount("EUR", 0.11), hasGrossValue("EUR", 0.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees in 10th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-07-02"), //
                        hasShares(0.001), //
                        hasSource("Fondabrechnung11.txt"), //
                        hasNote("Auftrags-Nr. 2538422391 | lfd. Vermögensverwaltungsentgelt"), //
                        hasAmount("EUR", 0.11), hasGrossValue("EUR", 0.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung12()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009807016"), hasWkn("980701"), hasTicker(null), //
                        hasName("hausInvest"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-01-02"), hasShares(0.466), //
                        hasSource("Fondabrechnung12.txt"), hasNote("Auftrags-Nr. 2530389235 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.50))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-01-02"), hasShares(0.597), //
                        hasSource("Fondabrechnung12.txt"), hasNote("Auftrags-Nr. 2531092602 | Entgeltbelastung"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-01-02"), //
                        hasShares(0.597), //
                        hasSource("Fondabrechnung12.txt"), //
                        hasNote("Auftrags-Nr. 2531092602 | Depotführungsentgelt 2018"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung13()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(6L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0075056555"), hasWkn("986932"), hasTicker(null), //
                        hasName("BGF World Mining Fund A2 USD"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009807016"), hasWkn("980701"), hasTicker(null), //
                        hasName("hausInvest"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0048578792"), hasWkn("973270"), hasTicker(null), //
                        hasName("Fidelity F.- Europ.Growth F. A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0114760746"), hasWkn("941034"), hasTicker(null), //
                        hasName("Templeton Growth (Euro) Fund"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0088814487"), hasWkn("988525"), hasTicker(null), //
                        hasName("Fid Fd - Euro Blue Chip Fd A"), //
                        hasCurrencyCode("EUR"))));

        // check 1st security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-04"), hasShares(1.026), //
                        hasSource("Fondabrechnung13.txt"), hasNote("Auftrags-Nr. 2490116288 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.51), //
                        hasForexGrossValue("USD", 19.51 * 1.070478), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.49))));

        // check 2nd security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-04"), hasShares(0.474), //
                        hasSource("Fondabrechnung13.txt"), hasNote("Auftrags-Nr. 2490116288 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.47))));

        // check 3rd security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-04"), hasShares(1.449), //
                        hasSource("Fondabrechnung13.txt"), hasNote("Auftrags-Nr. 2490116288 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check 4th security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-04"), hasShares(1.272), //
                        hasSource("Fondabrechnung13.txt"), hasNote("Auftrags-Nr. 2490116288 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.54))));

        // check 5th security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-04"), hasShares(0.899), //
                        hasSource("Fondabrechnung13.txt"), hasNote("Auftrags-Nr. 2490116288 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check 6th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-01-04"), hasShares(1.634), //
                        hasSource("Fondabrechnung13.txt"), hasNote("Auftrags-Nr. 2490658604 | Entgeltbelastung"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // // check fees in 6th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2016-01-04"), //
                        hasShares(1.634), //
                        hasSource("Fondabrechnung13.txt"), //
                        hasNote("Auftrags-Nr. 2490658604 | Depotführungsentgelt"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondabrechnung13WithSecurityInEUR()
    {
        var security1 = new Security("BGF World Mining Fund A2 USD", "EUR");
        security1.setIsin("LU0075056555");
        security1.setWkn("986932");

        var client = new Client();
        client.addSecurity(security1);

        var extractor = new FILFondbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondabrechnung13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(6L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009807016"), hasWkn("980701"), hasTicker(null), //
                        hasName("hausInvest"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0048578792"), hasWkn("973270"), hasTicker(null), //
                        hasName("Fidelity F.- Europ.Growth F. A"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0114760746"), hasWkn("941034"), hasTicker(null), //
                        hasName("Templeton Growth (Euro) Fund"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0088814487"), hasWkn("988525"), hasTicker(null), //
                        hasName("Fid Fd - Euro Blue Chip Fd A"), //
                        hasCurrencyCode("EUR"))));

        // check 1st security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-04"), hasShares(1.026), //
                        hasSource("Fondabrechnung13.txt"), hasNote("Auftrags-Nr. 2490116288 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.49))));

        // check 2nd security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-04"), hasShares(0.474), //
                        hasSource("Fondabrechnung13.txt"), hasNote("Auftrags-Nr. 2490116288 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.47))));

        // check 3rd security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-04"), hasShares(1.449), //
                        hasSource("Fondabrechnung13.txt"), hasNote("Auftrags-Nr. 2490116288 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check 4th security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-04"), hasShares(1.272), //
                        hasSource("Fondabrechnung13.txt"), hasNote("Auftrags-Nr. 2490116288 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.54))));

        // check 5th security buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-04"), hasShares(0.899), //
                        hasSource("Fondabrechnung13.txt"), hasNote("Auftrags-Nr. 2490116288 | Splitkauf"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 19.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.51))));

        // check 6th security buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-01-04"), hasShares(1.634), //
                        hasSource("Fondabrechnung13.txt"), hasNote("Auftrags-Nr. 2490658604 | Entgeltbelastung"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // // check fees in 6th security buy sell transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2016-01-04"), //
                        hasShares(1.634), //
                        hasSource("Fondabrechnung13.txt"), //
                        hasNote("Auftrags-Nr. 2490658604 | Depotführungsentgelt"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

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
                        hasIsin("IE00B2QWCY14"), hasWkn("A0Q1YY"), hasTicker(null), //
                        hasName("iShs S&P SmallCap 600 UCITS ET"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-07-25"), hasExDate("2018-07-12"), //
                        hasShares(0.093), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Turnus halbjährlich"), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

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
                        hasIsin("LU0048578792"), hasWkn("973270"), hasTicker(null), //
                        hasName("Fidelity F.- Europ.Growth F. A"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-08-08"), hasExDate("2019-08-01"), //
                        hasShares(10.299), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Turnus jährlich"), //
                        hasAmount("EUR", 15.67), hasGrossValue("EUR", 15.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new FILFondbankPDFExtractor(new Client());

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
                        hasIsin("LU0731782826"), hasWkn("A1JSY2"), hasTicker(null), //
                        hasName("Fil. Fd-Gbl.Div.Fd.A Mln.EUR"), //
                        hasCurrencyCode("EUR"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-07-08T00:00"), hasExDate("2022-07-01T00:00"), hasShares(427.272), //
                        hasSource("Dividende03.txt"), hasNote("Turnus monatlich"), //
                        hasAmount("EUR", 15.46), hasGrossValue("EUR", 19.23), //
                        hasTaxes("EUR", 3.29 + 0.18 + 0.30), hasFees("EUR", 0.00))));
    }
}
