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

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

@SuppressWarnings("nls")
public class CommerzbankPDFExtractorTest
{

    @Test
    public void testErtragsgutschrift() throws IOException
    {
        CommerzbankPDFExctractor extractor = new CommerzbankPDFExctractor(new Client())
        {
            @Override
            String strip(File file) throws IOException
            {
                return from(file.getName());
            }
        };
        List<Exception> errors = new ArrayList<Exception>();

        List<Item> results = extractor.extract(Arrays.asList(new File("CommerzbankErtragsgutschrift.txt")), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findAny().get().getSecurity();
        assertThat(security.getName(), is("iShs-MSCI N . America UCITS ETF"));
        assertThat(security.getIsin(), is("DE000A0J2060"));
        assertThat(security.getWkn(), is("A0J206"));

        // check transaction
        Optional<Item> item = results.stream().filter(i -> i instanceof TransactionItem).findFirst();
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getSecurity(), is(security));
        assertThat(transaction.getDate(), is(Dates.date("2015-06-22")));
        assertThat(transaction.getAmount(), is(223_45L));
        assertThat(transaction.getShares(), is(Values.Share.factorize(123)));
    }

    private String from(String resource)
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
