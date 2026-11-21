package name.abuchen.portfolio.datatransfer.pdf.wuestenrotbankag;

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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.WuestenrotBankAGPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class WuestenrotBankAGPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new WuestenrotBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2026-08-05"), hasAmount("EUR", 40098.19), //
                        hasSource("Kontoauszug01.txt"), hasNote("Eingehende Echtzeitzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-08-07"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-08-13"), hasAmount("EUR", 0.30), //
                        hasSource("Kontoauszug01.txt"), hasNote("Eingehende Echtzeitzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-08-13"), hasAmount("EUR", 0.01), //
                        hasSource("Kontoauszug01.txt"), hasNote("Eingehende Echtzeitzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-08-17"), hasAmount("EUR", 0.50), //
                        hasSource("Kontoauszug01.txt"), hasNote("Eingehende Echtzeitzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-08-18"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Eingehende Echtzeitzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-08-26"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Eingehende Echtzeitzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-08-27"), hasAmount("EUR", 4.20), //
                        hasSource("Kontoauszug01.txt"), hasNote("Eingehende Echtzeitzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-08-28"), hasAmount("EUR", 13.91), //
                        hasSource("Kontoauszug01.txt"), hasNote("Eingehende Echtzeitzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-08-31"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2026-08-31"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Habenzinsen"), //
                        hasAmount("EUR", 0.21), hasGrossValue("EUR", 0.28), //
                        hasTaxes("EUR", 0.07), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2026-08-31"), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote("Bonuszinsen"), //
                        hasAmount("EUR", 53.24), hasGrossValue("EUR", 70.99), //
                        hasTaxes("EUR", 17.75), hasFees("EUR", 0.00))));
    }
}
