package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class InsertActionTest
{
    private Client client;
    private BuySellEntry entry;

    private LocalDateTime transactionDate = LocalDateTime.now().withSecond(0).withNano(0);

    @Before
    public void prepare()
    {
        client = new Client();
        Security security = new Security();
        client.addSecurity(security);
        Portfolio portfolio = new Portfolio();
        client.addPortfolio(portfolio);
        Account account = new Account();
        client.addAccount(account);

        entry = new BuySellEntry();
        entry.setType(Type.BUY);
        entry.setMonetaryAmount(Money.of(CurrencyUnit.EUR, 9_99));
        entry.setShares(99);
        entry.setDate(transactionDate);
        entry.setSecurity(security);
        entry.setNote("note");
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, 1_99)));
    }

    @Test
    public void testInsertOfBuySellEntry()
    {
        Account account = client.getAccounts().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);

        InsertAction action = new InsertAction(client);
        action.process(entry, account, portfolio);

        assertThat(account.getTransactions().size(), is(1));
        assertThat(portfolio.getTransactions().size(), is(1));

        PortfolioTransaction t = portfolio.getTransactions().get(0);
        assertThat(t.getType(), is(Type.BUY));
        assertTransaction(t);
    }

    @Test
    public void testConversionOfBuySellEntry()
    {
        Account account = client.getAccounts().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);

        InsertAction action = new InsertAction(client);
        action.setConvertBuySellToDelivery(true);
        action.process(entry, account, portfolio);

        assertThat(account.getTransactions().isEmpty(), is(true));
        assertThat(portfolio.getTransactions().size(), is(1));

        PortfolioTransaction delivery = portfolio.getTransactions().get(0);
        assertThat(delivery.getType(), is(Type.DELIVERY_INBOUND));
        assertTransaction(delivery);
    }

    private void assertTransaction(PortfolioTransaction t)
    {
        assertThat(t.getSecurity(), is(client.getSecurities().get(0)));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 9_99)));
        assertThat(t.getNote(), is("note"));
        assertThat(t.getDateTime(), is(transactionDate));
        assertThat(t.getShares(), is(99L));

        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 1_99)));
        assertThat(t.getUnits().count(), is(1L));
    }

    @Test
    public void testPortfolioTransactionAttributes() throws IntrospectionException
    {
        // This test is only a "marker" that fails if the PortfolioTransaction
        // is structurally changed. If it is changed, then the ImportAction
        // needs to change too.

        BeanInfo info = Introspector.getBeanInfo(PortfolioTransaction.class);

        Set<String> properties = Arrays.stream(info.getPropertyDescriptors()).filter(p -> p.getWriteMethod() != null)
                        .map(PropertyDescriptor::getName).collect(Collectors.toSet());

        assertThat(properties, hasItem("security"));
        assertThat(properties, hasItem("monetaryAmount"));
        assertThat(properties, hasItem("currencyCode"));
        assertThat(properties, hasItem("amount"));
        assertThat(properties, hasItem("shares"));
        assertThat(properties, hasItem("dateTime"));
        assertThat(properties, hasItem("type"));
        assertThat(properties, hasItem("note"));
        assertThat(properties, hasItem("updatedAt"));

        assertThat(properties.size(), is(9));
    }
}
