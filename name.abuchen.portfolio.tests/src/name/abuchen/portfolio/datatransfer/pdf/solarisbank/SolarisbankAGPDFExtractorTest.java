package name.abuchen.portfolio.datatransfer.pdf.solarisbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.InputFile;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SolarisbankAGPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class SolarisbankAGPDFExtractorTest
{
    @Test
    public void testGiroKontoauszug01()
    {
        SolarisbankAGPDFExtractor extractor = new SolarisbankAGPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("GiroKontoauszug01.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));

        assertThat(results, hasItem(deposit(hasDate("2022-10-26"), hasAmount("EUR", 200), //
                        hasNote("008eb3a1d003 etoken-google"))));

        assertThat(results, hasItem(deposit(hasDate("2022-10-27"), hasAmount("EUR", 150), //
                        hasNote("a141b0b25f9d etoken-google"))));

        assertThat(results, hasItem(removal(hasDate("2022-10-26"), hasAmount("EUR", 100), //
                        hasNote("an Peter Panzwischen Kunden DE11111111111111111111"))));

        assertThat(results, hasItem(removal(hasDate("2022-10-28"), hasAmount("EUR", 16.10), //
                        hasNote("Amazon.de AMAZON.DE LU"))));
    }

    @Test
    public void testGiroKontoauszug02()
    {
        SolarisbankAGPDFExtractor extractor = new SolarisbankAGPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("GiroKontoauszug02.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        assertThat(results, hasItem(deposit(hasDate("2022-10-26"), hasAmount("EUR", 100), //
                        hasNote("von Peter Panzwischen Kunden DE11111111111111111111"))));
    }


    @Test
    public void testGiroKontoauszug03()
    {
        SolarisbankAGPDFExtractor extractor = new SolarisbankAGPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("GiroKontoauszug03.txt"), errors);

        assertThat(errors, empty());

        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        assertThat(results, hasItem(removal(hasDate("2022-10-19"), hasAmount("EUR", 11.99), //
                        hasNote("11,99 EUR"))));
    }

    @Test
    public void testGiroKontoauszug04()
    {
        SolarisbankAGPDFExtractor extractor = new SolarisbankAGPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(loadFile("GiroKontoauszug04.txt"), errors);
        assertThat(errors, empty());

        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        assertThat(results, hasItem(deposit(hasDate("2022-10-03"), hasAmount("EUR", 200), //
                        hasNote("von Peter Pan"))));

        assertThat(results, hasItem(deposit(hasDate("2022-10-06"), hasAmount("EUR", 0.40), //
                        hasNote("Visa Geld zurueck AktionVISACLP0324 GB"))));

        assertThat(results, hasItem(removal(hasDate("2022-10-20"), hasAmount("EUR", 18.78), //
                        hasNote("an Peter Pan"))));

    }

    private List<InputFile> loadFile(String filename)
    {
        return PDFInputFile.loadTestCase(getClass(), filename);
    }
}
