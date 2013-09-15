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

    public static double sqr(double Value)
    {
        return Value * Value;
    }

    public static class Statistics
    {
        private int n;
        private double sum1, sum2, min, max;

        public void clear()
        {
            n = 0;
            sum1 = 0;
            sum2 = 0;
            min = 0;
            max = 0;
        }

        public void add(double Value)
        {
            if (n == 0)
            {
                min = Value;
                max = Value;
            }
            else
            {
                if (Value < min)
                    min = Value;
                if (Value > max)
                    max = Value;
            }
            sum1 += Value;
            sum2 += sqr(Value);
            n++;
        }

        public double mean()
        {
            return n == 0 ? 0 : sum1 / n;
        }

        public double sDev1()
        {
            return Math.sqrt(sDev2());
        }

        public double sDev2()
        {
            return n == 0 ? 0 : sum2 / n - sqr(mean());
        }

    }
}
