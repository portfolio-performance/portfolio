package name.abuchen.portfolio.datatransfer.pdf.deutschebank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.DeutscheBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US17275R1023"), hasWkn("878841"), hasTicker(null), //
                        hasName("CISCO SYSTEMS INC.REGISTERED SHARES DL-,001"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-12-15"), hasShares(380), //
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
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-12-15"), hasShares(380), //
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2014-12-15"), hasShares(123), //
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0J2060"));
        assertThat(security.getWkn(), is("A0J206"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ISHS-MSCI N. AMERIC.UCITS ETF BE.SH.(DT.ZT.)"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-24T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(123)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(16.17))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(19.51))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(3.34))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(19.51 * 1.0746))));
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
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-03-24T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(123)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(16.17))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(19.51))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(3.34))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var c = new CheckCurrenciesAction();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B1YZSC51"));
        assertThat(security.getWkn(), is("A0MZWQ"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ISHSII-CORE MSCI EUROPE U.ETF REG.SH.O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1014)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(297.61))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(297.61))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0008474156"));
        assertThat(security.getWkn(), is("847415"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("DWS EUROPEAN OPPORTUNITIES INHABER-ANTEIL.LD"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-26T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(175)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(275.80))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(343.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(58.70 + 3.22 + 5.28))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B3VVMM84"));
        assertThat(security.getWkn(), is("A1JX51"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("VANGUARD FTSE EMU.ETF DLD FUNDS"));
        assertThat(security.getCurrencyCode(), is("USD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(2000)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of("EUR", Values.Amount.factorize(544.56))));
        assertThat(transaction.getGrossValue(), is(Money.of("EUR", Values.Amount.factorize(677.27))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(115.91 + 6.37 + 10.43))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of("EUR", Values.Amount.factorize(0.00))));

        var grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("USD", Values.Amount.factorize(765.45))));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0006501554"), hasWkn("650155"), hasTicker(null), //
                        hasName("6% MAGNUM AG GENUßSCHEINE 99/UNBEGR."), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-09-05T00:00"), hasShares(60.00), //
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US1912161007"), hasWkn("850663"), hasTicker(null), //
                        hasName("COCA-COLA CO., THE REGISTERED SHARES DL -,25"), //
                        hasCurrencyCode("USD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-15T00:00"), hasShares(170.00), //
                        hasSource("Dividende08.txt"), //
                        hasNote(null), //
                        hasAmount("USD", 58.22), hasGrossValue("USD", 78.21), //
                        hasTaxes("USD", 11.74 + 7.82 + 0.43), hasFees("USD", 0.00))));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CA0641491075"), hasWkn("850388"), hasTicker(null), //
                        hasName("BANK OF NOVA SCOTIA, THE RG.SH. O.N."), //
                        hasCurrencyCode("CAD"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-07-31T00:00"), hasShares(200.00), //
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
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-07-31T00:00"), hasShares(200.00), //
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DWS0TS9"), hasWkn("DWS0TS"), hasTicker(null), //
                        hasName("FOS STRATEGIE-FONDS NR.1 INHABER-ANTEILE"), //
                        hasCurrencyCode("EUR"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-05T00:00"), hasShares(10.00), //
                        hasSource("Dividende10.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 381.03), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", 103.91 + 5.71 + 9.35), hasFees("EUR", 0.00))));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("XS2722190795"), hasWkn("A3511H"), hasTicker(null), //
                        hasName("4% DEUTSCHE BAHN AG MTN.23 23.11. 43"), //
                        hasCurrencyCode("EUR"))));

        // check dividends (here: interest) transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2025-11-24T00:00"), hasShares(10.00), //
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000BASF111"));
        assertThat(security.getWkn(), is("BASF11"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("BASF SE"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-04-02T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(36)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Belegnummer 1234567890 / 123456"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(3524.98))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(3513.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(8.78 + 2.00 + 0.60))));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("COMSTAGE-MSCI WORLD TRN U.ETF INH.ANT.I O.N. 1/1"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-08-20T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5.5791)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Belegnummer 1694278628 / 24281"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(300.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(300.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BK1PV551"));
        assertThat(security.getWkn(), is("A1XEY2"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("X(IE)-MSCI WORLD 1D FUNDS 1/1"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-20T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.4353)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertThat(entry.getNote(), is("Belegnummer 1111111111 / 1111111"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(100.95))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(100.95))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BL25JL35"));
        assertThat(security.getWkn(), is("A1103D"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("X(IE)-MSCI WRLD QUAL.1CDL FUNDS 1/1"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-12-20T09:09")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.9672)));
        assertThat(entry.getSource(), is("Kauf05.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(250.05))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(250.05))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BTJRMP35"));
        assertThat(security.getWkn(), is("A12GVR"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("X(IE)-MSCI EM.MKTS 1CDL FUNDS 1/1"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-12-20T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4.8634)));
        assertThat(entry.getSource(), is("Kauf06.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(250.25))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(250.25))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0321464652"), hasWkn("DBX0A1"), hasTicker(null), //
                        hasName("XTRACKERS II GBP OVER.RATE SW.INH.ANT.1D ON 1/1"), //
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008476524"), hasWkn("847652"), hasTicker(null), //
                        hasName("DWS VERMÖGENSBG.FONDS I INHABER-ANTEILE LD 1/1"), //
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0008476524"), hasWkn("847652"), hasTicker(null), //
                        hasName("DWS VERMÖGENSBG.FONDS I INHABER-ANTEILE LD 1/1"), //
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000BASF111"));
        assertThat(security.getWkn(), is("BASF11"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("BASF SE"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-04-02T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(61)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Belegnummer 1234567890 / 123456"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(2074.71))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(2216.14))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(122.94 + 6.76 + 1.23))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(7.90 + 2.00 + 0.60))));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000BASF111"), hasWkn("BASF11"), hasTicker(null), //
                        hasName("BASF SE"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2015-01-28T09:00"), hasShares(8), //
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000BASF111"));
        assertThat(security.getWkn(), is("BASF11"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("BASF SE"));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-02-16T15:28")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getSource(), is("Verkauf03.txt"));
        assertThat(entry.getNote(), is("Belegnummer 1202359588 / 831475"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(4753.16))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(4774.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(11.94 + 3.50 + 5.40))));
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007448508"));
        assertThat(security.getWkn(), is("744850"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("IVU TRAFFIC TECHNOLOGIES AG INH.AKT. O.N."));
        assertThat(security.getCurrencyCode(), is("EUR"));

        // check buy sell transaction
        var entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-07-07T09:05")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1870)));
        assertThat(entry.getSource(), is("Verkauf04.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("EUR", Values.Amount.factorize(27613.77))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("EUR", Values.Amount.factorize(27676.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("EUR", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(58.90 + 2.00 + 1.33))));
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
        assertThat(results.size(), is(32));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-02"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 40.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-02"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 1000.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-02"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Überweisung an Mustermann, Max"), //
                        hasAmount("EUR", 14.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-02"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 14.40))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-02"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 46.50))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-03"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 50.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-05"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 4.34))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-11-06"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Überweisung von Unser Sparverein"), //
                        hasAmount("EUR", 562.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-06"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 5.33))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-06"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Bargeldauszahlung GAA"), //
                        hasAmount("EUR", 100.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-12"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 7.16))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-12"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 26.39))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-13"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasAmount("EUR", 100.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-13"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 1.59))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-13"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 14.69))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-13"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 29.90))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-13"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("2104 ERNSTINGS FAM. Stadt"), //
                        hasAmount("EUR", 15.98))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-13"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("H+M 382 SAGT VIELEN DANK"), //
                        hasAmount("EUR", 34.09))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-11-16"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Überweisung von 2104 ERNSTINGS FAM. Stadt"), //
                        hasAmount("EUR", 9.99))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-16"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 69.32))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-16"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 14.61))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-19"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("AMAZON DIGITAL GERMANY GMBH"), //
                        hasAmount("EUR", 33.99))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-23"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("AMAZON DIGITAL GERMANY GMBH"), //
                        hasAmount("EUR", 3.89))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-23"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 34.39))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-23"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 53.11))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-24"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"), //
                        hasAmount("EUR", 29.24))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-11-27"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Überweisung von Die Firma GmbH"), //
                        hasAmount("EUR", 2222.22))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-27"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 11.80))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-11-30"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Überweisung von Max Mustermann"), //
                        hasAmount("EUR", 80.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-30"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 2.05))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-30"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 10.38))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-11-30"), //
                        hasSource("GiroKontoauszug01.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 55.46))));
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
        assertThat(results.size(), is(48));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-12-01"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Überweisung von Landeshauptstadt Stadt Stadtverwaltung"), //
                        hasAmount("EUR", 293.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-01"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 40.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-01"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"), //
                        hasAmount("EUR", 29.24))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-01"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 39.99))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-02"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 1000.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-03"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 50.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-03"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 0.57))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-03"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 34.82))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-03"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Bargeldauszahlung GAA"), //
                        hasAmount("EUR", 50.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-12-04"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Überweisung von Max Mustermann"), //
                        hasAmount("EUR", 344.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-04"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 39.99))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-12-07"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Überweisung von Unser Sparverein"), //
                        hasAmount("EUR", 562.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-07"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"), //
                        hasAmount("EUR", 150.12))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-07"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 14.44))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-07"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 50.01))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-08"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 45.26))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-09"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 16.87))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-09"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("H+M 382 SAGT VIELEN DANK"), //
                        hasAmount("EUR", 38.78))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-10"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 34.95))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-10"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 37.40))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-12-14"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Überweisung von AMAZON EU S.A R.L., NIEDERLASSUNG"), //
                        hasAmount("EUR", 150.12))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-14"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 36.28))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-15"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 6.36))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-15"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"), //
                        hasAmount("EUR", 14.99))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-15"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."), //
                        hasAmount("EUR", 15.14))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-15"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."), //
                        hasAmount("EUR", 43.47))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-15"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 21.94))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-15"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 41.50))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-17"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 21.99))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-18"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 10.99))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-18"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 15.99))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-12-21"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Überweisung von PayPal (Europe) S.a.r.l. et Cie., S.C.A."), //
                        hasAmount("EUR", 5.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-12-21"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Überweisung von Die Firma GmbH"), //
                        hasAmount("EUR", 2222.22))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-21"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 13.85))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-21"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Überweisung an Max Mustermann"), //
                        hasAmount("EUR", 23.50))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-21"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 26.10))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-21"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 56.14))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-22"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 14.29))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-22"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("ROSSMANN VIELEN DANK"), //
                        hasAmount("EUR", 10.11))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-23"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 47.86))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-24"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"), //
                        hasAmount("EUR", 8.45))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-28"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Überweisung an Max Mustermann"), //
                        hasAmount("EUR", 31.79))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-29"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasAmount("EUR", 500.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-29"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."), //
                        hasAmount("EUR", 8.74))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-29"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 31.19))));

        assertThat(results, hasItem(removal( //
                        hasDate("2020-12-29"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 27.96))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2020-12-30"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Überweisung von Landeshauptstadt Stadt Stadtverwaltung"), //
                        hasAmount("EUR", 309.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2020-12-31"), //
                        hasSource("GiroKontoauszug02.txt"), //
                        hasNote("Saldo der Abschlussposten"), //
                        hasAmount("EUR", 13.47))));
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
        assertThat(results.size(), is(30));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-04"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasAmount("EUR", 250.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-04"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 40.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-04"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 50.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-04"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 1000.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-05"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."), //
                        hasAmount("EUR", 11.30))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-05"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 2.09))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-01-05"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Echtzeitüberweisung von Max Mustermann"), //
                        hasAmount("EUR", 365.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-07"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"), //
                        hasAmount("EUR", 69.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-01-11"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Überweisung von Unser Sparverein"), //
                        hasAmount("EUR", 438.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-11"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 29.24))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-01-14"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Überweisung von AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 12.49))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-14"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("NETFLIX INTERNATIONAL B.V."), //
                        hasAmount("EUR", 7.99))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-14"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."), //
                        hasAmount("EUR", 16.80))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-18"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 25.46))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-19"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 14.90))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-19"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 28.61))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-20"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 34.99))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-20"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 39.18))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-21"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("ROSSMANN VIELEN DANK"), //
                        hasAmount("EUR", 4.79))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-25"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 29.85))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-25"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 37.99))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-25"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 48.01))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-25"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Überweisung an Rundfunk ARD, ZDF, DRadio"), //
                        hasAmount("EUR", 140.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-25"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Überweisung an Max Mustermann"), //
                        hasAmount("EUR", 365.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-26"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Überweisung an Max Mustermann"), //
                        hasAmount("EUR", 118.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-27"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 41.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-01-28"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Überweisung von Die Firma GmbH"), //
                        hasAmount("EUR", 2222.22))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-01-29"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Überweisung von Landeshauptstadt Stadt Stadtverwaltung"), //
                        hasAmount("EUR", 309.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-29"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Vodafone GmbH"), //
                        hasAmount("EUR", 9.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-01-29"), //
                        hasSource("GiroKontoauszug03.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 44.49))));
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
        assertThat(results.size(), is(27));
        new AssertImportActions().check(results, "EUR");

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-01"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasAmount("EUR", 300.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-01"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 40.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-01"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Überweisung an QVC Handel S.a.r.l und Co. KG"), //
                        hasAmount("EUR", 74.94))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-02"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 1000.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-02"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."), //
                        hasAmount("EUR", 12.55))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-02-03"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Überweisung von AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 20.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-02-03"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Überweisung von AMAZON PAYMENTS EUROPE S.C.A."), //
                        hasAmount("EUR", 29.85))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-02-03"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Überweisung von Max Mustermann"), //
                        hasAmount("EUR", 365.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-03"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Dauerauftrag an Mustermann, Max"), //
                        hasAmount("EUR", 50.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-03"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 43.57))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-04"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 11.72))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-05"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 31.66))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-08"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Überweisung an Mustermann, Max"), //
                        hasAmount("EUR", 365.00))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-02-09"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Überweisung von Unser Sparverein"), //
                        hasAmount("EUR", 438.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-09"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 5.17))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-10"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 58.62))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-02-15"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Überweisung von Unser Sparverein"), //
                        hasAmount("EUR", 406.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-15"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("NETFLIX INTERNATIONAL B.V."), //
                        hasAmount("EUR", 7.99))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-15"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 25.98))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-15"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 75.46))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-16"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("PayPal (Europe) S.a.r.l. et Cie., S.C.A."), //
                        hasAmount("EUR", 17.90))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-18"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 29.10))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-19"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Überweisung an Mustermann, Max"), //
                        hasAmount("EUR", 365.00))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-23"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Kartenzahlung"), //
                        hasAmount("EUR", 25.61))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-02-25"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Überweisung von Die Firma GmbH"), //
                        hasAmount("EUR", 7665.83))));

        assertThat(results, hasItem(removal( //
                        hasDate("2021-02-25"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("AMAZON EU S.A R.L., NIEDERLASSUNG"), //
                        hasAmount("EUR", 22.49))));

        assertThat(results, hasItem(deposit( //
                        hasDate("2021-02-26"), //
                        hasSource("GiroKontoauszug04.txt"), //
                        hasNote("Überweisung von Landeshauptstadt Stadt Stadtverwaltung"), //
                        hasAmount("EUR", 309.00))));
    }

    @Test
    public void testGiroKontoauszug05()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug05.txt"), errors);

        // Check if the results list is not empty
        assertTrue(results.isEmpty());

        // Check if at least one error is present
        assertTrue(!errors.isEmpty());

        // Extract the first error from the list
        var firstError = errors.get(0);

        // Check if the first error is an UnsupportedOperationException
        assertTrue(firstError instanceof UnsupportedOperationException);

        // Check the error message of the first error
        var expectedErrorMessage = MessageFormat.format(Messages.PDFdbMsgCannotDetermineFileType,
                        "Deutsche Bank Privat- und Geschäftskunden AG", "GiroKontoauszug05.txt");
        assertEquals(expectedErrorMessage, firstError.getMessage());
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
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-08-14"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug06.txt"), hasNote("Überweisung an Max Mustermann"))));

    }

    @Test
    public void testGiroKontoauszug07()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug07.txt"), errors);

        // Check if the results list is not empty
        assertTrue(results.isEmpty());

        // Check if at least one error is present
        assertTrue(!errors.isEmpty());

        // Extract the first error from the list
        var firstError = errors.get(0);

        // Check if the first error is an UnsupportedOperationException
        assertTrue(firstError instanceof UnsupportedOperationException);

        // Check the error message of the first error
        var expectedErrorMessage = MessageFormat.format(Messages.PDFdbMsgCannotDetermineFileType,
                        "Deutsche Bank Privat- und Geschäftskunden AG", "GiroKontoauszug07.txt");
        assertEquals(expectedErrorMessage, firstError.getMessage());
    }

    @Test
    public void testGiroKontoauszug08()
    {
        var extractor = new DeutscheBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug08.txt"), errors);

        // Check if the results list is not empty
        assertTrue(results.isEmpty());

        // Check if at least one error is present
        assertTrue(!errors.isEmpty());

        // Extract the first error from the list
        var firstError = errors.get(0);

        // Check if the first error is an UnsupportedOperationException
        assertTrue(firstError instanceof UnsupportedOperationException);

        // Check the error message of the first error
        var expectedErrorMessage = MessageFormat.format(Messages.PDFdbMsgCannotDetermineFileType,
                        "Deutsche Bank Privat- und Geschäftskunden AG", "GiroKontoauszug08.txt");
        assertEquals(expectedErrorMessage, firstError.getMessage());
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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-15"), hasAmount("EUR", 400.00), //
                        hasSource("GiroKontoauszug10.txt"), hasNote("Überweisung von Dr. kQEbfBPDq ZgltrGG wBPgFcQwn"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-15"), hasAmount("EUR", 600.00), //
                        hasSource("GiroKontoauszug10.txt"), hasNote("Überweisung von Dr. ICcCbCKba zlKgUWI NwzaZJPVb"))));
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
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "EUR");

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2025-08-25"), hasAmount("EUR", 34.30), //
                        hasSource("GiroKontoauszug11.txt"), hasNote("Steuererstattung"))));
    }
}
