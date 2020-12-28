package name.abuchen.portfolio.snapshot;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class PortfolioSnapshotTest
{

    @Test
    public void testBuyAndSellLeavesNoEntryInSnapshot()
    {
        Client client = new Client();

        Security a = new SecurityBuilder() //
                        .addPrice("2010-01-01", 1000) //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(a, "2010-01-01", 1000000, 10000) //
                        .sell(a, "2010-01-02", 700000, 12000) //
                        .sell(a, "2010-01-03", 300000, 12000) //
                        .addTo(client);

        LocalDate date = LocalDate.parse("2010-01-31");
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new TestCurrencyConverter(), date);

        assertTrue(snapshot.getPositions().isEmpty());
    }

    @Test
    public void testValuationIfNoPricesExist()
    {
        Client client = new Client();
        Security security = new SecurityBuilder().addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", Values.Share.factorize(10), Values.Amount.factorize(2000)) //
                        .buy(security, "2010-01-10", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);

        LocalDate date = LocalDate.parse("2010-01-31");
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new TestCurrencyConverter(), date);

        assertThat(snapshot.getPositions(), hasSize(1));

        SecurityPosition position = snapshot.getPositions().get(0);
        assertThat(position.getPrice(),
                        is(new SecurityPrice(LocalDate.parse("2010-01-10"), Values.Quote.factorize(100d))));
    }

    @Test
    public void testValuationIfNoPricesExistWithForex()
    {
        Client client = new Client();
        Security security = new SecurityBuilder(CurrencyUnit.USD).addTo(client);
        Account account = new AccountBuilder().addTo(client);
        Portfolio portfolio = new PortfolioBuilder(account).addTo(client);

        BuySellEntry purchase = new BuySellEntry(portfolio, portfolio.getReferenceAccount());
        purchase.setType(PortfolioTransaction.Type.BUY);
        purchase.setDate(LocalDateTime.parse("2010-01-10T00:00"));
        purchase.setSecurity(security);
        purchase.setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)));
        purchase.setShares(Values.Share.factorize(10));

        purchase.getPortfolioTransaction()
                        .addUnit(new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(1214.1)),
                                        BigDecimal.valueOf(0.8236553825).setScale(10)));

        purchase.insert();

        LocalDate date = LocalDate.parse("2010-01-31");
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, new TestCurrencyConverter(), date);

        assertThat(snapshot.getPositions(), hasSize(1));

        SecurityPosition position = snapshot.getPositions().get(0);
        assertThat(position.getPrice(),
                        is(new SecurityPrice(LocalDate.parse("2010-01-10"), Values.Quote.factorize(121.41))));

    }
}
