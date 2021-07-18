package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousYear;
import name.abuchen.portfolio.util.Interval;

public class PreviousYearTest
{
    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        assertEquals(ReportingPeriod.from(new PreviousYear().getCode()), new PreviousYear());
    }

    @Test
    public void testToInterval()
    {
        ReportingPeriod period = new PreviousYear();

        LocalDate today = LocalDate.of(2021, 7, 11);

        Interval result = period.toInterval(today);

        assertEquals(result, Interval.of(LocalDate.of(2019, 12, 31), LocalDate.of(2020, 12, 31)));
    }
}
