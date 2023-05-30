package scenarios;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ReportingPeriod;

@SuppressWarnings("nls")
public class ClientPerformanceSnapshotTestCase
{
    private static TestCurrencyConverter converter = new TestCurrencyConverter();

    private static Client client;

    @BeforeClass
    public static void prepare() throws IOException
    {
        client = ClientFactory.load(SecurityTestCase.class.getResourceAsStream("client_performance_snapshot.xml"));
    }

    @Test
    public void testClientPerformanceSnapshot()
    {
        ReportingPeriod period = new ReportingPeriod.FromXtoY(LocalDate.parse("2015-01-02"),
                        LocalDate.parse("2015-01-14"));
        ClientPerformanceSnapshot performance = new ClientPerformanceSnapshot(client, converter,
                        period.toInterval(LocalDate.now()));

        // calculating the totals is tested with #testClientSnapshot
        assertThat(performance.getValue(CategoryType.INITIAL_VALUE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2128.58))));
        assertThat(performance.getValue(CategoryType.CAPITAL_GAINS),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.35))));
        assertThat(performance.getValue(CategoryType.REALIZED_CAPITAL_GAINS),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.69))));
        assertThat(performance.getValue(CategoryType.EARNINGS),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(28.54))));
        assertThat(performance.getValue(CategoryType.FEES),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.52))));
        assertThat(performance.getValue(CategoryType.TAXES),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(116.50))));
        assertThat(performance.getValue(CategoryType.TRANSFERS),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(787.49))));
        assertThat(performance.getValue(CategoryType.CURRENCY_GAINS),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.68))));
        assertThat(performance.getValue(CategoryType.FINAL_VALUE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2854.32))));

    }
}
