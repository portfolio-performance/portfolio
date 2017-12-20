package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

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
                        .buy(security, LocalDateTime.of(2010, 01, 01, 0, 0), 109 * Values.Share.factor(), 314920) //
                        .sell(security, LocalDateTime.of(2010, 02, 01, 0, 0), 15 * Values.Share.factor(), 53150) //
                        .buy(security, LocalDateTime.of(2010, 03, 01, 0, 0), 52 * Values.Share.factor(), 168492) //
                        .buy(security, LocalDateTime.of(2010, 03, 01, 0, 0), 32 * Values.Share.factor(), 95930) //
                        .addTo(client);

        CostCalculation cost = new CostCalculation();
        cost.setTermCurrency(CurrencyUnit.EUR);
        cost.visitAll(new TestCurrencyConverter(), portfolio.getTransactions());

        // expected:
        // 3149,20 - round(3149,20 * 15/109) + 1684,92 + 959,30 = 5360,04385

        assertThat(cost.getFifoCost(), is(Money.of(CurrencyUnit.EUR, 536005L)));
    }

    @Test
    public void testThatRoundingDifferencesAreRemovedIfZeroSharesHeld()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, LocalDateTime.of(2010, 01, 01, 0, 0), 10 * Values.Share.factor(), 1) //
                        .sell(security, LocalDateTime.of(2010, 02, 01, 0, 0), 3 * Values.Share.factor(), 1) //
                        .sell(security, LocalDateTime.of(2010, 03, 01, 0, 0), 3 * Values.Share.factor(), 1) //
                        .sell(security, LocalDateTime.of(2010, 03, 01, 0, 0), 4 * Values.Share.factor(), 1) //
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
                        .buy(security, LocalDateTime.of(2010, 01, 01, 0, 0), 100 * Values.Share.factor(), 314920) //
                        .sell(security, LocalDateTime.of(2010, 02, 01, 0, 0), 100 * Values.Share.factor(), 53150) //
                        .buy(security, LocalDateTime.of(2010, 03, 01, 0, 0), 50 * Values.Share.factor(), 168492) //
                        .sell(security, LocalDateTime.of(2010, 04, 01, 0, 0), 50 * Values.Share.factor(), 53150) //
                        .addTo(client);

        CostCalculation cost = new CostCalculation();
        cost.setTermCurrency(CurrencyUnit.EUR);
        cost.visitAll(new TestCurrencyConverter(), portfolio.getTransactions());

        assertThat(cost.getFifoCost(), is(Money.of(CurrencyUnit.EUR, 0L)));
    }

}
