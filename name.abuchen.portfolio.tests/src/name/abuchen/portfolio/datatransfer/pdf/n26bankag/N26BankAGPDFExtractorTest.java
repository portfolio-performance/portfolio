package name.abuchen.portfolio.datatransfer.pdf.n26bankag;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
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
import name.abuchen.portfolio.datatransfer.pdf.N26BankAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class N26BankAGPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new N26BankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-06-01T00:00"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 185.66), hasGrossValue("EUR", 252.16), //
                        hasTaxes("EUR", 63.04 + 3.46), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-06-19"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new N26BankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

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
        assertThat(results, hasItem(interest( //
                        hasDate("2024-07-01T00:00"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 196.67), hasGrossValue("EUR", 267.12), //
                        hasTaxes("EUR", 66.78 + 3.67), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-07-02"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-07-02"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new N26BankAGPDFExtractor(new Client());

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
        assertThat(results, hasItem(interest( //
                        hasDate("2023-12-01T00:00"), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.42), hasGrossValue("EUR", 0.56), //
                        hasTaxes("EUR", 0.14), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new N26BankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

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
        assertThat(results, hasItem(interest( //
                        hasDate("2025-03-01T00:00"), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 43.08), hasGrossValue("EUR", 58.51), //
                        hasTaxes("EUR", 14.63 + 0.80), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new N26BankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-04-13"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug05.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-04-01T00:00"), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 11.06), hasGrossValue("EUR", 15.01), //
                        hasTaxes("EUR", 3.75 + 0.20), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new N26BankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2025-01-11"), hasAmount("EUR", 3000.00), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-13"), hasAmount("EUR", 4500.00), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-20"), hasAmount("EUR", 3500.00), //
                        hasSource("Kontoauszug06.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-01-01T00:00"), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.55), hasGrossValue("EUR", 8.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug07()
    {
        var extractor = new N26BankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

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
        assertThat(results, hasItem(removal(hasDate("2025-05-04"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-05-12"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-05-26"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-05-27"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-05-27"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug07.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-05-01T00:00"), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 6.60), hasGrossValue("EUR", 6.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
