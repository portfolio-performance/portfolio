package scenarios;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;

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
        ReportingPeriod report = new ReportingPeriod.FromXtoY(LocalDate.parse("2014-01-31"),
                        LocalDate.parse("2014-07-31"));
        List<Exception> warnings = new ArrayList<>();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, report, warnings);

        assertThat(warnings, empty());
        assertThat(index.getVolatility().getStandardDeviation(), closeTo(0.14154409652302308, 0.1e-10)); // excel
    }

    @Test
    public void testVolatilityIfSecurityIsSoldDuringReportingPeriod() throws IOException
    {
        ReportingPeriod report = new ReportingPeriod.FromXtoY(LocalDate.parse("2014-01-31"),
                        LocalDate.parse("2015-01-31"));
        List<Exception> warnings = new ArrayList<>();

        Security basf = client.getSecurities().stream().filter(s -> "Basf SE".equals(s.getName())).findAny().get();
        PerformanceIndex index = PerformanceIndex.forInvestment(client, converter, basf, report, warnings);
        PerformanceIndex clientIndex = PerformanceIndex.forClient(client, converter, report, warnings);

        assertThat(warnings, empty());
        assertThat(index.getVolatility().getStandardDeviation(), closeTo(0.20055381503459427, 0.1e-10)); // excel
        assertThat(clientIndex.getVolatility().getStandardDeviation(), closeTo(0.20057708976532865, 0.1e-10)); // excel
        assertThat(index.getDates()[index.getDates().length - 1], is(LocalDate.parse("2015-01-31")));
    }

    @Test
    public void testVolatilityIfSecurityIsSoldAndLaterBoughtDuringReportingPeriod() throws IOException
    {
        ReportingPeriod report = new ReportingPeriod.FromXtoY(LocalDate.parse("2014-01-31"),
                        LocalDate.parse("2015-02-20"));
        List<Exception> warnings = new ArrayList<>();

        Security basf = client.getSecurities().stream().filter(s -> "Basf SE".equals(s.getName())).findAny().get();
        PerformanceIndex index = PerformanceIndex.forInvestment(client, converter, basf, report, warnings);

        assertThat(warnings, empty());
        assertThat(index.getVolatility().getStandardDeviation(), closeTo(0.20292376022238387, 0.1e-10)); // excel
        assertThat(index.getDates()[index.getDates().length - 1], is(LocalDate.parse("2015-02-20")));
    }

    @Test
    public void testVolatilityIfBenchmarkHasNoQuotes() throws IOException
    {
        ReportingPeriod report = new ReportingPeriod.FromXtoY(LocalDate.parse("2014-01-31"),
                        LocalDate.parse("2015-01-31"));
        List<Exception> warnings = new ArrayList<>();

        PerformanceIndex index = PerformanceIndex.forClient(client, converter, report, warnings);

        Security sap = client.getSecurities().stream().filter(s -> "Sap AG".equals(s.getName())).findAny().get();
        PerformanceIndex sapIndex = PerformanceIndex.forSecurity(index, sap);

        assertThat(warnings, empty());
        // quotes only until December 31st
        assertThat(sapIndex.getDates()[sapIndex.getDates().length - 1], is(LocalDate.parse("2014-12-31")));
        assertThat(sapIndex.getVolatility().getStandardDeviation(), closeTo(0.19304305063898652, 0.1e-10)); // excel
    }

}
