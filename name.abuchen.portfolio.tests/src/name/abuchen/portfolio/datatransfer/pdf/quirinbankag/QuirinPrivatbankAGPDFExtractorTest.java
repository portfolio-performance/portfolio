package name.abuchen.portfolio.datatransfer.pdf.quirinbankag;

import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.QuirinBankAGPDFExtractor;
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
public class QuirinPrivatbankAGPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0690964092"));
        assertThat(security.getWkn(), is("DBX0MF"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("db x-tr.II Gl Sovereign ETF Inhaber-Anteile 1D EUR o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-12-30T12:46:28")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(140)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Referenz-Nr 28522373"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30090.76))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30085.86))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.90))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0002643889"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShares PLC - S&P 500 Index Fd Bearer Shares (Dt. Zert.) o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2010-02-10T17:08:23")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(515)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Referenz O:000481758:1"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3997.09))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3996.26))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.08 + 0.02 + 0.69 + 0.04))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US4282361033"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Hewlett-Packard Co. Registered Shares DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2010-08-20T15:46:50")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(150)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Referenz O:000745405:1"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4749.86))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4734.08))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.78))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5999.50))));
    }

    @Test
    public void testWertpapierKauf03WithSecurityInEUR()
    {
        Security security = new Security("Hewlett-Packard Co. Registered Shares DL -,01", CurrencyUnit.EUR);
        security.setIsin("US4282361033");

        Client client = new Client();
        client.addSecurity(security);

        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(client);

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

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2010-08-20T15:46:50")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(150)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertThat(entry.getNote(), is("Referenz O:000745405:1"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4749.86))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4734.08))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.78))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B0M63177"));
        assertThat(security.getWkn(), is("A0HGWC"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShs MSCI EM U.ETF USD (D) Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-12-06T16:52:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(325)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Referenz-Nr 123452676"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12766.68))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13569.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(752.05 + 41.36))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.81 + 8.50))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007873200"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Bayer.Hypo- und Vereinsbank AG DAX Indexzert(2006/unlim.)"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2009-11-23T11:00:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Referenz O:000409887:1"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1261.61))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1265.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.39))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0002643889"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShares PLC - S&P 500 Index Fd Bearer Shares (Dt. Zert.) o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2010-05-07T16:20:11")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(515)));
        assertThat(entry.getSource(), is("Verkauf03.txt"));
        assertThat(entry.getNote(), is("Referenz O:000591758:1"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4515.86))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4660.88))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(127.15 + 7.00 + 10.18))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.69))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0327757729"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("F.Temp.Inv.Fds-T.Growth (EUR) Namens-Anteile A (acc.)DL o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2009-11-23T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(378.362)));
        assertThat(entry.getSource(), is("Verkauf04.txt"));
        assertThat(entry.getNote(), is("Referenz O:000409894:1"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3046.43))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3052.93))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.50))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4597.10))));
    }

    @Test
    public void testWertpapierVerkauf04WithSecurityInEUR()
    {
        Security security = new Security("F.Temp.Inv.Fds-T.Growth (EUR) Namens-Anteile A (acc.)DL o.N.", CurrencyUnit.EUR);
        security.setIsin("LU0327757729");

        Client client = new Client();
        client.addSecurity(security);

        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2009-11-23T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(378.362)));
        assertThat(entry.getSource(), is("Verkauf04.txt"));
        assertThat(entry.getNote(), is("Referenz O:000409894:1"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3046.43))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3052.93))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.50))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende01()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0D8Q07"));
        assertThat(security.getWkn(), is("A0D8Q0"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShare.EURO STOXX UCITS ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-16T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(700)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertThat(transaction.getNote(), is("Referenz-Nr 12345858"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(343.46))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(421.22))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(73.71 + 4.05))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende02()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007236101"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Siemens AG Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-01-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(52)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertThat(transaction.getNote(), is("Referenz DZ:255990"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(83.20))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(83.20))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende03()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US8740391003"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Taiwan Semiconduct.Manufact.Co Reg.Shs (Spons.ADRs) 5/TA 10"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(350)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertThat(transaction.getNote(), is("Referenz DZ:368384"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(92.67))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(124.50))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.90 + 6.10 + 0.34 + 0.49))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(163.08))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        Security security = new Security("Taiwan Semiconduct.Manufact.Co Reg.Shs (Spons.ADRs) 5/TA 10", CurrencyUnit.EUR);
        security.setIsin("US8740391003");

        Client client = new Client();
        client.addSecurity(security);

        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-27T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(350)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertThat(transaction.getNote(), is("Referenz DZ:368384"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(92.67))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(124.50))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.90 + 6.10 + 0.34 + 0.49))));
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
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

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
                        hasIsin("IE00B3F81G20"), hasWkn("A0RGER"), hasTicker(null), //
                        hasName("iShsIII-MSCI EM Sm.Cap U.ETF Registered Shares o.N."), //
                        hasCurrencyCode("USD"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-07-26T00:00"), hasShares(1.9983), //
                        hasSource("Dividende04.txt"), hasNote("Referenz-Nr 12345858"), //
                        hasAmount("EUR", 1.41), hasGrossValue("EUR", 1.41), hasForexGrossValue("USD", 1.57), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf27WithSecurityInEUR()
    {
        Security security = new Security("iShsIII-MSCI EM Sm.Cap U.ETF Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("IE00B3F81G20");
        security.setWkn("A0RGER");

        Client client = new Client();
        client.addSecurity(security);

        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check check buy sell transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-07-26T00:00"), hasShares(1.9983), //
                        hasSource("Dividende04.txt"), hasNote("Referenz-Nr 12345858"), //
                        hasAmount("EUR", 1.41), hasGrossValue("EUR", 1.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDepotauszug01()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(9L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-28T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(3000.00)));
        assertThat(transaction.getSource(), is("Depotauszug01.txt"));
        assertThat(transaction.getNote(), is("Kontoübertrag 1197537 Ref.: 86991330"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-19T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(5000.00)));
        assertThat(transaction.getSource(), is("Depotauszug01.txt"));
        assertThat(transaction.getNote(), is("Sammelgutschrift Ref.: 105892216"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.84)));
        assertThat(transaction.getSource(), is("Depotauszug01.txt"));
        assertThat(transaction.getNote(), is("Interne Buchung Ref.: 110934428"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-27T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2000.00)));
        assertThat(transaction.getSource(), is("Depotauszug01.txt"));
        assertThat(transaction.getNote(), is("Überweisungsgutschrift Inland Ref.: 106509889"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-19T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(5002.84)));
        assertThat(transaction.getSource(), is("Depotauszug01.txt"));
        assertThat(transaction.getNote(), is("Rücküberweisung Inland Ref.: 106317528"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-12T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(36.82)));
        assertThat(transaction.getSource(), is("Depotauszug01.txt"));
        assertThat(transaction.getNote(), is("Steueroptimierung Ref.: 135435928"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-01-03T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.40)));
        assertThat(transaction.getSource(), is("Depotauszug01.txt"));
        assertThat(transaction.getNote(), is("Steueroptimierung Ref.: 107410183"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(5.75)));
        assertThat(transaction.getSource(), is("Depotauszug01.txt"));
        assertThat(transaction.getNote(), is("Vermögensverwaltungshonorar Ref.: 94613532"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-09-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(6.98)));
        assertThat(transaction.getSource(), is("Depotauszug01.txt"));
        assertThat(transaction.getNote(), is("Vermögensverwaltungshonorar Ref.: 97492689"));
    }

    @Test
    public void testDepotauszug02()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-12-31T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.70)));
        assertThat(transaction.getSource(), is("Depotauszug02.txt"));
        assertThat(transaction.getNote(), is("Vermögensverwaltungshonorar Ref.: 123458336"));
    }

    @Test
    public void testDepotauszug03()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(4L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-06-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(75.00)));
        assertThat(transaction.getSource(), is("Depotauszug03.txt"));
        assertThat(transaction.getNote(), is("Flatrate Gebühren 01.06.2010 - 30.06.2010"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-06-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(29.55)));
        assertThat(transaction.getSource(), is("Depotauszug03.txt"));
        assertThat(transaction.getNote(), is("Volumen Fee Gebühren 01.04.2010 - 30.06.2010"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-06-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.61)));
        assertThat(transaction.getSource(), is("Depotauszug03.txt"));
        assertThat(transaction.getNote(), is("Haben-Zinsen Kontoabschluss Ref: KA-0139907281"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-06-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.28)));
        assertThat(transaction.getSource(), is("Depotauszug03.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0139925023"));
    }

    @Test
    public void testDepotauszug04()
    {
        QuirinBankAGPDFExtractor extractor = new QuirinBankAGPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Depotauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(43));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(43L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(75.00)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Flatrate Gebühren 01.07.2010 - 30.07.2010"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.73)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144748177"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(3.77)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144754954"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.59)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144767171"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.28)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144772794"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.24)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144774420"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.33)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144775628"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.19)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144776788"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.65)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144794566"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.52)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144796585"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.39)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144798144"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.19)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144802453"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.20)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144802618"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(10.57)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144827860"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.07)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144835278"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.52)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144836892"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.35)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144976464"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.56)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144989517"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.73)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144994720"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.24)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144996297"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.26)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144996479"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.72)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Steuerbuchung Abgeltungsteuer Ref: H-0144999672"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.63)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0140363002 Ref: KA -0144680022"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.03)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0303756539 Ref: KA-0144683460"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(1.02)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0075056555 Ref: KA-0144689194"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.85)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0066902890 Ref: KA-0144691722"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.90)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0121747215 Ref: KA-0144692430"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(3.12)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision DE0008490822 Ref: KA-0144692886"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.78)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0052750758 Ref: KA-0144693436"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.10)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0303756539 Ref: KA-0144700356"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.59)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0140363002 Ref: KA-0144701132"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.88)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0121747215 Ref: KA-0144701836"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.93)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0052750758 Ref: KA-01 44703846"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.98)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovisio n LU0075056555 Ref: KA-0144703918"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.89)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0066902890 Ref: KA-0144717889"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.94)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision DE0008490822 Ref: KA-0144723419"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(4.83)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0052750758 Ref: KA-0144724147"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.85)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0121747215 Ref: KA-0144726719"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.05)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0303756539 Ref: KA-0144732844"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.61)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision DE0008490822 Ref: KA-0144735471"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.86)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0066902890 Ref: KA-0144736296"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.94)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0075056555 Ref: KA-0144736391"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2010-07-30T00:00")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(2.58)));
        assertThat(transaction.getSource(), is("Depotauszug04.txt"));
        assertThat(transaction.getNote(), is("Bestandsprovision LU0140363002 Ref: KA-01 44738244"));
    }
}
