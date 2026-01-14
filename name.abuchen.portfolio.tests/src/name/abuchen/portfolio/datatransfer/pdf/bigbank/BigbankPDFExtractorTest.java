package name.abuchen.portfolio.datatransfer.pdf.bigbank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSkippedItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.BigbankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class BigbankPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new BigbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2024-03-20"), hasAmount("EUR", 10.12), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-03-21"), hasAmount("EUR", 1500.34), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-03-25"), hasAmount("EUR", 10.12), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new BigbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2024-03-20"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-03-21"), hasAmount("EUR", 10500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-03-25"), hasAmount("EUR", 10.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-03-28"), hasAmount("EUR", 3500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new BigbankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

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
        assertThat(results, hasItem(interest(hasDate("2024-12-31"), hasAmount("EUR", 99.01), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));
    }
}
