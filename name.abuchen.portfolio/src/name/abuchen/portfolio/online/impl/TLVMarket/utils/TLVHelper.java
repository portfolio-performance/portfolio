package name.abuchen.portfolio.online.impl.TLVMarket.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.money.Values;

public class TLVHelper
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

    public enum SecurityType
    {
        SHARES(1), INDEX(2), MUTUAL_FUND(4), OTHER(0);

        private final int value;

        SecurityType(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }
    }

    public enum Language
    {
        HEBREW(0), ENGLISH(1);

        private final int value;

        Language(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }
    }


    public static Optional<String> extract(String body, int startIndex, String startToken, String endToken)
    {
        int begin = body.indexOf(startToken, startIndex);

        if (begin < 0)
            return Optional.empty();

        int end = body.indexOf(endToken, begin + startToken.length());
        if (end < 0)
            return Optional.empty();

        return Optional.of(body.substring(begin + startToken.length(), end));
    }

    static long asPrice(String s) throws ParseException
    {
        return asPrice(s, BigDecimal.ONE);
    }

    static long asPrice(String s, BigDecimal factor) throws ParseException
    {
        if ("N/A".equals(s) || "null".equals(s) || "NaN".equals(s) || ".".equals(s)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            return LatestSecurityPrice.NOT_AVAILABLE;
        BigDecimal v = (BigDecimal) FMT_PRICE.get().parse(s);
        return v.multiply(factor).multiply(Values.Quote.getBigDecimalFactor()).setScale(0, RoundingMode.HALF_UP)
                        .longValue();
    }

    static int asNumber(String s) throws ParseException
    {
        if ("N/A".equals(s) || "null".equals(s)) //$NON-NLS-1$ //$NON-NLS-2$
            return -1;
        return FMT_PRICE.get().parse(s).intValue();
    }

    public static LocalDate asDate(String s)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$

        if ("\"N/A\"".equals(s)) //$NON-NLS-1$
            return null;
        String dt = (s.trim()).replace("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
        return LocalDate.parse(dt, formatter); // $NON-NLS-1$
    }

    public static LocalDate asDateTime(String s)
    {
        DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss"); //$NON-NLS-1$

        if ("\"N/A\"".equals(s)) //$NON-NLS-1$
            return null;
        String dt = (s.trim()).replace("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
        return LocalDate.parse(dt, datetimeFormatter); // $NON-NLS-1$
    }

}
