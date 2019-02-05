package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDate;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

public class Issue672CapitalGainsIfSecurityIsTransferred
{
    @Test
    public void testPurchaseValueOfSecurityPositionWithTransfers() throws IOException
    {
        Client client = ClientFactory.load(Issue371PurchaseValueWithTransfers.class
                        .getResourceAsStream("Issue672CapitalGainsIfSecurityIsTransferred.xml")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();
        Portfolio secondPortfolio = client.getPortfolios().get(1);
        Interval period = Interval.of(LocalDate.parse("2016-01-01"), //$NON-NLS-1$
                        LocalDate.parse("2017-01-01")); //$NON-NLS-1$

        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, converter, secondPortfolio,
                        period);

        assertThat(snapshot.getRecords().size(), is(1));

        SecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getMarketValue(), is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(971.41))));
        assertThat(record.getFifoCost(), is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(883.1))));
        assertThat(record.getCapitalGainsOnHoldings(), is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(88.31))));
        assertThat(record.getCapitalGainsOnHoldingsPercent(), is(IsCloseTo.closeTo(0.1d, 0.0000000001)));
    }
}
