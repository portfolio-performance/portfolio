package name.abuchen.portfolio.datatransfer;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.pdfbox.io.IOUtils;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class IBFlexStatementExtractorWithDuplicateTransactionsTest
{
    private List<Item> runExtractor(List<Exception> errors) throws IOException
    {
        InputStream activityStatement = getClass().getResourceAsStream("IBActivityStatementWithDuplicateTransactions.xml");
        Client client = new Client();
        Extractor.InputFile tempFile = createTempFile(activityStatement);
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(client);

        return extractor.extract(Collections.singletonList(tempFile), errors);
    }

    @Test
    public void testIBAcitvityStatement() throws IOException
    {
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = runExtractor(errors);
        assertTrue(errors.isEmpty());
        int numSecurity = 0;
        int numBuySell = 0;
        int numTransactions = 1;

        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        List<Extractor.Item> securityItems = results.stream().filter(i -> i instanceof SecurityItem)
                        .collect(Collectors.toList());

        assertThat(securityItems.size(), is(numSecurity));

        List<Extractor.Item> buySellTransactions = results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList());

        assertThat(buySellTransactions.size(), is(numBuySell));

        List<Extractor.Item> accountTransactions = results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList());

        assertThat(accountTransactions.size(), is(numTransactions));

        assertThat(results.size(), is(numSecurity + numBuySell + numTransactions));

        assertFirstSecurity(results.stream().filter(i -> i instanceof SecurityItem).findFirst());
        assertFirstTransaction(results.stream().filter(i -> i instanceof TransactionItem).findFirst());
        assertSecondTransaction(results.stream().filter(i -> i instanceof TransactionItem).skip(1).findFirst());
    }

    private void assertFirstTransaction(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction transaction = (AccountTransaction) item.get().getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, 0_02L)));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.USD));
        assertTrue(!transaction.getUnit(Unit.Type.GROSS_VALUE).isPresent());
    }

    private void assertSecondTransaction(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(false));
    }

    private void assertFirstSecurity(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(false));
    }

    private Extractor.InputFile createTempFile(InputStream input) throws IOException
    {
        File tempFile = File.createTempFile("iBFlexStatementExtractorTest", null);
        FileOutputStream fos = new FileOutputStream(tempFile);

        IOUtils.copy(input, fos);
        return new Extractor.InputFile(tempFile);
    }
}
