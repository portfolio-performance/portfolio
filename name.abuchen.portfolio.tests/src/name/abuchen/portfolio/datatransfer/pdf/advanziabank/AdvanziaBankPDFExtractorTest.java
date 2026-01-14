package name.abuchen.portfolio.datatransfer.pdf.advanziabank;

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
import name.abuchen.portfolio.datatransfer.pdf.AdvanziaBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class AdvanziaBankPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new AdvanziaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-14"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-06-15"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-16"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-20"), hasAmount("EUR", 9200.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-06-30"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-06-30"), hasAmount("EUR", 47.11), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new AdvanziaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-06"), hasAmount("EUR", 190.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-06"), hasAmount("EUR", 562.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-13"), hasAmount("EUR", 350.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-21"), hasAmount("EUR", 140.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-05-28"), hasAmount("EUR", 604.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-05-31"), hasAmount("EUR", 308.76), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new AdvanziaBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2025-07-04"), hasAmount("EUR", 190.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-07-04"), hasAmount("EUR", 562.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-07-28"), hasAmount("EUR", 340.00), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2025-07-31"), hasAmount("EUR", 123.66), //
                        hasSource("Kontoauszug03.txt"), hasNote(null))));
    }
}
