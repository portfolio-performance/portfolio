package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousTradingDay;
import name.abuchen.portfolio.util.Interval;

public class PreviousTradingDayTest
{
    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        PreviousTradingDay previousDay = new PreviousTradingDay();
        assertEquals(ReportingPeriod.from(previousDay.getCode()), previousDay);
    }

    @Test
    @SuppressWarnings("nls")
    public void testToInterval()
    {
        PreviousTradingDay period = new PreviousTradingDay();

        assertEquals(Interval.of(LocalDate.parse("2022-01-06"), LocalDate.parse("2022-01-07")),
                        period.toInterval(LocalDate.parse("2022-01-09"))); // Sunday

        assertEquals(Interval.of(LocalDate.parse("2022-01-06"), LocalDate.parse("2022-01-07")),
                        period.toInterval(LocalDate.parse("2022-01-10"))); // Monday

        assertEquals(Interval.of(LocalDate.parse("2022-01-09"), LocalDate.parse("2022-01-10")),
                        period.toInterval(LocalDate.parse("2022-01-11"))); // Tuesday

        assertEquals(Interval.of(LocalDate.parse("2022-01-10"), LocalDate.parse("2022-01-11")),
                        period.toInterval(LocalDate.parse("2022-01-12"))); // Wednesday

    }

}
