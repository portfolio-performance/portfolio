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
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastWeek;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class LastWeekTest
{
    @Test
    public void testContructor() throws IOException
    {
        String code = "C";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), LastWeek.class); // NOSONAR
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "C";
        StringBuilder strb = new StringBuilder();

        ReportingPeriod period = new ReportingPeriod.LastWeek();
        period.writeTo(strb);

        assertEquals(strb.toString(), code);
    }

    @Test
    public void testToInterval() throws IOException
    {
        Locale defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.GERMANY);

            LocalDate today = LocalDate.now();
            LocalDate lastMonday = today.minusWeeks(1).with(previousOrSame(MONDAY));
            LocalDate lastSunday = lastMonday.with(nextOrSame(SUNDAY));

            ReportingPeriod period = ReportingPeriod.from("C");

            Interval result = period.toInterval(today);

            assertEquals(result, Interval.of(lastMonday.minusDays(1), lastSunday));
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
            LocalDate lastMonday = date.minusWeeks(1).with(previousOrSame(MONDAY));
            LocalDate lastSunday = lastMonday.with(nextOrSame(SUNDAY));

            ReportingPeriod period = ReportingPeriod.from("C");

            Interval result = period.toInterval(date);

            assertEquals(result, Interval.of(lastMonday.minusDays(1), lastSunday));
            assertEquals(result.getStart(), LocalDate.of(2021, 5, 30));
            assertEquals(result.getEnd(), LocalDate.of(2021, 6, 6));
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
            LocalDate sunday = date.minusWeeks(1).with(previousOrSame(SUNDAY));
            LocalDate saturday = date.minusWeeks(1).with(nextOrSame(SATURDAY));

            ReportingPeriod period = ReportingPeriod.from("C");

            Interval result = period.toInterval(date);

            assertEquals(result, Interval.of(sunday.minusDays(1), saturday));
            assertEquals(result.getStart(), LocalDate.of(2021, 5, 29));
            assertEquals(result.getEnd(), LocalDate.of(2021, 6, 5));
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }

    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("C");
        ReportingPeriod equal2 = ReportingPeriod.from("C");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
