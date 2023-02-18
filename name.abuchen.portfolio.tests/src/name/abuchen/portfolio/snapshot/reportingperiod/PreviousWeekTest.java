package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousWeek;
import name.abuchen.portfolio.util.Interval;

public class PreviousWeekTest
{
    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        assertEquals(new PreviousWeek(), ReportingPeriod.from(new PreviousWeek().getCode()));
    }

    @Test
    public void testToIntervalWithStaticDateGermany()
    {
        Locale defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.GERMANY);

            ReportingPeriod period = new PreviousWeek();

            // random, static Wednesday
            LocalDate date = LocalDate.of(2021, 6, 9);

            Interval result = period.toInterval(date);

            assertEquals(LocalDate.of(2021, 5, 30), result.getStart());
            assertEquals(LocalDate.of(2021, 6, 6), result.getEnd());
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

            ReportingPeriod period = new PreviousWeek();

            // random, static Wednesday
            LocalDate date = LocalDate.of(2021, 6, 9);

            Interval result = period.toInterval(date);

            assertEquals(LocalDate.of(2021, 5, 29), result.getStart());
            assertEquals(LocalDate.of(2021, 6, 5), result.getEnd());
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }

    }
}
