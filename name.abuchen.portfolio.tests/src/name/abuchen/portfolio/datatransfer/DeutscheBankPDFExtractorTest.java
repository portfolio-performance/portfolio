package name.abuchen.portfolio.datatransfer;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

@SuppressWarnings("nls")
public class DeutscheBankPDFExtractorTest
{

    @Test
    public void testSanityCheckForBankName() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract("", "some text", errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), instanceOf(UnsupportedOperationException.class));
    }

    @Test
    public void testErtragsgutschrift() throws IOException
    {
        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(new Client());
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract("", from("DeutscheBankErtragsgutschrift.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Optional<Item> item = results.stream().filter(i -> i instanceof SecurityItem).findFirst();
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("DE000BASF111"));

        // check transaction
        item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(Dates.date("2014-12-15")));
        assertThat(transaction.getAmount(), is(1495L));
        assertThat(transaction.getShares(), is(123_00000L));
    }

    @Test
    public void testErtragsgutschriftWhenSecurityExists() throws IOException
    {
        Client client = new Client();
        Security security = new Security("BASF", "DE000BASF111", null, null);
        client.addSecurity(security);

        DeutscheBankPDFExctractor extractor = new DeutscheBankPDFExctractor(client);
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract("", from("DeutscheBankErtragsgutschrift.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        // check transaction
        AccountTransaction transaction = (AccountTransaction) results.get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
    }

    private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
