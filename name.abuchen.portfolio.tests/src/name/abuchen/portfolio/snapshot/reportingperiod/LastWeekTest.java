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
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastWeek;
import name.abuchen.portfolio.snapshot.ReportingPeriod.LastXTradingDays;
import name.abuchen.portfolio.snapshot.ReportingPeriodType;
import name.abuchen.portfolio.util.Interval;

public class LastWeekTest
{
    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        ReportingPeriod period = new LastWeek();

        StringBuilder strb = new StringBuilder();
        period.writeTo(strb);
        String serialized = strb.toString();
        assertTrue(serialized.contains(ReportingPeriodType.PREVIOUS_WEEK.name())); //NOSONAR

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
            
            ReportingPeriod period = new LastWeek();

            LocalDate today = LocalDate.now();
            LocalDate lastMonday = today.minusWeeks(1).with(previousOrSame(MONDAY));
            LocalDate lastSunday = lastMonday.with(nextOrSame(SUNDAY));

            Interval result = period.toInterval(today);

            assertEquals(result, Interval.of(lastMonday.minusDays(1), lastSunday));
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

            ReportingPeriod period = new LastWeek();
            
            // random, static Wednesday
            LocalDate date = LocalDate.of(2021, 6, 9);
            LocalDate lastMonday = date.minusWeeks(1).with(previousOrSame(MONDAY));
            LocalDate lastSunday = lastMonday.with(nextOrSame(SUNDAY));

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
    public void testToIntervalWithStaticDateUSA()
    {
        Locale defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.US);

            ReportingPeriod period = new LastWeek();
            
            // random, static Wednesday
            LocalDate date = LocalDate.of(2021, 6, 9);
            LocalDate sunday = date.minusWeeks(1).with(previousOrSame(SUNDAY));
            LocalDate saturday = date.minusWeeks(1).with(nextOrSame(SATURDAY));

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
    public void testEquals()
    {
        ReportingPeriod equal1 = new LastWeek();
        ReportingPeriod equal2 = new LastWeek();
        ReportingPeriod notEqualDifferentClass = new LastXTradingDays(10);

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
