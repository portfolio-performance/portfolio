package name.abuchen.portfolio.snapshot.reportingperiod;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;

@SuppressWarnings("nls")
public class ReportingYearTest
{
    @Test()
    public void testGetCode() throws IOException
    {
        String code = "D10";
        ReportingPeriod period = ReportingPeriod.from(code);
        String result = period.getCode();

        assertEquals(result, code);
    }

    @Test(expected = IOException.class)
    public void testInvalidCode() throws IOException
    {
        ReportingPeriod.from("HANS");
    }
}
