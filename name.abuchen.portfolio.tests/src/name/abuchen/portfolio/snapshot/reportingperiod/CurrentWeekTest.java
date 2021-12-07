package name.abuchen.portfolio.snapshot.reportingperiod;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.time.temporal.TemporalAdjusters.previousOrSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
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

        assertEquals(period.getClass(), CurrentWeek.class); // NOSONAR
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "W";

        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getCode(), code);
    }

    @Test
    public void testToInterval() throws IOException
    {
        Locale defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.GERMANY);

            LocalDate today = LocalDate.now();
            LocalDate monday = today.with(previousOrSame(MONDAY));
            LocalDate sunday = today.with(nextOrSame(SUNDAY));

            ReportingPeriod period = ReportingPeriod.from("W");

            Interval result = period.toInterval(today);

            assertEquals(result, Interval.of(monday.minusDays(1), sunday));
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testToIntervalWithStaticDateGermany() throws IOException
    {
        Locale defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.GERMANY);

            // random, static Wednesday
            LocalDate date = LocalDate.of(2021, 6, 9);
            LocalDate monday = date.with(previousOrSame(MONDAY));
            LocalDate sunday = date.with(nextOrSame(SUNDAY));

            ReportingPeriod period = ReportingPeriod.from("W");

            Interval result = period.toInterval(date);

            assertEquals(result, Interval.of(monday.minusDays(1), sunday));
            assertEquals(result.getStart(), LocalDate.of(2021, 6, 6));
            assertEquals(result.getEnd(), LocalDate.of(2021, 6, 13));
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testToIntervalWithStaticDateUSA() throws IOException
    {
        Locale defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.US);

            // random, static Wednesday
            LocalDate date = LocalDate.of(2021, 6, 9);
            LocalDate sunday = date.with(previousOrSame(SUNDAY));
            LocalDate saturday = date.with(nextOrSame(SATURDAY));

            ReportingPeriod period = ReportingPeriod.from("W");

            Interval result = period.toInterval(date);

            assertEquals(result, Interval.of(sunday.minusDays(1), saturday));
            assertEquals(result.getStart(), LocalDate.of(2021, 6, 5));
            assertEquals(result.getEnd(), LocalDate.of(2021, 6, 12));
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }

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
