package name.abuchen.portfolio.snapshot.reportingperiod;

import static name.abuchen.portfolio.snapshot.ReportingPeriodType.PREVIOUS_X_YEARS_Y_MONTHS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousXYearsYMonths;
import name.abuchen.portfolio.snapshot.ReportingPeriod.PreviousXTradingDays;
import name.abuchen.portfolio.util.Interval;

public class PreviousXYearsYMonthsTest
{
    @Test
    public void testLegacyContructor() throws IOException
    {
        String code = "L2Y6";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period, new PreviousXYearsYMonths(2, 6)); // NOSONAR
    }

    @Test
    public void testSerializationDeserializationRoundtrip() throws IOException
    {
        ReportingPeriod period = new ReportingPeriod.PreviousXYearsYMonths(2, 6);

        StringBuilder strb = new StringBuilder();
        period.writeTo(strb);
        String serialized = strb.toString();
        assertTrue(serialized.contains(PREVIOUS_X_YEARS_Y_MONTHS.name()));

        ReportingPeriod deserialized = ReportingPeriod.from(serialized);
        assertEquals(deserialized, period);
    }

    @Test
    public void testToInterval()
    {
        ReportingPeriod period = new PreviousXYearsYMonths(1, 1);
        
        LocalDate intervalStart = LocalDate.of(2019, 03, 04);
        LocalDate intervalEnd = LocalDate.of(2020, 04, 04);

        Interval result = period.toInterval(intervalEnd);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals()
    {
        ReportingPeriod equal1 = new PreviousXYearsYMonths(2, 0);
        ReportingPeriod equal2 = new PreviousXYearsYMonths(2, 0);
        ReportingPeriod notEqualSameClass = new PreviousXYearsYMonths(2, 1);
        ReportingPeriod notEqualDifferentClass = new PreviousXTradingDays(10);

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualSameClass);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }

    @Test
    public void testBackwardsCompability() throws IOException
    {
        String code = "2Y";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period, new PreviousXYearsYMonths(2, 0));
    }

}
