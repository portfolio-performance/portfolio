package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
    }
}
