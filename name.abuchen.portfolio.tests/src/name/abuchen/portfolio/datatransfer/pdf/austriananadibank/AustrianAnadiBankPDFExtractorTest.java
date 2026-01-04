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
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-05-14"), hasShares(0), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2500.00), hasGrossValue("EUR", 2500.00), //
                        hasTaxes("EUR", (0.00)), hasFees("EUR", 0.00))));
        assertThat(results, hasItem(removal( //
                        hasDate("2025-05-15"), hasShares(0), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("ONLINE-FESTGELD / KONTO 12345555555"), //
                        hasAmount("EUR", 2500.00), hasGrossValue("EUR", 2500.00), //
                        hasTaxes("EUR", (0.00)), hasFees("EUR", 0.00))));
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-05-19"), hasShares(0), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8320.00), hasGrossValue("EUR", 8320.00), //
                        hasTaxes("EUR", (0.00)), hasFees("EUR", 0.00))));
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
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-12-09"), hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3500.00), hasGrossValue("EUR", 3500.00), //
                        hasTaxes("EUR", (0.00)), hasFees("EUR", 0.00))));
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-12-11"), hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 500.00), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", (0.00)), hasFees("EUR", 0.00))));
        assertThat(results, hasItem(removal( //
                        hasDate("2025-12-16"), hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2000.00), hasGrossValue("EUR", 2000.00), //
                        hasTaxes("EUR", (0.00)), hasFees("EUR", 0.00))));
        assertThat(results, hasItem(interest( //
                        hasDate("2025-12-31"), hasShares(0), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("Habenzinsen"), //
                        hasAmount("EUR", 123.91), hasGrossValue("EUR", 165.21), //
                        hasTaxes("EUR", (41.30)), hasFees("EUR", 0.00))));
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
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2025-11-04"), hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 500.00), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", (0.00)), hasFees("EUR", 0.00))));
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-11-05"), hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 500.00), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", (0.00)), hasFees("EUR", 0.00))));
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-11-11"), hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 500.00), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", (0.00)), hasFees("EUR", 0.00))));
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-11-17"), hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("ABRECHNUNG ZU KONTO 00123456678"), //
                        hasAmount("EUR", 2500.00), hasGrossValue("EUR", 2500.00), //
                        hasTaxes("EUR", (0.00)), hasFees("EUR", 0.00))));
        assertThat(results, hasItem(interest( //
                        hasDate("2025-11-17"), hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote("Habenzinsen ABRECHNUNG ZU KONTO 00123456678"), //
                        hasAmount("EUR", 22.75), hasGrossValue("EUR", 30.33), //
                        hasTaxes("EUR", (7.58)), hasFees("EUR", 0.00))));
        assertThat(results, hasItem(removal( //
                        hasDate("2025-11-24"), hasShares(0), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2784.00), hasGrossValue("EUR", 2784.00), //
                        hasTaxes("EUR", (0.00)), hasFees("EUR", 0.00))));
    }
}
