package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.ReportingPeriod.FromXtoY;
import name.abuchen.portfolio.util.Interval;

public class FromXtoYTest
{
    @Before
    public void setup() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.US);

        Field field = ReportingPeriod.class.getDeclaredField("DATE_FORMATTER");
        field.setAccessible(true);
        field.set(null, formatter);
    }

    @Test
    public void testContructor() throws IOException
    {
        String code = "F2020-04-04_2020-04-08";
        ReportingPeriod period = ReportingPeriod.from(code);

        assertEquals(period.getClass(), FromXtoY.class);
    }

    @Test
    public void testWriteTo() throws IOException
    {
        String code = "F2020-04-04_2020-04-08";
        StringBuilder strb = new StringBuilder();

        ReportingPeriod period = ReportingPeriod.from(code);
        period.writeTo(strb);

        assertEquals(strb.toString(), code);
    }

    @Test
    public void testToInterval() throws IOException
    {
        LocalDate intervalStart = LocalDate.of(2020, 4, 4);
        LocalDate intervalEnd = LocalDate.of(2020, 4, 8);
        ReportingPeriod period = ReportingPeriod.from("F2020-04-04_2020-04-08");

        Interval result = period.toInterval(intervalEnd);

        assertEquals(result, Interval.of(intervalStart, intervalEnd));
    }

    @Test
    public void testEquals() throws IOException
    {
        ReportingPeriod equal1 = ReportingPeriod.from("F2020-04-04_2020-04-08");
        ReportingPeriod equal2 = ReportingPeriod.from("F2020-04-04_2020-04-08");
        ReportingPeriod notEqualSameClass = ReportingPeriod.from("F2020-04-04_2020-04-09");
        ReportingPeriod notEqualDifferentClass = ReportingPeriod.from("T10");

        assertNotEquals(equal1, null);
        assertNotEquals(equal1, notEqualSameClass);
        assertNotEquals(equal1, notEqualDifferentClass);

        assertEquals(equal1, equal1);
        assertEquals(equal1, equal2);
    }
}
