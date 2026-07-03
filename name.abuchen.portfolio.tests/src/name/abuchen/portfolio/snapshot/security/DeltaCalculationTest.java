package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DeltaCalculationTest
{
    @Test
    public void testDistributionOutboundIncreasesDelta()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", Values.Share.factorize(100), Values.Amount.factorize(5000)) //
                        .addTo(client);

        // spin-off source leg: market value leaves the security, shares
        // unchanged
        PortfolioTransaction distribution = new PortfolioTransaction();
        distribution.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        distribution.setDateTime(LocalDateTime.parse("2010-06-01T00:00"));
        distribution.setSecurity(security);
        distribution.setShares(0);
        distribution.setCurrencyCode(CurrencyUnit.EUR);
        distribution.setAmount(Values.Amount.factorize(2000));
        portfolio.addTransaction(distribution);

        DeltaCalculation delta = new DeltaCalculation();
        delta.setTermCurrency(CurrencyUnit.EUR);
        delta.visitAll(new TestCurrencyConverter(), portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList()));

        // -5000 (buy) + 2000 (distribution out) = -3000
        assertThat(delta.getDelta(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-3000))));
    }

    @Test
    public void testDistributionInboundDecreasesDelta()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addTo(client);

        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2010-01-01", Values.Share.factorize(100), Values.Amount.factorize(5000)) //
                        .addTo(client);

        // spin-off target leg: market value arrives at the security, shares
        // unchanged
        PortfolioTransaction distribution = new PortfolioTransaction();
        distribution.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        distribution.setDateTime(LocalDateTime.parse("2010-06-01T00:00"));
        distribution.setSecurity(security);
        distribution.setShares(0);
        distribution.setCurrencyCode(CurrencyUnit.EUR);
        distribution.setAmount(Values.Amount.factorize(2000));
        portfolio.addTransaction(distribution);

        DeltaCalculation delta = new DeltaCalculation();
        delta.setTermCurrency(CurrencyUnit.EUR);
        delta.visitAll(new TestCurrencyConverter(), portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList()));

        // -5000 (buy) - 2000 (distribution in) = -7000
        assertThat(delta.getDelta(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-7000))));
    }
}
