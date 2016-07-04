package name.abuchen.portfolio.datatransfer.pdf;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class FlatexPDFExtractorTest
{

    @Test
    public void testWertpapierKauf() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexKauf.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertFirstSecurity(results.stream().filter(i -> i instanceof SecurityItem).findFirst());
        assertFirstTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst());

        assertSecondSecurity(results.stream().filter(i -> i instanceof SecurityItem) //
                        .collect(Collectors.toList()).get(1));
        assertSecondTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1));

        assertThirdTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem) //
                        .collect(Collectors.toList()).get(2));

    }

    private Security assertFirstSecurity(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0005194062"));
        assertThat(security.getWkn(), is("519406"));
        assertThat(security.getName(), is("BAYWA AG VINK.NA. O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    private void assertFirstTransaction(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5893_10L)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2014-01-28")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(150_000000L));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 5_90L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(39.248))));
    }

    private Security assertSecondSecurity(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE0008402215"));
        assertThat(security.getWkn(), is("840221"));
        assertThat(security.getName(), is("HANN.RUECK SE NA O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    private void assertSecondTransaction(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5954_80L)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2014-01-28")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(100_000000L));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 5_90L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(59.489))));
    }

    private void assertThirdTransaction(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 5843_00L)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2014-01-28")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(100_000000L));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, 5_90L)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 100_00L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(59.489))));
    }

    @Test
    public void testWertpapierKauf2() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexKauf2.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0392495023"));
        assertThat(security.getWkn(), is("ETF114"));
        assertThat(security.getName(), is("C.S.-MSCI PACIF.T.U.ETF I"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(50.30)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-12-03")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(5.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
    }

    @Test
    public void testKontoauszug() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexKontoauszug.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDate(), is(LocalDate.parse("2016-01-29")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 1100_00L)));
        
        Item item2 = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(1);
        assertThat(item2.getSubject(), instanceOf(AccountTransaction.class));
        transaction = (AccountTransaction) item2.getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDate(), is(LocalDate.parse("2016-03-31")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.00)));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
    }
    
    @Test
    public void testKontoauszug2() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexKontoauszug2.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDate(), is(LocalDate.parse("2016-01-26")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 15000_00L)));
        
        Item item2 = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(1);
        assertThat(item2.getSubject(), instanceOf(AccountTransaction.class));
        transaction = (AccountTransaction) item2.getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDate(), is(LocalDate.parse("2016-03-31")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.00)));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
    }

    @Test
    public void testErtragsgutschrift() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexErtragsgutschrift.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0008402215"));
        assertThat(security.getWkn(), is("840221"));
        assertThat(security.getName(), is("HANN.RUECK SE NA O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(LocalDate.parse("2014-05-08")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 795_15L)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(360)));
    }

    @Test
    public void testErtragsgutschrift2() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexErtragsgutschrift2.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE1234567890"));
        assertThat(security.getWkn(), is("AB1234"));
        assertThat(security.getName(), is("ISH.FOOBAR 12345666 x.EFT"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(LocalDate.parse("2014-01-15")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(55.55)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(99)));
    }

    @Test
    public void testZinsgutschriftInland() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexZinsgutschriftInland.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE1234567890"));
        assertThat(security.getWkn(), is("AB1234"));
        assertThat(security.getName(), is("ISH.FOOBAR 12345666 x.EFT"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(LocalDate.parse("2016-04-28")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(73.75)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1000)));
    }
    
    @Test
    public void testWertpapierVerkauf() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexVerkauf.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000US9RGR9"));
        assertThat(security.getWkn(), is("US9RGR"));
        assertThat(security.getName(), is("UBS AG LONDON 14/16 RWE"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(16508.16)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2016-01-26")));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("EUR", Values.Amount.factorize(5.90))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(250)));
    }
    
    @Test
    public void testWertpapierÜbertrag() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexDepoteingang.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000US9RGR9"));
        assertThat(security.getName(), is("UBS AG LONDON 14/16 RWE"));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry = (PortfolioTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getAmount(), is(Values.Amount.factorize(7517.50)));
        assertThat(entry.getDate(), is(LocalDate.parse("2015-11-24")));
        assertThat(entry.getShares(), is(Values.Share.factorize(250)));
    }
    
    @Test
    public void testWertpapierAusgang() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexDepotausgang.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000CM31SV9"));
        assertThat(security.getName(), is("COMMERZBANK INLINE09EO/SF"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(2867.88)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2009-12-02")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(325)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 382_12L)));
    }
    
    @Test
    public void testWertpapierAusgang2() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexDepotausgang2.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000CK1Q3N7"));
        assertThat(security.getName(), is("COMMERZBANK INLINE11EO/SF"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(0.20)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2011-07-18")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
    }
    
    @Test
    public void testWertpapierBestandsausbuchung() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexBestandsausbuchung.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertFirstSecurityBestandsausbuchung(results.stream().filter(i -> i instanceof SecurityItem).findFirst());
        assertFirstTransactionBestandsausbuchung(results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst());

        assertSecondSecurityBestandsausbuchung(results.stream().filter(i -> i instanceof SecurityItem) //
                        .collect(Collectors.toList()).get(1));
        assertSecondTransactionBestandsausbuchung(results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1));
        assertThirdSecurityBestandsausbuchung(results.stream().filter(i -> i instanceof SecurityItem) //
                        .collect(Collectors.toList()).get(2));
        assertThirdTransactionBestandsausbuchung(results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(2));
                        
    }
    
    private Security assertFirstSecurityBestandsausbuchung(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000CB81KN1"));
        assertThat(security.getName(), is("COMMERZBANK PUT10 EOLS"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    private void assertFirstTransactionBestandsausbuchung(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 0_00L)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2010-03-16")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(2000_000000L));
    }

    private Security assertSecondSecurityBestandsausbuchung(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000CM3C8A3"));
        assertThat(security.getName(), is("COMMERZBANK CALL10 EO/DL"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    private void assertSecondTransactionBestandsausbuchung(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 0_00L)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2010-03-16")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(1250_000000L));
    }
    
    private Security assertThirdSecurityBestandsausbuchung(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000CM3C896"));
        assertThat(security.getName(), is("COMMERZBANK CALL10 EO/DL"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        return security;
    }

    private void assertThirdTransactionBestandsausbuchung(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.TRANSFER_OUT));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 0_00L)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2010-03-16")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(750_000000L));
    }
    
    @Test
    public void testWertpapierBestandsausbuchungNeuesFormat() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexBestandsausbuchung2.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item;

        // security
        item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000SG0WRD3"));
        assertThat(security.getWkn(), is("SG0WRD"));
        assertThat(security.getName(), is("SG EFF. TURBOL ZS"));

        item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(111.22)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-09-28")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(83)));
    }
    
    
    @Test
    public void testZinsBelastung() throws IOException
    {
        FlatexPDFExctractor extractor = new FlatexPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("FlatexZinsBelastung.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDate(), is(LocalDate.parse("2010-09-30")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(0.00)));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
        
        Item item2 = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(1);
        assertThat(item2.getSubject(), instanceOf(AccountTransaction.class));
        transaction = (AccountTransaction) item2.getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDate(), is(LocalDate.parse("2010-12-31")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(-0.20)));
        assertThat(transaction.getCurrencyCode(), is("EUR"));
    }
    

   private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
