package name.abuchen.portfolio.datatransfer.pdf.scorepriorityinc;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.ScorePriorityIncPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class ScorePriorityIncPDFExtractorTest
{
    /***
     * Information: Score Priority is a US-based financial services company. The
     * currency is US$. All security currencies are USD. CUSIP Number: The CUSIP
     * number is the WKN number. Dividend transactions: The amount of dividends
     * is reported in gross.
     */

    @Test
    public void testAccountStatement01()
    {
        var extractor = new ScorePriorityIncPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(17));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("64110L106"), hasTicker(null), //
                        hasName("Netflix Inc Com"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("003260106"), hasTicker(null), //
                        hasName("Aberdeen Std Platinum Etf Tr Physcl Platm Shs"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("067901108"), hasTicker(null), //
                        hasName("Barrick Gold Corp Com"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("922908363"), hasTicker(null), //
                        hasName("Vanguard Index Fds S P 500 Etf Shs"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("756109104"), hasTicker(null), //
                        hasName("Realty Income C"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("902494103"), hasTicker(null), //
                        hasName("Tyson Foods Inc"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("81369Y605"), hasTicker(null), //
                        hasName("Select Sector S"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-09-02T00:00"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 1132.39), hasGrossValue("USD", 1132.39), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-09-15T00:00"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 89.80), hasGrossValue("USD", 89.80), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-09-15T00:00"), hasShares(4.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 78.48), hasGrossValue("USD", 78.48), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-09-15T00:00"), hasShares(4.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 1638.44), hasGrossValue("USD", 1638.44), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-09-30T00:00"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 401.00), hasGrossValue("USD", 401.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-09-16"), hasExDate(null), //
                        hasShares(14.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 1.97), hasGrossValue("USD", 1.97), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2021-09-17"), hasExDate(null), //
                        hasShares(14.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 0.95), hasGrossValue("USD", 1.26), //
                        hasTaxes("USD", 0.31), hasFees("USD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2021-09-15"), hasExDate(null), //
                        hasShares(22.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 3.63), hasGrossValue("USD", 5.18), //
                        hasTaxes("USD", 1.55), hasFees("USD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2021-09-15"), hasExDate(null), //
                        hasShares(6.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 1.87), hasGrossValue("USD", 2.67), //
                        hasTaxes("USD", 0.80), hasFees("USD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2021-09-23"), hasExDate(null), //
                        hasShares(40.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 4.54), hasGrossValue("USD", 6.48), //
                        hasTaxes("USD", 1.94), hasFees("USD", 0.00))));
    }

    @Test
    public void testAccountStatement02()
    {
        var extractor = new ScorePriorityIncPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-12-29"), hasAmount("USD", 71000.00), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2020-12-31"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 0.30), hasGrossValue("USD", 0.30), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testAccountStatement03()
    {
        var extractor = new ScorePriorityIncPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(11L));
        assertThat(countBuySell(results), is(8L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(24));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("902494103"), hasTicker(null), //
                        hasName("Tyson Foods Inc CL A"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("75886F107"), hasTicker(null), //
                        hasName("Regeneron Pharmaceuticals Com"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("46090E103"), hasTicker(null), //
                        hasName("Invesco QQQ Tr Unit Ser 1"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("003260106"), hasTicker(null), //
                        hasName("Aberdeen Std Platinum Etf Tr Physcl Platm Shs"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("067901108"), hasTicker(null), //
                        hasName("Barrick Gold Corp Com"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("881624209"), hasTicker(null), //
                        hasName("Teva Pharmaceutical Inds Ltd Adr"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("92204A306"), hasTicker(null), //
                        hasName("Vanguard World Fds Energy Etf"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("756109104"), hasTicker(null), //
                        hasName("Realty Income C"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("46434G103"), hasTicker(null), //
                        hasName("Ishares Inc"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("81369Y605"), hasTicker(null), //
                        hasName("Select Sector S"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("58933Y105"), hasTicker(null), //
                        hasName("Merck & Co Inc New"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-09T00:00"), hasShares(6.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 468.00), hasGrossValue("USD", 468.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasDate("2021-06-14T00:00"), hasShares(2.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 1049.99), hasGrossValue("USD", 1049.99), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasDate("2021-06-16T00:00"), hasShares(5.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 1714.99), hasGrossValue("USD", 1714.99), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-21T00:00"), hasShares(2.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 204.60), hasGrossValue("USD", 204.60), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-21T00:00"), hasShares(14.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 296.24), hasGrossValue("USD", 296.24), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-21T00:00"), hasShares(20.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 207.80), hasGrossValue("USD", 207.80), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-21T00:00"), hasShares(7.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 527.03), hasGrossValue("USD", 527.03), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-06-21T00:00"), hasShares(13.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 1006.85), hasGrossValue("USD", 1006.85), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-15"), hasExDate(null), //
                        hasShares(22.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 3.62), hasGrossValue("USD", 5.17), //
                        hasTaxes("USD", 1.55), hasFees("USD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-16"), hasExDate(null), //
                        hasShares(400.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 131.75), hasGrossValue("USD", 188.21), //
                        hasTaxes("USD", 56.46), hasFees("USD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-24"), hasExDate(null), //
                        hasShares(40.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 3.90), hasGrossValue("USD", 5.57), //
                        hasTaxes("USD", 1.67), hasFees("USD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-24"), hasExDate(null), //
                        hasShares(20.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 9.22), hasGrossValue("USD", 13.16), //
                        hasTaxes("USD", 3.94), hasFees("USD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-23"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 29.98), hasGrossValue("USD", 29.98), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testAccountStatement04()
    {
        var extractor = new ScorePriorityIncPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(16));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("067901108"), hasTicker(null), //
                        hasName("Barrick Gold Corp Com"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("02913V103"), hasTicker(null), //
                        hasName("American Public Education Inc Com"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("756109104"), hasTicker(null), //
                        hasName("Realty Income C"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("29273V100"), hasTicker(null), //
                        hasName("Energy Transfer"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("901384107"), hasTicker(null), //
                        hasName("2seventy Bio Inc Common Stock"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("68629Y103"), hasTicker(null), //
                        hasName("Orion Office Reit Inc Com"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("09609G100"), hasTicker(null), //
                        hasName("Bluebird Bio Inc"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-11-02T00:00"), hasShares(2.00), //
                        hasSource("AccountStatement04.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 37.00), hasGrossValue("USD", 37.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2021-11-29T00:00"), hasShares(2.00), //
                        hasSource("AccountStatement04.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 42.80), hasGrossValue("USD", 42.80), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-11-15"), hasExDate(null), //
                        hasShares(22.00), //
                        hasSource("AccountStatement04.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 3.64), hasGrossValue("USD", 5.19), //
                        hasTaxes("USD", 1.55), hasFees("USD", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2021-11-19"), hasExDate(null), //
                        hasShares(200.00), //
                        hasSource("AccountStatement04.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 21.35), hasGrossValue("USD", 30.50), //
                        hasTaxes("USD", 9.15), hasFees("USD", 0.00))));

        // check delivery inbound transaction
        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2021-11-05T00:00"), hasShares(5.00), //
                        hasSource("AccountStatement04.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 0.00), hasGrossValue("USD", 0.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        assertThat(results, hasItem(inboundDelivery( //
                        hasDate("2021-11-15T00:00"), hasShares(2.00), //
                        hasSource("AccountStatement04.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 0.00), hasGrossValue("USD", 0.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check failure message
        assertThat(results, hasItem(withFailureMessage( //
                        MessageFormat.format(Messages.MsgErrorInvalidWKN, "09609"), //
                        fee( //
                                        hasDate("2021-11-05"), //
                                        hasSource("AccountStatement04.txt"), //
                                        hasNote(null), //
                                        hasAmount("USD", 30.00), hasGrossValue("USD", 30.00), //
                                        hasTaxes("USD", 0.00), hasFees("USD", 0.00)))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-11-15"), //
                        hasSource("AccountStatement04.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 30.00), hasGrossValue("USD", 30.00), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-11-22"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement04.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 11.70), hasGrossValue("USD", 11.70), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }
}
