package name.abuchen.portfolio.util;

import java.util.Date;
import org.joda.time.DateTime;
import org.joda.time.Days;

public class Helper
{
    public static void Assert(boolean check)
    {
        if (!check)
        {
            Assert(true); // for setting a breakpoint here
        }
    }

    public static Date dateAddDays(Date date, int offset)
    {
        return new DateTime(date).plusDays(offset).toDate();
    }

    public static int daysBetween(Date dateFrom, Date dateTo)
    {
        return Days.daysBetween(new DateTime(dateFrom), new DateTime(dateTo)).getDays();
    }
}
