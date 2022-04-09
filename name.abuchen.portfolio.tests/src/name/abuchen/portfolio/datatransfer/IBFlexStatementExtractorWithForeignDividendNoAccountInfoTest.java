package name.abuchen.portfolio.datatransfer;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
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
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class IBFlexStatementExtractorWithForeignDividendNoAccountInfoTest
{

    private List<Item> runExtractor(List<Exception> errors) throws IOException
    {
        Client client = new Client();
        // We add two securities to the client with EUR as currency, both will
        // receive dividends in USD.
        Security security = new Security("3M CO. already defined", CurrencyUnit.EUR);
        security.setIsin("US88579Y1010");
        client.addSecurity(security);

        security = new Security("CDW CORP/DE already defined", CurrencyUnit.EUR);
        security.setIsin("US12514G1085");
        client.addSecurity(security);

        InputStream activityStatement = getClass()
                        .getResourceAsStream("IBActivityStatementWithForeignDividendNoAccountInfo.xml");
        Extractor.InputFile tempFile = createTempFile(activityStatement);
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(client);

        return extractor.extract(Collections.singletonList(tempFile), errors);
    }

    @Test
    public void testIBAcitvityStatement() throws IOException
    {
        List<Exception> errors = new ArrayList<>();
        List<Item> results = runExtractor(errors);
        assertThat(errors.isEmpty(), is(true));
        int numSecurity = 0; // The two securities are already present in the
                             // client.
        int numBuySell = 2;
        int numTransactions = 2;

        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        List<Extractor.Item> securityItems = results.stream().filter(SecurityItem.class::isInstance)
                        .collect(Collectors.toList());

        assertThat(securityItems.size(), is(numSecurity));

        List<Extractor.Item> buySellTransactions = results.stream().filter(BuySellEntryItem.class::isInstance)
                        .collect(Collectors.toList());

        assertThat(buySellTransactions.size(), is(numBuySell));

        List<Extractor.Item> accountTransactions = results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList());

        assertThat(accountTransactions.size(), is(numTransactions));

        assertThat(results.size(), is(numSecurity + numBuySell + numTransactions));

        assertFirstBuySell(results.stream().filter(BuySellEntryItem.class::isInstance).findFirst());
        assertFirstTransaction(results.stream().filter(TransactionItem.class::isInstance).findFirst());
        assertSecondTransaction(results.stream().filter(TransactionItem.class::isInstance).skip(1).findFirst());
    }

    private void assertFirstBuySell(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.orElseThrow().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.orElseThrow().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getSecurity().getName(), is("3M CO. already defined"));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of("EUR", 1275_25L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-02-09T11:19")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 5_80L)));

        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("EUR", Values.Quote.factorize(181.35))));
    }

    private void assertFirstTransaction(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.orElseThrow().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction entry = (AccountTransaction) item.orElseThrow().getSubject();

        assertThat(entry.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(entry.getSecurity().getName(), is("3M CO. already defined"));
        assertThat(entry.getSecurity().getIsin(), is("US88579Y1010"));
        assertThat(entry.getMonetaryAmount(), is(Money.of("USD", 9_52L)));
        assertThat(entry.getCurrencyCode(), is("USD"));
        assertThat(entry.getSecurity().getCurrencyCode(), is("EUR"));
        Unit grossValue = entry.getUnit(Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(grossValue.getForex(), is(Money.of("EUR", 7_74L)));
        assertThat(grossValue.getAmount(), is(Money.of("USD", 9_52L)));
    }

    private void assertSecondTransaction(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.orElseThrow().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction entry = (AccountTransaction) item.orElseThrow().getSubject();

        assertThat(entry.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(entry.getSecurity().getName(), is("CDW CORP/DE already defined"));
        assertThat(entry.getSecurity().getIsin(), is("US12514G1085"));
        assertThat(entry.getMonetaryAmount(), is(Money.of("USD", 9_50L)));
        assertThat(entry.getCurrencyCode(), is("USD"));
        assertThat(entry.getSecurity().getCurrencyCode(), is("EUR"));
        Unit grossValue = entry.getUnit(Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(grossValue.getForex(), is(Money.of("EUR", 8_04L)));
        assertThat(grossValue.getAmount(), is(Money.of("USD", 9_50L)));
    }

    private Extractor.InputFile createTempFile(InputStream input) throws IOException
    {
        File tempFile = File.createTempFile("iBFlexStatementExtractorTest", null);
        FileOutputStream fos = new FileOutputStream(tempFile);

        IOUtils.copy(input, fos);
        return new Extractor.InputFile(tempFile);
    }
}
