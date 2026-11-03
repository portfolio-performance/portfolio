package name.abuchen.portfolio.datatransfer.pdf.postbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.PostbankPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class PostbankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new PostbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

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
                        hasIsin("IE00BJ0KDQ92"), hasWkn("A1XB5U"), hasTicker(null), //
                        hasName("XTR.(IE) - MSCI WORLD REGISTERED SHARES 1C O.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0635178014"), hasWkn("ETF127"), hasTicker(null), //
                        hasName("COMSTA.-MSCI EM.MKTS.TRN U.ETF INHABER-ANTEILE I O.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-04T08:00:04"), hasShares(158.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Limit 63,00 EUR"), //
                        hasAmount("EUR", 9978.18), hasGrossValue("EUR", 9925.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (39.95 + 0.04 + 11.82 + 0.65)))));

        // check 2nd purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-04T08:00:04"), hasShares(140.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Limit 43,00 EUR"), //
                        hasAmount("EUR", 6062.19), hasGrossValue("EUR", 6014.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (39.95 + 0.04 + 7.15 + 0.65)))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new PostbankPDFExtractor(new Client());

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
                        hasIsin("IE00BZ0PKT83"), hasWkn("A14YPA"), hasTicker(null), //
                        hasName("ISHSIV-EDGE MSCI WO.MULT.U.ETF REGISTERED SHARES USD (ACC)O.N"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-05T00:00"), hasShares(36.7701), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer 242816/24.00"), //
                        hasAmount("EUR", 250.90), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new PostbankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn("A1JX52"), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL-WORLD U.ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-01T00:00"), hasShares(12.4085), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftragsnummer 123456/26.00"), //
                        hasAmount("EUR", 1000.90), hasGrossValue("EUR", 1000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new PostbankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00BD8HBD32"), hasWkn("A2AN2J"), hasTicker(null), //
                        hasName("CIVITAS SOCIAL HOUSING PLC REGISTERED SHARES LS -,01"), //
                        hasCurrencyCode("GBP"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-29T17:35:23"), hasShares(1700.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftragsnummer 69123/22.01 | Limit 0,84 GBP"), //
                        hasAmount("EUR", 1765.16), hasGrossValue("EUR", 1690.20), //
                        hasForexGrossValue("GBP", 1407.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (42.95 + 16.86 + 15.15)))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInEUR()
    {
        var security = new Security("CIVITAS SOCIAL HOUSING PLC REGISTERED SHARES LS -,01", "EUR");
        security.setIsin("GB00BD8HBD32");
        security.setWkn("A2AN2J");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostbankPDFExtractor(client);

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
        new AssertImportActions().check(results, "EUR");

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-29T17:35:23"), hasShares(1700.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftragsnummer 69123/22.01 | Limit 0,84 GBP"), //
                        hasAmount("EUR", 1765.16), hasGrossValue("EUR", 1690.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (42.95 + 16.86 + 15.15)), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new PostbankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0009769760"), hasWkn("976976"), hasTicker(null), //
                        hasName("DWS ESG TOP ASIEN INHABER-ANTEILE LC"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-24T00:00"), hasShares(88), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Belegnummer 1481234555 / 987123"), //
                        hasAmount("EUR", 17169.09), hasGrossValue("EUR", 16588.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 580.61))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new PostbankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1681042609"), hasWkn("A2H567"), hasTicker(null), //
                        hasName("AIS-M.EUR.ESG BR.CTB AEOA FUNDS"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-23T09:04"), hasShares(22), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Belegnummer 1222222222 / 22212322"), //
                        hasAmount("EUR", 7441.35), hasGrossValue("EUR", 7400.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (39.95 + 0.6)))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new PostbankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0328475792"), hasWkn("DBX1A7"), hasTicker(null), //
                        hasName("XTRACKERS STOXX EUROPE 600 INH.ANT.1C O.N."), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-04-07T08:00:50"), hasShares(0.6526), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Belegnummer 9867409933 / 344685043"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new PostbankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7427181091"), hasWkn("852062"), hasTicker(null), //
                        hasName("PROCTER & GAMBLE CO., THE RG.SH. O.N."), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-06-01T15:34:02"), hasShares(14), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Belegnummer 1603718970 / 128935901"), //
                        hasAmount("EUR", 1720.95), hasGrossValue("EUR", 1699.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (17.95 + 3.12)))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new PostbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

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
                        hasIsin("DE000DB9WSZ7"), hasWkn("DB9WSZ"), hasTicker(null), //
                        hasName("3,1% DT.BANK FESTZINSANL.V.26 9.10. 28"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-10-06T00:00:00"), hasShares(30), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Belegnummer 7654421234 / 8989898"), //
                        hasAmount("EUR", 3015.00), hasGrossValue("EUR", 3015.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new PostbankPDFExtractor(new Client());

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
                        hasIsin("IE00BF2B0K52"), hasWkn("A2DTF1"), hasTicker(null), //
                        hasName("FRAN.LIBERTYQ EM.MAR.EQ.UC.ETF REGISTERED SHARES USD ACC.O.N."), //
                        hasCurrencyCode("EUR"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-03-11T16:34:51"), hasShares(137.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer 724152/50.00 | Limit 23,15 EUR"), //
                        hasAmount("EUR", 3141.58), hasGrossValue("EUR", 3171.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (29.95 + 0.02)))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new PostbankPDFExtractor(new Client());

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
                        hasIsin("US64110L1061"), hasWkn("552484"), hasTicker(null), //
                        hasName("NETFLIX INC. REGISTERED SHARES DL -,001"), //
                        hasCurrencyCode("EUR"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-12-13T10:35:11"), hasShares(10.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftragsnummer 12345678999/50.00 | Limit bestens"), //
                        hasAmount("EUR", 1094.73), hasGrossValue("EUR", 1151.50), //
                        hasTaxes("EUR", (42.45 + 2.33 + 3.39)), hasFees("EUR", (7.95 + 0.65)))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new PostbankPDFExtractor(new Client());

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
                        hasIsin("US53578A1088"), hasWkn("A1H82D"), hasTicker(null), //
                        hasName("LINKEDIN CORP. REGISTERED SHS CL.A DL-,0001"), //
                        hasCurrencyCode("USD"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-12-14T00:00:00"), hasShares(7.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftragsnummer 12345678/32.00 | Barabfindung wegen Fusion"), //
                        hasAmount("EUR", 1279.69), hasGrossValue("EUR", 1289.64), //
                        hasForexGrossValue("USD", 1372.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.95))));
    }

    @Test
    public void testWertpapierVerkauf03WithSecurityInEUR()
    {
        var security = new Security("LINKEDIN CORP. REGISTERED SHS CL.A DL-,0001", "EUR");
        security.setIsin("US53578A1088");
        security.setWkn("A1H82D");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-12-14T00:00:00"), hasShares(7.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftragsnummer 12345678/32.00 | Barabfindung wegen Fusion"), //
                        hasAmount("EUR", 1279.69), hasGrossValue("EUR", 1289.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.95), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new PostbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

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
                        hasIsin("LU0274208692"), hasWkn("DBX1MW"), hasTicker(null), //
                        hasName("XTRACKERS MSCI WORLD SWAP INHABER-ANTEILE 1C O.N."), //
                        hasCurrencyCode("EUR"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-01-14T00:00:00"), hasShares(0.7918), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftragsnummer 123456/37.00"), //
                        hasAmount("EUR", 49.01), hasGrossValue("EUR", 49.19), //
                        hasTaxes("EUR", (0.17 + 0.01)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new PostbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

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
                        hasIsin("GB00BWNPZS82"), hasWkn("A42KQC"), hasTicker(null), //
                        hasName("ANR.A.PENNON GROUP PLC 26. 10.26"), //
                        hasCurrencyCode("GBP"))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-10-20T11:33:30"), hasShares(93.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Belegnummer 8695653127 / 338317982"), //
                        hasAmount("EUR", 78.10), hasGrossValue("EUR", 88.05), //
                        hasForexGrossValue("GBP", 74.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.95))));
    }

    @Test
    public void testWertpapierVerkauf05WithSecurityInEUR()
    {
        var security = new Security("ANR.A.PENNON GROUP PLC 26. 10.26", "EUR");
        security.setIsin("GB00BWNPZS82");
        security.setWkn("A42KQC");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-10-20T11:33:30"), hasShares(93.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Belegnummer 8695653127 / 338317982"), //
                        hasAmount("EUR", 78.10), hasGrossValue("EUR", 88.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.95), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testSammelabrechnungKaufVerkauf01()
    {
        var extractor = new PostbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SammelabrechnungKaufVerkauf01.txt"),
                        errors);

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
                        hasIsin("IE00BZ163K21"), hasWkn("A143JM"), hasTicker(null), //
                        hasName("VANGUARD USD CORPORATE B.U.ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKX55T58"), hasWkn("A12CX1"), hasTicker(null), //
                        hasName("VANG.FTSE DEVELOP.WORLD U.ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3VVMM84"), hasWkn("A1JX51"), hasTicker(null), //
                        hasName("VANGUARD FTSE EM.MARKETS U.ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B945VV12"), hasWkn("A1T8FS"), hasTicker(null), //
                        hasName("VANGUARD FTSE DEV.EUROPE U.ETF REGISTERED SHARES EUR DIS.ON"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF4RFH31"), hasWkn("A2DWBY"), hasTicker(null), //
                        hasName("ISHSIII-MSCI WLD SM.CA.UCI.ETF REGISTERED SHARES USD(ACC)O.N."), //
                        hasCurrencyCode("EUR"))));

        // check 1st purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-05T00:00"), hasShares(3.6027), //
                        hasSource("SammelabrechnungKaufVerkauf01.txt"), //
                        hasNote("Auftragsnummer 189617/56.00"), //
                        hasAmount("EUR", 180.90), hasGrossValue("EUR", 180.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check 2nd purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-05T00:00"), hasShares(8.6892), //
                        hasSource("SammelabrechnungKaufVerkauf01.txt"), //
                        hasNote("Auftragsnummer 189663/67.00"), //
                        hasAmount("EUR", 525.90), hasGrossValue("EUR", 525.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check 3rd purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-05T00:00"), hasShares(9.2308), //
                        hasSource("SammelabrechnungKaufVerkauf01.txt"), //
                        hasNote("Auftragsnummer 189751/60.00"), //
                        hasAmount("EUR", 480.90), hasGrossValue("EUR", 480.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check 4th purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-05T00:00"), hasShares(5.6693), //
                        hasSource("SammelabrechnungKaufVerkauf01.txt"), //
                        hasNote("Auftragsnummer 189908/99.00"), //
                        hasAmount("EUR", 180.90), hasGrossValue("EUR", 180.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        // check 5th purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-05T00:00"), hasShares(29.9897), //
                        hasSource("SammelabrechnungKaufVerkauf01.txt"), //
                        hasNote("Auftragsnummer 190945/98.00"), //
                        hasAmount("EUR", 145.90), hasGrossValue("EUR", 145.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));
    }

    @Test
    public void testDividende01()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

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
                        hasIsin("US4781601046"), hasWkn("853260"), hasTicker(null), //
                        hasName("JOHNSON & JOHNSON  SHARES REGISTERED SHARES DL 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-09"), hasExDate("2021-02-22T00:00"), //
                        hasShares(12.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abrechnungsnr. 12345678999 | Quartalsdividende"), //
                        hasAmount("EUR", 8.64), hasGrossValue("EUR", 10.17), //
                        hasForexGrossValue("USD", 12.12), //
                        hasTaxes("EUR", 1.53), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("JOHNSON & JOHNSON  SHARES REGISTERED SHARES DL 1", "EUR");
        security.setIsin("US4781601046");
        security.setWkn("853260");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

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
                        hasDate("2021-03-09"), hasExDate("2021-02-22T00:00"), //
                        hasShares(12.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abrechnungsnr. 12345678999 | Quartalsdividende"), //
                        hasAmount("EUR", 8.64), hasGrossValue("EUR", 10.17), //
                        hasTaxes("EUR", 1.53), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende02()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

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
                        hasIsin("DE0006231004"), hasWkn("623100"), hasTicker(null), //
                        hasName("INFINEON TECHNOLOGIES AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-02"), hasExDate("2021-02-26T00:00"), //
                        hasShares(20.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungsnr. 56024913320"), //
                        hasAmount("EUR", 4.40), hasGrossValue("EUR", 4.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

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
                        hasIsin("DE0005557508"), hasWkn("555750"), hasTicker(null), //
                        hasName("DEUTSCHE TELEKOM AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-04-08"), hasExDate("2021-04-06T00:00"), //
                        hasShares(114.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr. 12345678901"), //
                        hasAmount("EUR", 68.40), hasGrossValue("EUR", 68.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

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
                        hasIsin("DE000A0Q4R36"), hasWkn("A0Q4R3"), hasTicker(null), //
                        hasName("ISH.ST.EU.600 HEALT.C.U.ETF DE INHABER-ANLAGEAKTIEN"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-04-15"), hasExDate("2016-04-15T00:00"), //
                        hasShares(20.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 64628421310"), //
                        hasAmount("EUR", 19.25), hasGrossValue("EUR", 19.54), //
                        hasTaxes("EUR", 0.29), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

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
                        hasIsin("IE00B1FZS350"), hasWkn("A0LEW8"), hasTicker(null), //
                        hasName("ISHSII-DEV.MKTS PROP.YLD U.ETF REGISTERED SHS USD (DIST) O.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-02-24"), hasExDate("2021-02-11T00:00"), //
                        hasShares(81.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Abrechnungsnr. 55626672580"), //
                        hasAmount("EUR", 9.65), hasGrossValue("EUR", 9.65), //
                        hasForexGrossValue("USD", 11.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        var security = new Security("ISHSII-DEV.MKTS PROP.YLD U.ETF REGISTERED SHS USD (DIST) O.N.", "EUR");
        security.setIsin("IE00B1FZS350");
        security.setWkn("A0LEW8");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostbankPDFExtractor(client);

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
                        hasDate("2021-02-24"), hasExDate("2021-02-11T00:00"), //
                        hasShares(81.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Abrechnungsnr. 55626672580"), //
                        hasAmount("EUR", 9.65), hasGrossValue("EUR", 9.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende06()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

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
                        hasIsin("US03027X1000"), hasWkn("A1JRLA"), hasTicker(null), //
                        hasName("AMERICAN TOWER CORP. REGISTERED SHARES DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-01-14"), hasExDate("2021-12-23T00:00"), //
                        hasShares(120.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Abrechnungsnr. 11111111111"), //
                        hasAmount("EUR", 124.02), hasGrossValue("EUR", 145.91), //
                        hasForexGrossValue("USD", 166.80), //
                        hasTaxes("EUR", 21.89), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        var security = new Security("AMERICAN TOWER CORP. REGISTERED SHARES DL -,01", "EUR");
        security.setIsin("US03027X1000");
        security.setWkn("A1JRLA");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

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
                        hasDate("2022-01-14"), hasExDate("2021-12-23T00:00"), //
                        hasShares(120.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Abrechnungsnr. 11111111111"), //
                        hasAmount("EUR", 124.02), hasGrossValue("EUR", 145.91), //
                        hasTaxes("EUR", 21.89), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende07()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

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
                        hasIsin("LU0029871042"), hasWkn("971663"), hasTicker(null), //
                        hasName("FR.TEMP.INV.FDS -T.GL.BD FD NAMENS-ANTEILE A(MDIS.)USD O.N"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-08-16"), hasExDate("2022-08-08T00:00"), //
                        hasShares(1430.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Abrechnungsnr. 72953370000"), //
                        hasAmount("EUR", 74.38), hasGrossValue("EUR", 101.02), //
                        hasForexGrossValue("USD", 102.96), //
                        hasTaxes("EUR", (12.63 + 0.69 + 12.63 + 0.69)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07WithSecurityInEUR()
    {
        var security = new Security("FR.TEMP.INV.FDS -T.GL.BD FD NAMENS-ANTEILE A(MDIS.)USD O.N", "EUR");
        security.setIsin("LU0029871042");
        security.setWkn("971663");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

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
                        hasDate("2022-08-16"), hasExDate("2022-08-08T00:00"), //
                        hasShares(1430.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Abrechnungsnr. 72953370000"), //
                        hasAmount("EUR", 74.38), hasGrossValue("EUR", 101.02), //
                        hasTaxes("EUR", (12.63 + 0.69 + 12.63 + 0.69)), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende08()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

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
                        hasIsin("GB00BD8HBD32"), hasWkn("A2AN2J"), hasTicker(null), //
                        hasName("CIVITAS SOCIAL HOUSING PLC REGISTERED SHARES LS -,01"), //
                        hasCurrencyCode("GBP"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-09"), hasExDate("2022-08-18T00:00"), //
                        hasShares(1700.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Abrechnungsnr. 11223344550"), //
                        hasAmount("EUR", 17.76), hasGrossValue("EUR", 22.20), //
                        hasForexGrossValue("GBP", 19.38), //
                        hasTaxes("EUR", 4.44), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08WithSecurityInEUR()
    {
        var security = new Security("CIVITAS SOCIAL HOUSING PLC REGISTERED SHARES LS -,01", "EUR");
        security.setIsin("GB00BD8HBD32");
        security.setWkn("A2AN2J");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

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
                        hasDate("2022-09-09"), hasExDate("2022-08-18T00:00"), //
                        hasShares(1700.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Abrechnungsnr. 11223344550"), //
                        hasAmount("EUR", 17.76), hasGrossValue("EUR", 22.20), //
                        hasTaxes("EUR", 4.44), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende09()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

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
                        hasIsin("XS0177089298"), hasWkn("908043"), hasTicker(null), //
                        hasName("ENEL FINANCE INTL N.V. EO-MEDIUM-TERM NOTES 2003(23)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-29"), hasExDate(null), //
                        hasShares(150.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Abrechnungsnr. 77122332030 | Zinsschein 365 Tag(e)"), //
                        hasAmount("EUR", 579.80), hasGrossValue("EUR", 787.50), //
                        hasTaxes("EUR", (98.44 + 5.41 + 98.44 + 5.41)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

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
                        hasIsin("US500769JD71"), hasWkn("A2YNRB"), hasTicker(null), //
                        hasName("KREDITANST.F.WIEDERAUFBAU DL-ANLEIHE V.19(29)"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-14"), hasExDate(null), //
                        hasShares(190.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Abrechnungsnr. 75805627080 | Zinsschein 180 Tag(e)"), //
                        hasAmount("EUR", 122.44), hasGrossValue("EUR", 166.30), //
                        hasForexGrossValue("USD", 166.25), //
                        hasTaxes("EUR", (20.79 + 1.14 + 20.79 + 1.14)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10WithSecurityInEUR()
    {
        var security = new Security("KREDITANST.F.WIEDERAUFBAU DL-ANLEIHE V.19(29)", "EUR");
        security.setIsin("US500769JD71");
        security.setWkn("A2YNRB");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

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
                        hasDate("2022-09-14"), hasExDate(null), //
                        hasShares(190.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Abrechnungsnr. 75805627080 | Zinsschein 180 Tag(e)"), //
                        hasAmount("EUR", 122.44), hasGrossValue("EUR", 166.30), //
                        hasTaxes("EUR", (20.79 + 1.14 + 20.79 + 1.14)), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende11()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

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
                        hasIsin("IE00BZ163K21"), hasWkn("A143JM"), hasTicker(null), //
                        hasName("VANGUARD USD CORPORATE B.U.ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-12-04"), hasExDate("2019-11-21T00:00"), //
                        hasShares(13.3024), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Abrechnungsnr. 83904157190"), //
                        hasAmount("EUR", 1.56), hasGrossValue("EUR", 1.56), //
                        hasForexGrossValue("USD", 1.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11WithSecurityInEUR()
    {
        var security = new Security("VANGUARD USD CORPORATE B.U.ETF REGISTERED SHARES USD DIS.ON", "EUR");
        security.setIsin("IE00BZ163K21");
        security.setWkn("A143JM");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

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
                        hasDate("2019-12-04"), hasExDate("2019-11-21T00:00"), //
                        hasShares(13.3024), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Abrechnungsnr. 83904157190"), //
                        hasAmount("EUR", 1.56), hasGrossValue("EUR", 1.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende12()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

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
                        hasIsin("XS1014610254"), hasWkn("A0JCCZ"), hasTicker(null), //
                        hasName("2,625% VOLKSWAGEN LEASING MTN.V.14 15.1. 24"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-01-16"), hasExDate("2023-01-16T00:00"), //
                        hasShares(160.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 309.23), hasGrossValue("EUR", 420.00), //
                        hasTaxes("EUR", (105.00 + 5.77)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende13()
    {
        var client = new Client();

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

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
                        hasIsin("US46625HKC33"), hasWkn("JPM4DQ"), hasTicker(null), //
                        hasName("3,125% JPMORGAN CHASE & CO.NT.15 23.J/J 01:25"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-01-25"), hasExDate("2023-01-23T00:00"), //
                        hasShares(200.00), //
                        hasSource("Dividende13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 210.44), hasGrossValue("EUR", 285.83), //
                        hasForexGrossValue("USD", 312.50), //
                        hasTaxes("EUR", (71.46 + 3.93)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende13WithSecurityInEUR()
    {
        var security = new Security("3,125% JPMORGAN CHASE & CO.NT.15 23.J/J 01:25", "EUR");
        security.setIsin("US46625HKC33");
        security.setWkn("JPM4DQ");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

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
                        hasDate("2023-01-25"), hasExDate("2023-01-23T00:00"), //
                        hasShares(200.00), //
                        hasSource("Dividende13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 210.44), hasGrossValue("EUR", 285.83), //
                        hasTaxes("EUR", (71.46 + 3.93)), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividendeStorno01()
    {
        var extractor = new PostbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno01.txt"), errors);

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
                        hasIsin("US6819361006"), hasWkn("890454"), hasTicker(null), //
                        hasName("OMEGA HEALTHCARE INVEST. INC.RG.SH. DL -,10"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2024-05-17"), hasExDate(null), hasShares(60.00), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 31.42), hasGrossValue("EUR", 36.96), //
                                        hasForexGrossValue("USD", 40.20), //
                                        hasTaxes("EUR", 5.54), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividendeStorno01WithSecurityInEUR()
    {
        var security = new Security("OMEGA HEALTHCARE INVEST. INC.RG.SH. DL -,10", "EUR");
        security.setIsin("US6819361006");
        security.setWkn("890454");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new PostbankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2024-05-17"), hasExDate(null), hasShares(60.00), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 31.42), hasGrossValue("EUR", 36.96), //
                                        hasTaxes("EUR", 5.54), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new PostbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

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
                        hasIsin("DE0009769760"), hasWkn("976976"), hasTicker(null), //
                        hasName("DWS ESG TOP ASIEN INHABER-ANTEILE LC"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-01-02T00:00"), hasShares(88), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.41), hasGrossValue("EUR", 26.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        var extractor = new PostbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BK5BQT80"), hasWkn("A2PKXG"), hasTicker(null), //
                        hasName("VANG.FTSE A.W. DLA FUNDS"), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, taxes( //
                                        hasDate("2026-01-15"), hasShares(70), //
                                        hasSource("Vorabpauschale02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDepotauszug01()
    {
        var extractor = new PostbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-08-01"), hasAmount("EUR", 2.70), //
                        hasSource("Depotauszug01.txt"), hasNote("Dauerauftrag"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-08-01"), hasAmount("EUR", 1100.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Dauerauftrag"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-08-01"), hasAmount("EUR", 600.00), //
                        hasSource("Depotauszug01.txt"), hasNote("SEPA Überweisungsgutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-08-07"), hasAmount("EUR", 300.90), //
                        hasSource("Depotauszug01.txt"), hasNote("SEPA Überweisungsgutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-09-02"), hasAmount("EUR", 2.70), //
                        hasSource("Depotauszug01.txt"), hasNote("Dauerauftrag"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-09-02"), hasAmount("EUR", 1100.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Dauerauftrag"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-09-26"), hasAmount("EUR", 5000.00), //
                        hasSource("Depotauszug01.txt"), hasNote("SEPA Überweisungsgutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-09-26"), hasAmount("EUR", 2500.00), //
                        hasSource("Depotauszug01.txt"), hasNote("SEPA Überweisungsgutschrift"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-10-01"), hasAmount("EUR", 960.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Dauerauftrag"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-10-01"), hasAmount("EUR", 5.40), //
                        hasSource("Depotauszug01.txt"), hasNote("Dauerauftrag"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2019-08-06"), hasAmount("EUR", 250.00), //
                        hasSource("Depotauszug01.txt"), hasNote("SEPA Überweisungslastschrift"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2019-08-06"), hasAmount("EUR", 49.10), //
                        hasSource("Depotauszug01.txt"), hasNote("SEPA Überweisungslastschrift"))));
    }

    @Test
    public void testDepotauszug02()
    {
        var extractor = new PostbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug02.txt"), errors);

        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-08-01"), hasAmount("EUR", 1100.00), //
                        hasSource("Depotauszug02.txt"), hasNote("SEPA Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-08-01"), hasAmount("EUR", 8.00), //
                        hasSource("Depotauszug02.txt"), hasNote("SEPA Lastschrifteinzug"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-08-01"), hasAmount("EUR", 19.47), //
                        hasSource("Depotauszug02.txt"), hasNote("SEPA Lastschrifteinzug"))));
    }
}
