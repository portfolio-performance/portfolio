package name.abuchen.portfolio.datatransfer.pdf.fondsdepotbank;

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

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.FondsdepotBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class FondsdepotBankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new FondsdepotBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0187079347"), hasWkn("A0CA0W"), hasTicker(null), //
                        hasName("Robeco GlobConsTrend D EUR"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-15"), hasShares(0.125), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 48.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 1.43))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-10-15"), hasShares(0.126), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 48.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 1.38))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new FondsdepotBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0060230025"), hasWkn("986514"), hasTicker(null), //
                        hasName("AB - InternaTechnologyPfAU"), //
                        hasCurrencyCode("USD"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-18"), hasShares(0.050), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 47.41), //
                        hasForexGrossValue("USD", 49.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 2.59))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-17"), hasShares(0.062), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 47.82), //
                        hasForexGrossValue("USD", 51.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 2.18))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-15"), hasShares(0.072), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 47.87), //
                        hasForexGrossValue("USD", 53.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 2.13))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-15"), hasShares(0.059), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 47.40), //
                        hasForexGrossValue("USD", 52.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 2.60))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-16"), hasShares(0.058), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 47.33), //
                        hasForexGrossValue("USD", 54.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 2.67))));
    }

    @Test
    public void testWertpapierKauf02WithSecurityInEUR()
    {
        var security = new Security("AB - InternaTechnologyPfAU", "EUR");
        security.setIsin("LU0060230025");
        security.setWkn("986514");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FondsdepotBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-02-18"), hasShares(0.050), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 47.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 2.59))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-03-17"), hasShares(0.062), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 47.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 2.18))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-15"), hasShares(0.072), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 47.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 2.13))));

        // check 4th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-15"), hasShares(0.059), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 47.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 2.60))));

        // check 5th buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-16"), hasShares(0.058), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.00), hasGrossValue("EUR", 47.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 2.67))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new FondsdepotBankPDFExtractor(new Client());

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
                        hasIsin("DE000A2N8127"), hasWkn("A2N812"), hasTicker(null), //
                        hasName("BITGlobTechnoloLeaders R-I"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-05"), hasShares(6.104), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Tausch"), //
                        hasAmount("EUR", 4723.15), hasGrossValue("EUR", 4580.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 137.22))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new FondsdepotBankPDFExtractor(new Client());

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
                        hasIsin("LU0329201957"), hasWkn("A0M6Z1"), hasTicker(null), //
                        hasName("JPM GlobalDividendAaccUSD"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-02"), hasShares(20.803), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Tausch"), //
                        hasAmount("EUR", 5000.00), hasGrossValue("EUR", 4757.20), //
                        hasForexGrossValue("USD", 5499.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 237.80))));
    }

    @Test
    public void testWertpapierKauf04WithSecurityInEUR()
    {
        var security = new Security("JPM GlobalDividendAaccUSD", "EUR");
        security.setIsin("LU0329201957");
        security.setWkn("A0M6Z1");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FondsdepotBankPDFExtractor(client);

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
                        hasDate("2025-09-02"), hasShares(20.803), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Tausch"), //
                        hasAmount("EUR", 5000.00), hasGrossValue("EUR", 4757.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 5.00 + 237.80))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new FondsdepotBankPDFExtractor(new Client());

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
                        hasIsin("IE00BFNM3J75"), hasWkn("A2N6TD"), hasTicker(null), //
                        hasName("iSharesIV MSCIWoScr UCETFUa"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-07-16"), hasShares(2.590), //
                        hasSource("Kauf05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.85), //
                        hasForexGrossValue("USD", 28.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.15 + 0.00))));
    }

    @Test
    public void testWertpapierKauf05WithSecurityInEUR()
    {
        var security = new Security("iSharesIV MSCIWoScr UCETFUa", "EUR");
        security.setIsin("IE00BFNM3J75");
        security.setWkn("A2N6TD");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FondsdepotBankPDFExtractor(client);

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
                        hasDate("2025-07-16"), hasShares(2.590), //
                        hasSource("Kauf05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 24.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.15 + 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new FondsdepotBankPDFExtractor(new Client());

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
                        hasIsin("LU0187079347"), hasWkn("A0CA0W"), hasTicker(null), //
                        hasName("Robeco GlobConsTrend D EUR"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-09-02"), hasShares(12.791), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Tausch"), //
                        hasAmount("EUR", 4723.15), hasGrossValue("EUR", 4859.43), //
                        hasTaxes("EUR", 124.44 + 6.84 + 0.00), hasFees("EUR", 5.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new FondsdepotBankPDFExtractor(new Client());

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
                        hasIsin("LU0060230025"), hasWkn("986514"), hasTicker(null), //
                        hasName("AB - InternaTechnologyPfAU"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-09-02"), hasShares(6.340), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Tausch"), //
                        hasAmount("EUR", 5000.00), hasGrossValue("EUR", 5471.32), //
                        hasForexGrossValue("USD", 6423.78), //
                        hasTaxes("EUR", 442.01 + 24.31 + 0.00), hasFees("EUR", 5.00))));
    }

    @Test
    public void testWertpapierVerkauf02WithSecurityInEUR()
    {
        var security = new Security("AB - InternaTechnologyPfAU", "EUR");
        security.setIsin("LU0060230025");
        security.setWkn("986514");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new FondsdepotBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

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
                        hasDate("2025-09-02"), hasShares(6.340), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Tausch"), //
                        hasAmount("EUR", 5000.00), hasGrossValue("EUR", 5471.32), //
                        hasTaxes("EUR", 442.01 + 24.31 + 0.00), hasFees("EUR", 5.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new FondsdepotBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

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
                        hasIsin("LU0187079347"), hasWkn("A0CA0W"), hasTicker(null), //
                        hasName("Robeco GlobConsTrend D EUR"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-04-01"), hasShares(0.201), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Gebührenverkauf"), //
                        hasAmount("EUR", 45.83), hasGrossValue("EUR", 45.83), //
                        hasTaxes("EUR", 0.00 + 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-04-01"), hasShares(0.201), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Depotführungsentgelt lfd. Jahr"), //
                        hasAmount("EUR", 45.83), hasGrossValue("EUR", 45.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWiederanlage01()
    {
        var extractor = new FondsdepotBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Wiederanlage01.txt"), errors);

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
                        hasIsin("LU0731782404"), hasWkn("A1JSY0"), hasTicker(null), //
                        hasName("FF-GloDivFd A-QINCOME(G)Eur"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-11"), hasShares(0.775), //
                        hasSource("Wiederanlage01.txt"), //
                        hasNote("Wiederanlage Ertrag"), //
                        hasAmount("EUR", 19.81), hasGrossValue("EUR", 19.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00 + 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new FondsdepotBankPDFExtractor(new Client());

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
                        hasIsin("LU0731782404"), hasWkn("A1JSY0"), hasTicker(null), //
                        hasName("FF-GloDivFd A-QINCOME(G)Eur"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-11-12"), hasExDate("2025-11-03"), //
                        hasShares(157.501), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 19.81), hasGrossValue("EUR", 24.29), //
                        hasTaxes("EUR", 4.25 + 0.23 + 0.00), hasFees("EUR", 0.00))));
    }
}
