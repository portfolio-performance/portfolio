package name.abuchen.portfolio.datatransfer.pdf.limetradingcorp;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
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
import name.abuchen.portfolio.datatransfer.pdf.LimeTradingCorpPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class LimeTradingCorpPDFExtractorTest
{
    @Test
    public void testAccountStatement01()
    {
        var extractor = new LimeTradingCorpPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("067901108"), hasTicker(null), //
                        hasName("Barrick Gold Corp Com"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("78463V107"), hasTicker(null), //
                        hasName("Spdr Gold Trust Gold Shs"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("922908363"), hasTicker(null), //
                        hasName("Vanguard Index Fds S P 500 Etf Shs"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("756109104"), hasTicker(null), //
                        hasName("Realty Income C"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("922042742"), hasTicker(null), //
                        hasName("Vanguard Intl E"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-03-11T00:00"), hasShares(20.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 481.79), hasGrossValue("USD", 481.79), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-03-11T00:00"), hasShares(42.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 7811.96), hasGrossValue("USD", 7811.96), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-11T00:00"), hasShares(23.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 9039.00), hasGrossValue("USD", 9039.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-03-16T00:00"), hasShares(21.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 8158.50), hasGrossValue("USD", 8158.50), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-17T00:00"), hasExDate(null), hasShares(20.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 1.50), hasGrossValue("USD", 2.00), //
                        hasTaxes("USD", 0.50), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-15T00:00"), hasExDate(null), hasShares(24.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 4.14), hasGrossValue("USD", 5.92), //
                        hasTaxes("USD", 1.78), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-24T00:00"), hasExDate(null), hasShares(12.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 2.16), hasGrossValue("USD", 3.09), //
                        hasTaxes("USD", 0.93), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2022-03-29T00:00"), hasExDate(null), hasShares(105.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 100.97), hasGrossValue("USD", 144.24), //
                        hasTaxes("USD", 43.27), hasFees("USD", 0.00))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2022-03-01T00:00"), hasAmount("USD", 9000.00), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2022-03-31T00:00"), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 0.19), hasGrossValue("USD", 0.19), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testAccountStatement02()
    {
        var extractor = new LimeTradingCorpPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("756109104"), hasTicker(null), //
                        hasName("Realty Income C"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("29273V100"), hasTicker(null), //
                        hasName("Energy Transfer"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("12485U102"), hasTicker(null), //
                        hasName("Cboe Oil Index-reduced Value"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("922042742"), hasTicker(null), //
                        hasName("Vanguard Intl Equity Index Fd"), //
                        hasCurrencyCode("USD"))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-08-15T00:00"), hasExDate(null), hasShares(24.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 5.21), hasGrossValue("USD", 6.13), //
                        hasTaxes("USD", 0.92), hasFees("USD", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-08-21T00:00"), hasExDate(null), hasShares(200.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 39.06), hasGrossValue("USD", 62.00), //
                        hasTaxes("USD", 22.94), hasFees("USD", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-08-31T00:00"), hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Withholding Adjustment"), //
                        hasAmount("USD", 23.43), hasGrossValue("USD", 23.43), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-08-31T00:00"), hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Withholding Adjustment"), //
                        hasAmount("USD", 9.15), hasGrossValue("USD", 9.15), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-08-31T00:00"), hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Withholding Adjustment"), //
                        hasAmount("USD", 1.79), hasGrossValue("USD", 1.79), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-08-31T00:00"), hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Withholding Adjustment"), //
                        hasAmount("USD", 0.92), hasGrossValue("USD", 0.92), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check taxes transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-08-31T00:00"), hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Withholding Adjustment"), //
                        hasAmount("USD", 0.52), hasGrossValue("USD", 0.52), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }
}
