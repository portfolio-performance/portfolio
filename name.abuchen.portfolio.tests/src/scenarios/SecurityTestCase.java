package scenarios;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.hamcrest.number.OrderingComparison.lessThan;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
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
        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, converter, period);

        SecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getSecurity().getName(), is("Basf SE"));
        assertThat(record.getTrueTimeWeightedRateOfReturn(), closeTo(-0.0594, 0.0001));
        assertThat(record.getIrr(), closeTo(-0.0643, 0.0001));

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
