package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousXTradingDays;
import name.abuchen.portfolio.snapshot.ReportingPeriod.YearX;
import name.abuchen.portfolio.snapshot.ReportingPeriodType;
import name.abuchen.portfolio.util.Interval;

public class YearXTest
{
    @Test
    public void testLegacyContructor() throws IOException
    {
        String code = "Y2019";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period, new YearX(2019)); //NOSONAR
    }
    
    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        ReportingPeriod period = new YearX(2019);

        StringBuilder strb = new StringBuilder();
        period.writeTo(strb);
        String serialized = strb.toString();
        assertTrue(serialized.contains(ReportingPeriodType.YEAR_X.name()));

        ReportingPeriod deserialized = ReportingPeriod.from(serialized);
        assertEquals(deserialized, period);
    }

    @Test
    public void testToInterval()
    {
        ReportingPeriod period = new YearX(2019);
        
        LocalDate intervalStart = LocalDate.of(2018, 12, 31);
        LocalDate intervalEnd = LocalDate.of(2019, 12, 31);

        // The input of toInterval will be ignored
        Interval result = period.toInterval(LocalDate.of(2020, 12, 31));

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals()
    {
        ReportingPeriod equal1 = new YearX(2019);
        ReportingPeriod equal2 = new YearX(2019);
        ReportingPeriod notEqualSameClass = new YearX(2020);
        ReportingPeriod notEqualDifferentClass = new PreviousXTradingDays(10);

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualSameClass);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
