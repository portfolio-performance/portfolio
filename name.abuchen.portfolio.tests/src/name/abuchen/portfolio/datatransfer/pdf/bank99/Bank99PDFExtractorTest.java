package name.abuchen.portfolio.datatransfer.pdf.bank99;

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
import name.abuchen.portfolio.datatransfer.pdf.Bank99PDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class Bank99PDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new Bank99PDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2026-03-19"), hasAmount("EUR", 38852.86), //
                        hasSource("Kontoauszug01.txt"), hasNote("IBAN: gI33 3931 7920 0382 3470"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-03-19"), hasAmount("EUR", 0.22), //
                        hasSource("Kontoauszug01.txt"), hasNote("IBAN: FT32 7047 0282 7196 6611"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-03-27"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("IBAN: De58 1818 7938 4975 3154"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-03-31"), hasAmount("EUR", 1.88), //
                        hasSource("Kontoauszug01.txt"), hasNote("IBAN: PQ86 2943 6421 5841 0841"))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new Bank99PDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(16L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(16));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-04-01"), hasAmount("EUR", 5.99), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: Yw22 4620 0382 4590 3965"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-04-09"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: Wv63 5616 6408 8648 6313"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-04-10"), hasAmount("EUR", 107.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: bl15 3563 4136 6347 1311"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-04-22"), hasAmount("EUR", 369.22), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: WC81 2631 9649 6365 2503"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-04-27"), hasAmount("EUR", 2000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: aR91 0168 5918 0204 4893"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-04-30"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: lr21 1250 3679 7172 4289"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-04"), hasAmount("EUR", 2.57), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: jW59 3186 9843 7558 2609"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-05-15"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: ja75 8417 0823 1460 4656"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-22"), hasAmount("EUR", 5000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: KB29 0203 4593 1148 2975"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-28"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: BJ10 1038 6035 3085 6043"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-06-01"), hasAmount("EUR", 2.94), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: ff89 0700 6798 1176 4921"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-06-10"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: Wb74 9458 7850 2367 2501"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2026-06-19"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 155.65), hasGrossValue("EUR", (206.94 + 0.60)), //
                        hasTaxes("EUR", 51.89), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-06-19"), hasAmount("EUR", 0.30), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: sZ25 4000 8887 9468 8755"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2026-06-22"), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.40), hasGrossValue("EUR", (0.04 + 1.83)), //
                        hasTaxes("EUR", 0.47), hasFees("EUR", 0.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2026-06-22"), hasAmount("EUR", 44600.03), //
                        hasSource("Kontoauszug02.txt"), hasNote("IBAN: cT30 6127 3622 3309 0015"))));
    }
}
