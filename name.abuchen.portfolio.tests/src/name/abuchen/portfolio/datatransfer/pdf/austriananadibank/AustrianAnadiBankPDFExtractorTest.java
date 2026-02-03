package name.abuchen.portfolio.datatransfer.pdf.austriananadibank;

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
import name.abuchen.portfolio.datatransfer.pdf.AustrianAnadiBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class AustrianAnadiBankPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new AustrianAnadiBankPDFExtractor(new Client());

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
        assertThat(results, hasItem(deposit(hasDate("2025-05-14"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("IBAN: TT40 1705 1256 3324 8502"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-05-15"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("ONLINE-FESTGELD / KONTO 12345555555"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-05-19"), hasAmount("EUR", 8320.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("IBAN: ur98 0202 5368 2910 8017"))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new AustrianAnadiBankPDFExtractor(new Client());

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
        assertThat(results, hasItem(deposit(hasDate("2025-12-09"), hasAmount("EUR", 3500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: AT12 1234 1234 1234 1234"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-12-11"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: AT12 1234 1234 1234 1234"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-12-16"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: AT12 1234 1234 1234 1234"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-12-31"), hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Abschluss 31.12.2025"), //
                        hasAmount("EUR", 123.91), hasGrossValue("EUR", 165.21), //
                        hasTaxes("EUR", 41.30), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new AustrianAnadiBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-11-04"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("IBAN: AT11 1234 1234 1234 1234"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-11-05"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("IBAN: AT11 1234 1234 1234 1234"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-11-11"), hasAmount("EUR", 500.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("IBAN: AT11 1234 1234 1234 1234"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-11-17"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("Abrechnung zu Konto: 00123456678"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-11-17"), hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Abrechnung zu Konto: 00123456678"), //
                        hasAmount("EUR", 22.75), hasGrossValue("EUR", 30.33), //
                        hasTaxes("EUR", 7.58), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-11-24"), hasAmount("EUR", 2784.00), //
                        hasSource("Kontoauszug03.txt"), hasNote("IBAN: AT11 1234 1234 1234 1234"))));
    }
}
