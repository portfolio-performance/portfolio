package name.abuchen.portfolio.datatransfer.pdf.onvista;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNotNull;
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

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.OnvistaPDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

import org.junit.Test;

@SuppressWarnings("nls")
public class OnvistaPDFExtractorTest
{

    @Test
    public void testSanityCheckForBankName() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return "some text";
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), instanceOf(UnsupportedOperationException.class));
    }

    private Security assertSecurityBuyAktien(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000CBK1001"));
        assertThat(security.getName(), is("Commerzbank AG Inhaber-Aktien o.N."));

        return security;
    }
    
    private Security assertSecurityBuyBezugsrechte(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1KRCZ2"));
        assertThat(security.getName(), is("Commerzbank AG Inhaber-Bezugsrechte"));

        return security;
    }

    private Security assertSecuritySell(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1KRRB1"));
        assertThat(security.getName(), is("Porsche Automobil Holding SE Inhaber-Bezugsrechte auf VZO"));

        return security;
    }

    private Security assertSecurityErtragsgutschriftKupon(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000TUAG117"));
        assertThat(security.getName(), is("5,5% TUI AG Wandelanl.v.2009(2014)"));

        return security;
    }

    private Security assertSecurityErtragsgutschriftDividende(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000CBK1001"));
        assertThat(security.getName(), is("Commerzbank AG Inhaber-Aktien o.N."));
        
        return security;
    }
    
    private Security assertSecurityEinloesung(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000TUAG117"));
        assertThat(security.getName(), is("TUI AG Wandelanl.v.2009(2014)"));
        
        return security;
    }
    
    private Security assertSecurityErtragsgutschriftErtraegnisgutschrift(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0140355917"));
        assertThat(security.getName(), is("Allianz Euro Bond Fund Inhaber-Anteile A (EUR) o.N."));
        
        return security;
    }
    
    private Security assertSecurityWertpapieruebertrag(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0140355917"));
        assertThat(security.getName(), is("Allianz PIMCO Euro Bd Tot.Ret. Inhaber-Anteile A (EUR) o.N."));
        
        return security;
    }
    
    private Security assertSecurityErtragsgutschriftDividendeReinvestition(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0005557508"));
        assertThat(security.getName(), is("Deutsche Telekom AG Namens-Aktien o.N."));
        
        return security;
    }
    
    private Security assertSecurityErtragsgutschriftDividendeReinvestitionTarget(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000A1TNRX5"));
        assertThat(security.getName(), is("Deutsche Telekom AG Dividend in Kind-Cash Line"));
        
        return security;
    }
    
    private Security assertSecurityKapitalherabsetzungOriginal(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0008032004"));
        assertThat(security.getName(), is("Commerzbank AG Inhaber-Aktien o.N."));

        return security;
    }

    private Security assertSecurityKapitalherabsetzungTransfer(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000CBKTLR7"));
        assertThat(security.getName(), is("Commerzbank AG Inhaber-Teilrechte"));

        return security;
    }

    private Security assertSecurityKapitalherabsetzungZiel(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE000CBK1001"));
        assertThat(security.getName(), is("Commerzbank AG konv.Inhaber-Aktien o.N."));

        return security;
    }
    
    private Security assertSecurityKapitalherhöhung(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1KRJ01"));
        assertThat(security.getName(), is("Commerzbank AG Inhaber-Erwerbsrechte"));

        return security;
    }

    private Security assertSecurityEinAusbuchungDividendenRechte(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A2AA2C3"));
        assertThat(security.getName(), is("Deutsche Telekom AG Dividend in Kind-Cash Line"));

        return security;
    }
    
    private Security assertSecurityUmtauschZiel(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("LU0165915215"));
        assertThat(security.getName(), is("AGIF-Allianz Euro Bond Inhaber Anteile A (EUR) o.N."));

        return security;
    }

    private Security assertSecurityUmtauschOriginal(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("LU0140355917"));
        assertThat(security.getName(), is("Allianz Euro Bond Fund Inhaber-Anteile A (EUR) o.N."));

        return security;
    }
    
    private Security assertSecurityZwangsabfindung(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000SKYD000"));
        assertThat(security.getName(), is("Sky Deutschland AG Namens-Aktien o.N."));

        return security;
    }

    private Security assertSecurityDividendeAbfindung(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1TNRX5"));
        assertThat(security.getName(), is("Deutsche Telekom AG Dividend in Kind-Cash Line"));

        return security;
    }

    private Security assertFirstSecurityDepotauszug(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000PAH0038"));
        assertThat(security.getName(), is("Porsche Automobil Holding SE Inhaber-Vorzugsaktien o.St.o.N"));

        return security;
    }


    @Test
    public void testErtragsgutschriftDividende() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaErtragsgutschriftDividende.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurityErtragsgutschriftDividende(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDate(), is(LocalDate.parse("2016-04-21")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(50)));
    }

    @Test
    public void testErtragsgutschriftDividende2() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<>();

        File file = new File("OnvistaErtragsgutschriftDividende2.txt");
        List<Item> results = extractor.extract(Arrays.asList(file), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));

        // transaction #1
        Security security = results.stream() //
                        .filter(i -> i instanceof Extractor.SecurityItem) //
                        .map(i -> i.getSecurity()) //
                        .filter(s -> "FR0010296061".equals(s.getIsin())) //
                        .findFirst().get();
        assertThat(security.getName(), is("Lyxor ETF MSCI USA Actions au Porteur D-EUR o.N."));

        AccountTransaction transaction = results.stream() //
                        .filter(i -> i instanceof Extractor.TransactionItem) //
                        .filter(i -> "FR0010296061".equals(i.getSecurity().getIsin())) //
                        .map(i -> (AccountTransaction) ((Extractor.TransactionItem) i).getSubject()).findFirst().get();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDate(), is(LocalDate.parse("2016-12-16")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.8))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1.0545)));

        // transaction #2
        security = results.stream() //
                        .filter(i -> i instanceof Extractor.SecurityItem) //
                        .map(i -> i.getSecurity()) //
                        .filter(s -> "FR0010315770".equals(s.getIsin())) //
                        .findFirst().get();
        assertThat(security.getName(), is("Lyxor ETF MSCI WORLD FCP Actions au Port.D-EUR o.N."));

        transaction = results.stream() //
                        .filter(i -> i instanceof Extractor.TransactionItem) //
                        .filter(i -> "FR0010315770".equals(i.getSecurity().getIsin())) //
                        .map(i -> (AccountTransaction) ((Extractor.TransactionItem) i).getSubject()).findFirst().get();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDate(), is(LocalDate.parse("2016-12-16")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.8))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1.2879)));
    }

    @Test
    public void testErtragsgutschriftKupon() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaErtragsgutschriftKupon.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = assertSecurityErtragsgutschriftKupon((SecurityItem) item.get());

        // check transaction
        Optional<Item> transactionItem = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(transactionItem.isPresent(), is(true));
        assertThat(transactionItem.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) transactionItem.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDate(), is(LocalDate.parse("2010-11-17")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.14))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.41))));
    }
    
    @Test
    public void testErtragsgutschriftErtraegnisgutschrift() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaErtragsgutschriftErtraegnisgutschrift.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurityErtragsgutschriftErtraegnisgutschrift(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDate(), is(LocalDate.parse("2015-03-04")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.69))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(28)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.96))));
    }

    @Test
    public void testErtragsgutschriftErtraegnisgutschrift2() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaErtragsgutschriftErtraegnisgutschrift2.txt");
            }
        };
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream() //
                        .filter(i -> i instanceof Extractor.SecurityItem) //
                        .map(i -> i.getSecurity()) //
                        .findFirst().get();
        assertThat(security.getIsin(), is("DE0002635307"));
        assertThat(security.getName(), is("iSh.STOXX Europe 600 U.ETF DE Inhaber-Anteile"));

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDate(), is(LocalDate.parse("2016-12-15")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.16))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(5.8192)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

    @Test
    public void testWertpapierKaufAktien() throws IOException // Aktien
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaKaufAktien.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityBuyAktien(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(59.55))));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-01-12")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.05))));
    }

    @Test
    public void testWertpapierKaufBezugsrechte() throws IOException // Aktien
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaKaufBezugsrechte.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityBuyBezugsrechte(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.40))));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2011-05-30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(8)));
    }
    
    @Test
    public void testWertpapierVerkauf() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaVerkauf.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecuritySell(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.45))));
        assertThat(entry.getPortfolioTransaction().getDate(), is(is(LocalDate.parse("2011-04-08"))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.75))));
    }
    
    @Test
    public void testWertpapierEinloesung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaEinloesung.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityEinloesung(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(51.85))));
        assertThat(entry.getPortfolioTransaction().getDate(), is(is(LocalDate.parse("2014-11-17"))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.45))));
    }

    @Test
    public void testWertpapieruebertrag() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaWertpapieruebertragEingang.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityWertpapieruebertrag(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.TRANSFER_IN));

        assertThat(entry.getPortfolioTransaction().getDate(), is(is(LocalDate.parse("2011-12-02"))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(28)));
    }
    
    @Test
    public void testErtragsgutschriftDividendeReinvestition() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaErtragsgutschriftDividendeReinvestition.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(4));

        // check security
        Security security = assertSecurityErtragsgutschriftDividendeReinvestition(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDate(), is(LocalDate.parse("2013-05-17")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.50))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(25)));
        
        assertSecurityErtragsgutschriftDividendeReinvestitionTarget(
                        results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList()).get(1));
        Item reinvestItem = results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList()).get(0);

        // check transaction
        assertThat(reinvestItem.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry2 = (BuySellEntry) reinvestItem.getSubject();
        assertThat(entry2.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry2.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry2.getPortfolioTransaction().getDate(), is(LocalDate.parse("2013-05-17")));
        assertThat(entry2.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
        assertThat(entry2.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.50))));
    }
    
    @Test
    public void testKapitalherabsetzung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaKapitalherabsetzung.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(7));

        // check security
        Security security = assertSecurityKapitalherabsetzungOriginal(results);

        // check transaction (original security)
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction transaction = (PortfolioTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDate(), is(LocalDate.parse("2013-04-24")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(55)));

        assertSecurityKapitalherabsetzungTransfer(results.stream().filter(i -> i instanceof SecurityItem)
                        .collect(Collectors.toList()).get(1));
        Item transferItem = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(1);

        // check transaction (transfer security, in)
        assertThat(transferItem.getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry2 = (PortfolioTransaction) transferItem.getSubject();
        assertThat(entry2.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(entry2.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry2.getDate(), is(LocalDate.parse("2013-04-24")));
        assertThat(entry2.getShares(), is(Values.Share.factorize(5.5)));

        assertSecurityKapitalherabsetzungZiel(results.stream().filter(i -> i instanceof SecurityItem)
                        .collect(Collectors.toList()).get(2));
        Item transferItem2 = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(2);

        // check transaction (transfer security, out)
        assertThat(transferItem2.getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry3 = (PortfolioTransaction) transferItem2.getSubject();
        assertThat(entry3.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(entry3.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry3.getDate(), is(LocalDate.parse("2013-04-24")));
        assertThat(entry3.getShares(), is(Values.Share.factorize(5)));

        assertSecurityKapitalherabsetzungZiel(results.stream().filter(i -> i instanceof SecurityItem)
                        .collect(Collectors.toList()).get(2));
        Item targetItem = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(3);

        // check transaction (target security)
        assertThat(targetItem.getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry4 = (PortfolioTransaction) targetItem.getSubject();
        assertThat(entry4.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(entry4.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry4.getDate(), is(LocalDate.parse("2013-04-24")));
        assertThat(entry4.getShares(), is(Values.Share.factorize(5)));
    }
    
    @Test
    public void testKapitalerhoehung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaKapitalerhoehung.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityKapitalherhöhung(results);

        // check delivery transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry = (PortfolioTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getDate(), is(is(LocalDate.parse("2011-04-06"))));
        assertThat(entry.getShares(), is(Values.Share.factorize(25)));
    }

    @Test
    public void testEinbuchungDividendenRechte() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaEinbuchungDividendenRechte.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityEinAusbuchungDividendenRechte(results);

        // check delivery transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry = (PortfolioTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        assertThat(entry.getDate(), is(is(LocalDate.parse("2016-05-25"))));
        assertThat(entry.getShares(), is(Values.Share.factorize(25)));
    }

    @Test
    public void testAusbuchungDividendenRechte() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaAusbuchungDividendenRechte.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityEinAusbuchungDividendenRechte(results);

        // check delivery transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry = (PortfolioTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));

        assertThat(entry.getDate(), is(is(LocalDate.parse("2016-06-21"))));
        assertThat(entry.getShares(), is(Values.Share.factorize(25)));
    }

    @Test
    public void testUmtausch() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaUmtausch.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityBuyBezugsrechte(results);

        // check delivery transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry = (PortfolioTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));

        assertThat(entry.getDate(), is(is(LocalDate.parse("2011-06-06"))));
        assertThat(entry.getShares(), is(Values.Share.factorize(33)));
    }

    @Test
    public void testUmtauschFonds() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaUmtauschFonds.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));

        // check security
        Security security = assertSecurityUmtauschZiel(results);

        // check transaction (target security, in)
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction transaction = (PortfolioTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDate(), is(LocalDate.parse("2015-11-26")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(156.729)));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check Steuererstattung
        Item itemTaxReturn = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(2);
        AccountTransaction entryTaxReturn = (AccountTransaction) itemTaxReturn.getSubject();
        assertThat(entryTaxReturn.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(entryTaxReturn.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.90))));
        assertThat(entryTaxReturn.getDate(), is(is(LocalDate.parse("2015-11-26"))));

        // check security (original)
        assertSecurityUmtauschOriginal(results.stream().filter(i -> i instanceof SecurityItem)
                        .collect(Collectors.toList()).get(1));
        Item targetItem = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(1);

        // check transaction (original security, out)
        assertThat(targetItem.getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry2 = (PortfolioTransaction) targetItem.getSubject();
        assertThat(entry2.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(entry2.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry2.getDate(), is(LocalDate.parse("2015-11-23")));
        assertThat(entry2.getShares(), is(Values.Share.factorize(28)));
        assertThat(entry2.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.86))));
        assertThat(entry2.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.86))));

        // check Steuerbuchung
        Item itemTax = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList()).get(3);
        AccountTransaction entryTax = (AccountTransaction) itemTax.getSubject();
        assertThat(entryTax.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(entryTax.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.86))));
        assertThat(entryTax.getDate(), is(is(LocalDate.parse("2015-11-23"))));
    }

    @Test
    public void testWertpapierVerkaufSpitzeMitSteuerrückerstattung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaVerkaufSpitzeMitSteuerErstattung.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        assertSecurityKapitalherabsetzungTransfer(results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .get());

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.41))));
        assertThat(entry.getPortfolioTransaction().getDate(), is(is(LocalDate.parse("2013-05-06"))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.5)));

        // check Steuererstattung
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        AccountTransaction entryTaxReturn = (AccountTransaction) item.get().getSubject();
        assertThat(entryTaxReturn.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.05))));
        assertThat(entryTaxReturn.getDate(), is(is(LocalDate.parse("2013-05-06"))));

    }

    @Test
    public void testZwangsabfindung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaZwangsabfindung.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityZwangsabfindung(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(167.00))));
        assertThat(entry.getPortfolioTransaction().getDate(), is(is(LocalDate.parse("2015-09-22"))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
    }

    @Test
    public void testDividendeAbfindung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaDividendeAbfindung.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurityDividendeAbfindung(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.50))));
        assertThat(entry.getPortfolioTransaction().getDate(), is(is(LocalDate.parse("2013-06-11"))));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
    }

    @Test
    public void testDepotauszug() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaDepotauszug.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(26));

        // check first security
        Security security = assertFirstSecurityDepotauszug(results);

        // check transaction (first security, in)
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction transaction = (PortfolioTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDate(), is(LocalDate.parse("2010-12-31")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(4)));

        // check second security
        assertSecurityErtragsgutschriftKupon(results.stream().filter(i -> i instanceof SecurityItem)
                        .collect(Collectors.toList()).get(3));
        Item secondItem = results.stream().filter(i -> i instanceof TransactionItem).collect(Collectors.toList())
                        .get(3);

        // check second transaction (second security, in)
        assertThat(secondItem.getSubject(), instanceOf(PortfolioTransaction.class));
        PortfolioTransaction entry2 = (PortfolioTransaction) secondItem.getSubject();
        assertThat(entry2.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(entry2.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(entry2.getDate(), is(LocalDate.parse("2010-12-31")));
        assertThat(entry2.getShares(), is(Values.Share.factorize(1)));

    }

    @Test
    public void testKontoauszugEinzelneBuchung() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaKontoauszugEinzelneBuchung.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDate(), is(LocalDate.parse("2010-10-31")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.66))));

    }

    @Test
    public void testKontoauszugMehrereBuchungen() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaKontoauszugMehrereBuchungen.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDate(), is(LocalDate.parse("2015-04-03")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.62))));

    }
    
    @Test
    public void testMehrereTransaktionenInEinerDatei() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            protected String strip(File file) throws IOException
            {
                return from("OnvistaMultipartKaufVerkauf.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        List<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList());
        assertThat(item.isEmpty(), is(false));
        
        Item firstItem = item.get(0);
        assertNotNull(firstItem);
        assertThat(firstItem.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry firstEntry = (BuySellEntry) firstItem.getSubject();

        assertThat(firstEntry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(firstEntry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(firstEntry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(firstEntry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(firstEntry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(623.49))));
        assertThat(firstEntry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2016-09-02")));
        assertThat(firstEntry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5))));
        
        Item secondItem = item.get(1);
        assertNotNull(secondItem);
        assertThat(secondItem.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry secondEntry = (BuySellEntry) secondItem.getSubject();
       
        assertThat(secondEntry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(secondEntry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));
        assertThat(secondEntry.getPortfolioTransaction().getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(secondEntry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(80)));
        assertThat(secondEntry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2508.47))));
        assertThat(secondEntry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2016-09-02")));
        assertThat(secondEntry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.5))));
    }

    private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
