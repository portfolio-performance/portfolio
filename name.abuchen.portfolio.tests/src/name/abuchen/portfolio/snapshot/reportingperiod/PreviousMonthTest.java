package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousMonth;
import name.abuchen.portfolio.util.Interval;

public class PreviousMonthTest
{
    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        assertEquals(new PreviousMonth(), ReportingPeriod.from(new PreviousMonth().getCode()));
    }

    @Test
    public void testToInterval()
    {
        ReportingPeriod period = new PreviousMonth();

        LocalDate today = LocalDate.of(2021, 7, 11);

        assertEquals(Interval.of(LocalDate.of(2021, 5, 31), LocalDate.of(2021, 6, 30)), period.toInterval(today));
    }
}
