package name.abuchen.portfolio.datatransfer.pdf.neon;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
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
import name.abuchen.portfolio.datatransfer.pdf.NEON3aPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class NEON3aPDFExtractorTest
{
    @Test
    public void testBuy01()
    {
        var extractor = new NEON3aPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "CHF");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0394628066"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Inv.II Mo.Re.Opp"))));

        // check 1st buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-24"), //
                        hasShares(0.102347), //
                        hasSource("Buy01.txt"), //
                        hasAmount("CHF", 10.00), //
                        hasGrossValue("CHF", 10.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0215804714"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Ind EqLar Cap CH"))));

        // check 2nd buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-24"), //
                        hasShares(2.657624), //
                        hasSource("Buy01.txt"), //
                        hasAmount("CHF", 480.00), //
                        hasGrossValue("CHF", 480.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 3rd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0132501898"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Eq S&Mid Caps CH"))));

        // check 3rd buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-24"), //
                        hasShares(0.351181), //
                        hasSource("Buy01.txt"), //
                        hasAmount("CHF", 120.00), //
                        hasGrossValue("CHF", 120.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));
    }

    @Test
    public void testBuy02()
    {
        var extractor = new NEON3aPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "CHF");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0117044971"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc F. V Eq Emer. M."))));

        // check 1st buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-25"), //
                        hasShares(0.580442), //
                        hasSource("Buy02.txt"), //
                        hasAmount("CHF", 80.00), //
                        hasGrossValue("CHF", 80.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0117044906"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Ind Eq Wo.ex.CH"))));

        // check 2nd buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-25"), //
                        hasShares(0.932137), //
                        hasSource("Buy02.txt"), //
                        hasAmount("CHF", 310.00), //
                        hasGrossValue("CHF", 310.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));
    }

    @Test
    public void testBuy03()
    {
        var extractor = new NEON3aPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "CHF");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0117044906"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Ind Eq Wo.ex.CH"))));

        // check 1st buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-16"), //
                        hasShares(5.552571), //
                        hasSource("Buy03.txt"), //
                        hasAmount("CHF", 1939.98), //
                        hasGrossValue("CHF", 1939.98), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0117044971"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc F. V Eq Emer. M."))));

        // check 2nd buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-16"), //
                        hasShares(3.308542), //
                        hasSource("Buy03.txt"), //
                        hasAmount("CHF", 500.64), //
                        hasGrossValue("CHF", 500.64), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check deposit
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-01-16"), //
                        hasSource("Buy03.txt"), //
                        hasAmount("CHF", 1000.00))));
    }

    @Test
    public void testBuy04()
    {
        var extractor = new NEON3aPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "CHF");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0394628066"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Inv.II Mo.Re.Opp"))));

        // check 1st buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-26"), //
                        hasShares(0.10248), //
                        hasSource("Buy04.txt"), //
                        hasAmount("CHF", 10.00), //
                        hasGrossValue("CHF", 10.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0215804714"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Ind EqLar Cap CH"))));

        // check 2nd buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-26"), //
                        hasShares(2.519818), //
                        hasSource("Buy04.txt"), //
                        hasAmount("CHF", 480.00), //
                        hasGrossValue("CHF", 480.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 3rd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0132501898"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Eq S&Mid Caps CH"))));

        // check 3rd buy
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-26"), //
                        hasShares(0.323213), //
                        hasSource("Buy04.txt"), //
                        hasAmount("CHF", 120.00), //
                        hasGrossValue("CHF", 120.00), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));
    }

    @Test
    public void testSell01()
    {
        var extractor = new NEON3aPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "CHF");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0117044906"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Ind Eq Wo.ex.CH"))));

        // check 1st sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.000439), //
                        hasSource("Sell01.txt"), //
                        hasAmount("CHF", 0.15), //
                        hasGrossValue("CHF", 0.15), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 2nd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0117044971"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc F. V Eq Emer. M."))));

        // check 2nd sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.000213), //
                        hasSource("Sell01.txt"), //
                        hasAmount("CHF", 0.03), //
                        hasGrossValue("CHF", 0.03), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 3rd security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0132501898"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Eq S&Mid Caps CH"))));

        // check 3rd sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.000172), //
                        hasSource("Sell01.txt"), //
                        hasAmount("CHF", 0.06), //
                        hasGrossValue("CHF", 0.06), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 4th security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0215804714"), //
                        hasCurrencyCode("CHF"), //
                        hasName("Sc Ind EqLar Cap CH"))));

        // check 4th sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.001239), //
                        hasSource("Sell01.txt"), //
                        hasAmount("CHF", 0.23), //
                        hasGrossValue("CHF", 0.23), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));
    }

    @Test
    public void testSell02()
    {
        var extractor = new NEON3aPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "CHF");

        // check 1st sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.000439), //
                        hasSource("Sell02.txt"), //
                        hasAmount("CHF", 0.15), //
                        hasGrossValue("CHF", 0.15), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 2nd sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.000213), //
                        hasSource("Sell02.txt"), //
                        hasAmount("CHF", 0.03), //
                        hasGrossValue("CHF", 0.03), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 3rd sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.000172), //
                        hasSource("Sell02.txt"), //
                        hasAmount("CHF", 0.06), //
                        hasGrossValue("CHF", 0.06), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));

        // check 4th sell
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), //
                        hasShares(0.001239), //
                        hasSource("Sell02.txt"), //
                        hasAmount("CHF", 0.23), //
                        hasGrossValue("CHF", 0.23), //
                        hasTaxes("CHF", 0.00), //
                        hasFees("CHF", 0.00))));
    }

    @Test
    public void testDeposit01()
    {
        var extractor = new NEON3aPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Deposit1.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check deposit
        assertThat(results, hasItem(deposit( //
                        hasDate("2025-11-18"), //
                        hasSource("Deposit1.txt"), //
                        hasAmount("CHF", 1000.00))));
    }

    @Test
    public void testDeposit02()
    {
        var extractor = new NEON3aPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Deposit02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check deposit
        assertThat(results, hasItem(deposit( //
                        hasDate("2026-01-08"), //
                        hasSource("Deposit02.txt"), //
                        hasAmount("CHF", 6258.00))));
    }

    @Test
    public void testFees01()
    {
        var extractor = new NEON3aPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fees01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // check management fee
        assertThat(results, hasItem(fee( //
                        hasDate("2025-12-03"), //
                        hasSource("Fees01.txt"), //
                        hasNote("Assessment from 24.11.25 - 31.12.25"), //
                        hasAmount("CHF", 0.47))));
    }
}
