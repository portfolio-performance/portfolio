package name.abuchen.portfolio.snapshot.reportingperiod;

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.CurrentQuarter;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class CurrentQuarterTest
{
    @Test
    public void testContructor() throws IOException
    {
        String code = "Q";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(CurrentQuarter.class, period.getClass()); // NOSONAR
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "Q";

        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getCode(), code);
    }

    @Test
    public void testToIntervalForQ1() throws IOException
    {
        LocalDate dateQ1 = LocalDate.of(2021, 2, 10);

        ReportingPeriod period = ReportingPeriod.from("Q");

        Interval result = period.toInterval(dateQ1);

        LocalDate expectedStartDate = LocalDate.of(2020, 12, 31);
        LocalDate expectedEndDate = LocalDate.of(2021, 3, 1).with(lastDayOfMonth());
        assertEquals(Interval.of(expectedStartDate, expectedEndDate), result);
    }

    @Test
    public void testToIntervalForQ2() throws IOException
    {
        LocalDate dateQ2 = LocalDate.of(2021, 5, 10);

        ReportingPeriod period = ReportingPeriod.from("Q");

        Interval result = period.toInterval(dateQ2);

        LocalDate expectedStartDate = LocalDate.of(2021, 3, 31);
        LocalDate expectedEndDate = LocalDate.of(2021, 6, 1).with(lastDayOfMonth());
        assertEquals(Interval.of(expectedStartDate, expectedEndDate), result);
    }

    @Test
    public void testToIntervalForQ3() throws IOException
    {
        LocalDate dateQ3 = LocalDate.of(2021, 8, 10);

        ReportingPeriod period = ReportingPeriod.from("Q");

        Interval result = period.toInterval(dateQ3);

        LocalDate expectedStartDate = LocalDate.of(2021, 6, 30);
        LocalDate expectedEndDate = LocalDate.of(2021, 9, 1).with(lastDayOfMonth());
        assertEquals(Interval.of(expectedStartDate, expectedEndDate), result);
    }

    @Test
    public void testToIntervalForQ4() throws IOException
    {
        LocalDate dateQ4 = LocalDate.of(2021, 10, 10);

        ReportingPeriod period = ReportingPeriod.from("Q");

        Interval result = period.toInterval(dateQ4);

        LocalDate expectedStartDate = LocalDate.of(2021, 9, 30);
        LocalDate expectedEndDate = LocalDate.of(2021, 12, 1).with(lastDayOfMonth());
        assertEquals(Interval.of(expectedStartDate, expectedEndDate), result);
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("Q");
        ReportingPeriod equal2 = ReportingPeriod.from("Q");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotNull(equal1);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
