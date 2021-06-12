package name.abuchen.portfolio.snapshot.reportingperiod;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.time.temporal.TemporalAdjusters.previousOrSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.CurrentMonth;
import name.abuchen.portfolio.snapshot.ReportingPeriod.CurrentWeek;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class CurrentWeekTest
{
    @Test
    public void testContructor() throws IOException
    {
        String code = "W";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), CurrentWeek.class);
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "W";
        StringBuilder strb = new StringBuilder();

        ReportingPeriod period = ReportingPeriod.from(code);
        period.writeTo(strb);

        assertEquals(strb.toString(), code);
    }

    @Test
    public void testToInterval() throws IOException
    {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(previousOrSame(MONDAY));
        LocalDate friday = today.with(nextOrSame(FRIDAY));

        ReportingPeriod period = ReportingPeriod.from("W");

        Interval result = period.toInterval(today);

        assertEquals(result, Interval.of(monday, friday));
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("W");
        ReportingPeriod equal2 = ReportingPeriod.from("W");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
