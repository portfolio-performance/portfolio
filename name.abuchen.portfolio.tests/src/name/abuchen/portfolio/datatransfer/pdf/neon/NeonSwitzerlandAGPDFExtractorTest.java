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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
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
import name.abuchen.portfolio.datatransfer.pdf.NeonSwitzerlandAGPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class NeonSwitzerlandAGPDFExtractorTest
{
    @Test
    public void testSecurityBuy01()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "CHF");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("39462806"), hasTicker(null), //
                        hasName("Sc Inv.II Mo.Re.Opp"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("21580471"), hasTicker(null), //
                        hasName("Sc Ind EqLar Cap CH"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("13250189"), hasTicker(null), //
                        hasName("Sc Eq S&Mid Caps CH"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-24"), hasShares(0.102347), //
                        hasSource("Buy01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 10.00), hasGrossValue("CHF", 10.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-24"), hasShares(2.657624), //
                        hasSource("Buy01.txt"), //
                        hasAmount("CHF", 480.00), hasGrossValue("CHF", 480.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-24"), hasShares(0.351181), //
                        hasSource("Buy01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 120.00), hasGrossValue("CHF", 120.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSecurityBuy02()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "CHF");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("11704497"), hasTicker(null), //
                        hasName("Sc F. V Eq Emer. M."), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("11704490"), hasTicker(null), //
                        hasName("Sc Ind Eq Wo.ex.CH"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-25"), hasShares(0.580442), //
                        hasSource("Buy02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 80.00), hasGrossValue("CHF", 80.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-11-25"), hasShares(0.932137), //
                        hasSource("Buy02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 310.00), hasGrossValue("CHF", 310.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSecurityBuy03()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "CHF");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("11704490"), hasTicker(null), //
                        hasName("Sc Ind Eq Wo.ex.CH"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("11704497"), hasTicker(null), //
                        hasName("Sc F. V Eq Emer. M."), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-16"), hasShares(5.552571), //
                        hasSource("Buy03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1939.98), hasGrossValue("CHF", 1939.98), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-16"), hasShares(3.308542), //
                        hasSource("Buy03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 500.64), hasGrossValue("CHF", 500.64), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-16"), hasAmount("CHF", 1000.00), //
                        hasSource("Buy03.txt"), hasNote(null))));
    }

    @Test
    public void testSecurityBuy04()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "CHF");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("39462806"), hasTicker(null), //
                        hasName("Sc Inv.II Mo.Re.Opp"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("21580471"), hasTicker(null), //
                        hasName("Sc Ind EqLar Cap CH"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("13250189"), hasTicker(null), //
                        hasName("Sc Eq S&Mid Caps CH"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-26"), hasShares(0.10248), //
                        hasSource("Buy04.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 10.00), hasGrossValue("CHF", 10.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-26"), hasShares(2.519818), //
                        hasSource("Buy04.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 480.00), hasGrossValue("CHF", 480.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-01-26"), hasShares(0.323213), //
                        hasSource("Buy04.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 120.00), hasGrossValue("CHF", 120.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSecuritySell01()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "CHF");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("11704490"), hasTicker(null), //
                        hasName("Sc Ind Eq Wo.ex.CH"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("11704497"), hasTicker(null), //
                        hasName("Sc F. V Eq Emer. M."), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("13250189"), hasTicker(null), //
                        hasName("Sc Eq S&Mid Caps CH"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("21580471"), hasTicker(null), //
                        hasName("Sc Ind EqLar Cap CH"), //
                        hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), hasShares(0.000439), //
                        hasSource("Sell01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 0.15), hasGrossValue("CHF", 0.15), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), hasShares(0.000213), //
                        hasSource("Sell01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 0.03), hasGrossValue("CHF", 0.03), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), hasShares(0.000172), //
                        hasSource("Sell01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 0.06), hasGrossValue("CHF", 0.06), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), hasShares(0.001239), //
                        hasSource("Sell01.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 0.23), hasGrossValue("CHF", 0.23), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testSecuritySell02()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, "CHF");

        // check 1st security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("11704490"), hasTicker(null), //
                        hasName("Sc Ind Eq Wo.ex.CH"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("11704497"), hasTicker(null), //
                        hasName("Sc F. V Eq Emer. M."), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("13250189"), hasTicker(null), //
                        hasName("Sc Eq S&Mid Caps CH"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("21580471"), hasTicker(null), //
                        hasName("Sc Ind EqLar Cap CH"), hasCurrencyCode("CHF"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), hasShares(0.000439), //
                        hasSource("Sell02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 0.15), hasGrossValue("CHF", 0.15), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), hasShares(0.000213), //
                        hasSource("Sell02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 0.03), hasGrossValue("CHF", 0.03), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), hasShares(0.000172), //
                        hasSource("Sell02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 0.06), hasGrossValue("CHF", 0.06), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2025-12-04"), hasShares(0.001239), //
                        hasSource("Sell02.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 0.23), hasGrossValue("CHF", 0.23), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00))));
    }

    @Test
    public void testDeposit01()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Deposit01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-11-18"), hasAmount("CHF", 1000.00), //
                        hasSource("Deposit01.txt"), hasNote(null))));
    }

    @Test
    public void testDeposit02()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Deposit02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2026-01-08"), hasAmount("CHF", 6258.00), //
                        hasSource("Deposit02.txt"), hasNote(null))));
    }

    @Test
    public void testFees01()
    {
        var extractor = new NeonSwitzerlandAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fees01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CHF");

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2025-12-03"), hasAmount("CHF", 0.47), //
                        hasSource("Fees01.txt"), hasNote("Assessment from 24.11.25 - 31.12.25"))));
    }
}
