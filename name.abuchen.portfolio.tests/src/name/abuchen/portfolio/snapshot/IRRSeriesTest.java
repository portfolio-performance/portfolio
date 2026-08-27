package name.abuchen.portfolio.snapshot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.ArrayList;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class IRRSeriesTest
{
    private static final double PRECISION = 0.00001d;

    @Test
    public void testEndpointMatchesScalarIRRWithExternalCashFlows()
    {
        Client client = new Client();
        new AccountBuilder() //
                        .deposit_("2024-01-01", Values.Amount.factorize(10000)) //
                        .interest("2024-03-01", Values.Amount.factorize(500)) //
                        .deposit_("2024-06-01", Values.Amount.factorize(2500)) //
                        .interest("2024-09-01", Values.Amount.factorize(750)) //
                        .withdraw("2024-10-01", Values.Amount.factorize(1000)) //
                        .interest("2024-12-31", Values.Amount.factorize(600)) //
                        .addTo(client);

        Interval period = Interval.of(LocalDate.of(2023, 12, 31), LocalDate.of(2024, 12, 31));
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, period, new ArrayList<>());

        double[] irr = IRRSeries.calculate(index);

        assertThat(irr.length, is(index.getDates().length));
        assertThat(irr[irr.length - 1], IsCloseTo.closeTo(index.getPerformanceIRR(), PRECISION));
    }

    @Test
    public void testEveryPointMatchesScalarIRROfTruncatedInterval()
    {
        Client client = new Client();
        new AccountBuilder() //
                        .deposit_("2024-01-01", Values.Amount.factorize(10000)) //
                        .interest("2024-02-15", Values.Amount.factorize(300)) //
                        .deposit_("2024-04-01", Values.Amount.factorize(1500)) //
                        .interest("2024-07-01", Values.Amount.factorize(400)) //
                        .withdraw("2024-09-01", Values.Amount.factorize(500)) //
                        .interest("2024-12-31", Values.Amount.factorize(500)) //
                        .addTo(client);

        LocalDate start = LocalDate.of(2023, 12, 31);
        LocalDate end = LocalDate.of(2024, 12, 31);
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, Interval.of(start, end),
                        new ArrayList<>());

        double[] irr = IRRSeries.calculate(index);
        LocalDate[] dates = index.getDates();

        for (int ii = 1; ii < dates.length; ii++)
        {
            PerformanceIndex truncated = PerformanceIndex.forClient(client, converter, Interval.of(start, dates[ii]),
                            new ArrayList<>());
            double expected = truncated.getPerformanceIRR();

            if (Double.isFinite(expected))
                assertThat("Mismatch at " + dates[ii], irr[ii], IsCloseTo.closeTo(expected, PRECISION));
            else
                assertThat("Expected a non-finite value at " + dates[ii], Double.isFinite(irr[ii]), is(false));
        }
    }

    @Test
    public void testBenchmarkEndpointMatchesScalarIRR()
    {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);

        Client client = new Client();
        new AccountBuilder().deposit_(start.atStartOfDay(), Values.Amount.factorize(10000)).addTo(client);

        Security benchmark = new SecurityBuilder() //
                        .generatePrices(Values.Quote.factorize(100), start, end) //
                        .addTo(client);

        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex clientIndex = PerformanceIndex.forClient(client, converter, Interval.of(start, end),
                        new ArrayList<>());
        PerformanceIndex benchmarkIndex = PerformanceIndex.forSecurity(clientIndex, benchmark);

        double[] irr = IRRSeries.calculate(benchmarkIndex);

        assertThat(irr.length, is(benchmarkIndex.getDates().length));
        assertThat(irr[irr.length - 1], IsCloseTo.closeTo(benchmarkIndex.getPerformanceIRR(), PRECISION));
    }
}
