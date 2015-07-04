package name.abuchen.portfolio.datatransfer;

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

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

import org.junit.Test;

@SuppressWarnings("nls")
public class DeutscheBankPDFExtractorTest
{

    @Test
    public void testSanityCheckForBankName() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
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

    private Security assertSecurity(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000BASF111"));
        assertThat(security.getWkn(), is("BASF11"));
        assertThat(security.getName(), is("BASF SE"));

        return security;
    }

    @Test
    public void testErtragsgutschrift() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("DeutscheBankErtragsgutschrift.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurity(results);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(LocalDate.parse("2014-12-15")));
        assertThat(transaction.getAmount(), is(1495L));
        assertThat(transaction.getShares(), is(Values.Share.factorize(123)));
    }

    @Test
    public void testErtragsgutschriftWhenSecurityExists() throws IOException
    {
        Client client = new Client();
        Security security = new Security("BASF", "DE000BASF111", null, null);
        client.addSecurity(security);

        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(client)
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("DeutscheBankErtragsgutschrift.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check transaction
        AccountTransaction transaction = (AccountTransaction) results.get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
    }

    @Test
    public void testWertpapierKauf() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("DeutscheBankKauf.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        assertSecurity(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(675.50)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-04-08")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(19)));
        assertThat(entry.getPortfolioTransaction().getFees(), is(Values.Amount.factorize(10.50)));
    }

    @Test
    public void testWertpapierKauf2() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("DeutscheBankKauf2.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurity(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(3524.98)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-04-08")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(36)));
        assertThat(entry.getPortfolioTransaction().getFees(), is(Values.Amount.factorize(11.38)));
    }

    @Test
    public void testWertpapierVerkauf() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("DeutscheBankVerkauf.txt");
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        assertSecurity(results);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(2074.71)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(LocalDate.parse("2015-04-08")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(61)));
        assertThat(entry.getPortfolioTransaction().getTaxes(),
                        is(Values.Amount.factorize(122.94) + Values.Amount.factorize(6.76)));
        assertThat(entry.getPortfolioTransaction().getFees(), is(Values.Amount.factorize(7.90 + 0.60 + 2)));
    }

    private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
