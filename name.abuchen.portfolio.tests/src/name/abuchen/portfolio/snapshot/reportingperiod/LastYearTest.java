package name.abuchen.portfolio.snapshot.reportingperiod;

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastYear;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class LastYearTest
{
    @Test
    public void testContructor() throws IOException
    {
        String code = "G";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), LastYear.class);
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "G";
        StringBuilder strb = new StringBuilder();

        ReportingPeriod period = ReportingPeriod.from(code);
        period.writeTo(strb);

        assertEquals(strb.toString(), code);
    }

    @Test
    public void testToInterval() throws IOException
    {
        LocalDate today = LocalDate.now();
        
        LocalDate firstDayOfLastYear = today.withDayOfMonth(1).withMonth(1).minusYears(1);
        LocalDate lastDayOfLastYear = firstDayOfLastYear.withMonth(12).with(lastDayOfMonth());

        LocalDate intervalStart = firstDayOfLastYear.minusDays(1);
        LocalDate intervalEnd = lastDayOfLastYear;
        
        ReportingPeriod period = ReportingPeriod.from("G");

        Interval result = period.toInterval(today);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("G");
        ReportingPeriod equal2 = ReportingPeriod.from("G");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
