package name.abuchen.portfolio.datatransfer.pdf.ingdiba;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
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
import name.abuchen.portfolio.datatransfer.pdf.INGDiBaPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class INGDiBaPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("iSh.STOXX Europe 600 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-11-17T16:17:32"), hasShares(14.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Ordernummer 12345678.001 | Limit: 38,10 EUR"), //
                        hasAmount("EUR", 533.39), hasGrossValue("EUR", 533.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("iSh.STOXX Europe 600 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-06-11T16:30:32"), hasShares(18.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Ordernummer 12345678.001"), //
                        hasAmount("EUR", 726.28), hasGrossValue("EUR", 726.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("XS0138973010"), hasWkn("778998"), hasTicker(null), //
                        hasName("7,125 % Aareal Bank Capital Fdg Trust EO-Trust.Pref.Sec. 01(06/Und.)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-11-06T16:10:26"), hasShares(40.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Ordernummer 90000000.001 | Limit: 25,33 EUR"), //
                        hasAmount("EUR", 1027.40), hasGrossValue("EUR", 1012.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.50 + 9.90 + 3.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE000A0F5UF5"), hasWkn("A0F5UF"), hasTicker(null), //
                        hasName("iShare.NASDAQ-100 UCITS ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-12-15T09:06:08"), hasShares(1.19591), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Ordernummer 98112180.001"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 49.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.86))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE000A0MKQK7"), hasWkn("A0MKQK"), hasTicker(null), //
                        hasName("ETF-PORTFOLIO GLOBAL Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-12-15T00:00"), hasShares(3.67647), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Ordernummer 98169066.001"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("GB0002771383"), hasWkn("987665"), hasTicker(null), //
                        hasName("Threadn.Inv.Fds-Euro.Sm.Cos Fd Thesaurierungsant. Klasse 1 oN"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-12-15T00:00"), hasShares(6.53245), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Ordernummer 98163239.001"), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US0378331005"), hasWkn("865985"), hasTicker(null), //
                        hasName("Apple Inc. Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-03-20T19:35:40"), hasShares(11.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Ordernummer 60972506.001"), //
                        hasAmount("EUR", 5009.71), hasGrossValue("EUR", 4997.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 12.49))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("GB0030934490"), hasWkn("797739"), hasTicker(null), //
                        hasName("M&G Inv.(1)-M&G Global Leaders Reg. Shares Euro-Class A o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2012-02-01T00:00"), hasShares(6.41234), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Ordernummer 00000000.000"), //
                        hasAmount("EUR", 75.00), hasGrossValue("EUR", 75.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE000TUAG000"), hasWkn("TUAG00"), hasTicker(null), //
                        hasName("TUI AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-01-26T00:00"), hasShares(275.00), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Ordernummer 187744999.001"), //
                        hasAmount("EUR", 297.15), hasGrossValue("EUR", 294.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.90))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US76954A1034"), hasWkn("A3C47B"), hasTicker(null), //
                        hasName("Rivian Automotive Inc. Reg.Shares Cl.A  DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-11-10T20:25:34"), hasShares(15.00), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Ordernummer 815471111.001"), //
                        hasAmount("EUR", 1335.07), hasGrossValue("EUR", 1311.99), //
                        hasForexGrossValue("USD", 1503.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 14.90 + 8.18))));
    }

    @Test
    public void testWertpapierKauf10WithSecurityinEUR()
    {
        var security = new Security("Rivian Automotive Inc. Reg.Shares Cl.A  DL -,0001", "EUR");
        security.setIsin("US76954A1034");
        security.setWkn("A3C47B");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

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
                        hasDate("2021-11-10T20:25:34"), hasShares(15.00), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Ordernummer 815471111.001"), //
                        hasAmount("EUR", 1335.07), hasGrossValue("EUR", 1311.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 14.90 + 8.18), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

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
                        hasIsin("LU0109392836"), hasWkn("937446"), hasTicker(null), //
                        hasName("Fr.Temp.Inv.Fds -F.Technol. Fd Namens-Anteile A (acc.) o.N."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-09-15T00:00"), hasShares(3.6939), //
                        hasSource("Kauf11.txt"), //
                        hasNote("Ordernummer 2xxxxxxx1.001"), //
                        hasAmount("EUR", 110.00), hasGrossValue("EUR", 110.00), //
                        hasForexGrossValue("USD", 109.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf11WithSecurityinEUR()
    {
        var security = new Security("Fr.Temp.Inv.Fds -F.Technol. Fd Namens-Anteile A (acc.) o.N.", "EUR");
        security.setIsin("LU0109392836");
        security.setWkn("937446");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

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
                        hasDate("2022-09-15T00:00"), hasShares(3.6939), //
                        hasSource("Kauf11.txt"), //
                        hasNote("Ordernummer 2xxxxxxx1.001"), //
                        hasAmount("EUR", 110.00), hasGrossValue("EUR", 110.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf12.txt"), errors);

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
                        hasIsin("XS2263517364"), hasWkn("A27DLG"), hasTicker(null), //
                        hasName("0,01 % Landwirtschaftliche Rentenbank Med.T.Nts.v.20(40)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-16T14:40:15"), hasShares(10.00), //
                        hasSource("Kauf12.txt"), //
                        hasNote("Ordernummer 274272365.001 | Stückzinsen EUR 0,10 (Zinsvaluta 17.11.2022 357 Tage)"), //
                        hasAmount("EUR", 615.57), hasGrossValue("EUR", 609.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.42))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf13.txt"), errors);

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
                        hasIsin("DE000TUAG000"), hasWkn("TUAG00"), hasTicker(null), //
                        hasName("TUI AG Namens-Aktien o.N. Actions Port. EO 1"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-19T15:54:07"), hasShares(210.00), //
                        hasSource("Kauf13.txt"), //
                        hasNote("Ordernummer 123456789.001 | Limit: 3,999 EUR"), //
                        hasAmount("EUR", 851.21), hasGrossValue("EUR", 839.79), //
                        hasTaxes("EUR", 2.52), hasFees("EUR", 1.90 + 7.00))));
    }

    @Test
    public void testWertpapierKauf14()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf14.txt"), errors);

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
                        hasIsin("DE000TUAG000"), hasWkn("TUAG00"), hasTicker(null), //
                        hasName("Wertpapier AG Wertpapier XXXXXXXXXX"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-07-28T09:00:08"), hasShares(1.00), //
                        hasSource("Kauf14.txt"), //
                        hasNote("Ordernummer 93626494.001 | Limit: 350,00 EUR"), //
                        hasAmount("EUR", 361.26), hasGrossValue("EUR", 350.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90 + 0.39))));
    }

    @Test
    public void testWertpapierKauf15()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf15.txt"), errors);

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
                        hasIsin("DE000ETFL235"), hasWkn("ETFL23"), hasTicker(null), //
                        hasName("Wertpapier AG Wertpapier XXXXXXXXXX"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-12-15T15:30:52"), hasShares(1.00), //
                        hasSource("Kauf15.txt"), //
                        hasNote("Ordernummer 98094304.001 | Limit: 2.490,00 EUR"), //
                        hasAmount("EUR", 2503.47), hasGrossValue("EUR", 2490.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90 + 2.82))));
    }

    @Test
    public void testWertpapierKauf16()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf16.txt"), errors);

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
                        hasIsin("LU0629459743"), hasWkn("A1JA1R"), hasTicker(null), //
                        hasName("UBS(L)FS-MSCI World Soc. Resp. Namens-Anteile (USD) A-dis o.N"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-10T09:04:29"), hasShares(10.68328), //
                        hasSource("Kauf16.txt"), //
                        hasNote("Ordernummer 12345678.001"), //
                        hasAmount("EUR", 1234.56), hasGrossValue("EUR", 1234.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf17()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf17.txt"), errors);

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
                        hasIsin("DE000A3H2UX0"), hasWkn("A3H2UX"), hasTicker(null), //
                        hasName("1,75 % Sixt SE MTN v.2020(2024/2024)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-18T10:45:15"), hasShares(100.00), //
                        hasSource("Kauf17.txt"), //
                        hasNote("Ordernummer 123456789.001 | Stückzinsen EUR 6,23 (Zinsvaluta 21.12.2020 13 Tage)"), //
                        hasAmount("EUR", 10226.61), hasGrossValue("EUR", 10196.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 30.38))));
    }

    @Test
    public void testWertpapierKauf18()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf18.txt"), errors);

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
                        hasIsin("DE000A3H2UX0"), hasWkn("A3H2UX"), hasTicker(null), //
                        hasName("1,75 % Sixt SE MTN v.2020(2024/2024)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-18T10:45:15"), hasShares(100.00), //
                        hasSource("Kauf18.txt"), //
                        hasNote("Ordernummer 123456789.001 | Stückzinsen EUR 6,23 (Zinsvaluta 21.12.2020 13 Tage)"), //
                        hasAmount("EUR", 10226.61), hasGrossValue("EUR", 10196.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 30.38))));
    }

    @Test
    public void testWertpapierKauf19()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf19.txt"), errors);

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
                        hasName("Xtrackers Stoxx Europe 600 Inhaber-Anteile 1C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-09T09:04:17"), hasShares(44.86352), //
                        hasSource("Kauf19.txt"), //
                        hasNote("Ordernummer 304273918.001"), //
                        hasAmount("EUR", 5000.00), hasGrossValue("EUR", 4980.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.90 + 17.35))));
    }

    @Test
    public void testWertpapierKauf20()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf20.txt"), errors);

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
                        hasIsin("XS2647239651"), hasWkn("A4AF84"), hasTicker(null), //
                        hasName("3,00 % ING Bank N.V. EO-Medium-Term Notes 2024(25)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-17T13:52:25"), hasShares(20.00), //
                        hasSource("Kauf20.txt"), //
                        hasNote("Ordernummer 752862834.001"), //
                        hasAmount("EUR", 2000.00), hasGrossValue("EUR", 2000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf21()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf21.txt"), errors);

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
                        hasIsin("IE00B9CQXS71"), hasWkn("A1T8GD"), hasTicker(null), //
                        hasName("SPDR S&P Glob.Div.Aristocr.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-24T09:04:29"), hasShares(8.9548), //
                        hasSource("Kauf21.txt"), //
                        hasNote("Ordernummer 381419383.001"), //
                        hasAmount("EUR", 284.27), hasGrossValue("EUR", 284.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf22()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf22.txt"), errors);

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
                        hasIsin("LU0274209237"), hasWkn("DBX1ME"), hasTicker(null), //
                        hasName("Xtrackers MSCI Europe Inhaber-Anteile 1C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-01T09:04:09"), hasShares(2.30287), //
                        hasSource("Kauf22.txt"), //
                        hasNote("Ordernummer 389457606.001"), //
                        hasAmount("EUR", 220.50), hasGrossValue("EUR", 220.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testValorCompra01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Compra01.txt"), errors);

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
                        hasIsin("ES0173093024"), hasWkn(null), hasTicker(null), //
                        hasName("RED ELECTRICA"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-14T15:37"), hasShares(67.00), //
                        hasSource("Compra01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1012.02), hasGrossValue("EUR", 1005.67), //
                        hasTaxes("EUR", 2.01), hasFees("EUR", 4.01 + 0.33))));
    }

    @Test
    public void testValorCompra02()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Compra02.txt"), errors);

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
                        hasIsin("GB0002374006"), hasWkn(null), hasTicker(null), //
                        hasName("DIAGEO"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-19T09:00"), hasShares(30.00), //
                        hasSource("Compra02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 907.94), hasGrossValue("EUR", 894.48), //
                        hasForexGrossValue("GBP", 1064.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.47 + 4.52 + 4.47))));
    }

    @Test
    public void testValorCompra02WithValorInEUR()
    {
        var security = new Security("DIAGEO", "EUR");
        security.setIsin("GB0002374006");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Compra02.txt"), errors);

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
                        hasDate("2024-06-19T09:00"), hasShares(30.00), //
                        hasSource("Compra02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 907.94), hasGrossValue("EUR", 894.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.47 + 4.52 + 4.47), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testValorCompra03()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Compra03.txt"), errors);

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
                        hasIsin("GB00B10RZP78"), hasWkn(null), hasTicker(null), //
                        hasName("UNILEVER PLC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-25T09:52"), hasShares(4.00), //
                        hasSource("Compra03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 188.14), hasGrossValue("EUR", 184.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.18))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE0006632003"), hasWkn("663200"), hasTicker(null), //
                        hasName("MorphoSys AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-05-07T11:43:33"), hasShares(31.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ordernummer 90619267.001 | Limit: 57,00 EUR"), //
                        hasAmount("EUR", 1887.64), hasGrossValue("EUR", 1897.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE0002635281"), hasWkn("263528"), hasTicker(null), //
                        hasName("iSh.EO ST.Sel.Div.30 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2016-12-22T11:45:08"), hasShares(30.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Ordernummer 10xxxxxxxx"), //
                        hasAmount("EUR", 568.41), hasGrossValue("EUR", 597.63), //
                        hasTaxes("EUR", 18.32 + 1.00), hasFees("EUR", 9.90))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US0378331005"), hasWkn("865985"), hasTicker(null), //
                        hasName("Apple Inc. Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2013-06-21T17:04:26"), hasShares(11.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Ordernummer 71305394.001 | Limit: 310,00 EUR"), //
                        hasAmount("EUR", 3421.66), hasGrossValue("EUR", 3431.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.90))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE000MA03JY5"), hasWkn("MA03JY"), hasTicker(null), //
                        hasName("Morgan Stanley & Co. Intl PLC FaktL O.End DAX 11476,96"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-24T08:14:25"), hasShares(70.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Ordernummer 123456789.001 | Limit: 5,70 EUR"), //
                        hasAmount("EUR", 393.10), hasGrossValue("EUR", 399.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.90))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE000A3H22F2"), hasWkn("A3H22F"), hasTicker(null), //
                        hasName("KION GROUP AG Inhaber-Bezugsrechte Fälligkeit 03.12.2020"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-12-01T12:00"), hasShares(100.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Ordernummer 190195614.001"), //
                        hasAmount("EUR", 7.41), hasGrossValue("EUR", 15.00), //
                        hasTaxes("EUR", 2.52 + 0.13), hasFees("EUR", 4.94))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("FR0014000IN0"), hasWkn("A2QJCL"), hasTicker(null), //
                        hasName("Alstom S.A. Anrechte Fälligkeit 30.11.2020"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-11-24T08:17:03"), hasShares(300.00), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("Ordernummer 189282593.001"), //
                        hasAmount("EUR", 569.23), hasGrossValue("EUR", 780.00), //
                        hasTaxes("EUR", 193.29 + 10.63), hasFees("EUR", 6.85))));
    }

    @Test
    public void testWertpapierVerkauf07()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US09075V1026"), hasWkn("A2PSR2"), hasTicker(null), //
                        hasName("BioNTech SE Nam.-Akt.(sp.ADRs)1/o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-07-23T16:47:25"), hasShares(25.00), //
                        hasSource("Verkauf07.txt"), //
                        hasNote("Ordernummer 123456789.001"), //
                        hasAmount("EUR", 4547.75), hasGrossValue("EUR", 5852.50), //
                        hasTaxes("EUR", 561.24 + 50.51 + 30.86 + 561.24 + 50.51 + 30.86), hasFees("EUR", 19.53))));
    }

    @Test
    public void testWertpapierVerkauf08()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US76954A1034"), hasWkn("A3C47B"), hasTicker(null), //
                        hasName("Rivian Automotive Inc. Reg.Shares Cl.A  DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-11-16T15:41:12"), hasShares(5.00), //
                        hasSource("Verkauf08.txt"), //
                        hasNote("Ordernummer 815471111.001 | Limit: 166,00 USD"), //
                        hasAmount("EUR", 634.34), hasGrossValue("EUR", 729.54), //
                        hasForexGrossValue("USD", 830.00), //
                        hasTaxes("EUR", 32.14 + 32.14 + 2.89 + 2.89 + 1.76 + 1.76), hasFees("EUR", 14.90 + 6.72))));
    }

    @Test
    public void testWertpapierVerkauf08WithSecurityinEUR()
    {
        var security = new Security("Rivian Automotive Inc. Reg.Shares Cl.A  DL -,0001", "EUR");
        security.setIsin("US76954A1034");
        security.setWkn("A3C47B");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf08.txt"), errors);

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
                        hasDate("2021-11-16T15:41:12"), hasShares(5.00), //
                        hasSource("Verkauf08.txt"), //
                        hasNote("Ordernummer 815471111.001 | Limit: 166,00 USD"), //
                        hasAmount("EUR", 634.34), hasGrossValue("EUR", 729.54), //
                        hasTaxes("EUR", 32.14 + 32.14 + 2.89 + 2.89 + 1.76 + 1.76), hasFees("EUR", 14.90 + 6.72), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf09()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE000CF7HEF1"), hasWkn("CF7HEF"), hasTicker(null), //
                        hasName("Citigroup Global Markets Dt. CA.BO.Z 23.12.14 Coba"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2014-12-23T00:00"), hasShares(70.00), //
                        hasSource("Verkauf09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1050.00), hasGrossValue("EUR", 1050.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf10()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf10.txt"), errors);

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
                        hasIsin("US62526P3073"), hasWkn("A3ER8P"), hasTicker(null), //
                        hasName("Mullen Automotive Inc. Registered Shares DL -,001"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-08-14T00:00"), hasShares(0.77778), //
                        hasSource("Verkauf10.txt"), //
                        hasNote("Ordernummer 123654987.456"), //
                        hasAmount("EUR", 0.70), hasGrossValue("EUR", 0.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf11()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US49177J1025"), hasWkn("A3EEHU"), hasTicker(null), //
                        hasName("Kenvue Inc. Registered Shares DL -,001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-01T00:00"), hasShares(0.0648), //
                        hasSource("Verkauf11.txt"), //
                        hasNote("Ordernummer 123654987.456"), //
                        hasAmount("EUR", 1.25), hasGrossValue("EUR", 1.25), //
                        hasForexGrossValue("USD", 1.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf11WithSecurityInEUR()
    {
        var security = new Security("Kenvue Inc. Registered Shares DL -,001", "EUR");
        security.setIsin("US49177J1025");
        security.setWkn("A3EEHU");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf11.txt"), errors);

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
                        hasDate("2023-09-01T00:00"), hasShares(0.0648), //
                        hasSource("Verkauf11.txt"), //
                        hasNote("Ordernummer 123654987.456"), //
                        hasAmount("EUR", 1.25), hasGrossValue("EUR", 1.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf12()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("LU0434032149"), hasWkn("A0X82B"), hasTicker(null), //
                        hasName("Stuttgarter Energiefonds Inhaber-Anteile o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-02-09T15:40:06"), hasShares(100.00), //
                        hasSource("Verkauf12.txt"), //
                        hasNote("Ordernummer 323268877.001 | Limit: 40,00 EUR"), //
                        hasAmount("EUR", 3747.06), hasGrossValue("EUR", 4000.00), //
                        hasTaxes("EUR", 219.85 + 12.09), hasFees("EUR", 3.20 + 2.90 + 14.90))));
    }

    @Test
    public void testWertpapierVerkauf13()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE000A254UR5"), hasWkn("A254UR"), hasTicker(null), //
                        hasName("4,25% Karlsberg Brauerei GmbH ITV v.2020(2022/2025)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-22T00:00"), hasShares(20.00), //
                        hasSource("Verkauf13.txt"), //
                        hasNote("Rückzahlung"), //
                        hasAmount("EUR", 2020.00), hasGrossValue("EUR", 2020.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testValorVenta01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Venta01.txt"), errors);

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
                        hasIsin("ES0112501012"), hasWkn(null), hasTicker(null), //
                        hasName("EBRO FOODS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-04-14T09:13"), hasShares(125.00), //
                        hasSource("Venta01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2074.25), hasGrossValue("EUR", 2082.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.08 + 3.17))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("IE00BKPT2S34"), hasWkn("A2P1KU"), hasTicker(null), //
                        hasName("iShsIII-Gl.Infl.L.Gov.Bd U.ETF Reg. Shs HGD EUR Acc. oN"), //
                        hasCurrencyCode("EUR"))));

        // check transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-01-04"), //
                        hasShares(378.00), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.16), hasGrossValue("EUR", 0.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale02.txt"), errors);

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
                        hasIsin("LU0274208692"), hasWkn("DBX1MW"), hasTicker(null), //
                        hasName("Xtrackers MSCI World Swap Inhaber-Anteile 1C o.N."), //
                        hasCurrencyCode("EUR"))));

        // check transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2021-01-04"), //
                        hasShares(61.76876), //
                        hasSource("Vorabpauschale02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.32), hasGrossValue("EUR", 0.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale03()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale03.txt"), errors);

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
                        hasIsin("IE00BSPLC413"), hasWkn("A12HU5"), hasTicker(null), //
                        hasName("SPDR MSCI USA Sm.C.Val.W.UETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        taxes( //
                                        hasDate("2024-01-02T00:00"), hasShares(60.00), //
                                        hasSource("Vorabpauschale03.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US5801351017"), hasWkn("856958"), hasTicker(null), //
                        hasName("McDonald's Corp. Registered Shares DL-,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-12-15T00:00"), hasExDate("2016-11-29"), hasShares(66.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.01), hasGrossValue("EUR", 59.11), //
                        hasForexGrossValue("USD", 62.04), //
                        hasTaxes("EUR", 8.87 + 5.91 + 0.32), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityinEUR()
    {
        var security = new Security("McDonald's Corp. Registered Shares DL-,01", "EUR");
        security.setIsin("US5801351017");
        security.setWkn("856958");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

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
                        hasDate("2016-12-15T00:00"), hasExDate("2016-11-29"), hasShares(66.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.01), hasGrossValue("EUR", 59.11), //
                        hasTaxes("EUR", 8.87 + 5.91 + 0.32), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE000A0H0744"), hasWkn("A0H074"), hasTicker(null), //
                        hasName("iSh.DJ Asia Pa.S.D.30 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-12-15T00:00"), hasExDate("2016-12-15"), hasShares(694.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 234.92), hasGrossValue("EUR", 303.60), //
                        hasTaxes("EUR", 65.10 + 3.58), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("CH0038389992"), hasWkn("A0NFN3"), hasTicker(null), //
                        hasName("BB Biotech AG Namens-Aktien SF 0,20"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-03-22T00:00"), hasExDate("2017-03-20"), hasShares(35.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 58.19), hasGrossValue("EUR", 89.53), //
                        hasForexGrossValue("CHF", 96.25), //
                        hasTaxes("EUR", 31.34), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityinEUR()
    {
        var security = new Security("BB Biotech AG Namens-Aktien SF 0,20", "EUR");
        security.setIsin("CH0038389992");
        security.setWkn("A0NFN3");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

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
                        hasDate("2017-03-22T00:00"), hasExDate("2017-03-20"), hasShares(35.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 58.19), hasGrossValue("EUR", 89.53), //
                        hasTaxes("EUR", 31.34), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("BE0974293251"), hasWkn("A2ASUV"), hasTicker(null), //
                        hasName("Anheuser-Busch InBev S.A./N.V. Actions au Port. o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-05-09T00:00"), hasExDate("2019-05-07"), hasShares(55.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 38.50), hasGrossValue("EUR", 55.00), //
                        hasTaxes("EUR", 16.50), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US5801351017"), hasWkn("856958"), hasTicker(null), //
                        hasName("McDonald’s Corp. Registered Shares DL-,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-15T00:00"), hasExDate("2020-11-30"), hasShares(10.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.94), hasGrossValue("EUR", 10.52), //
                        hasForexGrossValue("USD", 12.90), //
                        hasTaxes("EUR", 1.58), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05WithSecurityinEUR()
    {
        var security = new Security("McDonald’s Corp. Registered Shares DL-,01", "EUR");
        security.setIsin("US5801351017");
        security.setWkn("856958");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

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
                        hasDate("2020-12-15T00:00"), hasExDate("2020-11-30"), hasShares(10.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.94), hasGrossValue("EUR", 10.52), //
                        hasTaxes("EUR", 1.58), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US4581401001"), hasWkn("855681"), hasTicker(null), //
                        hasName("Intel Corp. Registered Shares DL -,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-01T00:00"), hasExDate("2021-02-04"), hasShares(16.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.90), hasGrossValue("EUR", 4.58), //
                        hasForexGrossValue("USD", 5.56), //
                        hasTaxes("EUR", 0.68), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06WithSecurityinEUR()
    {
        var security = new Security("Intel Corp. Registered Shares DL -,001", "EUR");
        security.setIsin("US4581401001");
        security.setWkn("855681");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

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
                        hasDate("2021-03-01T00:00"), hasExDate("2021-02-04"), hasShares(16.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.90), hasGrossValue("EUR", 4.58), //
                        hasTaxes("EUR", 0.68), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US92826C8394"), hasWkn("A0NC7B"), hasTicker(null), //
                        hasName("VISA Inc. Reg. Shares Class A DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-01T00:00"), hasExDate("2021-02-11"), hasShares(5.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.12), hasGrossValue("EUR", 1.32), //
                        hasForexGrossValue("USD", 1.60), //
                        hasTaxes("EUR", 0.20), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07WithSecurityinEUR()
    {
        var security = new Security("VISA Inc. Reg. Shares Class A DL -,0001", "EUR");
        security.setIsin("US92826C8394");
        security.setWkn("A0NC7B");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

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
                        hasDate("2021-03-01T00:00"), hasExDate("2021-02-11"), hasShares(5.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.12), hasGrossValue("EUR", 1.32), //
                        hasTaxes("EUR", 0.20), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US4581401001"), hasWkn("855681"), hasTicker(null), //
                        hasName("Intel Corp. Registered Shares DL -,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-01T00:00"), hasExDate("2021-02-04"), hasShares(250.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 60.82), hasGrossValue("EUR", 71.55), //
                        hasForexGrossValue("USD", 86.88), //
                        hasTaxes("EUR", 10.73), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08WithSecurityinEUR()
    {
        var security = new Security("Intel Corp. Registered Shares DL -,001", "EUR");
        security.setIsin("US4581401001");
        security.setWkn("855681");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

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
                        hasDate("2021-03-01T00:00"), hasExDate("2021-02-04"), hasShares(250.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 60.82), hasGrossValue("EUR", 71.55), //
                        hasTaxes("EUR", 10.73), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("CA29250N6679"), hasWkn("A2DPXK"), hasTicker(null), //
                        hasName("4,40000% Enbridge Inc. 4,4%.Cum.Red.Pref.Shs.5 CD 25"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-01T00:00"), hasExDate("2021-02-11"), hasShares(165.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.60), hasGrossValue("EUR", 45.94), //
                        hasForexGrossValue("USD", 55.43), //
                        hasTaxes("EUR", 11.49 + 4.60 + 0.25), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09WithSecurityinEUR()
    {
        var security = new Security("4,40000% Enbridge Inc. 4,4%.Cum.Red.Pref.Shs.5 CD 25", "EUR");
        security.setIsin("CA29250N6679");
        security.setWkn("A2DPXK");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

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
                        hasDate("2021-03-01T00:00"), hasExDate("2021-02-11"), hasShares(165.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.60), hasGrossValue("EUR", 45.94), //
                        hasTaxes("EUR", 11.49 + 4.60 + 0.25), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US92826C8394"), hasWkn("A0NC7B"), hasTicker(null), //
                        hasName("VISA Inc. Reg. Shares Class A DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-01T00:00"), hasExDate("2021-02-11"), hasShares(11.97545), //
                        hasSource("Dividende10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.68), hasGrossValue("EUR", 3.15), //
                        hasForexGrossValue("USD", 3.83), //
                        hasTaxes("EUR", 0.47), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10WithSecurityinEUR()
    {
        var security = new Security("VISA Inc. Reg. Shares Class A DL -,0001", "EUR");
        security.setIsin("US92826C8394");
        security.setWkn("A0NC7B");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

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
                        hasDate("2021-03-01T00:00"), hasExDate("2021-02-11"), hasShares(11.97545), //
                        hasSource("Dividende10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.68), hasGrossValue("EUR", 3.15), //
                        hasTaxes("EUR", 0.47), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("iSh.STOXX Europe 600 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-09-15T00:00"), hasExDate("2015-09-15"), hasShares(18.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 6.70), hasGrossValue("EUR", 6.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE000A1PGUT9"), hasWkn("A1PGUT"), hasTicker(null), //
                        hasName("7,25000% posterXXL AG Inh.-Schv.v.2012(2015/2017)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-12-15T00:00"), hasExDate("2015-12-15"), hasShares(10.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 72.50), hasGrossValue("EUR", 72.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende13()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE0002635281"), hasWkn("263528"), hasTicker(null), //
                        hasName("iSh.EO ST.Sel.Div.30 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-10-17T00:00"), hasExDate("2016-10-17"), hasShares(1112.0958), //
                        hasSource("Dividende13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 101.32), hasGrossValue("EUR", 116.84), //
                        hasTaxes("EUR", 14.72 + 0.80), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("IE00B53QG562"), hasWkn("A0YEDX"), hasTicker(null), //
                        hasName("iShs VII-Core MSCI EMU UCI.ETF Reg. Shares EUR (Acc) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-02T00:00"), hasShares(304.00), //
                        hasSource("Dividende14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 20.03), hasGrossValue("EUR", 20.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("DE000TLX1005"), hasWkn("TLX100"), hasTicker(null), //
                        hasName("Talanx AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-05-12T00:00"), hasExDate("2016-05-12"), hasShares(500.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 468.04), hasGrossValue("EUR", 650.00), //
                        hasTaxes("EUR", 79.46 + 79.46 + 4.37 + 4.37 + 7.15 + 7.15), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende16()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("GB0007980591"), hasWkn("850517"), hasTicker(null), //
                        hasName("BP PLC Registered Shares DL -,25"), //
                        hasCurrencyCode("GBP"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2018-12-21T00:00"), hasExDate("2018-11-08"), hasShares(700.00), //
                        hasSource("Dividende16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 61.85), hasGrossValue("EUR", 61.85), //
                        hasForexGrossValue("GBP", 56.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende16WithSecurityinEUR()
    {
        var security = new Security("BP PLC Registered Shares DL -,25", "EUR");
        security.setIsin("GB0007980591");
        security.setWkn("850517");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

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
                        hasDate("2018-12-21T00:00"), hasExDate("2018-11-08"), hasShares(700.00), //
                        hasSource("Dividende16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 61.85), hasGrossValue("EUR", 61.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US70806A1060"), hasWkn("A1JQAB"), hasTicker(null), //
                        hasName("PennantPark Floating Rate Cap. Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-05-01T00:00"), hasExDate("2023-04-17"), hasShares(42.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.24), hasGrossValue("EUR", 3.81), //
                        hasForexGrossValue("USD", 4.20), //
                        hasTaxes("EUR", 0.57), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17WithSecurityinEUR()
    {
        var security = new Security("PennantPark Floating Rate Cap. Registered Shares o.N.", "EUR");
        security.setIsin("US70806A1060");
        security.setWkn("A1JQAB");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

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
                        hasDate("2023-05-01T00:00"), hasExDate("2023-04-17"), hasShares(42.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.24), hasGrossValue("EUR", 3.81), //
                        hasTaxes("EUR", 0.57), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende18()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("US46131B7047"), hasWkn("A3DMJV"), hasTicker(null), //
                        hasName("Invesco Mortgage Capital Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-04-27T00:00"), hasExDate("2023-04-06"), hasShares(10.00), //
                        hasSource("Dividende18.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.08), hasGrossValue("EUR", 3.62), //
                        hasForexGrossValue("USD", 4.00), //
                        hasTaxes("EUR", 0.54), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende18WithSecurityinEUR()
    {
        var security = new Security("Invesco Mortgage Capital Registered Shares DL -,01", "EUR");
        security.setIsin("US46131B7047");
        security.setWkn("A3DMJV");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

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
                        hasDate("2023-04-27T00:00"), hasExDate("2023-04-06"), hasShares(10.00), //
                        hasSource("Dividende18.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.08), hasGrossValue("EUR", 3.62), //
                        hasTaxes("EUR", 0.54), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende19()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende19.txt"), errors);

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
                        hasIsin("US0357108390"), hasWkn("A3DUCY"), hasTicker(null), //
                        hasName("Annaly Capital Management Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-04-28T00:00"), hasExDate("2023-03-30"), hasShares(50.00), //
                        hasSource("Dividende19.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.10), hasGrossValue("EUR", 29.53), //
                        hasForexGrossValue("USD", 32.50), //
                        hasTaxes("EUR", 4.43), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende19WithSecurityinEUR()
    {
        var security = new Security("Annaly Capital Management Inc. Registered Shares DL -,01", "EUR");
        security.setIsin("US0357108390");
        security.setWkn("A3DUCY");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new INGDiBaPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende19.txt"), errors);

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
                        hasDate("2023-04-28T00:00"), hasExDate("2023-03-30"), hasShares(50.00), //
                        hasSource("Dividende19.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.10), hasGrossValue("EUR", 29.53), //
                        hasTaxes("EUR", 4.43), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende20()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende20.txt"), errors);

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
                        hasIsin("DE000A3MP4P9"), hasWkn("A3MP4P"), hasTicker(null), //
                        hasName("4,00000% PCC SE Inh.-Teilschuldv. v.21(22/26)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-02T00:00"), hasExDate("2024-01-02"), hasShares(50.00), //
                        hasSource("Dividende20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende21()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende21.txt"), errors);

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
                        hasIsin("XS2892988192"), hasWkn("A383QY"), hasTicker(null), //
                        hasName("4,12500% Deutsche Lufthansa AG MTN v.2024(2032/2032)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-09-03T00:00"), hasExDate("2025-09-03"), hasShares(20.00), //
                        hasSource("Dividende21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 60.74), hasGrossValue("EUR", 82.50), //
                        hasTaxes("EUR", 20.63 + 1.13), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividendos01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividendos01.txt"), errors);

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
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("ADMIRAL GROUP PLC(ADM)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-07T00:00"), hasExDate(null), hasShares(103.00), //
                        hasSource("Dividendos01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 51.25), hasGrossValue("EUR", 63.27), //
                        hasTaxes("EUR", 12.02), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividendos02()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividendos02.txt"), errors);

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
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("INDITEX(ITX)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-05-02T00:00"), hasExDate(null), hasShares(95.00), //
                        hasSource("Dividendos02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 59.25), hasGrossValue("EUR", 73.15), //
                        hasTaxes("EUR", 13.90), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividendos03()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividendos03.txt"), errors);

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
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("T ROWE PRICE GROUP INC(TROW)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-27T00:00"), hasExDate(null), hasShares(52.00), //
                        hasSource("Dividendos03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 41.54), hasGrossValue("EUR", 60.33), //
                        hasTaxes("EUR", 9.05 + 9.74), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividendeStorno01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
                        hasIsin("IE00BTN1Y115"), hasWkn("A14M2J"), hasTicker(null), //
                        hasName("Medtronic PLC Registered Shares DL -,0001"), //
                        hasCurrencyCode("USD"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2020-04-17T00:00"), hasExDate("2020-03-26"), //
                                        hasShares(100.00), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 24.20), hasGrossValue("EUR", 24.20), //
                                        hasForexGrossValue("USD", 26.25), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testWertpapierRueckzahlung01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rueckzahlung01.txt"), errors);

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
                        hasIsin("CH0347556943"), hasWkn("UZ1LSU"), hasTicker(null), //
                        hasName("UBS AG (London Branch) Liquid. Z31.12.19 Fund"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-05-25T00:00"), hasShares(49.00), //
                        hasSource("Rueckzahlung01.txt"), //
                        hasNote("Rückzahlung"), //
                        hasAmount("EUR", 256.66), hasGrossValue("EUR", 256.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierRueckzahlung02()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rueckzahlung02.txt"), errors);

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
                        hasIsin("DE000SR9YJB8"), hasWkn("SR9YJB"), hasTicker(null), //
                        hasName("Société Générale Effekten GmbH DISC.Z 25.06.21 Roche 310"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-06-25T00:00"), hasShares(5.00), //
                        hasSource("Rueckzahlung02.txt"), //
                        hasNote("Rückzahlung"), //
                        hasAmount("EUR", 1404.34), hasGrossValue("EUR", 1417.40), //
                        hasTaxes("EUR", 12.38 + 0.68), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierRueckzahlung03()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rueckzahlung03.txt"), errors);

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
                        hasIsin("DE000HU89K58"), hasWkn("HU89K5"), hasTicker(null), //
                        hasName("UniCredit Bank AG HVB Call 13.12.17 Tesla 300"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-12-20T00:00"), hasShares(180.00), //
                        hasSource("Rueckzahlung03.txt"), //
                        hasNote("Rückzahlung"), //
                        hasAmount("EUR", 594.00), hasGrossValue("EUR", 594.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerrelevanterUmtausch01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "SteuerrelevanterUmtausch01.txt"),
                        errors);

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
                        hasIsin("LU1291109293"), hasWkn("A2ACQY"), hasTicker(null), //
                        hasName("BNP P.Easy-ECPI Gl ESG Infra. Nam.-Ant.UCITS ETF CAP EUR o.N"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        inboundDelivery( //
                                        hasDate("2023-11-03T00:00"), hasShares(4.00), //
                                        hasSource("SteuerrelevanterUmtausch01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmtauschEingang01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "UmtauschEingang01.txt"), errors);

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
                        hasIsin("US49177J1025"), hasWkn("A3EEHU"), hasTicker(null), //
                        hasName("Kenvue Inc. Registered Shares DL -,001"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2023-08-25T00:00"), hasShares(16.0648), //
                                        hasSource("UmtauschEingang01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmtauschEingang02()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "UmtauschEingang02.txt"), errors);

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
                        hasIsin("DE0006969603"), hasWkn("696960"), hasTicker(null), //
                        hasName("PUMA SE Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        inboundDelivery( //
                                        hasDate("2025-12-30T00:00"), hasShares(22.00), //
                                        hasSource("UmtauschEingang02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUmtauschAusgang01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "UmtauschAusgang01.txt"), errors);

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
                        hasIsin("US4781601046"), hasWkn("853260"), hasTicker(null), //
                        hasName("Johnson & Johnson Registered Shares DL 1"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        outboundDelivery( //
                                        hasDate("2023-08-25T00:00"), hasShares(2.00), //
                                        hasSource("UmtauschAusgang01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testUebertragAusgang01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "UebertragAusgang01.txt"), errors);

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
                        hasIsin("IE000KJPDY61"), hasWkn("A40121"), hasTicker(null), //
                        hasName("iShs III-iShs Wl.E.H.In.Ac.ETF Reg.Shs USD Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2026-08-31T00:00"), hasShares(601.00), //
                                        hasSource("UebertragAusgang01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testGiroKontoauszug01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug01.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2020-02-14"), hasAmount("EUR", 30.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Dauerauftrag/Terminüberweisung Max Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-02-16"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Lastschrift XYZ GmbH"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-02-22"), hasAmount("EUR", 5.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Überweisung XYZ GmbH"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-02-12"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Gutschrift Max Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-02-13"), hasAmount("EUR", 1.01), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Gutschrift/Dauerauftrag Max Mustermann"))));
    }

    @Test
    public void testGiroKontoauszug02()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug02.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2016-07-13"), hasAmount("EUR", 5000.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-08-17"), hasAmount("EUR", 3000.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-10-31"), hasAmount("EUR", 2000.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-06-27"), hasAmount("EUR", 10000.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Gutschrift Max Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2016-12-30"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.06), hasGrossValue("EUR", 5.62), //
                        hasTaxes("EUR", (1.38 + 0.07 + 0.11)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testGiroKontoauszug07()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        var errors = new ArrayList<Exception>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug07.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2018-11-16"), hasAmount("EUR", 4.50), //
                        hasSource("GiroKontoauszug07.txt"), hasNote("Abbuchung First Data Deutschland GmbH"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-11-27"), hasAmount("EUR", 53.99), //
                        hasSource("GiroKontoauszug07.txt"), hasNote("Retoure XYZ GmbH"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-11-29"), hasAmount("EUR", 1200.04), //
                        hasSource("GiroKontoauszug07.txt"), hasNote("Bezüge Freie Hansestadt Bremen"))));
    }

    @Test
    public void testGiroKontoauszug08()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        var errors = new ArrayList<Exception>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug08.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2010-11-01"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug08.txt"), hasNote("Gutschrift NPkVomtcG EcftL"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2010-11-03"), hasAmount("EUR", 54.52), //
                        hasSource("GiroKontoauszug08.txt"), hasNote("Lastschrift SB-TANK. 5128, uKava"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2010-11-16"), hasAmount("EUR", 250.00), //
                        hasSource("GiroKontoauszug08.txt"), hasNote("Gutschrift-Dauerauftrag dfgfg,dafbfhb mavr gOVG"))));
    }

    @Test
    public void testKGiroKontoauszug03()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug03.txt"), errors);

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
                        hasDate("2023-05-03"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.99), hasGrossValue("EUR", 0.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKGiroKontoauszug04()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug04.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2021-04-29"), hasAmount("EUR", 806.83), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Gehalt/Rente Hauptkasse des Freistaates Sachsen"))));
    }

    @Test
    public void testKGiroKontoauszug05()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug05.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2024-10-01"), hasAmount("EUR", 15.30), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Lastschrift VISA GETRAENKE FLEISCHMANN"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-10-07"), hasAmount("EUR", 5.00), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Lastschrift tdsKzkEv GmbH"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-10-08"), hasAmount("EUR", 19.40), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Lastschrift KtgV LIDL SAGT DANKE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-10-08"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Lastschrift Bargeldauszahlung VISA Card SPARKASSE LANDSHUT"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-10-10"), hasAmount("EUR", 25.11), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Lastschrift VISA LIDL SAGT DANKE"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-10-10"), hasAmount("EUR", 4.49), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Lastschrift VISA AMZNPRIME DE*TO6H12SU4"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-10-11"), hasAmount("EUR", 114.87), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Lastschrift VISA AMZN MKTP DE*TO81V3YH4"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-10-15"), hasAmount("EUR", 17.81), //
                        hasSource("GiroKontoauszug05.txt"), hasNote("Lastschrift VISA LIDL SAGT DANKE"))));
    }

    @Test
    public void testKGiroKontoauszug06()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

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
        assertThat(results, hasItem(removal(hasDate("2026-03-02"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug06.txt"), hasNote("Lastschrift Strom GmbH"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-03-16"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug06.txt"), hasNote("Echtzeitüberweisung Peter Pan Bla"))));
    }
    
    @Test
    public void testExtraKontoauszug01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ExtraKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-12-28"), hasAmount("EUR", 40.00), //
                        hasSource("ExtraKontoauszug01.txt"), hasNote("Gutschrift Vorname Nachname"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-29"), //
                        hasSource("ExtraKontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testExtraKontoauszug02()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ExtraKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(13L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(13));
        new AssertImportActions().check(results, "EUR");

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2022-01-04"), hasAmount("EUR", 50.00), //
                        hasSource("ExtraKontoauszug02.txt"), hasNote("Gutschrift/Dauerauftrag soYAn fXoVd rQHnqUa NpOYg"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2022-02-02"), hasAmount("EUR", 50.00), //
                        hasSource("ExtraKontoauszug02.txt"), hasNote("Gutschrift/Dauerauftrag zrGKj pqUOT BHNYJcN BPDzv"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2022-03-02"), hasAmount("EUR", 50.00), //
                        hasSource("ExtraKontoauszug02.txt"), hasNote("Gutschrift/Dauerauftrag vnViH oDejU jQHmNWS TxXEx"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2022-04-04"), hasAmount("EUR", 50.00), //
                        hasSource("ExtraKontoauszug02.txt"), hasNote("Gutschrift/Dauerauftrag sgxkF VgRvj LSrsRpX zGUSF"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2022-05-03"), hasAmount("EUR", 50.00), //
                        hasSource("ExtraKontoauszug02.txt"), hasNote("Gutschrift/Dauerauftrag drFJW FUqPJ JTiYezL XYQWb"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2022-06-02"), hasAmount("EUR", 50.00), //
                        hasSource("ExtraKontoauszug02.txt"), hasNote("Gutschrift/Dauerauftrag saByP ymzaz upMDuwC TYPZr"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2022-07-04"), hasAmount("EUR", 50.00), //
                        hasSource("ExtraKontoauszug02.txt"), hasNote("Gutschrift/Dauerauftrag iARTx zNRbT WKZFnNh qLjec"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2022-08-02"), hasAmount("EUR", 50.00), //
                        hasSource("ExtraKontoauszug02.txt"), hasNote("Gutschrift/Dauerauftrag Xvjmk qbfuq HAegAYM gSrwz"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2022-09-02"), hasAmount("EUR", 50.00), //
                        hasSource("ExtraKontoauszug02.txt"), hasNote("Gutschrift/Dauerauftrag SjSsx DdtZG AweiJLt DaoTr"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2022-10-04"), hasAmount("EUR", 50.00), //
                        hasSource("ExtraKontoauszug02.txt"), hasNote("Gutschrift/Dauerauftrag ktxgX kIrZn UGxoJzu dLEwD"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2022-11-02"), hasAmount("EUR", 50.00), //
                        hasSource("ExtraKontoauszug02.txt"), hasNote("Gutschrift/Dauerauftrag XBzVd KtJyh ZuCSPto lmUPq"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2022-12-02"), hasAmount("EUR", 50.00), //
                        hasSource("ExtraKontoauszug02.txt"), hasNote("Gutschrift/Dauerauftrag GyipU UEJqb EcpZhws yKXkv"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-12-30"), //
                        hasSource("ExtraKontoauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testExtraKontoauszug03()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "ExtraKontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(17L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(17));
        new AssertImportActions().check(results, "EUR");

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2024-04-23"), hasAmount("EUR", 5000.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Gutschrift qaPHnx GoUBDjvD"))));

        // assert transactions
        assertThat(results, hasItem(removal(hasDate("2024-06-03"), hasAmount("EUR", 3500.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Überweisung qcAfBwsS"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2024-06-04"), hasAmount("EUR", 5300.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Lastschrift-Einzug mvezSX fnHyElys"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2024-06-28"), hasAmount("EUR", 70000.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Gutschrift GebjSw slEgOvkw"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2024-07-09"), hasAmount("EUR", 48000.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Gutschrift VUpvDC JaFuUYIK"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2024-07-29"), hasAmount("EUR", 700.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Lastschrift-Einzug FxSkbY iomDFPRB"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2024-08-05"), hasAmount("EUR", 10000.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Gutschrift BaTqSR MFOodQHf"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2024-08-06"), hasAmount("EUR", 700.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Lastschrift-Einzug sVBOEV POzGjMEG"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2024-09-02"), hasAmount("EUR", 500.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Gutschrift VqAUcs QXGCxRyX"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2024-09-02"), hasAmount("EUR", 200.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Gutschrift gwiBrH DfcIXnTQ"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2024-10-02"), hasAmount("EUR", 500.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Lastschrift-Einzug JfDzzg vrgJTHTd"))));

        // assert transactions
        assertThat(results, hasItem(removal(hasDate("2024-10-21"), hasAmount("EUR", 500.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Überweisung ubQMDV ytXpZLuE"))));

        // assert transactions
        assertThat(results, hasItem(removal(hasDate("2024-10-23"), hasAmount("EUR", 25386.95), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Überweisung nMYTCeCd"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-04"), hasAmount("EUR", 500.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Lastschrift-Einzug AKRJgc BmeNrNmx"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2024-12-03"), hasAmount("EUR", 500.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Lastschrift-Einzug FcHCSQ omZMOALT"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2024-12-16"), hasAmount("EUR", 500.00), //
                        hasSource("ExtraKontoauszug03.txt"), hasNote("Gutschrift HzPljY PLWmJPVr"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-12-30"), //
                        hasSource("ExtraKontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 272.47), hasGrossValue("EUR", 370.07), //
                        hasTaxes("EUR", 92.52 + 5.08), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVLKontoauszug01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "VLKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2020-10-13"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug01.txt"), hasNote("Gutschrift-VWL"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2020-11-12"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug01.txt"), hasNote("Gutschrift-VWL"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2020-12-11"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug01.txt"), hasNote("Gutschrift-VWL"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-12-31"), //
                        hasSource("VLKontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVLKontoauszug02()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "VLKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(11L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, "EUR");

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-03-13"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug02.txt"), hasNote("Gutschrift-VWL"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-04-12"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug02.txt"), hasNote("Gutschrift-VWL"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-05-12"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug02.txt"), hasNote("Gutschrift-VWL"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-06-14"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug02.txt"), hasNote("Gutschrift-VWL"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-07-13"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug02.txt"), hasNote("Gutschrift-VWL"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-08-14"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug02.txt"), hasNote("Gutschrift-VWL"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-09-14"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug02.txt"), hasNote("Gutschrift-VWL"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-10-13"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug02.txt"), hasNote("Gutschrift-VWL"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-11-14"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug02.txt"), hasNote("Gutschrift-VWL"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-12-14"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug02.txt"), hasNote("Gutschrift-VWL"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-31"), //
                        hasSource("VLKontoauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.85), hasGrossValue("EUR", 2.55), //
                        hasTaxes("EUR", (0.62 + 0.03 + 0.05)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVLKontoauszug03()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "VLKontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-01-12"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug03.txt"), hasNote("Gutschrift-VWL"))));

        // assert transactions
        assertThat(results, hasItem(deposit(hasDate("2023-02-13"), hasAmount("EUR", 40.00), //
                        hasSource("VLKontoauszug03.txt"), hasNote("Gutschrift-VWL"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2023-03-06"), //
                        hasSource("VLKontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.16), hasGrossValue("EUR", 0.21), //
                        hasTaxes("EUR", 0.05), hasFees("EUR", 0.00))));

        // assert transactions
        assertThat(results, hasItem(removal(hasDate("2023-03-06"), hasAmount("EUR", 1161.10), //
                        hasSource("VLKontoauszug03.txt"), hasNote("Kontolöschung"))));
    }

    @Test
    public void testNachtraeglicheVerlustverrechnung01()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "NachtraeglicheVerlustverrechnung01.txt"),
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

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2026-01-16T00:00"), hasShares(0.00), //
                        hasSource("NachtraeglicheVerlustverrechnung01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testNachtraeglicheVerlustverrechnung02()
    {
        var extractor = new INGDiBaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "NachtraeglicheVerlustverrechnung02.txt"),
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

        // check tax refund transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-12-30T00:00"), hasShares(0.00), //
                        hasSource("NachtraeglicheVerlustverrechnung02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.29), hasGrossValue("EUR", 0.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
