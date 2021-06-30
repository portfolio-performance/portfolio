package name.abuchen.portfolio.snapshot.reportingperiod;

import java.io.IOException;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.ReportingPeriod;

public class ReportingYearTest
{
    @Test(expected = IOException.class)
    public void testInvalidCode() throws IOException
    {
        ReportingPeriod.from("HANS");
    }
}
