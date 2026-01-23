package name.abuchen.portfolio.datatransfer.pdf.c24bankgmbh;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
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
import name.abuchen.portfolio.datatransfer.pdf.C24BankGmbHPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class C24BankGmbHPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

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
        assertThat(results, hasItem(removal(hasDate("2024-05-17"), hasAmount("EUR", 1508.42), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-17"), hasAmount("EUR", 1115.22), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

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
                        hasDate("2024-05-31"), hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Zinsen"), //
                        hasAmount("EUR", 1.93), hasGrossValue("EUR", 4.22), //
                        hasTaxes("EUR", 2.29), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-05-17"), hasAmount("EUR", 1460.11), //
                        hasSource("Kontoauszug02.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

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
                        hasDate("2024-06-30"), hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Zinsen"), //
                        hasAmount("EUR", 15.32), hasGrossValue("EUR", 19.36), //
                        hasTaxes("EUR", 4.04), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

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
                        hasDate("2024-07-31"), hasShares(0), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote("Zinsen"), //
                        hasAmount("EUR", 15.86), hasGrossValue("EUR", 20.04), //
                        hasTaxes("EUR", 4.18), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

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
        assertThat(results, hasItem(removal(hasDate("2024-08-05"), hasAmount("EUR", 2800.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Echtzeitüberweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-08-05"), hasAmount("EUR", 2800.00), //
                        hasSource("Kontoauszug05.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(12));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-31"), hasAmount("EUR", 3800.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-30"), hasAmount("EUR", 1055.70), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-29"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-27"), hasAmount("EUR", 400.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-27"), hasAmount("EUR", 200.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-20"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-20"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-20"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug06.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-01-19"), hasAmount("EUR", 1.20), //
                        hasSource("Kontoauszug06.txt"), hasNote("Echtzeitüberweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-16"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-14"), hasAmount("EUR", 6000.00), //
                        hasSource("Kontoauszug06.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testKontoauszug07()
    {
        var extractor = new C24BankGmbHPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(17L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(17));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-31"), hasAmount("EUR", 3753.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-30"), hasAmount("EUR", 2253.11), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-27"), hasAmount("EUR", 728.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-27"), hasAmount("EUR", 649.00), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-15"), hasAmount("EUR", 68.97), //
                        hasSource("Kontoauszug07.txt"), hasNote("Echtzeitüberweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-14"), hasAmount("EUR", 6.35), //
                        hasSource("Kontoauszug07.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-11"), hasAmount("EUR", 0.30), //
                        hasSource("Kontoauszug07.txt"), hasNote("Echtzeitüberweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-11"), hasAmount("EUR", 6.93), //
                        hasSource("Kontoauszug07.txt"), hasNote("MoneySend Zahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-06"), hasAmount("EUR", 407.44), //
                        hasSource("Kontoauszug07.txt"), hasNote("Echtzeitüberweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-06"), hasAmount("EUR", 670.67), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-04"), hasAmount("EUR", 32.51), //
                        hasSource("Kontoauszug07.txt"), hasNote("Online-Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-03"), hasAmount("EUR", 4286.49), //
                        hasSource("Kontoauszug07.txt"), hasNote("Echtzeitüberweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-10-03"), hasAmount("EUR", 575.42), //
                        hasSource("Kontoauszug07.txt"), hasNote("Online-Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-03"), hasAmount("EUR", 9444.60), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-02"), hasAmount("EUR", 19.89), //
                        hasSource("Kontoauszug07.txt"), hasNote("Überweisung"))));
    }
}
