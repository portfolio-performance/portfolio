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
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

@SuppressWarnings("nls")
public class sBrokerPDFExtractorTest
{
    @Test
    public void testWertpapierKauf1() throws IOException
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("sBroker_Kauf1.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE000A0H0785"));
        assertThat(security.getName(), is("iS.EO G.B.C.1.5-10.5y.U.ETF DE"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1930.17)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(Dates.date("2014-10-01")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(16)));
        assertThat(entry.getPortfolioTransaction().getFees(), is(377L));
    }

    @Test
    public void testWertpapierVerkauf1() throws IOException
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("sBroker_Verkauf1.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE000A0H0785"));
        assertThat(security.getName(), is("iS.EO G.B.C.1.5-10.5y.U.ETF DE"));

        // check buy sell transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(5648.24)));
        assertThat(entry.getPortfolioTransaction().getDate(), is(Dates.date("2015-06-04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(47)));
        assertThat(entry.getPortfolioTransaction().getFees(), is(821L));
    }
    
    @Test
    public void testErtragsgutschrift1() throws IOException
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("sBroker_Ertragsgutschrift1.txt")), errors);
        
        assertThat(errors, empty());
        assertThat(results.size(), is(3));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst().get().getSecurity();
        assertThat(security.getIsin(), is("DE000A0H0785"));
        assertThat(security.getName(), is("iS.EO G.B.C.1.5-10.5y.U.ETF DE"));

        // check buy sell transaction
        AccountTransaction t = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().get().getSubject();

        assertThat(t.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(t.getAmount(), is(Values.Amount.factorize(12.70)));
        assertThat(t.getDate(), is(Dates.date("2014-11-17")));
        assertThat(t.getShares(), is(Values.Share.factorize(16)));
    }

    private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
