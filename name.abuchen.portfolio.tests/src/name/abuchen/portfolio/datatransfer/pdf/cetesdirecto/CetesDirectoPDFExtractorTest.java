package name.abuchen.portfolio.datatransfer.pdf.cetesdirecto;

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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.CetesDirectoPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class CetesDirectoPDFExtractorTest
{
    @Test
    public void testEdoCta01()
    {
        var extractor = new CetesDirectoPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EdoCta01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(9L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(21));
        new AssertImportActions().check(results, "MXN");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("CETES (220203)"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("CETES (220106)"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("BONDDIA (PF2)"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("CETES (220210)"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("CETES (220113)"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("CETES (220217)"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("CETES (220120)"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("CETES (220224)"), //
                        hasCurrencyCode("MXN"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("CETES (220127)"), //
                        hasCurrencyCode("MXN"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-04T00:00"), hasShares(6080.00), //
                        hasSource("EdoCta01.txt"), //
                        hasNote("Term: 2 | Rate: 5.51"), //
                        hasAmount("MXN", 60540.38), hasGrossValue("MXN", 60540.38), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-06T00:00"), hasShares(6055.00), //
                        hasSource("EdoCta01.txt"), //
                        hasNote(null), //
                        hasAmount("MXN", 60546.30), hasGrossValue("MXN", 60550.00), //
                        hasTaxes("MXN", 3.70), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-06T00:00"), hasShares(3.00), //
                        hasSource("EdoCta01.txt"), //
                        hasNote(null), //
                        hasAmount("MXN", 4.72), hasGrossValue("MXN", 4.72), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-11T00:00"), hasShares(21.00), //
                        hasSource("EdoCta01.txt"), //
                        hasNote("Term: 2 | Rate: 5.52"), //
                        hasAmount("MXN", 209.10), hasGrossValue("MXN", 209.10), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-13T00:00"), hasShares(21.00), //
                        hasSource("EdoCta01.txt"), //
                        hasNote(null), //
                        hasAmount("MXN", 209.99), hasGrossValue("MXN", 210.00), //
                        hasTaxes("MXN", 0.01), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-13T00:00"), hasShares(1.00), //
                        hasSource("EdoCta01.txt"), //
                        hasNote(null), //
                        hasAmount("MXN", 1.58), hasGrossValue("MXN", 1.58), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-18T00:00"), hasShares(2872.00), //
                        hasSource("EdoCta01.txt"), //
                        hasNote("Term: 2 | Rate: 5.57"), //
                        hasAmount("MXN", 28596.03), hasGrossValue("MXN", 28596.03), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-20T00:00"), hasShares(2860.00), //
                        hasSource("EdoCta01.txt"), //
                        hasNote(null), //
                        hasAmount("MXN", 28598.25), hasGrossValue("MXN", 28600.00), //
                        hasTaxes("MXN", 1.75), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-20T00:00"), hasShares(1.00), //
                        hasSource("EdoCta01.txt"), //
                        hasNote(null), //
                        hasAmount("MXN", 1.58), hasGrossValue("MXN", 1.58), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-25T00:00"), hasShares(3505.00), //
                        hasSource("EdoCta01.txt"), //
                        hasNote("Term: 2 | Rate: 5.50"), //
                        hasAmount("MXN", 34900.61), hasGrossValue("MXN", 34900.61), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-01-27T00:00"), hasShares(3491.00), //
                        hasSource("EdoCta01.txt"), //
                        hasNote(null), //
                        hasAmount("MXN", 34907.87), hasGrossValue("MXN", 34910.00), //
                        hasTaxes("MXN", 2.13), hasFees("MXN", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2022-01-27T00:00"), hasShares(5.00), //
                        hasSource("EdoCta01.txt"), //
                        hasNote(null), //
                        hasAmount("MXN", 7.89), hasGrossValue("MXN", 7.89), //
                        hasTaxes("MXN", 0.00), hasFees("MXN", 0.00))));
    }
}
