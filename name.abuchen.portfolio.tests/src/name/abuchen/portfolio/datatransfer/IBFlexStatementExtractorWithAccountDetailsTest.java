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
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.impl.AlphavantageQuoteFeed;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;

@SuppressWarnings("nls")
public class IBFlexStatementExtractorWithAccountDetailsTest
{

    private List<Item> runExtractor(List<Exception> errors) throws IOException
    {
        InputStream activityStatement = getClass().getResourceAsStream("IBActivityStatementWithAccountDetails.xml");
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
        int numSecurity = 8;
        int numBuySell = 9;
        int numTransactions = 4;

        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        List<Extractor.Item> securityItems = results.stream().filter(i -> i instanceof SecurityItem)
                        .collect(Collectors.toList());

        assertThat(securityItems.size(), is(numSecurity));

        assertOptionSecurity((SecurityItem) securityItems.get(2));

        List<Extractor.Item> buySellTransactions = results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList());

        assertThat(buySellTransactions.size(), is(numBuySell));
        assertOptionBuySellTransaction((BuySellEntryItem) buySellTransactions.get(2));

        List<Extractor.Item> accountTransactions = results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList());

        assertThat(accountTransactions.size(), is(numTransactions));

        assertThat(results.size(), is(numSecurity + numBuySell + numTransactions));

        assertSecurity(results.stream().filter(i -> i instanceof SecurityItem).findFirst());
        assertFirstTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst());
    }

    @Test
    public void testSymbolTranslation() throws IOException
    {
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = runExtractor(errors);
        List<Extractor.Item> securityItems = results.stream().filter(i -> i instanceof SecurityItem)
                        .collect(Collectors.toList());

        assertThat(securityItems.get(0).getSecurity().getTickerSymbol(), is("ORCL"));
        assertThat(securityItems.get(0).getSecurity().getFeed(), is(AlphavantageQuoteFeed.ID));
        assertThat(securityItems.get(3).getSecurity().getTickerSymbol(), is("PAYC181116C00120000"));
        assertThat(securityItems.get(3).getSecurity().getFeed(), is(YahooFinanceQuoteFeed.ID));
        assertThat(securityItems.get(4).getSecurity().getTickerSymbol(), is("BMW.DE"));
        assertThat(securityItems.get(5).getSecurity().getFeed(), is(AlphavantageQuoteFeed.ID));
        assertThat(securityItems.get(5).getSecurity().getTickerSymbol(), is("DBK.DE"));
        assertThat(securityItems.get(6).getSecurity().getTickerSymbol(), is("H5E.DE"));
        assertThat(securityItems.get(6).getSecurity().getFeed(), is(AlphavantageQuoteFeed.ID));
        assertThat(securityItems.get(7).getSecurity().getTickerSymbol(), is("BAS"));
        assertThat(securityItems.get(7).getSecurity().getFeed(), is(AlphavantageQuoteFeed.ID));

    }

    private void assertOptionSecurity(SecurityItem item)
    {
        assertThat(item.getSecurity().getFeed(), is(YahooFinanceQuoteFeed.ID));
        assertThat(item.getSecurity().getTickerSymbol(), is("ORCL171117C00050000"));

    }

    private void assertOptionBuySellTransaction(BuySellEntryItem item)
    {
        assertThat(item.getShares(), is((long) 100 * Values.Share.factor()));
    }
    // private void assertInterestCharge(Optional<Item> item)
    // {
    // assertThat(item.isPresent(), is(true));
    // assertThat(item.get().getSubject(),
    // instanceOf(AccountTransaction.class));
    // AccountTransaction entry = (AccountTransaction) item.get().getSubject();
    //
    // assertThat(entry.getType(), is(Type.INTEREST_CHARGE));
    // assertThat(entry.getMonetaryAmount(), is(Money.of("CAD", 15_17L)));
    // assertThat(entry.getDate(), is(LocalDate.parse("2013-02-05")));
    // }

    private void assertSecurity(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getWkn(), is("272800"));
        assertThat(security.getName(), is("ORACLE CORP"));
        assertThat(security.getTickerSymbol(), is("ORCL"));
        assertThat(security.getCurrencyCode(), is("USD"));
    }

    private void assertFirstTransaction(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getSecurity().getName(), is("ORACLE CORP"));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of("EUR", 4185_05L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-09-15T16:20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 1_67L)));
        // 100 shares at 50 USD minus 2USD transaction cost is 49.98 USD per
        // share times 0.83701 is 41.8338
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("EUR", Values.Quote.factorize(41.8338))));

    }

    @Test
    public void testThatExceptionIsAddedForNonFlexStatementDocuments() throws IOException
    {
        InputStream otherFile = getClass().getResourceAsStream("pdf/comdirect/comdirectGutschrift1.txt");
        Extractor.InputFile tempFile = createTempFile(otherFile);
        Client client = new Client();
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(client);
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        assertThat(results.isEmpty(), is(true));
        assertThat(errors.size(), is(1));
    }

    private Extractor.InputFile createTempFile(InputStream input) throws IOException
    {
        File tempFile = File.createTempFile("iBFlexStatementExtractorTest", null);
        FileOutputStream fos = new FileOutputStream(tempFile);

        IOUtils.copy(input, fos);
        return new Extractor.InputFile(tempFile);
    }
}
