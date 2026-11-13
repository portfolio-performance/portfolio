package name.abuchen.portfolio.ui.views.dashboard.performance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

@SuppressWarnings("nls")
public class PerformancePerYearWidgetTest
{
    @Test
    public void testDatesAreProjectedOntoTheReferenceYear()
    {
        LocalDate[] projected = PerformancePerYearWidget
                        .project(new LocalDate[] { LocalDate.parse("2023-03-15") }, 2023);

        assertThat(projected[0], is(LocalDate.parse("2000-03-15")));
    }

    @Test
    public void testSameDayOfDifferentYearsIsProjectedOntoTheSameDate()
    {
        LocalDate[] of2019 = PerformancePerYearWidget.project(new LocalDate[] { LocalDate.parse("2019-07-01") }, 2019);
        LocalDate[] of2024 = PerformancePerYearWidget.project(new LocalDate[] { LocalDate.parse("2024-07-01") }, 2024);

        assertThat(of2019[0], is(of2024[0]));
    }

    @Test
    public void testReferenceDateKeepsSortingBeforeJanuaryFirst()
    {
        // the reporting period of a calendar year starts on December 31st of
        // the previous year

        LocalDate[] projected = PerformancePerYearWidget
                        .project(new LocalDate[] { LocalDate.parse("2022-12-31") }, 2023);

        assertThat(projected[0], is(LocalDate.parse("1999-12-31")));
    }

    @Test
    public void testLeapDayIsProjectedOntoValidDate()
    {
        // the reference year is a leap year, therefore February 29th must not
        // be adjusted to February 28th

        LocalDate[] projected = PerformancePerYearWidget
                        .project(new LocalDate[] { LocalDate.parse("2024-02-29") }, 2024);

        assertThat(projected[0], is(LocalDate.parse("2000-02-29")));
    }

    @Test
    public void testProjectedDatesOfLeapYearRemainUniqueAndAscending()
    {
        LocalDate[] projected = PerformancePerYearWidget.project(fullYear(2024), 2024);

        // December 31st 2023 plus the 366 days of 2024
        assertThat(projected.length, is(367));
        assertThat(projected[0], is(LocalDate.parse("1999-12-31")));
        assertThat(projected[projected.length - 1], is(LocalDate.parse("2000-12-31")));

        for (int ii = 1; ii < projected.length; ii++)
            assertThat(projected[ii - 1].isBefore(projected[ii]), is(true));
    }

    @Test
    public void testYearWithoutHoldingsIsDetected()
    {
        assertThat(PerformancePerYearWidget.hasNoHoldings(new long[] { 0, 0, 0 }), is(true));
        assertThat(PerformancePerYearWidget.hasNoHoldings(new long[] { 0, 0, 100_00 }), is(false));
    }

    private LocalDate[] fullYear(int year)
    {
        var start = LocalDate.of(year - 1, 12, 31);
        int size = (int) ChronoUnit.DAYS.between(start, LocalDate.of(year, 12, 31)) + 1;

        var dates = new LocalDate[size];
        for (int ii = 0; ii < size; ii++)
            dates[ii] = start.plusDays(ii);
        return dates;
    }
}
