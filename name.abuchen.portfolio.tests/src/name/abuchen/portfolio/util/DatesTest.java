package name.abuchen.portfolio.util;

import java.util.Calendar;
import java.util.Date;

import name.abuchen.portfolio.util.Dates;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

public class DatesTest
{
    @Test
    public void testToday()
    {
        Date today = Dates.today();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        assertThat(cal.get(Calendar.HOUR_OF_DAY), is(0));
        assertThat(cal.get(Calendar.HOUR_OF_DAY), is(0));
        assertThat(cal.get(Calendar.MINUTE), is(0));
        assertThat(cal.get(Calendar.SECOND), is(0));
        assertThat(cal.get(Calendar.MILLISECOND), is(0));
    }

    @Test
    public void testSame()
    {
        Date d1 = Dates.date(2010, Calendar.JANUARY, 1);
        assertThat(Dates.daysBetween(d1, d1), is(0));
    }

    @Test
    public void testOne()
    {
        Date d1 = Dates.date(2010, Calendar.JANUARY, 1);
        Date d2 = Dates.date(2010, Calendar.JANUARY, 2);
        assertThat(Dates.daysBetween(d1, d2), is(1));
    }

    @Test
    public void testYearLeap()
    {
        Date d1 = Dates.date(2009, Calendar.DECEMBER, 31);
        Date d2 = Dates.date(2010, Calendar.JANUARY, 1);
        assertThat(Dates.daysBetween(d1, d2), is(1));
    }

    @Test
    public void testReverse()
    {
        Date d1 = Dates.date(2009, Calendar.DECEMBER, 31);
        Date d2 = Dates.date(2010, Calendar.JANUARY, 1);
        assertThat(Dates.daysBetween(d2, d1), is(1));
    }

    @Test
    public void testLeapYear()
    {
        Date d1 = Dates.date(2008, Calendar.FEBRUARY, 28);
        Date d2 = Dates.date(2008, Calendar.MARCH, 1);
        assertThat(Dates.daysBetween(d1, d2), is(2));
    }

}
