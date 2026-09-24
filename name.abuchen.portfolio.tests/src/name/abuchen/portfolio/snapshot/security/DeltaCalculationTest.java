package name.abuchen.portfolio.snapshot.security;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DeltaCalculationTest
{
    @Test
    public void testShareDividendWithWithheldCharges()
    {
        assertShareDividendDelta(50, 100);
    }

    @Test
    public void testShareDividendWithoutCharges()
    {
        assertShareDividendDelta(0, 0);
    }

    private void assertShareDividendDelta(long fees, long taxes)
    {
        var security = new Security("Reward", "EUR");
        var portfolio = new Portfolio();
        var reward = new PortfolioTransaction(LocalDateTime.of(2024, 1, 1, 0, 0), "EUR", 243 + fees + taxes,
                        security, Values.Share.factorize(0.0243), Type.DIVIDENDS, fees, taxes);
        var sale = new PortfolioTransaction(LocalDateTime.of(2025, 1, 1, 0, 0), "EUR", 243, security,
                        reward.getShares(), Type.SELL, 0, 0);

        var delta = Calculation.perform(DeltaCalculation.class, new TestCurrencyConverter(), security,
                        List.of(CalculationLineItem.dividend(portfolio, reward), CalculationLineItem.of(portfolio, reward),
                                        CalculationLineItem.of(portfolio, sale)));

        assertEquals(Money.of("EUR", 243), delta.getDelta());
    }
}
