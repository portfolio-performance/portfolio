package name.abuchen.portfolio.datatransfer;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.pdfbox.io.IOUtils;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class IBFlexStatementExtractorWithAccountDetailsTest
{
    @Test
    public void testIBAcitvityStatement() throws IOException
    {
        InputStream activityStatement = getClass().getResourceAsStream("IBActivityStatementWithAccountDetails.xml");
        Client client = new Client();
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(client);

        Extractor.InputFile tempFile = createTempFile(activityStatement);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(Collections.singletonList(tempFile), errors);

        results.stream().filter(i -> !(i instanceof SecurityItem))
                        .forEach(i -> assertThat(i.getAmount(), notNullValue()));

        assertThat(results.size(), is(6));

        assertSecurity(results.stream().filter(i -> i instanceof SecurityItem).findFirst());
        assertFirstTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst());
    }

//    private void assertInterestCharge(Optional<Item> item)
//    {
//        assertThat(item.isPresent(), is(true));
//        assertThat(item.get().getSubject(), instanceOf(AccountTransaction.class));
//        AccountTransaction entry = (AccountTransaction) item.get().getSubject();
//
//        assertThat(entry.getType(), is(Type.INTEREST_CHARGE));
//        assertThat(entry.getMonetaryAmount(), is(Money.of("CAD", 15_17L)));
//        assertThat(entry.getDate(), is(LocalDate.parse("2013-02-05")));
//    }

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
        assertThat(entry.getPortfolioTransaction().getShares(), is(100_000000L));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE), is(Money.of("EUR", 0_00L)));
        assertThat(entry.getPortfolioTransaction().getGrossPricePerShare(),
                        is(Quote.of("EUR", Values.Quote.factorize(41.8505))));

    }

    @Test
    public void testThatExceptionIsAddedForNonFlexStatementDocuments() throws IOException
    {
        InputStream otherFile = getClass().getResourceAsStream("pdf/comdirect/comdirectGutschrift1.txt");
        Extractor.InputFile tempFile = createTempFile(otherFile);
        Client client = new Client();
        IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(client);
        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results =  extractor.extract(Collections.singletonList(tempFile), errors);

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
