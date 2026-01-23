package name.abuchen.portfolio.datatransfer.pdf.modenaestonia;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
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

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.ModenaEstoniaPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class ModenaEstoniaPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new ModenaEstoniaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

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
        assertThat(results, hasItem(interest(hasDate("2025-03-13T21:39:01"), hasAmount("EUR", 12.56), //
                        hasSource("Kontoauszug01.txt"), hasNote("Vault revenue share"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-03-14T07:00:00"), hasAmount("EUR", 1.42), //
                        hasSource("Kontoauszug01.txt"), hasNote("Vault signup bonus"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-03-21T21:59:09"), hasAmount("EUR", 50.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Vault campaign bonus"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-03-22T07:00:02"), hasAmount("EUR", 1.48), //
                        hasSource("Kontoauszug01.txt"), hasNote("Vault signup bonus"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2025-03-22T09:00:04"), hasAmount("EUR", 1.23), //
                        hasSource("Kontoauszug01.txt"), hasNote("Vault revenue share"))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new ModenaEstoniaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

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
        assertThat(results, hasItem(interestCharge(hasDate("2025-04-17T07:00:16"), hasAmount("EUR", 0.63), //
                        hasSource("Kontoauszug02.txt"), hasNote("Debt claims buyback"))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new ModenaEstoniaPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2025-05-01T08:00:10"), hasAmount("EUR", 0.61), //
                        hasSource("Kontoauszug03.txt"), hasNote("Vault accrued revenue"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interestCharge( //
                                        hasDate("2025-05-12T07:30:56"), //
                                        hasSource("Kontoauszug03.txt"), //
                                        hasNote("Debt claims buyback"), //
                                        hasAmount("EUR", 0.00)))));
    }
}
