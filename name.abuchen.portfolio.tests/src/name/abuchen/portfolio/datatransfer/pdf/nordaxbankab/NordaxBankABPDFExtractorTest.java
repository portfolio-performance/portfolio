package name.abuchen.portfolio.datatransfer.pdf.nordaxbankab;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundCash;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundCash;
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
import name.abuchen.portfolio.datatransfer.pdf.NordaxBankABPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class NordaxBankABPDFExtractorTest
{
    @Test
    public void testAccountStatement01()
    {
        var extractor = new NordaxBankABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(2L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2024-08-24"), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 403.39), hasGrossValue("EUR", 403.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 1st cash transfer transaction
        assertThat(results, hasItem(outboundCash(hasDate("2024-08-24"), hasAmount("EUR", 403.39), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));
        assertThat(results, hasItem(inboundCash(hasDate("2024-08-24"), hasAmount("EUR", 403.39), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));

        // check 2nd cash transfer transaction
        assertThat(results, hasItem(outboundCash(hasDate("2024-08-24"), hasAmount("EUR", 10000.00), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));
        assertThat(results, hasItem(inboundCash(hasDate("2024-08-24"), hasAmount("EUR", 10000.00), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));
    }

    @Test
    public void testAccountStatement02()
    {
        var extractor = new NordaxBankABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-11-03"), hasAmount("EUR", 2000.00),
                        hasSource("AccountStatement02.txt"), hasNote("DEXXX 18633010 Übertrag Girokonto"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-06-17"), hasAmount("EUR", 2000.00),
                        hasSource("AccountStatement02.txt"), hasNote("DEXXX 17557781 Übertrag Tagesgeld"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2025-05-12"), hasAmount("EUR", 1000.00),
                        hasSource("AccountStatement02.txt"), hasNote("DEXXX 17290216 Übertrag Tagesgeld"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-12-10"), hasAmount("EUR", 12000.00),
                        hasSource("AccountStatement02.txt"), hasNote("DEXXX"))));

        // assert transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2025-01-01"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 24.40), hasGrossValue("EUR", 24.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testAccountStatement03()
    {
        var extractor = new NordaxBankABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(14L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(14));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-01-14"), hasAmount("EUR", 1020.00), //
                        hasSource("AccountStatement03.txt"), hasNote("DE92500617410200574015"))));

        assertThat(results, hasItem(deposit(hasDate("2025-02-14"), hasAmount("EUR", 1020.00), //
                        hasSource("AccountStatement03.txt"), hasNote("DE92500617410200574015"))));

        assertThat(results, hasItem(deposit(hasDate("2025-03-28"), hasAmount("EUR", 1020.00), //
                        hasSource("AccountStatement03.txt"), hasNote("DE92500617410200574015 xyz PKW Abschlag"))));

        assertThat(results, hasItem(deposit(hasDate("2025-04-30"), hasAmount("EUR", 1020.00), //
                        hasSource("AccountStatement03.txt"), hasNote("DE92500617410200574015 xyz PKW Abschlag"))));

        assertThat(results, hasItem(deposit(hasDate("2025-06-02"), hasAmount("EUR", 1020.00), //
                        hasSource("AccountStatement03.txt"), hasNote("DE92500617410200574015 xyz PKW Abschlag"))));

        assertThat(results, hasItem(deposit(hasDate("2025-06-30"), hasAmount("EUR", 1020.00), //
                        hasSource("AccountStatement03.txt"), hasNote("DE92500617410200574015 xyz PKW Abschlag"))));

        assertThat(results, hasItem(deposit(hasDate("2025-07-31"), hasAmount("EUR", 1020.00), //
                        hasSource("AccountStatement03.txt"), hasNote("DE92500617410200574015 xyz PKW Abschlag"))));

        assertThat(results, hasItem(deposit(hasDate("2025-09-01"), hasAmount("EUR", 1020.00), //
                        hasSource("AccountStatement03.txt"), hasNote("DE92500617410200574015 xyz PKW Abschlag"))));

        assertThat(results, hasItem(deposit(hasDate("2025-09-30"), hasAmount("EUR", 1020.00), //
                        hasSource("AccountStatement03.txt"), hasNote("DE92500617410200574015 xyz PKW Abschlag"))));

        assertThat(results, hasItem(deposit(hasDate("2025-10-31"), hasAmount("EUR", 1020.00), //
                        hasSource("AccountStatement03.txt"), hasNote("DE92500617410200574015 xyz PKW Abschlag"))));

        assertThat(results, hasItem(deposit(hasDate("2025-12-01"), hasAmount("EUR", 1020.00), //
                        hasSource("AccountStatement03.txt"), hasNote("DE22513900000075401501 xyz PKW Abschlag"))));

        assertThat(results, hasItem(removal(hasDate("2025-12-22"), hasAmount("EUR", 20.00), //
                        hasSource("AccountStatement03.txt"),
                        hasNote("DE92500617410200574015 19026490 Auszahlung BN 001"))));

        assertThat(results, hasItem(deposit(hasDate("2025-12-30"), hasAmount("EUR", 1020.00), //
                        hasSource("AccountStatement03.txt"), hasNote("DE22513900000075401501 xyz PKW Abschlag"))));

        assertThat(results, hasItem(interest(hasDate("2026-01-01"), hasAmount("EUR", 129.70), //
                        hasSource("AccountStatement03.txt"), hasNote(null))));
    }

    @Test
    public void testAccountStatement04()
    {
        var extractor = new NordaxBankABPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement04.txt"), errors);

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
        assertThat(results, hasItem(deposit(hasDate("2025-12-11"), hasAmount("EUR", 24500), //
                        hasSource("AccountStatement04.txt"),
                        hasNote("DE22513900000075401501 Einzahlung 01 BankNorwegian"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2026-01-01"), hasAmount("EUR", 34.77), //
                        hasSource("AccountStatement04.txt"), hasNote(null))));

    }
}
