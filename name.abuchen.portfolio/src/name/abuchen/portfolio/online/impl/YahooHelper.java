package name.abuchen.portfolio.online.impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import name.abuchen.portfolio.money.Values;

public /* package */class YahooHelper
{
    static final ThreadLocal<DecimalFormat> FMT_PRICE = new ThreadLocal<DecimalFormat>()
    {
        @Override
        protected DecimalFormat initialValue()
        {
            DecimalFormat fmt = new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$
            fmt.setParseBigDecimal(true);
            return fmt;
        }
    };

    static boolean isNotApplicable(String s)
    {
        if ("\"N/A\"".equals(s) || "N/A".equals(s)) //$NON-NLS-1$
            return true;
        return false;
    }

    static long asPrice(String s) throws ParseException
    {
        if (isNotApplicable(s)) //$NON-NLS-1$
            return -1;
        BigDecimal v = (BigDecimal) FMT_PRICE.get().parse(s);
        return v.multiply(Values.Quote.getBigDecimalFactor()).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
    }

    static double asDouble(String s) throws ParseException
    {
        if (isNotApplicable(s)) //$NON-NLS-1$
            return -1;
        return (double) FMT_PRICE.get().parse(s).doubleValue();
    }

    static BigDecimal asBigDecimal(String s) throws ParseException
    {
        if (isNotApplicable(s)) //$NON-NLS-1$
            return BigDecimal.ONE.multiply(BigDecimal.valueOf((long) -1));
        return BigDecimal.valueOf(asDouble(s));
    }

    static int asNumber(String s) throws ParseException
    {
        if (isNotApplicable(s)) //$NON-NLS-1$
            return -1;
        return FMT_PRICE.get().parse(s).intValue();
    }

    static LocalDate asDate(String s) throws ParseException
    {
        if (isNotApplicable(s)) //$NON-NLS-1$
            return null;
        return LocalDate.parse(s, DateTimeFormatter.ofPattern("\"M/d/yyyy\"")); //$NON-NLS-1$
    }

    static String stripQuotes(String s)
    {
        int firstIndex = 0;
        int lastIndex = s.length();
        if (s.startsWith("\""))
            firstIndex++;
        if (s.endsWith("\""))
            lastIndex--;
        //System.err.println("YahooHelper.stripQuotes: " + s.toString() + " [" + firstIndex + "," + lastIndex + "]");
        return s.substring(firstIndex, lastIndex);
    }

}
