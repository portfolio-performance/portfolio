package name.abuchen.portfolio.datatransfer.pdf.debitum;

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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.pdf.DebitumPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

public class DebitumPDFExtractorTest
{

    @Test
    public void testAccountStatement01()
    {
        var extractor = new DebitumPDFExtractor(new Client());
        var errors = new ArrayList<Exception>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(3));

        assertThat(results, hasItem(deposit(hasDate("2025-11-30"), hasAmount("EUR", 4170.00),
                        hasSource("AccountStatement01.txt"), hasNote(null))));

        assertThat(results, hasItem(removal(hasDate("2025-11-30"), hasAmount("EUR", 2.00),
                        hasSource("AccountStatement01.txt"), hasNote(null))));

        assertThat(results, hasItem(interest(hasDate("2025-11-30"), hasAmount("EUR", 26.14 + 1.5 - 1.38),
                        hasSource("AccountStatement01.txt"), hasNote(null))));

    }

    @Test
    public void testAccountStatement02()
    {
        var extractor = new DebitumPDFExtractor(new Client());
        var errors = new ArrayList<Exception>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(1));

        assertThat(results, hasItem(interest(hasDate("2025-12-17"), hasAmount("EUR", 1.83 - 0.09),
                        hasSource("AccountStatement02.txt"), hasNote(null))));

    }

}
