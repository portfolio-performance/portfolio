package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class CapitalGainsCalculationTest
{

    @Test
    public void testPartialTransfersAndTrailMatches()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addTo(client);

        Portfolio portfolioA = new PortfolioBuilder(new Account("one"))
                        .inbound_delivery(security, "2021-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000))
                        .outbound_delivery(security, "2021-01-02", Values.Share.factorize(5),
                                        Values.Amount.factorize(500), 0, 0)
                        .addTo(client);

        Portfolio portfolioB = new PortfolioBuilder(new Account("two")).addTo(client);

        PortfolioTransferEntry transfer = new PortfolioTransferEntry(portfolioA, portfolioB);
        transfer.setSecurity(security);
        transfer.setDate(LocalDateTime.parse("2021-01-03T00:00"));
        transfer.setShares(Values.Share.factorize(5));
        transfer.setAmount(Values.Amount.factorize(600));
        transfer.setCurrencyCode(security.getCurrencyCode());

        transfer.insert();

        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(),
                        Interval.of(LocalDate.parse("2020-12-31"), LocalDate.parse("2021-01-31")));

        SecurityPerformanceRecord record = snapshot.getRecord(security).orElseThrow(IllegalArgumentException::new);

        Money eur100 = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100));
        assertThat(record.getCapitalGainsOnHoldings(), is(eur100));

        CapitalGainsRecord unrealizedCapitalGains = record.getUnrealizedCapitalGains();
        assertThat(unrealizedCapitalGains.getCapitalGains(), is(eur100));
        assertThat(unrealizedCapitalGains.getCapitalGainsTrail().getValue(), is(eur100));

    }
}
