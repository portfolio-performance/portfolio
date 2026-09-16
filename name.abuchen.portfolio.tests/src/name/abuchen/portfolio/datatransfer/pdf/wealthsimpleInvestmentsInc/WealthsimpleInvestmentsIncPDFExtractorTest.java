package name.abuchen.portfolio.datatransfer.pdf.wealthsimpleInvestmentsInc;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
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
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.WealthsimpleInvestmentsIncPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class WealthsimpleInvestmentsIncPDFExtractorTest
{
    @Test
    public void testDepotStatement01()
    {
        Client client = new Client();

        WealthsimpleInvestmentsIncPDFExtractor extractor = new WealthsimpleInvestmentsIncPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(10L));
        assertThat(countBuySell(results), is(32L));
        assertThat(countAccountTransactions(results), is(41L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(3L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(83));

        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ZFL"), //
                        hasName("BMO Long Federal Bond ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("QTIP"), //
                        hasName("Mackenzie Financial Corp"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ZAG"), //
                        hasName("BMO AGGREGATE BOND INDEX ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XSH"), //
                        hasName("iShares Core Canadian ST Corp"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("GLDM"), //
                        hasName("World Gold Trust"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("EEMV"), //
                        hasName("iShares MSCI Emerg Min Vol ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ACWV"), //
                        hasName("iShares Edge MSCI Min Vol Global ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VTI"), //
                        hasName("Vanguard Total Stock Market ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XIC"), //
                        hasName("iShares Core S&P/TSX Capped Composite Index ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XEF"), //
                        hasName("iShares Core MSCI EAFE IMI Index ETF"), //
                        hasCurrencyCode("CAD"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)"), //
                        dividend( //
                                        hasDate("2020-07-10T00:00"), hasExDate("2020-07-03T00:00"), //
                                        hasShares(14.0853), //
                                        hasSource("DepotStatement01.txt"), //
                                        hasNote(null), //
                                        hasAmount("CAD", 1.36), hasGrossValue("CAD", 1.36), //
                                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00)))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)"), //
                        dividend( //
                                        hasDate("2020-06-09T00:00"), hasExDate("2020-06-02T00:00"), //
                                        hasShares(14.3972), //
                                        hasSource("DepotStatement01.txt"), //
                                        hasNote(null), //
                                        hasAmount("CAD", 0.88), hasGrossValue("CAD", 0.88), //
                                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00)))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)"), //
                        dividend( //
                                        hasDate("2020-05-11T00:00"), hasExDate("2020-05-04T00:00"), //
                                        hasShares(14.3972), //
                                        hasSource("DepotStatement01.txt"), //
                                        hasNote(null), //
                                        hasAmount("CAD", 1.27), hasGrossValue("CAD", 1.27), //
                                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00)))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-02T00:00"), hasAmount("CAD", 1000.00), //
                        hasSource("DepotStatement01.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-01T00:00"), hasAmount("CAD", 100.00), //
                        hasSource("DepotStatement01.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-05-02T00:00"), hasAmount("CAD", 100.00), //
                        hasSource("DepotStatement01.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-04-02T00:00"), hasAmount("CAD", 5000.00), //
                        hasSource("DepotStatement01.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-04-02T00:00"), hasAmount("CAD", 5000.00), //
                        hasSource("DepotStatement01.txt"), hasNote(null))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-29T00:00"), hasShares(1.7582), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 35.04), hasGrossValue("CAD", 35.04), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-05T00:00"), hasShares(0.6794), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 13.82), hasGrossValue("CAD", 13.82), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-25T00:00"), hasShares(14.0853), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1521.49), hasGrossValue("CAD", 1521.49), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-25T00:00"), hasShares(50.9493), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1055.16), hasGrossValue("CAD", 1055.16), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-25T00:00"), hasShares(88.9806), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1493.09), hasGrossValue("CAD", 1493.09), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-25T00:00"), hasShares(79.9364), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1582.74), hasGrossValue("CAD", 1582.74), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-25T00:00"), hasShares(15.0926), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 376.84), hasGrossValue("CAD", 376.84), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-07T00:00"), hasShares(0.5764), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 12.19), hasGrossValue("CAD", 12.19), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-03T00:00"), hasShares(1.0702), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 22.32), hasGrossValue("CAD", 22.32), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(1.7993), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 128.87), hasGrossValue("CAD", 128.87), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(0.4814), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 57.32), hasGrossValue("CAD", 57.32), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(0.8204), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 172.91), hasGrossValue("CAD", 172.91), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(3.3852), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 82.43), hasGrossValue("CAD", 82.43), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(4.7504), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 138.26), hasGrossValue("CAD", 138.26), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(9.2542), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 154.73), hasGrossValue("CAD", 154.73), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(0.3119), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 33.00), hasGrossValue("CAD", 33.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(10.9669), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 228.77), hasGrossValue("CAD", 228.77), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-26T00:00"), hasShares(1.72), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 28.75), hasGrossValue("CAD", 28.75), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-01T00:00"), hasShares(0.9932), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 16.39), hasGrossValue("CAD", 16.39), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-01T00:00"), hasShares(4.1164), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 84.16), hasGrossValue("CAD", 84.16), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-04T00:00"), hasShares(0.0232), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 2.78), hasGrossValue("CAD", 2.78), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-04T00:00"), hasShares(0.0357), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 2.50), hasGrossValue("CAD", 2.50), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-04T00:00"), hasShares(1.6774), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 27.71), hasGrossValue("CAD", 27.71), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-04T00:00"), hasShares(3.1217), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 64.15), hasGrossValue("CAD", 64.15), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(22.6219), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1494.00), hasGrossValue("CAD", 1494.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(8.8192), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 996.00), hasGrossValue("CAD", 996.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(5.591), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 996.01), hasGrossValue("CAD", 996.01), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(5.591), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 996.01), hasGrossValue("CAD", 996.01), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(14.3972), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1494.00), hasGrossValue("CAD", 1494.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(39.0135), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 996.00), hasGrossValue("CAD", 996.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(23.8851), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 498.00), hasGrossValue("CAD", 498.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(93.8442), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1494.00), hasGrossValue("CAD", 1494.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(98.7605), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1992.00), hasGrossValue("CAD", 1992.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-22T00:00"), hasExDate("2020-12-15T00:00"), //
                        hasShares(8.3610), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 8.55), hasGrossValue("CAD", 9.83), //
                        hasTaxes("CAD", 1.28), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-22T00:00"), hasExDate("2020-12-15T00:00"), //
                        hasShares(20.8583), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 25.40), hasGrossValue("CAD", 29.21), //
                        hasTaxes("CAD", 3.81), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-03T00:00"), hasExDate("2020-11-30T00:00"), //
                        hasShares(148.3070), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 6.38), hasGrossValue("CAD", 6.38), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-30T00:00"), hasExDate("2020-11-25T00:00"), //
                        hasShares(79.9364), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.44), hasGrossValue("CAD", 3.44), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-03T00:00"), hasExDate("2020-10-29T00:00"), //
                        hasShares(148.3070), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 6.38), hasGrossValue("CAD", 6.38), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-30T00:00"), hasExDate("2020-10-27T00:00"), //
                        hasShares(79.9364), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.44), hasGrossValue("CAD", 3.44), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-02T00:00"), hasExDate("2020-09-28T00:00"), //
                        hasShares(4.7706), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.29), hasGrossValue("CAD", 4.93), //
                        hasTaxes("CAD", 0.64), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-02T00:00"), hasExDate("2020-09-29T00:00"), //
                        hasShares(147.6276), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 6.35), hasGrossValue("CAD", 6.35), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-09-30T00:00"), hasExDate("2020-09-25T00:00"), //
                        hasShares(20.4999), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.30), hasGrossValue("CAD", 4.30), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-09-02T00:00"), hasExDate("2020-08-28T00:00"), //
                        hasShares(88.9806), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.56), hasGrossValue("CAD", 3.56), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-09-02T00:00"), hasExDate("2020-08-28T00:00"), //
                        hasShares(96.6783), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.16), hasGrossValue("CAD", 4.16), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-05T00:00"), hasExDate("2020-07-30T00:00"), //
                        hasShares(88.9806), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.56), hasGrossValue("CAD", 3.56), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-05T00:00"), hasExDate("2020-07-30T00:00"), //
                        hasShares(96.1019), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.13), hasGrossValue("CAD", 4.13), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-06T00:00"), hasExDate("2020-06-29T00:00"), //
                        hasShares(96.5148), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.86), hasGrossValue("CAD", 3.86), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-06T00:00"), hasExDate("2020-06-29T00:00"), //
                        hasShares(105.9986), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.56), hasGrossValue("CAD", 4.56), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-02T00:00"), hasExDate("2020-06-26T00:00"), //
                        hasShares(5.5910), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 5.32), hasGrossValue("CAD", 6.12), //
                        hasTaxes("CAD", 0.80), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-30T00:00"), hasExDate("2020-06-25T00:00"), //
                        hasShares(39.0135), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 13.93), hasGrossValue("CAD", 13.93), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-30T00:00"), hasExDate("2020-06-25T00:00"), //
                        hasShares(23.8851), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 5.23), hasGrossValue("CAD", 5.23), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-23T00:00"), hasExDate("2020-06-16T00:00"), //
                        hasShares(8.8424), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 11.02), hasGrossValue("CAD", 12.67), //
                        hasTaxes("CAD", 1.65), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-23T00:00"), hasExDate("2020-06-16T00:00"), //
                        hasShares(22.6576), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 16.93), hasGrossValue("CAD", 19.47), //
                        hasTaxes("CAD", 2.54), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-02T00:00"), hasExDate("2020-05-28T00:00"), //
                        hasShares(101.8822), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.38), hasGrossValue("CAD", 4.38), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-02T00:00"), hasExDate("2020-05-28T00:00"), //
                        hasShares(95.5216), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.82), hasGrossValue("CAD", 3.82), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-04T00:00"), hasExDate("2020-04-29T00:00"), //
                        hasShares(98.7605), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.25), hasGrossValue("CAD", 4.25), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-04T00:00"), hasExDate("2020-04-29T00:00"), //
                        hasShares(93.8442), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.75), hasGrossValue("CAD", 3.75), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-12-31T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.42 - 0.26 + 0.21), hasGrossValue("CAD", 4.42 - 0.26 + 0.21), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-11-30T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.25 - 0.09 + 0.21), hasGrossValue("CAD", 4.25 - 0.09 + 0.21), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-10-31T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.33 - 0.09 + 0.21), hasGrossValue("CAD", 4.33 - 0.09 + 0.21), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-09-30T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.15 - 0.08 + 0.20), hasGrossValue("CAD", 4.15 - 0.08 + 0.20), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-08-31T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.34 - 0.09 + 0.21), hasGrossValue("CAD", 4.34 - 0.09 + 0.21), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-07-31T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.30 - 0.51 + 0.19), hasGrossValue("CAD", 4.30 - 0.51 + 0.19), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-06-30T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.46 - 0.49 + 0.20), hasGrossValue("CAD", 4.46 - 0.49 + 0.20), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-05-31T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.51 - 0.47 + 0.20), hasGrossValue("CAD", 4.51 - 0.47 + 0.20), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-04-30T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.08 - 0.42 + 0.18), hasGrossValue("CAD", 4.08 - 0.42 + 0.18), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }

    @Test
    public void testtestDepotStatement01WithAllSecuritiesInUSD()
    {
        Security security6 = new Security("iShares MSCI Emerg Min Vol ETF", CurrencyUnit.USD);
        security6.setTickerSymbol("EEMV");

        Security security7 = new Security("iShares Edge MSCI Min Vol Global ETF", CurrencyUnit.USD);
        security7.setTickerSymbol("ACWV");

        Security security8 = new Security("Vanguard Total Stock Market ETF", CurrencyUnit.USD);
        security8.setTickerSymbol("VTI");

        Client client = new Client();
        client.addSecurity(security6);
        client.addSecurity(security7);
        client.addSecurity(security8);

        WealthsimpleInvestmentsIncPDFExtractor extractor = new WealthsimpleInvestmentsIncPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(32L));
        assertThat(countAccountTransactions(results), is(41L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(3L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(80));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ZFL"), //
                        hasName("BMO Long Federal Bond ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("QTIP"), //
                        hasName("Mackenzie Financial Corp"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ZAG"), //
                        hasName("BMO AGGREGATE BOND INDEX ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XSH"), //
                        hasName("iShares Core Canadian ST Corp"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("GLDM"), //
                        hasName("World Gold Trust"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XIC"), //
                        hasName("iShares Core S&P/TSX Capped Composite Index ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XEF"), //
                        hasName("iShares Core MSCI EAFE IMI Index ETF"), //
                        hasCurrencyCode("CAD"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)"), //
                        dividend( //
                                        hasDate("2020-07-10T00:00"), hasExDate("2020-07-03T00:00"), //
                                        hasShares(14.0853), //
                                        hasSource("DepotStatement01.txt"), //
                                        hasNote(null), //
                                        hasAmount("CAD", 1.36), hasGrossValue("CAD", 1.36), //
                                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00)))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)"), //
                        dividend( //
                                        hasDate("2020-06-09T00:00"), hasExDate("2020-06-02T00:00"), //
                                        hasShares(14.3972), //
                                        hasSource("DepotStatement01.txt"), //
                                        hasNote(null), //
                                        hasAmount("CAD", 0.88), hasGrossValue("CAD", 0.88), //
                                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00)))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)"), //
                        dividend( //
                                        hasDate("2020-05-11T00:00"), hasExDate("2020-05-04T00:00"), //
                                        hasShares(14.3972), //
                                        hasSource("DepotStatement01.txt"), //
                                        hasNote(null), //
                                        hasAmount("CAD", 1.27), hasGrossValue("CAD", 1.27), //
                                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00)))));

        // check removal transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-02T00:00"), hasAmount("CAD", 1000.00), //
                        hasSource("DepotStatement01.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-01T00:00"), hasAmount("CAD", 100.00), //
                        hasSource("DepotStatement01.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-05-02T00:00"), hasAmount("CAD", 100.00), //
                        hasSource("DepotStatement01.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-04-02T00:00"), hasAmount("CAD", 5000.00), //
                        hasSource("DepotStatement01.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-04-02T00:00"), hasAmount("CAD", 5000.00), //
                        hasSource("DepotStatement01.txt"), hasNote(null))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-12-29T00:00"), hasShares(1.7582), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 35.04), hasGrossValue("CAD", 35.04), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-10-05T00:00"), hasShares(0.6794), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 13.82), hasGrossValue("CAD", 13.82), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-25T00:00"), hasShares(14.0853), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1521.49), hasGrossValue("CAD", 1521.49), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-25T00:00"), hasShares(50.9493), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1055.16), hasGrossValue("CAD", 1055.16), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-25T00:00"), hasShares(88.9806), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1493.09), hasGrossValue("CAD", 1493.09), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-25T00:00"), hasShares(79.9364), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1582.74), hasGrossValue("CAD", 1582.74), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-09-25T00:00"), hasShares(15.0926), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 376.84), hasGrossValue("CAD", 376.84), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-08-07T00:00"), hasShares(0.5764), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 12.19), hasGrossValue("CAD", 12.19), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-03T00:00"), hasShares(1.0702), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 22.32), hasGrossValue("CAD", 22.32), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(1.7993), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 128.87), hasGrossValue("CAD", 128.87), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(0.4814), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 57.32), hasGrossValue("CAD", 57.32), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(0.8204), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 172.91), hasGrossValue("CAD", 172.91), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(3.3852), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 82.43), hasGrossValue("CAD", 82.43), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(4.7504), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 138.26), hasGrossValue("CAD", 138.26), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(9.2542), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 154.73), hasGrossValue("CAD", 154.73), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(0.3119), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 33.00), hasGrossValue("CAD", 33.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(10.9669), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 228.77), hasGrossValue("CAD", 228.77), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-26T00:00"), hasShares(1.72), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 28.75), hasGrossValue("CAD", 28.75), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-01T00:00"), hasShares(0.9932), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 16.39), hasGrossValue("CAD", 16.39), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-01T00:00"), hasShares(4.1164), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 84.16), hasGrossValue("CAD", 84.16), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-04T00:00"), hasShares(0.0232), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 2.78), hasGrossValue("CAD", 2.78), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-04T00:00"), hasShares(0.0357), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 2.50), hasGrossValue("CAD", 2.50), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-04T00:00"), hasShares(1.6774), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 27.71), hasGrossValue("CAD", 27.71), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-04T00:00"), hasShares(3.1217), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 64.15), hasGrossValue("CAD", 64.15), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(22.6219), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1494.00), hasGrossValue("CAD", 1494.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(8.8192), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 996.00), hasGrossValue("CAD", 996.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(5.591), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 996.01), hasGrossValue("CAD", 996.01), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(5.591), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 996.01), hasGrossValue("CAD", 996.01), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(14.3972), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1494.00), hasGrossValue("CAD", 1494.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(39.0135), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 996.00), hasGrossValue("CAD", 996.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(23.8851), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 498.00), hasGrossValue("CAD", 498.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(93.8442), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1494.00), hasGrossValue("CAD", 1494.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-04-02T00:00"), hasShares(98.7605), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 1992.00), hasGrossValue("CAD", 1992.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-22T00:00"), hasExDate("2020-12-15T00:00"), //
                        hasShares(8.3610), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 8.55), hasGrossValue("CAD", 9.83), //
                        hasForexGrossValue("USD", 7.60), //
                        hasTaxes("CAD", 1.28), hasFees("CAD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CAD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-22T00:00"), hasExDate("2020-12-15T00:00"), //
                        hasShares(20.8583), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 25.40), hasGrossValue("CAD", 29.21), //
                        hasForexGrossValue("USD", 22.60), //
                        hasTaxes("CAD", 3.81), hasFees("CAD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CAD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-12-03T00:00"), hasExDate("2020-11-30T00:00"), //
                        hasShares(148.3070), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 6.38), hasGrossValue("CAD", 6.38), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-30T00:00"), hasExDate("2020-11-25T00:00"), //
                        hasShares(79.9364), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.44), hasGrossValue("CAD", 3.44), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-11-03T00:00"), hasExDate("2020-10-29T00:00"), //
                        hasShares(148.3070), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 6.38), hasGrossValue("CAD", 6.38), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-30T00:00"), hasExDate("2020-10-27T00:00"), //
                        hasShares(79.9364), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.44), hasGrossValue("CAD", 3.44), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-02T00:00"), hasExDate("2020-09-28T00:00"), //
                        hasShares(4.7706), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.29), hasGrossValue("CAD", 4.93), //
                        hasForexGrossValue("USD", 3.70), //
                        hasTaxes("CAD", 0.64), hasFees("CAD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CAD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-10-02T00:00"), hasExDate("2020-09-29T00:00"), //
                        hasShares(147.6276), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 6.35), hasGrossValue("CAD", 6.35), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-09-30T00:00"), hasExDate("2020-09-25T00:00"), //
                        hasShares(20.4999), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.30), hasGrossValue("CAD", 4.30), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-09-02T00:00"), hasExDate("2020-08-28T00:00"), //
                        hasShares(88.9806), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.56), hasGrossValue("CAD", 3.56), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-09-02T00:00"), hasExDate("2020-08-28T00:00"), //
                        hasShares(96.6783), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.16), hasGrossValue("CAD", 4.16), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-05T00:00"), hasExDate("2020-07-30T00:00"), //
                        hasShares(88.9806), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.56), hasGrossValue("CAD", 3.56), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-08-05T00:00"), hasExDate("2020-07-30T00:00"), //
                        hasShares(96.1019), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.13), hasGrossValue("CAD", 4.13), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-06T00:00"), hasExDate("2020-06-29T00:00"), //
                        hasShares(96.5148), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.86), hasGrossValue("CAD", 3.86), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-06T00:00"), hasExDate("2020-06-29T00:00"), //
                        hasShares(105.9986), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.56), hasGrossValue("CAD", 4.56), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-02T00:00"), hasExDate("2020-06-26T00:00"), //
                        hasShares(5.5910), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 5.32), hasGrossValue("CAD", 6.12), //
                        hasForexGrossValue("USD", 4.50), //
                        hasTaxes("CAD", 0.80), hasFees("CAD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CAD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-30T00:00"), hasExDate("2020-06-25T00:00"), //
                        hasShares(39.0135), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 13.93), hasGrossValue("CAD", 13.93), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-30T00:00"), hasExDate("2020-06-25T00:00"), //
                        hasShares(23.8851), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 5.23), hasGrossValue("CAD", 5.23), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-23T00:00"), hasExDate("2020-06-16T00:00"), //
                        hasShares(8.8424), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 11.02), hasGrossValue("CAD", 12.67), //
                        hasForexGrossValue("USD", 9.36), //
                        hasTaxes("CAD", 1.65), hasFees("CAD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CAD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-23T00:00"), hasExDate("2020-06-16T00:00"), //
                        hasShares(22.6576), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 16.93), hasGrossValue("CAD", 19.47), //
                        hasForexGrossValue("USD", 14.38), //
                        hasTaxes("CAD", 2.54), hasFees("CAD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CAD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-02T00:00"), hasExDate("2020-05-28T00:00"), //
                        hasShares(101.8822), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.38), hasGrossValue("CAD", 4.38), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-02T00:00"), hasExDate("2020-05-28T00:00"), //
                        hasShares(95.5216), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.82), hasGrossValue("CAD", 3.82), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-04T00:00"), hasExDate("2020-04-29T00:00"), //
                        hasShares(98.7605), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.25), hasGrossValue("CAD", 4.25), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-05-04T00:00"), hasExDate("2020-04-29T00:00"), //
                        hasShares(93.8442), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.75), hasGrossValue("CAD", 3.75), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-12-31T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.42 - 0.26 + 0.21), hasGrossValue("CAD", 4.42 - 0.26 + 0.21), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-11-30T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.25 - 0.09 + 0.21), hasGrossValue("CAD", 4.25 - 0.09 + 0.21), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-10-31T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.33 - 0.09 + 0.21), hasGrossValue("CAD", 4.33 - 0.09 + 0.21), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-09-30T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.15 - 0.08 + 0.20), hasGrossValue("CAD", 4.15 - 0.08 + 0.20), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-08-31T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.34 - 0.09 + 0.21), hasGrossValue("CAD", 4.34 - 0.09 + 0.21), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-07-31T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.30 - 0.51 + 0.19), hasGrossValue("CAD", 4.30 - 0.51 + 0.19), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-06-30T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.46 - 0.49 + 0.20), hasGrossValue("CAD", 4.46 - 0.49 + 0.20), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-05-31T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.51 - 0.47 + 0.20), hasGrossValue("CAD", 4.51 - 0.47 + 0.20), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-04-30T00:00"), //
                        hasSource("DepotStatement01.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.08 - 0.42 + 0.18), hasGrossValue("CAD", 4.08 - 0.42 + 0.18), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }

    @Test
    public void testDepotStatement02()
    {
        Client client = new Client();

        WealthsimpleInvestmentsIncPDFExtractor extractor = new WealthsimpleInvestmentsIncPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotStatement02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(8L));
        assertThat(countBuySell(results), is(11L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(28));

        new AssertImportActions().check(results, "CAD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("EEMV"), //
                        hasName("iShares MSCI Emerg Min Vol ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ACWV"), //
                        hasName("iShares Edge MSCI Min Vol Global ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VTI"), //
                        hasName("Vanguard Total Stock Market ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XIC"), //
                        hasName("iShares Core S&P/TSX Capped Composite Index ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XEF"), //
                        hasName("iShares Core MSCI EAFE IMI Index ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ZAG"), //
                        hasName("BMO AGGREGATE BOND INDEX ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("QTIP"), //
                        hasName("Mackenzie Financial Corp"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ZFL"), //
                        hasName("BMO Long Federal Bond ETF"), //
                        hasCurrencyCode("CAD"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)"), //
                        dividend( //
                                        hasDate("2020-06-09T00:00"), hasExDate("2020-06-02T00:00"), //
                                        hasShares(14.3972), //
                                        hasSource("DepotStatement02.txt"), //
                                        hasNote(null), //
                                        hasAmount("CAD", 0.88), hasGrossValue("CAD", 0.88), //
                                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00)))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-01T00:00"), hasAmount("CAD", 100.00), //
                        hasSource("DepotStatement02.txt"), hasNote(null))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(1.7993), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 128.87), hasGrossValue("CAD", 128.87), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(0.4814), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 57.32), hasGrossValue("CAD", 57.32), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(0.8204), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 172.91), hasGrossValue("CAD", 172.91), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(3.3852), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 82.43), hasGrossValue("CAD", 82.43), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(4.7504), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 138.26), hasGrossValue("CAD", 138.26), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(9.2542), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 154.73), hasGrossValue("CAD", 154.73), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(0.3119), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 33.00), hasGrossValue("CAD", 33.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(10.9669), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 228.77), hasGrossValue("CAD", 228.77), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-26T00:00"), hasShares(1.72), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 28.75), hasGrossValue("CAD", 28.75), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-01T00:00"), hasShares(0.9932), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 16.39), hasGrossValue("CAD", 16.39), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-01T00:00"), hasShares(4.1164), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 84.16), hasGrossValue("CAD", 84.16), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-30T00:00"), hasExDate("2020-06-25T00:00"), //
                        hasShares(39.0135), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 13.93), hasGrossValue("CAD", 13.93), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-30T00:00"), hasExDate("2020-06-25T00:00"), //
                        hasShares(23.8851), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 5.23), hasGrossValue("CAD", 5.23), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-23T00:00"), hasExDate("2020-06-16T00:00"), //
                        hasShares(8.8424), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 11.02), hasGrossValue("CAD", 12.67), //
                        hasTaxes("CAD", 1.65), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-23T00:00"), hasExDate("2020-06-16T00:00"), //
                        hasShares(22.6576), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 16.93), hasGrossValue("CAD", 19.47), //
                        hasTaxes("CAD", 2.54), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-02T00:00"), hasExDate("2020-05-28T00:00"), //
                        hasShares(101.8822), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.38), hasGrossValue("CAD", 4.38), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-02T00:00"), hasExDate("2020-05-28T00:00"), //
                        hasShares(95.5216), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.82), hasGrossValue("CAD", 3.82), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-06-30T00:00"), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.46 - 0.49 + 0.20), hasGrossValue("CAD", 4.46 - 0.49 + 0.20), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }

    @Test
    public void testtestDepotStatement02WithAllSecuritiesInUSD()
    {
        Security security1 = new Security("iShares MSCI Emerg Min Vol ETF", CurrencyUnit.USD);
        security1.setTickerSymbol("EEMV");

        Security security2 = new Security("iShares Edge MSCI Min Vol Global ETF", CurrencyUnit.USD);
        security2.setTickerSymbol("ACWV");

        Client client = new Client();
        client.addSecurity(security1);
        client.addSecurity(security2);

        WealthsimpleInvestmentsIncPDFExtractor extractor = new WealthsimpleInvestmentsIncPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DepotStatement02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(6L));
        assertThat(countBuySell(results), is(11L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(1L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(26));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VTI"), //
                        hasName("Vanguard Total Stock Market ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XIC"), //
                        hasName("iShares Core S&P/TSX Capped Composite Index ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XEF"), //
                        hasName("iShares Core MSCI EAFE IMI Index ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ZAG"), //
                        hasName("BMO AGGREGATE BOND INDEX ETF"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("QTIP"), //
                        hasName("Mackenzie Financial Corp"), //
                        hasCurrencyCode("CAD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ZFL"), //
                        hasName("BMO Long Federal Bond ETF"), //
                        hasCurrencyCode("CAD"))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        MessageFormat.format(Messages.MsgMissingTickerSymbol, "Mackenzie US TIPS Index ETF (CAD-Hedged)"), //
                        dividend( //
                                        hasDate("2020-06-09T00:00"), hasExDate("2020-06-02T00:00"), //
                                        hasShares(14.3972), //
                                        hasSource("DepotStatement02.txt"), //
                                        hasNote(null), //
                                        hasAmount("CAD", 0.88), hasGrossValue("CAD", 0.88), //
                                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00)))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-01T00:00"), hasAmount("CAD", 100.00), //
                        hasSource("DepotStatement02.txt"), hasNote(null))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(1.7993), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 128.87), hasGrossValue("CAD", 128.87), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(0.4814), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 57.32), hasGrossValue("CAD", 57.32), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(0.8204), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 172.91), hasGrossValue("CAD", 172.91), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(3.3852), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 82.43), hasGrossValue("CAD", 82.43), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(4.7504), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 138.26), hasGrossValue("CAD", 138.26), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(9.2542), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 154.73), hasGrossValue("CAD", 154.73), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(0.3119), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 33.00), hasGrossValue("CAD", 33.00), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-06-29T00:00"), hasShares(10.9669), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 228.77), hasGrossValue("CAD", 228.77), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-26T00:00"), hasShares(1.72), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 28.75), hasGrossValue("CAD", 28.75), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-01T00:00"), hasShares(0.9932), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 16.39), hasGrossValue("CAD", 16.39), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-06-01T00:00"), hasShares(4.1164), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 84.16), hasGrossValue("CAD", 84.16), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-30T00:00"), hasExDate("2020-06-25T00:00"), //
                        hasShares(39.0135), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 13.93), hasGrossValue("CAD", 13.93), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-30T00:00"), hasExDate("2020-06-25T00:00"), //
                        hasShares(23.8851), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 5.23), hasGrossValue("CAD", 5.23), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-23T00:00"), hasExDate("2020-06-16T00:00"), //
                        hasShares(8.8424), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 11.02), hasGrossValue("CAD", 12.67), //
                        hasForexGrossValue("USD", 9.36), //
                        hasTaxes("CAD", 1.65), hasFees("CAD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CAD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-23T00:00"), hasExDate("2020-06-16T00:00"), //
                        hasShares(22.6576), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 16.93), hasGrossValue("CAD", 19.47), //
                        hasForexGrossValue("USD", 14.38), //
                        hasTaxes("CAD", 2.54), hasFees("CAD", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("CAD");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-02T00:00"), hasExDate("2020-05-28T00:00"), //
                        hasShares(101.8822), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 4.38), hasGrossValue("CAD", 4.38), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-06-02T00:00"), hasExDate("2020-05-28T00:00"), //
                        hasShares(95.5216), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("CAD", 3.82), hasGrossValue("CAD", 3.82), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-06-30T00:00"), //
                        hasSource("DepotStatement02.txt"), //
                        hasNote("Management fee to Wealthsimple"), //
                        hasAmount("CAD", 4.46 - 0.49 + 0.20), hasGrossValue("CAD", 4.46 - 0.49 + 0.20), //
                        hasTaxes("CAD", 0.00), hasFees("CAD", 0.00))));
    }
}
