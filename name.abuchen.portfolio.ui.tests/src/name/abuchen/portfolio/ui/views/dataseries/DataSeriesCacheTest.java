package name.abuchen.portfolio.ui.views.dataseries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.util.Interval;

public class DataSeriesCacheTest
{
    private static final Interval INTERVAL = Interval.of(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 5));

    private static long lastTotal(PerformanceIndex index)
    {
        long[] totals = index.getTotals();
        return totals[totals.length - 1];
    }

    @Test
    public void testClientFilterIsAppliedToClientDataSeries()
    {
        Client client = new Client();

        Account accountA = new AccountBuilder().deposit_("2024-01-01", Values.Amount.factorize(100)).addTo(client);
        new AccountBuilder().deposit_("2024-01-01", Values.Amount.factorize(100)).addTo(client);

        DataSeries series = new DataSeries(DataSeries.Type.CLIENT, DataSeries.ClientDataSeries.TOTALS,
                        "total", null); //$NON-NLS-1$

        DataSeriesCache cache = new DataSeriesCache(client, new ExchangeRateProviderFactory(client));

        assertThat(lastTotal(cache.lookup(series, INTERVAL)), is(Values.Amount.factorize(200)));

        cache.setClientFilter(new PortfolioClientFilter(Collections.emptyList(), List.of(accountA)));

        assertThat(lastTotal(cache.lookup(series, INTERVAL)), is(Values.Amount.factorize(100)));
    }
}
