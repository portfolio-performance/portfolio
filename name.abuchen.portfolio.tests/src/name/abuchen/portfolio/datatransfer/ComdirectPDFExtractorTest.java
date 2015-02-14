package name.abuchen.portfolio.datatransfer;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

import org.junit.Test;

@SuppressWarnings("nls")
public class ComdirectPDFExtractorTest
{

    private String gutschriftText;
    private String kaufText;
    private String gutschrift2;

    public ComdirectPDFExtractorTest()
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("Gutschrift.txt"), "UTF-8"))
        {
            gutschriftText = scanner.useDelimiter("\\A").next();
        }

        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("Wertpapierabrechnung_Kauf.txt"), "UTF-8");)
        {
            kaufText = scanner.useDelimiter("\\A").next();
        }

        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("Gutschrift2.txt"), "UTF-8");)
        {
            gutschrift2 = scanner.useDelimiter("\\A").next();
        }
    }

    @Test
    public void testGutschrift() throws IOException
    {
        Client client = new Client();
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(gutschriftText, "Gutschrift", errors);
        SecurityItem secItem = null;
        AccountTransaction accItem = null;
        for (Item it : results)
        {
            if (it instanceof SecurityItem)
            {
                secItem = (SecurityItem) it;
            }
            if (it instanceof TransactionItem)
            {
                accItem = (AccountTransaction) ((TransactionItem) it).getSubject();
            }
        }
        assertThat(secItem, is(notNullValue()));
        assertThat(accItem, is(notNullValue()));
        Security security = secItem.getSecurity();
        assertThat(security.getName(), is("Name des Wertpapiers"));
        assertThat(accItem.getAmount(), is(1_00L));
        assertThat(accItem.getSecurity(), is(security));
        assertThat(results.size(), is(2));
        // Should complete without error
        assertThat(errors, is(empty()));
    }

    @Test
    public void testGutschrift2() throws IOException
    {
        Client client = new Client();
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(gutschrift2, "Gutschrift2", errors);
        SecurityItem secItem = null;
        AccountTransaction accItem = null;
        for (Item it : results)
        {
            if (it instanceof SecurityItem)
            {
                secItem = (SecurityItem) it;
            }
            if (it instanceof TransactionItem)
            {
                accItem = (AccountTransaction) ((TransactionItem) it).getSubject();
            }
        }
        Security security = secItem.getSecurity();
        assertThat(accItem.getSecurity(), is(security));
        assertThat(accItem.getAmount(), is(1_11L));
        assertThat(security.getName(), is("Bank-Global-Rent"));
        assertThat(security.getIsin(), is("AT0000123456"));
        assertThat(results.size(), is(2));
        assertThat(errors, is(empty()));
    }

    @Test
    public void testKauf() throws IOException
    {
        Client client = new Client();
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(client);
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(kaufText, "Wertpapierabrechnung_Kauf", errors);
        SecurityItem secItem = null;
        BuySellEntryItem buyItem = null;
        for (Item it : results)
        {
            if (it instanceof SecurityItem)
            {
                secItem = (SecurityItem) it;
            }
            if (it instanceof BuySellEntryItem)
            {
                buyItem = (BuySellEntryItem) it;
            }
        }
        assertThat(secItem, is(notNullValue()));
        assertThat(buyItem, is(notNullValue()));
        Security security = secItem.getSecurity();
        assertThat(security.getName(), is("Name der Security"));
        assertThat(buyItem.getSecurity(), is(security));
        assertThat(buyItem.getAmount(), is(1_00L));
        assertThat(results.size(), is(2));
        // Should complete without error
        assertThat(errors, is(empty()));
    }

    @Test
    public void testThatExceptionIsAddedForNonComdirectDocuments() throws IOException
    {
        ComdirectPDFExtractor extractor = new ComdirectPDFExtractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract("some document text", "otherfile", errors);

        assertThat(results.isEmpty(), is(true));
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString("otherfile"));
    }
}
