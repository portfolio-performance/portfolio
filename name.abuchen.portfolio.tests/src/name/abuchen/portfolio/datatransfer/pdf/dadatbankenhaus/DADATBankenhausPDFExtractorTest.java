package name.abuchen.portfolio.datatransfer.pdf.dadatbankenhaus;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundCash;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundCash;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.DADATBankenhausPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class DADATBankenhausPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

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
                        hasIsin("US09247X1019"), hasWkn(null), hasTicker(null), //
                        hasName("B L A C K R O C K  I NC. Reg. Shares Class A DL -,01"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-17T20:49:54"), hasShares(3.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Auftrags-Nr.: 45247499"), //
                        hasAmount("EUR", 1800.00), hasGrossValue("EUR", 1800.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

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
                        hasIsin("NL0011794037"), hasWkn(null), hasTicker(null), //
                        hasName("AHOLD DELHAIZE,KON.EO-,01"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2023-12-27T00:00"), hasShares(40.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1053.18), hasGrossValue("EUR", 1046.40), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.78))));
    }

    @Test
    public void testWertpapierStornoVerkauf01()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "StornoKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(7L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(7L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(14));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US00206R1023"), hasWkn(null), hasTicker(null), //
                        hasName("AT + T INC. DL 1"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US7427181091"), hasWkn(null), hasTicker(null), //
                        hasName("PROCTER GAMBLE"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn(null), hasTicker(null), //
                        hasName("MICROSOFT DL-,00000625"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US1912161007"), hasWkn(null), hasTicker(null), //
                        hasName("COCA-COLA CO. DL-,25"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasWkn(null), hasTicker(null), //
                        hasName("APPLE INC."), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US92826C8394"), hasWkn(null), hasTicker(null), //
                        hasName("VISA INC. CL. A DL -,0001"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US4781601046"), hasWkn(null), hasTicker(null), //
                        hasName("JOHNSON + JOHNSON DL 1"), //
                        hasCurrencyCode("USD"))));

        // check 1st cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        purchase( //
                                        hasDate("2021-08-23T00:00"), hasShares(2.91), //
                                        hasSource("StornoKontoauszug01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 71.45), hasGrossValue("EUR", 71.45), //
                                        hasForexGrossValue("USD", 81.86), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 2nd cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        purchase( //
                                        hasDate("2021-08-23T00:00"), hasShares(0.57), //
                                        hasSource("StornoKontoauszug01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 71.16), hasGrossValue("EUR", 71.16), //
                                        hasForexGrossValue("USD", 82.08), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 3rd cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        purchase( //
                                        hasDate("2021-08-23T00:00"), hasShares(0.28), //
                                        hasSource("StornoKontoauszug01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 71.04), hasGrossValue("EUR", 71.04), //
                                        hasForexGrossValue("USD", 82.04), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 4th cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        purchase( //
                                        hasDate("2021-08-23T00:00"), hasShares(1.43), //
                                        hasSource("StornoKontoauszug01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 71.02), hasGrossValue("EUR", 71.02), //
                                        hasForexGrossValue("USD", 81.80), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 5th cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        purchase( //
                                        hasDate("2021-08-23T00:00"), hasShares(0.55), //
                                        hasSource("StornoKontoauszug01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 70.82), hasGrossValue("EUR", 70.82), //
                                        hasForexGrossValue("USD", 81.73), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 6th cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        purchase( //
                                        hasDate("2021-08-23T00:00"), hasShares(0.35), //
                                        hasSource("StornoKontoauszug01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 70.52), hasGrossValue("EUR", 70.52), //
                                        hasForexGrossValue("USD", 81.27), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        // check 7th cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        purchase( //
                                        hasDate("2021-08-23T00:00"), hasShares(0.46), //
                                        hasSource("StornoKontoauszug01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 70.43), hasGrossValue("EUR", 70.43), //
                                        hasForexGrossValue("USD", 81.21), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKontoauszug01()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0378449770"), hasWkn(null), hasTicker(null), //
                        hasName("COMST.-NASDAQ-100 U.ETF I"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU0392494562"), hasWkn(null), hasTicker(null), //
                        hasName("COMS.-MSCI WORL.T.U.ETF I"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-16T00:00"), hasShares(1.22), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 99.68), hasGrossValue("EUR", 98.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.67))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-16T00:00"), hasShares(1.68), //
                        hasSource("Kontoauszug01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 100.29), hasGrossValue("EUR", 98.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.67))));
    }

    @Test
    public void testKontoauszug02()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

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
                        hasIsin("IE00BKM4GZ66"), hasWkn(null), hasTicker(null), //
                        hasName("IS C.MSCI EMIMI U.ETF DLA"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-02-22T00:00"), hasShares(25.00), //
                        hasSource("Kontoauszug02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 817.60), hasGrossValue("EUR", 806.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.66 + 6.36 + 1.00))));
    }

    @Test
    public void testKontoauszug03()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

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
                        hasIsin("US2561631068"), hasWkn(null), hasTicker(null), //
                        hasName("DOCUSIGN INC DL-,0001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-30T00:00"), hasShares(7.00), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1250.01), hasGrossValue("EUR", 1237.57), //
                        hasForexGrossValue("USD", 1448.58), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (5.06 / 1.1705) + 7.12 + 1.00))));
    }

    @Test
    public void testKontoauszug03WithSecurityInEUR()
    {
        var security = new Security("DOCUSIGN INC    DL-,0001", "EUR");
        security.setIsin("US2561631068");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DADATBankenhausPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-07-30T00:00"), hasShares(7.00), //
                        hasSource("Kontoauszug03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1250.01), hasGrossValue("EUR", 1237.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (5.06 / 1.1705) + 7.12 + 1.00))));
    }

    @Test
    public void testKontoauszug04()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

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
                        hasIsin("US2561631068"), hasWkn(null), hasTicker(null), //
                        hasName("DOCUSIGN INC DL-,0001"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-03T00:00"), hasShares(7.00), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1535.94), hasGrossValue("EUR", 1667.24), //
                        hasForexGrossValue("USD", 1979.18), //
                        hasTaxes("EUR", 140.27 / 1.1871), hasFees("EUR", (5.07 / 1.1871) + 7.87 + 1.00))));
    }

    @Test
    public void testKontoauszug04WithSecurityInEUR()
    {
        var security = new Security("DOCUSIGN INC    DL-,0001", "EUR");
        security.setIsin("US2561631068");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DADATBankenhausPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-09-03T00:00"), hasShares(7.00), //
                        hasSource("Kontoauszug04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1535.94), hasGrossValue("EUR", 1667.24), //
                        hasTaxes("EUR", 140.27 / 1.1871), hasFees("EUR", (5.07 / 1.1871) + 7.87 + 1.00))));
    }

    @Test
    public void testKontoauszug05()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("AT0000969985"), hasWkn(null), hasTicker(null), //
                        hasName("AT+S AUST. TECH.SYS.O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2020-07-31T00:00"), hasExDate(null), //
                        hasShares(45.00), //
                        hasSource("Kontoauszug05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.16), hasGrossValue("EUR", 11.25), //
                        hasTaxes("EUR", 3.09), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug06()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US00206R1023"), hasWkn(null), hasTicker(null), //
                        hasName("AT + T INC. DL 1"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US92343V1044"), hasWkn(null), hasTicker(null), //
                        hasName("VERIZON COMM. INC. DL-,10"), //
                        hasCurrencyCode("USD"))));

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-08-03T00:00"), hasExDate(null), //
                        hasShares(200.00), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 63.05), hasGrossValue("EUR", 86.96), //
                        hasForexGrossValue("USD", 104.00), //
                        hasTaxes("EUR", 23.91), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-08-03T00:00"), hasExDate(null), //
                        hasShares(40.00), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 15.22), hasGrossValue("EUR", 20.99), //
                        hasForexGrossValue("USD", 25.10), //
                        hasTaxes("EUR", (3.77 / 1.1959) + (3.13 / 1.1959)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug06WithSecurityInEUR()
    {
        var security1 = new Security("AT + T INC.          DL 1", "EUR");
        security1.setIsin("US00206R1023");

        var security2 = new Security("VERIZON COMM. INC. DL-,10", "EUR");
        security2.setIsin("US92343V1044");

        var client = new Client();
        client.addSecurity(security1);
        client.addSecurity(security2);

        var extractor = new DADATBankenhausPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check 1st dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-08-03T00:00"), hasExDate(null), //
                        hasShares(200.00), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 63.05), hasGrossValue("EUR", 86.96), //
                        hasTaxes("EUR", 23.91), hasFees("EUR", 0.00))));

        // check 2nd dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-08-03T00:00"), hasExDate(null), //
                        hasShares(40.00), //
                        hasSource("Kontoauszug06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 15.22), hasGrossValue("EUR", 20.99), //
                        hasTaxes("EUR", (3.77 / 1.1959) + (3.13 / 1.1959)), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug07()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0378449770"), hasWkn(null), hasTicker(null), //
                        hasName("COMST.-NASDAQ-100 U.ETF I"), //
                        hasCurrencyCode("USD"))));

        // check tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-08T00:00"), hasShares(1.22), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.34), hasGrossValue("EUR", 1.34), //
                        hasForexGrossValue("USD", 1.51), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug07WithSecurityInEUR()
    {
        var security = new Security("COMST.-NASDAQ-100 U.ETF I", "EUR");
        security.setIsin("LU0378449770");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DADATBankenhausPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-08T00:00"), hasShares(1.22), //
                        hasSource("Kontoauszug07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.34), hasGrossValue("EUR", 1.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug08()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0378449770"), hasWkn(null), hasTicker(null), //
                        hasName("COMST.-NASDAQ-100 U.ETF I"), //
                        hasCurrencyCode("USD"))));

        // check tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-09-18T00:00"), hasShares(6.05), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.62), hasGrossValue("EUR", 33.62), //
                        hasForexGrossValue("USD", 39.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug08WithSecurityInEUR()
    {
        var security = new Security("COMST.-NASDAQ-100 U.ETF I", "EUR");
        security.setIsin("LU0378449770");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DADATBankenhausPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-09-18T00:00"), hasShares(6.05), //
                        hasSource("Kontoauszug08.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.62), hasGrossValue("EUR", 33.62), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug09()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2021-03-31T00:00"), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote("Sollzinsen"), //
                        hasAmount("EUR", 4.76), hasGrossValue("EUR", 4.76), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-03-31T00:00"), //
                        hasSource("Kontoauszug09.txt"), //
                        hasNote("Kontoführungsgebühr"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug10()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-01-07T00:00"), //
                        hasSource("Kontoauszug10.txt"), //
                        hasNote("Depotgebührenabrechnung per 31.12.2020"), //
                        hasAmount("EUR", 63.68), hasGrossValue("EUR", 63.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug11()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-06-18T00:00"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug11.txt"), hasNote("Max Muster"))));
    }

    @Test
    public void testKontoauszug12()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0S9GB0"), hasWkn(null), hasTicker(null), //
                        hasName("0% DT.BOERSE COM. XETRA-GOL"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5YC18"), hasWkn(null), hasTicker(null), //
                        hasName("ISHSIII-MSCI EM USD(ACC)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker(null), //
                        hasName("ISHSIII-CORE MSCI WLD DLA"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2020-03-10T00:00"), hasShares(43.00), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1975.04), hasGrossValue("EUR", 2029.60), //
                        hasTaxes("EUR", 38.34), hasFees("EUR", 7.72 + 8.50))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-10T00:00"), hasShares(19.00), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 514.19), hasGrossValue("EUR", 504.66), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.70 + 5.83))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-03-10T00:00"), hasShares(30.00), //
                        hasSource("Kontoauszug12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1422.93), hasGrossValue("EUR", 1416.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 6.03))));
    }

    @Test
    public void testKontoauszug13()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5YC18"), hasWkn(null), hasTicker(null), //
                        hasName("ISHSIII-MSCI EM USD(ACC)"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B4L5Y983"), hasWkn(null), hasTicker(null), //
                        hasName("ISHSIII-CORE MSCI WLD DLA"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B5BMR087"), hasWkn(null), hasTicker(null), //
                        hasName("ISHSVII-CORE S+P500 DLACC"), //
                        hasCurrencyCode("USD"))));

        // check 1st tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-13T00:00"), hasShares(134.00), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.99), hasGrossValue("EUR", 33.99), //
                        hasForexGrossValue("USD", 23.08 + 14.94), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-13T00:00"), hasShares(184.00), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.80), hasGrossValue("EUR", 44.80), //
                        hasForexGrossValue("USD", 11.33 + 38.78), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-13T00:00"), hasShares(45.00), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 95.06), hasGrossValue("EUR", 95.06), //
                        hasForexGrossValue("USD", 75.05 + 31.29 - 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug13WithSecurityInEUR()
    {
        var security1 = new Security("ISHSIII-MSCI EM USD(ACC)", "EUR");
        security1.setIsin("IE00B4L5YC18");

        var security2 = new Security("ISHSIII-CORE MSCI WLD DLA", "EUR");
        security2.setIsin("IE00B4L5Y983");

        var security3 = new Security("ISHSVII-CORE S+P500 DLACC", "EUR");
        security3.setIsin("IE00B5BMR087");

        var client = new Client();
        client.addSecurity(security1);
        client.addSecurity(security2);
        client.addSecurity(security3);

        var extractor = new DADATBankenhausPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check 1st tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-13T00:00"), hasShares(134.00), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.99), hasGrossValue("EUR", 33.99), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-13T00:00"), hasShares(184.00), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.80), hasGrossValue("EUR", 44.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2020-01-13T00:00"), hasShares(45.00), //
                        hasSource("Kontoauszug13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 95.06), hasGrossValue("EUR", 95.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug14()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2018-12-31T00:00"), //
                        hasSource("Kontoauszug14.txt"), //
                        hasNote("Kontoführungsgebühr"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug15()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2019-12-31T00:00"), //
                        hasSource("Kontoauszug15.txt"), //
                        hasNote("Kontoführungsgebühr"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug16()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-06-30T00:00"), //
                        hasSource("Kontoauszug16.txt"), //
                        hasNote("Spesen"), //
                        hasAmount("EUR", 2.53), hasGrossValue("EUR", 2.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug17()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2018-10-31T00:00"), hasAmount("EUR", 75.00), //
                        hasSource("Kontoauszug17.txt"), hasNote("Werbebonus"))));
    }

    @Test
    public void testKontoauszug18()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug18.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-06-18T00:00"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug18.txt"), hasNote("Max Muster"))));
    }

    @Test
    public void testKontoauszug19()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug19.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2019-03-18T00:00"), hasAmount("EUR", 100.00), //
                        hasSource("Kontoauszug19.txt"), hasNote("Max Muster"))));
    }

    @Test
    public void testKontoauszug20()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug20.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check interest transaction
        assertThat(results, hasItem(interest( //
                        hasDate("2021-09-30T00:00"), //
                        hasSource("Kontoauszug20.txt"), //
                        hasNote("Sollzinsen"), //
                        hasAmount("EUR", 0.01), hasGrossValue("EUR", 0.01), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2021-09-30T00:00"), //
                        hasSource("Kontoauszug20.txt"), //
                        hasNote("Spesen"), //
                        hasAmount("EUR", 2.53), hasGrossValue("EUR", 2.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug21()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US92556H2067"), hasWkn(null), hasTicker(null), //
                        hasName("VIACOMCBS INC. BDL-,001"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US1667641005"), hasWkn(null), hasTicker(null), //
                        hasName("CHEVRON CORP. DL-,75"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-15T00:00"), hasShares(110.00), //
                        hasSource("Kontoauszug21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3716.47), hasGrossValue("EUR", 3731.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.70 + 1.10 + 11.48))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2021-10-15T00:00"), hasShares(35.00), //
                        hasSource("Kontoauszug21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 3211.15), hasGrossValue("EUR", 3266.90), //
                        hasTaxes("EUR", 46.78), hasFees("EUR", 7.17 + 1.80))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2021-10-15T00:00"), hasShares(0.00), //
                        hasSource("Kontoauszug21.txt"), //
                        hasNote("KESt-Verlustausgleich"), //
                        hasAmount("EUR", 159.57), hasGrossValue("EUR", 159.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug22()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug22.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(1L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check cash transfer transaction
        assertThat(results, hasItem(outboundCash(hasDate("2021-11-15"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug22.txt"), hasNote(null))));
        assertThat(results, hasItem(inboundCash(hasDate("2021-11-15"), hasAmount("EUR", 2500.00), //
                        hasSource("Kontoauszug22.txt"), hasNote(null))));
    }

    @Test
    public void testKontoauszug23()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug23.txt"), errors);

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
                        hasIsin("IE00B4L5YC18"), hasWkn(null), hasTicker(null), //
                        hasName("ISHSIII-MSCI EM USD(ACC)"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-01-13T00:00"), hasShares(30.00), //
                        hasSource("Kontoauszug23.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1002.31), hasGrossValue("EUR", 991.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.70 + 6.69))));
    }

    @Test
    public void testKontoauszug24()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug24.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BYX2JD69"), hasWkn(null), hasTicker(null), //
                        hasName("ISHSIV-MSCI WLD.SRI U.EOA"), //
                        hasCurrencyCode("EUR"))));

        // check 1st tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2022-11-10T00:00"), hasShares(68.809), //
                        hasSource("Kontoauszug24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.18), hasGrossValue("EUR", 5.18), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd tax transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2022-11-10T00:00"), hasShares(35369.00), //
                        hasSource("Kontoauszug24.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2663.29), hasGrossValue("EUR", 2663.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug25()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug25.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US00206R1023"), hasWkn(null), hasTicker(null), //
                        hasName("AT + T INC. DL 1"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US7672921050"), hasWkn(null), hasTicker(null), //
                        hasName("RIOT PLATFORMS DL-,001"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US9344231041"), hasWkn(null), hasTicker(null), //
                        hasName("WB DISCOVERY SER.A DL-,01"), //
                        hasCurrencyCode("EUR"))));

        // check 1st buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-11-23T00:00"), hasShares(200.00), //
                        hasSource("Kontoauszug25.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2956.65), hasGrossValue("EUR", 2965.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 8.35))));

        // check 2nd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-11-23T00:00"), hasShares(50.00), //
                        hasSource("Kontoauszug25.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 513.92), hasGrossValue("EUR", 518.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.68))));

        // check 3rd buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-11-23T00:00"), hasShares(48.00), //
                        hasSource("Kontoauszug25.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 463.11), hasGrossValue("EUR", 467.71), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.60))));

        // check 1st tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-11-23T00:00"), hasShares(0.00), //
                        hasSource("Kontoauszug25.txt"), //
                        hasNote("KESt-Verlustausgleich"), //
                        hasAmount("EUR", 461.29), hasGrossValue("EUR", 461.29), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 2nd tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-11-23T00:00"), hasShares(0.00), //
                        hasSource("Kontoauszug25.txt"), //
                        hasNote("KESt-Verlustausgleich"), //
                        hasAmount("EUR", 258.64), hasGrossValue("EUR", 258.64), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check 3rd tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2023-11-23T00:00"), hasShares(0.00), //
                        hasSource("Kontoauszug25.txt"), //
                        hasNote("KESt-Verlustausgleich"), //
                        hasAmount("EUR", 196.77), hasGrossValue("EUR", 196.77), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug26()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug26.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US7134481081"), hasWkn(null), hasTicker(null), //
                        hasName("PEPSICO INC. DL-,0166"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-09T00:00"), hasExDate(null), //
                        hasShares(55.00), //
                        hasSource("Kontoauszug26.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 45.97), hasGrossValue("EUR", 63.41), //
                        hasForexGrossValue("USD", 69.58), //
                        hasTaxes("EUR", (10.44 + 8.70) / 1.097300), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug26WithSecurityInEUR()
    {
        var security = new Security("PEPSICO INC. DL-,0166", "EUR");
        security.setIsin("US7134481081");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DADATBankenhausPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug26.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-01-09T00:00"), hasExDate(null), //
                        hasShares(55.00), //
                        hasSource("Kontoauszug26.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 45.97), hasGrossValue("EUR", 63.41), //
                        hasTaxes("EUR", (10.44 + 8.70) / 1.097300), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug27()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug27.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE33AB1CD551"), hasWkn(null), hasTicker(null), //
                        hasName("Y(DR)-XDUI ABCDE 1F"), //
                        hasCurrencyCode("USD"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-07-30T00:00"), hasShares(1123.00), //
                        hasSource("Kontoauszug27.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 28.45), hasGrossValue("EUR", 28.45), //
                        hasForexGrossValue("USD", 31.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug27WithSecurityInEUR()
    {
        var security = new Security("Y(DR)-XDUI ABCDE 1F", "EUR");
        security.setIsin("IE33AB1CD551");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DADATBankenhausPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug27.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-07-30T00:00"), hasShares(1123.00), //
                        hasSource("Kontoauszug27.txt"), hasNote(null), //
                        hasAmount("EUR", 28.45), hasGrossValue("EUR", 28.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKontoauszug28()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug28.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(1L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check cash transfer transaction
        assertThat(results, hasItem(outboundCash(hasDate("2024-01-07"), hasAmount("EUR", 20000.00), //
                        hasSource("Kontoauszug28.txt"), hasNote(null))));
        assertThat(results, hasItem(inboundCash(hasDate("2024-01-07"), hasAmount("EUR", 20000.00), //
                        hasSource("Kontoauszug28.txt"), hasNote(null))));

        // assert transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2025-01-07"), //
                        hasSource("Kontoauszug28.txt"), //
                        hasNote("Depotgebührenabrechnung per 31.12.2024"), //
                        hasAmount("EUR", 277.33), hasGrossValue("EUR", 277.33), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US09247X1019"), hasWkn(null), hasTicker(null), //
                        hasName("B L A C K R O C K  I NC. Reg. Shares Class A DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-23T00:00"), hasExDate("2021-03-04T00:00"), //
                        hasShares(3.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("R.-Nr.: 45970540"), //
                        hasAmount("EUR", 7.51), hasGrossValue("EUR", 10.35), //
                        hasForexGrossValue("USD", 12.39), //
                        hasTaxes("EUR", 2.84), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("B L A C K R O C K  I NC. Reg. Shares Class A DL -,01", "EUR");
        security.setIsin("US09247X1019");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DADATBankenhausPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-03-23T00:00"), hasExDate("2021-03-04T00:00"), //
                        hasShares(3.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote("R.-Nr.: 45970540"), //
                        hasAmount("EUR", 7.51), hasGrossValue("EUR", 10.35), //
                        hasTaxes("EUR", 2.84), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US56035L1044"), hasWkn(null), hasTicker(null), //
                        hasName("M a i n  S t r e e t Capital Corp. Registered Shares DL -,01"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-27T00:00"), hasExDate("2023-12-19T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("R.-Nr.: 84052423"), //
                        hasAmount("EUR", 17.99), hasGrossValue("EUR", 24.82), //
                        hasForexGrossValue("USD", 27.51), //
                        hasTaxes("EUR", (4.13 + 3.44) / 1.1082), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        var security = new Security("M a i n  S t r e e t Capital Corp. Registered Shares DL -,01", "EUR");
        security.setIsin("US56035L1044");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DADATBankenhausPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-27T00:00"), hasExDate("2023-12-19T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote("R.-Nr.: 84052423"), //
                        hasAmount("EUR", 17.99), hasGrossValue("EUR", 24.82), //
                        hasTaxes("EUR", (4.13 + 3.44) / 1.1082), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US02209S1033"), hasWkn(null), hasTicker(null), //
                        hasName("A l t r i a  G r o u p Inc. Registered Shares DL -,333"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-04-30T00:00"), hasExDate("2024-03-22T00:00"), //
                        hasShares(50.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 32.99), hasGrossValue("EUR", 45.50), //
                        hasForexGrossValue("USD", 49.00), //
                        hasTaxes("EUR", 12.51), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("A l t r i a  G r o u p Inc. Registered Shares DL -,333", "EUR");
        security.setIsin("US02209S1033");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DADATBankenhausPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-04-30T00:00"), hasExDate("2024-03-22T00:00"), //
                        hasShares(50.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 32.99), hasGrossValue("EUR", 45.50), //
                        hasTaxes("EUR", 12.51), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new DADATBankenhausPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NO0003054108"), hasWkn(null), hasTicker(null), //
                        hasName("M o w i  A S A Navne-Aksjer NK 7,50"), //
                        hasCurrencyCode("NOK"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-11-25T00:00"), hasExDate("2024-11-15T00:00"), //
                        hasShares(400.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("R.-Nr.: 92276651"), //
                        hasAmount("EUR", 32.34), hasGrossValue("EUR", 51.74), //
                        hasForexGrossValue("NOK", 600.05), //
                        hasTaxes("EUR", (150.00 + 75.04) / 11.5975), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        var security = new Security("M o w i  A S A Navne-Aksjer NK 7,50", "EUR");
        security.setIsin("NO0003054108");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DADATBankenhausPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-11-25T00:00"), hasExDate("2024-11-15T00:00"), //
                        hasShares(400.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("R.-Nr.: 92276651"), //
                        hasAmount("EUR", 32.34), hasGrossValue("EUR", 51.74), //
                        hasTaxes("EUR", (150.00 + 75.04) / 11.5975), hasFees("EUR", 0.00))));
    }
}
