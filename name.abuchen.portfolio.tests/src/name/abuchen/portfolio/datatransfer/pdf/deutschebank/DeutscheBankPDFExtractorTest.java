package name.abuchen.portfolio.datatransfer.pdf.deutschebank;

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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
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
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.DeutscheBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class DeutscheBankPDFExtractorTest
{
    @Test
    public void testDividende01()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

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
                        hasIsin("US17275R1023"), hasWkn("878841"), hasTicker(null), //
                        hasName("CISCO SYSTEMS INC.REGISTERED SHARES DL-,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-12-15"), hasExDate("2014-12-04"), //
                        hasShares(380.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 64.88), hasGrossValue("EUR", 87.13), //
                        hasForexGrossValue("USD", 98.80), //
                        hasTaxes("EUR", 8.71 + 0.47 + 13.07), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        var security = new Security("CISCO SYSTEMS INC.REGISTERED SHARES DL-,001", "EUR");
        security.setIsin("US17275R1023");
        security.setWkn("878841");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DeutscheBankPDFExtractor(client);

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
                        hasDate("2014-12-15"), hasExDate("2014-12-04"), //
                        hasShares(380.00), //
                        hasSource("Dividende01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 64.88), hasGrossValue("EUR", 87.13), //
                        hasTaxes("EUR", 8.71 + 0.47 + 13.07), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende02()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

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
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-12-15"), hasExDate(null), //
                        hasShares(123.00), //
                        hasSource("Dividende02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.95), hasGrossValue("EUR", 20.22), //
                        hasTaxes("EUR", 4.28 + 0.23 + 0.76), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

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
                        hasIsin("DE000A0J2060"), hasWkn("A0J206"), hasTicker(null), //
                        hasName("ISHS-MSCI N. AMERIC.UCITS ETF BE.SH.(DT.ZT.)"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2015-03-24T00:00"), hasExDate(null), //
                        hasShares(123.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.17), hasGrossValue("EUR", 19.51), //
                        hasForexGrossValue("USD", 19.51 * 1.0746), //
                        hasTaxes("EUR", 3.34), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        var security = new Security("ISHS-MSCI N. AMERIC.UCITS ETF BE.SH.(DT.ZT.)", "EUR");
        security.setIsin("DE000A0J2060");
        security.setWkn("A0J206");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DeutscheBankPDFExtractor(client);

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
                        hasDate("2015-03-24T00:00"), hasExDate(null), //
                        hasShares(123.00), //
                        hasSource("Dividende03.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 16.17), hasGrossValue("EUR", 19.51), //
                        hasTaxes("EUR", 3.34), hasFees("EUR", 0.00), //
                        check(tx -> {
                            var c = new CheckCurrenciesAction();
                            var account = new Account();
                            account.setCurrencyCode("EUR");
                            var s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende04()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

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
                        hasIsin("IE00B1YZSC51"), hasWkn("A0MZWQ"), hasTicker(null), //
                        hasName("ISHSII-CORE MSCI EUROPE U.ETF REG.SH.O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2019-05-31T00:00"), hasExDate(null), //
                        hasShares(1014.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 297.61), hasGrossValue("EUR", 297.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende05()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

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
                        hasIsin("DE0008474156"), hasWkn("847415"), hasTicker(null), //
                        hasName("DWS EUROPEAN OPPORTUNITIES INHABER-ANTEIL.LD"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-11-26T00:00"), hasExDate(null), //
                        hasShares(175.00), //
                        hasSource("Dividende05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 275.80), hasGrossValue("EUR", 343.00), //
                        hasTaxes("EUR", 58.70 + 3.22 + 5.28), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende06()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

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
                        hasIsin("IE00B3VVMM84"), hasWkn("A1JX51"), hasTicker(null), //
                        hasName("VANGUARD FTSE EMU.ETF DLD FUNDS"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-12-31T00:00"), hasExDate(null), //
                        hasShares(2000.00), //
                        hasSource("Dividende06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 544.56), hasGrossValue("EUR", 677.27), //
                        hasForexGrossValue("USD", 765.45), //
                        hasTaxes("EUR", 115.91 + 6.37 + 10.43), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende07()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

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
                        hasIsin("DE0006501554"), hasWkn("650155"), hasTicker(null), //
                        hasName("6% MAGNUM AG GENUßSCHEINE 99/UNBEGR."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-05T00:00"), hasExDate("2023-09-01T00:00"), //
                        hasShares(60.00), //
                        hasSource("Dividende07.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 259.22), hasGrossValue("EUR", 360.00), //
                        hasTaxes("EUR", 88.02 + 4.84 + 7.92), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende08()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1912161007"), hasWkn("850663"), hasTicker(null), //
                        hasName("COCA-COLA CO., THE REGISTERED SHARES DL -,25"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-15T00:00"), hasExDate("2023-11-30T00:00"), //
                        hasShares(170.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 58.22), hasGrossValue("USD", 78.20), //
                        hasTaxes("USD", 11.73 + 7.82 + 0.43), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende09()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

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
                        hasIsin("CA0641491075"), hasWkn("850388"), hasTicker(null), //
                        hasName("BANK OF NOVA SCOTIA, THE RG.SH. O.N."), //
                        hasCurrencyCode("CAD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-07-31T00:00"), hasExDate("2025-07-02T00:00"), //
                        hasShares(200.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 102.40), hasGrossValue("EUR", 137.53), //
                        hasForexGrossValue("CAD", 220.00), //
                        hasTaxes("EUR", 20.63 + 13.75 + 0.75), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende09WithSecurityInEUR()
    {
        var security = new Security("BANK OF NOVA SCOTIA, THE RG.SH. O.N.", "EUR");
        security.setIsin("CA0641491075");
        security.setWkn("850388");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DeutscheBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

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
                        hasDate("2025-07-31T00:00"), hasExDate("2025-07-02T00:00"), //
                        hasShares(200.00), //
                        hasSource("Dividende09.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 102.40), hasGrossValue("EUR", 137.53), //
                        hasTaxes("EUR", 20.63 + 13.75 + 0.75), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende10()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

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
                        hasIsin("DE000DWS0TS9"), hasWkn("DWS0TS"), hasTicker(null), //
                        hasName("FOS STRATEGIE-FONDS NR.1 INHABER-ANTEILE"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-05T00:00"), hasExDate(null), //
                        hasShares(10.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 381.03), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", 103.91 + 5.71 + 9.35), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende11()
    {
        // US-security bought via EUR-based stock exchange. That is the crucial
        // difference to the Coca Cola case in testDividende08.
        var security = new Security("BWX TECHNOLOGIES INC.RG.SH. DL -,01", "EUR");
        security.setIsin("US05605H1005");
        security.setWkn("A14V4U");

        var client = new Client();
        client.addSecurity(security);

        var extractor = new DeutscheBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        // dividend payment is supposed to go into newly created USD-account
        // clearing account. Despite the EUR currency of the security.
        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR", "USD");

        // check foreign currency dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-10T00:00"), hasExDate("2025-11-19T00:00"), //
                        hasShares(100.00), //
                        hasSource("Dividende11.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 18.62), hasGrossValue("USD", 25.00), //
                        hasTaxes("USD", 3.75 + 2.50 + 0.13), hasFees("USD", 0.00))));
    }

    @Test
    public void testDividende12()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

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
                        hasIsin("GG00BQZCBZ44"), hasWkn("A3D8TJ"), hasTicker(null), //
                        hasName("SHURGARD SELF STORAGE LTD.RG.SH. O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-05-27T00:00"), hasExDate(null), //
                        hasShares(170.00), //
                        hasSource("Dividende12.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 14.87), hasGrossValue("EUR", 21.40), //
                        hasTaxes("EUR", 4.28 + 2.14 + 0.11), hasFees("EUR", 0.00))));
    }

    @Test
    public void testKupon01()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kupon01.txt"), errors);

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
                        hasIsin("XS2722190795"), hasWkn("A3511H"), hasTicker(null), //
                        hasName("4% DEUTSCHE BAHN AG MTN.23 23.11. 43"), //
                        hasCurrencyCode("EUR"))));

        // check dividends (here: interest) transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-11-24T00:00"), hasExDate("2025-11-24T00:00"), //
                        hasShares(10.00), //
                        hasSource("Kupon01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 33.46), hasGrossValue("EUR", 40.00), //
                        hasTaxes("EUR", 6.20 + 0.34), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf01()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

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
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-04-02T09:04"), hasShares(19.00), //
                        hasSource("Kauf01.txt"), //
                        hasNote("Belegnummer 1234567890 / 123456"), //
                        hasAmount("EUR", 675.50), hasGrossValue("EUR", 665.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 7.90 + 2.00 + 0.60))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

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
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2015-04-02T09:04"), hasShares(36.00), //
                        hasSource("Kauf02.txt"), //
                        hasNote("Belegnummer 1234567890 / 123456"), //
                        hasAmount("EUR", 3524.98), hasGrossValue("EUR", 3513.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 8.78 + 2.00 + 0.60))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

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
                        hasIsin("LU0392494562"), hasWkn("ETF110"), hasTicker(null), //
                        hasName("COMSTAGE-MSCI WORLD TRN U.ETF INH.ANT.I O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-20T00:00"), hasShares(5.5791), //
                        hasSource("Kauf03.txt"), //
                        hasNote("Belegnummer 1694278628 / 24281"), //
                        hasAmount("EUR", 300.00), hasGrossValue("EUR", 300.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

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
                        hasIsin("IE00BK1PV551"), hasWkn("A1XEY2"), hasTicker(null), //
                        hasName("X(IE)-MSCI WORLD 1D FUNDS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-20T09:04"), hasShares(1.4353), //
                        hasSource("Kauf04.txt"), //
                        hasNote("Belegnummer 1111111111 / 1111111"), //
                        hasAmount("EUR", 100.95), hasGrossValue("EUR", 100.95), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

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
                        hasIsin("IE00BL25JL35"), hasWkn("A1103D"), hasTicker(null), //
                        hasName("X(IE)-MSCI WRLD QUAL.1CDL FUNDS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-12-20T09:09"), hasShares(4.9672), //
                        hasSource("Kauf05.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 250.05), hasGrossValue("EUR", 250.05), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

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
                        hasIsin("IE00BTJRMP35"), hasWkn("A12GVR"), hasTicker(null), //
                        hasName("X(IE)-MSCI EM.MKTS 1CDL FUNDS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-12-20T09:04"), hasShares(4.8634), //
                        hasSource("Kauf06.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 250.25), hasGrossValue("EUR", 250.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

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
                        hasIsin("LU0321464652"), hasWkn("DBX0A1"), hasTicker(null), //
                        hasName("XTRACKERS II GBP OVER.RATE SW.INH.ANT.1D ON"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-23T18:20"), hasShares(17.00), //
                        hasSource("Kauf07.txt"), //
                        hasNote("Belegnummer 1039975477 / 91752537"), //
                        hasAmount("EUR", 3733.27), hasGrossValue("EUR", 3728.61), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.32 - 4.66))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

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
                        hasIsin("DE0008476524"), hasWkn("847652"), hasTicker(null), //
                        hasName("DWS VERMÖGENSBG.FONDS I INHABER-ANTEILE LD"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-07-23T18:21"), hasShares(0.6413), //
                        hasSource("Kauf08.txt"), //
                        hasNote("Belegnummer 1522788379 / 181373046"), //
                        hasAmount("EUR", 200.01), hasGrossValue("EUR", 189.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 15.21 - 4.88))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

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
                        hasIsin("DE0008476524"), hasWkn("847652"), hasTicker(null), //
                        hasName("DWS VERMÖGENSBG.FONDS I INHABER-ANTEILE LD"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-09-23T00:00"), hasShares(0.6047), //
                        hasSource("Kauf09.txt"), //
                        hasNote("Belegnummer 1572848278 / 211210221"), //
                        hasAmount("EUR", 199.99), hasGrossValue("EUR", 188.74), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 11.25))));
    }

    @Test
    public void testWertpapierKauf10()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security (bond)
        assertThat(results, hasItem(security( //
                        hasIsin("US900123AY60"), hasWkn("A0GLU5"), hasTicker(null), //
                        hasName("6,875% TÜRKEI, REPUBLIK NT.06 17.M/S 03.36"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-21T16:37"), hasShares(5000.0 / 100), //
                        hasSource("Kauf10.txt"), //
                        hasNote("Belegnummer 1234567890 / 123456789 | Zinsen für 158 Zinstage: 150,86 USD"), //
                        hasAmount("USD", 5232.20), hasGrossValue("USD", 5153.86), //
                        hasTaxes("USD", 0.00), hasFees("USD", 68.32 + 5.22 + 4.80))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security (bond)
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DB9WGP3"), hasWkn("DB9WGP"), hasTicker(null), //
                        hasName("3% KUENDB. DB FESTZ. 28/32 25.08.32"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2025-08-19T00:00"), hasShares(1000.0 / 100), //
                        hasSource("Kauf11.txt"), //
                        hasNote("Belegnummer 1234567890 / 1234567"), //
                        hasAmount("EUR", 1010.00), hasGrossValue("EUR", 1010.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf12.txt"), errors);

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
                        hasIsin("IE00BP3QZ825"), hasWkn("A12ATF"), hasTicker(null), //
                        hasName("ISIV-E.MSCI WMF U.ETF DLA FUNDS"), //
                        hasCurrencyCode("EUR"))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-03-20T08:00:46"), hasShares(0.3945), //
                        hasSource("Kauf12.txt"), //
                        hasNote("Belegnummer 1595497338 / 718623085"), //
                        hasAmount("EUR", 32.60), hasGrossValue("EUR", 32.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf13.txt"), errors);

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
                        hasIsin("GB0002869419"), hasWkn("539971"), hasTicker(null), //
                        hasName("BIG YELLOW GROUP PLC RG.SH. LS 0,10"), //
                        hasCurrencyCode("EUR"))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-03-20T12:53:58"), hasShares(400.00), //
                        hasSource("Kauf13.txt"), //
                        hasNote("Belegnummer 6555923701 / 601948906"), //
                        hasAmount("EUR", 4258.22), hasGrossValue("EUR", 4240.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.60 + 4.50 + 3.12))));
    }

    @Test
    public void testWertpapierKauf14()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf14.txt"), errors);

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
                        hasIsin("IE00BJ0KDQ92"), hasWkn("A1XB5U"), hasTicker(null), //
                        hasName("X(IE)-MSCI WORLD 1C FUNDS"), //
                        hasCurrencyCode("EUR"))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-03-20T08:09:01"), hasShares(2.0796), //
                        hasSource("Kauf14.txt"), //
                        hasNote("Belegnummer 1595487602 / 223917324"), //
                        hasAmount("EUR", 250.00), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.13 - 3.13))));
    }

    @Test
    public void testWertpapierKauf15()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf15.txt"), errors);

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
                        hasIsin("LU0321464652"), hasWkn("DBX0A1"), hasTicker(null), //
                        hasName("XTRACKERS II GBP OVER.RATE SW.INH.ANT.1D ON"), //
                        hasCurrencyCode("EUR"))));

        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2026-03-10T19:26:45"), hasShares(6.00), //
                        hasSource("Kauf15.txt"), //
                        hasNote("Belegnummer 8406935960 / 650331086"), //
                        hasAmount("EUR", 1255.44), hasGrossValue("EUR", 1250.54), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 8.90 - 4.00))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

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
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-04-02T09:04"), hasShares(61.00), //
                        hasSource("Verkauf01.txt"), //
                        hasNote("Belegnummer 1234567890 / 123456"), //
                        hasAmount("EUR", 2074.71), hasGrossValue("EUR", 2216.14), //
                        hasTaxes("EUR", 122.94 + 6.76 + 1.23), hasFees("EUR", 7.90 + 2.00 + 0.60))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

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
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-01-28T09:00"), hasShares(8.00), //
                        hasSource("Verkauf02.txt"), //
                        hasNote("Belegnummer 1234567890 / 123456"), //
                        hasAmount("EUR", 453.66), hasGrossValue("EUR", 464.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 7.90 + 2.00 + 0.60))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

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
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2017-02-16T15:28"), hasShares(100.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Belegnummer 1202359588 / 831475"), //
                        hasAmount("EUR", 4753.16), hasGrossValue("EUR", 4774.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 11.94 + 3.50 + 5.40))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

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
                        hasIsin("DE0007448508"), hasWkn("744850"), hasTicker(null), //
                        hasName("IVU TRAFFIC TECHNOLOGIES AG INH.AKT. O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2022-07-07T09:05"), hasShares(1870.00), //
                        hasSource("Verkauf04.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 27613.77), hasGrossValue("EUR", 27676.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 58.90 + 2.00 + 1.33))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

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
                        hasIsin("HK0992009065"), hasWkn("894983"), hasTicker(null), //
                        hasName("LENOVO GROUP LTD.REGISTERED SHARES O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-02-24T08:02:00"), hasShares(8000.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Belegnummer 6978580640 / 890153444"), //
                        hasAmount("EUR", 7185.60), hasGrossValue("EUR", 8200.00), //
                        hasTaxes("EUR", 933.16 + 51.32), hasFees("EUR", 20.50 + 4.50 + 4.92))));
    }

    @Test
    public void testWertpapierVerkauf06()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf06.txt"), errors);

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
                        hasIsin("LU0274211480"), hasWkn("DBX1DA"), hasTicker(null), //
                        hasName("XTRACKERS DAX INHABER-ANTEILE 1C O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2026-02-02T14:58"), hasShares(2.2450), //
                        hasSource("Verkauf06.txt"), //
                        hasNote("Belegnummer 1588255927 / 9998"), //
                        hasAmount("EUR", 520.39), hasGrossValue("EUR", 520.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierSparplan01()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(6L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("847652"), hasTicker(null), //
                        hasName("DWS VERMÖGENSBG.FONDS I INHABER-ANTEILE LD"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-01-03T00:00"), hasShares(0.1610), //
                        hasSource("Sparplan01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.98), hasGrossValue("EUR", 31.68), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 13.30))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-02-02T00:00"), hasShares(0.1558), //
                        hasSource("Sparplan01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 45.00), hasGrossValue("EUR", 31.25), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 13.75))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-03-04T00:00"), hasShares(0.1513), //
                        hasSource("Sparplan01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.98), hasGrossValue("EUR", 30.82), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 14.16))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-04-03T00:00"), hasShares(0.1470), //
                        hasSource("Sparplan01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 44.98), hasGrossValue("EUR", 30.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 14.57))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-05-03T00:00"), hasShares(0.1487), //
                        hasSource("Sparplan01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 45.00), hasGrossValue("EUR", 30.59), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 14.41))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-06-04T00:00"), hasShares(0.1465), //
                        hasSource("Sparplan01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 45.00), hasGrossValue("EUR", 30.37), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 14.63))));
    }

    @Test
    public void testWertpapierSparplan02()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Sparplan02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(5L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin(null), hasWkn("847652"), hasTicker(null), //
                        hasName("DWS VERMÖGENSBG.FONDS I INHABER-ANTEILE LD"), //
                        hasCurrencyCode("EUR"))));
        
        // check transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2019-08-01T00:00"), hasShares(0.1028), //
                        hasSource("Sparplan02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.0))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-09-02T00:00"), hasShares(0.1042), //
                        hasSource("Sparplan02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.0))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-10-01T00:00"), hasShares(0.1017), //
                        hasSource("Sparplan02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.0))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-11-01T00:00"), hasShares(0.1008), //
                        hasSource("Sparplan02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.0))));

        assertThat(results, hasItem(purchase( //
                        hasDate("2019-12-02T00:00"), hasShares(0.0972), //
                        hasSource("Sparplan02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.00), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.0))));
    }

    @Test
    public void testGiroKontoauszug01()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(32L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(32));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-02"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 40.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-02"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Dauerauftrag an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-02"), hasAmount("EUR", 14.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Überweisung an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-02"), hasAmount("EUR", 14.40), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-02"), hasAmount("EUR", 46.50), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-03"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Dauerauftrag an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-05"), hasAmount("EUR", 4.34), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-06"), hasAmount("EUR", 562.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Überweisung von Unser Sparverein"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-06"), hasAmount("EUR", 5.33), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-06"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Bargeldauszahlung GAA"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-12"), hasAmount("EUR", 7.16), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-12"), hasAmount("EUR", 26.39), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-13"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote(""))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-13"), hasAmount("EUR", 1.59), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-13"), hasAmount("EUR", 14.69), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-13"), hasAmount("EUR", 29.90), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-13"), hasAmount("EUR", 15.98), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("2104 ERNSTINGS FAM. Stadt"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-13"), hasAmount("EUR", 34.09), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("H+M 382 SAGT VIELEN DANK"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-16"), hasAmount("EUR", 9.99), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Überweisung von 2104 ERNSTINGS FAM. Stadt"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-16"), hasAmount("EUR", 69.32), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-16"), hasAmount("EUR", 14.61), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-19"), hasAmount("EUR", 33.99), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("AMAZON DIGITAL GERMANY GMBH"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-23"), hasAmount("EUR", 3.89), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("AMAZON DIGITAL GERMANY GMBH"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-23"), hasAmount("EUR", 34.39), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-23"), hasAmount("EUR", 53.11), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-24"), hasAmount("EUR", 29.24), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-11-27"), hasAmount("EUR", 2222.22), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Überweisung von Die Firma GmbH"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-27"), hasAmount("EUR", 11.80), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit( //
                        hasDate("2020-11-30"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Überweisung von Max Mustermann"), //
                        hasAmount("EUR", 80.00))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-30"), hasAmount("EUR", 2.05), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-30"), hasAmount("EUR", 10.38), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-11-30"), hasAmount("EUR", 55.46), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));
    }

    @Test
    public void testGiroKontoauszug02()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(48L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(49));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-12-01"), hasAmount("EUR", 293.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung von Landeshauptstadt Stadt Stadtverwaltung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-01"), hasAmount("EUR", 40.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Dauerauftrag an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-01"), hasAmount("EUR", 29.24), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-01"), hasAmount("EUR", 39.99), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-02"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Dauerauftrag an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-03"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Dauerauftrag an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-03"), hasAmount("EUR", 0.57), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-03"), hasAmount("EUR", 34.82), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-03"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Bargeldauszahlung GAA"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-12-04"), hasAmount("EUR", 344.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung von Max Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-04"), hasAmount("EUR", 39.99), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-12-07"), hasAmount("EUR", 562.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung von Unser Sparverein"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-07"), hasAmount("EUR", 150.12), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-07"), hasAmount("EUR", 14.44), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-07"), hasAmount("EUR", 50.01), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-08"), hasAmount("EUR", 45.26), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-09"), hasAmount("EUR", 16.87), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-09"), hasAmount("EUR", 38.78), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("H+M 382 SAGT VIELEN DANK"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-10"), hasAmount("EUR", 34.95), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-10"), hasAmount("EUR", 37.40), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-12-14"), hasAmount("EUR", 150.12), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung von AMAZON EU S.A R.L., NIEDERLASSUNG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-14"), hasAmount("EUR", 36.28), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-15"), hasAmount("EUR", 6.36), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-15"), hasAmount("EUR", 14.99), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-15"), hasAmount("EUR", 15.14), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-15"), hasAmount("EUR", 43.47), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-15"), hasAmount("EUR", 21.94), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-15"), hasAmount("EUR", 41.50), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-17"), hasAmount("EUR", 21.99), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-18"), hasAmount("EUR", 10.99), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-18"), hasAmount("EUR", 15.99), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-12-21"), hasAmount("EUR", 5.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung von PayPal (Europe) S.a.r.l. et Cie., S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-12-21"), hasAmount("EUR", 2222.22), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung von Die Firma GmbH"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-21"), hasAmount("EUR", 13.85), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-21"), hasAmount("EUR", 23.50), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung an Max Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-21"), hasAmount("EUR", 26.10), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-21"), hasAmount("EUR", 56.14), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-22"), hasAmount("EUR", 14.29), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-22"), hasAmount("EUR", 10.11), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("ROSSMANN VIELEN DANK"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-23"), hasAmount("EUR", 47.86), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-24"), hasAmount("EUR", 8.45), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-28"), hasAmount("EUR", 31.79), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung an Max Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-29"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote(""))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-29"), hasAmount("EUR", 8.74), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-29"), hasAmount("EUR", 31.19), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-12-29"), hasAmount("EUR", 27.96), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-12-30"), hasAmount("EUR", 309.00), //
                        hasSource("GiroKontoauszug02.txt"), hasNote("Überweisung von Landeshauptstadt Stadt Stadtverwaltung"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2020-12-31"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Saldo der Abschlussposten"), //
                        hasAmount("EUR", 13.47), hasGrossValue("EUR", 13.47), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        removal(hasDate("2020-12-31"), hasAmount("EUR", 13.47), //
                                        hasSource("GiroKontoauszug02.txt"), hasNote("")))));
    }

    @Test
    public void testGiroKontoauszug03()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(30L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(30));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-04"), hasAmount("EUR", 250.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote(""))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-04"), hasAmount("EUR", 40.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Dauerauftrag an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-04"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Dauerauftrag an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-04"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Dauerauftrag an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-05"), hasAmount("EUR", 11.30), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-05"), hasAmount("EUR", 2.09), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-05"), hasAmount("EUR", 365.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Echtzeitüberweisung von Max Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-07"), hasAmount("EUR", 69.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-11"), hasAmount("EUR", 438.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Überweisung von Unser Sparverein"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-11"), hasAmount("EUR", 29.24), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-14"), hasAmount("EUR", 12.49), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Überweisung von AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-14"), hasAmount("EUR", 7.99), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("NETFLIX INTERNATIONAL B.V."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-14"), hasAmount("EUR", 16.80), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-18"), hasAmount("EUR", 25.46), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-19"), hasAmount("EUR", 14.90), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-19"), hasAmount("EUR", 28.61), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-20"), hasAmount("EUR", 34.99), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-20"), hasAmount("EUR", 39.18), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-21"), hasAmount("EUR", 4.79), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("ROSSMANN VIELEN DANK"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-25"), hasAmount("EUR", 29.85), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-25"), hasAmount("EUR", 37.99), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-25"), hasAmount("EUR", 48.01), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-25"), hasAmount("EUR", 140.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Überweisung an Rundfunk ARD, ZDF, DRadio"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-25"), hasAmount("EUR", 365.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Überweisung an Max Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-26"), hasAmount("EUR", 118.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Überweisung an Max Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-27"), hasAmount("EUR", 41.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-28"), hasAmount("EUR", 2222.22), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Überweisung von Die Firma GmbH"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-01-29"), hasAmount("EUR", 309.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Überweisung von Landeshauptstadt Stadt Stadtverwaltung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-29"), hasAmount("EUR", 9.00), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Vodafone GmbH"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-01-29"), hasAmount("EUR", 44.49), //
                        hasSource("GiroKontoauszug03.txt"), hasNote("Kartenzahlung"))));
    }

    @Test
    public void testGiroKontoauszug04()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(27L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(27));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-01"), hasAmount("EUR", 300.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote(""))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-01"), hasAmount("EUR", 40.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Dauerauftrag an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-01"), hasAmount("EUR", 74.94), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Überweisung an QVC Handel S.a.r.l und Co. KG"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-02"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Dauerauftrag an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-02"), hasAmount("EUR", 12.55), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-03"), hasAmount("EUR", 20.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Überweisung von AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-03"), hasAmount("EUR", 29.85), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Überweisung von AMAZON PAYMENTS EUROPE S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-03"), hasAmount("EUR", 365.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Überweisung von Max Mustermann"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-03"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Dauerauftrag an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-03"), hasAmount("EUR", 43.57), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-04"), hasAmount("EUR", 11.72), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-05"), hasAmount("EUR", 31.66), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-08"), hasAmount("EUR", 365.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Überweisung an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-09"), hasAmount("EUR", 438.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Überweisung von Unser Sparverein"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-09"), hasAmount("EUR", 5.17), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-10"), hasAmount("EUR", 58.62), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-15"), hasAmount("EUR", 406.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Überweisung von Unser Sparverein"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-15"), hasAmount("EUR", 7.99), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("NETFLIX INTERNATIONAL B.V."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-15"), hasAmount("EUR", 25.98), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-15"), hasAmount("EUR", 75.46), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-16"), hasAmount("EUR", 17.90), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-18"), hasAmount("EUR", 29.10), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-19"), hasAmount("EUR", 365.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Überweisung an Mustermann, Max"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-23"), hasAmount("EUR", 25.61), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-25"), hasAmount("EUR", 7665.83), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Überweisung von Die Firma GmbH"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-02-25"), hasAmount("EUR", 22.49), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-02-26"), hasAmount("EUR", 309.00), //
                        hasSource("GiroKontoauszug04.txt"), hasNote("Überweisung von Landeshauptstadt Stadt Stadtverwaltung"))));
    }

    @Test
    public void testGiroKontoauszug05()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        removal(hasDate("2023-06-02"), hasAmount("EUR", 1073.65), //
                                        hasSource("GiroKontoauszug05.txt"), hasNote("")))));
    }

    @Test
    public void testGiroKontoauszug06()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(2L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-14"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug06.txt"), hasNote("Überweisung an Max Mustermann"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        deposit(hasDate("2023-08-16"), hasAmount("EUR", 33.23), //
                                        hasSource("GiroKontoauszug06.txt"), hasNote("")))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        deposit(hasDate("2023-08-31"), hasAmount("EUR", 74.49), //
                                        hasSource("GiroKontoauszug06.txt"), hasNote("")))));
    }

    @Test
    public void testGiroKontoauszug07()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, "EUR");

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        removal(hasDate("2024-10-03"), hasAmount("EUR", 44.99), //
                                        hasSource("GiroKontoauszug07.txt"), hasNote("")))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        removal(hasDate("2024-10-03"), hasAmount("EUR", 44.99), //
                                        hasSource("GiroKontoauszug07.txt"), hasNote("")))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        removal(hasDate("2024-10-04"), hasAmount("EUR", 45.00), //
                                        hasSource("GiroKontoauszug07.txt"), hasNote("")))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        removal(hasDate("2024-10-04"), hasAmount("EUR", 45.00), //
                                        hasSource("GiroKontoauszug07.txt"), hasNote("")))));
    }

    @Test
    public void testGiroKontoauszug08()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "USD");

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        deposit(hasDate("2025-10-01"), hasAmount("USD", 7.87), //
                                        hasSource("GiroKontoauszug08.txt"), hasNote("")))));
    }

    @Test
    public void testGiroKontoauszug09()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-10-21"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug09.txt"), hasNote("Übertrag (Überweisung) von Max Mustermann"))));
    }

    @Test
    public void testGiroKontoauszug10()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(3L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-15"), hasAmount("EUR", 400.00), //
                        hasSource("GiroKontoauszug10.txt"), hasNote("Überweisung von Dr. kQEbfBPDq ZgltrGG wBPgFcQwn"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-15"), hasAmount("EUR", 600.00), //
                        hasSource("GiroKontoauszug10.txt"), hasNote("Überweisung von Dr. ICcCbCKba zlKgUWI NwzaZJPVb"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        removal(hasDate("2025-10-22"), hasAmount("EUR", 200.00), //
                                        hasSource("GiroKontoauszug10.txt"), hasNote("")))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        removal(hasDate("2025-10-22"), hasAmount("EUR", 300.00), //
                                        hasSource("GiroKontoauszug10.txt"), hasNote("")))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionSkipIfDetailsMissing, //
                        removal(hasDate("2025-10-22"), hasAmount("EUR", 500.01), //
                                        hasSource("GiroKontoauszug10.txt"), hasNote("")))));
    }

    @Test
    public void testGiroKontoauszug11()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2025-08-25"), hasAmount("EUR", 34.30), //
                        hasSource("GiroKontoauszug11.txt"), hasNote("Steuererstattung"))));
    }

    @Test
    public void testVorabpauschale01()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

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
                        hasIsin("IE00B4X9L533"), hasWkn("A1C9KK"), hasTicker(null), //
                        hasName("HSBC MSCI WORLD UCITS ETF FUNDS"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2026-01-02T00:00"), hasShares(300.00), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 8.87), hasGrossValue("EUR", 8.87), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B0M62X26"), hasWkn("A0HGV1"), hasTicker(null), //
                        hasName("ISHS EO IN.LI.GO.BD U.ETF(D)RG.SH.O.N."), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, taxes( //
                                        hasDate("2026-01-15"), hasShares(120.00), //
                                        hasSource("Vorabpauschale02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testVorabpauschale03()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0274211480"), hasWkn("DBX1DA"), hasTicker(null), //
                        hasName("XTRACKERS DAX INHABER-ANTEILE 1C O.N."), //
                        hasCurrencyCode("EUR"))));

        // check skipped item
        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        taxes( //
                                        hasDate("2026-01-19T00:00"), hasShares(44.0454), //
                                        hasSource("Vorabpauschale03.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }
}
