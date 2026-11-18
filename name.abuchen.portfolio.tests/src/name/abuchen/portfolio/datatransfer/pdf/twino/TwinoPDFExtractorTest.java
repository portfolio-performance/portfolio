package name.abuchen.portfolio.datatransfer.pdf.twino;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.TwinoPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class TwinoPDFExtractorTest
{
    @Test
    public void testIncomeStatement01()
    {
        var extractor = new TwinoPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "IncomeStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(4L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check skipped item for withdrawal (zero)
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2026-09-30"), hasAmount("EUR", 0.00), //
                                        hasSource("IncomeStatement01.txt"), hasNote(null)))));

        // check skipped item for inactivity fee (zero)
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        fee( //
                                        hasDate("2026-09-30"), //
                                        hasSource("IncomeStatement01.txt"), //
                                        hasNote("Inactivity fee"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // assert deposit
        assertThat(results, hasItem(deposit(hasDate("2026-09-30"), hasAmount("EUR", 2000.00),
                        hasSource("IncomeStatement01.txt"), hasNote(null))));

        // assert loan securities income
        assertThat(results, hasItem(interest( //
                        hasDate("2026-09-30"), //
                        hasSource("IncomeStatement01.txt"), //
                        hasNote("Loan Securities"), //
                        hasAmount("EUR", 0.85), hasGrossValue("EUR", 0.90), //
                        hasTaxes("EUR", 0.05), hasFees("EUR", 0.00))));

        // check skipped item for claim rights income (zero)
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        interest( //
                                        hasDate("2026-09-30"), //
                                        hasSource("IncomeStatement01.txt"), //
                                        hasNote("claim rights"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check skipped item for real estate securities income (zero)
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        interest( //
                                        hasDate("2026-09-30"), //
                                        hasSource("IncomeStatement01.txt"), //
                                        hasNote("Real estate Securities"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // assert campaign income
        assertThat(results, hasItem(interest( //
                        hasDate("2026-09-30"), //
                        hasSource("IncomeStatement01.txt"), //
                        hasNote("Income from campaigns (cashback, referral)"), //
                        hasAmount("EUR", 20.00), hasGrossValue("EUR", 20.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
