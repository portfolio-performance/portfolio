package name.abuchen.portfolio.datatransfer.pdf.solarisbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SolarisbankAGPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class SolarisbankAGPDFExtractorTest
{
    @Test
    public void testGiroKontoauszug01()
    {
        var extractor = new SolarisbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug01.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2022-10-26"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("008eb3a1d003 etoken-google"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-10-27"), hasAmount("EUR", 150.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("a141b0b25f9d etoken-google"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-10-26"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug01.txt"),
                        hasNote("an Peter Panzwischen Kunden DE11111111111111111111"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-10-28"), hasAmount("EUR", 16.10), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Amazon.de AMAZON.DE LU"))));
    }

    @Test
    public void testGiroKontoauszug02()
    {
        var extractor = new SolarisbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug02.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2022-10-26"), hasAmount("EUR", 100.00), //
                        hasNote("von Peter Panzwischen Kunden DE11111111111111111111"))));
    }

    @Test
    public void testGiroKontoauszug03()
    {
        var extractor = new SolarisbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug03.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2022-10-19"), hasAmount("EUR", 11.99), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("11,99 EUR"))));
    }

    @Test
    public void testGiroKontoauszug04()
    {
        var extractor = new SolarisbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug04.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2022-10-03"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("von Peter Pan"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-10-06"), hasAmount("EUR", 0.40), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Visa Geld zurueck AktionVISACLP0324 GB"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-10-20"), hasAmount("EUR", 18.78), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("an Peter Pan"))));
    }

    @Test
    public void testReferenzkontoauszug01()
    {
        var extractor = new SolarisbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Referenzkontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-08"), hasAmount("EUR", 3000.00), //
                        hasSource("Referenzkontoauszug01.txt"), hasNote("YLbKRus qXwsATAvg"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-11-10"), hasAmount("EUR", 1000.00), //
                        hasSource("Referenzkontoauszug01.txt"), hasNote("EUWAX Aktiengesellschaft"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-14"), hasAmount("EUR", 1105.19), //
                        hasSource("Referenzkontoauszug01.txt"), hasNote("EUWAX Aktiengesellschaft"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-11-15"), hasAmount("EUR", 0.01), //
                        hasSource("Referenzkontoauszug01.txt"), hasNote("sMPghUOQW KOQy PbfeyvP"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-11-15"), hasAmount("EUR", 0.01), //
                        hasSource("Referenzkontoauszug01.txt"), hasNote("UVOshYrqp ejDi CIXGqbX"))));
    }
}
