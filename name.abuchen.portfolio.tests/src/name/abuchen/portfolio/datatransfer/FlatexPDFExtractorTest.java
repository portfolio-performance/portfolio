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
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

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
        assertThat(results.size(), is(4));

        assertFirstSecurity(results.stream().filter(i -> i instanceof SecurityItem).findFirst());
        assertFirstTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst());

        assertSecondSecurity(results.stream().filter(i -> i instanceof SecurityItem)
                        .reduce((previous, current) -> current).get());
        assertSecondTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .reduce((previous, current) -> current).get());
    }

    private Security assertFirstSecurity(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0005194062"));
        assertThat(security.getWkn(), is("519406"));
        assertThat(security.getName(), is("BAYWA AG VINK.NA. O.N."));

        return security;
    }

    private void assertFirstTransaction(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(5893_10L));
        assertThat(entry.getPortfolioTransaction().getDate(), is(Dates.date("2014-01-28")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(150_000000L));
        assertThat(entry.getPortfolioTransaction().getFees(), is(5_90L));
        assertThat(entry.getPortfolioTransaction().getActualPurchasePrice(), is(39_24L));
    }

    private Security assertSecondSecurity(Item item)
    {
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("DE0008402215"));
        assertThat(security.getWkn(), is("840221"));
        assertThat(security.getName(), is("HANN.RUECK SE NA O.N."));

        return security;
    }

    private void assertSecondTransaction(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(5954_80L));
        assertThat(entry.getPortfolioTransaction().getDate(), is(Dates.date("2014-01-28")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(100_000000L));
        assertThat(entry.getPortfolioTransaction().getFees(), is(5_90L));
        assertThat(entry.getPortfolioTransaction().getActualPurchasePrice(), is(59_48L));
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

        // security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE0008402215"));
        assertThat(security.getWkn(), is("840221"));
        assertThat(security.getName(), is("HANN.RUECK SE NA O.N."));

        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(Dates.date("2014-05-08")));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(795.15)));
        assertThat(transaction.getShares(), is(Values.Share.factorize(360)));
    }

    private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
