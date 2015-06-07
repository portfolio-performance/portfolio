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
public class DABPDFExtractorTest
{

    private Security getSecurity(List<Item> results)
    {
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        return ((SecurityItem) item.get()).getSecurity();
    }

    @Test
    public void testWertpapierKauf() throws IOException
    {
        DABPDFExctractor extractor = new DABPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("DABKauf.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("LU0360863863"));
        assertThat(security.getName(), is("ARERO - Der Weltfonds Inhaber-Anteile o.N."));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(150.00)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(Dates.date("2015-01-06")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.91920)));
        assertThat(entry.getPortfolioTransaction().getFees(), is(0L));
    }

    @Test
    public void testWertpapierKauf2() throws IOException
    {
        DABPDFExctractor extractor = new DABPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("DABKauf2.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("LU0274208692"));
        assertThat(security.getName(), is("db x-tr.MSCI World Index ETF Inhaber-Anteile 1C o.N."));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(60)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(Dates.date("2015-05-04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1.42270)));
        assertThat(entry.getPortfolioTransaction().getFees(), is(4_95L));
    }

    @Test
    public void testDividend() throws IOException
    {
        DABPDFExctractor extractor = new DABPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Arrays.asList(new File("DABDividend.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = getSecurity(results);
        assertThat(security.getIsin(), is("DE0005660104"));
        assertThat(security.getName(), is("EUWAX AG Inhaber-Aktien o.N."));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getAmount(), is(Values.Amount.factorize(326)));
        assertThat(transaction.getDate(), is(Dates.date("2014-07-02")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
    }

    private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
