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
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class IBFlexStatementExtractorTest
{
    @Test
    public void testIBAcitvityStatement() throws IOException
    {
        InputStream activityStatement = getClass().getResourceAsStream("IBActivityStatement.xml");
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
        List<Item> buySellTransactions = results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList());

        assertThat(securityItems.size(), is(5));

        // 14 Trade item and one corporate transaction
        assertThat(buySellTransactions.size(), is(15));
        assertThat(results.size(), is(31));

        assertFirstSecurity(results.stream().filter(i -> i instanceof SecurityItem).findFirst());
        assertFirstTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst());

        assertSecondSecurity(results.stream().filter(i -> i instanceof SecurityItem)
                        .reduce((previous, current) -> current).get());
        assertFourthTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem).skip(3).findFirst());

        assertOptionBuyTransaction(buySellTransactions.get(13));

        assertInterestCharge(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction
                                        && ((AccountTransaction) i.getSubject()).getType() == Type.INTEREST_CHARGE)
                        .findFirst());
        assertTax(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction
                                        && ((AccountTransaction) i.getSubject()).getType() == Type.TAXES)
                        .findFirst());
        assertFee(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction
                                        && ((AccountTransaction) i.getSubject()).getType() == Type.FEES)
                        .skip(2).findFirst());
        assertFeeRefund(results.stream().filter(i -> i instanceof TransactionItem)
                        .filter(i -> i.getSubject() instanceof AccountTransaction
                                        && ((AccountTransaction) i.getSubject()).getType() == Type.FEES_REFUND)
                        .findFirst());
        // TODO Check CorporateActions
    }

    private void assertTax(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction entry = (AccountTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(Type.TAXES));
        assertThat(entry.getMonetaryAmount(), is(Money.of("USD", 2_07L)));
        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2017-09-15T00:00")));
    }

    private void assertFee(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction entry = (AccountTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(Type.FEES));
        assertThat(entry.getMonetaryAmount(), is(Money.of("USD", 9_18L)));
        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2017-05-03T00:00")));
    }

    private void assertFeeRefund(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction entry = (AccountTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(Type.FEES_REFUND));
        assertThat(entry.getMonetaryAmount(), is(Money.of("USD", 9_18L)));
        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2017-05-03T00:00")));
    }

    private void assertInterestCharge(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
        AccountTransaction entry = (AccountTransaction) item.get().getSubject();

        assertThat(entry.getType(), is(Type.INTEREST_CHARGE));
        assertThat(entry.getMonetaryAmount(), is(Money.of("CAD", 15_17L)));
        assertThat(entry.getDateTime(), is(LocalDateTime.parse("2013-02-05T00:00")));
    }

    private void assertFirstSecurity(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("CA38501D2041"));
        assertThat(security.getWkn(), is("80845553"));
        assertThat(security.getName(), is("GRAN COLOMBIA GOLD CORP"));
        assertThat(security.getTickerSymbol(), is("GCM.TO"));
        assertThat(security.getCurrencyCode(), is("CAD"));
    }

    private void assertSecondSecurity(Item item)
    {
        // Why is the second Security the GCM after Split ??? expected to be UUU
        Security security = ((SecurityItem) item).getSecurity();
        assertThat(security.getIsin(), is("CA38501D5010"));
        assertThat(security.getWkn(), is("129258970"));
        assertThat(security.getName(),
                        is("GCM(CA38501D2041) SPLIT 1 FOR 25 (GCM, GRAN COLOMBIA GOLD CORP, CA38501D5010)"));
        assertThat(security.getCurrencyCode(), is("CAD"));

        // setting GCM.TO as ticker symbol
        // currently fails because the exchange is empty in corporate actions.
    }

    private void assertFirstTransaction(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getSecurity().getName(), is("GRAN COLOMBIA GOLD CORP"));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of("CAD", 1356_75L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-04-01T09:34")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5000)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of("CAD", 6_75L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("CAD", Values.Quote.factorize(0.27))));

    }

    private void assertFourthTransaction(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getSecurity().getName(), is("URANIUM ONE INC."));
        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of("CAD", 232_00L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2013-01-02T15:12")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of("CAD", 1_00L)));
    }

    private void assertOptionBuyTransaction(Item item)
    {
        assertThat(item.getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getSecurity().getTickerSymbol(), is("FB180921C00200000"));
    }

    @Test
    public void testThatExceptionIsAddedForNonFlexStatementDocuments() throws IOException
    {
        InputStream otherFile = getClass().getResourceAsStream("pdf/comdirect/Dividende05.txt");
        Extractor.InputFile tempFile = createTempFile(otherFile);
        Client client = new Client();
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(client);
        List<Exception> errors = new ArrayList<>();
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
