package name.abuchen.portfolio.datatransfer.pdf.hypothekarbanklenzburgag;

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
import name.abuchen.portfolio.datatransfer.pdf.HypothekarbankLenzburgAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class HypothekarbankLenzburgAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000716YHJ7"), hasWkn("125615212"), hasTicker(null), //
                        hasName("Inve FTSE All"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-14T00:00"), hasShares(720.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Transaktion 61327806-0002"), //
                        hasAmount("CHF", 3974.14), hasGrossValue("CHF", 3948.48), //
                        hasTaxes("CHF", 5.92), hasFees("CHF", 19.74))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000716YHJ7"), hasWkn("125615212"), hasTicker(null), //
                        hasName("Inve FTSE All"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-04T00:00"), hasShares(44.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Transaktion 62108127-0002"), //
                        hasAmount("CHF", 251.86), hasGrossValue("CHF", 250.23), //
                        hasTaxes("CHF", 0.38), hasFees("CHF", 1.25))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BKS7L097"), hasWkn("51992937"), hasTicker(null), //
                        hasName("Inv S&P 500 ESG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-04T00:00"), hasShares(4.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Transaktion 62108136-0002"), //
                        hasAmount("CHF", 260.10), hasGrossValue("CHF", 258.42), //
                        hasTaxes("CHF", 0.39), hasFees("CHF", 1.29))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn("18575459"), hasTicker(null), //
                        hasName("Van FTSE All Wr"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-03T00:00"), hasShares(8.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Transaktion 52584421-1234"), //
                        hasAmount("CHF", 929.78), hasGrossValue("CHF", 923.77), //
                        hasTaxes("CHF", 1.39), hasFees("CHF", 4.62))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B6YX5D40"), hasWkn("13976063"), hasTicker(null), //
                        hasName("SPDR S&P US Di"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-03T00:00"), hasShares(7.00), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Transaktion 85741290-0003"), //
                        hasAmount("CHF", 461.87), hasGrossValue("CHF", 458.89), //
                        hasTaxes("CHF", 0.69), hasFees("CHF", 2.29))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000V93BNU0"), hasWkn("113303241"), hasTicker(null), //
                        hasName("Wld ESG"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-09-03T00:00"), hasShares(113.00), //
                        hasSource("Kauf06.txt"), //
                        hasNote("Transaktion 12301780-0324"), //
                        hasAmount("CHF", 519.20), hasGrossValue("CHF", 515.85), //
                        hasTaxes("CHF", 0.77), hasFees("CHF", 2.58))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn("951692"), hasTicker(null), //
                        hasName("Registered Shs Microsoft Corp"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-10-09T00:00"), hasShares(15.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Transaktion 69155585-0002"), //
                        hasAmount("CHF", 5399.37), hasGrossValue("CHF", 5337.98), //
                        hasTaxes("CHF", 8.01), hasFees("CHF", 53.38))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFNM3G45"), hasWkn("43695283"), hasTicker(null), //
                        hasName("iSh MSCI USA"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-02-04T00:00"), hasShares(19.00), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Transaktion 41805757-0041"), //
                        hasAmount("CHF", 152.63), hasGrossValue("CHF", 152.25), //
                        hasForexGrossValue("USD", 165.40), //
                        hasTaxes("CHF", 0.23), hasFees("CHF", 0.15))));
    }

    @Test
    public void testWertpapierKauf08WithSecurityInCHF()
    {
        var security = new Security("iSh MSCI USA", "CHF");
        security.setIsin("IE00BFNM3G45");
        security.setWkn("43695283");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HypothekarbankLenzburgAGPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-02-04T00:00"), hasShares(19.00), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Transaktion 41805757-0041"), //
                        hasAmount("CHF", 152.63), hasGrossValue("CHF", 152.25), //
                        hasTaxes("CHF", 0.23), hasFees("CHF", 0.15))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0189613653"), hasWkn("18961365"), hasTicker(null), //
                        hasName("Namen-Anteile -I-A1- UBSCHIF-BCHACP"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-29T00:00"), hasShares(0.218), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Transaktion 55679687-0038"), //
                        hasAmount("CHF", 213.76), hasGrossValue("CHF", 213.76), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH1108676268"), hasWkn("110867626"), hasTicker(null), //
                        hasName("Underlying Tracker Asset Segregated SPV"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-08T00:00"), hasShares(5.00), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Transaktion 62225588-0006"), //
                        hasAmount("CHF", 4774.78), hasGrossValue("CHF", 4767.63), //
                        hasForexGrossValue("USD", 5261.70), //
                        hasTaxes("CHF", 7.15), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf10WithSecurityInCHF()
    {
        var security = new Security("Underlying Tracker Asset Segregated SPV", "CHF");
        security.setIsin("CH1108676268");
        security.setWkn("110867626");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HypothekarbankLenzburgAGPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-08T00:00"), hasShares(5.00), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Transaktion 62225588-0006"), //
                        hasAmount("CHF", 4774.78), hasGrossValue("CHF", 4767.63), //
                        hasTaxes("CHF", 7.15), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0012335540"), hasWkn("1233554"), hasTicker(null), //
                        hasName("Namen-Akt Vontobel Holding AG Nom. CHF"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-12-27T00:00"), hasShares(10.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Transaktion 1234567-0131"), //
                        hasAmount("CHF", 634.21), hasGrossValue("CHF", 637.88), //
                        hasTaxes("CHF", 0.48), hasFees("CHF", 3.19))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFNM3L97"), hasWkn("43671001"), hasTicker(null), //
                        hasName("iShs Jap ESG"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-29T00:00"), hasShares(5.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Transaktion 66334665-0076"), //
                        hasAmount("CHF", 29.46), hasGrossValue("CHF", 29.55), //
                        hasForexGrossValue("USD", 33.40), //
                        hasTaxes("CHF", 0.04), hasFees("CHF", 0.05))));
    }

    @Test
    public void testWertpapierVerkauf02WithSecurityInCHF()
    {
        var security = new Security("iShs Jap ESG", "CHF");
        security.setIsin("IE00BFNM3L97");
        security.setWkn("43671001");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HypothekarbankLenzburgAGPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-07-29T00:00"), hasShares(5.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Transaktion 66334665-0076"), //
                        hasAmount("CHF", 29.46), hasGrossValue("CHF", 29.55), //
                        hasTaxes("CHF", 0.04), hasFees("CHF", 0.05))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0189613653"), hasWkn("18961365"), hasTicker(null), //
                        hasName("Namen-Anteile -I-A1- UBSCHIF-BCHACP"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-10-10T00:00"), hasShares(0.218), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Transaktion 55973830-0045"), //
                        hasAmount("CHF", 214.15), hasGrossValue("CHF", 214.15), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH1108676268"), hasWkn("110867626"), hasTicker(null), //
                        hasName("Underlying Tracker Asset Segregated SPV"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-08-11T00:00"), hasShares(5.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Transaktion 82230675"), //
                        hasAmount("CHF", 3728.25), hasGrossValue("CHF", 3728.25), //
                        hasForexGrossValue("USD", 4603.35), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf04WithSecurityInCHF()
    {
        var security = new Security("Underlying Tracker Asset Segregated SPV", "CHF");
        security.setIsin("CH1108676268");
        security.setWkn("110867626");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HypothekarbankLenzburgAGPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-08-11T00:00"), hasShares(5.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Transaktion 82230675"), //
                        hasAmount("CHF", 3728.25), hasGrossValue("CHF", 3728.25), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3RBWM25"), hasWkn("18575459"), hasTicker(null), //
                        hasName("Van FTSE All Wr"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-26T00:00"), hasShares(168.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Quartalsdividende | Transaktion 65062408"), //
                        hasAmount("CHF", 116.96), hasGrossValue("CHF", 116.96), //
                        hasForexGrossValue("USD", 132.53), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInCHF()
    {
        var security = new Security("Van FTSE All Wr", "CHF");
        security.setIsin("IE00B3RBWM25");
        security.setWkn("18575459");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HypothekarbankLenzburgAGPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-26T00:00"), hasShares(168.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Quartalsdividende | Transaktion 65062408"), //
                        hasAmount("CHF", 116.96), hasGrossValue("CHF", 116.96), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B6YX5D40"), hasWkn("13976063"), hasTicker(null), //
                        hasName("SPDR S&P US Di"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-02T00:00"), hasShares(7.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Quartalsdividende | Transaktion 65379273"), //
                        hasAmount("CHF", 2.66), hasGrossValue("CHF", 2.66), //
                        hasForexGrossValue("USD", 2.99), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInCHF()
    {
        var security = new Security("SPDR S&P US Di", "CHF");
        security.setIsin("IE00B6YX5D40");
        security.setWkn("13976063");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HypothekarbankLenzburgAGPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-07-02T00:00"), hasShares(7.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Quartalsdividende | Transaktion 65379273"), //
                        hasAmount("CHF", 2.66), hasGrossValue("CHF", 2.66), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0008742519"), hasWkn("874251"), hasTicker(null), //
                        hasName("Namen-Akt Swisscom AG Nom."), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-04-04T00:00"), hasShares(10.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Jahresdividende | Transaktion 46148061"), //
                        hasAmount("CHF", 143.00), hasGrossValue("CHF", 220.00), //
                        hasTaxes("CHF", 77.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0008837566"), hasWkn("883756"), hasTicker(null), //
                        hasName("Namen-Akt Allreal Holding AG Nom."), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-04-25T00:00"), hasShares(25.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Jahresdividende | Transaktion 57923179"), //
                        hasAmount("CHF", 56.87), hasGrossValue("CHF", 87.50), //
                        hasTaxes("CHF", 30.63), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0008837566"), hasWkn("883756"), hasTicker(null), //
                        hasName("Namen-Akt Allreal Holding AG Nom."), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-04-25T00:00"), hasShares(25.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Transaktion 62847906"), //
                        hasAmount("CHF", 87.50), hasGrossValue("CHF", 87.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB0000536739"), hasWkn("370440"), hasTicker(null), //
                        hasName("Registered Shs Ashtead Group PLC Nom."), //
                        hasCurrencyCode("GBP"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-12T00:00"), hasShares(9.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Halbjahresdividende | Transaktion 55134009"), //
                        hasAmount("CHF", 6.75), hasGrossValue("CHF", 6.75), //
                        hasForexGrossValue("GBP", 6.05), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende06WithSecurityInCHF()
    {
        var security = new Security("Registered Shs Ashtead Group PLC Nom.", "CHF");
        security.setIsin("GB0000536739");
        security.setWkn("370440");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HypothekarbankLenzburgAGPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-12T00:00"), hasShares(9.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote("Halbjahresdividende | Transaktion 55134009"), //
                        hasAmount("CHF", 6.75), hasGrossValue("CHF", 6.75), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000TLX1005"), hasWkn("19625225"), hasTicker(null), //
                        hasName("Namen-Akt Talanx AG"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-13T00:00"), hasShares(2.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Jahresdividende | Transaktion 78321807"), //
                        hasAmount("CHF", 3.73), hasGrossValue("CHF", 5.06), //
                        hasForexGrossValue("EUR", 5.40), //
                        hasTaxes("CHF", 1.33), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende07WithSecurityInCHF()
    {
        var security = new Security("Namen-Akt Talanx AG", "CHF");
        security.setIsin("DE000TLX1005");
        security.setWkn("19625225");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HypothekarbankLenzburgAGPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-13T00:00"), hasShares(2.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote("Jahresdividende | Transaktion 78321807"), //
                        hasAmount("CHF", 3.73), hasGrossValue("CHF", 5.06), //
                        hasTaxes("CHF", 1.33), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new HypothekarbankLenzburgAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US74762E1029"), hasWkn("852865"), hasTicker(null), //
                        hasName("Registered Shs Quanta Services Inc"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-10-13T00:00"), hasShares(4.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Quartalsdividende | Transaktion 56086336"), //
                        hasAmount("CHF", 0.19), hasGrossValue("CHF", 0.24), //
                        hasForexGrossValue("USD", 0.26), //
                        hasTaxes("CHF", 0.05), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende08WithSecurityInCHF()
    {
        var security = new Security("Registered Shs Quanta Services Inc", "CHF");
        security.setIsin("US74762E1029");
        security.setWkn("852865");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new HypothekarbankLenzburgAGPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-10-13T00:00"), hasShares(4.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote("Quartalsdividende | Transaktion 56086336"), //
                        hasAmount("CHF", 0.19), hasGrossValue("CHF", 0.24), //
                        hasTaxes("CHF", 0.05), hasFees("CHF", 0.00))));
    }
}
