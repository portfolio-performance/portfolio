package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

public class Issue1498FifoCrossPortfolio
{
    @Test
    public void testPurchaseValueOfSecurityPositionWithTransfers() throws IOException
    {
        Client client = ClientFactory
                        .load(Issue1498FifoCrossPortfolio.class.getResourceAsStream("Issue1498FifoCrossPortfolio.xml")); //$NON-NLS-1$

        Security lufthansa = client.getSecurities().get(0);
        assertThat(lufthansa.getName(), is("Deutsche Lufthansa AG")); //$NON-NLS-1$
        assertThat(client.getPortfolios().size(), is(4));
        assertThat(client.getAccounts().size(), is(4));

        Interval period = Interval.of(LocalDate.parse("2018-12-31"), //$NON-NLS-1$
                        LocalDate.parse("2019-12-31")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();

        // fifo cost must be 1150 EUR of the purchase on portfolio 1 as trade on
        // portfolio 2 has been closed

        SecurityPerformanceSnapshot securitySnapshot = SecurityPerformanceSnapshot.create(client, converter, period);
        SecurityPerformanceRecord securityRecord = securitySnapshot.getRecords().get(0);
        assertThat(securityRecord.getSecurity(), is(lufthansa));
        assertThat(securityRecord.getFifoCost(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1150))));

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
