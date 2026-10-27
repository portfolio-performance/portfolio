package name.abuchen.portfolio.datatransfer.pdf.lgtbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
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
import name.abuchen.portfolio.datatransfer.pdf.LGTBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class LGTBankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "DKK");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DK0010244508"), hasWkn("861837"), hasTicker(null), //
                        hasName("A.P. Moeller - Maersk A/S Namen- und Inhaber-Aktien -B-"), //
                        hasCurrencyCode("DKK"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-14T09:00:02"), hasShares(12.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftragsnummer: 262697612 | Valorennummer 906020"), //
                        hasAmount("DKK", 82452.21), hasGrossValue("DKK", 80784.00), //
                        hasTaxes("DKK", 121.19), hasFees("DKK", 1534.90 + 12.12))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "DKK");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DK0060534915"), hasWkn("A1XA8R"), hasTicker(null), //
                        hasName("Novo Nordisk A/S Namen-Aktien -B-"), //
                        hasCurrencyCode("DKK"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-14T09:00"), hasShares(180.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Auftragsnummer: 323232609 | Valorennummer 23159222"), //
                        hasAmount("DKK", 72811.75), hasGrossValue("DKK", 71280.00), //
                        hasTaxes("DKK", 106.90), hasFees("DKK", 1414.16 + 10.69))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
                        hasIsin("CH0031768937"), hasWkn("A0MW4N"), hasTicker(null), //
                        hasName("iShares ETF (CH) - iShares SLI(R) ETF (CH) Inhaber-Anteile -A-"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-29T14:54:02"), hasShares(260.00), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Auftragsnummer: 210796978 | Valorennummer 3176893"), //
                        hasAmount("CHF", 48502.01), hasGrossValue("CHF", 48465.46), //
                        hasTaxes("CHF", 36.35), hasFees("CHF", 0.20))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2051469208"), hasWkn(null), hasTicker(null), //
                        hasName("JPMorgan Funds SICAV - Emerging Markets Sustainable Equity Fund"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-08T00:00"), hasShares(480.00), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Auftragsnummer: 209174085 | Valorennummer 50139326"), //
                        hasAmount("USD", 50595.78), hasGrossValue("USD", 50520.00), //
                        hasTaxes("USD", 75.78), hasFees("USD", 0.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
                        hasIsin("CH0012032048"), hasWkn("855167"), hasTicker(null), //
                        hasName("Roche Holding AG Inhaber-Genussschein"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-26T00:00"), hasShares(40.00), //
                        hasSource("Kauf05.txt"), //
                        hasNote("Auftragsnummer: 212706117 | Valorennummer 1203204"), //
                        hasAmount("CHF", 10931.63), hasGrossValue("CHF", 10922.00), //
                        hasTaxes("CHF", 8.19), hasFees("CHF", 1.24 + 0.20))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BZ02LR44"), hasWkn("A2AQST"), hasTicker(null), //
                        hasName("Xtrackers(IE)PLC - Xtrackers MSCI World ESG UCITS ETF Namen-Anteile -1C- / Class USD"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-06-08T11:22:14"), hasShares(260.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Auftragsnummer: 209179086 | Valorennummer 41359963"), //
                        hasAmount("USD", 8332.19), hasGrossValue("USD", 8344.70), //
                        hasTaxes("USD", 12.51), hasFees("USD", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
                        hasIsin("CH0031768937"), hasWkn("A0MW4N"), hasTicker(null), //
                        hasName("iShares ETF (CH) - iShares SLI(R) ETF (CH) Inhaber-Anteile -A-"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-07-04T00:00"), hasShares(260.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Auftragsnummer: 123295456 | Valorennummer 3176893"), //
                        hasAmount("CHF", 48591.42), hasGrossValue("CHF", 48636.90), //
                        hasTaxes("CHF", 36.48), hasFees("CHF", 8.80 + 0.20))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
                        hasIsin("CH0012005267"), hasWkn("904278"), hasTicker(null), //
                        hasName("Novartis AG Namen-Aktien"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-11-02T00:00"), hasShares(260.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftragsnummer: 219764567 | Valorennummer 1200526"), //
                        hasAmount("CHF", 21954.31), hasGrossValue("CHF", 21972.42), //
                        hasTaxes("CHF", 16.48), hasFees("CHF", 1.43 + 0.20))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
                        hasIsin("FR0000124141"), hasWkn("1098758"), hasTicker(null), //
                        hasName("Veolia Environnement SA Namen- und Inhaber-Aktien"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-14T00:00"), hasExDate("2020-05-12T00:00"), //
                        hasShares(551.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Auftragsnummer: 256401138 | Ordentliche Dividende"), //
                        hasAmount("EUR", 198.36), hasGrossValue("EUR", 275.50), //
                        hasTaxes("EUR", 77.14), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
                        hasIsin("CH0012005267"), hasWkn("1200526"), hasTicker(null), //
                        hasName("Novartis AG Namen-Aktien"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-03-13T00:00"), hasExDate("2023-03-09T00:00"), //
                        hasShares(760.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Auftragsnummer: 200738771 | Ordentliche Dividende"), //
                        hasAmount("CHF", 1580.80), hasGrossValue("CHF", 2432.00), //
                        hasTaxes("CHF", 851.20), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
                        hasIsin("CH0024638196"), hasWkn("2463819"), hasTicker(null), //
                        hasName("Schindler Holding AG Inhaber-Partizipationsschein"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-04-03T00:00"), hasExDate("2023-03-30T00:00"), //
                        hasShares(130.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote("Auftragsnummer: 330401346 | Ordentliche Dividende"), //
                        hasAmount("CHF", 338.00), hasGrossValue("CHF", 520.00), //
                        hasTaxes("CHF", 182.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
                        hasIsin("CH0002497458"), hasWkn("249745"), hasTicker(null), //
                        hasName("SGS Ltd Namen-Aktien"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-04-03T00:00"), hasExDate("2023-03-30T00:00"), //
                        hasShares(12.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Auftragsnummer: 303105603 | Ordentliche Dividende"), //
                        hasAmount("CHF", 624.00), hasGrossValue("CHF", 960.00), //
                        hasTaxes("CHF", 336.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new LGTBankPDFExtractor(new Client());

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
                        hasIsin("CH0031768937"), hasWkn("3176893"), hasTicker(null), //
                        hasName("iShares ETF (CH) - iShares SLI(R) ETF (CH) Inhaber-Anteile -A-"), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-04-03T00:00"), hasExDate("2023-03-30T00:00"), //
                        hasShares(490.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote("Auftragsnummer: 303107922 | Ordentliche Dividende"), //
                        hasAmount("CHF", 127.40), hasGrossValue("CHF", 196.00), //
                        hasTaxes("CHF", 68.60), hasFees("CHF", 0.00))));
    }
}
