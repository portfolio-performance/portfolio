package name.abuchen.portfolio.snapshot.reportingperiod;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.time.temporal.TemporalAdjusters.previousOrSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.CurrentWeek;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousXTradingDays;
import name.abuchen.portfolio.snapshot.ReportingPeriodType;
import name.abuchen.portfolio.util.Interval;

public class CurrentWeekTest
{
    @Test
    public void testLegacyContructor() throws IOException
    {
        String code = "W";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period, new CurrentWeek()); // NOSONAR
    }

    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        ReportingPeriod period = new CurrentWeek();

        StringBuilder strb = new StringBuilder();
        period.writeTo(strb);
        String serialized = strb.toString();
        assertTrue(serialized.contains(ReportingPeriodType.CURRENT_WEEK.name()));

        ReportingPeriod deserialized = ReportingPeriod.from(serialized);
        assertEquals(deserialized, period);
    }

    @Test
    public void testToInterval()
    {
        Locale defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.GERMANY);

            ReportingPeriod period = new CurrentWeek();
            
            LocalDate today = LocalDate.now();
            LocalDate monday = today.with(previousOrSame(MONDAY));
            LocalDate sunday = today.with(nextOrSame(SUNDAY));

            Interval result = period.toInterval(today);

            assertEquals(result, Interval.of(monday.minusDays(1), sunday));
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testToIntervalWithStaticDateGermany()
    {
        Locale defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.GERMANY);

            ReportingPeriod period = new CurrentWeek();
            
            // random, static Wednesday
            LocalDate date = LocalDate.of(2021, 6, 9);
            LocalDate monday = date.with(previousOrSame(MONDAY));
            LocalDate sunday = date.with(nextOrSame(SUNDAY));

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
    public void testToIntervalWithStaticDateUSA()
    {
        Locale defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.US);

            ReportingPeriod period = new CurrentWeek();
            
            // random, static Wednesday
            LocalDate date = LocalDate.of(2021, 6, 9);
            LocalDate sunday = date.with(previousOrSame(SUNDAY));
            LocalDate saturday = date.with(nextOrSame(SATURDAY));

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
    public void testEquals()
    {
        ReportingPeriod equal1 = new CurrentWeek();
        ReportingPeriod equal2 = new CurrentWeek();
        ReportingPeriod notEqualDifferentClass = new PreviousXTradingDays(10);

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
