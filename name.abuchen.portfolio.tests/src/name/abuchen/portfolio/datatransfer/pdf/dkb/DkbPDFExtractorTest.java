package name.abuchen.portfolio.datatransfer.pdf.dkb;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.feeRefund;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
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
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.DkbPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class DkbPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("DE000A1HLTD2"), hasWkn("A1HLTD"), hasTicker(null), //
                        hasName("8,75 % METALCORP GROUP B.V. EO-ANLEIHE 2013(18)"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-11-25T11:02:54"), hasShares(20.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftragsnummer 495752/48.00 | Limit 97,50 %"), //
                        hasAmount("EUR", 2030.66), hasGrossValue("EUR", 2023.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 7.50))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("BMG7945E1057"), hasWkn("A0ERZ0"), hasTicker(null), //
                        hasName("SEADRILL LTD. REGISTERED SHARES DL 2,-"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-01-25T09:33:06"), hasShares(1000.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Limit 1,75 EUR"), //
                        hasAmount("EUR", 1760.91), hasGrossValue("EUR", 1750.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00 + 0.71 + 0.20))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("LU0392494562"), hasWkn("ETF110"), hasTicker(null), //
                        hasName("COMSTAGE-MSCI WORLD TRN U.ETF INHABER-ANTEILE I O.N."), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-03-06T00:00"), hasShares(29.2893), //
                        hasSource("Kauf03.txt"), //
                        hasAmount("EUR", 1410.00), hasGrossValue("EUR", 1400.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("LU0392494562"), hasWkn("ETF110"), hasTicker(null), //
                        hasName("COMSTAGE-MSCI WORLD TRN U.ETF INHABER-ANTEILE I O.N."), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-10-05T00:00"), hasShares(2.521), //
                        hasSource("Kauf04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 130.41), hasGrossValue("EUR", 128.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.50 + 0.71 + 0.20))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("LU0392494562"), hasWkn("ETF110"), hasTicker(null), //
                        hasName("COMSTAGE-MSCI WORLD TRN U.ETF INHABER-ANTEILE I O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-01-07"), hasShares(25.60), //
                        hasSource("Kauf05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1201.50), hasGrossValue("EUR", 1200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.50))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("LU0392494562"), hasWkn("ETF110"), hasTicker(null), //
                        hasName("COMSTAGE-MSCI WORLD TRN U.ETF NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-07T11:11:56"), hasShares(10.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1144.20), hasGrossValue("EUR", 1142.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.00 + 0.60))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("FR0000121014"), hasWkn("853292"), hasTicker(null), //
                        hasName("LVMH MOET HENN. L. VUITTON SE ACTIONS PORT. (C.R.) EO 0,3"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-09-01T08:00:04"), hasShares(3.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1918.31), hasGrossValue("EUR", 1902.60), //
                        hasTaxes("EUR", 5.71), hasFees("EUR", 10.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("CH0126639464"), hasWkn("A1JJES"), hasTicker(null), //
                        hasName("CALIDA HOLDING AG NAM.-AKT. SF 0,10"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-09T17:14:41"), hasShares(20.00), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Auftragsnummer 123456/78.90 | Limit 50,00 CHF"), //
                        hasAmount("EUR", 1014.68), hasGrossValue("EUR", 984.79), //
                        hasForexGrossValue("CHF", 971.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 20.00 + 9.89))));
    }

    @Test
    public void testWertpapierKauf08WithSecurityInEUR()
    {
        var security = new Security("CALIDA HOLDING AG NAM.-AKT. SF 0,10", "EUR");
        security.setIsin("CH0126639464");
        security.setWkn("A1JJES");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

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
                        hasDate("2023-01-09T17:14:41"), hasShares(20.00), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Auftragsnummer 123456/78.90 | Limit 50,00 CHF"), //
                        hasAmount("EUR", 1014.68), hasGrossValue("EUR", 984.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 20.00 + 9.89), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var entry = (BuySellEntry) tx.getCrossEntry();
                            var s = c.process(entry, account, entry.getPortfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("US88080T1043"), hasWkn("A3C9C7"), hasTicker(null), //
                        hasName("TERAWULF INC. REGISTERED SHARES DL -,10"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-01-31T19:46:30"), hasShares(2300), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Limit 1,80 USD"), //
                        hasAmount("EUR", 3884.81), hasGrossValue("EUR", 3848.30), //
                        hasForexGrossValue("USD", 4140.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00 + 6.51 + 20.00))));
    }

    @Test
    public void testWertpapierKauf09WithSecurityInEUR()
    {
        var security = new Security("TERAWULF INC. REGISTERED SHARES DL -,10", "EUR");
        security.setIsin("US88080T1043");
        security.setWkn("A3C9C7");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

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
                        hasDate("2024-01-31T19:46:30"), hasShares(2300), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Limit 1,80 USD"), //
                        hasAmount("EUR", 3884.81), hasGrossValue("EUR", 3848.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00 + 6.51 + 20.00))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

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
                        hasIsin("US75629F1093"), hasWkn("A1XFAV"), hasTicker(null), //
                        hasName("SOCIETAL CDMO INC. REGISTERED SHARES DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-02-27T19:42:48"), hasShares(2000), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Auftragsnummer 123456/31.01 | Limit 0,54 USD"), //
                        hasAmount("EUR", 1037.14), hasGrossValue("EUR", 1000.65), //
                        hasForexGrossValue("USD", 1079.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00 + 6.49 + 20.00))));
    }

    @Test
    public void testWertpapierKauf10WithSecurityInEUR()
    {
        var security = new Security("SOCIETAL CDMO INC. REGISTERED SHARES DL -,01", "EUR");
        security.setIsin("US75629F1093");
        security.setWkn("A1XFAV");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

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
                        hasDate("2024-02-27T19:42:48"), hasShares(2000), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Auftragsnummer 123456/31.01 | Limit 0,54 USD"), //
                        hasAmount("EUR", 1037.14), hasGrossValue("EUR", 1000.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00 + 6.49 + 20.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000A0U9J2"), hasWkn("A1MLSS"), hasTicker(null), //
                        hasName("8,5 % SCHOLZ HOLDING INH.-SCHV. V.2012(2017)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-10-27T09:05:33"), hasShares(60.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer 495752/36.00 | Limit 85,00 %"), //
                        hasAmount("EUR", 4937.19), hasGrossValue("EUR", 5428.36), //
                        hasTaxes("EUR", 420.24 + 23.11 + 37.82), hasFees("EUR", 10.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2015-10-27"), hasShares(60.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 56.57), hasGrossValue("EUR", 56.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("DE0005140008"), hasWkn("514000"), hasTicker(null), //
                        hasName("DEUTSCHE BANK AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-02-10T10:58:02"), hasShares(200.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftragsnummer 590966/56.00 | Limit 15,05 EUR"), //
                        hasAmount("EUR", 3000.00), hasGrossValue("EUR", 3010.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("DE000A2YN900"), hasWkn("A2YN90"), hasTicker(null), //
                        hasName("TEAMVIEWER AG INHABER-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-05-06T14:32:46"), hasShares(100.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftragsnummer 123456/12.34"), //
                        hasAmount("EUR", 4123.12), hasGrossValue("EUR", 4245.50), //
                        hasTaxes("EUR", 109.37 + 6.01), hasFees("EUR", 7.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US00165C1045"), hasWkn("A1W90H"), hasTicker(null), //
                        hasName("AMC ENTERTAINMENT HOLDINGS INC REG. SHARES CLASS A DL -,01"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-22T20:56:04"), hasShares(20.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftragsnummer 123456/78.00 | Limit 5,44 EUR"), //
                        hasAmount("EUR", 99.00), hasGrossValue("EUR", 109.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-02-23"), hasShares(20.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.08), hasGrossValue("EUR", 16.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("DE000LED02V0"), hasWkn("LED02V"), hasTicker(null), //
                        hasName("OSRAM LICHT AG Z.VERKAUF EING.NAMENS-AKTIEN"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-07-09"), hasShares(4.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 164.00), hasGrossValue("EUR", 164.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

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
                        hasIsin("CA3012831077"), hasWkn("A1C30Q"), hasTicker(null), //
                        hasName("EXCHANGE INCOME CORP. REGISTERED SHARES O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-07-06T14:49:39"), hasShares(75.00), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("Limit 27,80 EUR"), //
                        hasAmount("EUR", 2051.02), hasGrossValue("EUR", 2085.00), //
                        hasTaxes("EUR", 19.63 + 1.07), hasFees("EUR", 10.00 + 0.06 + 1.55 + 1.67))));
    }

    @Test
    public void testWertpapierVerkauf07()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

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
                        hasIsin("FR0010524777"), hasWkn("LYX0CB"), hasTicker(null), //
                        hasName("LYXOR NEW ENERGY UCITS ETF ACTIONS AU PORT.DIST O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2019-10-07"), hasShares(0.982), //
                        hasSource("Verkauf07.txt"), //
                        hasNote("Auftragsnummer 000000/00.00"), //
                        hasAmount("EUR", 24.69), hasGrossValue("EUR", 24.79), //
                        hasTaxes("EUR", 0.10), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf08()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf08.txt"), errors);

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
                        hasIsin("XS0149161217"), hasWkn("858865"), hasTicker(null), //
                        hasName("2,309 % RBS CAPITAL TRUST A EO-FLR TR.PREF.SEC.02(12/UND.)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2014-07-31"), hasShares(30.00), //
                        hasSource("Verkauf08.txt"), //
                        hasNote("Auftragsnummer 9892578200 | Rückzahlungskurs 100 %"), //
                        hasAmount("EUR", 2974.39), hasGrossValue("EUR", 3000.00), //
                        hasTaxes("EUR", 24.28 + 1.33), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf09()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf09.txt"), errors);

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
                        hasIsin("DE000A1RE7V0"), hasWkn("A1RE7V"), hasTicker(null), //
                        hasName("6,875 % MS DEUTSCHLAND GMBH INH.-SCHV. V.2012(2017)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-01-13"), hasShares(20.00), //
                        hasSource("Verkauf09.txt"), //
                        hasNote("Auftragsnummer 9796635900 | Rückzahlungskurs 100 %"), //
                        hasAmount("EUR", 1908.39), hasGrossValue("EUR", 2000.00), //
                        hasTaxes("EUR", 80.01 + 4.40 + 7.20), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf10()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A1RE7V0"), hasWkn("A1RE7V"), hasTicker(null), //
                        hasName("6,875 % MS DEUTSCHLAND GMBH INH.-SCHV. V.2012(2017)"), //
                        hasCurrencyCode("EUR"))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        sale( //
                                        hasDate("2016-01-13T00:00"), hasShares(20.00), //
                                        hasSource("Verkauf10.txt"), //
                                        hasNote("Auftragsnummer 9796635950 | Rückzahlungskurs 100 %"), //
                                        hasAmount("EUR", 1908.39), hasGrossValue("EUR", 2000.00), //
                                        hasTaxes("EUR", 80.01 + 4.40 + 7.20), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierVerkauf11()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf11.txt"), errors);

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
                        hasIsin("DE000A1RE7V0"), hasWkn("A1RE7V"), hasTicker(null), //
                        hasName("6,875 % MS DEUTSCHLAND GMBH INH.-SCHV. V.2012(2017)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-01-13"), hasShares(20.00), //
                        hasSource("Verkauf11.txt"), //
                        hasNote("Rückzahlungskurs 100 %"), //
                        hasAmount("EUR", 2000.00), hasGrossValue("EUR", 2000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf12()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf12.txt"), errors);

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
                        hasIsin("DE000VC5YHF3"), hasWkn("VC5YHF"), hasTicker(null), //
                        hasName("VONTOBEL FINANCIAL PRODUCTS DIZ 26.09.25 TOTAL 48"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-09-26T00:00"), hasShares(50.00), //
                        hasSource("Verkauf12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2390.77), hasGrossValue("EUR", 2400.00), //
                        hasTaxes("EUR", 8.75 + 0.48), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf13()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf13.txt"), errors);

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
                        hasIsin("DE000HLB2DM0"), hasWkn("HLB2DM"), hasTicker(null), //
                        hasName("LB.HESSEN-THÜRINGEN GZ NACHR.ANLEIHE V.15(25)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-11-18T00:00"), hasShares(50.00), //
                        hasSource("Verkauf13.txt"), //
                        hasNote("Auftragsnummer CERZ 9504228500 | Rückzahlungskurs 100 %"), //
                        hasAmount("EUR", 4976.27), hasGrossValue("EUR", 5000.00), //
                        hasTaxes("EUR", 22.50 + 1.23), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf14()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BJXRT698"), hasWkn("A2PM4Q"), hasTicker(null), //
                        hasName("SPDR BLOOM.1-3M.T-BI.U.ETF REGISTERED SHARES (ACC) O.N."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-29T10:14:13"), hasShares(547.00), //
                        hasSource("Verkauf14.txt"), //
                        hasNote("Auftragsnummer 249453/79.00 | Limit 118,02 USD"), //
                        hasAmount("EUR", 54599.35), hasGrossValue("EUR", 54635.19), //
                        hasForexGrossValue("USD", 64556.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 30.00 + 2.50 + 2.62 + 0.72))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-12-30T00:00"), hasShares(547.00), //
                        hasSource("Verkauf14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1558.35), hasGrossValue("EUR", 1558.35), //
                        hasForexGrossValue("USD", 1841.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf14WithSecurityInEUR()
    {
        var security = new Security("SPDR BLOOM.1-3M.T-BI.U.ETF REGISTERED SHARES (ACC) O.N.", "EUR");
        security.setIsin("IE00BJXRT698");
        security.setWkn("A2PM4Q");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-29T10:14:13"), hasShares(547.00), //
                        hasSource("Verkauf14.txt"), //
                        hasNote("Auftragsnummer 249453/79.00 | Limit 118,02 USD"), //
                        hasAmount("EUR", 54599.35), hasGrossValue("EUR", 54635.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 30.00 + 2.50 + 2.62 + 0.72))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-12-30T00:00"), hasShares(547.00), //
                        hasSource("Verkauf14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1558.35), hasGrossValue("EUR", 1558.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkaufStorno01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "VerkaufStorno01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKX55T58"), hasWkn("A12CX1"), hasTicker(null), //
                        hasName("VANG.FTSE DEVELOP.WORLD U.ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("EUR"))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        purchase( //
                                        hasDate("2021-01-05"), hasShares(16.3986), //
                                        hasSource("VerkaufStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 1051.50), hasGrossValue("EUR", 1051.50), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("DE000A1R1AN5"), hasWkn("A1R1AN"), hasTicker(null), //
                        hasName("PCC SE INH.-TEILSCHULDV. V.13(13/17)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-01-04"), hasExDate(null), //
                        hasShares(100.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Abrechnungsnr. 86525618110"), //
                        hasAmount("EUR", 144.52), hasGrossValue("EUR", 181.25), //
                        hasTaxes("EUR", 32.09 + 1.76 + 2.88), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("DE0007100000"), hasWkn("710000"), hasTicker(null), //
                        hasName("DAIMLER AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-04-07"), hasExDate("2016-04-07"), //
                        hasShares(30.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Abrechnungsnr. 59717175720"), //
                        hasAmount("EUR", 97.50), hasGrossValue("EUR", 97.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("IE00B3XNN521"), hasWkn("A1JJAG"), hasTicker(null), //
                        hasName("DIM.FDS-GLOBAL SMALL COMPANIES REGISTERED SHARES EUR DIS.O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-12-07"), hasExDate("2017-11-30"), //
                        hasShares(216.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Abrechnungsnr. 84033925310"), //
                        hasAmount("EUR", 32.93), hasGrossValue("EUR", 45.61), //
                        hasTaxes("EUR", 11.18 + 0.61 + 0.89), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("CH0010570767"), hasWkn("870503"), hasTicker(null), //
                        hasName("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-05-15"), hasExDate("2018-05-08"), //
                        hasShares(1.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 63136911234"), //
                        hasAmount("EUR", 27.72), hasGrossValue("EUR", 42.65), //
                        hasForexGrossValue("CHF", 51.00), //
                        hasTaxes("EUR", 14.93), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10", "EUR");
        security.setIsin("CH0010570767");
        security.setWkn("870503");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

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
                        hasDate("2018-05-15"), hasExDate("2018-05-08"), //
                        hasShares(1.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 63136911234"), //
                        hasAmount("EUR", 27.72), hasGrossValue("EUR", 42.65), //
                        hasTaxes("EUR", 14.93), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("IE00B4L5Y983"), hasWkn("A0RPWH"), hasTicker(null), //
                        hasName("iShares Core MSCI World UCITS ETF USD REGISTERED SHARES O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-06-01"), hasExDate("2018-05-12"), //
                        hasShares(10.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Abrechnungsnr. 12345678901"), //
                        hasAmount("EUR", 3.02), hasGrossValue("EUR", 4.10), //
                        hasTaxes("EUR", 1.03 + 0.05), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("IE00B3VVMM84"), hasWkn("A1JX51"), hasTicker(null), //
                        hasName("VANGUARD FTSE EM.MARKETS U.ETF REGISTERED SHARES USD DIS.ON"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-01-11"), hasExDate("2018-12-27"), //
                        hasShares(16.3517), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Abrechnungsnr. 11111"), //
                        hasAmount("EUR", 2.45), hasGrossValue("EUR", 2.99), //
                        hasForexGrossValue("USD", 3.46), //
                        hasTaxes("EUR", 0.52 + 0.02), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        var security = new Security("VANGUARD FTSE EM.MARKETS U.ETF REGISTERED SHARES USD DIS.ON", "EUR");
        security.setIsin("IE00B3VVMM84");
        security.setWkn("A1JX51");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

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
                        hasDate("2019-01-11"), hasExDate("2018-12-27"), //
                        hasShares(16.3517), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Abrechnungsnr. 11111"), //
                        hasAmount("EUR", 2.45), hasGrossValue("EUR", 2.99), //
                        hasTaxes("EUR", 0.52 + 0.02), hasFees("EUR", 0.00), //
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
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("DE000A1R1AN5"), hasWkn("A1R1AN"), hasTicker(null), //
                        hasName("PCC SE INH.-TEILSCHULDV. V.13(13/17)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-01-02"), hasExDate(null), //
                        hasShares(100.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Abrechnungsnr. 86508012450"), //
                        hasAmount("EUR", 173.02), hasGrossValue("EUR", 181.25), //
                        hasTaxes("EUR", 7.81 + 0.42), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("DE0005552004"), hasWkn("555200"), hasTicker(null), //
                        hasName("DEUTSCHE POST AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-05-20"), hasExDate("2019-05-16"), //
                        hasShares(92.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Abrechnungsnr. 63736123456"), //
                        hasAmount("EUR", 105.80), hasGrossValue("EUR", 105.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("DE000A0LGQL5"), hasWkn("A0LGQL"), hasTicker(null), //
                        hasName("IS.II-DEV.MARK.PR.YLD. UC. ETF BEARER SHARES (DT. ZERT.) O.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-02-23"), hasExDate("2015-01-29"), //
                        hasShares(53.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Abrechnungsnr. 8127381273"), //
                        hasAmount("EUR", 7.79), hasGrossValue("EUR", 8.53), //
                        hasForexGrossValue("USD", 8.53 * 1.1426), //
                        hasTaxes("EUR", 0.85 / 1.1426), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09WithSecurityInEUR()
    {
        var security = new Security("IS.II-DEV.MARK.PR.YLD. UC. ETF BEARER SHARES (DT. ZERT.) O.N.", "EUR");
        security.setIsin("DE000A0LGQL5");
        security.setWkn("A0LGQL");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

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
                        hasDate("2015-02-23"), hasExDate("2015-01-29"), //
                        hasShares(53.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote("Abrechnungsnr. 8127381273"), //
                        hasAmount("EUR", 7.79), hasGrossValue("EUR", 8.53), //
                        hasTaxes("EUR", 0.85 / 1.1426), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("IE00B6YX5D40"), hasWkn("A1JKS0"), hasTicker(null), //
                        hasName("SPDR S&P US DIVID.ARISTOCR.ETF REGISTERED SHARES O.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-10-04"), hasExDate("2017-09-18"), //
                        hasShares(10.6841), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Abrechnungsnr. 123456789"), //
                        hasAmount("EUR", 2.09), hasGrossValue("EUR", 2.48), //
                        hasForexGrossValue("USD", 2.48 * 1.1780), //
                        hasTaxes("EUR", 0.46 / 1.1780), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10WithSecurityInEUR()
    {
        var security = new Security("SPDR S&P US DIVID.ARISTOCR.ETF REGISTERED SHARES O.N.", "EUR");
        security.setIsin("IE00B6YX5D40");
        security.setWkn("A1JKS0");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

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
                        hasDate("2017-10-04"), hasExDate("2017-09-18"), //
                        hasShares(10.6841), //
                        hasSource("Dividende10.txt"), //
                        hasNote("Abrechnungsnr. 123456789"), //
                        hasAmount("EUR", 2.09), hasGrossValue("EUR", 2.48), //
                        hasTaxes("EUR", 0.46 / 1.1780), hasFees("EUR", 0.00), //
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
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("US0378331005"), hasWkn("865985"), hasTicker(null), //
                        hasName("APPLE INC. REGISTERED SHARES O.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-02-20"), hasExDate("2017-02-09"), //
                        hasShares(66.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Abrechnungsnr. 123456789 | Quartalsdividende"), //
                        hasAmount("EUR", 29.95), hasGrossValue("EUR", 35.24), //
                        hasForexGrossValue("USD", 37.62), //
                        hasTaxes("EUR", 5.29), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11WithSecurityInEUR()
    {
        var security = new Security("APPLE INC. REGISTERED SHARES O.N.", "EUR");
        security.setIsin("US0378331005");
        security.setWkn("865985");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

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
                        hasDate("2017-02-20"), hasExDate("2017-02-09"), //
                        hasShares(66.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Abrechnungsnr. 123456789 | Quartalsdividende"), //
                        hasAmount("EUR", 29.95), hasGrossValue("EUR", 35.24), //
                        hasTaxes("EUR", 5.29), hasFees("EUR", 0.00), //
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
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("JP3414750004"), hasWkn("471496"), hasTicker(null), //
                        hasName("SEIKO EPSON CORP. REGISTERED SHARES O.N."), //
                        hasCurrencyCode("JPY"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-07-01"), hasExDate("2019-03-27"), //
                        hasShares(715.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Abrechnungsnr. 111111111 | Schlussdividende"), //
                        hasAmount("EUR", 133.58), hasGrossValue("EUR", 180.17), //
                        hasForexGrossValue("JPY", 22165.00), //
                        hasTaxes("EUR", 27.03 + 18.01 + 0.99 + 0.56), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12WithSecurityInEUR()
    {
        var security = new Security("SEIKO EPSON CORP. REGISTERED SHARES O.N.", "EUR");
        security.setIsin("JP3414750004");
        security.setWkn("471496");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

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
                        hasDate("2019-07-01"), hasExDate("2019-03-27"), //
                        hasShares(715.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote("Abrechnungsnr. 111111111 | Schlussdividende"), //
                        hasAmount("EUR", 133.58), hasGrossValue("EUR", 180.17), //
                        hasTaxes("EUR", 27.03 + 18.01 + 0.99 + 0.56), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende13()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("GB00B02J6398"), hasWkn("A0DJ58"), hasTicker(null), //
                        hasName("ADMIRAL GROUP PLC REGISTERED SHARES LS -,001"), //
                        hasCurrencyCode("GBP"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-10-13"), hasExDate("2015-09-10"), //
                        hasShares(450.00), //
                        hasSource("Dividende13.txt"), //
                        hasNote("Abrechnungsnr. 12345678901 | Zwischendividende"), //
                        hasAmount("EUR", 227.63), hasGrossValue("EUR", 309.17), //
                        hasForexGrossValue("GBP", 229.50), //
                        hasTaxes("EUR", 77.29 + 4.25), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende13WithSecurityInEUR()
    {
        var security = new Security("ADMIRAL GROUP PLC REGISTERED SHARES LS -,001", "EUR");
        security.setIsin("GB00B02J6398");
        security.setWkn("A0DJ58");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

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
                        hasDate("2015-10-13"), hasExDate("2015-09-10"), //
                        hasShares(450.00), //
                        hasSource("Dividende13.txt"), //
                        hasNote("Abrechnungsnr. 12345678901 | Zwischendividende"), //
                        hasAmount("EUR", 227.63), hasGrossValue("EUR", 309.17), //
                        hasTaxes("EUR", 77.29 + 4.25), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende14()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

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
                        hasIsin("DE000A1R1AN5"), hasWkn("A1R1AN"), hasTicker(null), //
                        hasName("PCC SE INH.-TEILSCHULDV. V.13(13/17)"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-05"), hasExDate("2021-02-18"), //
                        hasShares(935.00), //
                        hasSource("Dividende14.txt"), //
                        hasNote("Abrechnungsnr. 12345678901"), //
                        hasAmount("EUR", 13.24), hasGrossValue("EUR", 18.36), //
                        hasForexGrossValue("USD", 22.16), //
                        hasTaxes("EUR", 2.24 + 2.24 + 0.12 + 0.12 + 0.20 + 0.20), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14WithSecurityInEUR()
    {
        var security = new Security("PCC SE INH.-TEILSCHULDV. V.13(13/17)", "EUR");
        security.setIsin("DE000A1R1AN5");
        security.setWkn("A1R1AN");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

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
                        hasDate("2021-03-05"), hasExDate("2021-02-18"), //
                        hasShares(935.00), //
                        hasSource("Dividende14.txt"), //
                        hasNote("Abrechnungsnr. 12345678901"), //
                        hasAmount("EUR", 13.24), hasGrossValue("EUR", 18.36), //
                        hasTaxes("EUR", 2.24 + 2.24 + 0.12 + 0.12 + 0.20 + 0.20), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende15()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

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
                        hasIsin("CH0010570767"), hasWkn("870503"), hasTicker(null), //
                        hasName("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-05-15"), hasExDate("2018-05-08"), //
                        hasShares(1.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Abrechnungsnr. 63136911234 | Kapitalrückzahlung"), //
                        hasAmount("EUR", 35.12), hasGrossValue("EUR", 35.12), //
                        hasForexGrossValue("CHF", 42.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15WithSecurityInEUR()
    {
        var security = new Security("CHOCOLADEF. LINDT & SPRUENGLI INHABER-PART.SCH. SF 10", "EUR");
        security.setIsin("CH0010570767");
        security.setWkn("870503");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

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
                        hasDate("2018-05-15"), hasExDate("2018-05-08"), //
                        hasShares(1.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Abrechnungsnr. 63136911234 | Kapitalrückzahlung"), //
                        hasAmount("EUR", 35.12), hasGrossValue("EUR", 35.12), //
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
    public void testDividende16()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

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
                        hasIsin("DE000A0Z2ZZ5"), hasWkn("A0Z2ZZ"), hasTicker(null), //
                        hasName("FREENET AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-05-14"), hasExDate("2014-05-14"), //
                        hasShares(200.00), //
                        hasSource("Dividende16.txt"), //
                        hasNote("Abrechnungsnr. 63310000000"), //
                        hasAmount("EUR", 290.00), hasGrossValue("EUR", 290.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

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
                        hasIsin("US56035L1044"), hasWkn("A0X8Y3"), hasTicker(null), //
                        hasName("MAIN STREET CAPITAL CORP. REGISTERED SHARES DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-19T00:00"), hasExDate("2023-12-07T00:00"), //
                        hasShares(497.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Abrechnungsnr. 85345940130 | Monatliche Dividende"), //
                        hasAmount("EUR", 141.90), hasGrossValue("EUR", 192.24), //
                        hasForexGrossValue("USD", 211.50), //
                        hasTaxes("EUR", 28.84 + (2 * 9.40) + (2 * 0.51) + (2 * 0.84)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17WithSecurityInEUR()
    {
        var security = new Security("MAIN STREET CAPITAL CORP. REGISTERED SHARES DL -,01", "EUR");
        security.setIsin("US56035L1044");
        security.setWkn("A0X8Y3");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

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
                        hasDate("2023-12-19T00:00"), hasExDate("2023-12-07T00:00"), //
                        hasShares(497.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Abrechnungsnr. 85345940130 | Monatliche Dividende"), //
                        hasAmount("EUR", 141.90), hasGrossValue("EUR", 192.24), //
                        hasTaxes("EUR", 28.84 + (2 * 9.40) + (2 * 0.51) + (2 * 0.84)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende18()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende18.txt"), errors);

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
                        hasIsin("US91282CFM82"), hasWkn("A3K92B"), hasTicker(null), //
                        hasName("UNITED STATES OF AMERICA DL-BONDS 2022(27) S.AD-2027"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-10-01T00:00"), hasExDate(null), hasShares(180.00), //
                        hasSource("Dividende18.txt"), //
                        hasNote("Abrechnungsnr. 77349248800"), //
                        hasAmount("EUR", 231.64), hasGrossValue("EUR", 314.62), //
                        hasForexGrossValue("USD", 371.25), //
                        hasTaxes("EUR", 78.66 + 4.32), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende18WithSecurityInEUR()
    {
        var security = new Security("UNITED STATES OF AMERICA DL-BONDS 2022(27) S.AD-2027", "EUR");
        security.setIsin("US91282CFM82");
        security.setWkn("A3K92B");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DkbPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende18.txt"), errors);

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
                        hasDate("2025-10-01T00:00"), hasExDate(null), hasShares(180.00), //
                        hasSource("Dividende18.txt"), //
                        hasNote("Abrechnungsnr. 77349248800"), //
                        hasAmount("EUR", 231.64), hasGrossValue("EUR", 314.62), //
                        hasTaxes("EUR", 78.66 + 4.32), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierAusgang01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "WertpapierAusgang01.txt"), errors);

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
                        hasIsin("DE000US9RGR9"), hasWkn("US9RGR"), hasTicker(null), //
                        hasName("24,75 % UBS AG (LONDON BRANCH) EO-ANL. 14(16) RWE"), //
                        hasCurrencyCode("EUR"))));

        // check outbound delivery transaction
        assertThat(results, hasItem(outboundDelivery( //
                        hasDate("2015-11-30"), hasShares(250.00), //
                        hasSource("WertpapierAusgang01.txt"), //
                        hasNote("Auftragsnummer 489130/67.00 | Depotkonto-Nr. 100235452280"), //
                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("IE00BK5BQT80"), hasWkn("A2PKXG"), hasTicker(null), //
                        hasName("VANGUARD FTSE ALL-WORLD U.ETF REG. SHS USD ACC. ON"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-01-06"), hasShares(49.1102), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote("Abrechnungsnr. 12345678901"), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        var extractor = new DkbPDFExtractor(new Client());

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
                        hasIsin("DE000A1CUAY0"), hasWkn("A1CUAY"), hasTicker(null), //
                        hasName("WERTGRUND WOHNSELECT D INHABER-ANTEILE"), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        taxes( //
                                        hasDate("2024-01-15T00:00"), hasShares(9.6469), //
                                        hasSource("Vorabpauschale02.txt"), //
                                        hasNote("Abrechnungsnr. 123456789"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testVorabpauschale03()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale03.txt"), errors);

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
                        hasIsin("LU0533033667"), hasWkn("LYX0GP"), hasTicker(null), //
                        hasName("MUL AMUNDI MSCI WORLD INF TECH UCITS ETF INH.ANTEILE ACC"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-02T00:00"), hasShares(5.00), //
                        hasSource("Vorabpauschale03.txt"), //
                        hasNote("Abrechnungsnr. 51511993430"), //
                        hasAmount("EUR", 5.28), hasGrossValue("EUR", 5.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondssparplan01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondssparplan01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(6L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0635178014"), hasWkn("ETF127"), hasTicker(null), //
                        hasName("COMSTA.-MSCI EM.MKTS.TRN U.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-07-05"), hasShares(2.2394), //
                        hasSource("Fondssparplan01.txt"), //
                        hasNote("Auftragsnummer 531781/77.00"), //
                        hasAmount("EUR", 90.00), hasGrossValue("EUR", 90.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-08-06"), hasShares(2.1692), //
                        hasSource("Fondssparplan01.txt"), //
                        hasNote("Auftragsnummer 548714/63.00"), //
                        hasAmount("EUR", 90.00), hasGrossValue("EUR", 90.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-09-05"), hasShares(2.3253), //
                        hasSource("Fondssparplan01.txt"), //
                        hasNote("Auftragsnummer 567153/38.00"), //
                        hasAmount("EUR", 90.00), hasGrossValue("EUR", 90.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-10-05"), hasShares(2.3496), //
                        hasSource("Fondssparplan01.txt"), //
                        hasNote("Auftragsnummer 586519/87.00"), //
                        hasAmount("EUR", 90.00), hasGrossValue("EUR", 90.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-11-05"), hasShares(2.3678), //
                        hasSource("Fondssparplan01.txt"), //
                        hasNote("Auftragsnummer 605113/36.00"), //
                        hasAmount("EUR", 90.00), hasGrossValue("EUR", 90.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2018-12-05"), hasShares(2.316), //
                        hasSource("Fondssparplan01.txt"), //
                        hasNote("Auftragsnummer 626208/11.00"), //
                        hasAmount("EUR", 90.00), hasGrossValue("EUR", 90.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testFondssparplan02()
    {
        // Fonds, only 3 decimal places for amount of shares

        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondssparplan02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1737652583"), hasWkn("A2H9Q0"), hasTicker(null), //
                        hasName("AMUNDI IND.SOL.-A.IN.MSCI E.M."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-20"), hasShares(3.9136), //
                        hasSource("Fondssparplan02.txt"), //
                        hasNote("Auftragsnummer 256485/46.00"), //
                        hasAmount("EUR", 200.49), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.49))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-20"), hasShares(4.8207), //
                        hasSource("Fondssparplan02.txt"), //
                        hasNote("Auftragsnummer 342983/71.00"), //
                        hasAmount("EUR", 200.49), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.49))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-22"), hasShares(4.4425), //
                        hasSource("Fondssparplan02.txt"), //
                        hasNote("Auftragsnummer 414802/35.00"), //
                        hasAmount("EUR", 200.49), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.49))));
    }

    @Test
    public void testGiroKontoauszug01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-06"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-08"), hasAmount("EUR", 20.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-28"), hasAmount("EUR", 91.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-01"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-01"), hasAmount("EUR", 999.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-01"), hasAmount("EUR", 43.86), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-01"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-18"), hasAmount("EUR", 0.01), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-29"), hasAmount("EUR", 1234.56), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-03"), hasAmount("EUR", 105.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("sonstige Buchung"))));
    }

    @Test
    public void testGiroKontoauszug02()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-31"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-04"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-01"), hasAmount("EUR", 1234.56), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-01-29"), hasShares(0.00), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Steuerausgleich"), //
                        hasAmount("EUR", 1.23), hasGrossValue("EUR", 1.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug03()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-11"), hasAmount("EUR", 31.95), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-15"), hasAmount("EUR", 69.96), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-18"), hasAmount("EUR", 1337.96), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-23"), hasAmount("EUR", 163.80), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-24"), hasAmount("EUR", 1337.69), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Kreditkartenabrechnung"))));
    }

    @Test
    public void testGiroKontoauszug04()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-21"), hasAmount("EUR", 3450.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Bareinzahlung am Geldautomat"))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-04-23"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Rechnung Bargeldeinzahlung"), //
                        hasAmount("EUR", 15.00), hasGrossValue("EUR", 15.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug05()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(21L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(21));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2021-03-31"), //
                        hasSource("GiroKontoauszug05.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.01.2021 bis 31.03.2021"), //
                        hasAmount("EUR", 0.24), hasGrossValue("EUR", 0.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-05"), hasAmount("EUR", 418.50), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-08"), hasAmount("EUR", 150.00), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-08"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-10"), hasAmount("EUR", 82.88), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-11"), hasAmount("EUR", 300.00), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-15"), hasAmount("EUR", 20.00), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-17"), hasAmount("EUR", 29.70), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-17"), hasAmount("EUR", 42.00), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-22"), hasAmount("EUR", 32.77), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-24"), hasAmount("EUR", 28.90), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-24"), hasAmount("EUR", 104.21), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Kreditkartenabrechnung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-25"), hasAmount("EUR", 28.00), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-29"), hasAmount("EUR", 19.54), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-29"), hasAmount("EUR", 30.00), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-30"), hasAmount("EUR", 92.35), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-30"), hasAmount("EUR", 33.92), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-03-31"), hasAmount("EUR", 39.66), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-01"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-01"), hasAmount("EUR", 23.56), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-01"), hasAmount("EUR", 52.50), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Basislastschrift"))));
    }

    @Test
    public void testGiroKontoauszug06()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-03-08"), hasAmount("EUR", 150.00), //
                        hasSource("GiroKontoauszug06.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-03-29"), hasAmount("EUR", 111.11), //
                        hasSource("GiroKontoauszug06.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug07()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-03-30"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug07.txt"), hasNote("Eingang Echtzeitüberweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-03-31"), hasAmount("EUR", 1400.00), //
                        hasSource("GiroKontoauszug07.txt"), hasNote("Eingang Echtzeitüberweisung"))));
    }

    @Test
    public void testGiroKontoauszug08()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-03-02"), hasAmount("EUR", 300.00), //
                        hasSource("GiroKontoauszug08.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2015-02-27"), hasAmount("EUR", 300.00), //
                        hasSource("GiroKontoauszug08.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug09()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2015-03-31"), //
                        hasSource("GiroKontoauszug09.txt"), //
                        hasNote("Abrechnungszeitraum vom 03.02.2015 bis 31.03.2015"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-03-19"), hasAmount("EUR", 300.00), //
                        hasSource("GiroKontoauszug09.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-03-31"), hasAmount("EUR", 34.00), //
                        hasSource("GiroKontoauszug09.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2015-03-17"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug09.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug10()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2015-09-30"), //
                        hasSource("GiroKontoauszug10.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.07.2015 bis 30.09.2015"), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.04), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2015-09-30"), //
                        hasSource("GiroKontoauszug10.txt"), //
                        hasNote("Zinsen für Dispositionskredit"), //
                        hasAmount("EUR", 0.18), hasGrossValue("EUR", 0.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug11()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2015-12-30"), //
                        hasSource("GiroKontoauszug11.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.10.2015 bis 31.12.2015"), //
                        hasAmount("EUR", 0.16), hasGrossValue("EUR", 0.22), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2015-12-30"), //
                        hasSource("GiroKontoauszug11.txt"), //
                        hasNote("Zinsen für Dispositionskredit"), //
                        hasAmount("EUR", 0.14), hasGrossValue("EUR", 0.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug12()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2015-06-30"), //
                        hasSource("GiroKontoauszug12.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.04.2015 bis 30.06.2015"), //
                        hasAmount("EUR", 0.14), hasGrossValue("EUR", 0.19), //
                        hasTaxes("EUR", 0.05), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-06-11"), hasAmount("EUR", 94.00), //
                        hasSource("GiroKontoauszug12.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-07-03"), hasAmount("EUR", 4.25), //
                        hasSource("GiroKontoauszug12.txt"), hasNote("Lastschrift"))));
    }

    @Test
    public void testGiroKontoauszug13()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug13.txt"), errors);

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
        assertThat(results, hasItem(fee( //
                        hasDate("2022-01-27"), //
                        hasSource("GiroKontoauszug13.txt"), //
                        hasNote("Rechnung Rückruf/Nachforschung"), //
                        hasAmount("EUR", 10.00), hasGrossValue("EUR", 10.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug14()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2022-03-31"), //
                        hasSource("GiroKontoauszug14.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.01.2022 bis 31.03.2022"), //
                        hasAmount("EUR", 0.64), hasGrossValue("EUR", 0.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-04-21"), hasAmount("EUR", 161.65), //
                        hasSource("GiroKontoauszug14.txt"), hasNote("Geldautomat (Fremdwährung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-04-22"), hasAmount("EUR", 69.00), //
                        hasSource("GiroKontoauszug14.txt"), hasNote("Kartenzahlung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-04-25"), hasAmount("EUR", 4.01), //
                        hasSource("GiroKontoauszug14.txt"), hasNote("Kartenzahlung onl FW"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-03-09"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug14.txt"), hasNote("Geldautomat"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-03-28"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug14.txt"), hasNote("Geldautomat"))));
    }

    @Test
    public void testGiroKontoauszug15()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug15.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2022-08-18"), hasAmount("EUR", 13.74), //
                        hasSource("GiroKontoauszug15.txt"), hasNote("Kartenzahlung online"))));
    }

    @Test
    public void testGiroKontoauszug16()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug16.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2022-09-05"), hasAmount("EUR", 15.00), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Überweisung entgeltfrei"))));
    }

    @Test
    public void testGiroKontoauszug17()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug17.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2018-08-01"), hasAmount("EUR", 1.00), //
                        hasSource("GiroKontoauszug17.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug18()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug18.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        deposit(hasDate("2022-11-03"), hasAmount("EUR", 26.91), //
                                        hasSource("GiroKontoauszug18.txt"), hasNote("Storno Gutschrift")))));
    }

    @Test
    public void testGiroKontoauszug19()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug19.txt"), errors);

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
        assertThat(results, hasItem(fee( //
                        hasDate("2018-06-12"), //
                        hasSource("GiroKontoauszug19.txt"), //
                        hasNote("Buchung Identifikationscode"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug20()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-03-05"), hasAmount("EUR", 4.80), //
                        hasSource("GiroKontoauszug20.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-03-06"), hasAmount("EUR", 24.65), //
                        hasSource("GiroKontoauszug20.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-03-24"), hasAmount("EUR", 1234.56), //
                        hasSource("GiroKontoauszug20.txt"), hasNote("Eingang Inst.Paym."))));
    }

    @Test
    public void testGiroKontoauszug21()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-06-22"), hasAmount("EUR", 8.97), //
                        hasSource("GiroKontoauszug21.txt"), hasNote("KARTENZAHLUNG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-06-29"), hasAmount("EUR", 4.14), //
                        hasSource("GiroKontoauszug21.txt"), hasNote("KARTENZAHLUNG"))));
    }

    @Test
    public void testGiroKontoauszug22()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug22.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-01-06"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-01-09"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-01-16"), hasAmount("EUR", 4.80), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-01-18"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-01-23"), hasAmount("EUR", 7.50), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-01-26"), hasAmount("EUR", 3.71), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Kartenzahlung (Fremdwährung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-01-30"), hasAmount("EUR", 4.40), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-01-30"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-01-27"), hasAmount("EUR", 201.23), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-02-01"), hasAmount("EUR", 1600.00), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-01-18"), //
                        hasSource("GiroKontoauszug22.txt"), //
                        hasNote("sonstige Entgelte Girokarte"), //
                        hasAmount("EUR", 11.88), hasGrossValue("EUR", 11.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        feeRefund( //
                                        hasDate("2023-01-31"), //
                                        hasSource("GiroKontoauszug22.txt"), //
                                        hasNote("sonstige Entgelte Stornorechnung"), //
                                        hasAmount("EUR", 10.12), hasGrossValue("EUR", 10.12), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testGiroKontoauszug23()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug23.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(47L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(47));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-26"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-07-04"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-05"), hasAmount("EUR", 288.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-05"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-05"), hasAmount("EUR", 12.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-06"), hasAmount("EUR", 9.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-06"), hasAmount("EUR", 26.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-07"), hasAmount("EUR", 276.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-07"), hasAmount("EUR", 4.34), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-08"), hasAmount("EUR", 7.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-08"), hasAmount("EUR", 39.90), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-08"), hasAmount("EUR", 33.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 10.55), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 9.90), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 41.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 31.63), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 93.70), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 22.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-12"), hasAmount("EUR", 30.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-13"), hasAmount("EUR", 29.58), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-14"), hasAmount("EUR", 40.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-15"), hasAmount("EUR", 51.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-15"), hasAmount("EUR", 37.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-16"), hasAmount("EUR", 69.06), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-20"), hasAmount("EUR", 9.80), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-20"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-21"), hasAmount("EUR", 46.37), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-21"), hasAmount("EUR", 41.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-21"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-22"), hasAmount("EUR", 14.83), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-22"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-23"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-26"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-27"), hasAmount("EUR", 17.42), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-27"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-28"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-30"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-03"), hasAmount("EUR", 1185.90), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-03"), hasAmount("EUR", 32.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-03"), hasAmount("EUR", 18.00), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-03"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-03"), hasAmount("EUR", 52.63), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-04"), hasAmount("EUR", 6.25), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-04"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-07-03"), //
                        hasSource("GiroKontoauszug23.txt"), //
                        hasNote("Entgelt für Konto ohne mtl. Eingang"), //
                        hasAmount("EUR", 4.50), hasGrossValue("EUR", 4.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug24()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(26L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(26));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-07"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-07"), hasAmount("EUR", 150.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-07"), hasAmount("EUR", 55.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-07"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-08-07"), hasAmount("EUR", 237.44), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-14"), hasAmount("EUR", 60.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-17"), hasAmount("EUR", 76.05), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-17"), hasAmount("EUR", 29.70), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-18"), hasAmount("EUR", 48.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-18"), hasAmount("EUR", 35.50), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-18"), hasAmount("EUR", 12.75), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-21"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Geldautomat"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-22"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-28"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-28"), hasAmount("EUR", 40.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-29"), hasAmount("EUR", 86.88), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-08-29"), hasAmount("EUR", 800.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-30"), hasAmount("EUR", 9.99), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-08-30"), hasAmount("EUR", 75.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-01"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-01"), hasAmount("EUR", 5.99), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-04"), hasAmount("EUR", 277.97), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-04"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Geldautomat"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-04"), hasAmount("EUR", 62.70), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Kartenzahlung"))));
    }

    @Test
    public void testGiroKontoauszug25()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug25.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-23"), hasAmount("EUR", 200.90), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-28"), hasAmount("EUR", 3.93), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Kartenzahlung (Fremdwährung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-09-01"), hasAmount("EUR", 1900.00), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug26()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug26.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-05"), hasAmount("EUR", 480.00), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-05"), hasAmount("EUR", 20.00), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-09-06"), hasAmount("EUR", 73.36), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-09-06"), hasAmount("EUR", 9999.99), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-04"), hasAmount("EUR", 150.00), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-10-04"), hasAmount("EUR", 150.00), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2023-10-02"), //
                        hasSource("GiroKontoauszug26.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.07.2023 bis 30.09.2023"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug27()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug27.txt"), errors);

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
        assertThat(results, hasItem(interest( //
                        hasDate("2023-06-30"), hasShares(0), //
                        hasSource("GiroKontoauszug27.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.04.2023 bis 30.06.2023"), //
                        hasAmount("EUR", 222.74), hasGrossValue("EUR", 302.54), //
                        hasTaxes("EUR", (75.64 + 4.16)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug28()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-01-15"), //
                        hasSource("GiroKontoauszug28.txt"), //
                        hasNote("sonstige Entgelte Girokarte"), //
                        hasAmount("EUR", 11.88), hasGrossValue("EUR", 11.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-16"), hasAmount("EUR", 65.28), //
                        hasSource("GiroKontoauszug28.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-17"), hasAmount("EUR", 52.33), //
                        hasSource("GiroKontoauszug28.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-24"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug28.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-01-26"), hasAmount("EUR", 4.75), //
                        hasSource("GiroKontoauszug28.txt"), hasNote("Kartenzahlung (Fremdwährung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-02-01"), hasAmount("EUR", 2600.00), //
                        hasSource("GiroKontoauszug28.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug29()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug29.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-09"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-09"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-23"), hasAmount("EUR", 390.11), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-23"), hasAmount("EUR", 400.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 110.82), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-30"), hasAmount("EUR", 8.70), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2024-12-30"), //
                        hasSource("GiroKontoauszug29.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.10.2024 bis 31.12.2024"), //
                        hasAmount("EUR", 0.07), hasGrossValue("EUR", 0.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug30()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug30.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-06"), hasAmount("EUR", 460.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Kartenzahlung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-02-17"), hasAmount("EUR", 44.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Kartenzahlung"))));
    }

    @Test
    public void testGiroKontoauszug31()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug31.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-04-01"), hasAmount("EUR", 5.99), //
                        hasSource("GiroKontoauszug31.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2025-04-01"), //
                        hasSource("GiroKontoauszug31.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.01.2025 bis 31.03.2025"), //
                        hasAmount("EUR", 0.04), hasGrossValue("EUR", 0.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug32()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug32.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-04-08"), hasAmount("EUR", 13.20), //
                        hasSource("GiroKontoauszug32.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-05-02"), hasAmount("EUR", 2000.00), //
                        hasSource("GiroKontoauszug32.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug33()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug33.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-07-07"), hasAmount("EUR", 1.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-07-10"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-07-14"), hasAmount("EUR", 65.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-07-14"), hasAmount("EUR", 65.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Eingang Echtzeitüberweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-08-01"), hasAmount("EUR", 7.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-08-01"), hasAmount("EUR", 30.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-08-04"), hasAmount("EUR", 4.99), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Kartenzahlung"))));
    }

    @Test
    public void testGiroKontoauszug34()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug34.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2025-11-06"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug34.txt"), hasNote("Echtzeitüberweisung"))));
    }

    @Test
    public void testGiroKontoauszug35()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug35.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2025-12-22"), hasAmount("EUR", 20.71), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Rückbuchung"))));
    }

    @Test
    public void testGiroKontoauszug36()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug36.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2026-01-19"), //
                        hasSource("GiroKontoauszug36.txt"), //
                        hasNote("sonstige Entgelte Girokarte"), //
                        hasAmount("EUR", 11.88), hasGrossValue("EUR", 11.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-02-02"), hasAmount("EUR", 2400.00), //
                        hasSource("GiroKontoauszug36.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug37()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug37.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2026-01-15"), hasAmount("EUR", 4.20), //
                        hasSource("GiroKontoauszug37.txt"), hasNote("Echtzeit-Dauerauftrag"))));
    }

    @Test
    public void testGiroKontoauszug38()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug38.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-11-10"), hasAmount("EUR", 1.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Kartenzahlung (Fremdwährung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-11-10"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Kartenzahlung online (Fremdwährung)"))));
    }

    @Test
    public void testTagesgeldKontoauszug01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TagesgeldKontoauszug01.txt"), errors);

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
        assertThat(results, hasItem(interest( //
                        hasDate("2024-07-01"), //
                        hasSource("TagesgeldKontoauszug01.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.04.2024 bis 30.06.2024"), //
                        hasAmount("EUR", 11.51), hasGrossValue("EUR", 11.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesgeldKontoauszug02()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TagesgeldKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-13"), hasAmount("EUR", 2000.00), //
                        hasSource("TagesgeldKontoauszug02.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-06-26"), hasAmount("EUR", 400.00), //
                        hasSource("TagesgeldKontoauszug02.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-27"), hasAmount("EUR", 9180.74), //
                        hasSource("TagesgeldKontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-04"), hasAmount("EUR", 9982.07), //
                        hasSource("TagesgeldKontoauszug02.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-07-01"), //
                        hasSource("TagesgeldKontoauszug02.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.04.2024 bis 30.06.2024"), //
                        hasAmount("EUR", 17.93), hasGrossValue("EUR", 17.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesgeldKontoauszug03()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TagesgeldKontoauszug03.txt"), errors);

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
        assertThat(results, hasItem(interest( //
                        hasDate("2024-07-01"), hasShares(0), //
                        hasSource("TagesgeldKontoauszug03.txt"), //
                        hasNote("Abrechnungszeitraum vom 01.04.2024 bis 30.06.2024"), //
                        hasAmount("EUR", 56.47), hasGrossValue("EUR", 76.70), //
                        hasTaxes("EUR", (19.18 + 1.05)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTagesgeldKontoauszug04()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TagesgeldKontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-02"), hasAmount("EUR", 133.51), //
                        hasSource("TagesgeldKontoauszug04.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-03"), hasAmount("EUR", 406.97), //
                        hasSource("TagesgeldKontoauszug04.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-04"), hasAmount("EUR", 406.99), //
                        hasSource("TagesgeldKontoauszug04.txt"), hasNote("Überweisung"))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        interest( //
                                        hasDate("2025-01-05"), //
                                        hasSource("TagesgeldKontoauszug04.txt"), //
                                        hasNote("Stornorechnung zur Abrechnung 30.12.2024"), //
                                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKreditKontoauszug01()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-09"), hasAmount("EUR", 100.00), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-11-23"), hasAmount("EUR", 57.57), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("WWW.onlineshop.DE,"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-11-21"), //
                        hasSource("KreditKontoauszug01.txt"), //
                        hasNote("Habenzins auf 28 Tage"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-10-23"), hasShares(0.00), //
                        hasSource("KreditKontoauszug01.txt"), //
                        hasNote("Abgeltungsteuer"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-25"), hasAmount("EUR", 62.32), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("Laden Ort 220,"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-25"), hasAmount("EUR", 19.36), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("Laden GmbH & Co., Ort"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-29"), hasAmount("EUR", 2450.00), //
                        hasSource("KreditKontoauszug01.txt"), hasNote("Auszahlung"))));
    }

    @Test
    public void testKreditKontoauszug02()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-08-04"), hasAmount("EUR", 300.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-07-26"), hasAmount("EUR", 100.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("DEUTSCHE BANK AG, DD-LOEBTAU"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-02"), hasAmount("EUR", 100.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("VB FlatCity-BAUTZEN EG, DD"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-05"), hasAmount("EUR", 100.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("VB FlatCity-BAUTZEN EG, DD"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-09"), hasAmount("EUR", 100.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("VB FlatCity-BAUTZEN EG, DD"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-11"), hasAmount("EUR", 100.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("VB FlatCity-BAUTZEN EG, DD"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-18"), hasAmount("EUR", 100.00), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("DEUTSCHE BANK AG, DD-LOEBTAU"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-18"), hasAmount("EUR", 4.27), //
                        hasSource("KreditKontoauszug02.txt"), hasNote("GITHUB, HTTPSGITHUB.C"))));
    }

    @Test
    public void testKreditKontoauszug03()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-08-21"), hasAmount("EUR", 40.72), //
                        hasSource("KreditKontoauszug03.txt"), hasNote("Ausgleich Kreditkarte gem. Abrechnung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-09-07"), hasAmount("EUR", 400.00), //
                        hasSource("KreditKontoauszug03.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-08-30"), hasAmount("EUR", 100.00), //
                        hasSource("KreditKontoauszug03.txt"), hasNote("VB DRESDEN-BAUTZEN EG, DD"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-09-09"), hasAmount("EUR", 100.00), //
                        hasSource("KreditKontoauszug03.txt"), hasNote("VB DRESDEN-BAUTZEN EG, DD"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-09-20"), hasAmount("EUR", 4.27), //
                        hasSource("KreditKontoauszug03.txt"), hasNote("GITHUB, HTTPSGITHUB.C"))));
    }

    @Test
    public void testKreditKontoauszug04()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-05-18"), hasAmount("EUR", 4.80), //
                        hasSource("KreditKontoauszug04.txt"), hasNote("GITHUB, HTTPSGITHUB.C"))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2022-05-20"), //
                        hasSource("KreditKontoauszug04.txt"), //
                        hasNote("Kartenpreis"), //
                        hasAmount("EUR", 2.49), hasGrossValue("EUR", 2.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKreditKontoauszug05()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug05.txt"), errors);

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
        assertThat(results, hasItem(fee( //
                        hasDate("2022-11-17"), //
                        hasSource("KreditKontoauszug05.txt"), //
                        hasNote("PIN-Gebühr"), //
                        hasAmount("EUR", 5.00), hasGrossValue("EUR", 5.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKreditKontoauszug06()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        deposit(hasDate("2015-10-23"), hasAmount("EUR", 1.00), //
                                        hasSource("KreditKontoauszug06.txt"), hasNote("STORNIERUNG")))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2015-11-21"), //
                        hasSource("KreditKontoauszug06.txt"), //
                        hasNote("Habenzins auf 28 Tage"), //
                        hasAmount("EUR", 1.23), hasGrossValue("EUR", 1.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-10-26"), hasAmount("EUR", 1234.00), //
                        hasSource("KreditKontoauszug06.txt"), hasNote("Auszahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-11-03"), hasAmount("EUR", 1.00), //
                        hasSource("KreditKontoauszug06.txt"), hasNote("ZAHLUNG1"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-11-04"), hasAmount("EUR", 4.96), //
                        hasSource("KreditKontoauszug06.txt"), hasNote("ZAHLUNG2"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-11-05"), hasAmount("EUR", 34.94), //
                        hasSource("KreditKontoauszug06.txt"), hasNote("ZAHLUNG3"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-11-06"), hasAmount("EUR", 89.95), //
                        hasSource("KreditKontoauszug06.txt"), hasNote("ZAHLUNG4,"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-11-06"), hasAmount("EUR", 29.95), //
                        hasSource("KreditKontoauszug06.txt"), hasNote("ZAHLUNG5"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-11-13"), hasAmount("EUR", 8.99), //
                        hasSource("KreditKontoauszug06.txt"), hasNote("ZAHLUNG6"))));
    }

    @Test
    public void testKreditKontoauszug07()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-29"), hasAmount("EUR", 392.31), //
                        hasSource("KreditKontoauszug07.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-11-29"), hasAmount("EUR", 500.00), //
                        hasSource("KreditKontoauszug07.txt"), hasNote("PAYPAL *abc, 35314369001"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-23"), hasAmount("EUR", 134.47), //
                        hasSource("KreditKontoauszug07.txt"), hasNote("PAYPAL *abc, 35314369001"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-12-27"), hasAmount("EUR", 500.00), //
                        hasSource("KreditKontoauszug07.txt"), hasNote("PAYPAL *abc, 35314369001"))));
    }

    @Test
    public void testKreditKontoauszug08()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2007-07-21"), //
                        hasSource("KreditKontoauszug08.txt"), //
                        hasNote("Habenzins auf 23 Tage"), //
                        hasAmount("EUR", 12.54), hasGrossValue("EUR", 12.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2007-07-21"), //
                        hasSource("KreditKontoauszug08.txt"), //
                        hasNote("Habenzins auf 5 Tage"), //
                        hasAmount("EUR", 2.92), hasGrossValue("EUR", 2.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKreditKontoauszug09()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2007-09-21"), hasAmount("EUR", 500.00), //
                        hasSource("KreditKontoauszug09.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2007-10-11"), hasAmount("EUR", 1017.85), //
                        hasSource("KreditKontoauszug09.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2007-10-23"), //
                        hasSource("KreditKontoauszug09.txt"), //
                        hasNote("Habenzins auf 31 Tage"), //
                        hasAmount("EUR", 19.88), hasGrossValue("EUR", 19.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        interest( //
                                        hasDate("2007-09-22"), //
                                        hasSource("KreditKontoauszug09.txt"), //
                                        hasNote("Storno Habenzinsen"), //
                                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKreditKontoauszug10()
    {
        var extractor = new DkbPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2009-01-26"), hasAmount("EUR", 18500.00), //
                        hasSource("KreditKontoauszug10.txt"), hasNote("Auszahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2009-02-03"), hasAmount("EUR", 550.00), //
                        hasSource("KreditKontoauszug10.txt"), hasNote("Auszahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2009-02-04"), hasAmount("EUR", 300.00), //
                        hasSource("KreditKontoauszug10.txt"), hasNote("Auszahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2009-02-20"), hasAmount("EUR", 17700.00), //
                        hasSource("KreditKontoauszug10.txt"), hasNote("Einzahlung"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2009-02-21"), //
                        hasSource("KreditKontoauszug10.txt"), //
                        hasNote("Habenzins auf 29 Tage"), //
                        hasAmount("EUR", 11.19), hasGrossValue("EUR", 11.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
