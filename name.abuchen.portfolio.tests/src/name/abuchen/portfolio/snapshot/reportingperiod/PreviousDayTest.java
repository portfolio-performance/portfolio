package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousDay;
import name.abuchen.portfolio.util.Interval;

public class PreviousDayTest
{
    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        PreviousDay previousDay = new PreviousDay();
        assertEquals(ReportingPeriod.from(previousDay.getCode()), previousDay);
    }

    @Test
    public void testToInterval()
    {
        ReportingPeriod period = new PreviousDay();

        LocalDate today = LocalDate.now();

        assertEquals(period.toInterval(today), Interval.of(today.minusDays(2), today.minusDays(1)));
    }


}
