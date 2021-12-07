package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceIndicator;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

public class Issue1909FIFOCalculationOfSecurityPosition
{
    @Test
    public void testDefaultSnapshot() throws IOException
    {
        Client client = ClientFactory.load(Issue1909FIFOCalculationOfSecurityPosition.class
                        .getResourceAsStream("Issue1909FIFOCalculationOfSecurityPosition.xml")); //$NON-NLS-1$

        Security security = client.getSecurities().get(0);
        assertThat(security.getName(), is("ADIDAS AG NA O.N.")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();

        LocalDate date = LocalDate.parse("2020-12-31"); //$NON-NLS-1$

        SecurityPerformanceSnapshot snapshot = SecurityPerformanceSnapshot.create(client, converter,
                        Interval.of(LocalDate.MIN, date), SecurityPerformanceIndicator.Costs.class);

        SecurityPerformanceRecord record = snapshot.getRecords().get(0);

        assertThat(record.getSecurity(), is(security));

        assertThat(record.getFifoCost(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000))));
        assertThat(record.getFifoCostPerSharesHeld(), is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(200))));

        assertThat(record.getMovingAverageCost(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500))));
        assertThat(record.getMovingAverageCostPerSharesHeld(),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(150))));

        assertThat(record.getCapitalGainsOnHoldings(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(805))));

    }
}
