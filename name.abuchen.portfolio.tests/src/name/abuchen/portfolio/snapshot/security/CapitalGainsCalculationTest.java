package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
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

        var interval = Interval.of(LocalDate.parse("2020-12-31"), LocalDate.parse("2021-01-31"));
        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(),
                        interval);

        new SecurityPerformanceSnapshotComparator(snapshot,
                        LazySecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(), interval))
                                        .compare();

        SecurityPerformanceRecord record = snapshot.getRecord(security).orElseThrow(IllegalArgumentException::new);

        Money eur100 = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100));
        assertThat(record.getCapitalGainsOnHoldings(), is(eur100));

        CapitalGainsRecord unrealizedCapitalGains = record.getUnrealizedCapitalGains();
        assertThat(unrealizedCapitalGains.getCapitalGains(), is(eur100));
        assertThat(unrealizedCapitalGains.getCapitalGainsTrail().getValue(), is(eur100));

    }

    @Test
    public void testFifoBuySellTransactions()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addPrice("2013-03-01", Values.Quote.factorize(100)) //
                        .addTo(client);

        new PortfolioBuilder() //
                        .buy(security, "2010-01-01", Values.Share.factorize(109), Values.Amount.factorize(3149.20)) //
                        .sell(security, "2010-02-01", Values.Share.factorize(15), Values.Amount.factorize(531.50)) //
                        .buy(security, "2010-03-01", Values.Share.factorize(52), Values.Amount.factorize(1684.92)) //
                        .buy(security, "2010-03-01", Values.Share.factorize(32), Values.Amount.factorize(959.30)) //
                        .addTo(client);

        var interval = Interval.of(LocalDate.parse("2009-12-31"), LocalDate.parse("2021-01-31"));
        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(),
                        interval);
        SecurityPerformanceRecord record = snapshot.getRecord(security).orElseThrow(IllegalArgumentException::new);


        // expected:
        // 531.5 - 3149.20 * 15/109 = 98,1238532110092
        Money expectedGains = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(98.12));
        CapitalGainsRecord realizedCapitalGains = record.getRealizedCapitalGains();
        assertThat(realizedCapitalGains.getCapitalGains(), is(expectedGains));

        // same for Moving average
        CapitalGainsRecord realizedCapitalGainsMA = record.getRealizedCapitalGainsMA();
        assertThat(realizedCapitalGainsMA.getCapitalGains(), is(expectedGains));

        // 100*178- [3149.2*(109-15)/109+1684.92+959.3] = 12439,956146789
        Money expectedGains2 = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12439.96));
        CapitalGainsRecord unRealizedCapitalGains = record.getUnrealizedCapitalGains();
        assertThat(unRealizedCapitalGains.getCapitalGains(), is(expectedGains2));

        // expected moving average is identical because it is only one buy
        CapitalGainsRecord unRealizedCapitalGainsMA = record.getUnrealizedCapitalGainsMA();
        assertThat(unRealizedCapitalGainsMA.getCapitalGains(), is(expectedGains2));

    }

    @Test
    public void testFifoBuySellTransactions2()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addPrice("2013-03-01", Values.Quote.factorize(100)) //
                        .addTo(client);
        new PortfolioBuilder() //
                        .buy(security, "2010-01-01", Values.Share.factorize(109), Values.Amount.factorize(3149.20)) //
                        .buy(security, "2010-02-01", Values.Share.factorize(52), Values.Amount.factorize(1684.92)) //
                        .sell(security, "2010-03-01", Values.Share.factorize(15), Values.Amount.factorize(531.50)) //
                        .addTo(client);

        var interval = Interval.of(LocalDate.parse("2009-12-31"), LocalDate.parse("2021-01-31"));
        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, new TestCurrencyConverter(),
                        interval);
        SecurityPerformanceRecord record = snapshot.getRecord(security).orElseThrow(IllegalArgumentException::new);

        // expected for FIFO :
        // 531.5 - 3149.20 * 15/109 = 98,1238532110092
        Money expectedGains = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(98.12));
        CapitalGainsRecord realizedCapitalGains = record.getRealizedCapitalGains();
        assertThat(realizedCapitalGains.getCapitalGains(), is(expectedGains));

        // expected for for Moving average
        // 531.5 - (3149.20 + 1684.92) * 15/(109+52) = 81,116149068323
        Money expectedGainsMA = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(81.12));
        CapitalGainsRecord realizedCapitalGainsMA = record.getRealizedCapitalGainsMA();
        assertThat(realizedCapitalGainsMA.getCapitalGains(), is(expectedGainsMA));

        // expected for FIFO :
        // 146*100 - [3149,20 + 1684,92 - (3149,20 * 15/109)]=
        // 10199,256146789
        Money expectedGainsF = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10199.26));
        CapitalGainsRecord unRealizedCapitalGainsFiFO = record.getUnrealizedCapitalGains();
        assertThat(unRealizedCapitalGainsFiFO.getCapitalGains(), is(expectedGainsF));

        // expected for for Moving average
        // 146*100 - (3149.20 + 1684.92)*146/(109+52) = 10216,2638509317
        Money expectedGainsMA2 = Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10216.26));
        CapitalGainsRecord unRealizedCapitalGainsMA = record.getUnrealizedCapitalGainsMA();
        assertThat(unRealizedCapitalGainsMA.getCapitalGains(), is(expectedGainsMA2));
    }

}
