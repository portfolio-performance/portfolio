package name.abuchen.portfolio.datatransfer.pdf.hellobank;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
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
import name.abuchen.portfolio.datatransfer.pdf.HelloBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class HelloBankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new HelloBankPDFExtractor(new Client());

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
                        hasIsin("NO0003054108"), hasWkn(null), hasTicker(null), //
                        hasName("M a r i n e  H a r v est ASA Navne-Aksjer NK 7,50"), //
                        hasCurrencyCode("NOK"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-06-30T09:00:10"), hasShares(74.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1118.80), hasGrossValue("EUR", 1092.14), //
                        hasForexGrossValue("NOK", 10360.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 25.20 + 1.46))));
    }

    @Test
    public void testWertpapierKauf01WithSecurityinEUR()
    {
        var security = new Security("M a r i n e  H a r v est ASA Navne-Aksjer NK 7,50", "EUR");
        security.setIsin("NO0003054108");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HelloBankPDFExtractor(client);

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
                        hasDate("2017-06-30T09:00:10"), hasShares(74.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1118.80), hasGrossValue("EUR", 1092.14), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 25.20 + 1.46))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new HelloBankPDFExtractor(new Client());

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
                        hasIsin("GB00B03MLX29"), hasWkn(null), hasTicker(null), //
                        hasName("R o y a l  D u t c h Shell Reg. Shares Class A EO -,07"), //
                        hasCurrencyCode("GBP"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2017-04-27T16:36:18"), hasShares(55.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1314.03), hasGrossValue("EUR", 1305.95), //
                        hasForexGrossValue("GBP", 1100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.53 + 1.55))));
    }

    @Test
    public void testWertpapierKauf02WithSecurityinEUR()
    {
        var security = new Security("R o y a l  D u t c h Shell Reg. Shares Class A EO -,07", "EUR");
        security.setIsin("GB00B03MLX29");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HelloBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

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
                        hasDate("2017-04-27T16:36:18"), hasShares(55.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1314.03), hasGrossValue("EUR", 1305.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.53 + 1.55))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new HelloBankPDFExtractor(new Client());

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
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s I I I - C o r e MSCI World U.ETF Registered Shs USD (Acc) o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-11T09:04:00"), hasShares(9.65), //
                        hasSource("Kauf03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 641.01), hasGrossValue("EUR", 641.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new HelloBankPDFExtractor(new Client());

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
                        hasIsin("IE00BKM4GZ66"), hasWkn(null), hasTicker(null), //
                        hasName("i S h s  C o r e  M SCI EM IMI U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-05-11T00:00:00"), hasShares(7.97), //
                        hasSource("Kauf04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 243.28), hasGrossValue("EUR", 243.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new HelloBankPDFExtractor(new Client());

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
                        hasIsin("AU000000SHV6"), hasWkn(null), hasTicker(null), //
                        hasName("S E L E C T  H A R V EST LTD. Registered Shares o.N."), //
                        hasCurrencyCode("AUD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-10-12T07:30:01"), hasShares(1000.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3096.85), hasGrossValue("EUR", 5000.00 / 1.5181), //
                        hasForexGrossValue("AUD", 5000.00), //
                        hasTaxes("EUR", 254.10 / 1.5181), hasFees("EUR", 14.99 + 6.42 + 7.95))));
    }

    @Test
    public void testWertpapierVerkauf01WithSecurityinEUR()
    {
        var security = new Security("S E L E C T  H A R V EST LTD. Registered Shares o.N.", "EUR");
        security.setIsin("AU000000SHV6");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HelloBankPDFExtractor(client);

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
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-10-12T07:30:01"), hasShares(1000.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3096.85), hasGrossValue("EUR", 5000.00 / 1.5181), //
                        hasTaxes("EUR", 254.10 / 1.5181), hasFees("EUR", 14.99 + 6.42 + 7.95))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new HelloBankPDFExtractor(new Client());

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
                        hasIsin("DE0007472060"), hasWkn(null), hasTicker(null), //
                        hasName("W i r e c a r d  A G Inhaber-Aktien o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-01-15T14:13:23"), hasShares(19.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2219.90), hasGrossValue("EUR", 2310.40), //
                        hasTaxes("EUR", 73.93), hasFees("EUR", 4.58 + 4.04 + 7.95))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new HelloBankPDFExtractor(new Client());

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
                        hasIsin("NO0003054108"), hasWkn(null), hasTicker(null), //
                        hasName("M a r i n e  H a r v est ASA Navne-Aksjer NK 7,50"), //
                        hasCurrencyCode("NOK"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-09-06T00:00"), hasExDate("2017-09-01T00:00"), //
                        hasShares(200.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 48.71), hasGrossValue("EUR", 640.00 / 9.308), //
                        hasForexGrossValue("NOK", 640.00), //
                        hasTaxes("EUR", (176.01 / 9.308) + 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende01WithSecurityinEUR()
    {
        var security = new Security("Marine Harvest ASA Navne-Aksjer NK 7,50", "EUR");
        security.setIsin("NO0003054108");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HelloBankPDFExtractor(client);

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
                        hasDate("2017-09-06T00:00"), hasExDate("2017-09-01T00:00"), //
                        hasShares(200.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 48.71), hasGrossValue("EUR", 640.00 / 9.308), //
                        hasTaxes("EUR", (176.01 / 9.308) + 0.19), hasFees("EUR", 0.95), //
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
        var extractor = new HelloBankPDFExtractor(new Client());

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
                        hasIsin("US56035L1044"), hasWkn(null), hasTicker(null), //
                        hasName("M a i n  S t r e e t Capital Corp. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-05-15T00:00"), hasExDate("2017-04-18T00:00"), //
                        hasShares(110.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.34), hasGrossValue("EUR", 20.35 / 1.0942), //
                        hasForexGrossValue("USD", 20.35), //
                        hasTaxes("EUR", ((2.55 + 3.05) / 1.0942) + 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende02WithSecurityinEUR()
    {
        var security = new Security("M a i n  S t r e e t Capital Corp. Registered Shares DL -,01", "EUR");
        security.setIsin("US56035L1044");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HelloBankPDFExtractor(client);

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
                        hasDate("2017-05-15T00:00"), hasExDate("2017-04-18T00:00"), //
                        hasShares(110.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.34), hasGrossValue("EUR", 20.35 / 1.0942), //
                        hasTaxes("EUR", ((2.55 + 3.05) / 1.0942) + 0.19), hasFees("EUR", 0.95), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new HelloBankPDFExtractor(new Client());

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
                        hasIsin("NL0012325773"), hasWkn(null), hasTicker(null), //
                        hasName("R o y a l  D u t c h Shell PLC Anrechte A (Wahldividende)"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-06-26T00:00"), hasExDate("2017-06-26T00:00"), //
                        hasShares(140.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 41.43), hasGrossValue("EUR", 58.72), //
                        hasTaxes("EUR", 8.81 + 7.34 + 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new HelloBankPDFExtractor(new Client());

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
                        hasIsin("US3682872078"), hasWkn(null), hasTicker(null), //
                        hasName("G a z p r o m  P J S C Nam.Akt.(Sp.ADRs)/2 RL 5"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-08-21T00:00"), hasExDate("2017-07-19T00:00"), //
                        hasShares(800.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 116.91), hasGrossValue("EUR", 214.28 / 1.1805), //
                        hasForexGrossValue("USD", 214.28), //
                        hasTaxes("EUR", (32.14 + 26.79) / 1.1805 + 0.19), hasFees("EUR", 16.00 / 1.1805 + 0.95))));
    }

    @Test
    public void testDividende04WithSecurityinEUR()
    {
        var security = new Security("G a z p r o m  P J S C Nam.Akt.(Sp.ADRs)/2 RL 5", "EUR");
        security.setIsin("US3682872078");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HelloBankPDFExtractor(client);

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
                        hasDate("2017-08-21T00:00"), hasExDate("2017-07-19T00:00"), //
                        hasShares(800.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 116.91), hasGrossValue("EUR", 214.28 / 1.1805), //
                        hasTaxes("EUR", (32.14 + 26.79) / 1.1805 + 0.19), hasFees("EUR", 16.00 / 1.1805 + 0.95), //
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
        var extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05_Synthetik.txt"), errors);

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
                        hasIsin("IE00B6YX5D40"), hasWkn(null), hasTicker(null), //
                        hasName("S P D R  S & P  U S Divid.Aristocr.ETF Registered Shares (Dist) o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2017-05-15T00:00"), hasExDate("2017-04-18T00:00"), //
                        hasShares(213.00), //
                        hasSource("Dividende05_Synthetik.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 42.77), hasGrossValue("EUR", 68.16 / 1.1254), //
                        hasForexGrossValue("USD", 68.16), //
                        hasTaxes("EUR", 18.75 / 1.1254 + 0.19), hasFees("EUR", 0.95))));
    }

    @Test
    public void testDividende05WithSecurityinEUR()
    {
        var security = new Security("S P D R  S & P  U S Divid.Aristocr.ETF Registered Shares (Dist) o.N.", "EUR");
        security.setIsin("IE00B6YX5D40");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HelloBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05_Synthetik.txt"), errors);

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
                        hasDate("2017-05-15T00:00"), hasExDate("2017-04-18T00:00"), //
                        hasShares(213.00), //
                        hasSource("Dividende05_Synthetik.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 42.77), hasGrossValue("EUR", 68.16 / 1.1254), //
                        hasTaxes("EUR", 18.75 / 1.1254 + 0.19), hasFees("EUR", 0.95), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testInboundDelivery01()
    {
        var extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "InboundDelivery01.txt"), errors);

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
                        hasIsin("DK0060534915"), hasWkn(null), hasTicker(null), //
                        hasName("N o v o - N o r d i s k AS Navne-Aktier B DK -,20"), //
                        hasCurrencyCode("EUR"))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2017-03-29T00:00"), hasShares(80.00), //
                        hasSource("InboundDelivery01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3225.37), hasGrossValue("EUR", 3225.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testInboundDelivery02()
    {
        var extractor = new HelloBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "InboundDelivery02.txt"), errors);

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
                        hasIsin("US56035L1044"), hasWkn(null), hasTicker(null), //
                        hasName("M a i n  S t r e e t Capital Corp. Registered Shares DL -,01"), //
                        hasCurrencyCode("EUR"))));

        // check delivery inbound (Einlieferung) transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2017-03-31T00:00"), hasShares(110.00), //
                        hasSource("InboundDelivery02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3021.70), hasGrossValue("EUR", 3021.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
