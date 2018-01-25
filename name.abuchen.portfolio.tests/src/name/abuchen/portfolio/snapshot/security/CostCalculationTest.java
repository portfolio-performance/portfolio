package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

import org.junit.Test;

@SuppressWarnings("nls")
public class CostCalculationTest
{
    @Test
    public void testFifoBuySellTransactions()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", 109 * Values.Share.factor(), 314920) //
                        .sell(security, "2010-02-01", 15 * Values.Share.factor(), 53150) //
                        .buy(security, "2010-03-01", 52 * Values.Share.factor(), 168492) //
                        .buy(security, "2010-03-01", 32 * Values.Share.factor(), 95930) //
                        .addTo(client);

        CostCalculation cost = new CostCalculation();
        cost.setTermCurrency(CurrencyUnit.EUR);
        cost.visitAll(new TestCurrencyConverter(), portfolio.getTransactions());

        // expected:
        // 3149,20 - round(3149,20 * 15/109) + 1684,92 + 959,30 = 5360,04385

        assertThat(cost.getFifoCost(), is(Money.of(CurrencyUnit.EUR, 536004L)));
    }

    @Test
    public void testThatRoundingDifferencesAreRemovedIfZeroSharesHeld()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", 10 * Values.Share.factor(), 1) //
                        .sell(security, "2010-02-01", 3 * Values.Share.factor(), 1) //
                        .sell(security, "2010-03-01", 3 * Values.Share.factor(), 1) //
                        .sell(security, "2010-03-01", 4 * Values.Share.factor(), 1) //
                        .addTo(client);

        CostCalculation cost = new CostCalculation();
        cost.setTermCurrency(CurrencyUnit.EUR);
        cost.visitAll(new TestCurrencyConverter(), portfolio.getTransactions());

        assertThat(cost.getFifoCost(), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

    @Test
    public void testWhenSharesHeldGoToZero()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", 100 * Values.Share.factor(), 314920) //
                        .sell(security, "2010-02-01", 100 * Values.Share.factor(), 53150) //
                        .buy(security, "2010-03-01", 50 * Values.Share.factor(), 168492) //
                        .sell(security, "2010-04-01", 50 * Values.Share.factor(), 53150) //
                        .addTo(client);

        CostCalculation cost = new CostCalculation();
        cost.setTermCurrency(CurrencyUnit.EUR);
        cost.visitAll(new TestCurrencyConverter(), portfolio.getTransactions());

        assertThat(cost.getFifoCost(), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

}
