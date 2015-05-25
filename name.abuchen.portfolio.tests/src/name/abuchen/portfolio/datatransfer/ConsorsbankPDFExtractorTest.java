package name.abuchen.portfolio.datatransfer;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

@SuppressWarnings("nls")
public class ConsorsbankPDFExtractorTest
{
    private Security assertSecurity(List<Item> results, boolean mustHaveIsin)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        if (mustHaveIsin)
            assertThat(security.getIsin(), is("LU0392494562"));
        assertThat(security.getWkn(), is("ETF110"));
        assertThat(security.getName(), is("COMS.-MSCI WORL.T.U.ETF I"));

        return security;
    }

    @Test
    public void testErtragsgutschrift() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from("ConsorsbankErtragsgutschrift.txt");
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("t")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = assertSecurity(results, false);

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(Dates.date("2015-05-08")));
        assertThat(transaction.getAmount(), is(326_90L));
        assertThat(transaction.getShares(), is(370_00000L));
    }

    @Test
    public void testWertpapierKauf() throws IOException
    {
        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankKauf.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        assertSecurity(results, true);

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(5000_00L));
        assertThat(entry.getPortfolioTransaction().getDate(), is(Dates.date("2015-01-19")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(132_80212L));
        assertThat(entry.getPortfolioTransaction().getFees(), is(0L));
    }

    @Test
    public void testWertpapierKaufIfSecurityIsPresent() throws IOException
    {
        Client client = new Client();
        Security s = new Security();
        s.setName("COMS.-MSCI WORL.T.U.ETF I");
        s.setWkn("ETF110");
        client.addSecurity(s);

        ConsorsbankPDFExctractor extractor = new ConsorsbankPDFExctractor(client)
        {
            @Override
            String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("ConsorsbankKauf.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check buy sell transaction
        Item item = results.get(0);
        BuySellEntry entry = (BuySellEntry) item.getSubject();
        assertThat(entry.getPortfolioTransaction().getAmount(), is(5000_00L));
    }

    private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
