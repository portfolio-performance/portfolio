package name.abuchen.portfolio.datatransfer.pdf.scalablecapital;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.ScalableCapitalPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class ScalableCapitalPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("IE0008T6IUX0"), hasWkn(null), hasTicker(null), //
                        hasName("Vngrd Fds-ESG Dv.As-Pc Al ETF"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-12T13:12:51"), hasShares(3.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Ord.-Nr.: SCALsin78vS5CYz"), //
                        hasAmount("EUR", 19.49), hasGrossValue("EUR", 18.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("CA25039N4084"), hasWkn(null), hasTicker(null), //
                        hasName("Desert Gold Ventures"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-05T09:08:10"), hasShares(23.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Ord.-Nr.: SCALXmdTbQ7nMxD"), //
                        hasAmount("EUR", 2.11), hasGrossValue("EUR", 1.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("US4228062083"), hasWkn(null), hasTicker(null), //
                        hasName("Heico Corp"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-16T13:39:04"), hasShares(4.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Ord.-Nr.: SCALTjDbBMvHgJv"), //
                        hasAmount("EUR", 864.00), hasGrossValue("EUR", 864.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("IE00B4L60045"), hasWkn(null), hasTicker(null), //
                        hasName("iShares EUR Corp Bond 1-5yr (Dist)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-09T13:00:49"), hasShares(0.242), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Ord.-Nr.: SCW2pFcgwaYpF84pLa3f"), //
                        hasAmount("EUR", 26.19), hasGrossValue("EUR", 26.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("NL0011683594"), hasWkn(null), hasTicker(null), //
                        hasName("VanEck Morningstar Developed Markets Dividend Leaders (Dist)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-12-30T10:16:29"), hasShares(163.00), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Ord.-Nr.: SCALJ7ia7RAxG2B"), //
                        hasAmount("EUR", 7807.88), hasGrossValue("EUR", 7806.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));
    }

    @Test
    public void testSecurityBuy01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

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
                        hasIsin("IE00B3YLTY66"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR MSCI All Country World Investable Market (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-12T09:16:57"), hasShares(21.00), //
                        hasSource("Buy01.txt"), //
                        hasNote("Order ID: SCALkWztnWYGVpk"), //
                        hasAmount("EUR", 4846.74), hasGrossValue("EUR", 4845.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));
    }

    @Test
    public void testSecurityBuy02()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy02.txt"), errors);

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
                        hasIsin("US7731211089"), hasWkn(null), hasTicker(null), //
                        hasName("Rocket Lab"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-18T21:33:30"), hasShares(50.00), //
                        hasSource("Buy02.txt"), //
                        hasNote("Order ID: tqnWxZxTmNwP8xJ"), //
                        hasAmount("EUR", 2000.00), hasGrossValue("EUR", 2000.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testEffectKopen01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kopen01.txt"), errors);

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
                        hasIsin("XS2875106242"), hasWkn(null), hasTicker(null), //
                        hasName("IncomeShares S&P 500 Options"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-07T11:28:30"), hasShares(0.935016), //
                        hasSource("Kopen01.txt"), //
                        hasNote("Order-ID: SCALcbFSrNzHRn3"), //
                        hasAmount("EUR", 5.00), hasGrossValue("EUR", 5.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testTitoliAcquisto01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Acquisto01.txt"), errors);

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
                        hasIsin("IE00B3YLTY66"), hasWkn(null), hasTicker(null), //
                        hasName("SPDR MSCI All Country World Investable Market (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-25T11:07:44"), hasShares(1.071122), //
                        hasSource("Acquisto01.txt"), //
                        hasNote("ID ordine: tRFkKlxFUnG7LmN"), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("LU2903252349"), hasWkn(null), hasTicker(null), //
                        hasName("Scalable MSCI AC World Xtrackers (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-12-30T13:39:12"), hasShares(1.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Ord.-Nr.: SCALRnomPwYQrc5"), //
                        hasAmount("EUR", 8.60), hasGrossValue("EUR", 9.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("DE000HD5ZFL9"), hasWkn(null), hasTicker(null), //
                        hasName("Roche Hldg Long 227,26 CHF Turbo Open End HVB"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-05-12T09:01:04"), hasShares(200.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Ord.-Nr.: DbcK99JE4enn0td"), //
                        hasAmount("EUR", 472.00), hasGrossValue("EUR", 472.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("GB0007188757"), hasWkn(null), hasTicker(null), //
                        hasName("Rio Tinto PLC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-05-12T09:48:14"), hasShares(5.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Ord.-Nr.: SCALzcysZPDAU8W"), //
                        hasAmount("EUR", 278.65), hasGrossValue("EUR", 278.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("IT0004176001"), hasWkn(null), hasTicker(null), //
                        hasName("Prysmian Group"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-06-13T11:16:30"), hasShares(21.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Ord.-Nr.: SCAL1SRff6HgHQD"), //
                        hasAmount("EUR", 1125.91), hasGrossValue("EUR", 1176.42), //
                        hasTaxes("EUR", 47.87 + 2.64), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSecuritySell01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell01.txt"), errors);

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
                        hasIsin("NL0010273215"), hasWkn(null), hasTicker(null), //
                        hasName("ASML Holding"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-09-18T13:15:54"), hasShares(19.00), //
                        hasSource("Sell01.txt"), //
                        hasNote("Order ID: vsDaTlnzwGCiNLQ"), //
                        hasAmount("EUR", 14951.10), hasGrossValue("EUR", 14951.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplanausfuehrung01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplanausfuehrung01.txt"), errors);

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
                        hasIsin("US0304201033"), hasWkn(null), hasTicker(null), //
                        hasName("American Water Works"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-27T11:46:01"), hasShares(0.016515), //
                        hasSource("Sparplanausfuehrung01.txt"), //
                        hasNote("Ord.-Nr.: SCALx2qhYduKQJf"), //
                        hasAmount("EUR", 2.00), hasGrossValue("EUR", 2.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplanausfuehrung02()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplanausfuehrung02.txt"), errors);

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
                        hasIsin("DE000EWG2LD7"), hasWkn(null), hasTicker(null), //
                        hasName("Boerse Stuttgart EUWAX Gold II"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-16T13:33:40"), hasShares(2.166255), //
                        hasSource("Sparplanausfuehrung02.txt"), //
                        hasNote("Ord.-Nr.: 000000000000000"), //
                        hasAmount("EUR", 200.00), hasGrossValue("EUR", 200.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplanausfuehrung03()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplanausfuehrung03.txt"), errors);

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
                        hasIsin("IE00BKX55R35"), hasWkn(null), hasTicker(null), //
                        hasName("Vanguard FTSE North America (Dist)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-16T13:49:30"), hasShares(3.85862), //
                        hasSource("Sparplanausfuehrung03.txt"), //
                        hasNote("Ord.-Nr.: 000000000000000"), //
                        hasAmount("EUR", 500.00), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplanausfuehrung04()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplanausfuehrung04.txt"), errors);

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
                        hasIsin("FR0000052292"), hasWkn(null), hasTicker(null), //
                        hasName("Hermes Intl"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-07-10T16:39:01"), hasShares(0.24321), //
                        hasSource("Sparplanausfuehrung04.txt"), //
                        hasNote("Ord.-Nr.: SCALz2xgFkvDZbm"), //
                        hasAmount("EUR", 602.40), hasGrossValue("EUR", 600.00), //
                        hasTaxes("EUR", 2.40), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplanausfuehrung05()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplanausfuehrung05.txt"), errors);

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
                        hasName("iShares Core MSCI Emerging Markets IMI (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-16T11:24:09"), hasShares(13.697504), //
                        hasSource("Sparplanausfuehrung05.txt"), //
                        hasNote("Ord.-Nr.: SCALPVvYDWL5iH2"), //
                        hasAmount("EUR", 500.00), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSparplanausfuehrung06()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplanausfuehrung06.txt"), errors);

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
                        hasIsin("GB0002875804"), hasWkn(null), hasTicker(null), //
                        hasName("British American Tobacco"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-19T13:31:02"), hasShares(0.055472), //
                        hasSource("Sparplanausfuehrung06.txt"), //
                        hasNote("Ord.-Nr.: SCALNUFeqbHQBbc"), //
                        hasAmount("EUR", 2.61), hasGrossValue("EUR", 2.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSavingsplan01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Savingsplan01.txt"), errors);

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
                        hasIsin("LU0908500753"), hasWkn(null), hasTicker(null), //
                        hasName("Amundi Stoxx Europe 600 (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-05T12:25:16"), hasShares(22.650225), //
                        hasSource("Savingsplan01.txt"), //
                        hasNote("Order ID: BZzUZDlXBK5mNIc"), //
                        hasAmount("EUR", 2425.00), hasGrossValue("EUR", 2425.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSavingsplan02()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Savingsplan02.txt"), errors);

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
                        hasIsin("IE00BZCQB185"), hasWkn(null), hasTicker(null), //
                        hasName("iShares MSCI India (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-05T02:11:02"), hasShares(1.32005), //
                        hasSource("Savingsplan02.txt"), //
                        hasNote("Order ID: aYRCKHJ8ZrONSjY"), //
                        hasAmount("EUR", 85.65), hasGrossValue("EUR", 85.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("US00123Q1040"), hasWkn(null), hasTicker(null), //
                        hasName("AGNC Investment Corp."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-01-15T00:00"), hasShares(0.663129), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.07), hasGrossValue("EUR", 0.08), //
                        hasForexGrossValue("USD", 0.08), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("AGNC Investment Corp.", "EUR");
        security.setIsin("US00123Q1040");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ScalableCapitalPDFExtractor(client);

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
                        hasDate("2025-01-15T00:00"), hasShares(0.663129), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.07), hasGrossValue("EUR", 0.08), //
                        hasTaxes("EUR", 0.01), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("US5949181045"), hasWkn(null), hasTicker(null), //
                        hasName("Microsoft Corp."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-06-17"), hasShares(0.284285), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.15), hasGrossValue("EUR", 0.20), //
                        hasForexGrossValue("USD", 0.23), //
                        hasTaxes("EUR", 0.03 + 0.02), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        var security = new Security("Microsoft Corp.", "EUR");
        security.setIsin("US5949181045");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ScalableCapitalPDFExtractor(client);

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
                        hasDate("2025-06-17"), hasShares(0.284285), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.15), hasGrossValue("EUR", 0.20), //
                        hasTaxes("EUR", 0.03 + 0.02), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("DE0007274136"), hasWkn(null), hasTicker(null), //
                        hasName("Sto SE & Co. KGaA"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-06-23"), hasShares(3.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 7.31), hasGrossValue("EUR", 9.93), //
                        hasTaxes("EUR", 2.48 + 0.14), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("US65339F1012"), hasWkn(null), hasTicker(null), //
                        hasName("Nextera Energy Inc."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-09-17"), hasShares(1.907523), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.69), hasGrossValue("EUR", 0.91), //
                        hasForexGrossValue("USD", 1.08), //
                        hasTaxes("EUR", 0.14 + 0.08), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("Nextera Energy Inc.", "EUR");
        security.setIsin("US65339F1012");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ScalableCapitalPDFExtractor(client);

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
                        hasDate("2025-09-17"), hasShares(1.907523), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.69), hasGrossValue("EUR", 0.91), //
                        hasTaxes("EUR", 0.14 + 0.08), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("US4278661081"), hasWkn(null), hasTicker(null), //
                        hasName("Hershey Co., The"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-09-18"), hasShares(0.489446), //
                        hasSource("Dividende05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.41), hasGrossValue("EUR", 0.56), //
                        hasForexGrossValue("USD", 0.66), //
                        hasTaxes("EUR", 0.09 + 0.06), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        var security = new Security("Hershey Co., The", "EUR");
        security.setIsin("US4278661081");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ScalableCapitalPDFExtractor(client);

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
                        hasDate("2025-09-18"), hasShares(0.489446), //
                        hasSource("Dividende05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.41), hasGrossValue("EUR", 0.56), //
                        hasTaxes("EUR", 0.09 + 0.06), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("XS2875106242"), hasWkn(null), hasTicker(null), //
                        hasName("Leverage Shares PLC"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-09-12"), hasShares(5.940872), //
                        hasSource("Dividende06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.82), hasGrossValue("EUR", 0.82), //
                        hasForexGrossValue("USD", 0.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        var security = new Security("Leverage Shares PLC", "EUR");
        security.setIsin("XS2875106242");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new ScalableCapitalPDFExtractor(client);

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
                        hasDate("2025-09-12"), hasShares(5.940872), //
                        hasSource("Dividende06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.82), hasGrossValue("EUR", 0.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

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
                        hasIsin("US67066G1040"), hasWkn(null), hasTicker(null), //
                        hasName("NVIDIA Corp."), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-31"), hasShares(20.43685), //
                        hasSource("Dividende07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.12), hasGrossValue("EUR", 0.17), //
                        hasTaxes("EUR", 0.05), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividend01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividend01.txt"), errors);

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
                        hasIsin("NL0011683594"), hasWkn(null), hasTicker(null), //
                        hasName("VanEck Mstr.DM Dividend.UC.ETF"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-06-11"), hasShares(2.769834), //
                        hasSource("Dividend01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.12), hasGrossValue("EUR", 2.49), //
                        hasTaxes("EUR", 0.37), hasFees("EUR", 0.00))));
    }

    @Test
    public void testRechnungsabschluss01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rechnungsabschluss01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-03-31T00:00"), hasShares(0.00), //
                        hasSource("Rechnungsabschluss01.txt"), //
                        hasNote("01.01.2025 - 31.03.2025"), //
                        hasAmount("EUR", 13.69), hasGrossValue("EUR", 13.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testRechnungsabschluss02()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rechnungsabschluss02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-09-30T00:00"), hasShares(0.00), //
                        hasSource("Rechnungsabschluss02.txt"), //
                        hasNote("01.07.2025 - 30.09.2025"), //
                        hasAmount("EUR", 6.06), hasGrossValue("EUR", 8.41), //
                        hasTaxes("EUR", 2.05 + 0.11 + 0.19), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest( //
                                        hasDate("2025-03-31"), //
                                        hasSource("Kontoauszug01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 13.69)))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-04-04"), hasAmount("EUR", 4.99), //
                        hasSource("Kontoauszug01.txt"), hasNote("Prime-Abonnementgebühr"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-04-07"), hasAmount("EUR", 29715.63), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-04-09"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2025-04-10"), hasAmount("EUR", 1.40), //
                        hasSource("Kontoauszug01.txt"), hasNote("Solidaritätszuschlag"))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2025-04-10"), hasAmount("EUR", 25.63), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kapitalertragssteuer"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2025-04-10"), hasAmount("EUR", 1.40), //
                        hasSource("Kontoauszug01.txt"), hasNote("Solidaritätszuschlag"))));

        // assert transaction
        assertThat(results, hasItem(taxes(hasDate("2025-04-10"), hasAmount("EUR", 25.63), //
                        hasSource("Kontoauszug01.txt"), hasNote("Kapitalertragssteuer"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-04-14"), hasAmount("EUR", 1200.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

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
        assertThat(results, hasItem(taxRefund(hasDate("2025-06-30"), hasAmount("EUR", 0.77), //
                        hasSource("Kontoauszug02.txt"), hasNote("Solidaritätszuschlag"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionAlternativeDocumentRequired, //
                        interest( //
                                        hasDate("2025-06-30"), //
                                        hasSource("Kontoauszug02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 56.27)))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2025-06-30"), hasAmount("EUR", 14.07), //
                        hasSource("Kontoauszug02.txt"), hasNote("Kapitalertragssteuer"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-07-24"), hasAmount("EUR", 11344.57), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2025-08-15"), hasAmount("EUR", 558.52), //
                        hasSource("Kontoauszug03.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-08-27"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Lastschrift"))));
    }

    @Test
    public void testAccountStatement01()
    {
        var extractor = new ScalableCapitalPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2025-07-04"), hasAmount("EUR", 1300.00), //
                        hasSource("AccountStatement01.txt"), hasNote("Direct debit"))));
    }
}
