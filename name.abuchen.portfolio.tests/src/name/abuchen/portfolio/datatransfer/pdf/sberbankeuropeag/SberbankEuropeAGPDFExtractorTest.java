package name.abuchen.portfolio.datatransfer.pdf.sberbankeuropeag;

import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SberbankEuropeAGPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class SberbankEuropeAGPDFExtractorTest
{
    @Test
    public void testTagesgeldKontoauszug01()
    {
        SberbankEuropeAGPDFExtractor extractor = new SberbankEuropeAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TagesgeldKontoauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-07-01"), hasAmount("EUR", 123456.78), //
                        hasSource("TagesgeldKontoauszug01.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-07-07"), hasAmount("EUR", 12345.67), //
                        hasSource("TagesgeldKontoauszug01.txt"), hasNote("Überweisungsgutschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-07-30"), hasAmount("EUR", 1234.56), //
                        hasSource("TagesgeldKontoauszug01.txt"), hasNote("Überweisungsgutschrift"))));
    }
}
