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

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
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
public class OnvistaPDFExtractorTest
{

    @Test
    public void testSanityCheckForBankName() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
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

    private Security assertSecuritySell(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000A1KRRB1"));
        assertThat(security.getName(), is("Porsche Automobil Holding SE Inhaber-Bezugsrechte auf VZO"));

        return security;
    }

    private Security assertSecurityErtragsgutschriftKupon(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
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
    
    @Test
    public void testErtragsgutschriftDividende() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
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
    public void testErtragsgutschriftKupon() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("OnvistaErtragsgutschriftKupon.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurityErtragsgutschriftKupon(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDate(), is(LocalDate.parse("2010-11-17")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.14))));
        assertThat(transaction.getShares(), is(Values.Share.factorize(1)));
    }


    @Test
    public void testWertpapierKaufAktien() throws IOException // Aktien
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
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
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-01-14")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.05))));
    }

    @Test
    public void testWertpapierVerkauf() throws IOException
    {
        OnvistaPDFExtractor extractor = new OnvistaPDFExtractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
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
        assertThat(entry.getPortfolioTransaction().getDate(), is(is(LocalDate.parse("2011-04-12"))));
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
            String strip(File file) throws IOException
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


    private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
