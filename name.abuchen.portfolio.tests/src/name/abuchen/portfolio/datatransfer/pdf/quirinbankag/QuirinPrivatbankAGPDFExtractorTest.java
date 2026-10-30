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
                        hasSecurity(hasIsin("FR0010135103")), //
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
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(11L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(19));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JM42"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE)MSCI World Value Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1681045370"), hasWkn(null), hasTicker(null), //
                        hasName("AIS-Amundi MSCI EMERG.MARKETS Namens-Anteile C Cap.EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BCBJG560"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR MSCI Wrld Small Cap U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B42THM37"), hasWkn(null), hasTicker(null), //
                        hasName("Dimensional Fds-Emerg.Mkts Va. Registered Shares EUR Dis.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-05-28"), hasAmount("EUR", 3000.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Kontoübertrag 1197537 | Ref.-Nr.: 86991330"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-12-19"), hasAmount("EUR", 5000.00), //
                        hasSource("Depotauszug01.txt"), hasNote("Sammelgutschrift | Ref.-Nr.: 105892216"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-01-31"), hasAmount("EUR", 2.84), //
                        hasSource("Depotauszug01.txt"), hasNote("Interne Buchung | Ref.-Nr.: 110934428"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-12-27"), hasAmount("EUR", 2000.00), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote("Überweisungsgutschrift Inland | Ref.-Nr.: 106509889"))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-19"), hasAmount("EUR", 5002.84), //
                        hasSource("Depotauszug01.txt"), hasNote("Rücküberweisung Inland | Ref.-Nr.: 106317528"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund(hasDate("2020-06-12"), hasAmount("EUR", 36.82), //
                        hasSource("Depotauszug01.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 135435928"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund(hasDate("2020-01-03"), hasAmount("EUR", 0.40), //
                        hasSource("Depotauszug01.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 107410183"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-08-31"), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 94613532"), //
                        hasAmount("EUR", 5.75), hasGrossValue("EUR", 5.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-09-30"), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 97492689"), //
                        hasAmount("EUR", 6.98), hasGrossValue("EUR", 6.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-10T00:00"), hasShares(16.091), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote("Ref.-Nr.: 174680182"), //
                        hasAmount("EUR", 428.06), hasGrossValue("EUR", 428.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-03T00:00"), hasShares(102.054), //
                        hasSecurity(hasIsin("LU1681045370")), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote("Ref.-Nr.: 133305911"), //
                        hasAmount("EUR", 408.26), hasGrossValue("EUR", 408.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-02T00:00"), hasShares(28.476), //
                        hasSecurity(hasIsin("IE00BCBJG560")), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote("Thesaurierung | Ref.-Nr.: 111353113"), //
                        hasAmount("EUR", 0.40), hasGrossValue("EUR", 0.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-03T00:00"), hasShares(449.231), //
                        hasSecurity(hasIsin("LU1681045370")), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote("Thesaurierung | Ref.-Nr.: 111727648"), //
                        hasAmount("EUR", 0.49), hasGrossValue("EUR", 0.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-05-08T00:00"), hasShares(3.648), //
                        hasSecurity(hasIsin("IE00BCBJG560")), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote("Ref.-Nr.: 85249245"), //
                        hasAmount("EUR", 235.86), hasGrossValue("EUR", 235.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-01-03T00:00"), hasShares(10.763), //
                        hasSecurity(hasIsin("IE00B42THM37")), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote("Ref.-Nr.: 106663542"), //
                        hasAmount("EUR", 110.86), hasGrossValue("EUR", 110.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug02()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(10L));
        assertThat(countBuySell(results), is(10L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(22));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1931974692"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi Index Solu.-A.PRIME GL. Nam.-Ant.UCI.ETF DR USD Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1650487413"), hasWkn(null), hasTicker(null), //
                        hasName("MUL-LYX.EO Gov.Bd 1-3Y(DR)U.E. Inhaber-Anteile Acc o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BDBRDM35"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-Core Gl.Aggr.Bd UC.ETF Registered Shs EUR Acc.hgd o.N"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JN58"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE)-MSCI World Minim.Vol. Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFY0GT14"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR MSCI World UCITS ETF Reg.Shares USD Unhgd Acc. o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2037748774"), hasWkn(null), hasTicker(null), //
                        hasName("Am.I.S.-AM.IDX EO COR.SRI 0-3Y Act.Nom.UCITS ETF EUR DR Ac.oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JP72"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE)MSCI World Momentum Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1931975079"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi I.S.-A.PRIME EURO CORP. Nam.-Ant.UC.ETF DR EUR Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKM4GZ66"), hasWkn(null), hasTicker(null), //
                        hasName("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1109942653"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.II EUR H.Yield Corp.Bond Inhaber-Anteile 1D o.N."), //
                        hasCurrencyCode("EUR"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-12-02"), hasAmount("EUR", 500.00), //
                        hasSource("Depotauszug02.txt"), hasNote("Überweisungsgutschrift aus dem Ausland | Ref.-Nr.: 123453609"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-12-31"), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 123458336"), //
                        hasAmount("EUR", 1.70), hasGrossValue("EUR", 1.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(4.466), //
                        hasSecurity(hasIsin("LU1931974692")), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Ref.-Nr.: 123456385"), //
                        hasAmount("EUR", 96.46), hasGrossValue("EUR", 96.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(0.526), //
                        hasSecurity(hasIsin("LU1650487413")), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Ref.-Nr.: 123456399"), //
                        hasAmount("EUR", 65.63), hasGrossValue("EUR", 65.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(11.802), //
                        hasSecurity(hasIsin("IE00BDBRDM35")), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Ref.-Nr.: 123456407"), //
                        hasAmount("EUR", 63.23), hasGrossValue("EUR", 63.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(2.079), //
                        hasSecurity(hasIsin("IE00BL25JN58")), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Ref.-Nr.: 123456416"), //
                        hasAmount("EUR", 61.07), hasGrossValue("EUR", 61.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(2.338), //
                        hasSecurity(hasIsin("IE00BFY0GT14")), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Ref.-Nr.: 123456427"), //
                        hasAmount("EUR", 49.88), hasGrossValue("EUR", 49.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(0.946), //
                        hasSecurity(hasIsin("LU2037748774")), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Ref.-Nr.: 123456431"), //
                        hasAmount("EUR", 47.53), hasGrossValue("EUR", 47.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(1.181), //
                        hasSecurity(hasIsin("IE00BL25JP72")), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Ref.-Nr.: 123456440"), //
                        hasAmount("EUR", 45.93), hasGrossValue("EUR", 45.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(2.186), //
                        hasSecurity(hasIsin("LU1931975079")), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Ref.-Nr.: 123456446"), //
                        hasAmount("EUR", 45.65), hasGrossValue("EUR", 45.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(1.245), //
                        hasSecurity(hasIsin("IE00BKM4GZ66")), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Ref.-Nr.: 123456450"), //
                        hasAmount("EUR", 34.89), hasGrossValue("EUR", 34.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(1.651), //
                        hasSecurity(hasIsin("LU1109942653")), //
                        hasSource("Depotauszug02.txt"), //
                        hasNote("Ref.-Nr.: 123456453"), //
                        hasAmount("EUR", 28.83), hasGrossValue("EUR", 28.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
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

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-06-30"), hasAmount("EUR", 1.28), //
                        hasSource("Depotauszug03.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0139925023"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2010-06-30"), //
                        hasSource("Depotauszug03.txt"), //
                        hasNote("Flatrate Gebühren 01.06.2010 - 30.06.2010 | Ref.-Nr.: KA-0139816662"), //
                        hasAmount("EUR", 75.00), hasGrossValue("EUR", 75.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2010-06-30"), //
                        hasSource("Depotauszug03.txt"), //
                        hasNote("Volumen Fee Gebühren 01.04.2010 - 30.06.2010 | Ref.-Nr.: KA-0139816664"), //
                        hasAmount("EUR", 29.55), hasGrossValue("EUR", 29.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2010-06-30"), //
                        hasSource("Depotauszug03.txt"), //
                        hasNote("Haben-Zinsen Kontoabschluss | Ref.-Nr.: KA-0139907281"), //
                        hasAmount("EUR", 4.61), hasGrossValue("EUR", 4.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
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

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.73), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144748177"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 3.77), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144754954"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 1.59), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144767171"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.28), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144772794"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.24), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144774420"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 1.33), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144775628"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.19), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144776788"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.65), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144794566"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.52), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144796585"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 1.39), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144798144"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.19), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144802453"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 1.20), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144802618"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 10.57), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144827860"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.07), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144835278"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 2.52), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144836892"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.35), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144976464"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.56), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144989517"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.73), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144994720"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.24), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144996297"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.26), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144996479"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2010-07-30"), hasAmount("EUR", 0.72), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0144999672"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Flatrate Gebühren 01.07.2010 - 30.07.2010 | Ref.-Nr.: KA-0144715444"), //
                        hasAmount("EUR", 75.00), hasGrossValue("EUR", 75.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0140363002 | Ref.-Nr.: KA -0144680022"), //
                        hasAmount("EUR", 2.63), hasGrossValue("EUR", 2.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0303756539 | Ref.-Nr.: KA-0144683460"), //
                        hasAmount("EUR", 2.03), hasGrossValue("EUR", 2.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0075056555 | Ref.-Nr.: KA-0144689194"), //
                        hasAmount("EUR", 1.02), hasGrossValue("EUR", 1.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0066902890 | Ref.-Nr.: KA-0144691722"), //
                        hasAmount("EUR", 0.85), hasGrossValue("EUR", 0.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0121747215 | Ref.-Nr.: KA-0144692430"), //
                        hasAmount("EUR", 0.90), hasGrossValue("EUR", 0.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision DE0008490822 | Ref.-Nr.: KA-0144692886"), //
                        hasAmount("EUR", 3.12), hasGrossValue("EUR", 3.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0052750758 | Ref.-Nr.: KA-0144693436"), //
                        hasAmount("EUR", 4.78), hasGrossValue("EUR", 4.78), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0303756539 | Ref.-Nr.: KA-0144700356"), //
                        hasAmount("EUR", 2.10), hasGrossValue("EUR", 2.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0140363002 | Ref.-Nr.: KA-0144701132"), //
                        hasAmount("EUR", 2.59), hasGrossValue("EUR", 2.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0121747215 | Ref.-Nr.: KA-0144701836"), //
                        hasAmount("EUR", 0.88), hasGrossValue("EUR", 0.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0052750758 | Ref.-Nr.: KA-01 44703846"), //
                        hasAmount("EUR", 4.93), hasGrossValue("EUR", 4.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovisio n LU0075056555 | Ref.-Nr.: KA-0144703918"), //
                        hasAmount("EUR", 0.98), hasGrossValue("EUR", 0.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0066902890 | Ref.-Nr.: KA-0144717889"), //
                        hasAmount("EUR", 0.89), hasGrossValue("EUR", 0.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision DE0008490822 | Ref.-Nr.: KA-0144723419"), //
                        hasAmount("EUR", 2.94), hasGrossValue("EUR", 2.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0052750758 | Ref.-Nr.: KA-0144724147"), //
                        hasAmount("EUR", 4.83), hasGrossValue("EUR", 4.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0121747215 | Ref.-Nr.: KA-0144726719"), //
                        hasAmount("EUR", 0.85), hasGrossValue("EUR", 0.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0303756539 | Ref.-Nr.: KA-0144732844"), //
                        hasAmount("EUR", 2.05), hasGrossValue("EUR", 2.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision DE0008490822 | Ref.-Nr.: KA-0144735471"), //
                        hasAmount("EUR", 2.61), hasGrossValue("EUR", 2.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0066902890 | Ref.-Nr.: KA-0144736296"), //
                        hasAmount("EUR", 0.86), hasGrossValue("EUR", 0.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0075056555 | Ref.-Nr.: KA-0144736391"), //
                        hasAmount("EUR", 0.94), hasGrossValue("EUR", 0.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2010-07-30"), //
                        hasSource("Depotauszug04.txt"), //
                        hasNote("Bestandsprovision LU0140363002 | Ref.-Nr.: KA-01 44738244"), //
                        hasAmount("EUR", 2.58), hasGrossValue("EUR", 2.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug05()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(14L));
        assertThat(countBuySell(results), is(14L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(30));
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
                        hasIsin("IE00BF4RFH31"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-MSCI Wld Sm.Ca.UCI.ETF Registered Shares USD(Acc)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JM42"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE)MSCI World Value Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKM4GZ66"), hasWkn(null), hasTicker(null), //
                        hasName("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BDBRDM35"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-Core Gl.Aggr.Bd UC.ETF Registered Shs EUR Acc.hgd o.N"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1109942653"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.II EUR H.Yield Corp.Bond Inhaber-Anteile 1D o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FR0010754135"), hasWkn(null), hasTicker(null), //
                        hasName("AMUN.GOV.BD EO BR.IG 1-3 U.ETF Actions au Porteur o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1931975079"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi I.S.-A.PRIME EURO CORP. Nam.-Ant.UC.ETF DR EUR Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2037748774"), hasWkn(null), hasTicker(null), //
                        hasName("Am.I.S.-AM.IDX EO COR.SRI 0-3Y Act.Nom.UCITS ETF EUR DR Ac.oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JN58"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE)-MSCI World Minim.Vol. Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BGJWWY63"), hasWkn(null), hasTicker(null), //
                        hasName("InvescoMI2 EUR Gov B 1-3Y ETF Reg. Shs EUR Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BG0SKF03"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIV-Edge MSCI EM Value F. Registered Shares USD (Acc.)oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3F81G20"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-MSCI EM Sm.Cap U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2023-10-02"), hasAmount("EUR", 250.00), //
                        hasSource("Depotauszug05.txt"), hasNote("Sammelgutschrift | Ref.-Nr.: 4*******2"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2023-10-16"), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 4******6"), //
                        hasAmount("EUR", 0.48), hasGrossValue("EUR", 0.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(1.6866), //
                        hasSecurity(hasIsin("LU1931974692")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747782"), //
                        hasAmount("EUR", 45.77), hasGrossValue("EUR", 45.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(1.4495), //
                        hasSecurity(hasIsin("IE00BFY0GT14")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747787"), //
                        hasAmount("EUR", 40.37), hasGrossValue("EUR", 40.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(6.3384), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747790"), //
                        hasAmount("EUR", 36.34), hasGrossValue("EUR", 36.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(0.8758), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747793"), //
                        hasAmount("EUR", 31.46), hasGrossValue("EUR", 31.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(0.612), //
                        hasSecurity(hasIsin("IE00BKM4GZ66")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747796"), //
                        hasAmount("EUR", 16.71), hasGrossValue("EUR", 16.71), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(2.7067), //
                        hasSecurity(hasIsin("IE00BDBRDM35")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747798"), //
                        hasAmount("EUR", 12.08), hasGrossValue("EUR", 12.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(0.7906), //
                        hasSecurity(hasIsin("LU1109942653")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747802"), //
                        hasAmount("EUR", 12.02), hasGrossValue("EUR", 12.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(0.0743), //
                        hasSecurity(hasIsin("FR0010754135")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747807"), //
                        hasAmount("EUR", 11.91), hasGrossValue("EUR", 11.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(0.543), //
                        hasSecurity(hasIsin("LU1931975079")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747811"), //
                        hasAmount("EUR", 9.55), hasGrossValue("EUR", 9.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(0.179), //
                        hasSecurity(hasIsin("LU2037748774")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747815"), //
                        hasAmount("EUR", 8.80), hasGrossValue("EUR", 8.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(0.2334), //
                        hasSecurity(hasIsin("IE00BL25JN58")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747821"), //
                        hasAmount("EUR", 8.22), hasGrossValue("EUR", 8.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(0.2069), //
                        hasSecurity(hasIsin("IE00BGJWWY63")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747828"), //
                        hasAmount("EUR", 7.72), hasGrossValue("EUR", 7.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(0.1258), //
                        hasSecurity(hasIsin("IE00BG0SKF03")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747834"), //
                        hasAmount("EUR", 4.66), hasGrossValue("EUR", 4.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-05T00:00"), hasShares(0.0482), //
                        hasSecurity(hasIsin("IE00B3F81G20")), //
                        hasSource("Depotauszug05.txt"), //
                        hasNote("Ref.-Nr.: 403747842"), //
                        hasAmount("EUR", 3.57), hasGrossValue("EUR", 3.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug06()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(15L));
        assertThat(countBuySell(results), is(23L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(39));
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
                        hasName("Xtr.(IE)MSCI World Value Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKM4GZ66"), hasWkn(null), hasTicker(null), //
                        hasName("iShs Core MSCI EM IMI U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JP72"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE)MSCI World Momentum Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BCBJG560"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR MSCI Wrld Small Cap U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JN58"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE)-MSCI World Minim.Vol. Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1650487413"), hasWkn(null), hasTicker(null), //
                        hasName("MUL-LYX.EO Gov.Bd 1-3Y(DR)U.E. Inhaber-Anteile Acc o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BDBRDM35"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-Core Gl.Aggr.Bd UC.ETF Registered Shs EUR Acc.hgd o.N"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1109942653"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.II EUR H.Yield Corp.Bond Inhaber-Anteile 1D o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2037748774"), hasWkn(null), hasTicker(null), //
                        hasName("Am.I.S.-AM.IDX EO COR.SRI 0-3Y Act.Nom.UCITS ETF EUR DR Ac.oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1931975079"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi I.S.-A.PRIME EURO CORP. Nam.-Ant.UC.ETF DR EUR Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BG0SKF03"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIV-Edge MSCI EM Value F. Registered Shares USD (Acc.)oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3F81G20"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-MSCI EM Sm.Cap U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0690964092"), hasWkn(null), hasTicker(null), //
                        hasName("Xtrackers II Global Gov.Bond Inhaber-Anteile 1D EUR Hgd oN"), //
                        hasCurrencyCode("EUR"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-23"), hasAmount("EUR", 1000.00), //
                        hasSource("Depotauszug06.txt"), hasNote("Sammelgutschrift | Ref.-Nr.: 193396101"))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(8.514), //
                        hasSecurity(hasIsin("LU1931974692")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293223"), //
                        hasAmount("EUR", 196.50), hasGrossValue("EUR", 196.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(7.582), //
                        hasSecurity(hasIsin("IE00BFY0GT14")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293228"), //
                        hasAmount("EUR", 173.09), hasGrossValue("EUR", 173.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(4.024), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293232"), //
                        hasAmount("EUR", 118.59), hasGrossValue("EUR", 118.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(2.245), //
                        hasSecurity(hasIsin("IE00BKM4GZ66")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293236"), //
                        hasAmount("EUR", 70.90), hasGrossValue("EUR", 70.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(1.697), //
                        hasSecurity(hasIsin("IE00BL25JP72")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293242"), //
                        hasAmount("EUR", 70.72), hasGrossValue("EUR", 70.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(0.759), //
                        hasSecurity(hasIsin("IE00BCBJG560")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293246"), //
                        hasAmount("EUR", 62.76), hasGrossValue("EUR", 62.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(1.884), //
                        hasSecurity(hasIsin("IE00BL25JN58")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293250"), //
                        hasAmount("EUR", 54.85), hasGrossValue("EUR", 54.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(0.341), //
                        hasSecurity(hasIsin("LU1650487413")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293255"), //
                        hasAmount("EUR", 42.39), hasGrossValue("EUR", 42.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(8.058), //
                        hasSecurity(hasIsin("IE00BDBRDM35")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293259"), //
                        hasAmount("EUR", 42.32), hasGrossValue("EUR", 42.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(1.857), //
                        hasSecurity(hasIsin("LU1109942653")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293265"), //
                        hasAmount("EUR", 32.76), hasGrossValue("EUR", 32.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(0.615), //
                        hasSecurity(hasIsin("LU2037748774")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293270"), //
                        hasAmount("EUR", 30.87), hasGrossValue("EUR", 30.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(1.39), //
                        hasSecurity(hasIsin("LU1931975079")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293274"), //
                        hasAmount("EUR", 28.83), hasGrossValue("EUR", 28.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(0.624), //
                        hasSecurity(hasIsin("IE00BG0SKF03")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293279"), //
                        hasAmount("EUR", 23.81), hasGrossValue("EUR", 23.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(0.23), //
                        hasSecurity(hasIsin("IE00B3F81G20")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293284"), //
                        hasAmount("EUR", 15.80), hasGrossValue("EUR", 15.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-25T00:00"), hasShares(0.07), //
                        hasSecurity(hasIsin("LU0690964092")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194293290"), //
                        hasAmount("EUR", 15.40), hasGrossValue("EUR", 15.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-26T00:00"), hasShares(0.151), //
                        hasSecurity(hasIsin("LU1931974692")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194569182"), //
                        hasAmount("EUR", 3.44), hasGrossValue("EUR", 3.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-26T00:00"), hasShares(0.14), //
                        hasSecurity(hasIsin("IE00BFY0GT14")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194569185"), //
                        hasAmount("EUR", 3.15), hasGrossValue("EUR", 3.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-26T00:00"), hasShares(0.04), //
                        hasSecurity(hasIsin("IE00BL25JP72")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194569191"), //
                        hasAmount("EUR", 1.65), hasGrossValue("EUR", 1.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-26T00:00"), hasShares(0.056), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194569195"), //
                        hasAmount("EUR", 1.62), hasGrossValue("EUR", 1.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-26T00:00"), hasShares(0.017), //
                        hasSecurity(hasIsin("IE00BCBJG560")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194569200"), //
                        hasAmount("EUR", 1.38), hasGrossValue("EUR", 1.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-26T00:00"), hasShares(0.035), //
                        hasSecurity(hasIsin("IE00BKM4GZ66")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194569205"), //
                        hasAmount("EUR", 1.08), hasGrossValue("EUR", 1.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-26T00:00"), hasShares(0.034), //
                        hasSecurity(hasIsin("IE00BL25JN58")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194569209"), //
                        hasAmount("EUR", 0.98), hasGrossValue("EUR", 0.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-26T00:00"), hasShares(0.108), //
                        hasSecurity(hasIsin("IE00BDBRDM35")), //
                        hasSource("Depotauszug06.txt"), //
                        hasNote("Ref.-Nr.: 194569213"), //
                        hasAmount("EUR", 0.57), hasGrossValue("EUR", 0.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug07()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(18L));
        assertThat(countBuySell(results), is(18L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(45));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3M0BZ05"), hasWkn(null), hasTicker(null), //
                        hasName("Dimens.Fds-Glbl Core Equity Fd Registered Shs EUR Dis.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3QL0Y14"), hasWkn(null), hasTicker(null), //
                        hasName("Dim.Fds-Gl Short Fixed Income Registered Shares EUR Dis.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1274520086"), hasWkn(null), hasTicker(null), //
                        hasName("Rob.C.Gr.Fds-R.QI Gl.De.Co.Eq. Actions Nom. I EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B03HD191"), hasWkn(null), hasTicker(null), //
                        hasName("Vang.Inv.S.-Global Stk.Ind.Fd Reg.Shs EUR Acc o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B60SX394"), hasWkn(null), hasTicker(null), //
                        hasName("InvescoMI MSCI World ETF Registered Shares Acc o.N"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF4RFH31"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-MSCI Wld Sm.Ca.UCI.ETF Registered Shares USD(Acc)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1650487413"), hasWkn(null), hasTicker(null), //
                        hasName("MUL Amundi EUR GovBond 1-3Y UCITS ETF Inh.Anteile Acc"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1048314196"), hasWkn(null), hasTicker(null), //
                        hasName("UBS(L)FS-BBG EO A.L.Crp1-5UETF Inhaber-Anteile A Dis.EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B6YX5F63"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR Bl.1-3Y.Eu.Go.Bd U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BC7GZW19"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR Bl.0-3Y.Eu.Co.Bd UETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF3N7219"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIV-Fa.An.Hi.Yi.Co.Bd U.ETF Reg.Shares EUR Hdgd Dist o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B66F4759"), hasWkn(null), hasTicker(null), //
                        hasName("iShs EO H.Yield Corp Bd U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JM42"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE)MSCI World Value Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0690964092"), hasWkn(null), hasTicker(null), //
                        hasName("Xtrackers II Global Gov.Bond Inhaber-Anteile 1D EUR Hgd oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2146179432"), hasWkn(null), hasTicker(null), //
                        hasName("Assenagon Funds Val.Size Glob. Act. Nom. RS EUR Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B42THM37"), hasWkn(null), hasTicker(null), //
                        hasName("Dimensional Fds-Emerg.Mkts Va. Registered Shares EUR Dis.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BD45KH83"), hasWkn(null), hasTicker(null), //
                        hasName("iShs Core MSCI EM IMI U.ETF Registered Shs USD (Dist) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1233758587"), hasWkn(null), hasTicker(null), //
                        hasName("Robeco C.G.Fds-R.QI.Em.Con.Eq. Actions Nominat. IE EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund(hasDate("2024-04-02"), hasAmount("EUR", 56.07), //
                        hasSource("Depotauszug07.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 464710285"))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2024-03-31"), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Bestandsprovision LU1274520086 01.01.2024 - 31.03.2024 | Ref.-Nr.: 467260165"), //
                        hasAmount("EUR", 2.04), hasGrossValue("EUR", 2.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2024-03-31"), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Bestandsprovision LU1233758587 01.01.2024 - 31.03.2024 | Ref.-Nr.: 467260166"), //
                        hasAmount("EUR", 3.17), hasGrossValue("EUR", 3.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-04-30"), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 474917266"), //
                        hasAmount("EUR", 440.14), hasGrossValue("EUR", 440.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund(hasDate("2024-05-02"), hasAmount("EUR", 53.38), //
                        hasSource("Depotauszug07.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 475292925"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund(hasDate("2024-05-21"), hasAmount("EUR", 132.52), //
                        hasSource("Depotauszug07.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 481573598"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-05-31"), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 484298415"), //
                        hasAmount("EUR", 459.73), hasGrossValue("EUR", 459.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund(hasDate("2024-06-03"), hasAmount("EUR", 55.78), //
                        hasSource("Depotauszug07.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 484782044"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-06-30"), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 494534896"), //
                        hasAmount("EUR", 451.89), hasGrossValue("EUR", 451.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-14T00:00"), hasShares(436.2506), //
                        hasSecurity(hasIsin("IE00B3M0BZ05")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524253"), //
                        hasAmount("EUR", 13380.85), hasGrossValue("EUR", 13380.85 + 1209.03 + 66.49), //
                        hasTaxes("EUR", 1209.03 + 66.49), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-14T00:00"), hasShares(132.0151), //
                        hasSecurity(hasIsin("IE00B3QL0Y14")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524302"), //
                        hasAmount("EUR", 1160.74), hasGrossValue("EUR", 1160.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-15T00:00"), hasShares(1.6316), //
                        hasSecurity(hasIsin("LU1274520086")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524262"), //
                        hasAmount("EUR", 278.66), hasGrossValue("EUR", 278.66 + 15.95 + 0.87), //
                        hasTaxes("EUR", 15.95 + 0.87), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-16T00:00"), hasShares(59.4386), //
                        hasSecurity(hasIsin("IE00B03HD191")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524267"), //
                        hasAmount("EUR", 2487.89), hasGrossValue("EUR", 2487.89 + 213.79 + 11.75), //
                        hasTaxes("EUR", 213.79 + 11.75), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-16T00:00"), hasShares(29.7082), //
                        hasSecurity(hasIsin("IE00B60SX394")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524273"), //
                        hasAmount("EUR", 2716.08), hasGrossValue("EUR", 2716.08 + 204.71 + 11.25), //
                        hasTaxes("EUR", 204.71 + 11.25), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-16T00:00"), hasShares(632.6936), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524292"), //
                        hasAmount("EUR", 3971.53), hasGrossValue("EUR", 3971.53 + 267.54 + 14.71), //
                        hasTaxes("EUR", 267.54 + 14.71), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-16T00:00"), hasShares(6.6838), //
                        hasSecurity(hasIsin("LU1650487413")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524309"), //
                        hasAmount("EUR", 812.75), hasGrossValue("EUR", 812.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-16T00:00"), hasShares(65.5288), //
                        hasSecurity(hasIsin("LU1048314196")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524318"), //
                        hasAmount("EUR", 855.03), hasGrossValue("EUR", 855.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-16T00:00"), hasShares(15.8913), //
                        hasSecurity(hasIsin("IE00B6YX5F63")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524326"), //
                        hasAmount("EUR", 811.99), hasGrossValue("EUR", 811.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-16T00:00"), hasShares(27.8929), //
                        hasSecurity(hasIsin("IE00BC7GZW19")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524337"), //
                        hasAmount("EUR", 831.54), hasGrossValue("EUR", 831.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-16T00:00"), hasShares(112.7489), //
                        hasSecurity(hasIsin("IE00BF3N7219")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524346"), //
                        hasAmount("EUR", 533.00), hasGrossValue("EUR", 533.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-16T00:00"), hasShares(8.1735), //
                        hasSecurity(hasIsin("IE00B66F4759")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524355"), //
                        hasAmount("EUR", 753.46), hasGrossValue("EUR", 753.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-16T00:00"), hasShares(413.8243), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524362"), //
                        hasAmount("EUR", 17058.37), hasGrossValue("EUR", 17058.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-16T00:00"), hasShares(9.5408), //
                        hasSecurity(hasIsin("LU0690964092")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524367"), //
                        hasAmount("EUR", 1691.00), hasGrossValue("EUR", 1691.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-17T00:00"), hasShares(18.8472), //
                        hasSecurity(hasIsin("LU2146179432")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 479524283"), //
                        hasAmount("EUR", 1413.93), hasGrossValue("EUR", 1413.93 + 101.15 + 5.56), //
                        hasTaxes("EUR", 101.15 + 5.56), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-17T00:00"), hasShares(1083.3492), //
                        hasSecurity(hasIsin("IE00B42THM37")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 480189188"), //
                        hasAmount("EUR", 12774.34), hasGrossValue("EUR", 12774.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-21T00:00"), hasShares(6096.0503), //
                        hasSecurity(hasIsin("IE00BD45KH83")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 480189180"), //
                        hasAmount("EUR", 27128.21), hasGrossValue("EUR", 27128.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-21T00:00"), hasShares(131.3616), //
                        hasSecurity(hasIsin("LU1233758587")), //
                        hasSource("Depotauszug07.txt"), //
                        hasNote("Ref.-Nr.: 480189194"), //
                        hasAmount("EUR", 13236.33), hasGrossValue("EUR", 13236.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
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

        // check removal transaction
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
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2014-03-03"), hasAmount("EUR", 75.00), //
                        hasSource("Depotauszug09.txt"), hasNote("Überweisungsgutschrift | Ref.-Nr.: EU-0384051229"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2014-03-03"), //
                        hasSource("Depotauszug09.txt"), //
                        hasNote("Honorar Feb/2014 | Ref.-Nr.: KA-0383911958"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund(hasDate("2014-03-03"), hasAmount("EUR", 13.91), //
                        hasSource("Depotauszug09.txt"), //
                        hasNote("Steuerbuchung Abgeltungsteuer | Ref.-Nr.: H-0383929197"))));

        // check removal transaction
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
        assertThat(countSecurities(results), is(9L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(13L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(22));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B03HD191"), hasWkn(null), hasTicker(null), //
                        hasName("Vang.Inv.S.-Global Stk.Ind.Fd Reg.Shs EUR Acc o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1274520086"), hasWkn(null), hasTicker(null), //
                        hasName("Rob.C.Gr.Fds-R.QI Gl.De.Co.Eq. Actions Nom. I EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B60SX394"), hasWkn(null), hasTicker(null), //
                        hasName("InvescoMI MSCI World ETF Registered Shares Acc o.N"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B6YX5F63"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR Bl.1-3Y.Eu.Go.Bd U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1681041114"), hasWkn(null), hasTicker(null), //
                        hasName("AIS-Amundi EUR F.R.Corp.Bd ESG Nam.-Ant.UCITS ETF DR-EUR C oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JM42"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE)MSCI World Value Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B48X4842"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR MSCI Emer.Mkts Sm.Cap ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF4RFH31"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-MSCI Wld Sm.Ca.UCI.ETF Registered Shares USD(Acc)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1650487413"), hasWkn(null), hasTicker(null), //
                        hasName("MUL Amundi EUR GovBond 1-3Y UCITS ETF Inh.Anteile Acc"), //
                        hasCurrencyCode("EUR"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund(hasDate("2025-01-02"), hasAmount("EUR", 60.16), //
                        hasSource("Depotauszug10.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 551694863"))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2024-12-31"), //
                        hasSource("Depotauszug10.txt"), //
                        hasNote("Bestandsprovision LU1274520086 01.10.2024 - 31.12.2024 | Ref.-Nr.: 554023678"), //
                        hasAmount("EUR", 2.31), hasGrossValue("EUR", 2.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2024-12-31"), //
                        hasSource("Depotauszug10.txt"), //
                        hasNote("Bestandsprovision LU1233758587 01.10.2024 - 31.12.2024 | Ref.-Nr.: 554023693"), //
                        hasAmount("EUR", 6.96), hasGrossValue("EUR", 6.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2025-01-31"), //
                        hasSource("Depotauszug10.txt"), //
                        hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 563214268"), //
                        hasAmount("EUR", 507.98), hasGrossValue("EUR", 507.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-02T00:00"), hasShares(1645.069), //
                        hasSecurity(hasIsin("IE00B03HD191")), //
                        hasSource("Depotauszug10.txt"), //
                        hasNote("Thesaurierung | Ref.-Nr.: 557786429"), //
                        hasAmount("EUR", 200.20), hasGrossValue("EUR", 200.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-02T00:00"), hasShares(91.1668), //
                        hasSecurity(hasIsin("LU1274520086")), //
                        hasSource("Depotauszug10.txt"), //
                        hasNote("Thesaurierung | Ref.-Nr.: 557794194"), //
                        hasAmount("EUR", 45.64), hasGrossValue("EUR", 45.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-02T00:00"), hasShares(766.6823), //
                        hasSecurity(hasIsin("IE00B60SX394")), //
                        hasSource("Depotauszug10.txt"), //
                        hasNote("Thesaurierung | Ref.-Nr.: 557849822"), //
                        hasAmount("EUR", 200.20), hasGrossValue("EUR", 200.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-02T00:00"), hasShares(318.52), //
                        hasSecurity(hasIsin("IE00B6YX5F63")), //
                        hasSource("Depotauszug10.txt"), //
                        hasNote("Thesaurierung | Ref.-Nr.: 558428333"), //
                        hasAmount("EUR", 52.40), hasGrossValue("EUR", 52.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-02T00:00"), hasShares(87.7672), //
                        hasSecurity(hasIsin("LU1681041114")), //
                        hasSource("Depotauszug10.txt"), //
                        hasNote("Thesaurierung | Ref.-Nr.: 558778401"), //
                        hasAmount("EUR", 6.41), hasGrossValue("EUR", 6.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-02T00:00"), hasShares(1429.5971), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Depotauszug10.txt"), //
                        hasNote("Thesaurierung | Ref.-Nr.: 559609470"), //
                        hasAmount("EUR", 141.98), hasGrossValue("EUR", 141.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-02T00:00"), hasShares(43.0035), //
                        hasSecurity(hasIsin("IE00B48X4842")), //
                        hasSource("Depotauszug10.txt"), //
                        hasNote("Thesaurierung | Ref.-Nr.: 559227626"), //
                        hasAmount("EUR", 12.31), hasGrossValue("EUR", 12.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-02T00:00"), hasShares(3197.6858), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Depotauszug10.txt"), //
                        hasNote("Thesaurierung | Ref.-Nr.: 559351476"), //
                        hasAmount("EUR", 59.93), hasGrossValue("EUR", 59.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-01-02T00:00"), hasShares(196.7106), //
                        hasSecurity(hasIsin("LU1650487413")), //
                        hasSource("Depotauszug10.txt"), //
                        hasNote("Thesaurierung | Ref.-Nr.: 559732387"), //
                        hasAmount("EUR", 57.77), hasGrossValue("EUR", 57.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug11()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(14L));
        assertThat(countBuySell(results), is(14L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(32));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B42THM37"), hasWkn(null), hasTicker(null), //
                        hasName("Dimensional Fds-Emerg.Mkts Va. Registered Shares EUR Dis.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3M0BZ05"), hasWkn(null), hasTicker(null), //
                        hasName("Dimens.Fds-Glbl Core Equity Fd Registered Shs EUR Dis.o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1233758587"), hasWkn(null), hasTicker(null), //
                        hasName("Robeco C.G.Fds-R.QI.Em.Con.Eq. Actions Nominat. IE EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1650487413"), hasWkn(null), hasTicker(null), //
                        hasName("MUL Amundi EUR GovBond 1-3Y UCITS ETF Inh.Anteile Acc"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B6YX5F63"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR Bl.1-3Y.Eu.Go.Bd U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF4RFH31"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-MSCI Wld Sm.Ca.UCI.ETF Registered Shares USD(Acc)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JM42"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE)MSCI World Value Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B14X4Q57"), hasWkn(null), hasTicker(null), //
                        hasName("iShs EO Govt Bd 1-3yr U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1708330235"), hasWkn(null), hasTicker(null), //
                        hasName("AIS-Amundi Ind.JPM Gl.GBI Gov. Act.Nom.UCITS ETF DR EUR Hd oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B48X4842"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR MSCI Emer.Mkts Sm.Cap ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1274520086"), hasWkn(null), hasTicker(null), //
                        hasName("Rob.C.Gr.Fds-R.QI Gl.De.Co.Eq. Actions Nom. I EUR o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B03HD191"), hasWkn(null), hasTicker(null), //
                        hasName("Vang.Inv.S.-Global Stk.Ind.Fd Reg.Shs EUR Acc o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BD45KH83"), hasWkn(null), hasTicker(null), //
                        hasName("iShs Core MSCI EM IMI U.ETF Registered Shs USD (Dist) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2146179432"), hasWkn(null), hasTicker(null), //
                        hasName("Assenagon Funds Val.Size Glob. Act. Nom. RS EUR Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund(hasDate("2025-06-02"), hasAmount("EUR", 58.85), //
                        hasSource("Depotauszug11.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 610616214"))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund(hasDate("2025-06-13"), hasAmount("EUR", 14.24), //
                        hasSource("Depotauszug11.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 615820948"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes(hasDate("2025-06-16"), hasAmount("EUR", 7.39), //
                        hasSource("Depotauszug11.txt"), hasNote("Steueroptimierung | Ref.-Nr.: 616720729"))));

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2025-06-30"), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 620879559"), //
                        hasAmount("EUR", 469.61), hasGrossValue("EUR", 469.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-11T00:00"), hasShares(1453.3036), //
                        hasSecurity(hasIsin("IE00B42THM37")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687808"), //
                        hasAmount("EUR", 16823.87), hasGrossValue("EUR", 16823.87 + 388.97 + 21.39), //
                        hasTaxes("EUR", 388.97 + 21.39), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-11T00:00"), hasShares(1083.763), //
                        hasSecurity(hasIsin("IE00B3M0BZ05")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687922"), //
                        hasAmount("EUR", 38497.21), hasGrossValue("EUR", 38497.21), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-13T00:00"), hasShares(178.3279), //
                        hasSecurity(hasIsin("LU1233758587")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687823"), //
                        hasAmount("EUR", 18291.39), hasGrossValue("EUR", 18291.39 + 207.47 + 11.41), //
                        hasTaxes("EUR", 207.47 + 11.41), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-13T00:00"), hasShares(6.0675), //
                        hasSecurity(hasIsin("LU1650487413")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687838"), //
                        hasAmount("EUR", 767.24), hasGrossValue("EUR", 767.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-13T00:00"), hasShares(7.1805), //
                        hasSecurity(hasIsin("IE00B6YX5F63")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687853"), //
                        hasAmount("EUR", 378.13), hasGrossValue("EUR", 378.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-13T00:00"), hasShares(1938.3874), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687866"), //
                        hasAmount("EUR", 12416.83), hasGrossValue("EUR", 12416.83 + 830.92 + 45.70), //
                        hasTaxes("EUR", 830.92 + 45.70), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-13T00:00"), hasShares(316.0119), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687879"), //
                        hasAmount("EUR", 13345.74), hasGrossValue("EUR", 13345.74 + 480.27 + 26.41), //
                        hasTaxes("EUR", 480.27 + 26.41), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-13T00:00"), hasShares(2.1008), //
                        hasSecurity(hasIsin("IE00B14X4Q57")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687889"), //
                        hasAmount("EUR", 297.43), hasGrossValue("EUR", 297.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-13T00:00"), hasShares(6.4513), //
                        hasSecurity(hasIsin("LU1708330235")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687901"), //
                        hasAmount("EUR", 299.33), hasGrossValue("EUR", 299.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-13T00:00"), hasShares(43.0035), //
                        hasSecurity(hasIsin("IE00B48X4842")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687912"), //
                        hasAmount("EUR", 4541.91), hasGrossValue("EUR", 4541.91 + 121.98 + 6.70), //
                        hasTaxes("EUR", 121.98 + 6.70), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-13T00:00"), hasShares(28.0779), //
                        hasSecurity(hasIsin("LU1274520086")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687930"), //
                        hasAmount("EUR", 5791.33), hasGrossValue("EUR", 5791.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-13T00:00"), hasShares(7.4825), //
                        hasSecurity(hasIsin("IE00B03HD191")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687935"), //
                        hasAmount("EUR", 375.90), hasGrossValue("EUR", 375.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-13T00:00"), hasShares(4650.3717), //
                        hasSecurity(hasIsin("IE00BD45KH83")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687949"), //
                        hasAmount("EUR", 21242.99), hasGrossValue("EUR", 21242.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-16T00:00"), hasShares(35.995), //
                        hasSecurity(hasIsin("LU2146179432")), //
                        hasSource("Depotauszug11.txt"), //
                        hasNote("Ref.-Nr.: 614687943"), //
                        hasAmount("EUR", 2948.14), hasGrossValue("EUR", 2948.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug12()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(13L));
        assertThat(countBuySell(results), is(37L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(52));
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
                        hasName("Xtr.(IE)MSCI World Value Registered Shares 1C USD o.N."), //
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
                        hasIsin("LU1650487413"), hasWkn(null), hasTicker(null), //
                        hasName("MUL Amundi EUR GovBond 1-3Y UCITS ETF Inh.Anteile Acc"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B48X4842"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR MSCI Emer.Mkts Sm.Cap ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1109942653"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.II EUR H.Yield Corp.Bond Inhaber-Anteile 1D o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF1QPL78"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR Bloom.Gl.Ag.Bd U.ETF Reg.Shs EUR Hdg UCITS ETF oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2037748774"), hasWkn(null), hasTicker(null), //
                        hasName("Am.I.S.-AM.IDX EO COR.SRI 0-3Y Act.Nom.UCITS ETF EUR DR Ac.oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1931975079"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi I.S.-A.PRIME EURO CORP. Nam.-Ant.UC.ETF DR EUR Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BGJWWY63"), hasWkn(null), hasTicker(null), //
                        hasName("InvescoMI2 EUR Gov B 1-3Y ETF Reg. Shs EUR Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-10"), hasAmount("EUR", 2.00), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Überweisungsgutschrift Inland | Ref.-Nr.: 499025059"))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-25"), hasAmount("EUR", 25.00), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Überweisungsgutschrift Inland | Ref.-Nr.: 231123912"))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.1796), //
                        hasSecurity(hasIsin("LU1931974692")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 810613137"), //
                        hasAmount("EUR", 6.01), hasGrossValue("EUR", 6.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.1584), //
                        hasSecurity(hasIsin("IE00BFY0GT14")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 789323993"), //
                        hasAmount("EUR", 5.53), hasGrossValue("EUR", 5.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.116), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 515457226"), //
                        hasAmount("EUR", 4.86), hasGrossValue("EUR", 4.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.6296), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 605189398"), //
                        hasAmount("EUR", 4.36), hasGrossValue("EUR", 4.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.052), //
                        hasSecurity(hasIsin("IE00BKM4GZ66")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500470782"), //
                        hasAmount("EUR", 1.67), hasGrossValue("EUR", 1.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.0257), //
                        hasSecurity(hasIsin("IE00BG0SKF03")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500470790"), //
                        hasAmount("EUR", 1.20), hasGrossValue("EUR", 1.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.0046), //
                        hasSecurity(hasIsin("LU1650487413")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500470797"), //
                        hasAmount("EUR", 0.56), hasGrossValue("EUR", 0.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.0042), //
                        hasSecurity(hasIsin("IE00B48X4842")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500470805"), //
                        hasAmount("EUR", 0.48), hasGrossValue("EUR", 0.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.0278), //
                        hasSecurity(hasIsin("LU1109942653")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 030025354"), //
                        hasAmount("EUR", 0.44), hasGrossValue("EUR", 0.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.0166), //
                        hasSecurity(hasIsin("IE00BF1QPL78")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 086981855"), //
                        hasAmount("EUR", 0.44), hasGrossValue("EUR", 0.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.0081), //
                        hasSecurity(hasIsin("LU2037748774")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 496756247"), //
                        hasAmount("EUR", 0.41), hasGrossValue("EUR", 0.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.02), //
                        hasSecurity(hasIsin("LU1931975079")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500470826"), //
                        hasAmount("EUR", 0.37), hasGrossValue("EUR", 0.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-17T00:00"), hasShares(0.0089), //
                        hasSecurity(hasIsin("IE00BGJWWY63")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500470831"), //
                        hasAmount("EUR", 0.33), hasGrossValue("EUR", 0.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-19T00:00"), hasShares(0.0278), //
                        hasSecurity(hasIsin("LU1109942653")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500904894"), //
                        hasAmount("EUR", 0.44), hasGrossValue("EUR", 0.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-19T00:00"), hasShares(0.0046), //
                        hasSecurity(hasIsin("LU1650487413")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500904906"), //
                        hasAmount("EUR", 0.56), hasGrossValue("EUR", 0.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-19T00:00"), hasShares(0.0166), //
                        hasSecurity(hasIsin("IE00BF1QPL78")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500904915"), //
                        hasAmount("EUR", 0.44), hasGrossValue("EUR", 0.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-19T00:00"), hasShares(0.02), //
                        hasSecurity(hasIsin("LU1931975079")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500904924"), //
                        hasAmount("EUR", 0.37), hasGrossValue("EUR", 0.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-19T00:00"), hasShares(0.0081), //
                        hasSecurity(hasIsin("LU2037748774")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500904933"), //
                        hasAmount("EUR", 0.41), hasGrossValue("EUR", 0.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-19T00:00"), hasShares(0.0089), //
                        hasSecurity(hasIsin("IE00BGJWWY63")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500904941"), //
                        hasAmount("EUR", 0.33), hasGrossValue("EUR", 0.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-19T00:00"), hasShares(0.0005), //
                        hasSecurity(hasIsin("IE00B48X4842")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 831292531"), //
                        hasAmount("EUR", 0.06), hasGrossValue("EUR", 0.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-19T00:00"), hasShares(0.0068), //
                        hasSecurity(hasIsin("IE00BKM4GZ66")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500904958"), //
                        hasAmount("EUR", 0.22), hasGrossValue("EUR", 0.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-19T00:00"), hasShares(0.0114), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500904963"), //
                        hasAmount("EUR", 0.47), hasGrossValue("EUR", 0.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-19T00:00"), hasShares(0.0704), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500904971"), //
                        hasAmount("EUR", 0.48), hasGrossValue("EUR", 0.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-19T00:00"), hasShares(0.0032), //
                        hasSecurity(hasIsin("IE00BG0SKF03")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500904980"), //
                        hasAmount("EUR", 0.15), hasGrossValue("EUR", 0.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-19T00:00"), hasShares(0.0216), //
                        hasSecurity(hasIsin("LU1931974692")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500904987"), //
                        hasAmount("EUR", 0.72), hasGrossValue("EUR", 0.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-19T00:00"), hasShares(0.0191), //
                        hasSecurity(hasIsin("IE00BFY0GT14")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 500904994"), //
                        hasAmount("EUR", 0.66), hasGrossValue("EUR", 0.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-26T00:00"), hasShares(0.1921), //
                        hasSecurity(hasIsin("LU1931974692")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 502262339"), //
                        hasAmount("EUR", 6.29), hasGrossValue("EUR", 6.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-26T00:00"), hasShares(0.169), //
                        hasSecurity(hasIsin("IE00BFY0GT14")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 502262349"), //
                        hasAmount("EUR", 5.77), hasGrossValue("EUR", 5.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-26T00:00"), hasShares(0.1226), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 502262358"), //
                        hasAmount("EUR", 5.06), hasGrossValue("EUR", 5.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-26T00:00"), hasShares(0.6534), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 502262367"), //
                        hasAmount("EUR", 4.50), hasGrossValue("EUR", 4.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-26T00:00"), hasShares(0.056), //
                        hasSecurity(hasIsin("IE00BKM4GZ66")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 502262374"), //
                        hasAmount("EUR", 1.76), hasGrossValue("EUR", 1.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-26T00:00"), hasShares(0.0277), //
                        hasSecurity(hasIsin("IE00BG0SKF03")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 502262381"), //
                        hasAmount("EUR", 1.26), hasGrossValue("EUR", 1.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-26T00:00"), hasShares(0.0045), //
                        hasSecurity(hasIsin("IE00B48X4842")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 502262389"), //
                        hasAmount("EUR", 0.50), hasGrossValue("EUR", 0.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-29T00:00"), hasShares(0.0015), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 502844109"), //
                        hasAmount("EUR", 0.06), hasGrossValue("EUR", 0.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-29T00:00"), hasShares(0.0111), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 502844116"), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-29T00:00"), hasShares(0.0015), //
                        hasSecurity(hasIsin("LU1931974692")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 502844123"), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-29T00:00"), hasShares(0.0011), //
                        hasSecurity(hasIsin("IE00BFY0GT14")), //
                        hasSource("Depotauszug12.txt"), //
                        hasNote("Ref.-Nr.: 502844130"), //
                        hasAmount("EUR", 0.04), hasGrossValue("EUR", 0.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepotauszug13()
    {
        var extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(14L));
        assertThat(countBuySell(results), is(14L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(29));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1650487413"), hasWkn(null), hasTicker(null), //
                        hasName("MUL Amundi EUR GovBond 1-3Y UCITS ETF Inh.Anteile Acc"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BZ163H91"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard EUR Euroz.Gov.B.U.ETF Registered Shares EUR Dis.oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B6YX5F63"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR Bl.1-3Y.Eu.Go.Bd U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1708330235"), hasWkn(null), hasTicker(null), //
                        hasName("AIS-Amundi Ind.JPM Gl.GBI Gov. Act.Nom.UCITS ETF DR EUR Hd oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BGJWWY63"), hasWkn(null), hasTicker(null), //
                        hasName("InvescoMI2 EUR Gov B 1-3Y ETF Reg. Shs EUR Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF5GB717"), hasWkn(null), hasTicker(null), //
                        hasName("iShs II-iSh E.Flt.Ra.Bd ESG UE Reg. Shares EUR Dis. o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BCRY6557"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIV-EO Ultrashort Bd U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BL25JM42"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.(IE)MSCI World Value Registered Shares 1C USD o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000QIF5N15"), hasWkn(null), hasTicker(null), //
                        hasName("Amu.ETF ICAV-PRIME Glbl ETF Bear.Shs USD Dis. oN"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF4RFH31"), hasWkn(null), hasTicker(null), //
                        hasName("iShsIII-MSCI Wld Sm.Ca.UCI.ETF Registered Shares USD(Acc)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFY0GT14"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR MSCI World UCITS ETF Reg.Shares USD Unhgd Acc. o.N."), //
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

        // check fees transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2025-04-30"), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Vermögensverwaltungshonorar | Ref.-Nr.: 599612351"), //
                        hasAmount("EUR", 4.34), hasGrossValue("EUR", 4.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-15T00:00"), hasShares(0.4979), //
                        hasSecurity(hasIsin("LU1650487413")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326918"), //
                        hasAmount("EUR", 62.37), hasGrossValue("EUR", 62.37 + 0.51 + 0.02), //
                        hasTaxes("EUR", 0.51 + 0.02), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-15T00:00"), hasShares(2.5449), //
                        hasSecurity(hasIsin("IE00BZ163H91")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326928"), //
                        hasAmount("EUR", 57.12), hasGrossValue("EUR", 57.12 + 0.20 + 0.01), //
                        hasTaxes("EUR", 0.20 + 0.01), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-15T00:00"), hasShares(0.8201), //
                        hasSecurity(hasIsin("IE00B6YX5F63")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326932"), //
                        hasAmount("EUR", 43.19), hasGrossValue("EUR", 43.19 + 0.07), //
                        hasTaxes("EUR", 0.07), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-15T00:00"), hasShares(0.8967), //
                        hasSecurity(hasIsin("LU1708330235")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326938"), //
                        hasAmount("EUR", 41.64), hasGrossValue("EUR", 41.64 + 0.04), //
                        hasTaxes("EUR", 0.04), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-15T00:00"), hasShares(0.9745), //
                        hasSecurity(hasIsin("IE00BGJWWY63")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326944"), //
                        hasAmount("EUR", 36.99), hasGrossValue("EUR", 36.99 + 0.07), //
                        hasTaxes("EUR", 0.07), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-15T00:00"), hasShares(4.1955), //
                        hasSecurity(hasIsin("IE00BF5GB717")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326952"), //
                        hasAmount("EUR", 21.29), hasGrossValue("EUR", 21.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-04-15T00:00"), hasShares(0.2079), //
                        hasSecurity(hasIsin("IE00BCRY6557")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326959"), //
                        hasAmount("EUR", 21.18), hasGrossValue("EUR", 21.18 + 0.01), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-15T00:00"), hasShares(1.8838), //
                        hasSecurity(hasIsin("IE00BL25JM42")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326968"), //
                        hasAmount("EUR", 75.82), hasGrossValue("EUR", 75.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-15T00:00"), hasShares(2.089), //
                        hasSecurity(hasIsin("IE000QIF5N15")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326975"), //
                        hasAmount("EUR", 65.49), hasGrossValue("EUR", 65.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-15T00:00"), hasShares(10.362), //
                        hasSecurity(hasIsin("IE00BF4RFH31")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326980"), //
                        hasAmount("EUR", 64.44), hasGrossValue("EUR", 64.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-15T00:00"), hasShares(1.8241), //
                        hasSecurity(hasIsin("IE00BFY0GT14")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326985"), //
                        hasAmount("EUR", 60.69), hasGrossValue("EUR", 60.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-15T00:00"), hasShares(0.6691), //
                        hasSecurity(hasIsin("IE00BKM4GZ66")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326990"), //
                        hasAmount("EUR", 20.08), hasGrossValue("EUR", 20.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-15T00:00"), hasShares(0.3901), //
                        hasSecurity(hasIsin("IE00BG0SKF03")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595326996"), //
                        hasAmount("EUR", 17.20), hasGrossValue("EUR", 17.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-15T00:00"), hasShares(0.101), //
                        hasSecurity(hasIsin("IE00B48X4842")), //
                        hasSource("Depotauszug13.txt"), //
                        hasNote("Ref.-Nr.: 595327001"), //
                        hasAmount("EUR", 9.74), hasGrossValue("EUR", 9.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
