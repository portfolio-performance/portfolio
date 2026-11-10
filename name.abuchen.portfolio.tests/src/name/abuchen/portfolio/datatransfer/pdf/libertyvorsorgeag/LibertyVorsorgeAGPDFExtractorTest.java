package name.abuchen.portfolio.datatransfer.pdf.libertyvorsorgeag;

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
import name.abuchen.portfolio.datatransfer.pdf.LibertyVorsorgeAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class LibertyVorsorgeAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new LibertyVorsorgeAGPDFExtractor(new Client());

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
                        hasIsin("CH0352765397"), hasWkn("35276539"), hasTicker(null), //
                        hasName("Credit Suisse Index Fund (CH) II Umbrella - CSIF (CH) II Gold Blue"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-05T00:00"), hasShares(0.201), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftragsnummer AUF230609-4926966"), //
                        hasAmount("CHF", 286.12), hasGrossValue("CHF", 286.12), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new LibertyVorsorgeAGPDFExtractor(new Client());

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
                        hasIsin("CH0002782263"), hasWkn("278226"), hasTicker(null), //
                        hasName("LA FONCIERE"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-05T00:00"), hasShares(1.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer AUF230609-4926595"), //
                        hasAmount("CHF", 130.12), hasGrossValue("CHF", 130.00), //
                        hasTaxes("CHF", 0.10), hasFees("CHF", 0.02))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new LibertyVorsorgeAGPDFExtractor(new Client());

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
                        hasIsin("CH0130595124"), hasWkn("13059512"), hasTicker(null), //
                        hasName("UBS ETF (CH) - SPI (R) Mid"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-26T00:00"), hasShares(1.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftragsnummer AUF230803-5149443"), //
                        hasAmount("CHF", 115.81), hasGrossValue("CHF", 115.70), //
                        hasTaxes("CHF", 0.09), hasFees("CHF", 0.02))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new LibertyVorsorgeAGPDFExtractor(new Client());

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
                        hasIsin("IE00BHZRQZ17"), hasWkn("46325074"), hasTicker(null), //
                        hasName("FranklinTempletonICAV -FranklinFTSEIndia UCITS"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-06T00:00"), hasShares(8.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer AUF250312-8240903"), //
                        hasAmount("CHF", 283.80), hasGrossValue("CHF", 284.26), //
                        hasForexGrossValue("USD", 321.19), //
                        hasTaxes("CHF", 0.42), hasFees("CHF", 0.04))));
    }

    @Test
    public void testWertpapierVerkauf01WithSecurityInCHF()
    {
        var security = new Security("FranklinTempletonICAV -FranklinFTSEIndia UCITS", "CHF");
        security.setIsin("IE00BHZRQZ17");
        security.setWkn("46325074");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new LibertyVorsorgeAGPDFExtractor(client);

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
        new AssertImportActions().check(results, "CHF");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-03-06T00:00"), hasShares(8.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer AUF250312-8240903"), //
                        hasAmount("CHF", 283.80), hasGrossValue("CHF", 284.26), //
                        hasTaxes("CHF", 0.42), hasFees("CHF", 0.04))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new LibertyVorsorgeAGPDFExtractor(new Client());

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
                        hasIsin("CH0130595124"), hasWkn("13059512"), hasTicker(null), //
                        hasName("UBS ETF (CH) - SPI (R) Mid"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-09-29T00:00"), hasShares(6.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftragsnummer AUF231005-5396675"), //
                        hasAmount("CHF", 643.16), hasGrossValue("CHF", 643.74), //
                        hasTaxes("CHF", 0.48), hasFees("CHF", 0.10))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new LibertyVorsorgeAGPDFExtractor(new Client());

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
                        hasIsin("CH0384998453"), hasWkn("38499845"), hasTicker(null), //
                        hasName("Credit Suisse Index Fund (CH) Umbrella - CSIF (CH) Equity Switzerland Large Cap Classic Blue"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-12-18T00:00"), hasShares(0.011), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftragsnummer AUF231221-5762573"), //
                        hasAmount("CHF", 14.54), hasGrossValue("CHF", 14.54), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new LibertyVorsorgeAGPDFExtractor(new Client());

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
                        hasIsin("LU2056739464"), hasWkn("53154391"), hasTicker(null), //
                        hasName("Multi Units Luxembourg SICAV - Lyxor MSCI World Climate Change (DR) UCITS ETF"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-11-16T00:00"), hasShares(43.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftragsnummer AUF231123-5614285"), //
                        hasAmount("CHF", 271.67), hasGrossValue("CHF", 272.08), //
                        hasForexGrossValue("USD", 306.29), //
                        hasTaxes("CHF", 0.41), hasFees("CHF", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf04WithSecurityInCHF()
    {
        var security = new Security("Multi Units Luxembourg SICAV - Lyxor MSCI World Climate Change (DR) UCITS ETF", "CHF");
        security.setIsin("LU2056739464");
        security.setWkn("53154391");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new LibertyVorsorgeAGPDFExtractor(client);

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
                        hasDate("2023-11-16T00:00"), hasShares(43.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftragsnummer AUF231123-5614285"), //
                        hasAmount("CHF", 271.67), hasGrossValue("CHF", 272.08), //
                        hasTaxes("CHF", 0.41), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new LibertyVorsorgeAGPDFExtractor(new Client());

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
                        hasIsin("CH1117196001"), hasWkn("111719600"), hasTicker(null), //
                        hasName("Swisscanto(CH)Index FundV - Index Bond Fu nd Corp.CH F Responsible"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-06-20T00:00"), hasExDate("2025-06-17T00:00"), //
                        hasShares(1.739), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Ref.-Nr.: CA20250624/95977"), //
                        hasAmount("CHF", 1.30), hasGrossValue("CHF", 2.00), //
                        hasTaxes("CHF", 0.70), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new LibertyVorsorgeAGPDFExtractor(new Client());

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
                        hasIsin("CH0130595124"), hasWkn("13059512"), hasTicker(null), //
                        hasName("UBS ETF (CH) - SPI (R) Mid"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-13T00:00"), hasExDate("2023-09-08T00:00"), //
                        hasShares(6.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Ref.-Nr.: CA20230914/18685"), //
                        hasAmount("CHF", 10.10), hasGrossValue("CHF", 15.54), //
                        hasTaxes("CHF", 5.44), hasFees("CHF", 0.00))));
    }

    @Test
    public void testKapitalgewinn01()
    {
        var extractor = new LibertyVorsorgeAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kapitalgewinn01.txt"), errors);

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
                        hasIsin("CH0130595124"), hasWkn("13059512"), hasTicker(null), //
                        hasName("UBS ETF (CH) - SPI (R) Mid"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-13T00:00"), hasExDate("2023-09-08T00:00"), //
                        hasShares(6.00), //
                        hasSource("Kapitalgewinn01.txt"), //
                        hasNote("Ref.-Nr.: CA20230914/18862"), //
                        hasAmount("CHF", 1.86), hasGrossValue("CHF", 1.86), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }
}
