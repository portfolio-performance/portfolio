package name.abuchen.portfolio.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
    
    @Test
    public void testTradingDaysSame()
    {
        LocalDate d1 = LocalDate.of(2020, Month.MARCH, 16);
        assertThat(Dates.tradingDaysBetween(d1, d1), is(0));
    }
    
    @Test
    public void testTradingDaysOne()
    {
        LocalDate d1 = LocalDate.of(2020, Month.MARCH, 16);
        LocalDate d2 = LocalDate.of(2020, Month.MARCH, 17);
        assertThat(Dates.tradingDaysBetween(d1, d2), is(1));
    }
    
    @Test
    public void testTradingDaysReverse()
    {
        LocalDate d1 = LocalDate.of(2020, Month.MARCH, 16);
        LocalDate d2 = LocalDate.of(2020, Month.MARCH, 17);
        assertThat(Dates.tradingDaysBetween(d2, d1), is(1));
    }
    
    @Test
    public void testTradingDaysWeek()
    {
        LocalDate d1 = LocalDate.of(2020, Month.MARCH, 15); // Sunday
        LocalDate d2 = LocalDate.of(2020, Month.MARCH, 21); // Saturday
        assertThat(Dates.tradingDaysBetween(d2, d1), is(5));
        
        LocalDate d3 = LocalDate.of(2020, Month.MARCH, 15); // Sunday
        LocalDate d4 = LocalDate.of(2020, Month.MARCH, 22); // Sunday
        assertThat(Dates.tradingDaysBetween(d3, d4), is(5));
    }
    
    @Test
    public void testTradingDaysWeekPublicHoliday()
    {
        LocalDate d1 = LocalDate.of(2020, Month.APRIL, 9); // Thursday
        LocalDate d2 = LocalDate.of(2020, Month.APRIL, 10); // Good Friday
        assertThat(Dates.tradingDaysBetween(d2, d1), is(0));
    }
}
