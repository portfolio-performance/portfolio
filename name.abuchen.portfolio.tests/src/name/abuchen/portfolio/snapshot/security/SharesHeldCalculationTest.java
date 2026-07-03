package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
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
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class SharesHeldCalculationTest
{
    @Test
    public void testSharesHeldIfMultiplePortfolioContainSameSecurity()
    {
        Client client = new Client();
        Security security = new Security();
        client.addSecurity(security);

        new PortfolioBuilder() //
                        .buy(security, "2018-01-01", Values.Share.factorize(1), 100) //
                        .addTo(client);

        new PortfolioBuilder() //
                        .buy(security, "2018-01-01", Values.Share.factorize(1), 100) //
                        .addTo(client);

        var interval = Interval.of(LocalDate.parse("2018-02-01"), LocalDate.parse("2018-02-02"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);

        assertThat(snapshot.getRecords().size(), is(1));

        assertThat(snapshot.getRecords().get(0).getSharesHeld(), is(Values.Share.factorize(2)));
    }

    @Test
    public void testDistributionInboundAndOutboundAffectSharesHeld()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addTo(client);

        // 100 shares held
        Portfolio portfolio = new PortfolioBuilder() //
                        .buy(security, "2018-01-01", Values.Share.factorize(100), Values.Amount.factorize(5000)) //
                        .addTo(client);

        // spin-off target leg: 100 new shares, no basis attached here
        PortfolioTransaction inbound = new PortfolioTransaction();
        inbound.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        inbound.setDateTime(LocalDateTime.parse("2018-06-01T00:00"));
        inbound.setSecurity(security);
        inbound.setShares(Values.Share.factorize(100));
        inbound.setCurrencyCode(CurrencyUnit.EUR);
        inbound.setAmount(0);
        portfolio.addTransaction(inbound);

        // spin-off source leg: shares unchanged, only basis moves
        PortfolioTransaction outbound = new PortfolioTransaction();
        outbound.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        outbound.setDateTime(LocalDateTime.parse("2018-06-01T00:00"));
        outbound.setSecurity(security);
        outbound.setShares(0);
        outbound.setCurrencyCode(CurrencyUnit.EUR);
        outbound.setAmount(Values.Amount.factorize(1000));
        portfolio.addTransaction(outbound);

        SharesHeldCalculation calculation = new SharesHeldCalculation();
        calculation.visitAll(new TestCurrencyConverter(), portfolio.getTransactions().stream()
                        .map(t -> CalculationLineItem.of(portfolio, t)).collect(Collectors.toList()));

        // 100 (buy) + 100 (distribution inbound) + 0 (distribution outbound) = 200
        assertThat(calculation.getSharesHeld(), is(Values.Share.factorize(200)));
    }

}
