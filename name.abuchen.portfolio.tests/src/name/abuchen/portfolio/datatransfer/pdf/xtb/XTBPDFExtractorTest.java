package name.abuchen.portfolio.datatransfer.pdf.xtb;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSecurity;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
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

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.XTBPDFExtractor;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class XTBPDFExtractorTest
{
    @Test
    public void testAccountStatement01()
    {
        var extractor = new XTBPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(17L));
        assertThat(countBuySell(results), is(137L));
        assertThat(countAccountTransactions(results), is(22L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(58L));
        assertThat(results.size(), is(234));
        new AssertImportActions().check(results, "EUR");

        // check security 1
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("P911.DE"), //
                        hasName("P911.DE"), //
                        hasCurrencyCode("EUR"))));

        // check security 2
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("MC.FR"), //
                        hasName("MC.FR"), //
                        hasCurrencyCode("EUR"))));

        // check security 3
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("AIR.FR"), //
                        hasName("AIR.FR"), //
                        hasCurrencyCode("EUR"))));

        // check security 4
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("TUI.DE"), //
                        hasName("TUI.DE"), //
                        hasCurrencyCode("EUR"))));

        // check security 5
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VT0P.DE"), //
                        hasName("VT0P.DE"), //
                        hasCurrencyCode("EUR"))));

        // check security 6
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ASWC.DE"), //
                        hasName("ASWC.DE"), //
                        hasCurrencyCode("EUR"))));

        // check security 7
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("DFEN.DE"), //
                        hasName("DFEN.DE"), //
                        hasCurrencyCode("EUR"))));

        // check security 8
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("DFNS.UK"), //
                        hasName("DFNS.UK"), //
                        hasCurrencyCode("EUR"))));

        // check security 9
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("VWCG.DE"), //
                        hasName("VWCG.DE"), //
                        hasCurrencyCode("EUR"))));

        // check security 10
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("DAXEX.DE"), //
                        hasName("DAXEX.DE"), //
                        hasCurrencyCode("EUR"))));

        // check security 11
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("IMAE.NL"), //
                        hasName("IMAE.NL"), //
                        hasCurrencyCode("EUR"))));

        // check security 12
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("MEUD.FR"), //
                        hasName("MEUD.FR"), //
                        hasCurrencyCode("EUR"))));

        // check security 13
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("SPY4.DE"), //
                        hasName("SPY4.DE"), //
                        hasCurrencyCode("EUR"))));

        // check security 14
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("XDEW.DE"), //
                        hasName("XDEW.DE"), //
                        hasCurrencyCode("EUR"))));

        // check security 15
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("ZPRV.DE"), //
                        hasName("ZPRV.DE"), //
                        hasCurrencyCode("EUR"))));

        // check security 16
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("2B76.DE"), //
                        hasName("2B76.DE"), //
                        hasCurrencyCode("EUR"))));

        // check security 17
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn(null), hasTicker("2B7B.DE"), //
                        hasName("2B7B.DE"), //
                        hasCurrencyCode("EUR"))));

        // check deposit transaction 1
        assertThat(results, hasItem(deposit(hasDate("2025-04-15T20:45:14"), hasAmount("EUR", 3110.00), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));

        // check deposit transaction 2
        assertThat(results, hasItem(deposit(hasDate("2025-04-22T11:50:02"), hasAmount("EUR", 399.00), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));

        // check deposit transaction 3
        assertThat(results, hasItem(deposit(hasDate("2025-04-22T17:47:32"), hasAmount("EUR", 3588.00), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));

        // check deposit transaction 4
        assertThat(results, hasItem(deposit(hasDate("2025-05-02T09:52:29"), hasAmount("EUR", 2009.00), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));

        // check deposit transaction 5
        assertThat(results, hasItem(deposit(hasDate("2025-06-19T13:00:48"), hasAmount("EUR", 1224.00), //
                        hasSource("AccountStatement01.txt"), hasNote(null))));

        // check skipped transfer transaction 1
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-04-15T20:55:01"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 2
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-04-25T13:55:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 3
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-04-28T17:18:15"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 4
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-04-29T09:38:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 5
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-04-29T16:00:50"), hasAmount("EUR", 350.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52146619")))));

        // check skipped transfer transaction 6
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-05-05T11:12:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 7
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-05-05T21:32:54"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 8
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-05-06T10:08:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 9
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-05-12T10:58:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 10
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-05-12T11:04:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 11
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-05-13T10:02:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 12
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-05-19T10:42:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 13
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-05-19T10:50:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 14
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-05-20T10:14:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 15
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-05-26T10:44:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 16
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-05-26T10:40:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 17
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-05-29T08:50:00"), hasAmount("EUR", 350.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52146619")))));

        // check skipped transfer transaction 18
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-06-02T10:52:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 19
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-06-02T11:02:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 20
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-06-03T10:00:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 21
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-06-09T14:35:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 22
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-06-09T14:40:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 23
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-06-10T10:16:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 24
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-06-16T10:38:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 25
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-06-16T10:44:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 26
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-06-17T13:50:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 27
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-06-23T10:24:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 28
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-06-23T10:30:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 29
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        removal(hasDate("2025-06-24T09:36:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 30
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-04-15T20:55:01"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 31
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-04-25T13:55:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 32
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-04-29T09:38:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 33
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-05-06T10:08:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 34
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-05-13T10:02:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 35
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-05-20T10:14:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 36
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-06-03T10:00:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 37
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-06-10T10:16:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 38
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-06-17T13:50:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 39
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-06-24T09:36:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52092041")))));

        // check skipped transfer transaction 40
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-04-28T17:18:15"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 41
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-05-05T11:12:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 42
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-05-12T10:58:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 43
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-05-19T10:42:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 44
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-05-26T10:40:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 45
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-06-02T10:52:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 46
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-06-09T14:35:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 47
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-06-16T10:38:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 48
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-06-23T10:24:00"), hasAmount("EUR", 150.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52142804")))));

        // check skipped transfer transaction 49
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-04-29T16:00:50"), hasAmount("EUR", 350.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52146619")))));

        // check skipped transfer transaction 50
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-05-29T08:50:00"), hasAmount("EUR", 350.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52146619")))));

        // check skipped transfer transaction 51
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-05-05T21:32:54"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 52
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-05-12T11:04:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 53
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-05-19T10:50:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 54
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-05-26T10:44:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 55
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-06-02T11:02:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 56
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-06-09T14:40:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 57
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-06-16T10:44:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check skipped transfer transaction 58
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        deposit(hasDate("2025-06-23T10:30:00"), hasAmount("EUR", 50.00), //
                                        hasSource("AccountStatement01.txt"), hasNote("Transfer from 52090475 to 52172086")))));

        // check buy sell transaction 1
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-16T09:00:01"), hasShares(11.00), //
                        hasSecurity(hasTicker("P911.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 477.40), hasGrossValue("EUR", 477.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 2
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-16T09:02:18"), hasShares(0.4233), //
                        hasSecurity(hasTicker("P911.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 18.34), hasGrossValue("EUR", 18.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 3
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-16T10:52:47"), hasShares(0.0306), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.85), hasGrossValue("EUR", 14.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 4
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-16T10:52:48"), hasShares(1.00), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 485.35), hasGrossValue("EUR", 485.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 5
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-17T09:00:30"), hasShares(0.6185), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 299.69), hasGrossValue("EUR", 299.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 6
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-22T11:50:43"), hasShares(0.3509), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 167.34), hasGrossValue("EUR", 167.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 7
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-23T09:00:24"), hasShares(3.00), //
                        hasSecurity(hasTicker("AIR.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 406.50), hasGrossValue("EUR", 406.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 8
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-23T10:46:28"), hasShares(1.00), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 505.80), hasGrossValue("EUR", 505.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 9
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-30T14:52:52"), hasShares(50.00), //
                        hasSecurity(hasTicker("TUI.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 335.00), hasGrossValue("EUR", 335.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 10
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-06T12:07:58"), hasShares(1.00), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 485.75), hasGrossValue("EUR", 485.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 11
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-22T10:04:36"), hasShares(0.5767), //
                        hasSecurity(hasTicker("P911.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.17), hasGrossValue("EUR", 25.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 12
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-22T10:04:38"), hasShares(12.00), //
                        hasSecurity(hasTicker("P911.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 523.68), hasGrossValue("EUR", 523.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 13
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-03T12:59:29"), hasShares(7.00), //
                        hasSecurity(hasTicker("P911.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 288.75), hasGrossValue("EUR", 288.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 14
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-12T10:19:45"), hasShares(1.00), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 463.45), hasGrossValue("EUR", 463.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 15
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-13T09:02:19"), hasShares(17.00), //
                        hasSecurity(hasTicker("TUI.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 106.79), hasGrossValue("EUR", 106.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 16
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-19T13:24:57"), hasShares(1.00), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 454.80), hasGrossValue("EUR", 454.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 17
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-20T09:00:24"), hasShares(60.00), //
                        hasSecurity(hasTicker("TUI.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 382.08), hasGrossValue("EUR", 382.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 18
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-23T09:43:40"), hasShares(20.00), //
                        hasSecurity(hasTicker("VT0P.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 157.30), hasGrossValue("EUR", 157.30), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 19
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-16T09:04:09"), hasShares(3.00), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 40.10), hasGrossValue("EUR", 40.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 20
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-16T09:04:09"), hasShares(0.6366), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.52), hasGrossValue("EUR", 8.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 21
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-16T09:04:08"), hasShares(0.1601), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 6.72), hasGrossValue("EUR", 6.72), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 22
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-16T09:04:09"), hasShares(1.00), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 41.92), hasGrossValue("EUR", 41.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 23
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-16T09:00:07"), hasShares(1.00), //
                        hasSecurity(hasTicker("DFNS.UK")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 42.06), hasGrossValue("EUR", 42.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 24
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-25T13:55:09"), hasShares(0.6528), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.76), hasGrossValue("EUR", 8.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 25
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-25T13:55:09"), hasShares(0.1495), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 6.37), hasGrossValue("EUR", 6.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 26
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-25T13:55:10"), hasShares(3.00), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 40.24), hasGrossValue("EUR", 40.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 27
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-25T13:55:10"), hasShares(1.00), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 42.63), hasGrossValue("EUR", 42.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 28
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-25T13:55:10"), hasShares(1.00), //
                        hasSecurity(hasTicker("DFNS.UK")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 42.86), hasGrossValue("EUR", 42.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 29
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-29T09:38:08"), hasShares(0.7211), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 31.10), hasGrossValue("EUR", 31.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 30
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-29T09:38:09"), hasShares(1.00), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 43.13), hasGrossValue("EUR", 43.13), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 31
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-29T09:38:09"), hasShares(5.00), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 68.35), hasGrossValue("EUR", 68.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 32
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-29T09:38:08"), hasShares(0.4316), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.90), hasGrossValue("EUR", 5.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 33
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-06T10:08:06"), hasShares(1.00), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.58), hasGrossValue("EUR", 44.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 34
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-06T10:08:05"), hasShares(0.2414), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.42), hasGrossValue("EUR", 3.42), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 35
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-06T10:08:06"), hasShares(5.00), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 70.83), hasGrossValue("EUR", 70.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 36
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-06T10:08:05"), hasShares(0.6655), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.67), hasGrossValue("EUR", 29.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 37
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-13T10:02:09"), hasShares(0.1634), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2.35), hasGrossValue("EUR", 2.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 38
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-13T10:02:10"), hasShares(5.00), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 71.90), hasGrossValue("EUR", 71.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 39
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-13T10:02:09"), hasShares(0.6536), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.34), hasGrossValue("EUR", 29.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 40
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-13T10:02:10"), hasShares(1.00), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.90), hasGrossValue("EUR", 44.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 41
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-20T10:14:08"), hasShares(4.00), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 59.54), hasGrossValue("EUR", 59.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 42
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-20T10:14:08"), hasShares(0.5823), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 27.32), hasGrossValue("EUR", 27.32), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 43
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-20T10:14:08"), hasShares(1.00), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 46.93), hasGrossValue("EUR", 46.93), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 44
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-20T10:14:08"), hasShares(0.9885), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.71), hasGrossValue("EUR", 14.71), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 45
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-03T10:00:08"), hasShares(0.8919), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.54), hasGrossValue("EUR", 13.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 46
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-03T10:00:08"), hasShares(0.574), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 27.08), hasGrossValue("EUR", 27.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 47
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-03T10:00:09"), hasShares(4.00), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 60.73), hasGrossValue("EUR", 60.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 48
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-03T10:00:09"), hasShares(1.00), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 47.17), hasGrossValue("EUR", 47.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 49
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-10T10:16:09"), hasShares(1.00), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 47.66), hasGrossValue("EUR", 47.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 50
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-10T10:16:09"), hasShares(0.5579), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.59), hasGrossValue("EUR", 26.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 51
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-10T10:16:09"), hasShares(0.8631), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.17), hasGrossValue("EUR", 13.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 52
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-10T10:16:09"), hasShares(4.00), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 61.06), hasGrossValue("EUR", 61.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 53
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-17T13:50:07"), hasShares(1.00), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 47.87), hasGrossValue("EUR", 47.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 54
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-17T13:50:07"), hasShares(4.00), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 61.05), hasGrossValue("EUR", 61.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 55
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-17T13:50:05"), hasShares(0.865), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.20), hasGrossValue("EUR", 13.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 56
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-17T13:50:05"), hasShares(0.5512), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 26.38), hasGrossValue("EUR", 26.38), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 57
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-24T09:36:08"), hasShares(0.8624), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 13.17), hasGrossValue("EUR", 13.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 58
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-24T09:36:10"), hasShares(4.00), //
                        hasSecurity(hasTicker("ASWC.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 61.08), hasGrossValue("EUR", 61.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 59
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-24T09:36:10"), hasShares(1.00), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 46.86), hasGrossValue("EUR", 46.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 60
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-24T09:36:08"), hasShares(0.5845), //
                        hasSecurity(hasTicker("DFEN.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 27.39), hasGrossValue("EUR", 27.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 61
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-28T17:18:20"), hasShares(0.6343), //
                        hasSecurity(hasTicker("VWCG.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.70), hasGrossValue("EUR", 29.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 62
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-28T17:18:20"), hasShares(0.2801), //
                        hasSecurity(hasTicker("DAXEX.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 51.95), hasGrossValue("EUR", 51.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 63
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-28T17:18:20"), hasShares(0.4532), //
                        hasSecurity(hasTicker("IMAE.NL")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 37.11), hasGrossValue("EUR", 37.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 64
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-28T17:18:20"), hasShares(0.1199), //
                        hasSecurity(hasTicker("MEUD.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.69), hasGrossValue("EUR", 29.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 65
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-05T11:12:09"), hasShares(0.44), //
                        hasSecurity(hasTicker("IMAE.NL")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 37.11), hasGrossValue("EUR", 37.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 66
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-05T11:12:09"), hasShares(0.1168), //
                        hasSecurity(hasTicker("MEUD.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.70), hasGrossValue("EUR", 29.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 67
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-05T11:12:09"), hasShares(0.2701), //
                        hasSecurity(hasTicker("DAXEX.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 51.96), hasGrossValue("EUR", 51.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 68
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-05T11:12:09"), hasShares(0.6182), //
                        hasSecurity(hasTicker("VWCG.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.70), hasGrossValue("EUR", 29.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 69
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-12T10:58:09"), hasShares(0.1151), //
                        hasSecurity(hasTicker("MEUD.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.68), hasGrossValue("EUR", 29.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 70
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-12T10:58:09"), hasShares(0.4348), //
                        hasSecurity(hasTicker("IMAE.NL")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 37.11), hasGrossValue("EUR", 37.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 71
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-12T10:58:09"), hasShares(0.6094), //
                        hasSecurity(hasTicker("VWCG.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.70), hasGrossValue("EUR", 29.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 72
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-12T10:58:09"), hasShares(0.2637), //
                        hasSecurity(hasTicker("DAXEX.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 51.97), hasGrossValue("EUR", 51.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 73
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-19T10:42:10"), hasShares(0.6035), //
                        hasSecurity(hasTicker("VWCG.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.70), hasGrossValue("EUR", 29.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 74
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-19T10:42:10"), hasShares(0.4309), //
                        hasSecurity(hasTicker("IMAE.NL")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 37.11), hasGrossValue("EUR", 37.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 75
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-19T10:42:10"), hasShares(0.114), //
                        hasSecurity(hasTicker("MEUD.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.70), hasGrossValue("EUR", 29.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 76
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-19T10:42:10"), hasShares(0.2641), //
                        hasSecurity(hasTicker("DAXEX.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 51.95), hasGrossValue("EUR", 51.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 77
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-26T10:40:06"), hasShares(0.7914), //
                        hasSecurity(hasTicker("VWCG.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 39.35), hasGrossValue("EUR", 39.35), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 78
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-26T10:40:07"), hasShares(1.00), //
                        hasSecurity(hasTicker("VWCG.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 49.73), hasGrossValue("EUR", 49.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 79
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-02T10:52:08"), hasShares(0.1135), //
                        hasSecurity(hasTicker("MEUD.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.70), hasGrossValue("EUR", 29.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 80
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-02T10:52:08"), hasShares(0.601), //
                        hasSecurity(hasTicker("VWCG.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.70), hasGrossValue("EUR", 29.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 81
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-02T10:52:08"), hasShares(0.4298), //
                        hasSecurity(hasTicker("IMAE.NL")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 37.10), hasGrossValue("EUR", 37.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 82
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-02T10:52:08"), hasShares(0.2622), //
                        hasSecurity(hasTicker("DAXEX.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 51.96), hasGrossValue("EUR", 51.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 83
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-09T14:35:06"), hasShares(0.2597), //
                        hasSecurity(hasTicker("DAXEX.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 51.97), hasGrossValue("EUR", 51.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 84
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-09T14:35:06"), hasShares(0.5957), //
                        hasSecurity(hasTicker("VWCG.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.70), hasGrossValue("EUR", 29.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 85
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-09T14:35:06"), hasShares(0.1125), //
                        hasSecurity(hasTicker("MEUD.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.70), hasGrossValue("EUR", 29.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 86
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-09T14:35:06"), hasShares(0.4255), //
                        hasSecurity(hasTicker("IMAE.NL")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 37.11), hasGrossValue("EUR", 37.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 87
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-16T10:38:09"), hasShares(0.4305), //
                        hasSecurity(hasTicker("IMAE.NL")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 37.12), hasGrossValue("EUR", 37.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 88
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-16T10:38:08"), hasShares(0.6023), //
                        hasSecurity(hasTicker("VWCG.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.70), hasGrossValue("EUR", 29.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 89
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-16T10:38:09"), hasShares(0.1137), //
                        hasSecurity(hasTicker("MEUD.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.68), hasGrossValue("EUR", 29.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 90
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-16T10:38:09"), hasShares(0.2657), //
                        hasSecurity(hasTicker("DAXEX.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 51.97), hasGrossValue("EUR", 51.97), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 91
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-23T10:24:10"), hasShares(0.4317), //
                        hasSecurity(hasTicker("IMAE.NL")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 36.75), hasGrossValue("EUR", 36.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 92
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-23T10:24:10"), hasShares(0.6124), //
                        hasSecurity(hasTicker("VWCG.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.70), hasGrossValue("EUR", 29.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 93
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-23T10:24:10"), hasShares(0.1156), //
                        hasSecurity(hasTicker("MEUD.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 29.68), hasGrossValue("EUR", 29.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 94
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-23T10:24:10"), hasShares(0.2683), //
                        hasSecurity(hasTicker("DAXEX.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 51.96), hasGrossValue("EUR", 51.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 95
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-29T16:00:56"), hasShares(1.00), //
                        hasSecurity(hasTicker("SPY4.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 76.43), hasGrossValue("EUR", 76.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 96
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-29T16:00:56"), hasShares(1.00), //
                        hasSecurity(hasTicker("XDEW.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 80.41), hasGrossValue("EUR", 80.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 97
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-29T16:00:55"), hasShares(0.2304), //
                        hasSecurity(hasTicker("ZPRV.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.36), hasGrossValue("EUR", 12.36), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 98
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-29T16:00:55"), hasShares(0.133), //
                        hasSecurity(hasTicker("SPY4.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 10.17), hasGrossValue("EUR", 10.17), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 99
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-04-29T16:00:56"), hasShares(3.00), //
                        hasSecurity(hasTicker("ZPRV.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 160.89), hasGrossValue("EUR", 160.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 100
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-29T09:04:02"), hasShares(1.00), //
                        hasSecurity(hasTicker("XDEW.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 86.27), hasGrossValue("EUR", 86.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 101
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-29T09:09:49"), hasShares(3.00), //
                        hasSecurity(hasTicker("ZPRV.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 176.22), hasGrossValue("EUR", 176.22), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 102
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-29T09:09:35"), hasShares(1.00), //
                        hasSecurity(hasTicker("SPY4.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 83.53), hasGrossValue("EUR", 83.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 103
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-29T09:09:48"), hasShares(0.0052), //
                        hasSecurity(hasTicker("ZPRV.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.31), hasGrossValue("EUR", 0.31), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 104
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-29T09:09:35"), hasShares(0.0531), //
                        hasSecurity(hasTicker("SPY4.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.44), hasGrossValue("EUR", 4.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 105
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-06T09:04:01"), hasShares(0.077), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.92), hasGrossValue("EUR", 0.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 106
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-06T09:04:04"), hasShares(2.00), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 23.69), hasGrossValue("EUR", 23.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 107
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-06T09:04:22"), hasShares(0.0116), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.10), hasGrossValue("EUR", 0.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 108
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-06T09:04:24"), hasShares(3.00), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 24.52), hasGrossValue("EUR", 24.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 109
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-12T11:04:08"), hasShares(0.9145), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 7.76), hasGrossValue("EUR", 7.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 110
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-12T11:04:09"), hasShares(2.00), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.96), hasGrossValue("EUR", 16.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 111
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-12T11:04:09"), hasShares(1.00), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.71), hasGrossValue("EUR", 12.71), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 112
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-12T11:04:08"), hasShares(0.9466), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.04), hasGrossValue("EUR", 12.04), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 113
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-19T10:50:08"), hasShares(0.9765), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.23), hasGrossValue("EUR", 12.23), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 114
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-19T10:50:08"), hasShares(0.9338), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 7.87), hasGrossValue("EUR", 7.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 115
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-19T10:50:10"), hasShares(1.00), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.52), hasGrossValue("EUR", 12.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 116
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-19T10:50:09"), hasShares(2.00), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.87), hasGrossValue("EUR", 16.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 117
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-26T10:44:06"), hasShares(0.9863), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.29), hasGrossValue("EUR", 12.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 118
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-26T10:44:06"), hasShares(0.958), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.01), hasGrossValue("EUR", 8.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 119
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-26T10:44:06"), hasShares(1.00), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.46), hasGrossValue("EUR", 12.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 120
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-05-26T10:44:06"), hasShares(2.00), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.73), hasGrossValue("EUR", 16.73), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 121
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-02T11:02:10"), hasShares(2.00), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 24.54), hasGrossValue("EUR", 24.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 122
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-02T11:02:08"), hasShares(0.0167), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.20), hasGrossValue("EUR", 0.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 123
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-02T11:02:10"), hasShares(2.00), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.70), hasGrossValue("EUR", 16.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 124
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-02T11:02:08"), hasShares(0.964), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.05), hasGrossValue("EUR", 8.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 125
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-09T14:40:06"), hasShares(2.00), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.96), hasGrossValue("EUR", 16.96), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 126
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-09T14:40:06"), hasShares(0.9488), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.05), hasGrossValue("EUR", 12.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 127
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-09T14:40:06"), hasShares(0.9193), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 7.79), hasGrossValue("EUR", 7.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 128
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-09T14:40:06"), hasShares(1.00), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.70), hasGrossValue("EUR", 12.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 129
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-16T10:44:08"), hasShares(0.9722), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.10), hasGrossValue("EUR", 8.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 130
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-16T10:44:10"), hasShares(1.00), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.50), hasGrossValue("EUR", 12.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 131
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-16T10:44:08"), hasShares(0.98), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.25), hasGrossValue("EUR", 12.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 132
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-16T10:44:10"), hasShares(2.00), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.65), hasGrossValue("EUR", 16.65), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 133
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-23T10:30:13"), hasShares(2.00), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.61), hasGrossValue("EUR", 16.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 134
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-23T10:30:08"), hasShares(0.9838), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.27), hasGrossValue("EUR", 12.27), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 135
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-23T10:30:08"), hasShares(0.9765), //
                        hasSecurity(hasTicker("2B7B.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.12), hasGrossValue("EUR", 8.12), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 136
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-06-23T10:30:13"), hasShares(1.00), //
                        hasSecurity(hasTicker("2B76.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 12.48), hasGrossValue("EUR", 12.48), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check buy sell transaction 137
        assertThat(results, hasItem(sale( //
                        hasDate("2025-08-11T09:14:43"), hasShares(20.00), //
                        hasSecurity(hasTicker("VT0P.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 227.46), hasGrossValue("EUR", 227.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividend transaction 1
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-28T11:46:04"), hasExDate(null), hasShares(0.00), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3.48), hasGrossValue("EUR", 4.64), //
                        hasTaxes("EUR", 1.16), hasFees("EUR", 0.00))));

        // check dividend transaction 2
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-28T11:46:05"), hasExDate(null), hasShares(0.00), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.62), hasGrossValue("EUR", 7.50), //
                        hasTaxes("EUR", 1.88), hasFees("EUR", 0.00))));

        // check dividend transaction 3
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-28T11:46:06"), hasExDate(null), hasShares(0.00), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.62), hasGrossValue("EUR", 7.50), //
                        hasTaxes("EUR", 1.88), hasFees("EUR", 0.00))));

        // check dividend transaction 4
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-28T11:46:08"), hasExDate(null), hasShares(0.00), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.17), hasGrossValue("EUR", 0.23), //
                        hasTaxes("EUR", 0.06), hasFees("EUR", 0.00))));

        // check dividend transaction 5
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-04-28T11:46:09"), hasExDate(null), hasShares(0.00), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.97), hasGrossValue("EUR", 2.63), //
                        hasTaxes("EUR", 0.66), hasFees("EUR", 0.00))));

        // check dividend transaction 6
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-27T16:00:04"), hasExDate(null), hasShares(0.00), //
                        hasSecurity(hasTicker("P911.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 6.61), hasGrossValue("EUR", 8.98), //
                        hasTaxes("EUR", 2.37), hasFees("EUR", 0.00))));

        // check dividend transaction 7
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-27T16:00:05"), hasExDate(null), hasShares(0.00), //
                        hasSecurity(hasTicker("P911.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.26), hasGrossValue("EUR", 0.35), //
                        hasTaxes("EUR", 0.09), hasFees("EUR", 0.00))));

        // check dividend transaction 8
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-27T16:02:05"), hasExDate(null), hasShares(0.00), //
                        hasSecurity(hasTicker("P911.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 0.63), hasGrossValue("EUR", 0.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check dividend transaction 9
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-05-27T16:02:05"), hasExDate(null), hasShares(0.00), //
                        hasSecurity(hasTicker("P911.DE")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.43), hasGrossValue("EUR", 16.43), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction 1
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-04-17T15:27:58"), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote("FTT France adj MC.FR 20250416"), //
                        hasAmount("EUR", 1.46), hasGrossValue("EUR", 1.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction 2
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-05-07T15:51:26"), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote("FTT France adj MC.FR 20250506"), //
                        hasAmount("EUR", 1.46), hasGrossValue("EUR", 1.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction 3
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-06-13T15:41:57"), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote("FTT France adj MC.FR 20250612"), //
                        hasAmount("EUR", 1.85), hasGrossValue("EUR", 1.85), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check taxes transaction 4
        assertThat(results, hasItem(taxes( //
                        hasDate("2025-06-20T15:21:20"), //
                        hasSecurity(hasTicker("MC.FR")), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote("FTT France adj MC.FR 20250619"), //
                        hasAmount("EUR", 1.82), hasGrossValue("EUR", 1.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check interest transaction 1
        assertThat(results, hasItem(interest( //
                        hasDate("2025-05-04T16:43:00"), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote("Free-funds Interest 2025-04"), //
                        hasAmount("EUR", 0.53), hasGrossValue("EUR", 0.62), //
                        hasTaxes("EUR", 0.09), hasFees("EUR", 0.00))));

        // check interest transaction 2
        assertThat(results, hasItem(interest( //
                        hasDate("2025-06-01T10:51:41"), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote("Free-funds Interest 2025-05"), //
                        hasAmount("EUR", 0.70), hasGrossValue("EUR", 0.82), //
                        hasTaxes("EUR", 0.12), hasFees("EUR", 0.00))));

        // check interest transaction 3
        assertThat(results, hasItem(interest( //
                        hasDate("2025-07-03T15:22:42"), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote("Free-funds Interest 2025-06"), //
                        hasAmount("EUR", 0.16), hasGrossValue("EUR", 0.19), //
                        hasTaxes("EUR", 0.03), hasFees("EUR", 0.00))));

        // check interest transaction 4
        assertThat(results, hasItem(interest( //
                        hasDate("2025-08-05T15:14:41"), //
                        hasSource("AccountStatement01.txt"), //
                        hasNote("Free-funds Interest 2025-07"), //
                        hasAmount("EUR", 0.30), hasGrossValue("EUR", 0.35), //
                        hasTaxes("EUR", 0.05), hasFees("EUR", 0.00))));
    }
}
