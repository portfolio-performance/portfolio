package name.abuchen.portfolio.datatransfer.pdf.direkt1822bank;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundDelivery;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.Direkt1822BankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class Direkt1822BankPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0635178014"));
        assertThat(security.getWkn(), is("ETF127"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("COMSTA.-MSCI EM.MKTS.TRN U.ETF INHABER-ANTEILE I O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-12-01T10:30:52")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(13)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 123456/10.00 | Limit 40,99 EUR"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(539.48))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(532.58))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.95 + 1.95))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0392495023"));
        assertThat(security.getWkn(), is("ETF114"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("COMSTA.-MSCI PACIFIC TRN U.ETF INHABER-ANTEILE I O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-12-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.9619)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0444971666"));
        assertThat(security.getWkn(), is("A1CU1W"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("THREADNEEDLE L-GLOBAL TECHNOL. NAMENS-ANTEILE AU USD O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-03-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.6646)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(59.94))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInEUR()
    {
        Security security = new Security("THREADNEEDLE L-GLOBAL TECHNOL. NAMENS-ANTEILE AU USD O.N.", CurrencyUnit.EUR);
        security.setIsin("LU0444971666");
        security.setWkn("A1CU1W");

        Client client = new Client();
        client.addSecurity(security);

        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-03-01T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.6646)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testWertpapierKauf04()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0635178014"));
        assertThat(security.getWkn(), is("ETF127"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("LYXOR MSCI EMERG.MKTS U.ETF INHABER-ANTEILE I O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-07-09T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 333333/33.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4020.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4000.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0290355717"));
        assertThat(security.getWkn(), is("DBX0AC"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("XTR.II EUROZONE GOV.BOND INHABER-ANTEILE 1C O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-07T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2.0913)));
        assertThat(entry.getSource(), is("Kauf05.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(548.10))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(540.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(08.10))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US10111G1011"));
        assertThat(security.getWkn(), is("111111"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("EXXON MOBIL CORP. REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-01T11:11:11")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 111111111/11.00 | Limit 11,00 EUR"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(111.11))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(114.54))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.43))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0635178014"));
        assertThat(security.getWkn(), is("ETF127"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("LYXOR MSCI EMERG.MKTS U.ETF INH.-AN. I O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-03-29T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.0702)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 123456/78.00"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.18))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.18))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0553164731"), hasWkn("A1C7Y8"), hasTicker(null), //
                        hasName("DJE - ZINS + DIVIDENDE INHABER-ANTEILE PA EUR O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-11-18T00:00"), hasShares(11.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftragsnummer 123456/12.00"), //
                        hasAmount("EUR", 1898.16), hasGrossValue("EUR", 1898.16), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check tax refund transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2024-11-18T00:00"), hasShares(11.00), //
                        hasSource("Verkauf03.txt"), //
                        hasNote("Auftragsnummer 123456/12.00"), //
                        hasAmount("EUR", 3.49), hasGrossValue("EUR", 3.49), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende01()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00BYM31M36"));
        assertThat(security.getWkn(), is("A2AFCX"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ISHSIV-FA.AN.HI.YI.CO.BD U.ETF REGISTERED SHARES USD O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-01-03T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(920)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 504xxxxxxxx"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(68.87))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(93.56))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(23.41 + 1.28))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(113.16))));
    }

    @Test
    public void testDividende01WithSecurityInEUR()
    {
        Security security = new Security("ISHSIV-FA.AN.HI.YI.CO.BD U.ETF REGISTERED SHARES USD O.N.", CurrencyUnit.EUR);
        security.setIsin("IE00BYM31M36");
        security.setWkn("A2AFCX");

        Client client = new Client();
        client.addSecurity(security);

        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-01-03T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(920)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 504xxxxxxxx"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(68.87))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(93.56))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(23.41 + 1.28))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende02()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000ETFL078"));
        assertThat(security.getWkn(), is("ETFL07"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("DEKA EO STOXX SEL.DIV.30 U.ETF INHABER-ANTEILE"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-10T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(118.901)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 56838700560"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.13))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.13))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende03()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US30231G1022"));
        assertThat(security.getWkn(), is("852549"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("EXXON MOBIL CORP. REGISTERED SHARES O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(6)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 11111111122"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.72))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.38))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.66))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5.22))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        Security security = new Security("EXXON MOBIL CORP. REGISTERED SHARES O.N.", CurrencyUnit.EUR);
        security.setIsin("US30231G1022");
        security.setWkn("852549");

        Client client = new Client();
        client.addSecurity(security);

        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-12T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(6)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. 11111111122"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.72))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.38))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.66))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende04()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("CH0012032048"), hasWkn("855167"), hasTicker(null), //
                        hasName("ROCHE HOLDING AG INHABER-GENUSSCHEINE O.N."), //
                        hasCurrencyCode("CHF"))));

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-03-20T00:00"), hasShares(20.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 45977616427"), //
                        hasAmount("EUR", 108.02), hasGrossValue("EUR", 198.39), //
                        hasForexGrossValue("CHF", 192.00), //
                        hasTaxes("EUR", 69.44 + 19.84 + 1.09), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende24WithSecurityInEUR()
    {
        Security security = new Security("ROCHE HOLDING AG INHABER-GENUSSCHEINE O.N.", CurrencyUnit.EUR);
        security.setIsin("CH0012032048");
        security.setWkn("855167");

        Client client = new Client();
        client.addSecurity(security);

        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-03-20T00:00"), hasShares(20.00), //
                        hasSource("Dividende04.txt"), //
                        hasNote("Abrechnungsnr. 45977616427"), //
                        hasAmount("EUR", 108.02), hasGrossValue("EUR", 198.39), //
                        hasTaxes("EUR", 69.44 + 19.84 + 1.09), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testVorabpauschale01()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU2611732632"), hasWkn("ETF198"), hasTicker(null), //
                        hasName("AIS AMUNDI DJ SWITZTIT30 UCITS ETF DIST INH ANTEILE"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-01-04T00:00"), hasShares(16.0871), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote("Abrechnungsnr. 72251346037"), //
                        hasAmount("EUR", 0.63), hasGrossValue("EUR", 0.63), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDepoteinlieferung01()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotuebertrag01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BJ0KDQ92"), hasWkn("A1XB5U"), hasTicker(null), //
                        hasName("XTR.(IE) - MSCI WORLD REGISTERED SHARES 1C O.N."), //
                        hasCurrencyCode("EUR"))));

        // check unsupported transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        outboundDelivery( //
                                        hasDate("2022-03-30T00:00"), hasShares(3.00), //
                                        hasSource("Depotuebertrag01.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testKontoauszug01()
    {
        Direkt1822BankPDFExtractor extractor = new Direkt1822BankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-10-17"), hasAmount("EUR", 1000.00), //
                        hasSource("Kontoauszug01.txt"), hasNote("Gutschrift (Überweisung)"))));
    }
}
