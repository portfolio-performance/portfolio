package name.abuchen.portfolio.datatransfer.pdf.creditsuisseag;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.feeRefund;
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
import name.abuchen.portfolio.datatransfer.pdf.CreditSuisseAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class CreditSuisseAGExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new CreditSuisseAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US46284V1017"), hasWkn("26754105"), hasTicker(null), //
                        hasName("Registered Shs Iron Mountain Inc"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-08T18:32:46"), hasShares(900.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 27776.51), hasGrossValue("USD", 27272.00), //
                        hasTaxes("USD", 40.91), hasFees("USD", 463.60))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2020-06-08T00:00"), hasShares(900.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 41.81), hasGrossValue("USD", 41.81), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new CreditSuisseAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("XS1055787680"), hasWkn("24160639"), hasTicker(null), //
                        hasName("6.25 % Fixed Rate Notes Norddeutsche"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-09-16T09:31:35"), hasShares(2000.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 190182.49), hasGrossValue("USD", 188486.11), //
                        hasTaxes("USD", 282.73), hasFees("USD", 1413.65))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2019-09-16T00:00"), hasShares(2000.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 40.48), hasGrossValue("USD", 40.48), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new CreditSuisseAGPDFExtractor(new Client());

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
                        hasIsin("GB00B03MLX29"), hasWkn("1987674"), hasTicker(null), //
                        hasName("Akt. -A- Royal Dutch Shell PLC"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2018-04-30T17:26:53"), hasShares(1000.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 28744.30), hasGrossValue("EUR", 29020.00), //
                        hasTaxes("EUR", 43.53), hasFees("EUR", 232.17))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new CreditSuisseAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88163VAD10"), hasWkn("2429251"), hasTicker(null), //
                        hasName("6.15 % Notes Teva Pharmaceutical Finance"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-02-22T09:00:55"), hasShares(1000.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 111718.52), hasGrossValue("USD", 112732.30), //
                        hasTaxes("USD", 168.96), hasFees("USD", 844.82))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2021-02-22T00:00"), hasShares(1000.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 44.69), hasGrossValue("USD", 44.69), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new CreditSuisseAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("XS1055787680"), hasWkn("24160639"), hasTicker(null), //
                        hasName("6.25 % FIXED RATE NOTES NORDDEUTSCHE"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-04-12T00:00"), hasExDate(null), //
                        hasShares(2000.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("Semesterzinsen"), //
                        hasAmount("USD", 6250.00), hasGrossValue("USD", 6250.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new CreditSuisseAGPDFExtractor(new Client());

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
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US46284V1017"), hasWkn("26754105"), hasTicker(null), //
                        hasName("REGISTERED SHS IRON MOUNTAIN INC"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-01-06T00:00"), hasExDate("2020-12-14T00:00"), //
                        hasShares(900.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("Quartalsdividende"), //
                        hasAmount("USD", 389.65), hasGrossValue("USD", 556.65), //
                        hasTaxes("USD", 167.00), hasFees("USD", 0.00))));
    }
}
