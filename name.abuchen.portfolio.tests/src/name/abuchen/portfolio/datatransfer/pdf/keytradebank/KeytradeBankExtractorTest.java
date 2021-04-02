package name.abuchen.portfolio.datatransfer.pdf.keytradebank;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.KeytradeBankPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
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
public class KeytradeBankExtractorTest
{

    @Test
    public void testWertpapierKauf01()
    {
        KeytradeBankPDFExtractor extractor = new KeytradeBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KeytradeBank_Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU1781541179"));
        assertThat(security.getName(), is("LYXOR CORE WORLD"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry t = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(t.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(t.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(t.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-03-15T12:31:50")));
        assertThat(t.getPortfolioTransaction().getShares(), is(Values.Share.factorize(168)));
        assertThat(t.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1994.39)));
        assertThat(t.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.95))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        KeytradeBankPDFExtractor extractor = new KeytradeBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KeytradeBank_Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A14KEB5"));
        assertThat(security.getName(), is("HOME24 SE  INH O.N."));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry t = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(t.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(t.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(t.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-19T11:53:02")));
        assertThat(t.getPortfolioTransaction().getShares(), is(Values.Share.factorize(310)));
        assertThat(t.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(4963.99)));
        assertThat(t.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.95))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        KeytradeBankPDFExtractor extractor = new KeytradeBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KeytradeBank_Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A14KEB5"));
        assertThat(security.getName(), is("HOME24 SE  INH O.N."));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry t = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(t.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(t.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(t.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-19T11:53:03")));
        assertThat(t.getPortfolioTransaction().getShares(), is(Values.Share.factorize(310)));
        assertThat(t.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(4963.99)));
        assertThat(t.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.95))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        KeytradeBankPDFExtractor extractor = new KeytradeBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KeytradeBank_Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007165607"));
        assertThat(security.getName(), is("SARTORIUS AG O.N."));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.orElseThrow(IllegalArgumentException::new).getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry t = (BuySellEntry) item.orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(t.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(t.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(t.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-13T09:56:05")));
        assertThat(t.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22)));
        assertThat(t.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(6118.95)));
        assertThat(t.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.95))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        KeytradeBankPDFExtractor extractor = new KeytradeBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KeytradeBank_Kauf05.txt"), errors);

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.orElseThrow(IllegalArgumentException::new)).getSecurity();
        assertThat(security.getIsin(), is("US6516391066"));
        assertThat(security.getName(), is("NEWMONT MINING CORPORATION"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2016-06-16T09:45:18")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(953.95))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(29.95))));

//        Unit gross = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
//                        .orElseThrow(IllegalArgumentException::new);
//        assertThat(gross.getAmount(), is( Values.Amount.factorize(0.00)));
//        assertThat(gross.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(953.95))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        KeytradeBankPDFExtractor extractor = new KeytradeBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KeytradeBank_Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0JL9W6"));
        assertThat(security.getName(), is("VERBIO VER.BIOENERGIE ON"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-10T15:23:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(310)));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10499.55))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.95))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        KeytradeBankPDFExtractor extractor = new KeytradeBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KeytradeBank_Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0007165607"));
        assertThat(security.getName(), is("SARTORIUS AG O.N."));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-01T12:34:24")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22)));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5409.05))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.95))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        KeytradeBankPDFExtractor extractor = new KeytradeBankPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KeytradeBank_Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.USD);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US6516391066"));
        assertThat(security.getName(), is("NEWMONT MINING CORPORATION"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-09-11T11:09:35")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(938.83))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(29.95))));
    }

    @Test
    public void testDividende01()
    {
        Client client = new Client();

        KeytradeBankPDFExtractor extractor = new KeytradeBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KeytradeBank_Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE0007165607"));
        assertThat(security.getName(), is("SARTORIUS AG O.N."));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.67))));
        assertThat(t.getShares(), is(Values.Share.factorize(22)));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2020-06-29T00:00")));

        assertThat(t.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.70))));
        assertThat(t.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.03))));
        assertThat(t.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende02()
    {
        Client client = new Client();

        KeytradeBankPDFExtractor extractor = new KeytradeBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KeytradeBank_Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "EUR");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE000A0JL9W6"));
        assertThat(security.getName(), is("VERBIO VER.BIOENERGIE  ON"));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(45.65))));
        assertThat(t.getShares(), is(Values.Share.factorize(310)));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));

        assertThat(t.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(62.00))));
        assertThat(t.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.35))));
        assertThat(t.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende03()
    {
        Client client = new Client();

        KeytradeBankPDFExtractor extractor = new KeytradeBankPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KeytradeBank_Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "USD");

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("US6516391066"));
        assertThat(security.getName(), is("NEWMONT MINING CORPORATION"));

        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(t.getDateTime(), is(LocalDateTime.parse("2016-12-08T00:00")));
        assertThat(t.getShares(), is(Values.Share.factorize(25)));
        assertThat(t.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.06))));
        assertThat(t.getGrossValue(), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.25))));
        assertThat(t.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.19))));
        assertThat(t.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.00))));
    }
}
