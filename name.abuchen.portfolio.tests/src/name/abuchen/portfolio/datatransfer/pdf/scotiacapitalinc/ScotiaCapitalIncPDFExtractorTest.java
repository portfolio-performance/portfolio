package name.abuchen.portfolio.datatransfer.pdf.scotiacapitalinc;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
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
import name.abuchen.portfolio.datatransfer.pdf.ScotiaCapitalIncPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class ScotiaCapitalIncPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        var extractor = new ScotiaCapitalIncPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(6L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(18));
        new AssertImportActions().check(results, "CAD", "USD");

        // check securities
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("RICHARDS PACKAGING INCOME FUND TRUST UTS"), //
                        hasCurrencyCode("CAD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("CHORUS AVIATION INC VTG & VARIABLE VTG SHS"), //
                        hasCurrencyCode("CAD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("BROOKFIELD INFRASTRUCTURE PARTNERS L P UNITS"), //
                        hasCurrencyCode("CAD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("TWILIO INC CL A"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("MARKEL CORP HOLDING CO"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("TRADE DESK INC (THE) CLASS A COMMON STOCK"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker(null), //
                        hasName("UNION PACIFIC CORP"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-03-14T00:00"), hasExDate(null), hasShares(150.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 16.50), hasGrossValue("CAD", 16.50), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2019-03-18T00:00"), hasExDate(null), hasShares(380.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 15.20), hasGrossValue("CAD", 15.20), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2019-03-29T00:00"), hasExDate(null), hasShares(88.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 58.23), hasGrossValue("CAD", 58.23), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2019-03-29T00:00"), hasExDate(null), hasShares(50.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 44.00), hasGrossValue("USD", 44.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check buy sell transaction (dividend reinvestment)
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-20T00:00"), hasShares(2.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 13.90), hasGrossValue("CAD", 13.90), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-29T00:00"), hasShares(1.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 55.29), hasGrossValue("CAD", 55.29), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-14T00:00"), hasShares(31.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 3832.29), hasGrossValue("USD", 3822.30), //
                        hasTaxes("USD", 0.00), hasFees("USD", 9.99))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-19T00:00"), hasShares(4.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 4086.19), hasGrossValue("USD", 4076.20), //
                        hasTaxes("USD", 0.00), hasFees("USD", 9.99))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-03-19T00:00"), hasShares(25.00), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 5265.99), hasGrossValue("USD", 5256.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 9.99))));

        // check currency conversion (removal CAD / deposit USD)
        assertThat(results, hasItem(removal(hasDate("2019-03-15"), hasAmount("CAD", 13505.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("conf#34Q7K | C$ - U$ @ 1.3505"))));

        assertThat(results, hasItem(deposit(hasDate("2019-03-15"), hasAmount("USD", 10000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("conf#34Q7K | C$ - U$ @ 1.3505"))));
    }
}
