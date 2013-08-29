package name.abuchen.portfolio.util;

import java.util.Calendar;
import java.util.Date;


public class Helper
{
    public static void Assert (boolean check)
    {
        if (!check) {
            Assert (true); // for setting a breakpoint here
        }
    }

    public static long cMilliSecPerDay = 24*60*60*1000;
    
    public static Date dateCopy (Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date); 
        return cal.getTime (); // neues Datum anlegen
    }
    
    public static Date dateOffsetDays (Date date, long offset)
    {
        long time = date.getTime(); // date in ms
        long day = Math.round( (double) time / cMilliSecPerDay );  // date in Tagen
        day += offset;
        time = day*cMilliSecPerDay; // d2 in ms

        Date res = dateCopy (date);
        res.setTime(time);
        return res;
    }
    
    public static long dateDifferenceDays (Date dateFrom, Date dateTo)
    {
        long time1 = dateFrom.getTime(); // date in ms
        long day1 = Math.round( (double) time1 / cMilliSecPerDay );  // date in Tagen
        long time2 = dateTo.getTime(); // date in ms
        long day2 = Math.round( (double) time2 / cMilliSecPerDay );  // date in Tagen
        return day2 - day1;
    }
    
}
