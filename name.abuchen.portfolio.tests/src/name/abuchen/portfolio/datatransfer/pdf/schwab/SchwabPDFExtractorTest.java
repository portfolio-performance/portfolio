package name.abuchen.portfolio.datatransfer.pdf.schwab;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SchwabPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class SchwabPDFExtractorTest
{
    @Test
    public void testAccountStatement01()
    {
        var extractor = new SchwabPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(6L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(13));
        new AssertImportActions().check(results, "USD");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("NVDA"), //
                        hasName("NVIDIA CORP"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("NEM"), //
                        hasName("NEWMONT CORP"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XOM"), //
                        hasName("EXXON MOBIL CORP"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("SGOV"), //
                        hasName("ISHARES 0-3 MONTH TREASURY"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("NLCP"), //
                        hasName("NEWLAKE CAP PARTNERS INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("QABSY"), //
                        hasName("QANTAS AIRWAYS LTD"), //
                        hasCurrencyCode("USD"))));

        // check purchase transactions
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-04-01T00:00"), hasShares(0.0002), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 0.04), hasGrossValue("USD", 0.04), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-04-15T00:00"), hasShares(5.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 592.81), hasGrossValue("USD", 592.81), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-04-21T00:00"), hasShares(4.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 593.80), hasGrossValue("USD", 593.80), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2026-04-22T00:00"), hasShares(100.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 10058.50), hasGrossValue("USD", 10058.50), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividend transactions
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-04-01"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 0.04), hasGrossValue("USD", 0.04), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2026-04-15"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 3.44), hasGrossValue("USD", 3.44), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2026-04-27"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 1.31), hasGrossValue("USD", 1.41), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.10))));
    }
}
