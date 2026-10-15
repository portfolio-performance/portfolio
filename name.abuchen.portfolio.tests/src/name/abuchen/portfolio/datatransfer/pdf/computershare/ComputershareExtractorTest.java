package name.abuchen.portfolio.datatransfer.pdf.computershare;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
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
import name.abuchen.portfolio.datatransfer.pdf.ComputersharePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class ComputershareExtractorTest
{
    @Test
    public void testDepotAuszugWithPurchase01()
    {
        final var extractor = new ComputersharePDFExtractor(new Client());

        final List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(9L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("023135106"), hasTicker("AMZN"), //
                        hasName(null), //
                        hasCurrencyCode("USD"))));

        // check 1st buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-01-31T00:00"), hasShares(7.50), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 756.00), hasGrossValue("USD", 750.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 6.00))));

        // check 2nd buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-02-28T00:00"), hasShares(7.50), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 750.00), hasGrossValue("USD", 750.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 3rd buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-03-31T00:00"), hasShares(7.50), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 750.00), hasGrossValue("USD", 750.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 4th buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-04-28T00:00"), hasShares(7.50), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 750.00), hasGrossValue("USD", 750.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 5th buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-05-31T00:00"), hasShares(7.50), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 750.00), hasGrossValue("USD", 750.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 6th buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-06-30T00:00"), hasShares(7.50), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 750.00), hasGrossValue("USD", 750.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 7th buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-07-31T00:00"), hasShares(7.50), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 756.00), hasGrossValue("USD", 750.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 6.00))));

        // check 8th buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-08-31T00:00"), hasShares(7.50), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 750.00), hasGrossValue("USD", 750.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check 9th buy transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-09-29T00:00"), hasShares(7.50), //
                        hasSource("Depotauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 750.00), hasGrossValue("USD", 750.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }
}
