package name.abuchen.portfolio.datatransfer.pdf.unicredit;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
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
import name.abuchen.portfolio.datatransfer.pdf.UnicreditPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class UnicreditPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new UnicreditPDFExtractor(new Client());

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
                        hasIsin("FR0000120578"), hasWkn("920657"), hasTicker(null), //
                        hasName("SANOFI S.A. ACTIONS PORT. EO 2"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-02-15T16:28:03"), hasShares(22.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Ref.-Nr.: 20160215WAB1861426155"), //
                        hasAmount("EUR", 1560.83), hasGrossValue("EUR", 1547.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.27 + 3.09 + 10.21))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new UnicreditPDFExtractor(new Client());

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
                        hasIsin("LU0565135745"), hasWkn("A1JQVV"), hasTicker(null), //
                        hasName("FIRST EAGLE AMUNDI-INTERNATIO. ACTIONS NOM. AE-C O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-20T03:53:15"), hasShares(25.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Ref.-Nr.: 20210421WAB1799275492"), //
                        hasAmount("EUR", 4963.46), hasGrossValue("EUR", 4803.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 159.96))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new UnicreditPDFExtractor(new Client());

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
                        hasIsin("US0605051046"), hasWkn("858388"), hasTicker(null), //
                        hasName("BANK OF AMERICA CORP. REGISTERED SHARES DL 0,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01T15:30"), hasShares(460.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Ref.-Nr.: 20220404WAB1406946452"), //
                        hasAmount("EUR", 17454.91), hasGrossValue("EUR", 17454.91), //
                        hasForexGrossValue("USD", 19200.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInEUR()
    {
        var security = new Security("BANK OF AMERICA CORP. REGISTERED SHARES DL 0,01", "EUR");
        security.setIsin("US0605051046");
        security.setWkn("858388");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new UnicreditPDFExtractor(client);

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
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-04-01T15:30"), hasShares(460.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Ref.-Nr.: 20220404WAB1406946452"), //
                        hasAmount("EUR", 17454.91), hasGrossValue("EUR", 17454.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new UnicreditPDFExtractor(new Client());

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
                        hasIsin("LU0225880524"), hasWkn("A0F426"), hasTicker(null), //
                        hasName("DWS ESG EURO MONEY MARKET FUND INHABER-ANTEILE O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-02T18:55:47"), hasShares(1.51), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Ref.-Nr.: 20240402WAB1515775338"), //
                        hasAmount("EUR", 149.96), hasGrossValue("EUR", 149.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new UnicreditPDFExtractor(new Client());

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
                        hasIsin("DE0005200000"), hasWkn("520000"), hasTicker(null), //
                        hasName("BEIERSDORF AG INHABER-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-04-16T12:38:47"), hasShares(90.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ref.-Nr.: 20210416WAB1798426394"), //
                        hasAmount("EUR", 8175.91), hasGrossValue("EUR", 8217.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 41.09))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new UnicreditPDFExtractor(new Client());

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
                        hasIsin("IE00BK5BQV03"), hasWkn("A2PLS9"), hasTicker(null), //
                        hasName("VANG.FTSE DEVELOP.WORLD U.ETF REGISTERED SHARES USD ACC.ON"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-01-09T10:33:28"), hasShares(50.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Ref.-Nr.: 20240109WAB1302517526"), //
                        hasAmount("EUR", 4208.44), hasGrossValue("EUR", 4222.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 14.06))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new UnicreditPDFExtractor(new Client());

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
                        hasIsin("DE000A1T73W9"), hasWkn("A1T73W"), hasTicker(null), //
                        hasName("ACATIS GANÉ VALUE EVENT FONDS INHABER-ANTEILE C"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-05-18"), hasExDate(null), //
                        hasShares(0.066), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Ref.-Nr.: 20210517KUP0123456789"), //
                        hasAmount("EUR", 0.99), hasGrossValue("EUR", 0.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new UnicreditPDFExtractor(new Client());

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
                        hasIsin("US0758871091"), hasWkn("857675"), hasTicker(null), //
                        hasName("BECTON, DICKINSON & CO. Registered Shares DL 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-07-01T00:00"), hasExDate(null), //
                        hasShares(1178.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Bel.-Nr.: 161815275103001"), //
                        hasAmount("USD", 893.04), hasGrossValue("USD", 1236.90), //
                        hasTaxes("USD", 340.15), hasFees("USD", 3.71))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new UnicreditPDFExtractor(new Client());

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
                        hasIsin("IE00BKM4GZ66"), hasWkn("A111X9"), hasTicker(null), //
                        hasName("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2026-01-24T00:00"), hasShares(2000.00), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote("Beleg-Nr.: 679178241/26/0002"), //
                        hasAmount("EUR", 216.83), hasGrossValue("EUR", 216.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
