package name.abuchen.portfolio.datatransfer.pdf.revolutltd;

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
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.RevolutLtdPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class RevolutLtdPDFExtractorTest
{
    @Test
    public void testSecurityBuy01()
    {
        var extractor = new RevolutLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BFY0GT14"), hasWkn(null), hasTicker("SPPW"), //
                        hasName("SPDR MSCI World UCITS ETF (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-01-02T08:10:12"), hasShares(2.61848651), //
                        hasSource("Buy01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testSecuritySell01()
    {
        var extractor = new RevolutLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sell01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88160R1014"), hasWkn(null), hasTicker("TSLA"), //
                        hasName("Tesla"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-11-01T15:51:47"), hasShares(2.1451261), //
                        hasSource("Sell01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 2501.45), hasGrossValue("USD", 2501.47), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.02))));
    }

    @Test
    public void testAccountStatement01()
    {
        var extractor = new RevolutLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(9L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(23));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("V"), //
                        hasName("VISA INC COM CL A"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("NIO"), //
                        hasName("NIO INC SPON ADS"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("EA"), //
                        hasName("ELECTRONIC ARTS INC COM"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("SPOT"), //
                        hasName("SPOTIFY TECHNOLOGY S A SHS"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("BYND"), //
                        hasName("BEYOND MEAT INC COM"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("DIS"), //
                        hasName("DISNEY WALT CO COM DISNEY"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("NET"), //
                        hasName("CLOUDFLARE INC CL A COM"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("NFLX"), //
                        hasName("NETFLIX INC COM"), //
                        hasCurrencyCode("USD"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("FVRR"), //
                        hasName("FIVERR INTL LTD ORD SHS"), //
                        hasCurrencyCode("USD"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-06T00:00"), hasShares(0.40), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 78.48), hasGrossValue("USD", 78.48), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-06T00:00"), hasShares(7.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 81.97), hasGrossValue("USD", 81.97), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-13T00:00"), hasShares(2.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 74.35), hasGrossValue("USD", 74.35), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-08T00:00"), hasAmount("USD", 460.85), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-15T00:00"), hasAmount("USD", 204.15), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-06T00:00"), hasShares(0.60), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 82.55), hasGrossValue("USD", 82.55), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-06T00:00"), hasShares(0.35), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 95.02), hasGrossValue("USD", 95.02), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-06T00:00"), hasShares(0.30), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 43.89), hasGrossValue("USD", 43.89), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-06T00:00"), hasShares(0.20), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 23.88), hasGrossValue("USD", 23.88), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-06T00:00"), hasShares(4.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 48.02), hasGrossValue("USD", 48.02), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-06T00:00"), hasShares(0.50), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 7.04), hasGrossValue("USD", 7.04), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-13T00:00"), hasShares(1.00), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 41.78), hasGrossValue("USD", 41.78), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-13T00:00"), hasShares(0.10), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 54.45), hasGrossValue("USD", 54.45), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-13T00:00"), hasShares(0.40), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 33.57), hasGrossValue("USD", 33.57), //
                        hasTaxes("USD", 0.00), hasFees("USD", 0.00))));
    }

    @Test
    public void testAccountStatement02()
    {
        var extractor = new RevolutLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(11L));
        assertThat(countBuySell(results), is(16L));
        assertThat(countAccountTransactions(results), is(33L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(60));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1686831030"), hasWkn(null), hasTicker("EMBH"), //
                        hasName("Amundi USD Emerging Markets Government Bond UCITS ETF EUR Hedged (Dist)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1829221024"), hasWkn(null), hasTicker("LYMS"), //
                        hasName("Amundi Nasdaq-100 II UCITS ETF (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0908500753"), hasWkn(null), hasTicker("LYP6"), //
                        hasName("Amundi Stoxx Europe 600 UCITS ETF (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2089238898"), hasWkn(null), hasTicker("PRAR"), //
                        hasName("Amundi Prime Euro Govies UCITS ETF DR (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B7KMNP07"), hasWkn(null), hasTicker("UBUD"), //
                        hasName("UBS Solactive Global Pure Gold Miners UCITS ETF (Dist)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU1681045024"), hasWkn(null), hasTicker("AMEL"), //
                        hasName("Amundi MSCI Emerging Markets Latin America UCITS ETF EUR (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2089238385"), hasWkn(null), hasTicker("PRAJ"), //
                        hasName("Amundi Prime Japan UCITS ETF DR (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF20LF40"), hasWkn(null), hasTicker("2B72"), //
                        hasName("iShares MSCI Europe Mid Cap ETF (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BM67HV82"), hasWkn(null), hasTicker("XDWI"), //
                        hasName("Xtrackers MSCI World Industrials UCITS ETF (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE000KYX7IP4"), hasWkn(null), hasTicker("WELK"), //
                        hasName("Amundi S&P Global Financials ESG Acc UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BF2FN869"), hasWkn(null), hasTicker("TRDE"), //
                        hasName("Invesco US Treasury Bond 7-10 Year EUR-hedged Dist ETF"), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-12T09:00:01"), hasShares(0.01925701), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.30), hasGrossValue("EUR", 1.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (0.02 + 0.01)))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-05-11T13:45:00"), hasShares(0.0571061), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.74), hasGrossValue("EUR", 5.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (0.02 + 0.01)))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-01T10:24:11"), hasAmount("EUR", 0.01), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2026-05-28T01:20:41"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Robo management fee"), //
                        hasAmount("EUR", 0.35), hasGrossValue("EUR", 0.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-12T09:01"), hasShares(0.01778749), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.29), hasGrossValue("EUR", 5.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-12T09:01:06"), hasShares(0.0773731), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.39), hasGrossValue("EUR", 1.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-12T09:01:16"), hasShares(0.02376279), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.15), hasGrossValue("EUR", 1.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-12T09:01:42"), hasShares(0.01826698), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.39), hasGrossValue("EUR", 0.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-19T13:45:01"), hasShares(0.00835784), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.54), hasGrossValue("EUR", 0.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-19T13:45:02"), hasShares(0.00033328), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-19T13:45:02"), hasShares(0.00495342), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.50), hasGrossValue("EUR", 0.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-19T13:45:02"), hasShares(0.0147719), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.51), hasGrossValue("EUR", 0.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-19T13:45:04"), hasShares(0.0588116), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.58), hasGrossValue("EUR", 0.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-19T13:45:11"), hasShares(0.00891862), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.64), hasGrossValue("EUR", 0.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-19T13:45:18"), hasShares(0.01876303), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.81), hasGrossValue("EUR", 0.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-19T13:45:34"), hasShares(0.05518926), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.13), hasGrossValue("EUR", 1.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-19T13:45:49"), hasShares(0.02853626), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.51), hasGrossValue("EUR", 0.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-05-19T13:45:59"), hasShares(0.02687913), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.81), hasGrossValue("EUR", 0.81), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-01T16:03:02"), hasAmount("EUR", 0.48), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-01T16:35:32"), hasAmount("EUR", 0.50), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-02T10:00"), hasAmount("EUR", 0.35), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-04T01:12:35"), hasAmount("EUR", 0.01), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-06T10:58:56"), hasAmount("EUR", 0.40), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-07T00:20:41"), hasAmount("EUR", 1.00), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-10T16:27:07"), hasAmount("EUR", 0.59), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-12T11:04:33"), hasAmount("EUR", 0.51), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-12T13:45:25"), hasAmount("EUR", 0.01), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-12T14:18:25"), hasAmount("EUR", 0.04), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-14T11:53:44"), hasAmount("EUR", 0.40), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-15T06:27"), hasAmount("EUR", 0.30), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-15T15:14:48"), hasAmount("EUR", 0.16), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-16T18:54:07"), hasAmount("EUR", 1.00), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-17T10:42:58"), hasAmount("EUR", 0.32), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-17T12:04:08"), hasAmount("EUR", 1.00), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-18T10:54:54"), hasAmount("EUR", 0.93), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-19T05:37:56"), hasAmount("EUR", 0.43), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-19T05:57:28"), hasAmount("EUR", 0.50), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-19T06:01:20"), hasAmount("EUR", 0.43), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-19T06:19:48"), hasAmount("EUR", 0.13), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-21T13:02:58"), hasAmount("EUR", 0.20), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-21T21:05:16"), hasAmount("EUR", 0.86), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-23T11:16:36"), hasAmount("EUR", 0.57), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-24T08:30:34"), hasAmount("EUR", 0.08), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-26T15:31:53"), hasAmount("EUR", 0.48), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-26T16:46:02"), hasAmount("EUR", 1.00), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-27T09:51:32"), hasAmount("EUR", 0.57), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-27T16:02:34"), hasAmount("EUR", 0.51), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-28T10:07:14"), hasAmount("EUR", 0.86), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2026-05-29T16:17:42"), hasAmount("EUR", 0.10), //
                        hasSource("AccountStatement02.txt"), hasNote(null))));
    }

    @Test
    public void testAccountStatement03()
    {
        var extractor = new RevolutLtdPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(13));
        new AssertImportActions().check(results, "EUR");

        // check security (still held --> enriched with name and ISIN from portfolio breakdown)
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BM67HV82"), hasWkn(null), hasTicker("XDWI"), //
                        hasName("Xtrackers MSCI World Industrials UCITS ETF (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check security (sold before period end --> ticker symbol only)
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("QDVY"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        // check security (sold before period end --> ticker symbol only)
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XDWT"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2089238898"), hasWkn(null), hasTicker("PRAR"), //
                        hasName("Amundi Prime Euro Govies UCITS ETF DR (Acc)"), //
                        hasCurrencyCode("EUR"))));

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("EXI2"), //
                        hasName(null), //
                        hasCurrencyCode("EUR"))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-03-06T10:03:13"), hasShares(0.10614448), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 7.67), hasGrossValue("EUR", 7.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check sale transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-03-05T13:00:53"), hasShares(0.81203982), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 78.56), hasGrossValue("EUR", 78.56), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-11-29T17:47:56"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.05), hasGrossValue("EUR", 0.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividend transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-12-17T17:40:20"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-09-29T10:03:54"), hasAmount("EUR", 0.10), //
                        hasSource("AccountStatement03.txt"), hasNote(null))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2025-06-07T01:35:13"), hasAmount("EUR", 1.00), //
                        hasSource("AccountStatement03.txt"), hasNote(null))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-10-29T00:54:48"), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote("Robo management fee"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check purchase transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-03-06T10:06:29"), hasShares(2.04187328), //
                        hasSource("AccountStatement03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 37.06), hasGrossValue("EUR", 37.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }
}
