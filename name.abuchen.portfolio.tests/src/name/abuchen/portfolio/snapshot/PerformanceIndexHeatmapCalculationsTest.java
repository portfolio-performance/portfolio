package name.abuchen.portfolio.snapshot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class PerformanceIndexHeatmapCalculationsTest
{
    private LocalDate startDate;
    private String endDateString;
    private String startDateString;
    private LocalDate endDate;
    private Client client;

    @Before
    public void setUp()
    {
        this.client = new Client();

        this.startDateString = "2021-12-01";
        this.endDateString = "2022-01-01";
        this.startDate = LocalDate.parse(startDateString);
        this.endDate = LocalDate.parse(endDateString);

        new AccountBuilder() //
                        .deposit_(startDate.atStartOfDay(), 100).addTo(client);
    }

    private PerformanceIndex getClientIndex()
    {
        return getClientIndex(client, Interval.of(startDate, endDate));
    }

    private PerformanceIndex getClientIndex(Client client, Interval reportInterval)
    {
        List<Exception> warnings = new ArrayList<>();
        CurrencyConverter converter = new TestCurrencyConverter();

        return PerformanceIndex.forClient(client, converter, reportInterval, warnings);
    }

    @Test
    public void testFirstHoldingIntervalSkipsLeadingZeroValuation()
    {
        // the deposit from setUp() happens on 2021-12-01; reporting starts
        // earlier, so the leading data points have a zero valuation
        Interval reportInterval = Interval.of(LocalDate.parse("2021-11-15"), endDate);

        Interval firstHolding = getClientIndex(client, reportInterval).getFirstHoldingInterval();

        // half-open: the start is anchored to the day *before* the first
        // holding (2021-12-01) so that the first active day is included
        assertThat(firstHolding, is(Interval.of(LocalDate.parse("2021-11-30"), endDate)));
    }

    @Test
    public void testFirstHoldingIntervalWhenHeldFromTheStart()
    {
        // reporting starts on the deposit date -> the very first data point
        // already holds assets, so it matches the full actual interval
        PerformanceIndex index = getClientIndex();

        assertThat(index.getFirstHoldingInterval(), is(index.getActualInterval()));
    }

    @Test
    public void testFirstHoldingIntervalWhenNeverHeld()
    {
        // a client without any transaction never holds assets
        PerformanceIndex index = getClientIndex(new Client(),
                        Interval.of(LocalDate.parse("2021-11-15"), endDate));

        Interval firstHolding = index.getFirstHoldingInterval();

        // empty interval (start == end) so the heat map shows nothing instead
        // of a spurious excess return for every month
        assertThat(firstHolding.getStart(), is(firstHolding.getEnd()));
        assertThat(firstHolding.getYears().isEmpty(), is(true));
    }

    @Test
    public void testThatClientIndexPerformanceCorectWhenDataPointsForBothDatesAreAvailableNoPerformance()
    {
        Interval interval = Interval.of(startDate, endDate);

        assertThat(getClientIndex().getPerformance(interval), closeTo(0.0, 0.1e-10));
    }

    @Test
    public void testThatClientIndexPerformanceZeroWhenDataPointsForNoDatesAreAvailable()
    {
        new AccountBuilder() //
                        .interest(startDate.plusDays(1).atStartOfDay(), 1).addTo(client);

        Interval interval = Interval.of(startDate.minusDays(1), endDate.plusDays(1));

        assertThat(getClientIndex().getPerformance(interval), closeTo(0.01, 0.1e-10));
    }

    @Test
    public void testThatClientIndexPerformance_WhenIntervalDoesNotIntersect() // NOSONAR
    {
        new AccountBuilder() //
                        .interest(startDate.plusDays(1).atStartOfDay(), 1).addTo(client);

        Interval interval = Interval.of(startDate.minusDays(5), startDate.minusDays(1));
        assertThat(getClientIndex().getPerformance(interval), closeTo(0.0, 0.1e-10));

        interval = Interval.of(endDate.plusDays(1), endDate.plusDays(5));
        assertThat(getClientIndex().getPerformance(interval), closeTo(0.0, 0.1e-10));
    }

    @Test
    public void testThatClientPerformanceIsCalculatedCorrectlyWithInterest()
    {
        new AccountBuilder() //
                        .interest(startDate.plusDays(1).atStartOfDay(), 1).addTo(client);

        Interval interval = Interval.of(startDate, endDate);

        assertThat(getClientIndex().getPerformance(interval), closeTo(0.01, 0.1e-10));
    }

    @Test
    public void testThatSecurityPerformanceCorrectWhenDataPointsForBothDatesAreAvailableNoPerformance()
    {
        Interval interval = Interval.of(startDate, endDate);

        Security security = new SecurityBuilder().addPrice(startDateString, 100).addTo(client);

        PerformanceIndex securityIndex = PerformanceIndex.forSecurity(getClientIndex(), security);

        assertThat(securityIndex.getPerformance(interval), closeTo(0.00, 0.1e-10));
    }

    @Test
    public void testThatSecurityPerformanceCorrectWhenDataPointsForBothDatesAreAvailableSomePerformance()
    {
        Interval interval = Interval.of(startDate, endDate);

        Security security = new SecurityBuilder().addPrice(startDateString, 100).addPrice(endDateString, 101)
                        .addTo(client);

        PerformanceIndex securityIndex = PerformanceIndex.forSecurity(getClientIndex(), security);

        assertThat(securityIndex.getPerformance(interval), closeTo(0.01, 0.1e-10));
    }

    @Test
    public void testThatSecurityPerformanceCorrectWhenLastDataPointIsNotAvailable()
    {
        Security security = new SecurityBuilder().addPrice(startDateString, 100).addPrice(endDateString, 101)
                        .addTo(client);

        PerformanceIndex securityIndex = PerformanceIndex.forSecurity(getClientIndex(), security);

        Interval interval = Interval.of(startDate, endDate.plusDays(1));

        assertThat(securityIndex.getPerformance(interval), closeTo(0.01, 0.1e-10));
    }

    @Test
    public void testThatSecurityPerformanceCorrectWhenFirstDataPointIsNotAvailable()
    {
        Security security = new SecurityBuilder().addPrice(startDateString, 100).addPrice(endDateString, 101)
                        .addTo(client);

        PerformanceIndex securityIndex = PerformanceIndex.forSecurity(getClientIndex(), security);

        Interval interval = Interval.of(startDate.minusDays(1), endDate);

        assertThat(securityIndex.getPerformance(interval), closeTo(0.01, 0.1e-10));
    }

    @Test
    public void testThatSecurityPerformanceCorrectWhenWhenIntervalIntersects()
    {
        Security security = new SecurityBuilder().addPrice(startDateString, 100).addPrice(endDateString, 101)
                        .addTo(client);

        PerformanceIndex securityIndex = PerformanceIndex.forSecurity(getClientIndex(), security);

        // intersects -> performance of full interval
        Interval interval = Interval.of(startDate.minusDays(1), endDate.plusDays(1));
        assertThat(securityIndex.getPerformance(interval), closeTo(0.01, 0.1e-10));

        // is before -> zero performance
        interval = Interval.of(startDate.minusDays(5), startDate.minusDays(1));
        assertThat(getClientIndex().getPerformance(interval), closeTo(0.0, 0.1e-10));

        // is after -> zero performance
        interval = Interval.of(endDate.plusDays(1), endDate.plusDays(5));
        assertThat(getClientIndex().getPerformance(interval), closeTo(0.0, 0.1e-10));

    }
}
