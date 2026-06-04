package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

public class Issue1498FifoCrossPortfolioTest
{
    @Test
    public void testPurchaseValueOfSecurityPositionWithTransfers() throws IOException
    {
        Client client = ClientFactory
                        .load(Issue1498FifoCrossPortfolioTest.class.getResourceAsStream("Issue1498FifoCrossPortfolio.xml")); //$NON-NLS-1$

        Security lufthansa = client.getSecurities().get(0);
        assertThat(lufthansa.getName(), is("Deutsche Lufthansa AG")); //$NON-NLS-1$
        assertThat(client.getPortfolios().size(), is(4));
        assertThat(client.getAccounts().size(), is(4));

        Interval period = Interval.of(LocalDate.parse("2018-12-31"), //$NON-NLS-1$
                        LocalDate.parse("2019-12-31")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();

        // fifo cost must be 1150 EUR of the purchase on portfolio 1 as trade on
        // portfolio 2 has been closed

        LazySecurityPerformanceSnapshot securitySnapshot = LazySecurityPerformanceSnapshot.create(client, converter,
                        period);
        LazySecurityPerformanceRecord record = securitySnapshot.getRecords().get(0);
        assertThat(record.getSecurity(), is(lufthansa));
        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1150))));

        // pinned values previously verified via SecurityPerformanceSnapshotComparator
        assertThat(record.getSharesHeld(), is(6000000000L));
        assertThat(record.getMarketValue(), is(Money.of("EUR", 60000L))); //$NON-NLS-1$
        assertThat(record.getQuote(), is(Quote.of("EUR", 1000000000L))); //$NON-NLS-1$
        assertThat(record.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED), is(Money.of("EUR", 99345L))); //$NON-NLS-1$
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of("EUR", 1916666667L))); //$NON-NLS-1$
        assertThat(record.getFees(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getTaxes(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getDelta(), is(Money.of("EUR", -53660L))); //$NON-NLS-1$
        assertThat(record.getDeltaPercent(), closeTo(-0.40510342744979616, 0.0001));
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.FIFO), is(Money.of("EUR", -55000L))); //$NON-NLS-1$
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.MOVING_AVERAGE), is(Money.of("EUR", -39345L))); //$NON-NLS-1$
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.FIFO),
                        closeTo(-0.4782608695652174, 0.0001));
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.MOVING_AVERAGE),
                        closeTo(-0.396044088781519, 0.0001));
        assertThat(record.getIrr(), closeTo(-0.507177136905005, 0.0001));
        assertThat(record.getTrueTimeWeightedRateOfReturn(), closeTo(-0.6300729995540222, 0.0001));
        assertThat(record.getTrueTimeWeightedRateOfReturnAnnualized(), closeTo(-0.6300729995540222, 0.0001));
        assertThat(record.getDrawdown().getMaxDrawdown(), closeTo(0.45454545454545464, 0.0001));
        assertThat(record.getDrawdown().getMaxDrawdownDuration().getDays(), is(350L));
        assertThat(record.getVolatility().getStandardDeviation(), closeTo(0.6072558271804332, 0.0001));
        assertThat(record.getVolatility().getSemiDeviation(), closeTo(0.6050967181252082, 0.0001));
        assertThat(record.getSumOfDividends(), is(Money.of("EUR", 0L))); //$NON-NLS-1$
        assertThat(record.getDividendEventCount(), is(0));
        assertThat(record.getLastDividendPayment(), is((LocalDate) null));
        assertThat(record.getPeriodicity(), is(BaseSecurityPerformanceRecord.Periodicity.NONE));
        assertThat(record.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains(),
                        is(Money.of("EUR", 1340L))); //$NON-NLS-1$
        assertThat(record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of("EUR", -14315L))); //$NON-NLS-1$
        assertThat(record.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains(),
                        is(Money.of("EUR", -55000L))); //$NON-NLS-1$
        assertThat(record.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of("EUR", -39345L))); //$NON-NLS-1$

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, period);

        // losses:
        // purchase #1 10 shares à 15 -> 50 EUR loss
        // purchase #2 50 shares à 20 -> 500 EUR loss

        assertThat(snapshot.getValue(CategoryType.CAPITAL_GAINS),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-550))));

        // realized
        // purchase 20 à 8,73 - sale 20 à 9,40 = 13,40 EUR realized profit

        assertThat(snapshot.getValue(CategoryType.REALIZED_CAPITAL_GAINS),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.4))));
    }
}
