package name.abuchen.portfolio.datatransfer.pdf.bundesschatz;

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
import name.abuchen.portfolio.datatransfer.pdf.BundesschatzPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class BundesschatzPDFExtractorTest
{
    @Test
    public void testEinUndAuszahlungen01()
    {
        var extractor = new BundesschatzPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EinUndAuszahlungen01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(deposit(hasDate("2025-12-30"), hasAmount("EUR", 5000.00), //
                        hasSource("EinUndAuszahlungen01.txt"), hasNote("AT745170342310484364"))));

        assertThat(results, hasItem(removal(hasDate("2026-01-12"), hasAmount("EUR", 2531.91), //
                        hasSource("EinUndAuszahlungen01.txt"), hasNote("AT811215280026188132"))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new BundesschatzPDFExtractor(new Client());

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

        assertThat(results, hasItem(interest( //
                        hasDate("2025-12-10"), hasShares(0), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("10.11.2025 - 10.12.2025 (1 Monat)"), //
                        hasAmount("EUR", 2.78), hasGrossValue("EUR", 3.84), //
                        hasTaxes("EUR", 1.06), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(interest( //
                        hasDate("2026-01-12"), hasShares(0), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("10.12.2025 - 12.01.2026 (1 Monat)"), //
                        hasAmount("EUR", 3.07), hasGrossValue("EUR", 4.23), //
                        hasTaxes("EUR", 1.16), hasFees("EUR", 0.00))));
    }

}
