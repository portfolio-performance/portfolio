package name.abuchen.portfolio.datatransfer.pdf.quirinbankag;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSecurity;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
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
import name.abuchen.portfolio.datatransfer.pdf.QuirinBankAGPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class QuirinPrivatbankAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("LU0690964092"), hasWkn("DBX0MF"), hasTicker(null), //
                        hasName("db x-tr.II Gl Sovereign ETF Inhaber-Anteile 1D EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2016-12-30T12:46:28"), hasShares(140.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Ref.-Nr.: 28522373"), //
                        hasAmount("EUR", 30090.76), hasGrossValue("EUR", 30085.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.90))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0002643889"), hasWkn(null), hasTicker(null), //
                        hasName("iShares PLC - S&P 500 Index Fd Bearer Shares (Dt. Zert.) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-02-10T17:08:23"), hasShares(515.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Ref.-Nr.: O:000481758:1"), //
                        hasAmount("EUR", 3997.09), hasGrossValue("EUR", 3996.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.08 + 0.02 + 0.69 + 0.04))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("US4282361033"), hasWkn(null), hasTicker(null), //
                        hasName("Hewlett-Packard Co. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2010-08-20T15:46:50"), hasShares(150.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Ref.-Nr.: O:000745405:1"), //
                        hasAmount("EUR", 4749.86), hasGrossValue("EUR", 4734.08), //
                        hasForexGrossValue("USD", 5999.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 15.78))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInEUR()
    {
        var security = new Security("Hewlett-Packard Co. Registered Shares DL -,01", "EUR");
        security.setIsin("US4282361033");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new QuirinBankAGPDFExtractor(client);

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
                        hasDate("2010-08-20T15:46:50"), hasShares(150.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Ref.-Nr.: O:000745405:1"), //
                        hasAmount("EUR", 4749.86), hasGrossValue("EUR", 4734.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 15.78), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

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
                        hasIsin("FR0010135103"), hasWkn("A0DPW0"), hasTicker(null), //
                        hasName("Carmignac Patrimoine FCP Act.au Port.A EUR acc o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2014-03-05T00:00"), hasShares(0.1318), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Ref.-Nr.: O:002831939:1"), //
                        hasAmount("EUR", 75.00), hasGrossValue("EUR", 75.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2014-03-10T00:00"), hasShares(0.1318), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Zwischengewinn 0,17 EUR | Ref.-Nr.: O:002831939:1"), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("iSh.STOXX Europe 600 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2014-09-12T16:22:33"), hasShares(31.00), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Ref.-Nr.: O:003198103:1"), //
                        hasAmount("EUR", 1087.55), hasGrossValue("EUR", 1082.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.12 + 0.75))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B0M63177"), hasWkn("A0HGWC"), hasTicker(null), //
                        hasName("iShs MSCI EM U.ETF USD (D) Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-12-06T16:52:15"), hasShares(325.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ref.-Nr.: 123452676"), //
                        hasAmount("EUR", 12766.68), hasGrossValue("EUR", 13569.40), //
                        hasTaxes("EUR", 752.05 + 41.36), hasFees("EUR", 0.81 + 8.50))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0007873200"), hasWkn(null), hasTicker(null), //
                        hasName("Bayer.Hypo- und Vereinsbank AG DAX Indexzert(2006/unlim.)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2009-11-23T11:00:15"), hasShares(22.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Ref.-Nr.: O:000409887:1"), //
                        hasAmount("EUR", 1261.61), hasGrossValue("EUR", 1265.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.39))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0002643889"), hasWkn(null), hasTicker(null), //
                        hasName("iShares PLC - S&P 500 Index Fd Bearer Shares (Dt. Zert.) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2010-05-07T16:20:11"), hasShares(515.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Ref.-Nr.: O:000591758:1"), //
                        hasAmount("EUR", 4515.86 - 144.33), hasGrossValue("EUR", 4516.55), //
                        hasTaxes("EUR", 127.15 + 7.00 + 10.18), hasFees("EUR", 0.69))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("LU0327757729"), hasWkn(null), hasTicker(null), //
                        hasName("F.Temp.Inv.Fds-T.Growth (EUR) Namens-Anteile A (acc.)DL o.N."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2009-11-23T00:00"), hasShares(378.362), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Ref.-Nr.: O:000409894:1"), //
                        hasAmount("EUR", 3046.43), hasGrossValue("EUR", 3052.93), //
                        hasForexGrossValue("USD", 4597.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.50))));
    }

    @Test
    public void testWertpapierVerkauf04WithSecurityInEUR()
    {
        var security = new Security("F.Temp.Inv.Fds-T.Growth (EUR) Namens-Anteile A (acc.)DL o.N.", "EUR");
        security.setIsin("LU0327757729");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new QuirinBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

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
                        hasDate("2009-11-23T00:00"), hasShares(378.362), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Ref.-Nr.: O:000409894:1"), //
                        hasAmount("EUR", 3046.43), hasGrossValue("EUR", 3052.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.50), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("LU0052750758"), hasWkn("973909"), hasTicker(null), //
                        hasName("Fr.Temp.Inv.Fds-T.China Fd Namens-Anteile A (acc.) o.N."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2014-09-15T00:00"), hasShares(340.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Ref.-Nr.: O:003198102:1"), //
                        hasAmount("EUR", 6185.81 - 87.65), hasGrossValue("EUR", 6192.31), //
                        hasForexGrossValue("USD", 8024.00), //
                        hasTaxes("EUR", 77.22 + 4.25 + 6.18), hasFees("EUR", 6.50))));
    }

    @Test
    public void testWertpapierVerkauf05WithSecurityInEUR()
    {
        var security = new Security("Fr.Temp.Inv.Fds-T.China Fd Namens-Anteile A (acc.) o.N.", "EUR");
        security.setIsin("LU0052750758");
        security.setWkn("973909");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new QuirinBankAGPDFExtractor(client);

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

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2014-09-15T00:00"), hasShares(340.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Ref.-Nr.: O:003198102:1"), hasAmount("EUR", 6185.81 - 87.65),
                        hasGrossValue("EUR", 6192.31), //
                        hasTaxes("EUR", 77.22 + 4.25 + 6.18), hasFees("EUR", 6.50), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0D8Q07"), hasWkn("A0D8Q0"), hasTicker(null), //
                        hasName("iShare.EURO STOXX UCITS ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-09-16T00:00"), hasExDate("2019-09-16T00:00"), //
                        hasShares(700), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Ref.-Nr.: 12345858"), //
                        hasAmount("EUR", 343.46), hasGrossValue("EUR", 421.22), //
                        hasTaxes("EUR", 73.71 + 4.05), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0007236101"), hasWkn(null), hasTicker(null), //
                        hasName("Siemens AG Namens-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-01-27T00:00"), hasExDate("2010-01-27T00:00"), //
                        hasShares(52), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Ref.-Nr.: DZ:255990"), //
                        hasAmount("EUR", 83.20), hasGrossValue("EUR", 83.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("US8740391003"), hasWkn(null), hasTicker(null), //
                        hasName("Taiwan Semiconduct.Manufact.Co Reg.Shs (Spons.ADRs) 5/TA 10"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2010-07-27T00:00"), hasExDate("2010-07-06T00:00"), //
                        hasShares(350), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Ref.-Nr.: DZ:368384"), //
                        hasAmount("EUR", 92.67), hasGrossValue("EUR", 124.50), //
                        hasForexGrossValue("USD", 163.08), //
                        hasTaxes("EUR", 24.90 + 6.10 + 0.34 + 0.49), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("Taiwan Semiconduct.Manufact.Co Reg.Shs (Spons.ADRs) 5/TA 10", "EUR");
        security.setIsin("US8740391003");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new QuirinBankAGPDFExtractor(client);

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
                        hasDate("2010-07-27T00:00"), hasExDate("2010-07-06T00:00"), //
                        hasShares(350), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Ref.-Nr.: DZ:368384"), //
                        hasAmount("EUR", 92.67), hasGrossValue("EUR", 124.50), //
                        hasTaxes("EUR", 24.90 + 6.10 + 0.34 + 0.49), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B3F81G20"), hasWkn("A0RGER"), hasTicker(null), //
                        hasName("iShsIII-MSCI EM Sm.Cap U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-07-26T00:00"), hasExDate("2023-07-13T00:00"), //
                        hasShares(1.9983), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Ref.-Nr.: 12345858"), //
                        hasAmount("EUR", 1.41), hasGrossValue("EUR", 1.41), //
                        hasForexGrossValue("USD", 1.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("iShsIII-MSCI EM Sm.Cap U.ETF Registered Shares o.N.", "EUR");
        security.setIsin("IE00B3F81G20");
        security.setWkn("A0RGER");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new QuirinBankAGPDFExtractor(client);

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

        // check buy sell transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-07-26T00:00"), hasExDate("2023-07-13T00:00"), //
                        hasShares(1.9983), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Ref.-Nr.: 12345858"), //
                        hasAmount("EUR", 1.41), hasGrossValue("EUR", 1.41), //
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
    public void testDividende05()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("DE0002635307"), hasWkn("263530"), hasTicker(null), //
                        hasName("iSh.STOXX Europe 600 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-03-17T00:00"), hasExDate("2014-03-17T00:00"), //
                        hasShares(459.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Ref.-Nr.: DZ:1415291"), //
                        hasAmount("EUR", 39.40), hasGrossValue("EUR", 51.53), //
                        hasTaxes("EUR", 10.68 + 0.59 + 0.86), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("LU1048314196"), hasWkn("A110QF"), hasTicker(null), //
                        hasName("UBS(L)FS-BBG EO A.L.Crp1-5UETF Inhaber-Anteile A Dis.EUR"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-01-02T00:00"), hasShares(852.8631), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote("Ref.-Nr.: 350705756"), //
                        hasAmount("EUR", 4.66), hasGrossValue("EUR", 4.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B3M0BZ05"), hasWkn("A1JJAB"), hasTicker(null), //
                        hasName("Dimens.Fds-Glbl Core Equity Fd Registered Shs EUR Dis.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-01-02T00:00"), hasShares(1734.0725), //
                        hasSource("Vorabpauschale02.txt"), //
                        hasNote("Ref.-Nr.: 437935500"), //
                        hasAmount("EUR", 1.92), hasGrossValue("EUR", 1.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testQuartalsbericht01()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Quartalsbericht01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(8L));
        assertThat(countBuySell(results), is(19L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(27));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1931974692"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi Index Solu.-A.PRIME GL. Nam.-Ant.UCI.ETF DR USD Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFY0GT14"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR MSCI World UCITS ETF Reg.Shares USD Unhgd Acc. o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JM42"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE) - MSCI World Value Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF4RFH31"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-MSCI Wld Sm.Ca.UCI.ETF Registered Shares USD(Acc)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKM4GZ66"), hasWkn(null), hasTicker(null), //
                        hasName("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BG0SKF03"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIV-Edge MSCI EM Value F. Registered Shares USD (Acc.)oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B48X4842"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR MSCI Emer.Mkts Sm.Cap ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000QIF5N15"), hasWkn(null), hasTicker(null), //
                        hasName("Amu.ETF ICAV-PRIME Glbl ETF Bear.Shs USD Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-14T00:00"), hasShares(0.1789), //
                        hasSecurity(hasIsin("LU1931974692")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 535338205"), //
                        hasAmount("EUR", 6.46), hasGrossValue("EUR", 6.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-14T00:00"), hasShares(0.157), //
                        hasSecurity(hasIsin("IE00BFY0GT14")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 535338209"), //
                        hasAmount("EUR", 5.94), hasGrossValue("EUR", 5.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-14T00:00"), hasShares(0.1218), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 535338214"), //
                        hasAmount("EUR", 5.20), hasGrossValue("EUR", 5.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-14T00:00"), hasShares(0.6272), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 535338219"), //
                        hasAmount("EUR", 4.65), hasGrossValue("EUR", 4.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-14T00:00"), hasShares(0.0556), //
                        hasSecurity(hasIsin("IE00BKM4GZ66")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 535338225"), //
                        hasAmount("EUR", 1.81), hasGrossValue("EUR", 1.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-14T00:00"), hasShares(0.0274), //
                        hasSecurity(hasIsin("IE00BG0SKF03")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 535338232"), //
                        hasAmount("EUR", 1.29), hasGrossValue("EUR", 1.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-11-14T00:00"), hasShares(0.0047), //
                        hasSecurity(hasIsin("IE00B48X4842")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 535338238"), //
                        hasAmount("EUR", 0.51), hasGrossValue("EUR", 0.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-05T00:00"), hasShares(0.1526), //
                        hasSecurity(hasIsin("IE00BFY0GT14")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 541851657"), //
                        hasAmount("EUR", 5.89), hasGrossValue("EUR", 5.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-05T00:00"), hasShares(0.1189), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 541851660"), //
                        hasAmount("EUR", 5.14), hasGrossValue("EUR", 5.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-05T00:00"), hasShares(0.6004), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 541851664"), //
                        hasAmount("EUR", 4.56), hasGrossValue("EUR", 4.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-05T00:00"), hasShares(0.0544), //
                        hasSecurity(hasIsin("IE00BKM4GZ66")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 541851669"), //
                        hasAmount("EUR", 1.81), hasGrossValue("EUR", 1.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-05T00:00"), hasShares(0.0273), //
                        hasSecurity(hasIsin("IE00BG0SKF03")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 541851672"), //
                        hasAmount("EUR", 1.30), hasGrossValue("EUR", 1.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-05T00:00"), hasShares(0.0045), //
                        hasSecurity(hasIsin("IE00B48X4842")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 541851675"), //
                        hasAmount("EUR", 0.51), hasGrossValue("EUR", 0.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-06T00:00"), hasShares(0.0071), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 542126306"), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-06T00:00"), hasShares(0.0012), //
                        hasSecurity(hasIsin("IE00BFY0GT14")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 542126309"), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-12-13T00:00"), hasShares(0.0009), //
                        hasSecurity(hasIsin("IE00BKM4GZ66")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 545602004"), //
                        hasAmount("EUR", 0.03), hasGrossValue("EUR", 0.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-13T00:00"), hasShares(0.1744), //
                        hasSecurity(hasIsin("IE000QIF5N15")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 545602008"), //
                        hasAmount("EUR", 6.46), hasGrossValue("EUR", 6.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-13T00:00"), hasShares(0.0138), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 545602011"), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-13T00:00"), hasShares(0.002), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Quartalsbericht01.txt"), //
                        hasNote("Ref.-Nr.: 545602013"), //
                        hasAmount("EUR", 0.09), hasGrossValue("EUR", 0.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug01()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-05-28"), hasAmount("EUR", 3000.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Kontoübertrag 1197537 | Ref.-Nr.: 86991330"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-12-19"), hasAmount("EUR", 5000.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Sammelgutschrift | Ref.-Nr.: 105892216"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-01-31"), hasAmount("EUR", 2.84), //
                        hasSource("Depotauszug01.txt"), hasNote("Interne Buchung | Ref.-Nr.: 110934428"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-12-27"), hasAmount("EUR", 2000.00), //
                        hasSource("Depotauszug01.txt"),
                        hasNote("Überweisungsgutschrift Inland | Ref.-Nr.: 106509889"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-19"), hasAmount("EUR", 5002.84), //
                        hasSource("Depotauszug01.txt"), hasNote("Rücküberweisung Inland | Ref.-Nr.: 106317528"))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2020-06-12"), hasAmount("EUR", 36.82), //
                        hasSource("Depotauszug01.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 135435928"))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2020-01-03"), hasAmount("EUR", 0.40), //
                        hasSource("Depotauszug01.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 107410183"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2019-08-31"), hasAmount("EUR", 5.75), //
                        hasSource("Depotauszug01.txt"), hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 94613532"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2019-09-30"), hasAmount("EUR", 6.98), //
                        hasSource("Depotauszug01.txt"), hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 97492689"))));
    }

    @Test
    public void testDepotauszug02()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

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

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2019-12-31"), hasAmount("EUR", 1.70), //
                        hasSource("Depotauszug02.txt"), hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 123458336"))));
    }

    @Test
    public void testDepotauszug03()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug03.txt"), errors);

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
        assertThat(results, hasItem(taxes(hasDate("2010-06-30"), hasAmount("EUR", 1.28), //
                        hasSource("Depotauszug03.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0139925023"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2010-06-30"), hasAmount("EUR", 75.00), //
                        hasSource("Depotauszug03.txt"),
                        hasNote("Flatrate Gebühren 01.06.2010 - 30.06.2010 | Ref.-Nr.: KA-0139816662"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2010-06-30"), hasAmount("EUR", 29.55), //
                        hasSource("Depotauszug03.txt"),
                        hasNote("Volumen Fee Gebühren 01.04.2010 - 30.06.2010 | Ref.-Nr.: KA-0139816664"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2010-06-30"), hasAmount("EUR", 4.61), //
                        hasSource("Depotauszug03.txt"),
                        hasNote("Haben-Zinsen Kontoabschluss | Ref.-Nr.: KA-0139907281"))));
    }

    @Test
    public void testDepotauszug04()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(43L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(43));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.73), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144748177"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 3.77), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144754954"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 1.59), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144767171"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.28), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144772794"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.24), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144774420"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 1.33), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144775628"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.19), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144776788"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.65), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144794566"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.52), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144796585"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 1.39), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144798144"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.19), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144802453"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 1.20), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144802618"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 10.57), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144827860"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.07), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144835278"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 2.52), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144836892"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.35), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144976464"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.56), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144989517"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.73), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144994720"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.24), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144996297"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.26), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144996479"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.72), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144999672"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2010-07-30"), hasAmount("EUR", 75.00), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Flatrate Gebühren 01.07.2010 - 30.07.2010 | Ref.-Nr.: KA-0144715444"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 2.63), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0140363002 | Ref.-Nr.: KA -0144680022"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 2.03), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0303756539 | Ref.-Nr.: KA-0144683460"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 1.02), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0075056555 | Ref.-Nr.: KA-0144689194"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 0.85), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0066902890 | Ref.-Nr.: KA-0144691722"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 0.90), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0121747215 | Ref.-Nr.: KA-0144692430"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 3.12), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision DE0008490822 | Ref.-Nr.: KA-0144692886"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 4.78), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0052750758 | Ref.-Nr.: KA-0144693436"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 2.10), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0303756539 | Ref.-Nr.: KA-0144700356"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 2.59), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0140363002 | Ref.-Nr.: KA-0144701132"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 0.88), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0121747215 | Ref.-Nr.: KA-0144701836"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 4.93), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0052750758 | Ref.-Nr.: KA-01 44703846"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 0.98), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovisio n LU0075056555 | Ref.-Nr.: KA-0144703918"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 0.89), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0066902890 | Ref.-Nr.: KA-0144717889"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 2.94), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision DE0008490822 | Ref.-Nr.: KA-0144723419"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 4.83), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0052750758 | Ref.-Nr.: KA-0144724147"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 0.85), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0121747215 | Ref.-Nr.: KA-0144726719"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 2.05), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0303756539 | Ref.-Nr.: KA-0144732844"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 2.61), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision DE0008490822 | Ref.-Nr.: KA-0144735471"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 0.86), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0066902890 | Ref.-Nr.: KA-0144736296"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 0.94), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0075056555 | Ref.-Nr.: KA-0144736391"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2010-07-30"), hasAmount("EUR", 2.58), //
                        hasSource("Depotauszug04.txt"),
                        hasNote("Bestandsprovision LU0140363002 | Ref.-Nr.: KA-01 44738244"))));
    }

    @Test
    public void testDepotauszug05()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug05.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2023-10-02"), hasAmount("EUR", 250.00), //
                        hasSource("Depotauszug05.txt"), hasNote("Sammelgutschrift | Ref.-Nr.: 4*******2"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-10-16"), hasAmount("EUR", 0.48), //
                        hasSource("Depotauszug05.txt"), hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 4******6"))));
    }

    @Test
    public void testDepotauszug06()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug06.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2021-02-23"), hasAmount("EUR", 1000.00), //
                        hasSource("Depotauszug06.txt"), hasNote("Sammelgutschrift | Ref.-Nr.: 193396101"))));
    }

    @Test
    public void testDepotauszug07()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2024-04-02"), hasAmount("EUR", 56.07), //
                        hasSource("Depotauszug07.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 464710285"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2024-03-31"), hasAmount("EUR", 2.04), //
                        hasSource("Depotauszug07.txt"),
                        hasNote("Bestandsprovision LU1274520086 01.01.2024 - 31.03.2024 | Ref.-Nr.: 467260165"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2024-03-31"), hasAmount("EUR", 3.17), //
                        hasSource("Depotauszug07.txt"),
                        hasNote("Bestandsprovision LU1233758587 01.01.2024 - 31.03.2024 | Ref.-Nr.: 467260166"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-04-30"), hasAmount("EUR", 440.14), //
                        hasSource("Depotauszug07.txt"), hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 474917266"))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2024-05-02"), hasAmount("EUR", 53.38), //
                        hasSource("Depotauszug07.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 475292925"))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2024-05-21"), hasAmount("EUR", 132.52), //
                        hasSource("Depotauszug07.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 481573598"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-05-31"), hasAmount("EUR", 459.73), //
                        hasSource("Depotauszug07.txt"), hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 484298415"))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2024-06-03"), hasAmount("EUR", 55.78), //
                        hasSource("Depotauszug07.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 484782044"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-06-30"), hasAmount("EUR", 451.89), //
                        hasSource("Depotauszug07.txt"), hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 494534896"))));
    }

    @Test
    public void testDepotauszug08()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug08.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2012-01-30"), hasAmount("EUR", 500.00), //
                        hasSource("Depotauszug08.txt"), hasNote("Überweisungsauftrag | Ref.-Nr.: UT-0239371469"))));
    }

    @Test
    public void testDepotauszug09()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug09.txt"), errors);

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
        assertThat(results, hasItem(fee(hasDate("2014-03-03"), hasAmount("EUR", 100.00), //
                        hasSource("Depotauszug09.txt"), hasNote("Honorar Feb/2014 | Ref.-Nr.: KA-0383911958"))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2014-03-03"), hasAmount("EUR", 13.91), //
                        hasSource("Depotauszug09.txt"),
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0383929197"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2014-03-09"), hasAmount("EUR", 1198.98), //
                        hasSource("Depotauszug09.txt"), hasNote("Überweisungsauftrag | Ref.-Nr.: UI-0385701357"))));
    }

    @Test
    public void testDepotauszug10()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug10.txt"), errors);

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
        assertThat(results, hasItem(taxRefund(hasDate("2025-01-02"), hasAmount("EUR", 60.16), //
                        hasSource("Depotauszug10.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 551694863"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2024-12-31"), hasAmount("EUR", 2.31), //
                        hasSource("Depotauszug10.txt"),
                        hasNote("Bestandsprovision LU1274520086 01.10.2024 - 31.12.2024 | Ref.-Nr.: 554023678"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2024-12-31"), hasAmount("EUR", 6.96), //
                        hasSource("Depotauszug10.txt"),
                        hasNote("Bestandsprovision LU1233758587 01.10.2024 - 31.12.2024 | Ref.-Nr.: 554023693"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2025-01-31"), hasAmount("EUR", 507.98), //
                        hasSource("Depotauszug10.txt"), hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 563214268"))));

    }

    @Test
    public void testDepotauszug11()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug11.txt"), errors);

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
        assertThat(results, hasItem(taxRefund(hasDate("2025-06-02"), hasAmount("EUR", 58.85), //
                        hasSource("Depotauszug11.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 610616214"))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2025-06-13"), hasAmount("EUR", 14.24), //
                        hasSource("Depotauszug11.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 615820948"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2025-06-16"), hasAmount("EUR", 7.39), //
                        hasSource("Depotauszug11.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 616720729"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2025-06-30"), hasAmount("EUR", 469.61), //
                        hasSource("Depotauszug11.txt"), hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 620879559"))));
    }

    @Test
    public void testDepotauszug12()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug12.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2024-07-10"), hasAmount("EUR", 2.00), //
                        hasSource("Depotauszug12.txt"),
                        hasNote("Überweisungsgutschrift Inland | Ref.-Nr.: 499025059"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-25"), hasAmount("EUR", 25.00), //
                        hasSource("Depotauszug12.txt"),
                        hasNote("Überweisungsgutschrift Inland | Ref.-Nr.: 231123912"))));
    }
}
