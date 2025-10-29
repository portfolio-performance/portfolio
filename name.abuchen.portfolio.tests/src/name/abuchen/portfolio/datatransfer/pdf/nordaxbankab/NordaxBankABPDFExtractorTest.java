package name.abuchen.portfolio.datatransfer.pdf.nordaxbankab;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundCash;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundCash;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
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
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2024-08-24"), hasAmount("EUR", 403.39), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));

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
}
