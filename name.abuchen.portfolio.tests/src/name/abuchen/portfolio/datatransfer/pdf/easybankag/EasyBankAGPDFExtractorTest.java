package name.abuchen.portfolio.datatransfer.pdf.easybankag;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
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
import name.abuchen.portfolio.datatransfer.pdf.EasyBankAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class EasyBankAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0F5UK5"), hasWkn(null), hasTicker(null), //
                        hasName("i S h . S T . E u . 6 00 Bas.Res.U.ETF DE Inhaber-Anlageaktien"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:04"), hasShares(6.94), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftrags-Nr.: 00000000-3.5.2017"), //
                        hasAmount("EUR", 468.43), hasGrossValue("EUR", 464.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.28 + 3.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00BYZK4776"), hasWkn(null), hasTicker(null), //
                        hasName("i S h a r e s  I V - Healthc.Innovation Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:04"), hasShares(73.84), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 475.72), hasGrossValue("EUR", 472.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00BYWQWR46"), hasWkn(null), hasTicker(null), //
                        hasName("V a n E c k  V i d  eSports UC. ETF Reg. Shares A USD Acc. o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:05"), hasShares(15.75), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 476.01), hasGrossValue("EUR", 471.71), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.30 + 3.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s I I I - C o r e MSCI World U.ETF Registered Shs USD (Acc) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:04"), hasShares(6.855), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 476.31), hasGrossValue("EUR", 473.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00BGL86Z12"), hasWkn(null), hasTicker(null), //
                        hasName("i S h a r e s  I V - Electr.Veh.+Dr.Tech Registered Shares USD Acc.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:04"), hasShares(78.80), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 479.35), hasGrossValue("EUR", 476.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00BZ0PKT83"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s I V - E d g e MSCI Wo.Mult.U.ETF Registered Shares USD (Acc)o.N"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:04"), hasShares(60.75), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 479.98), hasGrossValue("EUR", 475.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.31 + 3.00))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("FR0010527275"), hasWkn(null), hasTicker(null), //
                        hasName("L . M S C I  W a t . ESG F.(DR)UCITS ETF Actions au Port.Dist o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:04"), hasShares(9.67), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 483.94), hasGrossValue("EUR", 480.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B9CQXS71"), hasWkn(null), hasTicker(null), //
                        hasName("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:04"), hasShares(15.373), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 489.27), hasGrossValue("EUR", 484.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.33 + 3.00))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE0032523478"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s - E O  C o r p Bd Lar.Cap U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:04"), hasShares(4.00), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 491.08), hasGrossValue("EUR", 486.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.34 + 3.00))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00BYZK4883"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s I V - D i g i talisation U.ETF Registered Shares USD (Acc) oN"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:04"), hasShares(110.20), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 704.42), hasGrossValue("EUR", 701.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B1XNHC34"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s I I - G l . C lean Energy U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:04"), hasShares(90.68), //
                        hasSource("Kauf11.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 960.67), hasGrossValue("EUR", 955.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.63 + 3.00))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00BM67HK77"), hasWkn(null), hasTicker(null), //
                        hasName("X t r . ( I E ) - M S CI Wrld Health Care Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:04"), hasShares(5.622), //
                        hasSource("Kauf12.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 243.10), hasGrossValue("EUR", 239.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.66 + 3.00))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00BM67HT60"), hasWkn(null), hasTicker(null), //
                        hasName("X t r . ( I E ) - M S CI Wo.Inform.Techn. Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-06-13T09:04:12"), hasShares(10.04), //
                        hasSource("Kauf13.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 464.86), hasGrossValue("EUR", 460.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.27 + 3.00))));
    }

    @Test
    public void testWertpapierKauf14()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0005557508"), hasWkn(null), hasTicker(null), //
                        hasName("D E U T S C H E  T E LEKOM AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-07-25T09:00:07"), hasShares(80.00), //
                        hasSource("Kauf14.txt"), //
                        hasNote("Auftrags-Nr.: 36978407-25.7.2022"), //
                        hasAmount("EUR", 1482.52), hasGrossValue("EUR", 1446.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.86 + 25.31 + 7.95))));
    }

    @Test
    public void testWertpapierKauf15()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("AT0000815402"), hasWkn(null), hasTicker(null), //
                        hasName("UBM Development AG STAMMAKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-05T09:04:16"), hasShares(100.00), //
                        hasSource("Kauf15.txt"), //
                        hasNote("Auftrags-Nr.: 25095540 | Limit: 42,500000"), //
                        hasAmount("EUR", 4210.01), hasGrossValue("EUR", 4200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.47 + 0.24 + 6.30))));
    }

    @Test
    public void testWertpapierKauf16()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0006204407"), hasWkn(null), hasTicker(null), //
                        hasName("KUKA AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-12-27T09:11:22"), hasShares(27.00), //
                        hasSource("Kauf16.txt"), //
                        hasNote("Auftrags-Nr.: 25484466 | Limit: 73,400000"), //
                        hasAmount("EUR", 1997.80), hasGrossValue("EUR", 1981.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00 + 14.00))));
    }

    @Test
    public void testWertpapierKauf17()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("US1912161007"), hasWkn(null), hasTicker(null), //
                        hasName("COCA-COLA CO., THE Registered Shares DL -,25"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-10-27T12:21:47"), hasShares(13.00), //
                        hasSource("Kauf17.txt"), //
                        hasNote("Auftrags-Nr.: 25456794 | Limit: 59,150000"), //
                        hasAmount("EUR", 780.59), hasGrossValue("EUR", 768.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.69 + 2.00 + 8.95))));
    }

    @Test
    public void testWertpapierKauf18()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00BM67HK77"), hasWkn(null), hasTicker(null), //
                        hasName("X t r . ( I E ) - M S CI Wrld Health Care Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(7.617), //
                        hasSource("Kauf18.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 361.14), hasGrossValue("EUR", 357.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.98 + 3.00))));
    }

    @Test
    public void testWertpapierKauf19()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00BZ0PKT83"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s I V - E d g e MSCI Wo.Mult.U.ETF Registered Shares USD (Acc)o.N"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(86.00), //
                        hasSource("Kauf19.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 726.78), hasGrossValue("EUR", 721.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.98 + 3.00))));
    }

    @Test
    public void testWertpapierKauf20()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE0032523478"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s - E O  C o r p Bd Lar.Cap U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(6.05), //
                        hasSource("Kauf20.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 727.09), hasGrossValue("EUR", 722.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.99 + 3.00))));
    }

    @Test
    public void testWertpapierKauf21()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B9CQXS71"), hasWkn(null), hasTicker(null), //
                        hasName("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(23.954), //
                        hasSource("Kauf21.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 729.60), hasGrossValue("EUR", 724.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.99 + 3.00))));
    }

    @Test
    public void testWertpapierKauf22()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s I I I - C o r e MSCI World U.ETF Registered Shs USD (Acc) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(10.065), //
                        hasSource("Kauf22.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 739.95), hasGrossValue("EUR", 734.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.02 + 3.00))));
    }

    @Test
    public void testWertpapierKauf23()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf23.txt"), errors);

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
                        hasIsin("IE00BYZK4776"), hasWkn(null), hasTicker(null), //
                        hasName("i S h a r e s  I V - Healthc.Innovation Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(99.98), //
                        hasSource("Kauf23.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 743.18), hasGrossValue("EUR", 738.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.03 + 3.00))));
    }

    @Test
    public void testWertpapierKauf24()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf24.txt"), errors);

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
                        hasIsin("DE000A0F5UK5"), hasWkn(null), hasTicker(null), //
                        hasName("i S h . S T . E u . 6 00 Bas.Res.U.ETF DE Inhaber-Anlageaktien"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(11.98), //
                        hasSource("Kauf24.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 746.48), hasGrossValue("EUR", 741.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.04 + 3.00))));
    }

    @Test
    public void testWertpapierKauf25()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf25.txt"), errors);

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
                        hasIsin("FR0010527275"), hasWkn(null), hasTicker(null), //
                        hasName("L . M S C I  W a t . ESG F.(DR)UCITS ETF Actions au Port.Dist o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(13.76), //
                        hasSource("Kauf25.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 748.10), hasGrossValue("EUR", 745.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));
    }

    @Test
    public void testWertpapierKauf26()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf26.txt"), errors);

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
                        hasIsin("IE00BGL86Z12"), hasWkn(null), hasTicker(null), //
                        hasName("i S h a r e s  I V - Electr.Veh.+Dr.Tech Registered Shares USD Acc.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(118.00), //
                        hasSource("Kauf26.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 751.64), hasGrossValue("EUR", 746.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.05 + 3.00))));
    }

    @Test
    public void testWertpapierKauf27()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf27.txt"), errors);

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
                        hasIsin("IE00BYWQWR46"), hasWkn(null), hasTicker(null), //
                        hasName("V a n E c k  V i d  eSports UC. ETF Reg. Shares A USD Acc. o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(28.89), //
                        hasSource("Kauf27.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 764.17), hasGrossValue("EUR", 759.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.09 + 3.00))));
    }

    @Test
    public void testWertpapierKauf28()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf28.txt"), errors);

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
                        hasIsin("IE00BYZK4883"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s I V - D i g i talisation U.ETF Registered Shares USD (Acc) oN"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(173.00), //
                        hasSource("Kauf28.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 1148.29), hasGrossValue("EUR", 1142.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.14 + 3.00))));
    }

    @Test
    public void testWertpapierKauf29()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf29.txt"), errors);

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
                        hasIsin("IE00B1XNHC34"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s I I - G l . C lean Energy U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(128.1618), //
                        hasSource("Kauf29.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 1494.39), hasGrossValue("EUR", 1487.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.09 + 3.00))));
    }

    @Test
    public void testWertpapierKauf30()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf30.txt"), errors);

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
                        hasIsin("IE00BM67HT60"), hasWkn(null), hasTicker(null), //
                        hasName("X t r . ( I E ) - M S CI Wo.Inform.Techn. Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-11-11T00:00"), hasShares(15.67), //
                        hasSource("Kauf30.txt"), //
                        hasNote("Auftrags-Nr.: 36338031-3.5.2017"), //
                        hasAmount("EUR", 764.22), hasGrossValue("EUR", 759.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.09 + 3.00))));
    }

    @Test
    public void testWertpapierKauf31()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf31.txt"), errors);

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
                        hasIsin("AT0000APOST4"), hasWkn(null), hasTicker(null), //
                        hasName("O E S T E R R E I C H ISCHE POST AG AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-12-29T13:01:06"), hasShares(750.00), //
                        hasSource("Kauf31.txt"), //
                        hasNote("Auftrags-Nr.: 36075607-29.12.2022"), //
                        hasAmount("EUR", 21706.50), hasGrossValue("EUR", 21686.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 19.95))));
    }

    @Test
    public void testWertpapierKauf32()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf32.txt"), errors);

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
                        hasIsin("AT0000A0VRQ6"), hasWkn(null), hasTicker(null), //
                        hasName("Oesterreich, Republik Bundesanleihe 2012-2044/4"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-24T11:19:42"), hasShares(30.00), //
                        hasSource("Kauf32.txt"), //
                        hasNote("Auftrags-Nr.: 86422324 | Limit: 96,450000 | Stückzinsen für 6 Tage: 1,55 EUR"), //
                        hasAmount("EUR", 2900.00), hasGrossValue("EUR", 2895.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.95))));
    }

    @Test
    public void testWertpapierKauf33()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf33.txt"), errors);

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
                        hasIsin("US02209S1033"), hasWkn(null), hasTicker(null), //
                        hasName("Altria Group Inc. Registered Shares DL -,333"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-24T09:04:25"), hasShares(1.00), //
                        hasSource("Kauf33.txt"), //
                        hasNote("Auftrags-Nr.: 28715469 | Limit: 41,500000"), //
                        hasAmount("EUR", 53.14), hasGrossValue("EUR", 41.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.69 + 2.00 + 8.95))));
    }

    @Test
    public void testWertpapierKauf34()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf34.txt"), errors);

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
                        hasIsin("US02209S1033"), hasWkn(null), hasTicker(null), //
                        hasName("Altria Group Inc. Registered Shares DL -,333"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-24T09:04:25"), hasShares(29.00), //
                        hasSource("Kauf34.txt"), //
                        hasNote("Auftrags-Nr.: 28715469 | Limit: 41,500000"), //
                        hasAmount("EUR", 1203.50), hasGrossValue("EUR", 1203.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf35()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf35.txt"), errors);

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
                        hasIsin("US7033951036"), hasWkn(null), hasTicker(null), //
                        hasName("Patterson Companies Inc. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-02-20T16:54:59"), hasShares(25.00), //
                        hasSource("Kauf35.txt"), //
                        hasNote("Auftrags-Nr.: 45286197 | Limit: 23,250000"), //
                        hasAmount("USD", 608.99), hasGrossValue("USD", 579.25), //
                        hasTaxes("USD", 0.00), hasFees("USD", 13.25 + ((14.00 + 1.35) * 1.0745)))));
    }


    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("AT0000743059"), hasWkn(null), hasTicker(null), //
                        hasName("OMV AG AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-04T13:12:12"), hasShares(50.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftrags-Nr.: 25866072 | Limit: 51,520000"), //
                        hasAmount("EUR", 2482.91), hasGrossValue("EUR", 2584.00), //
                        hasTaxes("EUR", 93.23), hasFees("EUR", 1.67 + 0.24 + 5.95))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0006204407"), hasWkn(null), hasTicker(null), //
                        hasName("KUKA AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-10-27T11:15:57"), hasShares(27.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftrags-Nr.: 25058391 | Limit: 83,100000"), //
                        hasAmount("EUR", 2155.68), hasGrossValue("EUR", 2243.70), //
                        hasTaxes("EUR", 72.02), hasFees("EUR", 2.00 + 14.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("US0231351067"), hasWkn(null), hasTicker(null), //
                        hasName("AMAZON.COM INC. Registered Shares DL -,01"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-01-22T09:04:18"), hasShares(8.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftrags-Nr.: 25521770 | Stoplimit: 2.725,000000"), //
                        hasAmount("EUR", 21414.65), hasGrossValue("EUR", 21648.00), //
                        hasTaxes("EUR", 178.20), hasFees("EUR", 1.19 + 2.00 + 51.96))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A1R0RZ5"), hasWkn(null), hasTicker(null), //
                        hasName("Ekosem-Agrar AG Inh.-Schv. v.2012(2020/2027)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-11T11:38:21"), hasShares(20.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftrags-Nr.: 23456789 | Limit: 20,200000"), //
                        hasAmount("EUR", 394.69), hasGrossValue("EUR", 404.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.85 + 0.71 + 6.75))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-07-15T00:00"), hasShares(20.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 42.55), hasGrossValue("EUR", 42.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

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
                        hasIsin("DE000A2E4A94"), hasWkn(null), hasTicker(null), //
                        hasName("BDT Media Automation GmbH Nachr.Anl.v.2017(2019/2024)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-14T00:00"), hasShares(20.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1982.15), hasGrossValue("EUR", 2000.00), //
                        hasTaxes("EUR", 14.85 + 0.50), hasFees("EUR", 2.50))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-07-15T00:00"), hasShares(20.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.85), hasGrossValue("EUR", 14.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

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
                        hasIsin("AT0000A3EPA4"), hasWkn(null), hasTicker(null), //
                        hasName("ams-OSRAM AG Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-02-18T10:56:36"), hasShares(133.00), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("Auftrags-Nr.: 49674060 | Limit: 9,900000"), //
                        hasAmount("EUR", 1311.75), hasGrossValue("EUR", 1316.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.95))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2025-02-20T00:00"), hasShares(133.00), //
                        hasSource("Verkauf06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 109.69), hasGrossValue("EUR", 109.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf07()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf07.txt"), errors);

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
                        hasIsin("US9344231041"), hasWkn(null), hasTicker(null), //
                        hasName("Warner Bros. Discovery Inc. Reg. Shares Series A DL-,01"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-05-23T00:00"), hasShares(0.383), //
                        hasSource("Verkauf07.txt"), //
                        hasNote("Auftrags-Nr.: 66867637"), //
                        hasAmount("EUR", 6.27), hasGrossValue("EUR", 6.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2022-05-23T00:00"), hasShares(0.383), //
                        hasSource("Verkauf07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.61), hasGrossValue("EUR", 0.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf08()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0F5UH1"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-11-03T15:23:08"), hasShares(172.811), //
                        hasSource("Verkauf08.txt"), //
                        hasNote("Auftrags-Nr.: 90702744"), //
                        hasAmount("EUR", 5499.08), hasGrossValue("EUR", 5668.20), //
                        hasTaxes("EUR", 158.37), hasFees("EUR", 0.80 + 9.95))));

    }

    @Test
    public void testDividende01()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0008404005"), hasWkn(null), hasTicker(null), //
                        hasName("A l l i a n z  S E vink.Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-09T00:00"), hasExDate("2022-05-05T00:00"), //
                        hasShares(115.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 910.69), hasGrossValue("EUR", 1242.00), //
                        hasTaxes("EUR", 327.58 + 0.62), hasFees("EUR", 3.11))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0005552004"), hasWkn(null), hasTicker(null), //
                        hasName("D E U T S C H E  P O ST AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-11T00:00"), hasExDate("2022-05-09T00:00"), //
                        hasShares(1250.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1649.80), hasGrossValue("EUR", 2250.00), //
                        hasTaxes("EUR", 593.44 + 1.13), hasFees("EUR", 5.63))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B9CQXS71"), hasWkn(null), hasTicker(null), //
                        hasName("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-12T00:00"), hasExDate("2022-05-03T00:00"), //
                        hasShares(1204.86), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 315.56), hasGrossValue("EUR", (336.16 / 1.06145)), //
                        hasForexGrossValue("USD", 336.16), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende03WithSecurityinEUR()
    {
        var security = new Security("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N.", "EUR");
        security.setIsin("IE00B9CQXS71");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

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
                        hasDate("2022-05-12T00:00"), hasExDate("2022-05-03T00:00"), //
                        hasShares(1204.86), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 315.56), hasGrossValue("EUR", (336.16 / 1.06145)), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("US7427181091"), hasWkn(null), hasTicker(null), //
                        hasName("P R O C T E R  &  G AMBLE CO., THE Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-17T00:00"), hasExDate("2022-04-21T00:00"), //
                        hasShares(200.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 120.62), hasGrossValue("EUR", (182.66 / 1.05005)), //
                        hasForexGrossValue("USD", 182.66), //
                        hasTaxes("EUR", (54.80 / 1.05005) + 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende04WithSecurityinEUR()
    {
        var security = new Security("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N.", "EUR");
        security.setIsin("US7427181091");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

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
                        hasDate("2022-05-17T00:00"), hasExDate("2022-04-21T00:00"), //
                        hasShares(200.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 120.62), hasGrossValue("EUR", (182.66 / 1.05005)), //
                        hasTaxes("EUR", (54.80 / 1.05005) + 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0007164600"), hasWkn(null), hasTicker(null), //
                        hasName("S A P  S E Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-24T00:00"), hasExDate("2022-05-19T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 179.24), hasGrossValue("EUR", 245.00), //
                        hasTaxes("EUR", 64.62 + 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B1XNHC34"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s I I - G l . C lean Energy U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-27T00:00"), hasExDate("2022-05-12T00:00"), //
                        hasShares(3568.5778), //
                        hasSource("Dividende06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 101.96), hasGrossValue("EUR", (110.63 / 1.07305)), //
                        hasForexGrossValue("USD", 110.63), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende06WithSecurityinEUR()
    {
        var security = new Security("i S h s I I - G l . C lean Energy U.ETF Registered Shares o.N.", "EUR");
        security.setIsin("IE00B1XNHC34");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

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
                        hasDate("2022-05-27T00:00"), hasExDate("2022-05-12T00:00"), //
                        hasShares(3568.5778), //
                        hasSource("Dividende06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 101.96), hasGrossValue("EUR", (110.63 / 1.07305)), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("AT0000APOST4"), hasWkn(null), hasTicker(null), //
                        hasName("O E S T E R R E I C H ISCHE POST AG AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-05T00:00"), hasExDate("2022-05-03T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 137.75), hasGrossValue("EUR", 190.00), //
                        hasTaxes("EUR", 52.25), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0008404005"), hasWkn(null), hasTicker(null), //
                        hasName("A l l i a n z  S E vink.Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-09T00:00"), hasExDate("2022-05-05T00:00"), //
                        hasShares(115.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 910.69), hasGrossValue("EUR", 1242.00), //
                        hasTaxes("EUR", 327.58 + 0.62), hasFees("EUR", 3.11))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0005552004"), hasWkn(null), hasTicker(null), //
                        hasName("D E U T S C H E  P O ST AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-11T00:00"), hasExDate("2022-05-09T00:00"), //
                        hasShares(1250.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1649.80), hasGrossValue("EUR", 2250.00), //
                        hasTaxes("EUR", 593.44 + 1.13), hasFees("EUR", 5.63))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B9CQXS71"), hasWkn(null), hasTicker(null), //
                        hasName("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-12T00:00"), hasExDate("2022-05-03T00:00"), //
                        hasShares(1204.86), //
                        hasSource("Dividende10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 315.56), hasGrossValue("EUR", 316.70), //
                        hasForexGrossValue("USD", 336.16), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende10WithSecurityinEUR()
    {
        var security = new Security("S P D R  S & P  G l ob.Div.Aristocr.ETF Registered Shares o.N.", "EUR");
        security.setIsin("IE00B9CQXS71");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

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
                        hasDate("2022-05-12T00:00"), hasExDate("2022-05-03T00:00"), //
                        hasShares(1204.86), //
                        hasSource("Dividende10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 315.56), hasGrossValue("EUR", 316.70), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende11()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0007164600"), hasWkn(null), hasTicker(null), //
                        hasName("S A P  S E Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-24T00:00"), hasExDate("2022-05-19T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 179.24), hasGrossValue("EUR", 245.00), //
                        hasTaxes("EUR", 64.62 + 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende12()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B1XNHC34"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s I I - G l . C lean Energy U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-27T00:00"), hasExDate("2022-05-12T00:00"), //
                        hasShares(3568.5778), //
                        hasSource("Dividende12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 101.96), hasGrossValue("EUR", 103.10), //
                        hasForexGrossValue("USD", 110.63), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende12WithSecurityinEUR()
    {
        var security = new Security("i S h s I I - G l . C lean Energy U.ETF Registered Shares o.N.", "EUR");
        security.setIsin("IE00B1XNHC34");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

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
                        hasDate("2022-05-27T00:00"), hasExDate("2022-05-12T00:00"), //
                        hasShares(3568.5778), //
                        hasSource("Dividende12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 101.96), hasGrossValue("EUR", 103.10), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende13()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("AT0000821103"), hasWkn(null), hasTicker(null), //
                        hasName("UNIQA Insurance Group AG Stamm-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-06-07T00:00"), hasExDate("2022-06-02T00:00"), //
                        hasShares(1200.00), //
                        hasSource("Dividende13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 478.50), hasGrossValue("EUR", 660.00), //
                        hasTaxes("EUR", 181.50), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0005937007"), hasWkn(null), hasTicker(null), //
                        hasName("MAN SE Inhaber-Stammaktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-07-02T00:00"), hasExDate("2021-06-30T00:00"), //
                        hasShares(60.00), //
                        hasSource("Dividende14.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.03), hasGrossValue("EUR", 6.60), //
                        hasTaxes("EUR", 0.83 + 1.74), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("US46625H1005"), hasWkn(null), hasTicker(null), //
                        hasName("JPMorgan Chase & Co. Registered Shares DL 1"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-11-02T00:00"), hasExDate("2022-10-05T00:00"), //
                        hasShares(16.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 11.56), hasGrossValue("EUR", 16.00 * 0.99975), //
                        hasForexGrossValue("USD", 16.00), //
                        hasTaxes("EUR", (2.00 + 2.40) * 0.99975), hasFees("EUR", 0.04))));
    }

    @Test
    public void testDividende15WithSecurityinEUR()
    {
        var security = new Security("JPMorgan Chase & Co. Registered Shares DL 1", "EUR");
        security.setIsin("US46625H1005");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

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
                        hasDate("2022-11-02T00:00"), hasExDate("2022-10-05T00:00"), //
                        hasShares(16.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 11.56), hasGrossValue("EUR", 16.00 * 0.99975), //
                        hasTaxes("EUR", (2.00 + 2.40) * 0.99975), hasFees("EUR", 0.04))));
    }

    @Test
    public void testDividende16()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0F5UK5"), hasWkn(null), hasTicker(null), //
                        hasName("i S h . S T . E u . 6 00 Bas.Res.U.ETF DE Inhaber-Anlageaktien"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-10-17T00:00"), hasExDate("2022-10-17T00:00"), //
                        hasShares(966.48), //
                        hasSource("Dividende16.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1043.22), hasGrossValue("EUR", 1046.36), //
                        hasTaxes("EUR", 0.52), hasFees("EUR", 2.62))));
    }

    @Test
    public void testDividende17()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0F5UH1"), hasWkn(null), hasTicker(null), //
                        hasName("i S h . S T . G l . S el.Div.100 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-10-19T00:00"), hasExDate("2022-10-17T00:00"), //
                        hasShares(750.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 232.49), hasGrossValue("EUR", 233.63), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende18()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("IE0032523478"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s - E O  C o r p Bd Lar.Cap U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-09-28T00:00"), hasExDate("2022-09-15T00:00"), //
                        hasShares(374.95), //
                        hasSource("Dividende18.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 116.93), hasGrossValue("EUR", 118.07), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende19()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A14J587"), hasWkn(null), hasTicker(null), //
                        hasName("t h y s s e n k r u p p AG Medium Term Notes v.15(25)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-02-27T00:00"), hasExDate("2023-02-25T00:00"), //
                        hasShares(20.00), //
                        hasSource("Dividende19.txt"), //
                        hasNote("2,5 % von 25.2.2022 bis 24.2.2023"), //
                        hasAmount("EUR", 48.86), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende20()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B9CQXS71"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR S&P Glob.Div.Aristocr.ETF Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-11-13T00:00"), hasExDate("2024-11-01T00:00"), //
                        hasShares(31.807), //
                        hasSource("Dividende20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.09), hasGrossValue("EUR", 8.59), //
                        hasForexGrossValue("USD", 9.18), //
                        hasTaxes("EUR", 2.55), hasFees("EUR", 0.95))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-11-13T00:00"), hasShares(31.807), //
                        hasSource("Dividende20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.36), hasGrossValue("EUR", 2.36), //
                        hasForexGrossValue("USD", 2.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende20WithSecurityInEUR()
    {
        var security = new Security("SPDR S&P Glob.Div.Aristocr.ETF Registered Shares o.N.", "EUR");
        security.setIsin("IE00B9CQXS71");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-11-13T00:00"), hasExDate("2024-11-01T00:00"), //
                        hasShares(31.807), //
                        hasSource("Dividende20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.09), hasGrossValue("EUR", 8.59), //
                        hasTaxes("EUR", 2.55), hasFees("EUR", 0.95))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-11-13T00:00"), hasShares(31.807), //
                        hasSource("Dividende20.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.36), hasGrossValue("EUR", 2.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende21()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("US3765461070"), hasWkn(null), hasTicker(null), //
                        hasName("Gladstone Investment Corp. Registered Shares DL -,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-02-02T00:00"), hasExDate("2019-01-17T00:00"), //
                        hasShares(120.00), //
                        hasSource("Dividende21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.11), hasGrossValue("EUR", 7.07), //
                        hasForexGrossValue("USD", 8.16), //
                        hasTaxes("EUR", 1.94), hasFees("EUR", 0.02))));
    }

    @Test
    public void testDividende21WithSecurityInEUR()
    {
        var security = new Security("Gladstone Investment Corp. Registered Shares DL -,001", "EUR");
        security.setIsin("US3765461070");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende21.txt"), errors);

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
                        hasDate("2019-02-02T00:00"), hasExDate("2019-01-17T00:00"), //
                        hasShares(120.00), //
                        hasSource("Dividende21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.11), hasGrossValue("EUR", 7.07), //
                        hasTaxes("EUR", 1.94), hasFees("EUR", 0.02))));
    }

    @Test
    public void testDividende22()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende22.txt"), errors);

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
                        hasIsin("US3765461070"), hasWkn(null), hasTicker(null), //
                        hasName("Gladstone Investment Corp. Registered Shares DL -,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-02-03T00:00"), hasExDate("2020-01-23T00:00"), //
                        hasShares(120.00), //
                        hasSource("Dividende22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.47), hasGrossValue("EUR", 7.58), //
                        hasForexGrossValue("USD", 8.40), //
                        hasTaxes("EUR", 2.09), hasFees("EUR", 0.02))));
    }

    @Test
    public void testDividende22WithSecurityInEUR()
    {
        var security = new Security("Gladstone Investment Corp. Registered Shares DL -,001", "EUR");
        security.setIsin("US3765461070");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende22.txt"), errors);

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
                        hasDate("2020-02-03T00:00"), hasExDate("2020-01-23T00:00"), //
                        hasShares(120.00), //
                        hasSource("Dividende22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.47), hasGrossValue("EUR", 7.58), //
                        hasTaxes("EUR", 2.09), hasFees("EUR", 0.02))));
    }

    @Test
    public void testDividende23()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende23.txt"), errors);

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
                        hasIsin("GB0002875804"), hasWkn(null), hasTicker(null), //
                        hasName("BRITISH AMERICAN TOBACCO PLC Registered Shares LS -,25"), //
                        hasCurrencyCode("GBP"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-05-06T00:00"), hasExDate("2022-03-24T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 46.60), hasGrossValue("EUR", 64.51), //
                        hasForexGrossValue("GBP", 54.45), //
                        hasTaxes("EUR", 17.75), hasFees("EUR", 0.16))));
    }

    @Test
    public void testDividende23WithSecurityInEUR()
    {
        var security = new Security("BRITISH AMERICAN TOBACCO PLC Registered Shares LS -,25", "EUR");
        security.setIsin("GB0002875804");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende23.txt"), errors);

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
                        hasDate("2022-05-06T00:00"), hasExDate("2022-03-24T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 46.60), hasGrossValue("EUR", 64.51), //
                        hasTaxes("EUR", 17.75), hasFees("EUR", 0.16))));
    }

    @Test
    public void testDividende24()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB0002875804"), hasWkn(null), hasTicker(null), //
                        hasName("BRITISH AMERICAN TOBACCO PLC Registered Shares LS -,25"), //
                        hasCurrencyCode("GBP"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-02-03T00:00"), hasExDate("2023-12-21T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 48.59), hasGrossValue("EUR", 67.27), //
                        hasForexGrossValue("GBP", 57.72), //
                        hasTaxes("EUR", 18.51), hasFees("EUR", 0.17))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-02-03T00:00"), hasShares(100.00), //
                        hasSource("Dividende24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 18.50), hasGrossValue("EUR", 18.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende24WithSecurityInEUR()
    {
        var security = new Security("BRITISH AMERICAN TOBACCO PLC Registered Shares LS -,25", "EUR");
        security.setIsin("GB0002875804");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-02-03T00:00"), hasExDate("2023-12-21T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 48.59), hasGrossValue("EUR", 67.27), //
                        hasTaxes("EUR", 18.51), hasFees("EUR", 0.17))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-02-03T00:00"), hasShares(100.00), //
                        hasSource("Dividende24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 18.50), hasGrossValue("EUR", 18.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende25()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende25.txt"), errors);

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
                        hasIsin("US02079K3059"), hasWkn(null), hasTicker(null), //
                        hasName("Alphabet Inc. Reg. Shs Cap.Stk Cl. A DL-,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-03-18T00:00"), hasExDate("2026-03-09T00:00"), //
                        hasShares(700.00), //
                        hasSource("Dividende25.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 92.00), hasGrossValue("EUR", 127.34), //
                        hasForexGrossValue("USD", 147.00), //
                        hasTaxes("EUR", (15.92 + 19.10)), hasFees("EUR", 0.32))));
    }

    @Test
    public void testDividende25WithSecurityInEUR()
    {
        var security = new Security("Alphabet Inc. Reg. Shs Cap.Stk Cl. A DL-,001", "EUR");
        security.setIsin("US02079K3059");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende25.txt"), errors);

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
                        hasDate("2026-03-18T00:00"), hasExDate("2026-03-09T00:00"), //
                        hasShares(700.00), //
                        hasSource("Dividende25.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 92.00), hasGrossValue("EUR", 127.34), //
                        hasTaxes("EUR", (15.92 + 19.10)), hasFees("EUR", 0.32))));
    }

    @Test
    public void testDividendeStorno01()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

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
                        hasIsin("US3765461070"), hasWkn(null), hasTicker(null), //
                        hasName("Gladstone Investment Corp. Registered Shares DL -,001"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2021-01-04T00:00"), hasExDate("2020-12-22T00:00"), //
                                        hasShares(180.00), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 7.38), hasGrossValue("EUR", 10.22), //
                                        hasForexGrossValue("USD", 12.60), //
                                        hasTaxes("EUR", 2.81), hasFees("EUR", 0.03)))));
    }

    @Test
    public void testDividendeStorno01WithSecurityInEUR()
    {
        var security = new Security("Gladstone Investment Corp. Registered Shares DL -,001", "EUR");
        security.setIsin("US3765461070");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

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
                                        hasDate("2021-01-04T00:00"), hasExDate("2020-12-22T00:00"), //
                                        hasShares(180.00), //
                                        hasSource("DividendeStorno01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 7.38), hasGrossValue("EUR", 10.22), //
                                        hasTaxes("EUR", 2.81), hasFees("EUR", 0.03)))));
    }

    @Test
    public void testSteuerkorrektur01()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerkorrektur01.txt"), errors);

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
                        hasIsin("IE00B0M62Q58"), hasWkn(null), hasTicker(null), //
                        hasName("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-12-31T00:00"), hasShares(57.802), //
                        hasSource("Steuerkorrektur01.txt"), //
                        hasNote("Steuerkorrektur | 47346359-18.12.2024/Nr.1"), //
                        hasAmount("EUR", 1.76), hasGrossValue("EUR", 1.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerkorrektur02()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerkorrektur02.txt"), errors);

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
                        hasIsin("IE00BF4RFH31"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-MSCI Wld Sm.Ca.UCI.ETF Registered Shares USD(Acc)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-10T00:00"), hasShares(753.259), //
                        hasSource("Steuerkorrektur02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 23.11), hasGrossValue("EUR", 23.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerkorrektur02WithSecurityInUSD()
    {
        var security = new Security("iShsIII-MSCI Wld Sm.Ca.UCI.ETF Registered Shares USD(Acc)o.N.", "USD");
        security.setIsin("IE00BF4RFH31");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new EasyBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerkorrektur02.txt"), errors);

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
                        hasDate("2025-01-10T00:00"), hasShares(753.259), //
                        hasSource("Steuerkorrektur02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 23.11), hasGrossValue("EUR", 23.11), //
                        hasForexGrossValue("USD", 23.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSteuerkorrektur03()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Steuerkorrektur03.txt"), errors);

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
                        hasIsin("DE000A0F5UH1"), hasWkn(null), hasTicker(null), //
                        hasName("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-07-30T00:00"), hasShares(172.811), //
                        hasSource("Steuerkorrektur03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 38.24 - 7.48), hasGrossValue("EUR", 38.24 - 7.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug01()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-06-28"), hasAmount("EUR", 2000.00), //
                        hasSource("Depotauszug01.txt"), hasNote("REF: 38000220627-5336530-0000561"))));
    }

    @Test
    public void testDepotauszug02()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-10-18"), hasAmount("EUR", 1000.00), //
                        hasSource("Depotauszug02.txt"), hasNote("REF: 38000221017-1648514-0002320"))));
    }

    @Test
    public void testDepotauszug03()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        assertThat(results, hasItem(deposit(hasDate("2022-11-29"), hasAmount("EUR", 10000.00), //
                        hasSource("Depotauszug03.txt"), hasNote("REF: 38000221128-0294080-0002067"))));
    }

    @Test
    public void testDepotauszug04()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2022-09-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Kontoführungsgebühr"), //
                        hasAmount("EUR", 4.50), hasGrossValue("EUR", 4.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testUmtausch01()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Umtausch01.txt"), errors);

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
                        hasIsin("AT0000A1Z023"), hasWkn(null), hasTicker(null), //
                        hasName("CONWERT IMMOBILIEN INVEST SE AKTIEN O.N./ANSPR.NACHZAHLUNG"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        outboundDelivery( //
                                        hasDate("2025-02-18T00:00"), hasShares(27.00), //
                                        hasSource("Umtausch01.txt"), //
                                        hasNote("Auftrags-Nr.: 17374528 - 28.01.2025"), //
                                        hasAmount("EUR", 14.85), hasGrossValue("EUR", 14.85), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testAusbuchungWegenTitelumtausch01()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AusbuchungWegenTitelumtausch01.txt"),
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
                        hasIsin("US4592001014"), hasWkn(null), hasTicker(null), //
                        hasName("INTL BUSINESS MACHINES CORP. Registered Shares DL -,20"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        inboundDelivery( //
                                        hasDate("2021-10-25T00:00"), hasShares(4.00), //
                                        hasSource("AusbuchungWegenTitelumtausch01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKapitalmaßnahmeWegenEntflechtung01()
    {
        var extractor = new EasyBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KapitalmaßnahmeWegenEntflechtung01.txt"),
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
                        hasIsin("GB00B03MLX29"), hasWkn(null), hasTicker(null), //
                        hasName("Shell PLC Reg. Shares Class A EO -,07"), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionSplitUnsupported, //
                        outboundDelivery( //
                                        hasDate("2022-01-28T00:00"), hasShares(50.00), //
                                        hasSource("KapitalmaßnahmeWegenEntflechtung01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }
}
