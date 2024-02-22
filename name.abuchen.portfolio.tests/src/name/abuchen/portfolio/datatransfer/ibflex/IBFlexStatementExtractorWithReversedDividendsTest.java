package name.abuchen.portfolio.datatransfer.ibflex;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.pdfbox.io.IOUtils;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Client;


@SuppressWarnings("nls")
public class IBFlexStatementExtractorWithReversedDividendsTest
{
    @Test
    public void testIBAcitvityStatement() throws IOException
    {
        InputStream activityStatement = getClass().getResourceAsStream("IBActivityStatementWithReversedDividends.xml");
        Client client = new Client();
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(client);

        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<>();
        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        if (!errors.isEmpty())
            errors.forEach(Exception::printStackTrace);

        assertThat(errors.size(), is(0));

        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        List<Item> securityItems = results.stream().filter(i -> i instanceof SecurityItem).collect(Collectors.toList());
        List<Item> accountTransactions = results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList());

        // The file has three dividend transactions with separate transactions
        // for taxes. Two of the dividend transactions were reversed (the
        // corresponding tax transactions come as refunds which we expect to
        // import).
        // We expect to import one security.
        assertThat(securityItems.size(), is(1));
        // We expect to import six account transactions (see below).
        assertThat(accountTransactions.size(), is(6));

        // Expect three tax transactions.
        List<Item> taxTransactions = accountTransactions.stream()
                        .filter(i -> i.getSubject() instanceof AccountTransaction
                                        && ((AccountTransaction) i.getSubject()).getType() == Type.TAXES)
                        .collect(Collectors.toList());
        assertThat(taxTransactions.size(), is(3));

        // Expect two tax refunds for the two reversed dividend transactions.
        List<Item> taxRefundTransactions = accountTransactions.stream()
                        .filter(i -> i.getSubject() instanceof AccountTransaction
                                        && ((AccountTransaction) i.getSubject()).getType() == Type.TAX_REFUND)
                        .collect(Collectors.toList());
        assertThat(taxRefundTransactions.size(), is(2));

        // Expect only one dividend transaction to be imported as two have been
        // reversed.
        List<Item> dividendTransactions = accountTransactions.stream()
                        .filter(i -> i.getSubject() instanceof AccountTransaction
                                        && ((AccountTransaction) i.getSubject()).getType() == Type.DIVIDENDS)
                        .collect(Collectors.toList());
        assertThat(dividendTransactions.size(), is(1));
    }

    private Extractor.InputFile createTempFile(InputStream input) throws IOException
    {
        File tempFile = File.createTempFile("iBFlexStatementExtractorTest", null);
        FileOutputStream fos = new FileOutputStream(tempFile);

        IOUtils.copy(input, fos);
        return new Extractor.InputFile(tempFile);
    }
}
