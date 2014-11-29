package name.abuchen.portfolio.util;

import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Days;

public class Dates
{
    public static final long DAY_IN_MS = 24 * 60 * 60 * 1000;

    public static Date today()
    {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    public static Calendar cal(int year, int month, int day)
    {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal;
    }

    public static Date date(int year, int month, int day)
    {
        return cal(year, month, day).getTime();
    }

    public static Date date(String date)
    {
        return new DateMidnight(date).toDate();
    }

    public static int daysBetween(Date start, Date end)
    {
        if (start.after(end))
        {
            Date temp = start;
            start = end;
            end = temp;
        }

        return Days.daysBetween(new DateTime(start), new DateTime(end)).getDays();
    }

    public static int monthsBetween(Date start, Date end)
    {
        int answer = 0;

        if (start.after(end))
        {
            Date temp = start;
            start = end;
            end = temp;
        }

        Calendar c1 = Calendar.getInstance();
        c1.setTime(start);

        Calendar c2 = Calendar.getInstance();
        c2.setTime(end);

        answer += (c2.get(Calendar.YEAR) - c1.get(Calendar.YEAR)) * 12;
        answer += c2.get(Calendar.MONTH) - c1.get(Calendar.MONTH);
        answer += c2.get(Calendar.DAY_OF_MONTH) >= c1.get(Calendar.DAY_OF_MONTH) ? 0 : -1;

        return answer;
    }

    public static Date addDays(Date date, int offset)
    {
        return new DateTime(date).plusDays(offset).toDate();
    }
}
