package name.abuchen.portfolio.datatransfer.pdf.withebankgmbh;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
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
import name.abuchen.portfolio.datatransfer.pdf.WitheBoxGmbHPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class WitheBoxGmbHPDFExtractorTest
{
    @Test
    public void testGebuehrenabrechnung01()
    {
        WitheBoxGmbHPDFExtractor extractor = new WitheBoxGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Gebuehrenabrechnung01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-10-31"), hasAmount("EUR", 0.07), //
                        hasSource("Gebuehrenabrechnung01.txt"), hasNote(null))));
    }

    @Test
    public void testGebuehrenabrechnung02()
    {
        WitheBoxGmbHPDFExtractor extractor = new WitheBoxGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Gebuehrenabrechnung02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-08-31"), hasAmount("EUR", 0.32), //
                        hasSource("Gebuehrenabrechnung02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-08-31"), hasAmount("EUR", 0.11), //
                        hasSource("Gebuehrenabrechnung02.txt"), hasNote(null))));
    }

    @Test
    public void testGebuehrenabrechnung03()
    {
        WitheBoxGmbHPDFExtractor extractor = new WitheBoxGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Gebuehrenabrechnung03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2024-09-30"), hasAmount("EUR", 12.17), //
                        hasSource("Gebuehrenabrechnung03.txt"), hasNote(null))));
    }
}
