package name.abuchen.portfolio.datatransfer.ibflex;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

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

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class IBFlexStatementExtractorWithFixGrossValueBuySellTest
{
    @Test
    public void testIBAcitvityStatement() throws IOException
    {
        InputStream activityStatement = getClass().getResourceAsStream("IBActivityStatementWithFixGrossValueBuySell.xml");
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

        assertThat(securityItems.size(), is(1));

        // 1 Trade item and one corporate transaction
        assertThat(buySellTransactions.size(), is(1));
        assertThat(results.size(), is(2));

        assertFirstSecurity(results.stream().filter(i -> i instanceof SecurityItem).findFirst());
        assertFirstTransaction(results.stream().filter(i -> i instanceof BuySellEntryItem).findFirst());
    }

    private void assertFirstSecurity(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        Security security = ((SecurityItem) item.get()).getSecurity();
        assertThat(security.getIsin(), is("US0250727031"));
        assertThat(security.getWkn(), is("385086964"));
        assertThat(security.getName(), is("AVANTIS INTERNATIONAL EQUITY"));
        assertThat(security.getTickerSymbol(), is("AVDE"));
        assertThat(security.getCurrencyCode(), is("USD"));
    }

    private void assertFirstTransaction(Optional<Item> item)
    {
        assertThat(item.isPresent(), is(true));
        assertThat(item.get().getSubject(), instanceOf(BuySellEntry.class));
        BuySellEntry entry = (BuySellEntry) item.get().getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(), is(Money.of("CHF", 775_79L)));
        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-01-25T10:59")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14)));
        assertNull(entry.getSource());
        assertThat(entry.getNote(), is("AVANTIS INTERNATIONAL EQUITY"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(775.79))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(775.49))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.30))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(844.95))));
    }

    private Extractor.InputFile createTempFile(InputStream input) throws IOException
    {
        File tempFile = File.createTempFile("iBFlexStatementExtractorTest", null);
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);

        IOUtils.copy(input, fos);
        return new Extractor.InputFile(tempFile);
    }
}
