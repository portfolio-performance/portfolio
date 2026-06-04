package scenarios;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.hamcrest.number.OrderingComparison.lessThan;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class SecurityTestCase
{

    /**
     * Issue: If historical quotes start only after the purchase (or delivery)
     * of a security, the security is valued at 0 (no quote available) and the
     * performance would go crazy to -100% (as reported in the forum). This
     * scenario makes sure that earliest available historical quote is used.
     */
    @Test
    public void testSecurityPerformanceWithMissingHistoricalQuotes() throws IOException
    {
        Client client = ClientFactory.load(SecurityTestCase.class
                        .getResourceAsStream("security_performance_with_missing_historical_quotes.xml"));

        Security security = client.getSecurities().get(0);
        PortfolioTransaction delivery = client.getPortfolios().get(0).getTransactions().get(0);

        assertThat("delivery transaction must be before earliest historical quote",
                        delivery.getDateTime().toLocalDate(), lessThan(security.getPrices().get(0).getDate()));

        Interval period = Interval.of(LocalDate.parse("2013-12-04"), LocalDate.parse("2014-12-04"));
        TestCurrencyConverter converter = new TestCurrencyConverter();
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client, converter, period);

        LazySecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getSecurity().getName(), is("Basf SE"));
        assertThat(record.getTrueTimeWeightedRateOfReturn(), closeTo(-0.0594, 0.0001));
        assertThat(record.getIrr(), closeTo(-0.0643, 0.0001));

        // pinned values previously verified via SecurityPerformanceSnapshotComparator
        assertThat(record.getSharesHeld(), is(10000000000L));
        assertThat(record.getMarketValue(), is(Money.of("EUR", 728800L)));
        assertThat(record.getQuote(), is(Quote.of("EUR", 7288000000L)));
        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED), is(Money.of("EUR", 774800L)));
        assertThat(record.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED), is(Money.of("EUR", 774800L)));
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of("EUR", 7748000000L)));
        assertThat(record.getFees(), is(Money.of("EUR", 0L)));
        assertThat(record.getTaxes(), is(Money.of("EUR", 0L)));
        assertThat(record.getDelta(), is(Money.of("EUR", -46000L)));
        assertThat(record.getDeltaPercent(), closeTo(-0.05937016004130098, 0.0001));
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.FIFO), is(Money.of("EUR", -46000L)));
        assertThat(record.getCapitalGainsOnHoldings(CostMethod.MOVING_AVERAGE), is(Money.of("EUR", -46000L)));
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.FIFO), closeTo(-0.05937016004130102, 0.0001));
        assertThat(record.getCapitalGainsOnHoldingsPercent(CostMethod.MOVING_AVERAGE),
                        closeTo(-0.05937016004130102, 0.0001));
        assertThat(record.getTrueTimeWeightedRateOfReturnAnnualized(), closeTo(-0.05937016004130102, 0.0001));
        assertThat(record.getDrawdown().getMaxDrawdown(), closeTo(0.010454854039375412, 0.0001));
        assertThat(record.getDrawdown().getMaxDrawdownDuration().getDays(), is(336L));
        assertThat(record.getVolatility().getStandardDeviation(), closeTo(0.010509889956936377, 0.0001));
        assertThat(record.getVolatility().getSemiDeviation(), closeTo(0.01048769375484861, 0.0001));
        assertThat(record.getSumOfDividends(), is(Money.of("EUR", 0L)));
        assertThat(record.getDividendEventCount(), is(0));
        assertThat(record.getLastDividendPayment(), is((LocalDate) null));
        assertThat(record.getPeriodicity(), is(BaseSecurityPerformanceRecord.Periodicity.NONE));
        assertThat(record.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains(), is(Money.of("EUR", 0L)));
        assertThat(record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of("EUR", 0L)));
        assertThat(record.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains(), is(Money.of("EUR", -46000L)));
        assertThat(record.getUnrealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(Money.of("EUR", -46000L)));

        // actually, in this simple scenario (no cash transfers involved), the
        // ttwror is easy to calculate:

        double endvalue = BigDecimal.valueOf(delivery.getShares())
                        .multiply(BigDecimal.valueOf(
                                        security.getSecurityPrice(LocalDate.parse("2014-12-04")).getValue()), Values.MC)
                        .divide(Values.Share.getBigDecimalFactor(), Values.MC)
                        .divide(Values.Quote.getBigDecimalFactorToMoney(), Values.MC)
                        .divide(BigDecimal.valueOf(delivery.getAmount()), Values.MC).subtract(BigDecimal.ONE)
                        .doubleValue();

        assertThat(record.getTrueTimeWeightedRateOfReturn(), closeTo(endvalue, 0.0001));
    }
}
