package name.abuchen.portfolio.datatransfer.pdf.oldenburgischelandesbankag;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
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
import name.abuchen.portfolio.datatransfer.pdf.OldenburgischeLandesbankAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class OldenburgischeLandesbankAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0H0785"), hasWkn("A0H078"), hasTicker(null), //
                        hasName("iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-05-17T00:00"), hasShares(0.033037), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Ord.-Ref.: 100000 | Handels.-Ref.: 200000"), //
                        hasAmount("EUR", 1.48), hasGrossValue("EUR", 1.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.01))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("LU1861134382"), hasWkn("A2JSDA"), hasTicker(null), //
                        hasName("AIS-AM.WORLD SRI PAB Act.Nom. UCITS ETF DR (C)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-10-17T18:11:56"), hasShares(3.320171), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Ord.-Ref.: 0980298 | Handels.-Ref.: 029816"), //
                        hasAmount("EUR", 643.40), hasGrossValue("EUR", 638.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.87))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("LU1861134382"), hasWkn("A2JSDA"), hasTicker(null), //
                        hasName("AIS-AM.WORLD SRI PAB Act.Nom. UCITS ETF DR (C)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-11-02T05:21:03"), hasShares(9.727757), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Ord.-Ref.: 8774105 | Handels.-Ref.: 1143326"), //
                        hasAmount("EUR", 911.48), hasGrossValue("EUR", 904.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 7.41))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B4L5Y983"), hasWkn("A0RPWH"), hasTicker(null), //
                        hasName("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-01T00:00"), hasShares(0.263673), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Ord.-Ref.: 12345678 | Handels.-Ref.: 12345678"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.95), //
                        hasForexGrossValue("USD", 26.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInEUR()
    {
        var security = new Security("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N.", "EUR");
        security.setIsin("IE00B4L5Y983");
        security.setWkn("A0RPWH");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OldenburgischeLandesbankAGPDFExtractor(client);

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

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-01T00:00"), hasShares(0.263673), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Ord.-Ref.: 12345678 | Handels.-Ref.: 12345678"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B4L5Y983"), hasWkn("A0RPWH"), hasTicker(null), //
                        hasName("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N."), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-06T18:12:12"), hasShares(0.261293), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Ord.-Ref.: 5867856 | Handels.-Ref.: 5749054"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.95), //
                        hasForexGrossValue("USD", 28.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05))));
    }

    @Test
    public void testWertpapierKauf05WithSecurityInEUR()
    {
        var security = new Security("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc) o.N.", "EUR");
        security.setIsin("IE00B4L5Y983");
        security.setWkn("A0RPWH");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OldenburgischeLandesbankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

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
                        hasDate("2025-05-06T18:12:12"), hasShares(0.261293), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Ord.-Ref.: 5867856 | Handels.-Ref.: 5749054"), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.05), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B0M62Q58"), hasWkn("A0HGV0"), hasTicker(null), //
                        hasName("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-12-19T18:25:53"), hasShares(5.214577), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ord.-Ref.: 1431554 | Handels.-Ref.: 1367606"), //
                        hasAmount("EUR", 317.03), hasGrossValue("EUR", 317.67), //
                        hasForexGrossValue("USD", 346.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.64))));
    }

    @Test
    public void testWertpapierVerkauf01WithSecurityInEUR()
    {
        var security = new Security("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN", "EUR");
        security.setIsin("IE00B0M62Q58");
        security.setWkn("A0HGV0");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OldenburgischeLandesbankAGPDFExtractor(client);

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
                        hasDate("2023-12-19T18:25:53"), hasShares(5.214577), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ord.-Ref.: 1431554 | Handels.-Ref.: 1367606"), //
                        hasAmount("EUR", 317.03), hasGrossValue("EUR", 317.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.64), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var s = c.process((PortfolioTransaction) tx, new Portfolio());
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0H0785"), hasWkn("A0H078"), hasTicker(null), //
                        hasName("iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-02-29T00:00"), hasShares(0.082053), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Ord.-Ref.: 2194170 | Handels.-Ref.: 2105670"), //
                        hasAmount("EUR", 8.81), hasGrossValue("EUR", 8.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("IE000Y77LGG9"), hasWkn("ETF143"), hasTicker(null), //
                        hasName("Am.ETF-MSCI W.SRI CL.N.Z.AM.P. Bear.Shs EUR Acc. oN"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-11-15T00:00"), hasShares(90.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Ord.-Ref.: 4359234 | Handels.-Ref.: 4237898"), //
                        hasAmount("EUR", 8984.72), hasGrossValue("EUR", 9268.20), //
                        hasTaxes("EUR", 264.94), hasFees("EUR", 18.54))));
    }

    @Test
    public void testWertpapierStornoKauf01()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "StornoKauf01.txt"), errors);

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
                        hasIsin("NL0010408704"), hasWkn("A12HWR"), hasTicker(null), //
                        hasName("VanEck Sust.World EQ.UC.ETF Aandelen oop naam o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        purchase( //
                                        hasDate("2023-09-15T18:18:08"), hasShares(0.563027), //
                                        hasSource("StornoKauf01.txt"), //
                                        hasNote("Ord.-Ref.: 908703 | Handels.-Ref.: 847266"), //
                                        hasAmount("EUR", 15.88), hasGrossValue("EUR", 15.88), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0H0785"), hasWkn("A0H078"), hasTicker(null), //
                        hasName("iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-05-15T00:00"), hasShares(71.851808), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 7.44), hasGrossValue("EUR", 7.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B0M62Q58"), hasWkn("A0HGV0"), hasTicker(null), //
                        hasName("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-27T00:00"), hasShares(5.200029), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.84), hasGrossValue("EUR", 0.84), //
                        hasForexGrossValue("USD", 0.88), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        var security = new Security("iShs-MSCI World UCITS ETF Registered Shares USD (Dist)oN", "EUR");
        security.setIsin("IE00B0M62Q58");
        security.setWkn("A0HGV0");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new OldenburgischeLandesbankAGPDFExtractor(client);

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
                        hasDate("2023-09-27T00:00"), hasShares(5.200029), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.84), hasGrossValue("EUR", 0.84), //
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
    public void testDividende03()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0F5UH1"), hasWkn("A0F5UH"), hasTicker(null), //
                        hasName("iSh.ST.Gl.Sel.Div.100 U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-10-16T00:00"), hasShares(89.540503), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 35.37), hasGrossValue("EUR", 35.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0H0785"), hasWkn("A0H078"), hasTicker(null), //
                        hasName("iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-02-15T00:00"), hasShares(26.634225), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.15), hasGrossValue("EUR", 5.62), //
                        hasTaxes("EUR", 1.40 + 0.07), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierStornoDividende01()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "StornoDividende01.txt"), errors);

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
                        hasIsin("NL0010408704"), hasWkn("A12HWR"), hasTicker(null), //
                        hasName("VanEck Sust.World EQ.UC.ETF Aandelen oop naam o.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2023-09-13T00:00"), hasShares(95.967357), //
                                        hasSource("StornoDividende01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 15.88), hasGrossValue("EUR", 16.31), //
                                        hasTaxes("EUR", 0.41 + 0.02), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("DE000A0H0785"), hasWkn("A0H078"), hasTicker(null), //
                        hasName("iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-03-05T00:00"), hasShares(26.634225), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.81), hasGrossValue("EUR", 8.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B4L5Y983"), hasWkn("A0RPWH"), hasTicker(null), //
                        hasName("iShsIII-Core MSCI World U.ETF Registered Shs USD (Acc)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-03-05T00:00"), hasShares(210.496570), //
                        hasSource("Vorabpauschale02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 48.06), hasGrossValue("EUR", 48.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale03()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale03.txt"), errors);

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
                        hasIsin("LU1861134382"), hasWkn("A2JSDA"), hasTicker(null), //
                        hasName("AIS-AM.WORLD SRI PAB Act.Nom. UCITS ETF DR (C)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2024-01-02T00:00"), hasShares(255.212216), //
                                        hasSource("Vorabpauschale03.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2023-08-03"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-08-17"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2024-09-02"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-04"), hasAmount("EUR", 25.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-18"), hasAmount("EUR", 25.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-30"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2024-10-01"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-10-04"), hasAmount("EUR", 25.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-10-17"), hasAmount("EUR", 25.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));
    }

    @Test
    public void testFusion01()
    {
        var extractor = new OldenburgischeLandesbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fusion01.txt"), errors);

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
                        hasIsin("LU1861134382"), hasWkn(null), hasTicker(null), //
                        hasName("AIS-AM.WORLD SRI PAB Act.Nom. UCITS ETF DR (C)o.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        inboundDelivery( //
                                        hasDate("2024-02-02T00:00"), hasShares(255.212216), //
                                        hasSource("Fusion01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }
}
