package issues;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.util.Interval;

public class IssueCurrencyGainsRoundingError
{
    @Test
    public void testPurchaseValueOfSecurityPositionWithTransfers() throws IOException
    {
        Client client = ClientFactory.load(IssueCurrencyGainsRoundingError.class
                        .getResourceAsStream("IssueCurrencyGainsRoundingError.xml")); //$NON-NLS-1$

        Interval period = Interval.of(LocalDate.parse("2015-01-09"), //$NON-NLS-1$
                        LocalDate.parse("2016-01-09")); //$NON-NLS-1$

        CurrencyConverter converter = new TestCurrencyConverter();

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, period);

        MutableMoney currencyGains = MutableMoney.of(converter.getTermCurrency());
        currencyGains.subtract(snapshot.getValue(CategoryType.INITIAL_VALUE));
        currencyGains.subtract(snapshot.getValue(CategoryType.CAPITAL_GAINS));
        currencyGains.subtract(snapshot.getValue(CategoryType.REALIZED_CAPITAL_GAINS));
        currencyGains.subtract(snapshot.getValue(CategoryType.EARNINGS));
        currencyGains.add(snapshot.getValue(CategoryType.FEES));
        currencyGains.add(snapshot.getValue(CategoryType.TAXES));
        currencyGains.add(snapshot.getValue(CategoryType.TRANSFERS));
        currencyGains.add(snapshot.getValue(CategoryType.FINAL_VALUE));

        assertThat(snapshot.getCategoryByType(CategoryType.CURRENCY_GAINS).getValuation(), is(currencyGains.toMoney()));
    }
}
