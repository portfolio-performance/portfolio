package name.abuchen.portfolio.online.impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import name.abuchen.portfolio.money.Values;

/* package */class YahooHelper
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

    static long asPrice(String s) throws ParseException
    {
        if ("N/A".equals(s)) //$NON-NLS-1$
            return -1;
        BigDecimal v = (BigDecimal) FMT_PRICE.get().parse(s);
        return v.multiply(Values.Quote.getBigDecimalFactor()).longValue();
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
