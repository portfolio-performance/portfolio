package name.abuchen.portfolio.datatransfer.pdf.ayvensbank;

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
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.AyvensBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class AyvensBankPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new AyvensBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check deposit
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-04-02"), hasAmount("EUR", 78683.12), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("dI53998518932366313094 von Adva gesamt inkl. April Zins"))));

        // check interest
        assertThat(results, hasItem(interest( //
                        hasDate("2026-04-01"), hasAmount("EUR", 123.26), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null))));

        // check deposit
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-03-31"), hasAmount("EUR", 190.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("BC60230308373922159806 R ex Kasse 190"))));

        // check deposit
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-03-31"), hasAmount("EUR", 588.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("rQ26236415973132438367 monatl. Haeufchen 588,-"))));

        // check removal
        assertThat(results, hasItem(removal( //
                        hasDate("2026-03-31"), hasAmount("EUR", 64283.94), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("8912317415"))));

        // check interest
        assertThat(results, hasItem(interest( //
                        hasDate("2026-03-01"), hasAmount("EUR", 122.98), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null))));

        // check interest
        assertThat(results, hasItem(interest( //
                        hasDate("2026-02-01"), hasAmount("EUR", 122.74), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new AyvensBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check interest
        assertThat(results, hasItem(interest( //
                        hasDate("2026-04-01"), hasAmount("EUR", 19.23), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote(null))));

        // check interest
        assertThat(results, hasItem(interest( //
                        hasDate("2026-03-01"), hasAmount("EUR", 19.20), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote(null))));

        // check interest
        assertThat(results, hasItem(interest( //
                        hasDate("2026-02-01"), hasAmount("EUR", 16.08), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote(null))));

        // check deposit
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-01-06"), hasAmount("EUR", 9999.00), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("ND21837570459852711862"))));

        // check deposit
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-01-02"), hasAmount("EUR", 1.00), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote("dH76100077742295069568 Initiale Uberweisung"))));
    }
}
