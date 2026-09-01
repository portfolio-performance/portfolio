package scenarios;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class VolatilityTestCase
{
    private static TestCurrencyConverter converter = new TestCurrencyConverter();
    private static Client client;

    @BeforeClass
    public static void loadClientFile() throws IOException
    {
        client = ClientFactory.load(SecurityTestCase.class.getResourceAsStream("volatility.xml"));
    }

    @Test
    public void testVolatilityOfSharesHeldIsIdenticalToExcel() throws IOException
    {
        Interval report = Interval.of(LocalDate.parse("2014-01-31"), LocalDate.parse("2014-07-31"));
        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, report, warnings);

        assertThat(warnings, empty());
        assertThat(index.getVolatility().getStandardDeviation(), closeTo(0.141568791460, 0.1e-10)); // excel
    }

    @Test
    public void testVolatilityIfSecurityIsSoldDuringReportingPeriod() throws IOException
    {
        Interval report = Interval.of(LocalDate.parse("2014-01-31"), LocalDate.parse("2015-01-31"));
        List<Exception> warnings = new ArrayList<>();

        Security basf = client.getSecurities().stream().filter(s -> "Basf SE".equals(s.getName())).findAny().get();
        PerformanceIndex index = PerformanceIndex.forInvestment(client, converter, basf, report, warnings);
        PerformanceIndex clientIndex = PerformanceIndex.forClient(client, converter, report, warnings);

        assertThat(warnings, empty());
        assertThat(index.getVolatility().getStandardDeviation(), closeTo(0.200573810778, 0.1e-10)); // excel
        assertThat(clientIndex.getVolatility().getStandardDeviation(), closeTo(0.200599730118, 0.1e-10)); // excel
        assertThat(index.getDates()[index.getDates().length - 1], is(LocalDate.parse("2015-01-31")));
    }

    @Test
    public void testVolatilityIfSecurityIsSoldAndLaterBoughtDuringReportingPeriod() throws IOException
    {
        Interval report = Interval.of(LocalDate.parse("2014-01-31"), LocalDate.parse("2015-02-20"));
        List<Exception> warnings = new ArrayList<>();

        Security basf = client.getSecurities().stream().filter(s -> "Basf SE".equals(s.getName())).findAny().get();
        PerformanceIndex index = PerformanceIndex.forInvestment(client, converter, basf, report, warnings);

        assertThat(warnings, empty());
        assertThat(index.getVolatility().getStandardDeviation(), closeTo(0.202942041440, 0.1e-10)); // excel
        assertThat(index.getDates()[index.getDates().length - 1], is(LocalDate.parse("2015-02-20")));
    }

    /**
     * For a reporting period of exactly one year the period-scaled standard
     * deviation and the annualized standard deviation must be identical. This
     * is the anchor of the annualization: everything else is derived from it.
     */
    @Test
    public void testAnnualizedVolatilityEqualsStandardDeviationForOneYear() throws IOException
    {
        Interval report = Interval.of(LocalDate.parse("2014-01-31"), LocalDate.parse("2015-01-31"));
        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, report, warnings);

        assertThat(warnings, empty());
        assertThat(index.getActualInterval().getDays(), is(365L));

        // a one-year reporting period contributes about as many observations
        // as a year does, so annualizing must be close to a no-op
        double periodScaled = index.getVolatility().getStandardDeviation();
        assertThat(index.getAnnualizedVolatility(), closeTo(periodScaled, periodScaled * 0.01));
    }

    /**
     * For a six month reporting period the displayed volatility is scaled to
     * those six months; annualizing it must scale it up by sqrt(365/181).
     */
    @Test
    public void testAnnualizedVolatilityScalesUpShorterPeriod() throws IOException
    {
        Interval report = Interval.of(LocalDate.parse("2014-01-31"), LocalDate.parse("2014-07-31"));
        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, report, warnings);

        assertThat(warnings, empty());
        assertThat(index.getActualInterval().getDays(), is(181L));
        assertThat(index.getVolatility().getStandardDeviation(), closeTo(0.141568791460, 0.1e-10));

        // six months of observations scaled up to a full year of them: the
        // window contributes about half of the observations a year does, so
        // the annualized figure comes out about sqrt(2) larger
        assertThat(index.getAnnualizedVolatility(), closeTo(0.201001412964, 0.1e-10));
        assertThat(index.getAnnualizedVolatility() / index.getVolatility().getStandardDeviation(),
                        closeTo(Math.sqrt(2), 0.01));
    }

    @Test
    public void testVolatilityIfBenchmarkHasNoQuotes() throws IOException
    {
        Interval report = Interval.of(LocalDate.parse("2014-01-31"), LocalDate.parse("2015-01-31"));
        List<Exception> warnings = new ArrayList<>();

        PerformanceIndex index = PerformanceIndex.forClient(client, converter, report, warnings);

        Security sap = client.getSecurities().stream().filter(s -> "Sap AG".equals(s.getName())).findAny().get();
        PerformanceIndex sapIndex = PerformanceIndex.forSecurity(index, sap);

        assertThat(warnings, empty());
        // quotes only until December 31st
        assertThat(sapIndex.getDates()[sapIndex.getDates().length - 1], is(LocalDate.parse("2014-12-31")));
        assertThat(sapIndex.getVolatility().getStandardDeviation(), closeTo(0.193062749491, 0.1e-10)); // excel
    }

}
