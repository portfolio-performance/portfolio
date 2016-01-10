package name.abuchen.portfolio.online.impl;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/* package */class YahooHelper
{
    static final ThreadLocal<DecimalFormat> FMT_PRICE = new ThreadLocal<DecimalFormat>()
    {
        protected DecimalFormat initialValue()
        {
            return new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$
        }
    };

    static long asPrice(String s) throws ParseException
    {
        if ("N/A".equals(s)) //$NON-NLS-1$
            return -1;
        return (long) (FMT_PRICE.get().parse(s).doubleValue() * 100);
    }

    static int asNumber(String s) throws ParseException
    {
        if ("N/A".equals(s)) //$NON-NLS-1$
            return -1;
        return FMT_PRICE.get().parse(s).intValue();
    }

    static LocalDate asDate(String s)
    {
        if ("\"N/A\"".equals(s)) //$NON-NLS-1$
            return null;
        return LocalDate.parse(s, DateTimeFormatter.ofPattern("\"M/d/yyyy\"")); //$NON-NLS-1$
    }

    static String stripQuotes(String s)
    {
        return s.substring(1, s.length() - 1);
    }
}
