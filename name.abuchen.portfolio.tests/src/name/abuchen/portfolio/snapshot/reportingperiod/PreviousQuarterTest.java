package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousQuarter;
import name.abuchen.portfolio.util.Interval;

public class PreviousQuarterTest
{
    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        assertEquals(ReportingPeriod.from(new PreviousQuarter().getCode()), new PreviousQuarter());
    }

    @Test
    public void testToInterval()
    {
        ReportingPeriod period = new PreviousQuarter();

        LocalDate today = LocalDate.of(2021, 7, 11);

        assertEquals(period.toInterval(today), Interval.of(LocalDate.of(2021, 3, 31), LocalDate.of(2021, 6, 30)));
    }
}
