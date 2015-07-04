package name.abuchen.portfolio.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.Month;

import org.junit.Test;

public class DatesTest
{
    @Test
    public void testSame()
    {
        LocalDate d1 = LocalDate.of(2010, Month.JANUARY, 1);
        assertThat(Dates.daysBetween(d1, d1), is(0));
    }

    @Test
    public void testOne()
    {
        LocalDate d1 = LocalDate.of(2010, Month.JANUARY, 1);
        LocalDate d2 = LocalDate.of(2010, Month.JANUARY, 2);
        assertThat(Dates.daysBetween(d1, d2), is(1));
    }

    @Test
    public void testYearLeap()
    {
        LocalDate d1 = LocalDate.of(2009, Month.DECEMBER, 31);
        LocalDate d2 = LocalDate.of(2010, Month.JANUARY, 1);
        assertThat(Dates.daysBetween(d1, d2), is(1));
    }

    @Test
    public void testReverse()
    {
        LocalDate d1 = LocalDate.of(2009, Month.DECEMBER, 31);
        LocalDate d2 = LocalDate.of(2010, Month.JANUARY, 1);
        assertThat(Dates.daysBetween(d2, d1), is(1));
    }

    @Test
    public void testLeapYear()
    {
        LocalDate d1 = LocalDate.of(2008, Month.FEBRUARY, 28);
        LocalDate d2 = LocalDate.of(2008, Month.MARCH, 1);
        assertThat(Dates.daysBetween(d1, d2), is(2));
    }

}
