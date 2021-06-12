package name.abuchen.portfolio.snapshot.reportingperiod;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.CurrentMonth;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class CurrentMonthTest
{
    @Test
    public void testContructor() throws IOException
    {
        String code = "M";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), CurrentMonth.class);
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "M";
        StringBuilder strb = new StringBuilder();

        ReportingPeriod period = ReportingPeriod.from(code);
        period.writeTo(strb);

        assertEquals(strb.toString(), code);
    }

    @Test
    public void testToInterval() throws IOException
    {
        LocalDate now = LocalDate.now();
        LocalDate firstOfMonth = now.with(firstDayOfMonth());
        LocalDate lastOfMonth = now.with(lastDayOfMonth());

        ReportingPeriod period = ReportingPeriod.from("M");

        Interval result = period.toInterval(now);

        assertEquals(result, Interval.of(firstOfMonth, lastOfMonth));
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("M");
        ReportingPeriod equal2 = ReportingPeriod.from("M");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
