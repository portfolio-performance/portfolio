package name.abuchen.portfolio.datatransfer.pdf.solarisbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SolarisbankAGPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class SolarisbankAGPDFExtractorTest
{
    @Test
    public void testGiroKontoauszug01()
    {
        SolarisbankAGPDFExtractor extractor = new SolarisbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-10-26"), hasAmount("EUR", 200), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("008eb3a1d003 etoken-google"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-10-27"), hasAmount("EUR", 150), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("a141b0b25f9d etoken-google"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-10-26"), hasAmount("EUR", 100), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("an Peter Panzwischen Kunden DE11111111111111111111"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-10-28"), hasAmount("EUR", 16.10), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Amazon.de AMAZON.DE LU"))));
    }

    @Test
    public void testGiroKontoauszug02()
    {
        SolarisbankAGPDFExtractor extractor = new SolarisbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-10-26"), hasAmount("EUR", 100), //
                        hasNote("von Peter Panzwischen Kunden DE11111111111111111111"))));
    }

    @Test
    public void testGiroKontoauszug03()
    {
        SolarisbankAGPDFExtractor extractor = new SolarisbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-10-19"), hasAmount("EUR", 11.99), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("11,99 EUR"))));
    }

    @Test
    public void testGiroKontoauszug04()
    {
        SolarisbankAGPDFExtractor extractor = new SolarisbankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-10-03"), hasAmount("EUR", 200), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("von Peter Pan"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-10-06"), hasAmount("EUR", 0.40), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Visa Geld zurueck AktionVISACLP0324 GB"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-10-20"), hasAmount("EUR", 18.78), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("an Peter Pan"))));
    }
}
