package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Arrays;

import org.junit.Test;

@SuppressWarnings("nls")
public class IntervalTest
{

    @Test
    public void testYearList()
    {
        // only August as first day is not included
        Interval interval = Interval.of(LocalDate.parse("2019-07-31"), LocalDate.parse("2019-08-31"));
        assertThat(interval.getYears().size(), is(1));
        assertThat(interval.getYears().get(0), is(Year.of(2019)));

        // full year
        interval = Interval.of(LocalDate.parse("2018-12-31"), LocalDate.parse("2019-12-31"));
        assertThat(interval.getYears().size(), is(1));

        // 2 years
        interval = Interval.of(LocalDate.parse("2017-12-31"), LocalDate.parse("2019-12-31"));
        assertThat(interval.getYears().size(), is(2));

        // 2 years
        interval = Interval.of(LocalDate.parse("2017-02-01"), LocalDate.parse("2019-02-01"));
        assertThat(interval.getYears().size(), is(3));

        // full year + 1 day
        interval = Interval.of(LocalDate.parse("2018-12-30"), LocalDate.parse("2019-12-31"));
        assertThat(interval.getYears().size(), is(2));
        assertThat(interval.getYears(), is(Arrays.asList(Year.of(2018), Year.of(2019))));
    }

    @Test
    public void testYearMonthList()
    {
        // only August as first day is not included
        Interval interval = Interval.of(LocalDate.parse("2019-07-31"), LocalDate.parse("2019-08-31"));
        assertThat(interval.getYearMonths().size(), is(1));
        assertThat(interval.getYearMonths().get(0), is(YearMonth.of(2019, 8)));

        // include July as the 31 July is included
        interval = Interval.of(LocalDate.parse("2019-07-30"), LocalDate.parse("2019-08-31"));
        assertThat(interval.getYearMonths().size(), is(2));

        // full year
        interval = Interval.of(LocalDate.parse("2018-12-31"), LocalDate.parse("2019-12-31"));
        assertThat(interval.getYearMonths().size(), is(12));

        interval = Interval.of(LocalDate.parse("2017-09-14"), LocalDate.parse("2019-09-14"));
        assertThat(interval.getYearMonths().size(), is(25));
    }

    @Test
    public void testCapEnd()
    {
        var latest = LocalDate.parse("2019-11-19");

        // end before the given date: unchanged
        var interval = Interval.of(LocalDate.parse("2018-12-31"), LocalDate.parse("2019-06-30"));
        assertThat(interval.capEnd(latest), is(interval));

        // end on the given date: unchanged
        interval = Interval.of(LocalDate.parse("2018-12-31"), latest);
        assertThat(interval.capEnd(latest), is(interval));

        // end after the given date: end is capped
        interval = Interval.of(LocalDate.parse("2018-12-31"), LocalDate.parse("2019-12-31"));
        assertThat(interval.capEnd(latest), is(Interval.of(LocalDate.parse("2018-12-31"), latest)));

        // start and end after the given date: empty interval
        interval = Interval.of(LocalDate.parse("2019-12-31"), LocalDate.parse("2020-12-31"));
        assertThat(interval.capEnd(latest), is(Interval.of(latest, latest)));
    }
}
